// src/Components/CartPanel.tsx
import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { X, ShoppingCart, Trash2, Plus, Minus, ChevronRight, AlertTriangle, ExternalLink } from "lucide-react";
import { auth } from "../../config/Firebase";
import { 
  getCartItems, 
  updateCartItemQuantity, 
  removeFromCart,
  normalizeImageUrl
} from "../../utils/shopUtils";

interface CartItem {
  id: number;
  nomProduit: string;
  prix: number;
  imageUrl?: string;
  quantite: number;
  categorie?: string;
}

interface CartPanelProps {
  isOpen: boolean;
  onClose: () => void;
  userId?: string;
  authToken?: Promise<string> | undefined;
}

const CartPanel: React.FC<CartPanelProps> = ({ isOpen, onClose, userId, authToken }) => {
  const navigate = useNavigate();
  const [cartItems, setCartItems] = useState<CartItem[]>([]);
  const [subtotal, setSubtotal] = useState<number>(0);
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [imgErrors, setImgErrors] = useState<Record<number, boolean>>({});
  const [notification, setNotification] = useState<{message: string, type: 'success' | 'error'} | null>(null);
  const [isRemoving, setIsRemoving] = useState<number | null>(null);
  const [isUpdating, setIsUpdating] = useState<number | null>(null);

  useEffect(() => {
    // Vérifiez si userId existe et n'est pas vide
    setIsAuthenticated(userId !== undefined && userId !== null && userId !== "");
    
    // Si l'utilisateur est authentifié, chargez immédiatement les articles du panier
    if (userId) {
      loadCartItems();
    }
  }, [userId]);

  // Afficher une notification
  const showNotification = (message: string, type: 'success' | 'error') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 3000);
  };

  // Charger les éléments du panier depuis le localStorage
  useEffect(() => {
    if (isOpen) {
      // Vérifiez si l'utilisateur est authentifié dans cette fonction
      if (userId) {
        loadCartItems();
      }
    }
    
    // Écouter les événements de mise à jour du panier
    const handleCartUpdate = (event: Event) => {
      // Traiter l'événement comme CustomEvent
      const customEvent = event as CustomEvent;
      if (userId) {
        loadCartItems();
      }
    };
    
    window.addEventListener('cartUpdated', handleCartUpdate);
    
    return () => {
      window.removeEventListener('cartUpdated', handleCartUpdate);
    };
  }, [isOpen, userId]);

  const loadCartItems = () => {
    if (!userId) return;
    
    // Charger les articles du panier
    const items = getCartItems(userId);
    
    // Réinitialiser les erreurs d'image
    const newImgErrors: Record<number, boolean> = {};
    items.forEach((item: CartItem) => {
      newImgErrors[item.id] = false;
    });
    setImgErrors(newImgErrors);
    
    setCartItems(items);
    
    // Calculer le sous-total
    const total = items.reduce((sum: number, item: CartItem) => 
      sum + (item.prix * item.quantite), 0);
    setSubtotal(total);
  };

  // Mettre à jour la quantité d'un article
  const handleUpdateQuantity = async (itemId: number, newQuantity: number) => {
    if (!userId || newQuantity < 1) return;

    setIsUpdating(itemId);
    
    try {
      updateCartItemQuantity(itemId, newQuantity, userId);
      loadCartItems(); // Recharger le panier
      
      if (newQuantity > 1) {
        showNotification('Quantité mise à jour', 'success');
      }
    } catch (error) {
      showNotification('Erreur lors de la mise à jour de la quantité', 'error');
    } finally {
      setIsUpdating(null);
    }
  };

  // Supprimer un article du panier
  const handleRemoveItem = async (itemId: number) => {
    if (!userId) return;
    
    setIsRemoving(itemId);
    
    try {
      removeFromCart(itemId, userId);
      
      // Trouver le nom du produit pour le message
      const itemName = cartItems.find(item => item.id === itemId)?.nomProduit || 'Article';
      
      // Mise à jour optimiste
      setCartItems(prev => prev.filter(item => item.id !== itemId));
      
      // Recalculer le sous-total
      setSubtotal(
        cartItems
          .filter(item => item.id !== itemId)
          .reduce((sum, item) => sum + (item.prix * item.quantite), 0)
      );
      
      showNotification(`${itemName} retiré du panier`, 'success');
      
      // Charger à nouveau le panier après une courte période
      setTimeout(() => {
        loadCartItems();
      }, 300);
    } catch (error) {
      showNotification('Erreur lors de la suppression de l\'article', 'error');
      loadCartItems();
    } finally {
      setIsRemoving(null);
    }
  };

  // Rediriger vers la page de connexion si non connecté
  const handleSignInRedirect = () => {
    onClose();
    // Stocker l'URL actuelle pour rediriger après connexion
    localStorage.setItem('redirect_after_login', window.location.pathname);
    navigate('/sign-in');
  };

  // Handler pour le bouton Commander
  const handleCheckout = () => {
    onClose(); 
    navigate("/checkout"); // Redirection vers la page de commande
  };

  // Handler pour Voir le panier
  const handleViewCart = () => {
    onClose();
    navigate("/cart"); // Redirection vers la page du panier pour voir tous les produits
  };

  const handleImageError = (itemId: number) => {
    setImgErrors(prev => ({ ...prev, [itemId]: true }));
  };

  return (
    <>
      {/* Overlay */}
      {isOpen && (
        <div 
          className="fixed inset-0 bg-black/30 backdrop-blur-sm z-30"
          onClick={onClose}
        ></div>
      )}

      <div
        className={`fixed top-0 right-0 w-full sm:w-96 h-full bg-white shadow-2xl z-40 transform transition-transform duration-300 ease-in-out overflow-hidden ${
          isOpen ? "translate-x-0" : "translate-x-full"
        }`}
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
              <ShoppingCart className="mr-2" size={22} />
              Mon Panier
            </h2>
            <button
              onClick={onClose}
              className="text-white hover:text-teal-200 transition-colors rounded-full p-1 hover:bg-white/20"
            >
              <X size={24} />
            </button>
          </div>
          <p className="text-teal-100 text-sm mt-1">
            {isAuthenticated ? (
              cartItems.length > 0 
                ? `${cartItems.length} article${cartItems.length > 1 ? 's' : ''} dans votre panier`
                : 'Votre panier est vide'
            ) : (
              'Connectez-vous pour voir votre panier'
            )}
          </p>
        </div>

        <div
          className={`${isAuthenticated && cartItems.length > 0 ? 'h-[calc(100%-19rem)]' : 'h-[calc(100%-12rem)]'} overflow-y-auto bg-gray-50 p-3`}
        >
          {!isAuthenticated ? (
            // Afficher un message de connexion si l'utilisateur n'est pas connecté
            <div className="mx-auto my-8 max-w-xs bg-white rounded-xl shadow-md p-8 text-center">
              <div className="mb-4 w-16 h-16 bg-teal-50 rounded-full flex items-center justify-center mx-auto">
                <ShoppingCart className="text-teal-500" size={32} />
              </div>
              <h3 className="text-lg font-semibold mb-2">Connectez-vous</h3>
              <p className="text-gray-500 text-sm mb-5">
                Veuillez vous connecter pour accéder à votre panier et finaliser vos achats
              </p>
              <button
                onClick={handleSignInRedirect}
                className="px-6 py-2.5 bg-teal-500 text-white rounded-full text-sm font-medium hover:bg-teal-600 transition-colors shadow-sm hover:shadow"
              >
                Se connecter
              </button>
            </div>
          ) : cartItems.length > 0 ? (
            <div className="space-y-3">
              {cartItems.map((item) => (
                <div 
                  key={item.id} 
                  className="bg-white rounded-xl shadow-sm relative overflow-hidden"
                >
                  {isUpdating === item.id && (
                    <div className="absolute inset-0 bg-white/80 flex items-center justify-center z-10">
                      <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-teal-500"></div>
                    </div>
                  )}
                  
                  <div className="p-3 flex">
                    {/* Product image */}
                    <div 
                      className="mr-3 w-20 h-20 rounded-lg overflow-hidden flex-shrink-0 bg-gray-100 cursor-pointer"
                      onClick={() => navigate(`/product/${item.id}`)}
                    >
                      {item.imageUrl && !imgErrors[item.id] ? (
                        <img
                          src={normalizeImageUrl(item.imageUrl)}
                          alt={item.nomProduit}
                          className="w-full h-full object-cover"
                          onError={() => handleImageError(item.id)}
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center bg-gray-100">
                          <ShoppingCart className="text-gray-400" size={24} />
                        </div>
                      )}
                    </div>
                    
                    {/* Product details */}
                    <div className="flex-1 min-w-0">
                      <div 
                        className="flex justify-between items-start cursor-pointer"
                        onClick={() => navigate(`/product/${item.id}`)}
                      >
                        <h3 className="font-medium text-gray-900 truncate pr-2">
                          {item.nomProduit}
                        </h3>
                        <div className="flex-shrink-0">
                          <span className="font-bold text-teal-600">
                            {(item.prix * item.quantite).toFixed(2)} €
                          </span>
                        </div>
                      </div>
                      
                      {item.categorie && (
                        <div className="mt-1">
                          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-teal-50 text-teal-700">
                            {item.categorie}
                          </span>
                        </div>
                      )}
                      
                      <div className="flex justify-between items-center mt-3">
                        <div className="flex items-center bg-gray-50 rounded-lg border border-gray-200">
                          <button 
                            className="text-gray-500 hover:text-gray-700 h-8 w-8 flex items-center justify-center rounded-l-lg hover:bg-gray-100 transition-colors"
                            onClick={() => handleUpdateQuantity(item.id, item.quantite - 1)}
                            disabled={item.quantite <= 1 || isUpdating === item.id}
                          >
                            <Minus className="h-4 w-4" />
                          </button>
                          <span className="w-10 text-center text-sm font-medium">
                            {item.quantite}
                          </span>
                          <button 
                            className="text-gray-500 hover:text-gray-700 h-8 w-8 flex items-center justify-center rounded-r-lg hover:bg-gray-100 transition-colors"
                            onClick={() => handleUpdateQuantity(item.id, item.quantite + 1)}
                            disabled={isUpdating === item.id}
                          >
                            <Plus className="h-4 w-4" />
                          </button>
                        </div>
                        
                        <button 
                          className="text-gray-400 hover:text-red-500 transition-colors p-1.5 rounded-full hover:bg-red-50"
                          onClick={() => handleRemoveItem(item.id)}
                          disabled={isRemoving === item.id}
                        >
                          {isRemoving === item.id ? (
                            <div className="animate-spin h-4 w-4 border-t-2 border-red-500 rounded-full" />
                          ) : (
                            <Trash2 className="h-4 w-4" />
                          )}
                        </button>
                      </div>
                    </div>
                  </div>
                  
                  {/* Prix unitaire en bas */}
                  <div className="px-3 pb-2 text-xs text-gray-500">
                    Prix unitaire: {item.prix.toFixed(2)} €
                  </div>
                </div>
              ))}
            </div>
          ) : (
            /* Empty state if no items in cart */
            <div className="mx-auto my-8 max-w-xs bg-white rounded-xl shadow-md p-8 text-center">
              <div className="mb-4 w-16 h-16 bg-teal-50 rounded-full flex items-center justify-center mx-auto">
                <ShoppingCart className="text-teal-500" size={32} />
              </div>
              <h3 className="text-lg font-semibold mb-2">Votre panier est vide</h3>
              <p className="text-gray-500 text-sm mb-5">
                Ajoutez des articles à votre panier pour commencer vos achats
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
          )}
        </div>

        {isAuthenticated && cartItems.length > 0 && (
          <div className="absolute bottom-0 left-0 w-full bg-white border-t">
            {/* Résumé du panier */}
            <div className="p-4">
              <div className="flex justify-between mb-1.5">
                <span className="text-gray-600">Sous-total:</span>
                <span className="font-semibold">{subtotal.toFixed(2)} €</span>
              </div>
              <div className="flex justify-between pt-2 border-t mt-2">
                <span className="font-semibold text-lg">Total:</span>
                <span className="font-bold text-lg text-teal-600">{subtotal.toFixed(2)} €</span>
              </div>
            </div>
            
            {/* Boutons d'action */}
            <div className="p-4 pt-0">
              <button
                onClick={handleCheckout}
                className="w-full bg-teal-500 hover:bg-teal-600 text-white py-3 rounded-lg font-medium shadow-sm transition-colors mb-3"
              >
                Commander
              </button>
              <button
                onClick={handleViewCart}
                className="w-full flex items-center justify-center text-teal-600 hover:text-teal-800 transition-colors"
              >
                Voir tous les produits du panier
                <ChevronRight className="ml-1" size={16} />
              </button>
            </div>
          </div>
        )}
      </div>
    </>
  );
};

export default CartPanel;