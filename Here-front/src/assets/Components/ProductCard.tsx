import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Heart, ShoppingCart, Star } from 'lucide-react';
import { auth } from '../../config/Firebase';
import { 
  addToCart, 
  addToFavorites, 
  normalizeImageUrl,
  isProductInFavorites
} from '../../utils/shopUtils';

interface ProductCardProps {
  id?: number;
  name: string;
  originalPrice: number;
  currentPrice: number;
  discount?: string;
  rating: number;
  reviewCount: number;
  soldCount?: number;
  imageUrl: string;
  category?: string;
}

const ProductCard: React.FC<ProductCardProps> = ({
  id,
  name = '',
  originalPrice,
  currentPrice,
  discount,
  rating,
  reviewCount,
  soldCount = 0,
  imageUrl,
  category
}) => {
  const navigate = useNavigate();
  const [isAddingToCart, setIsAddingToCart] = useState(false);
  const [isAddingToFavorites, setIsAddingToFavorites] = useState(false);
  const [isFavorite, setIsFavorite] = useState(false);
  const [currentUser, setCurrentUser] = useState<any>(null);

  // Vérifier si l'utilisateur est connecté
  useEffect(() => {
    const unsubscribe = auth.onAuthStateChanged(user => {
      setCurrentUser(user);
      if (user && id) {
        checkIfFavorite(user.uid);
      }
    });
    return () => unsubscribe();
  }, [id]);

  // Vérifier si le produit est déjà dans les favoris
  const checkIfFavorite = async (userId: string) => {
    if (!id) return;
    
    try {
      // Si la fonction isProductInFavorites n'existe pas, on peut implémenter une version simplifiée ici
      // Cette fonction devrait vérifier dans le localStorage ou faire une requête API
      const FAVORITES_STORAGE_KEY = 'HERE_FAVORITES';
      const userFavoritesKey = `${FAVORITES_STORAGE_KEY}_${userId}`;
      const favoritesJson = localStorage.getItem(userFavoritesKey);
      
      if (favoritesJson) {
        const favorites = JSON.parse(favoritesJson);
        const isFav = favorites.some((item: any) => 
          (item.idProduit === id) || (item.id === id)
        );
        setIsFavorite(isFav);
      }
      
      // Si vous avez une API pour vérifier les favoris, utilisez-la en plus du stockage local
      try {
        const response = await fetch(`/api/favoris/check/${id}/user/${userId}`);
        if (response.ok) {
          const data = await response.json();
          if (data.isFavorite) {
            setIsFavorite(true);
          }
        }
      } catch (apiError) {
        console.log("API non disponible pour vérifier les favoris");
      }
    } catch (error) {
      console.error("Erreur lors de la vérification des favoris:", error);
    }
  };

  // Générer un chemin sécurisé pour le produit
  const productPath = id 
    ? `/product/${id}`
    : `/product/${(name || 'product').toLowerCase().replace(/\s+/g, '-')}`;
  
  // Fonction pour corriger les URLs des images
  const fixImageUrl = (url: string) => {
    if (!url) return '/placeholder-image.jpg';
    
    // Si l'URL ne commence pas par http ou /, ajouter /
    if (!url.startsWith('http') && !url.startsWith('/')) {
      return '/' + url;
    }
    
    // Si l'URL est déjà complète, la renvoyer telle quelle
    return url;
  };
  
  // Gestionnaire pour ajouter au panier
  const handleAddToCart = async (e: React.MouseEvent) => {
    e.preventDefault(); // Empêcher la navigation
    e.stopPropagation(); // Arrêter la propagation
    
    if (!currentUser) {
      navigate('/sign-in', { state: { redirect: window.location.pathname } });
      return;
    }

    if (isAddingToCart) return;

    try {
      setIsAddingToCart(true);

      const productToAdd = {
        id: id,
        nomProduit: name,
        prix: currentPrice,
        imageUrl: normalizeImageUrl(imageUrl),
        categorie: category
      };

      const added = addToCart(productToAdd, currentUser.uid);
      
      if (added) {
        alert(`${name} ajouté au panier`);
      }
    } catch (error) {
      console.error('Erreur lors de l\'ajout au panier:', error);
      alert('Impossible d\'ajouter le produit au panier');
    } finally {
      setIsAddingToCart(false);
    }
  };

  // Gestionnaire pour ajouter aux favoris
  const handleAddToFavorites = async (e: React.MouseEvent) => {
    e.preventDefault(); // Empêcher la navigation
    e.stopPropagation(); // Arrêter la propagation
    
    if (!currentUser) {
      navigate('/sign-in', { state: { redirect: window.location.pathname } });
      return;
    }

    if (isAddingToFavorites) return;

    try {
      setIsAddingToFavorites(true);

      const productToAdd = {
        id: id,
        name: name,
        price: currentPrice,
        imageUrl: normalizeImageUrl(imageUrl),
        category: category
      };

      // Si déjà dans les favoris, ne rien faire (ou on pourrait implémenter une suppression)
      if (isFavorite) {
        alert(`${name} est déjà dans vos favoris`);
        setIsAddingToFavorites(false);
        return;
      }

      const added = await addToFavorites(productToAdd, currentUser.uid);
      
      if (added) {
        setIsFavorite(true); // Mettre à jour l'état local
        alert(`${name} ajouté aux favoris`);
      } else {
        alert(`${name} est déjà dans vos favoris`);
        setIsFavorite(true); // Au cas où l'état n'est pas à jour
      }
    } catch (error) {
      console.error('Erreur lors de l\'ajout aux favoris:', error);
      alert('Impossible d\'ajouter le produit aux favoris');
    } finally {
      setIsAddingToFavorites(false);
    }
  };
  
  return (
    <div className="bg-white rounded-lg overflow-hidden shadow-sm hover:shadow-md transition-all h-full flex flex-col">
      {/* Image container avec lien */}
      <Link to={productPath} className="block relative h-48 overflow-hidden bg-gray-100">
        <img 
          src={fixImageUrl(imageUrl)} 
          alt={name || 'Product'}
          className="w-full h-full object-cover"
          onError={(e) => {
            // Fallback en cas d'erreur de chargement d'image
            e.currentTarget.src = '/placeholder-image.jpg';
            e.currentTarget.onerror = null; // Éviter les boucles infinies
          }}
        />
        
        {/* Badge de réduction */}
        {discount && (
          <div className="absolute top-2 left-2 bg-red-500 text-white px-2 py-1 text-xs font-semibold rounded">
            -{discount}
          </div>
        )}
      </Link>
      
      {/* Détails du produit */}
      <div className="p-3 flex-grow flex flex-col justify-between">
        {/* Nom du produit avec lien */}
        <Link to={productPath} className="block">
          <h3 className="font-medium text-sm mb-1 line-clamp-2 h-10">
            {name || "Produit sans nom"}
          </h3>
        </Link>
        
        {/* Prix et notation */}
        <div className="mt-auto">
          <Link to={productPath} className="block">
            <div className="flex items-baseline space-x-2 mb-1">
              <span className="font-bold text-lg">{(currentPrice || 0).toFixed(2)} DH</span>
              {originalPrice > currentPrice && (
                <span className="text-gray-500 text-sm line-through">{(originalPrice || 0).toFixed(2)} DH</span>
              )}
            </div>
            
            {/* Évaluation et ventes */}
            <div className="flex items-center text-xs text-gray-500 mb-3">
              <div className="flex items-center mr-2">
                <Star size={14} className="text-yellow-400 fill-yellow-400 mr-1" />
                <span>{(rating || 0).toFixed(1)}</span>
              </div>
              <span className="mx-1">•</span>
              <span>{reviewCount || 0} avis</span>
              {soldCount > 0 && (
                <>
                  <span className="mx-1">•</span>
                  <span>{soldCount} vendus</span>
                </>
              )}
            </div>
          </Link>
          
          {/* Boutons d'action (uniquement en bas) */}
          <div className="flex justify-between mt-1">
            <button 
              onClick={handleAddToFavorites}
              disabled={isAddingToFavorites}
              className="p-2 rounded-full hover:bg-gray-100 transition-colors"
              aria-label="Ajouter aux favoris"
            >
              <Heart 
                size={18} 
                className={`${isFavorite || isAddingToFavorites ? 'text-red-500 fill-red-500' : 'text-gray-500'}`} 
              />
            </button>
            
            <button 
              onClick={handleAddToCart}
              disabled={isAddingToCart}
              className="bg-emerald-500 hover:bg-emerald-600 text-white rounded-full p-2 transition-colors"
              aria-label="Ajouter au panier"
            >
              <ShoppingCart 
                size={18} 
                className={isAddingToCart ? 'animate-pulse' : ''} 
              />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProductCard;