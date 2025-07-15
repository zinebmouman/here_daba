import React, { useState } from 'react';
import FavoriteButton from '../FavoriteButton';
import { useNavigate } from 'react-router-dom';
import { auth } from '../../../config/Firebase';
import { 
  addToCart, 
  addToFavorites, 
  normalizeImageUrl,
  Product as ProductType
} from '../../../utils/shopUtils';

interface ProductProps {
  product: ProductType & {
    images: string[];
    sku?: string;
    brand?: string;
    description: string;
    quantite?: number;
  };
}

// Fonction qui vérifie si un produit est en stock
const isProductInStock = (product: any) => {
  // Vérifier si la propriété quantite existe et est supérieure à 0
  return (product && product.quantite && product.quantite > 0);
};

const Product: React.FC<ProductProps> = ({ product }) => {
  const navigate = useNavigate();
  const [activeImageIndex, setActiveImageIndex] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [isZoomed, setIsZoomed] = useState(false);
  const [zoomPosition, setZoomPosition] = useState({ x: 50, y: 50 }); // Position par défaut au centre
  const [isAddingToCart, setIsAddingToCart] = useState(false);
  const [isAddingToFavorites, setIsAddingToFavorites] = useState(false);

  const handleImageError = (event: React.SyntheticEvent<HTMLImageElement>) => {
    event.currentTarget.src = '/api/fichiers/placeholder.png';
  };

  const increaseQuantity = () => setQuantity(prev => prev + 1);
  const decreaseQuantity = () => setQuantity(prev => prev > 1 ? prev - 1 : 1);

  const thumbnails = product.images.slice(0, 3);

  // Vérifier si le produit a une réduction
  const hasDiscount = product.originalPrice && product.originalPrice > product.prix;

  // Gestion du zoom sur l'image
  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!isZoomed) return;
    
    const { left, top, width, height } = e.currentTarget.getBoundingClientRect();
    const x = ((e.clientX - left) / width) * 100;
    const y = ((e.clientY - top) / height) * 100;
    
    setZoomPosition({ x, y });
  };

  // Fonction pour activer/désactiver le zoom lorsqu'on clique sur la loupe
  const toggleZoom = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsZoomed(!isZoomed);
  };

  // Ajouter au panier
  const handleAddToCart = async () => {
    const currentUser = auth.currentUser;
    if (!currentUser) {
      navigate('/sign-in', { state: { redirect: window.location.pathname } });
      return;
    }

    if (isAddingToCart) return;

    try {
      setIsAddingToCart(true);

      const productToAdd = {
        id: product.id,
        nomProduit: product.nomProduit,
        prix: product.prix,
        imageUrl: normalizeImageUrl(product.images[0]),
        categorie: product.categorie
      };

      const added = addToCart(productToAdd, currentUser.uid, quantity);
      
      if (added) {
        alert(`${quantity} ${product.nomProduit} ajouté(s) au panier`);
      }
    } catch (error) {
      console.error('Erreur lors de l\'ajout au panier:', error);
      alert('Impossible d\'ajouter le produit au panier');
    } finally {
      setIsAddingToCart(false);
    }
  };

  // Ajouter aux favoris
  const handleAddToFavorites = async () => {
    const currentUser = auth.currentUser;
    if (!currentUser) {
      navigate('/sign-in', { state: { redirect: window.location.pathname } });
      return;
    }

    if (isAddingToFavorites) return;

    try {
      setIsAddingToFavorites(true);

      const productToAdd = {
        id: product.id,
        name: product.nomProduit,
        price: product.prix,
        imageUrl: normalizeImageUrl(product.images[0]),
        category: product.categorie
      };

      const added = await addToFavorites(productToAdd, currentUser.uid);
      
      if (added) {
        alert(`${product.nomProduit} ajouté aux favoris`);
      } else {
        alert(`${product.nomProduit} est déjà dans vos favoris`);
      }
    } catch (error) {
      console.error('Erreur lors de l\'ajout aux favoris:', error);
      alert('Impossible d\'ajouter le produit aux favoris');
    } finally {
      setIsAddingToFavorites(false);
    }
  };

  return (
    <div className="container mx-auto px-4 py-8">
      {/* Breadcrumb */}
      <div className="flex items-center text-sm text-gray-600 mb-8 space-x-3">
        <a href="/" className="hover:text-gray-900">Accueil</a>
        <span className="text-gray-400">&gt;</span>
        <a href="/categories" className="hover:text-gray-900">Catégories</a>
        <span className="text-gray-400">&gt;</span>
        <a href={`/categories/${product.categorie?.toLowerCase().replace(/\s+/g, '-')}`} className="hover:text-gray-900">
          {product.categorie}
        </a>
        <span className="text-gray-400">&gt;</span>
        <span className="text-gray-900">{product.nomProduit}</span>
      </div>

      <div className="flex flex-col md:flex-row gap-8">
        {/* Colonne des miniatures */}
        <div className="flex flex-col gap-4">
          {thumbnails.map((image, index) => (
            <div 
              key={index}
              className={`w-20 h-20 cursor-pointer rounded-xl border-2 overflow-hidden ${
                index === activeImageIndex ? 'border-teal-400' : 'border-gray-200'
              }`}
              onClick={() => setActiveImageIndex(index)}
            >
              <img
                src={image}
                alt={`${product.nomProduit} view ${index + 1}`}
                className="w-full h-full object-cover"
                onError={handleImageError}
              />
            </div>
          ))}
        </div>

        {/* Image principale - Taille réduite */}
        <div className="flex-1 max-w-md">
          <div className="relative bg-gray-100 rounded-2xl p-6">
            {hasDiscount && (
              <div className="absolute top-4 right-4 bg-red-500 text-white px-4 py-1 rounded-full text-sm font-bold">
                {`-${Math.round(((product.originalPrice - product.prix) / product.originalPrice) * 100)}%`}
              </div>
            )}
            <div 
              className="relative"
              onMouseMove={handleMouseMove}
            >
              <img
                src={product.images[activeImageIndex]}
                alt={product.nomProduit}
                className="w-full h-64 object-contain mx-auto"
                onError={handleImageError}
                style={{
                  transform: isZoomed ? 'scale(2)' : 'scale(1)',
                  transformOrigin: `${zoomPosition.x}% ${zoomPosition.y}%`,
                  transition: isZoomed ? 'none' : 'transform 0.3s ease-out'
                }}
              />
              <button 
                className="absolute bottom-4 right-4 bg-white p-3 rounded-full shadow-lg hover:bg-gray-100"
                onClick={toggleZoom}
                aria-label={isZoomed ? "Désactiver le zoom" : "Activer le zoom"}
              >
                <svg className="w-6 h-6 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </button>
            </div>
          </div>
        </div>

        {/* Informations produit */}
        <div className="flex-1 max-w-md">
          <h1 className="text-3xl font-bold mb-4">{product.nomProduit}</h1>
          
          <div className="bg-yellow-100 text-yellow-800 px-4 py-2 rounded-lg inline-block mb-4 font-medium">
            {product.categorie}
          </div>

          <div className="flex items-center mb-6">
            {hasDiscount && (
              <span className="text-gray-400 line-through text-2xl mr-4">
                {product.originalPrice?.toFixed(2)} €
              </span>
            )}
            <span className="text-3xl font-bold">{product.prix.toFixed(2)} €</span>
          </div>

          <p className="text-gray-500 mb-6">SKU: {product.sku || '123456789'}</p>

          {/* La section Description a été supprimée */}

          <div className="flex items-center gap-4">
            <button 
              onClick={decreaseQuantity}
              className="w-10 h-10 flex items-center justify-center border border-gray-300 rounded-full text-xl hover:bg-gray-100"
            >
              &lt;
            </button>
            <span className="text-xl font-medium w-10 text-center">{quantity}</span>
            <button 
              onClick={increaseQuantity}
              className="w-10 h-10 flex items-center justify-center border border-gray-300 rounded-full text-xl hover:bg-gray-100"
            >
              &gt;
            </button>
            
            <button 
              onClick={handleAddToCart}
              disabled={isAddingToCart}
              className={`flex-1 text-white py-3 px-6 rounded-full text-base font-medium transition-colors ${
                isAddingToCart 
                  ? 'bg-gray-400 cursor-not-allowed' 
                  : 'bg-teal-500 hover:bg-teal-600'
              }`}
            >
              {isAddingToCart ? 'Ajout en cours...' : 'Ajouter au Panier'}
            </button>
                        
            <button 
              className="w-10 h-10 flex items-center justify-center border border-gray-300 rounded-full hover:bg-gray-100"
              onClick={handleAddToFavorites}
              disabled={isAddingToFavorites}
            >
              <FavoriteButton 
                productId={product.id} 
                size={20}
                productDetails={{
                  name: product.nomProduit,
                  price: product.prix,
                  imageUrl: product.images[0],
                  category: product.categorie
                }}
                onToggleFavorite={(isFavorite) => {
                  console.log(`Favori mis à jour: ${isFavorite}`);
                }}
              />
            </button>
          </div>

          {/* Informations supplémentaires - Vérification du stock avec fonction dédiée */}
          <div className="mt-6 border-t pt-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <h4 className="font-semibold text-gray-700">Disponibilité</h4>
                <p className={`${isProductInStock(product) ? 'text-green-600' : 'text-red-600'}`}>
                  {isProductInStock(product) ? 'En stock' : 'Rupture de stock'}
                </p>
              </div>
              <div>
                <h4 className="font-semibold text-gray-700">Catégorie</h4>
                <p>{product.categorie || 'Non spécifiée'}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Product;