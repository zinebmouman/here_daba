import React, { useState, useRef, useEffect } from 'react';
import { ChevronRight, ChevronLeft } from 'lucide-react';
import ProductCard2 from './ProductCard2';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { auth } from '../../config/Firebase';
import { addToCart, addToFavorites } from '../../utils/shopUtils';

interface Product {
  id: number;
  name: string;
  originalPrice: number;
  currentPrice: number;
  discount?: string;
  rating: number;
  reviewCount: number;
  soldCount: number;
  imageUrl: string;
  idCategorie?: string;
}

interface Category {
  id: string;
  name: string;
}

interface ExploreInterestsProps {
  userId?: string;
}

const ExploreInterests: React.FC<ExploreInterestsProps> = ({ userId }) => {
  const navigate = useNavigate();
  const [activeCategory, setActiveCategory] = useState('recommended');
  const [showAll, setShowAll] = useState(false);
  const categoriesRef = useRef<HTMLDivElement>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [allProducts, setAllProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentUser, setCurrentUser] = useState<any>(null);
  
  useEffect(() => {
    // Surveiller les changements d'état d'authentification
    const unsubscribe = auth.onAuthStateChanged(user => {
      setCurrentUser(user);
    });

    return () => unsubscribe();
  }, []);
  
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        
        // Récupérer les catégories
        const categoriesResponse = await axios.get('/api/categories');
        const categoriesData = categoriesResponse.data.map((cat: any) => ({
          id: cat.idCategorie || cat.id,
          name: cat.nom || cat.name
        }));
        
        // Ajouter la catégorie "Recommandé" en première position
        setCategories([
          { id: 'recommended', name: 'Recommandé' },
          ...categoriesData
        ]);
        
        // Récupérer les produits
        const productsResponse = await axios.get('/api/produits');
        console.log("Données reçues de l'API produits:", productsResponse.data);
        
        // Traiter les produits et récupérer leurs images
        const productsWithDetails = await Promise.all(
          productsResponse.data.map(async (product: any) => {
            try {
              // Vérifier si le produit existe et a un ID
              if (!product || !product.id) {
                return null;
              }

              // Récupérer les images de chaque produit
              const imagesResponse = await axios.get(`/api/produits/${product.id}/images`);
              const images = imagesResponse.data;
              
              // Filtrer si aucune image n'est trouvée
              if (!images || images.length === 0) {
                return null;
              }

              // Prendre l'image principale ou la première
              const mainImage = images.find((img: any) => img.imagePrincipale) || images[0];
              
              // Vérifier que l'image a bien une URL valide
              if (!mainImage || !mainImage.url) {
                return null;
              }

              // Calculer la réduction en pourcentage si disponible
              let discountPercentage;
              if (product.prixOriginal && product.prix && product.prixOriginal > product.prix) {
                const reduction = Math.round(((product.prixOriginal - product.prix) / product.prixOriginal) * 100);
                discountPercentage = `${reduction}%`;
              } else if (product.reduction) {
                discountPercentage = `${product.reduction}%`;
              }

              console.log("Nom du produit identifié:", product.nomProduit || product.nom || product.name || "Produit sans nom");

              return {
                id: product.id,
                name: product.nomProduit || product.nom || product.name || "Produit sans nom", // Priorité à nomProduit
                originalPrice: product.prixOriginal || product.prix || product.price,
                currentPrice: product.prix || product.price,
                discount: discountPercentage,
                rating: product.note || product.rating || 0,
                reviewCount: product.nombreAvis || product.reviewCount || 0,
                soldCount: product.soldCount || 0,
                imageUrl: mainImage.url,
                idCategorie: product.idCategorie
              };
            } catch (error) {
              console.error(`Erreur lors de la récupération des images pour le produit ${product.id}:`, error);
              return null;
            }
          })
        );
        
        // Filtrer les produits valides (non null et avec image)
        const validProducts = productsWithDetails
          .filter(product => 
            product !== null && 
            product.imageUrl && 
            product.imageUrl.trim() !== ''
          ) as Product[];
        
        setAllProducts(validProducts);
        setProducts(validProducts);
        setLoading(false);
      } catch (error) {
        console.error("Erreur lors de la récupération des données:", error);
        setLoading(false);
      }
    };
    
    fetchData();
  }, []);
  
  // Effet pour filtrer les produits quand la catégorie change
  useEffect(() => {
    if (activeCategory === 'recommended') {
      // Afficher tous les produits pour "Recommandé"
      setProducts(allProducts);
    } else {
      // Filtrer les produits par catégorie
      const filteredProducts = allProducts.filter(
        product => product.idCategorie === activeCategory
      );
      setProducts(filteredProducts);
    }
    
    // Réinitialiser l'affichage à 16 produits max
    setShowAll(false);
  }, [activeCategory, allProducts]);
  
  // Navigation pour categories slider
  const scrollCategories = (direction: 'left' | 'right') => {
    if (categoriesRef.current) {
      const scrollAmount = direction === 'left' ? -200 : 200;
      categoriesRef.current.scrollBy({
        left: scrollAmount,
        behavior: 'smooth'
      });
    }
  };
  
  // Display only 16 products initially, show all when "View All" is clicked
  const displayedProducts = showAll ? products : products.slice(0, 16);

  // Fonction pour corriger les URLs des images
  const fixImageUrl = (url: string) => {
    // Si l'URL ne commence pas par http ou /, ajouter /
    if (!url.startsWith('http') && !url.startsWith('/')) {
      return '/' + url;
    }
    
    // Si l'URL est déjà complète, la renvoyer telle quelle
    return url;
  };

  // Handler for add to cart
  const handleAddToCart = (productId: number) => (e: React.MouseEvent) => {
    e.preventDefault(); // Empêcher la navigation
    e.stopPropagation(); // Arrêter la propagation
    
    // Vérifier si l'utilisateur est connecté
    if (!currentUser) {
      // Rediriger vers la page de connexion
      navigate('/sign-in', { state: { redirect: '/' } });
      return;
    }
    
    console.log("Ajout au panier du produit", productId);
    
    // Trouver le produit dans la liste
    const product = allProducts.find(p => p.id === productId);
    
    if (!product) {
      console.error(`Produit avec l'id ${productId} non trouvé`);
      return;
    }
    
    // Créer l'objet à ajouter au panier
    const productToAdd = {
      id: product.id,
      nomProduit: product.name,
      prix: product.currentPrice,
      imageUrl: fixImageUrl(product.imageUrl),
      categorie: categories.find(c => c.id === product.idCategorie)?.name
    };
    
    // Ajouter au panier
    const added = addToCart(productToAdd, currentUser.uid);
    
    if (added) {
      alert(`${product.name} ajouté au panier`);
    }
  };

  // Handler for add to favorites
  const handleAddToFavorite = (productId: number) => async (e: React.MouseEvent) => {
    e.preventDefault(); // Empêcher la navigation
    e.stopPropagation(); // Arrêter la propagation
    
    // Vérifier si l'utilisateur est connecté
    if (!currentUser) {
      // Rediriger vers la page de connexion
      navigate('/sign-in', { state: { redirect: '/' } });
      return;
    }
    
    console.log("Ajout aux favoris du produit", productId);
    
    // Trouver le produit dans la liste
    const product = allProducts.find(p => p.id === productId);
    
    if (!product) {
      console.error(`Produit avec l'id ${productId} non trouvé`);
      return;
    }
    
    // Créer l'objet à ajouter aux favoris
    const productToAdd = {
      id: product.id,
      name: product.name,
      price: product.currentPrice,
      imageUrl: fixImageUrl(product.imageUrl),
      category: categories.find(c => c.id === product.idCategorie)?.name
    };
    
    // Ajouter aux favoris
    const added = await addToFavorites(productToAdd, currentUser.uid);
    
    if (added) {
      alert(`${product.name} ajouté aux favoris`);
    } else {
      alert(`${product.name} est déjà dans vos favoris`);
    }
  };
  
  if (loading) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-6 all">
        <h2 className="text-2xl font-bold mb-4">Explore your interests</h2>
        <div className="flex justify-center items-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-emerald-500"></div>
        </div>
      </div>
    );
  }
  
  return (
    <div className="max-w-6xl mx-auto px-4 py-6 all">
      <h2 className="text-2xl font-bold mb-4">Explore your interests</h2>
      
      {/* Categories scrolling list with navigation buttons */}
      <div className="relative mb-6">
        <div className="absolute left-0 top-1/2 -translate-y-1/2 z-10">
          <button 
            onClick={() => scrollCategories('left')}
            className="p-1 bg-white rounded-full border border-gray-300 shadow-sm hover:bg-gray-100"
          >
            <ChevronLeft size={20} />
          </button>
        </div>
        
        <div
          ref={categoriesRef}
          className="flex overflow-x-auto py-1 px-8 no-scrollbar"
          style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
        >
          <div className="flex space-x-2">
            {categories.map(category => (
              <button
                key={category.id}
                className={`px-4 py-2 rounded-full border whitespace-nowrap text-sm ${
                  activeCategory === category.id 
                    ? 'font-semibold border-gray-900' 
                    : 'bg-white text-gray-700 border-gray-300'
                }`}
                onClick={() => setActiveCategory(category.id)}
              >
                {category.name}
              </button>
            ))}
          </div>
        </div>
        
        <div className="absolute right-0 top-1/2 -translate-y-1/2 z-10">
          <button 
            onClick={() => scrollCategories('right')}
            className="p-1 bg-white rounded-full border border-gray-300 shadow-sm hover:bg-gray-100"
          >
            <ChevronRight size={20} />
          </button>
        </div>
      </div>
      
      {/* Products grid or message when no products */}
      {products.length > 0 ? (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
          {displayedProducts.map((product) => (
            <div key={product.id} className="hover:shadow-md transition-shadow rounded-lg overflow-hidden">
              <ProductCard2 
                id={product.id}
                name={product.name}
                originalPrice={product.originalPrice}
                currentPrice={product.currentPrice}
                discount={product.discount}
                rating={product.rating}
                reviewCount={product.reviewCount}
                soldCount={product.soldCount}
                imageUrl={product.imageUrl}
                category={categories.find(c => c.id === product.idCategorie)?.name}
                onAddToCart={handleAddToCart(product.id)}
                onAddToFavorite={handleAddToFavorite(product.id)}
              />
            </div>
          ))}
        </div>
      ) : (
        <div className="text-center text-gray-500 py-8">
          Aucun produit trouvé dans cette catégorie.
        </div>
      )}
      
      {/* View all button */}
      {products.length > 16 && (
        <div className="flex justify-center mt-6">
          {!showAll ? (
            <button 
              className="flex items-center space-x-2 bg-emerald-500 text-white px-4 py-2 rounded-full hover:bg-emerald-600 transition-colors"
              onClick={() => setShowAll(true)}
            >
              <span>View All</span>
              <ChevronRight size={16} />
            </button>
          ) : (
            <button 
              className="flex items-center space-x-2 bg-gray-200 text-gray-700 px-4 py-2 rounded-full hover:bg-gray-300 transition-colors"
              onClick={() => setShowAll(false)}
            >
              <span>Show Less</span>
            </button>
          )}
        </div>
      )}
    </div>
  );
};

export default ExploreInterests;