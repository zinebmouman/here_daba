import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Heart, X, AlertTriangle, Trash2, ShoppingCart, ExternalLink, ChevronRight, Info } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { auth } from "../../config/Firebase";
import { 
  getFavorites, 
  saveFavorites, 
  addToCart, 
  removeFromFavorites,
  normalizeImageUrl,
  addToFavorites
} from '../../utils/shopUtils';

interface CartItem {
  id: number;
  nomProduit: string;
  prix: number;
  imageUrl?: string;
  quantite: number;
  categorie?: string;
}

interface Favorite {
  id: number;
  idProduit: number;
  dateAjout: string;
  produitDetails?: {
    nom: string;
    prix: number;
    imageUrl?: string;
    description?: string;
    categorie?: string;
  };
}

interface FavoritesPanelProps {
  isOpen: boolean;
  onClose: () => void;
  userId?: string;
}

const FAVORIS_ENDPOINT = '/api/favoris';
const PRODUITS_ENDPOINT = '/api/produits';

const FavoritesPanel: React.FC<FavoritesPanelProps> = ({
  isOpen,
  onClose,
  userId
}) => {
  const navigate = useNavigate();
  const [favorites, setFavorites] = useState<Favorite[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentUser, setCurrentUser] = useState<any>(null);
  const [addedToCart, setAddedToCart] = useState<number | null>(null);
  const [imgErrors, setImgErrors] = useState<Record<number, boolean>>({});
  const [isRemoving, setIsRemoving] = useState<number | null>(null);
  const [notification, setNotification] = useState<{message: string, type: 'success' | 'error'} | null>(null);

  // Surveiller les changements d'état d'authentification
  useEffect(() => {
    const unsubscribe = auth.onAuthStateChanged(user => {
      setCurrentUser(user);
    });

    return () => unsubscribe();
  }, []);

  // Fonction formatDate adaptée à LocalDateTime de Spring Boot
  const formatDate = (dateInput: string | number[]) => {
    try {
      let date: Date;

      // Si l'entrée est un tableau (format de Spring Boot)
      if (Array.isArray(dateInput)) {
        if (dateInput.length >= 6) {
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
      
      // Format jour/mois/année
      const day = date.getDate().toString().padStart(2, '0');
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const year = date.getFullYear();
      
      return `${day}/${month}/${year}`;
    } catch (error) {
      console.error("Erreur de formatage de date:", error, "pour la date:", dateInput);
      return "Date invalide";
    }
  };

  // Afficher une notification
  const showNotification = (message: string, type: 'success' | 'error') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 3000);
  };

  // Charger les favoris
  const loadFavorites = async () => {
    if (!isOpen || !currentUser) return;
    
    setLoading(true);
    setError(null);
    
    try {
      // Récupérer les favoris depuis le localStorage
      const localFavorites = getFavorites(currentUser.uid);
      
      // Trier par date d'ajout, du plus récent au plus ancien
      const sortedFavorites = localFavorites
        .filter(fav => fav && fav.produitDetails)
        .sort((a: Favorite, b: Favorite) => 
          new Date(b.dateAjout).getTime() - new Date(a.dateAjout).getTime()
        );
      
      setFavorites(sortedFavorites);
      setLoading(false);
    } catch (err: any) {
      console.error("Erreur lors du chargement des favoris:", err);
      setError("Impossible de charger les favoris");
      setLoading(false);
    }
  };

  // Surveiller les changements de favoris
  useEffect(() => {
    loadFavorites();
    
    // Écouter les événements de mise à jour des favoris
    const handleFavoritesUpdate = () => {
      loadFavorites();
    };
    
    window.addEventListener('favoritesUpdated', handleFavoritesUpdate);
    
    return () => {
      window.removeEventListener('favoritesUpdated', handleFavoritesUpdate);
    };
  }, [isOpen, currentUser]);

  // Supprimer un favori
  const handleRemoveFavorite = async (
    event: React.MouseEvent, 
    favoriteId: number, 
    productId: number
  ) => {
    event.preventDefault();
    event.stopPropagation();

    if (!currentUser) {
      navigate('/sign-in', { state: { redirect: window.location.pathname } });
      return;
    }

    try {
      setIsRemoving(productId);
      
      // Supprimer du localStorage
      const success = removeFromFavorites(productId, currentUser.uid);
      
      if (success) {
        // Mise à jour optimiste
        setFavorites(prev => prev.filter(fav => fav.idProduit !== productId));
        
        // Notification
        showNotification('Favori supprimé avec succès', 'success');
      }
    } catch (error) {
      console.error('Erreur lors de la suppression du favori:', error);
      showNotification('Impossible de supprimer ce favori', 'error');
    } finally {
      setIsRemoving(null);
    }
  };

  // Ajouter au panier
  const handleAddToCart = async (favorite: Favorite) => {
    if (!currentUser) {
      navigate('/sign-in', { state: { redirect: window.location.pathname } });
      return;
    }

    if (!favorite.produitDetails) {
      showNotification('Détails du produit manquants', 'error');
      return;
    }

    try {
      const productToAdd = {
        id: favorite.idProduit,
        nomProduit: favorite.produitDetails.nom,
        prix: favorite.produitDetails.prix,
        imageUrl: normalizeImageUrl(favorite.produitDetails.imageUrl),
        categorie: favorite.produitDetails.categorie
      };

      const added = addToCart(productToAdd, currentUser.uid);
      
      if (added) {
        setAddedToCart(favorite.idProduit);
        showNotification(`${favorite.produitDetails.nom} ajouté au panier`, 'success');
        
        // Réinitialiser l'état après 2 secondes
        setTimeout(() => setAddedToCart(null), 2000);
      }
    } catch (error) {
      console.error('Erreur lors de l\'ajout au panier:', error);
      showNotification('Impossible d\'ajouter le produit au panier', 'error');
    }
  };

  // Gérer les erreurs d'image
  const handleImageError = (productId: number) => {
    setImgErrors(prev => ({ ...prev, [productId]: true }));
  };

  // Ne pas afficher si le panneau est fermé
  if (!isOpen) return null;

  return (
    <>
      {/* Overlay */}
      <div 
        className="fixed inset-0 bg-black/30 backdrop-blur-sm z-40"
        onClick={onClose}
      />
    
      {/* Panel */}
      <div 
        className="fixed top-0 right-0 w-96 h-full bg-white shadow-2xl z-50 transform transition-transform duration-300 ease-in-out overflow-hidden"
        style={{ transform: isOpen ? 'translateX(0)' : 'translateX(100%)' }}
      >
        {/* Notification */}
        {notification && (
          <div 
            className={`absolute top-4 left-1/2 transform -translate-x-1/2 z-50 px-4 py-2 rounded-full ${
              notification.type === 'success' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
            } shadow-md flex items-center space-x-2 text-sm`}
          >
            {notification.type === 'success' ? (
              <div className="h-2 w-2 rounded-full bg-green-500"></div>
            ) : (
              <div className="h-2 w-2 rounded-full bg-red-500"></div>
            )}
            <span>{notification.message}</span>
          </div>
        )}
      
        {/* Header with gradient */}
        <div className="bg-gradient-to-r from-teal-500 to-teal-600 text-white p-6">
          <div className="flex justify-between items-center">
            <h2 className="text-xl font-bold flex items-center">
              <Heart className="fill-white stroke-white mr-2" size={22} />
              Mes Favoris
            </h2>
            <button 
              onClick={onClose} 
              className="text-white hover:text-teal-200 transition-colors rounded-full p-1 hover:bg-white/20"
            >
              <X size={24} />
            </button>
          </div>
          <p className="text-teal-100 text-sm mt-1">
            {favorites.length > 0 
              ? favorites.length === 1 
                ? 'Vous avez 1 article en favoris'
                : `Vous avez ${favorites.length} articles en favoris`
              : 'Vos produits préférés seront enregistrés ici'}
          </p>
        </div>

        {/* Body with card design */}
        <div className="h-[calc(100%-12rem)] overflow-y-auto bg-gray-50 p-2">
          {loading ? (
            <div className="flex justify-center items-center h-40">
              <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-teal-500" />
            </div>
          ) : error ? (
            <div className="mx-auto my-8 max-w-xs bg-white rounded-lg shadow-md p-6 text-center">
              <AlertTriangle className="mx-auto mb-3 text-amber-500" size={32} />
              <p className="text-gray-700">{error}</p>
              <button 
                onClick={() => loadFavorites()}
                className="mt-4 px-4 py-2 bg-teal-500 text-white rounded-full text-sm hover:bg-teal-600 transition-colors"
              >
                Réessayer
              </button>
            </div>
          ) : favorites.length === 0 ? (
            <div className="mx-auto my-8 max-w-xs bg-white rounded-xl shadow-md p-8 text-center">
              <div className="mb-4 w-16 h-16 bg-teal-50 rounded-full flex items-center justify-center mx-auto">
                <Heart className="text-teal-500" size={32} />
              </div>
              <h3 className="text-lg font-semibold mb-2">Aucun favori</h3>
              <p className="text-gray-500 text-sm mb-5">
                Commencez à explorer et ajoutez des produits à votre liste de favoris
              </p>
              <button 
                onClick={() => {
                  onClose();
                  navigate('/produits');
                }}
                className="px-6 py-2.5 bg-teal-500 text-white rounded-full text-sm font-medium hover:bg-teal-600 transition-colors shadow-sm hover:shadow"
              >
                Découvrir des produits
              </button>
            </div>
          ) : (
            <div className="space-y-3">
              {favorites.map((favorite) => (
                <div 
                  key={favorite.id} 
                  className="bg-white rounded-xl shadow-sm hover:shadow transition-all duration-200 overflow-hidden cursor-pointer"
                  onClick={() => navigate(`/product/${favorite.idProduit}`)}
                >
                  <div className="flex p-3">
                    {/* Image */}
                    <div className="mr-3 w-20 h-20 rounded-lg overflow-hidden flex-shrink-0 bg-gray-100">
                      {favorite.produitDetails?.imageUrl && !imgErrors[favorite.idProduit] ? (
                        <img 
                          src={normalizeImageUrl(favorite.produitDetails.imageUrl)} 
                          alt={favorite.produitDetails.nom} 
                          className="w-full h-full object-cover"
                          onError={() => handleImageError(favorite.idProduit)}
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center bg-gray-100">
                          <ShoppingCart className="text-gray-400" size={24} />
                        </div>
                      )}
                    </div>

                    {/* Product details */}
                    <div className="flex-1 min-w-0">
                      <div className="flex justify-between items-start">
                        <h3 className="font-medium text-gray-900 truncate pr-2">
                          {favorite.produitDetails?.nom || 'Produit sans nom'}
                        </h3>
                        <div className="flex-shrink-0">
                          <span className="font-bold text-teal-600">
                            {favorite.produitDetails?.prix.toFixed(2)} €
                          </span>
                        </div>
                      </div>
                      
                      {favorite.produitDetails?.categorie && (
                        <div className="mt-1">
                          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-teal-50 text-teal-700">
                            {favorite.produitDetails.categorie}
                          </span>
                        </div>
                      )}
                      
                      <p className="text-xs text-gray-500 mt-1.5">
                        Ajouté le {formatDate(favorite.dateAjout)}
                      </p>
                      
                      {/* Actions */}
                      <div className="flex justify-between items-center mt-2">
                        <div className="flex space-x-1">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              handleAddToCart(favorite);
                            }}
                            className={`p-1.5 rounded-full text-xs font-medium flex items-center ${
                              addedToCart === favorite.idProduit 
                                ? 'bg-green-100 text-green-700' 
                                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                            } transition-colors`}
                          >
                            {addedToCart === favorite.idProduit ? (
                              <>
                                <div className="w-4 h-4 rounded-full bg-green-500 flex items-center justify-center mr-1">
                                  <svg width="8" height="8" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                    <path d="M5 12L10 17L20 7" stroke="white" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"/>
                                  </svg>
                                </div>
                                <span>Ajouté</span>
                              </>
                            ) : (
                              <>
                                <ShoppingCart size={14} className="mr-1" />
                                <span>Ajouter au panier</span>
                              </>
                            )}
                          </button>
                        </div>
                        
                        <button
                          onClick={(e) => handleRemoveFavorite(e, favorite.id, favorite.idProduit)}
                          disabled={isRemoving === favorite.idProduit}
                          className="p-1.5 rounded-full text-gray-600 hover:bg-red-50 hover:text-red-500 transition-colors"
                          title="Supprimer des favoris"
                        >
                          {isRemoving === favorite.idProduit ? (
                            <div className="animate-spin h-4 w-4 border-t-2 border-red-500 rounded-full" />
                          ) : (
                            <Trash2 size={16} />
                          )}
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Footer */}
        {favorites.length > 0 && (
          <div className="absolute bottom-0 left-0 right-0 bg-white border-t p-4">
            <button 
              onClick={() => {
                onClose();
                navigate('/favoris');
              }}
              className="w-full bg-teal-500 hover:bg-teal-600 text-white py-3 rounded-lg shadow-sm transition-colors flex items-center justify-center font-medium"
            >
              Voir tous mes favoris
              <ChevronRight className="ml-1" size={16} />
            </button>
          </div>
        )}
      </div>
    </>
  );
};

export default FavoritesPanel;