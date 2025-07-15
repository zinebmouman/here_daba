import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { auth } from '../../config/Firebase'; // Ajustez le chemin selon votre structure
import { Trash2, Plus, Minus, ShoppingBag, ArrowLeft } from 'lucide-react';

interface CartItem {
  id: number;
  nomProduit: string;
  prix: number;
  imageUrl?: string;
  quantite: number;
  categorie?: string;
}

const CART_STORAGE_KEY = 'HERE_SHOPPING_CART';

const Cart: React.FC = () => {
  const navigate = useNavigate();
  const [cartItems, setCartItems] = useState<CartItem[]>([]);
  const [subtotal, setSubtotal] = useState<number>(0);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [userId, setUserId] = useState<string | null>(null);

  useEffect(() => {
    // Vérifier si l'utilisateur est connecté
    const unsubscribe = auth.onAuthStateChanged((user) => {
      if (user) {
        setUserId(user.uid);
        loadCartItems(user.uid);
      } else {
        // Rediriger vers la page de connexion si l'utilisateur n'est pas connecté
        navigate('/sign-in', { state: { redirect: '/cart' } });
      }
      setIsLoading(false);
    });

    return () => unsubscribe();
  }, [navigate]);

  const loadCartItems = (uid: string) => {
    const userCartKey = `${CART_STORAGE_KEY}_${uid}`;
    const cartJson = localStorage.getItem(userCartKey);
    
    if (cartJson) {
      try {
        const items = JSON.parse(cartJson);
        setCartItems(items);
        
        // Calculer le sous-total
        const total = items.reduce((sum: number, item: CartItem) => 
          sum + (item.prix * item.quantite), 0);
        setSubtotal(total);
      } catch (e) {
        console.error("Erreur lors du chargement du panier:", e);
        setCartItems([]);
        setSubtotal(0);
      }
    } else {
      setCartItems([]);
      setSubtotal(0);
    }
  };

  const updateItemQuantity = (itemId: number, newQuantity: number) => {
    if (!userId || newQuantity < 1) return;
    
    const updatedItems = cartItems.map(item => 
      item.id === itemId ? { ...item, quantite: newQuantity } : item
    );
    
    setCartItems(updatedItems);
    
    // Utiliser un identifiant unique pour chaque utilisateur
    const userCartKey = `${CART_STORAGE_KEY}_${userId}`;
    
    // Mettre à jour le localStorage
    localStorage.setItem(userCartKey, JSON.stringify(updatedItems));
    
    // Recalculer le sous-total
    const total = updatedItems.reduce((sum, item) => sum + (item.prix * item.quantite), 0);
    setSubtotal(total);
    
    // Mettre à jour le compteur dans la navbar
    const cartUpdateEvent = new CustomEvent('cartUpdated', {
      detail: { userId: userId }
    });
    window.dispatchEvent(cartUpdateEvent);
  };

  const removeItem = (itemId: number) => {
    if (!userId) return;
    
    const updatedItems = cartItems.filter(item => item.id !== itemId);
    setCartItems(updatedItems);
    
    // Utiliser un identifiant unique pour chaque utilisateur
    const userCartKey = `${CART_STORAGE_KEY}_${userId}`;
    
    // Mettre à jour le localStorage
    localStorage.setItem(userCartKey, JSON.stringify(updatedItems));
    
    // Recalculer le sous-total
    const total = updatedItems.reduce((sum, item) => sum + (item.prix * item.quantite), 0);
    setSubtotal(total);
    
    // Mettre à jour le compteur dans la navbar
    const cartUpdateEvent = new CustomEvent('cartUpdated', {
      detail: { userId: userId }
    });
    window.dispatchEvent(cartUpdateEvent);
  };

  const handleCheckout = () => {
    navigate('/checkout');
  };

  const continueShopping = () => {
    navigate('/');
  };

  if (isLoading) {
    return (
      <div className="container mx-auto p-8 flex justify-center items-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-teal-500"></div>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4 md:p-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl md:text-3xl font-bold">Mon Panier</h1>
        <button
          onClick={continueShopping}
          className="text-teal-600 flex items-center hover:text-teal-700"
        >
          <ArrowLeft className="h-4 w-4 mr-1" />
          Continuer les achats
        </button>
      </div>

      {cartItems.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-lg shadow-sm">
          <ShoppingBag className="h-16 w-16 mx-auto text-gray-300 mb-4" />
          <h2 className="text-xl font-semibold mb-2">Votre panier est vide</h2>
          <p className="text-gray-500 mb-6">Découvrez notre catalogue et ajoutez des produits à votre panier</p>
          <button
            onClick={continueShopping}
            className="px-6 py-3 bg-teal-500 text-white rounded-full hover:bg-teal-600 transition-colors"
          >
            Explorer les produits
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2">
            <div className="bg-white rounded-lg shadow-sm overflow-hidden">
              <div className="p-6 border-b">
                <h2 className="text-xl font-semibold">Articles dans votre panier</h2>
                <p className="text-gray-500 text-sm">{cartItems.length} article(s)</p>
              </div>

              <div className="divide-y">
                {cartItems.map((item) => (
                  <div key={item.id} className="p-6 flex flex-col md:flex-row items-start md:items-center">
                    <div className="bg-gray-100 h-24 w-24 rounded-md flex items-center justify-center mr-4 mb-4 md:mb-0 overflow-hidden">
                      {/* Toujours montrer l'icône ShoppingBag car les images ne s'affichent pas correctement */}
                      <ShoppingBag className="text-gray-400 h-10 w-10" />
                    </div>
                    
                    <div className="flex-1 min-w-0">
                      <h3 className="text-lg font-medium">{item.nomProduit}</h3>
                      {item.categorie && (
                        <p className="text-gray-500 text-sm mb-1">{item.categorie}</p>
                      )}
                      <p className="text-teal-600 font-semibold text-lg">{item.prix.toFixed(2)} €</p>
                    </div>
                    
                    <div className="flex flex-col items-end mt-4 md:mt-0">
                      <div className="flex items-center mb-3">
                        <button 
                          className="text-gray-500 hover:text-gray-700 h-8 w-8 flex items-center justify-center border border-gray-200 rounded-l-md"
                          onClick={() => updateItemQuantity(item.id, item.quantite - 1)}
                        >
                          <Minus className="h-4 w-4" />
                        </button>
                        <span className="mx-1 px-4 border-t border-b border-gray-200 h-8 flex items-center">
                          {item.quantite}
                        </span>
                        <button 
                          className="text-gray-500 hover:text-gray-700 h-8 w-8 flex items-center justify-center border border-gray-200 rounded-r-md"
                          onClick={() => updateItemQuantity(item.id, item.quantite + 1)}
                        >
                          <Plus className="h-4 w-4" />
                        </button>
                      </div>
                      <button 
                        className="text-red-500 hover:text-red-700 text-sm flex items-center"
                        onClick={() => removeItem(item.id)}
                      >
                        <Trash2 className="h-4 w-4 mr-1" />
                        Supprimer
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
          
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg shadow-sm p-6 sticky top-8">
              <h2 className="text-xl font-semibold mb-4">Résumé de la commande</h2>
              
              <div className="space-y-3 mb-6">
                <div className="flex justify-between text-gray-600">
                  <span>Sous-total</span>
                  <span>{subtotal.toFixed(2)} €</span>
                </div>
                <div className="flex justify-between text-gray-600">
                  <span>Frais de livraison</span>
                  <span>Gratuit</span>
                </div>
                <div className="border-t pt-3 mt-3"></div>
                <div className="flex justify-between font-semibold text-lg">
                  <span>Total</span>
                  <span>{subtotal.toFixed(2)} €</span>
                </div>
              </div>
              
              <button
                onClick={handleCheckout}
                className="w-full bg-teal-500 text-white py-3 rounded-full hover:bg-teal-600 transition-colors mb-3"
              >
                Procéder au paiement
              </button>
              
              <button
                onClick={continueShopping}
                className="w-full border border-gray-300 text-gray-700 py-3 rounded-full hover:bg-gray-50 transition-colors"
              >
                Continuer les achats
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Cart;