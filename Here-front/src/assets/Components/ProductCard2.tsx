import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Heart, ShoppingCart, Star } from 'lucide-react';
import { auth } from '../../config/Firebase';

interface ProductCard2Props {
  id?: number;
  name: string;
  originalPrice: number;
  currentPrice: number;
  discount?: string;
  rating: number;
  reviewCount: number;
  soldCount: number;
  imageUrl: string;
  category?: string;
  onAddToCart: (e: React.MouseEvent) => void;
  onAddToFavorite: (e: React.MouseEvent) => void;
}

// Constantes pour le stockage local
const FAVORITES_STORAGE_KEY = 'HERE_FAVORITES';

const ProductCard2: React.FC<ProductCard2Props> = ({
  id,
  name,
  originalPrice,
  currentPrice,
  discount,
  rating,
  reviewCount,
  soldCount,
  imageUrl,
  category,
  onAddToCart,
  onAddToFavorite
}) => {
  const navigate = useNavigate();
  const [isFavorite, setIsFavorite] = useState(false);
  const [isAddingToFavorites, setIsAddingToFavorites] = useState(false);
  const [isAddingToCart, setIsAddingToCart] = useState(false);
  
  // Générer un chemin sécurisé pour le produit
  const productPath = id 
    ? `/product/${id}`
    : `/product/${(name || 'product').toLowerCase().replace(/\s+/g, '-')}`;
  
  // Vérifier si le produit est dans les favoris en utilisant uniquement le localStorage
  useEffect(() => {
    const checkFavoriteStatus = () => {
      try {
        const user = auth.currentUser;
        if (user && id) {
          const userFavoritesKey = `${FAVORITES_STORAGE_KEY}_${user.uid}`;
          const favoritesJson = localStorage.getItem(userFavoritesKey);
          
          if (favoritesJson) {
            const favorites = JSON.parse(favoritesJson);
            const isFav = favorites.some((item: any) => 
              (item.idProduit === id) || (item.id === id) || 
              (item.produitDetails && item.produitDetails.id === id)
            );
            setIsFavorite(isFav);
          }
        }
      } catch (error) {
        console.error("Erreur lors de la vérification des favoris:", error);
      }
    };
    
    checkFavoriteStatus();
    
    // S'abonner aux événements de mise à jour des favoris
    const handleFavoritesUpdate = () => {
      checkFavoriteStatus();
    };
    
    window.addEventListener('favoritesUpdated', handleFavoritesUpdate);
    
    return () => {
      window.removeEventListener('favoritesUpdated', handleFavoritesUpdate);
    };
  }, [id]);
  
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
  
  // Gestionnaire personnalisé pour l'ajout au panier
  const handleAddToCartWithVisualFeedback = (e: React.MouseEvent) => {
    e.preventDefault(); // Empêcher la navigation
    e.stopPropagation(); // Arrêter la propagation
    
    setIsAddingToCart(true);
    
    // Appeler la fonction fournie en prop
    onAddToCart(e);
    
    // Réinitialiser l'état après animation
    setTimeout(() => setIsAddingToCart(false), 500);
  };
  
  // Gestionnaire personnalisé pour l'ajout aux favoris
  const handleAddToFavoriteWithVisualFeedback = (e: React.MouseEvent) => {
    e.preventDefault(); // Empêcher la navigation
    e.stopPropagation(); // Arrêter la propagation
    
    setIsAddingToFavorites(true);
    
    // Appeler la fonction fournie en prop
    onAddToFavorite(e);
    
    // Mettre à jour l'état avant même la réponse de l'API (optimistic update)
    setIsFavorite(true);
    
    // Réinitialiser l'état d'ajout après animation
    setTimeout(() => setIsAddingToFavorites(false), 500);
  };

  // Gestion du clic sur le produit
  const handleProductClick = () => {
    navigate(productPath);
  };

  return (
    <div 
      className="bg-white rounded-lg overflow-hidden shadow-sm hover:shadow-md border border-gray-200 transition-all h-full flex flex-col cursor-pointer"
      onClick={handleProductClick}
    >
      {/* Image container with fixed height */}
      <div className="relative h-48 overflow-hidden bg-gray-100">
        <img 
          src={fixImageUrl(imageUrl)} 
          alt={name}
          className="w-full h-full object-cover transition-transform hover:scale-105 duration-300"
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
      </div>
      
      {/* Détails du produit */}
      <div className="p-3 flex-grow flex flex-col justify-between">
        {/* Catégorie */}
        {category && (
          <div className="text-xs text-gray-500 mb-1">{category}</div>
        )}
        
        {/* Nom du produit limité à 2 lignes */}
        <h3 className="font-medium text-sm mb-1 line-clamp-2 h-10">
          {name || "Produit sans nom"}
        </h3>
        
        {/* Prix et notation */}
        <div className="mt-auto">
          <div className="flex items-baseline space-x-2 mb-1">
            <span className="font-bold text-lg">{currentPrice?.toFixed(2) || 0} DH</span>
            {originalPrice > currentPrice && (
              <span className="text-gray-500 text-sm line-through">{originalPrice?.toFixed(2) || 0} DH</span>
            )}
          </div>
          
          {/* Évaluation et ventes */}
          <div className="flex items-center text-xs text-gray-500 mb-2">
            <div className="flex items-center mr-2">
              <Star size={14} className="text-yellow-400 fill-yellow-400 mr-1" />
              <span>{rating?.toFixed(1) || "0.0"}</span>
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
          
          {/* Boutons d'action */}
          <div className="flex justify-between mt-2">
            <button 
              onClick={handleAddToFavoriteWithVisualFeedback}
              className="p-2 rounded-full hover:bg-gray-100 transition-colors z-10"
              aria-label="Ajouter aux favoris"
            >
              <Heart 
                size={18} 
                className={`${isFavorite || isAddingToFavorites ? 'text-red-500 fill-red-500' : 'text-gray-500'}`} 
              />
            </button>
            
            <button 
              onClick={handleAddToCartWithVisualFeedback}
              className="bg-emerald-500 hover:bg-emerald-600 text-white rounded-full p-2 transition-colors z-10"
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

export default ProductCard2;