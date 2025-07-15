// NotificationPanel.tsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import {
    AlertTriangle,
    Clock,
    CheckCircle,
    Bell,
    X,
    Trash2 // Ajout de l'icône de poubelle
} from 'lucide-react';

interface Notification {
    id: number;
    type: 'CRITICAL_STOCK' | 'PRODUCT_EXPIRATION' | 'ORDER_CONFIRMATION' | string;
    message: string;
    dateEnvoi: string;
    vendeurId: string;
    produitId?: number;
    status: 'LU' | 'NON_LU';
}

interface NotificationPanelProps {
    isOpen: boolean;
    onClose: () => void;
    vendeurId: string;
    authToken?: string;
    onNotificationsRead?: () => void; // Pour informer le parent quand les notifications sont lues
}

const NOTIFICATIONS_ENDPOINT = '/api/notifications';

const NotificationPanel: React.FC<NotificationPanelProps> = ({
    isOpen,
    onClose,
    vendeurId,
    authToken,
    onNotificationsRead
}) => {
    const [notifications, setNotifications] = useState<Notification[]>([]);
    const [filteredNotifications, setFilteredNotifications] = useState<Notification[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [filter, setFilter] = useState<string | null>(null);

    // Fonction formatDate adaptée à LocalDateTime de Spring Boot
    const formatDate = (dateInput: string | number[]) => {
        try {
            let date: Date;

            // Si l'entrée est un tableau (format de Spring Boot)
            if (Array.isArray(dateInput)) {
                // Vérifier que le tableau a la bonne structure
                if (dateInput.length >= 6) {
                    // Créer une date à partir des composants du tableau
                    // [année, mois, jour, heures, minutes, secondes, millisecondes]
                    date = new Date(
                        dateInput[0], // année
                        dateInput[1] - 1, // mois (0-indexé)
                        dateInput[2], // jour
                        dateInput[3], // heures
                        dateInput[4], // minutes
                        dateInput[5] // secondes
                    );
                } else {
                    console.error("Tableau de date invalide:", dateInput);
                    return "Date invalide";
                }
            } else {
                // Sinon, traiter comme une chaîne de date standard
                date = new Date(dateInput);
            }
            
            // Vérifier si la date est valide
            if (isNaN(date.getTime())) {
                console.error("Date invalide:", dateInput);
                return "Date invalide";
            }
            
            // Formater la date manuellement pour avoir un format jour/mois/année
            const day = date.getDate().toString().padStart(2, '0');
            const month = (date.getMonth() + 1).toString().padStart(2, '0'); // Les mois commencent à 0
            const year = date.getFullYear();
            const hours = date.getHours().toString().padStart(2, '0');
            const minutes = date.getMinutes().toString().padStart(2, '0');
            
            // Format jour/mois/année heures:minutes
            return `${day}/${month}/${year} ${hours}:${minutes}`;
        } catch (error) {
            console.error("Erreur de formatage de date:", error, "pour la date:", dateInput);
            return "Date invalide";
        }
    };

    const fetchNotifications = async () => {
        if (!isOpen || !vendeurId) return;
        setLoading(true);
        setError(null);
        try {
            const headers: Record<string, string> = {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            };
            if (authToken) {
                headers['Authorization'] = `Bearer ${authToken}`;
            }
            
            // Ajout des logs de debugging
            console.log("Récupération des notifications pour vendeurId:", vendeurId);
            console.log("URL complète:", `${NOTIFICATIONS_ENDPOINT}/vendeur/${vendeurId}`);
            console.log("Headers de la requête:", headers);
            
            const response = await axios.get<Notification[]>(
                `${NOTIFICATIONS_ENDPOINT}/vendeur/${vendeurId}`,
                { 
                    headers, 
                    timeout: 15000,
                    withCredentials: true // Permet d'envoyer des cookies avec la requête
                }
            );
            
            // Logs détaillés de la réponse
            console.log("Réponse des notifications - statut:", response.status);
            console.log("Réponse des notifications - headers:", response.headers);
            console.log("Réponse des notifications - données brutes:", response.data);
            
            const sortedNotifications = response.data
                .sort((a, b) => new Date(b.dateEnvoi).getTime() - new Date(a.dateEnvoi).getTime());
            console.log("Notifications triées:", sortedNotifications);
            setNotifications(sortedNotifications);
            setFilteredNotifications(sortedNotifications);
        } catch (err: any) {
            let errorMessage = "Une erreur s'est produite lors du chargement des notifications.";
            if (axios.isAxiosError(err)) {
                console.error("Détails de l'erreur Axios:", err);
                console.error("Message d'erreur:", err.message);
                console.error("Code d'erreur:", err.code);
                console.error("Statut de la réponse:", err.response?.status);
                console.error("Données de réponse:", err.response?.data);
                
                if (err.response?.status === 404) {
                    errorMessage = "Aucune notification trouvée pour ce vendeur.";
                } else if (err.response?.status === 400) {
                    errorMessage = "Requête invalide. Veuillez vérifier l'ID du vendeur.";
                } else if (err.response?.data) {
                    errorMessage = typeof err.response.data === 'string'
                        ? err.response.data
                        : err.response.data.message || "Erreur du serveur.";
                } else if (err.code === 'ECONNABORTED') {
                    errorMessage = "La requête a expiré. Veuillez vérifier votre connexion internet.";
                } else {
                    errorMessage = "Erreur de connexion au serveur.";
                }
            } else {
                console.error("Erreur non-Axios:", err);
                errorMessage = "Erreur inattendue.";
            }
            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        console.log("Les props du NotificationPanel ont changé. vendeurId:", vendeurId);
        fetchNotifications();
        
        // Ajouter un intervalle pour rafraîchir les notifications toutes les 30 secondes
        const intervalId = setInterval(() => {
            console.log("Rafraîchissement automatique des notifications");
            fetchNotifications();
        }, 30000);
        
        // Nettoyer l'intervalle lors du démontage du composant
        return () => clearInterval(intervalId);
    }, [isOpen, vendeurId, authToken]);

    useEffect(() => {
        console.log("État des notifications modifié:", notifications);
        if (filter) {
            setFilteredNotifications(notifications.filter(notif => notif.type === filter));
        } else {
            setFilteredNotifications(notifications);
        }
    }, [filter, notifications]);

    const getNotificationIcon = (type: string) => {
        switch (type) {
            case 'CRITICAL_STOCK':
                return <AlertTriangle className="text-red-500 w-5 h-5" />;
            case 'PRODUCT_EXPIRATION':
                return <Clock className="text-yellow-500 w-5 h-5" />;
            case 'ORDER_CONFIRMATION':
                return <CheckCircle className="text-green-500 w-5 h-5" />;
            default:
                return <Bell className="text-blue-500 w-5 h-5" />;
        }
    };

    const getNotificationTitle = (type: string) => {
        switch (type) {
            case 'CRITICAL_STOCK':
                return 'Alerte Stock';
            case 'PRODUCT_EXPIRATION':
                return 'Expiration Produit';
            case 'ORDER_CONFIRMATION':
                return 'Nouvelle Commande';
            default:
                return 'Notification';
        }
    };

    const markAsRead = async (id: number) => {
        try {
            const headers: Record<string, string> = {
                'Content-Type': 'application/json'
            };

            if (authToken) {
                headers['Authorization'] = `Bearer ${authToken}`;
            }

            console.log(`Marquage de la notification ${id} comme lue`);
            
            await axios.put(
                `${NOTIFICATIONS_ENDPOINT}/${id}/markAsRead`,
                {},
                { headers, withCredentials: true }
            );

            console.log(`Notification ${id} marquée comme lue avec succès`);
            
            // Mise à jour optimiste du statut
            setNotifications(prev => prev.map(notif =>
                notif.id === id ? { ...notif, status: 'LU' } : notif
            ));

            // Mise à jour des notifications filtrées également
            setFilteredNotifications(prev => prev.map(notif =>
                notif.id === id ? { ...notif, status: 'LU' } : notif
            ));
            
            // Informer le parent que des notifications ont été lues
            if (onNotificationsRead) {
                onNotificationsRead();
            }
        } catch (err: any) {
            console.error(`Erreur lors du marquage de la notification comme lue: ${id}`, err);
        }
    };

    const markAllAsRead = async () => {
        try {
            const headers: Record<string, string> = {
                'Content-Type': 'application/json'
            };

            if (authToken) {
                headers['Authorization'] = `Bearer ${authToken}`;
            }
            
            console.log(`Marquage de toutes les notifications comme lues pour le vendeur ${vendeurId}`);

            await axios.put(
                `${NOTIFICATIONS_ENDPOINT}/markAllAsRead?vendeurId=${vendeurId}`,
                {},
                { headers, withCredentials: true }
            );
            
            console.log(`Toutes les notifications marquées comme lues avec succès`);

            // Mise à jour optimiste des statuts de toutes les notifications
            const updatedNotifications = notifications.map(notif => ({
                ...notif,
                status: 'LU'
            }));

            setNotifications(updatedNotifications);
            setFilteredNotifications(updatedNotifications);
            
            // Informer le parent que des notifications ont été lues
            if (onNotificationsRead) {
                onNotificationsRead();
            }
        } catch (err: any) {
            console.error("Erreur lors du marquage de toutes les notifications comme lues", err);
        }
    };

    // Nouvelle fonction pour supprimer une notification
    const deleteNotification = async (event: React.MouseEvent, id: number) => {
        // Empêcher la propagation de l'événement pour éviter de marquer comme lu
        event.stopPropagation();
        
        try {
            const headers: Record<string, string> = {
                'Content-Type': 'application/json'
            };

            if (authToken) {
                headers['Authorization'] = `Bearer ${authToken}`;
            }

            console.log(`Suppression de la notification ${id}`);
            
            await axios.delete(
                `${NOTIFICATIONS_ENDPOINT}/${id}`,
                { headers, withCredentials: true }
            );

            console.log(`Notification ${id} supprimée avec succès`);
            
            // Mise à jour optimiste en retirant la notification supprimée
            setNotifications(prev => prev.filter(notif => notif.id !== id));
            setFilteredNotifications(prev => prev.filter(notif => notif.id !== id));
            
            // Informer le parent si nécessaire
            if (onNotificationsRead) {
                onNotificationsRead();
            }
        } catch (err: any) {
            console.error(`Erreur lors de la suppression de la notification: ${id}`, err);
        }
    };

    // Fonction pour supprimer toutes les notifications
    const deleteAllNotifications = async () => {
        try {
            const headers: Record<string, string> = {
                'Content-Type': 'application/json'
            };

            if (authToken) {
                headers['Authorization'] = `Bearer ${authToken}`;
            }
            
            console.log(`Suppression de toutes les notifications pour le vendeur ${vendeurId}`);

            await axios.delete(
                `${NOTIFICATIONS_ENDPOINT}/all?vendeurId=${vendeurId}`,
                { headers, withCredentials: true }
            );
            
            console.log(`Toutes les notifications supprimées avec succès`);

            // Mise à jour optimiste en vidant la liste des notifications
            setNotifications([]);
            setFilteredNotifications([]);
            
            // Informer le parent si nécessaire
            if (onNotificationsRead) {
                onNotificationsRead();
            }
        } catch (err: any) {
            console.error("Erreur lors de la suppression de toutes les notifications", err);
        }
    };

    const unreadCount = notifications.filter(n => n.status === 'NON_LU').length;

    if (!isOpen) return null;

    return (
        <div className="absolute right-0 mt-2 w-80 bg-white rounded-lg shadow-lg py-2 z-50" style={{ top: "100%" }}>
            <div className="flex justify-between items-center px-4 py-2 border-b border-gray-100">
                <h3 className="font-medium">Notifications {unreadCount > 0 && `(${unreadCount})`}</h3>
                <div className="flex items-center space-x-4">
                    {notifications.length > 0 && (
                        <button
                            onClick={deleteAllNotifications}
                            className="text-xs text-red-600 hover:text-red-800 flex items-center"
                            disabled={loading}
                            title="Supprimer toutes les notifications"
                        >
                            <Trash2 className="w-4 h-4 mr-1" />
                            <span>Tout</span>
                        </button>
                    )}
                    {unreadCount > 0 && (
                        <button
                            onClick={markAllAsRead}
                            className="text-xs text-teal-600 hover:text-teal-800"
                            disabled={loading}
                        >
                            Tout lu
                        </button>
                    )}
                    <button
                        onClick={onClose}
                        className="text-gray-500 hover:text-gray-700"
                        disabled={loading}
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>
            </div>

            <div className="flex border-b border-gray-100">
                <button
                    onClick={() => setFilter(null)}
                    className={`flex-1 py-2 px-1 text-center text-xs font-medium ${!filter ? 'text-teal-600 border-b-2 border-teal-500' : 'text-gray-500 hover:text-gray-700'}`}
                    disabled={loading}
                >
                    Toutes
                </button>
                <button
                    onClick={() => setFilter('CRITICAL_STOCK')}
                    className={`flex-1 py-2 px-1 text-center text-xs font-medium ${filter === 'CRITICAL_STOCK' ? 'text-red-600 border-b-2 border-red-500' : 'text-gray-500 hover:text-gray-700'}`}
                    disabled={loading}
                >
                    Stock
                </button>
                <button
                    onClick={() => setFilter('PRODUCT_EXPIRATION')}
                    className={`flex-1 py-2 px-1 text-center text-xs font-medium ${filter === 'PRODUCT_EXPIRATION' ? 'text-yellow-600 border-b-2 border-yellow-500' : 'text-gray-500 hover:text-gray-700'}`}
                    disabled={loading}
                >
                    Expiration
                </button>
                <button
                    onClick={() => setFilter('ORDER_CONFIRMATION')}
                    className={`flex-1 py-2 px-1 text-center text-xs font-medium ${filter === 'ORDER_CONFIRMATION' ? 'text-green-600 border-b-2 border-green-500' : 'text-gray-500 hover:text-gray-700'}`}
                    disabled={loading}
                >
                    Commandes
                </button>
            </div>

            <div className="max-h-80 overflow-y-auto">
                {loading ? (
                    <div className="flex items-center justify-center h-32">
                        <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-teal-500"></div>
                    </div>
                ) : error ? (
                    <div className="p-4 text-center text-red-500">
                        <AlertTriangle className="mx-auto h-8 w-8 mb-2" />
                        <p className="text-sm">{error}</p>
                        <button
                            onClick={() => fetchNotifications()}
                            className="mt-2 text-xs text-teal-600 hover:text-teal-800"
                        >
                            Réessayer
                        </button>
                    </div>
                ) : filteredNotifications.length === 0 ? (
                    <div className="p-4 text-center text-gray-500">
                        <Bell className="mx-auto h-8 w-8 mb-2 text-gray-400" />
                        <p className="text-sm">Aucune notification</p>
                    </div>
                ) : (
                    <div>
                        {filteredNotifications.map((notification) => (
                            <div
                                key={notification.id}
                                className={`px-4 py-3 border-b border-gray-100 hover:bg-gray-50 cursor-pointer ${notification.status === 'NON_LU' ? 'bg-blue-50' : ''}`}
                                onClick={() => notification.status === 'NON_LU' && markAsRead(notification.id)}
                            >
                                <div className="flex items-start">
                                    <div className="flex-shrink-0 mt-1 mr-3">
                                        {getNotificationIcon(notification.type)}
                                    </div>
                                    <div className="min-w-0 flex-1">
                                        <div className="flex justify-between items-center mb-1">
                                            <p className="text-sm font-medium text-gray-900">
                                                {getNotificationTitle(notification.type)}
                                            </p>
                                            {notification.status === 'NON_LU' && (
                                                <span className="h-2 w-2 bg-blue-600 rounded-full"></span>
                                            )}
                                        </div>
                                        <p className="text-sm text-gray-600">
                                            {notification.message}
                                        </p>
                                        <p className="mt-1 text-xs text-gray-500">
                                            {formatDate(notification.dateEnvoi)}
                                        </p>
                                    </div>
                                    {/* Bouton de suppression */}
                                    <div className="ml-2 flex-shrink-0">
                                        <button
                                            onClick={(e) => deleteNotification(e, notification.id)}
                                            className="text-gray-400 hover:text-red-600 transition-colors duration-200"
                                            title="Supprimer cette notification"
                                        >
                                            <Trash2 className="w-4 h-4" />
                                        </button>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            <div className="px-4 py-2 border-t border-gray-100">
            <a
                    href="/notifications" 
                    className="block text-center text-sm text-teal-600 hover:text-teal-800"
                    onClick={(e) => {
                        e.preventDefault();
                        onClose();
                        console.log("Voir toutes les notifications");
                    }}
                >
                    Voir toutes les notifications
                </a>
            </div>
            
            <div className="px-4 py-1 text-center">
                <button
                    onClick={() => {
                        console.log("Rafraîchissement manuel des notifications");
                        fetchNotifications();
                    }}
                    className="text-xs text-gray-400 hover:text-gray-600"
                >
                    Rafraîchir
                </button>
            </div>
        </div>
    );
};

export default NotificationPanel;