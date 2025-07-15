import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Heart } from 'lucide-react';
import { auth } from '../../config/Firebase';
import { 
  isInFavorites, 
  addToFavorites, 
  removeFromFavorites 
} from '../../utils/shopUtils';

interface FavoriteButtonProps {
  productId: number | string;
  size?: number;
  productDetails?: {
    name?: string;
    price?: number;
    imageUrl?: string;
    category?: string;
  };
  onToggleFavorite?: (isFavorite: boolean) => void;
  className?: string; // Ajout pour personnalisation
}

const FavoriteButton: React.FC<FavoriteButtonProps> = ({ 
  productId, 
  size = 24, 
  productDetails,
  onToggleFavorite,
  className = ''
}) => {
  const [isFavorite, setIsFavorite] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  // Vérifier l'état initial des favoris
  useEffect(() => {
    const checkFavoriteStatus = () => {
      const favoriteStatus = isInFavorites(productId);
      setIsFavorite(favoriteStatus);
    };

    checkFavoriteStatus();

    // Écouter les événements de mise à jour des favoris
    const handleFavoritesUpdate = () => {
      checkFavoriteStatus();
    };

    window.addEventListener('favoritesUpdated', handleFavoritesUpdate);

    return () => {
      window.removeEventListener('favoritesUpdated', handleFavoritesUpdate);
    };
  }, [productId]);

  const toggleFavorite = async (e: React.MouseEvent) => {
    // Arrêter la propagation de l'événement pour éviter les redirections
    e.preventDefault();
    e.stopPropagation();

    // Vérifier l'authentification
    const currentUser = auth.currentUser;
    if (!currentUser) {
      navigate('/sign-in', { state: { redirect: window.location.pathname } });
      return;
    }

    // Éviter les clics multiples
    if (isLoading) return;

    try {
      setIsLoading(true);

      let success: boolean;
      if (isFavorite) {
        // Supprimer des favoris
        success = await removeFromFavorites(productId, currentUser.uid);
      } else {
        // Ajouter aux favoris
        const productToAdd = productDetails || { id: productId };
        success = await addToFavorites(productToAdd, currentUser.uid);
      }

      if (success) {
        const newFavoriteStatus = !isFavorite;
        setIsFavorite(newFavoriteStatus);

        // Appeler le callback optionnel
        if (onToggleFavorite) {
          onToggleFavorite(newFavoriteStatus);
        }

        // Afficher un message approprié
        if (newFavoriteStatus) {
          alert(`Ajouté aux favoris`);
        } else {
          alert(`Retiré des favoris`);
        }
      }
    } catch (error) {
      console.error('Erreur lors de la gestion des favoris:', error);
      alert('Une erreur est survenue. Veuillez réessayer.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <button 
      onClick={toggleFavorite}
      disabled={isLoading}
      className={`cursor-pointer ${className} ${isLoading ? 'opacity-50' : ''}`}
      title={isFavorite ? "Retirer des favoris" : "Ajouter aux favoris"}
    >
      <Heart 
        size={size} 
        fill={isFavorite ? "#ef4444" : "none"}
        stroke={isFavorite ? "#ef4444" : "currentColor"}
        className={`transition-colors ${
          isFavorite 
            ? 'text-red-500 fill-current' 
            : 'text-gray-500 hover:text-red-500'
        }`}
      />
    </button>
  );
};

export default FavoriteButton;