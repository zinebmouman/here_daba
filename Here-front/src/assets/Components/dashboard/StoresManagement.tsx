import React, { useState, useEffect } from "react";
import { Plus, Edit, Trash2, MapPin, Phone, Clock, Store } from "lucide-react";
import { auth } from "../../../config/Firebase";
import { useNavigate } from "react-router-dom";
import StoreForm from "./StoreForm";
import DashboardNavigation from "./DashboardNavigation";

const StoresManagement = () => {
  const navigate = useNavigate();
  const [stores, setStores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingStore, setEditingStore] = useState(null);
  const [error, setError] = useState("");

  // Fonction pour vérifier l'état d'authentification
  useEffect(() => {
    const checkAuth = () => {
      const user = auth.currentUser;
      if (!user) {
        console.log("Redirection vers la page de connexion depuis useEffect");
        navigate('/login');
      }
    };

    checkAuth();
    
    // Ajouter un observateur pour les changements d'authentification
    const unsubscribe = auth.onAuthStateChanged((user) => {
      if (!user) {
        console.log("Utilisateur déconnecté, redirection vers la page de connexion");
        navigate('/login');
      }
    });

    return () => unsubscribe();
  }, [navigate]);

  // Load stores when component mounts
  useEffect(() => {
// Dans la fonction loadStores
const loadStores = async () => {
  setLoading(true);
  setError("");
  
  try {
    // Vérifier si l'utilisateur est connecté
    const user = auth.currentUser;
    if (!user) {
      console.log("Pas d'utilisateur authentifié lors du chargement des boutiques");
      setLoading(false);
      return;
    }

    console.log("Utilisateur authentifié:", user.uid);
    
    // Utiliser votre proxy Vite
    const idToken = await user.getIdToken(true);
    console.log("Token obtenu:", idToken.substring(0, 10) + "...");
    
    const response = await fetch('/api/boutiques/mes-boutiques', {
      headers: {
        'Authorization': `Bearer ${idToken}`,
        'X-Vendeur-ID': user.uid // Important: Envoyer cet en-tête
      }
    });

    console.log("Statut de la réponse:", response.status);
    
    if (!response.ok) {
      const errorText = await response.text();
      console.error("Contenu de l'erreur:", errorText);
      throw new Error(`Erreur HTTP: ${response.status} - ${errorText}`);
    }

    const storesData = await response.json();
    console.log("Données reçues:", storesData);
    setStores(storesData);
  } catch (error) {
    console.error("Erreur lors du chargement des boutiques:", error);
    setError("Impossible de charger les boutiques: " + error.message);
  } finally {
    setLoading(false);
  }
};

    loadStores();
  }, []);

  // Handle editing a store
  const handleEditStore = (store) => {
    setEditingStore(store);
    setShowForm(true);
  };

  // Handle form submission for creating a new store
  const handleCreateStore = async (submission) => {
    setError("");
    
    try {
      // Extraire les données et informations d'authentification
      const { formData, idToken, userId } = submission;
      
      console.log("Envoi des données de la boutique:", formData);
      console.log("Utilisation du token:", idToken.substring(0, 10) + "...");
      console.log("ID vendeur:", userId);
      
      // Envoyer la requête de création
      const response = await fetch('/api/boutiques', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${idToken}`,
          'X-Vendeur-ID': userId
        },
        body: JSON.stringify(formData)
      });

      // Vérifier si la requête a réussi
      if (!response.ok) {
        throw new Error(`Erreur HTTP: ${response.status}`);
      }

      // Récupérer les données de la nouvelle boutique
      const newStore = await response.json();
      console.log("Boutique créée avec succès:", newStore);
      
      // Mettre à jour l'état local
      setStores([...stores, newStore]);
      setShowForm(false);
      
      // Afficher un message de succès
      alert("Boutique créée avec succès!");
      
      // Recharger la page pour afficher les données mises à jour
      window.location.reload();
    } catch (error) {
      console.error("Erreur lors de la création de la boutique:", error);
      setError("Échec de la création de la boutique: " + error.message);
    }
  };

  // Handle updating a store
 // Handle updating a store
// Handle updating a store
const handleUpdateStore = async (data) => {
  if (!editingStore) return;
  
  setError("");
  
  try {
    // Vérifier que l'utilisateur est connecté
    const user = auth.currentUser;
    if (!user) {
      console.log("Tentative de mise à jour de boutique sans authentification");
      navigate('/login');
      return;
    }
    
    // Extraire les données du formulaire
    // Si les données viennent directement du formulaire (mise à jour)
    let storeData;
    let idToken;
    
    // Vérifier le format des données reçues
    console.log("STOREFORM - DONNÉES REÇUES:", data);
    
    if (data.formData) {
      // Format: { formData: {...}, idToken: "...", userId: "..." }
      storeData = data.formData;
      idToken = data.idToken;
      console.log("Données reçues au format création:", storeData);
    } else {
      // Format: données directes du formulaire
      storeData = data;
      idToken = await user.getIdToken(true);
      console.log("Données reçues au format mise à jour:", storeData);
    }
    
    // Assurer que l'ID boutique est inclus
    storeData.id_boutique = editingStore.id_boutique;
    
    // Si c'est une mise à jour, il faut envoyer l'idVendeur
    if (!storeData.idVendeur) {
      storeData.idVendeur = editingStore.idVendeur || user.uid;
    }
    
    console.log("DONNÉES FINALES ENVOYÉES AU SERVEUR:", storeData);
    
    // Envoyer la requête de mise à jour
    const response = await fetch(`/api/boutiques/${editingStore.id_boutique}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${idToken}`,
        'X-Vendeur-ID': user.uid
      },
      body: JSON.stringify(storeData)
    });

    // Vérifier si la requête a réussi
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Erreur HTTP: ${response.status} - ${errorText}`);
    }

    // Récupérer les données de la boutique mise à jour
    const updatedStore = await response.json();
    console.log("Boutique mise à jour avec succès:", updatedStore);
    
    // Mettre à jour l'état local
    const updatedStores = stores.map(store => 
      store.id_boutique === updatedStore.id_boutique ? updatedStore : store
    );
    setStores(updatedStores);
    setEditingStore(null);
    setShowForm(false);
    
    // Afficher un message de succès
    alert("Boutique mise à jour avec succès !");
  } catch (error) {
    console.error("Erreur lors de la mise à jour de la boutique:", error);
    
    // Vérifier si l'erreur est liée à l'authentification
    if (error.message.includes("401") || error.message.includes("403")) {
      setError("Problème d'authentification. Veuillez vous reconnecter.");
    } else {
      setError("Échec de la mise à jour de la boutique: " + error.message);
    }
  }
};
  // Handle deleting a store - CORRECTION: Implémentation de la fonction manquante
  const handleDeleteStore = async (storeId) => {
    if (!window.confirm("Êtes-vous sûr de vouloir supprimer cette boutique ?")) {
      return; // L'utilisateur a annulé la suppression
    }
    
    try {
      // Vérifier que l'utilisateur est connecté
      const user = auth.currentUser;
      if (!user) {
        console.log("Tentative de suppression de boutique sans authentification");
        navigate('/login');
        return;
      }

      // Récupérer un token d'authentification frais
      const idToken = await user.getIdToken(true);

      // Envoyer la requête de suppression
      const response = await fetch(`/api/boutiques/${storeId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${idToken}`,
          'X-Vendeur-ID': user.uid
        }
      });

      // Vérifier si la requête a réussi
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Erreur HTTP: ${response.status} - ${errorText}`);
      }

      // Mettre à jour l'état local en supprimant la boutique
      const updatedStores = stores.filter(store => store.id_boutique !== storeId);
      setStores(updatedStores);

      // Afficher un message de succès
      alert("Boutique supprimée avec succès !");
    } catch (error) {
      console.error("Erreur lors de la suppression de la boutique:", error);
      setError("Échec de la suppression de la boutique: " + error.message);
    }
  };

  return (
    <div className="space-y-6">
      {/* Dashboard Navigation */}
      <DashboardNavigation />
      
      {/* Header with add button */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">
            Gestion des boutiques
          </h1>
          <p className="text-gray-500 mt-1">
            Gérez vos emplacements de boutiques physiques
          </p>
        </div>
        <button
          onClick={() => {
            setEditingStore(null);
            setShowForm(true);
          }}
          className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-teal-600 hover:bg-teal-700"
        >
          <Plus size={18} className="mr-2" />
          Ajouter une boutique
        </button>
      </div>
      
      {/* Afficher les erreurs */}
      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
          {error}
        </div>
      )}
      
      {/* Indicateur de chargement */}
      {loading && (
        <div className="text-center py-6">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-teal-600"></div>
          <p className="mt-2 text-gray-500">Chargement des données...</p>
        </div>
      )}
      
      {/* Store Form */}
      {showForm && (
        <StoreForm
          store={editingStore}
          onSubmit={editingStore ? handleUpdateStore : handleCreateStore}
          onCancel={() => {
            setShowForm(false);
            setEditingStore(null);
          }}
        />
      )}

      {/* Stores Grid */}
      {!loading && (
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {stores.length > 0 ? (
            stores.map((store) => (
              <div 
                key={store.id_boutique}
                className="bg-white overflow-hidden shadow rounded-lg border border-gray-200"
              >
                <div className="px-4 py-5 sm:px-6 bg-gradient-to-r from-teal-50 to-blue-50 border-b border-gray-200">
                  <div className="flex justify-between items-start">
                    <div className="flex items-center">
                      <div className="flex-shrink-0 h-10 w-10 rounded-full bg-teal-100 flex items-center justify-center text-teal-600">
                        <Store size={20} />
                      </div>
                      <div className="ml-4">
                        <h3 className="text-lg font-medium leading-6 text-gray-900">
                          {store.nom}
                        </h3>
                        <p className="text-sm text-gray-500 mt-1">
                          {store.ville}, {store.pays}
                        </p>
                      </div>
                    </div>
                    <div className="flex space-x-2">
                      <button
                        onClick={() => handleEditStore(store)}
                        className="text-gray-400 hover:text-gray-500"
                        aria-label="Modifier"
                      >
                        <Edit size={18} />
                      </button>
                      <button
                        onClick={() => handleDeleteStore(store.id_boutique)}
                        className="text-red-400 hover:text-red-500"
                        aria-label="Supprimer"
                      >
                        <Trash2 size={18} />
                      </button>
                    </div>
                  </div>
                </div>
                <div className="px-4 py-5 sm:p-6 space-y-4">
                  <div className="flex items-start">
                    <MapPin className="h-5 w-5 text-gray-400 mt-0.5 flex-shrink-0" />
                    <div className="ml-3 text-sm text-gray-500">
                      <p>{store.adress}</p>
                      <p>
                        {store.codePostal} {store.ville}
                      </p>
                      <p>{store.pays}</p>
                    </div>
                  </div>
                  {store.contact && (
                    <div className="flex items-start">
                      <Phone className="h-5 w-5 text-gray-400 mt-0.5 flex-shrink-0" />
                      <div className="ml-3 text-sm text-gray-500">
                        <p>{store.contact}</p>
                      </div>
                    </div>
                  )}
                  {store.horaire && (
                    <div className="flex items-start">
                      <Clock className="h-5 w-5 text-gray-400 mt-0.5 flex-shrink-0" />
                      <div className="ml-3 text-sm text-gray-500">
                        <p>{store.horaire}</p>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            ))
          ) : (
            <div className="col-span-full bg-white p-6 rounded-lg shadow-md text-center">
              <Store size={48} className="mx-auto text-gray-300 mb-3" />
              <h3 className="text-lg font-medium text-gray-900 mb-1">
                Aucune boutique trouvée
              </h3>
              <p className="text-gray-500 mb-4">
                Ajoutez votre première boutique pour gérer vos emplacements physiques.
              </p>
              <button
                onClick={() => setShowForm(true)}
                className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-teal-600 hover:bg-teal-700"
              >
                <Plus size={18} className="mr-2" />
                Ajouter la première boutique
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default StoresManagement;