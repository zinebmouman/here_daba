// src/pages/StorePage.jsx
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { ArrowLeft, MapPin, Clock, Phone, ShoppingBag, Star, ExternalLink } from 'lucide-react';
import Navbar from '../../Components/Navbar';
import Footer from '../../components/Footer';

const StorePage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [store, setStore] = useState(null);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Base URL et placeholder
  const baseUrl = process.env.REACT_APP_API_URL || "http://localhost:8080";
  const placeholderImage = `${baseUrl}/api/fichiers/placeholder.png`;
  
  useEffect(() => {
    const fetchStoreData = async () => {
      setLoading(true);
      try {
        // Récupérer les informations de la boutique
        const storeResponse = await axios.get(`${baseUrl}/api/boutiques/${id}`);
        const storeData = storeResponse.data;
        
        // Formater l'objet boutique
        let imageUrl = placeholderImage;
        if (storeData.boutique_img) {
          if (!storeData.boutique_img.startsWith('http') && !storeData.boutique_img.startsWith('/')) {
            imageUrl = `${baseUrl}/api/fichiers/${storeData.boutique_img}`;
          } else if (storeData.boutique_img.startsWith('http')) {
            imageUrl = storeData.boutique_img;
          } else if (storeData.boutique_img.startsWith('/')) {
            imageUrl = `${baseUrl}${storeData.boutique_img}`;
          }
        }
        
        const formattedStore = {
          id: storeData.id_boutique,
          name: storeData.nom,
          image: imageUrl,
          address: storeData.adress,
          hours: storeData.horaire || "9h - 18h",
          contact: storeData.contact,
          description: storeData.description || "Aucune description disponible"
        };
        
        setStore(formattedStore);
        
        // Récupérer les produits liés à cette boutique
        try {
          // Si une API pour récupérer les produits par boutique existe
          const productsResponse = await axios.get(`${baseUrl}/api/produits?boutiqueId=${id}`);
          const productsData = productsResponse.data;
          
          const formattedProducts = await Promise.all(
            productsData.map(async (product) => {
              let images = [];
              try {
                const imagesResponse = await axios.get(`${baseUrl}/api/produits/${product.id}/images`);
                images = imagesResponse.data;
              } catch (error) {
                console.error(`Erreur lors du chargement des images pour le produit ${product.id}:`, error);
              }
              
              const mainImage = images.find(img => img.imagePrincipale) || images[0];
              let discount = null;
              let originalPrice = null;
              
              if (product.idReduction) {
                try {
                  const reductionResponse = await axios.get(`${baseUrl}/api/reductions/${product.idReduction}`);
                  const reductionData = reductionResponse.data;
                  if (reductionData.pourcentage && reductionData.pourcentage > 0) {
                    discount = `${reductionData.pourcentage}%`;
                    originalPrice = product.prix / (1 - reductionData.pourcentage / 100);
                  }
                } catch (error) {
                  console.error(`Erreur lors du chargement de la réduction pour le produit ${product.id}:`, error);
                }
              }
              
              return {
                id: product.id,
                name: product.nomProduit,
                originalPrice: originalPrice || product.prix * 1.2,
                currentPrice: product.prix,
                discount,
                rating: 4.5, // Valeur fictive
                reviewCount: 12, // Valeur fictive
                imageUrl: mainImage ? mainImage.url : placeholderImage,
              };
            })
          );
          
          setProducts(formattedProducts);
        } catch (error) {
          console.error("Erreur lors de la récupération des produits:", error);
          setProducts([]);
        }
      } catch (error) {
        console.error("Erreur lors de la récupération des données de la boutique:", error);
        setError("Impossible de charger les informations de cette boutique.");
      } finally {
        setLoading(false);
      }
    };
    
    fetchStoreData();
  }, [id, baseUrl, placeholderImage]);
  
  const handleProductClick = (productId) => {
    navigate(`/produits/${productId}`);
  };
  
  const handleBack = () => {
    navigate(-1);
  };
  
  // Composant pour afficher un produit
  const ProductCard = ({ product }) => {
    return (
      <div className="bg-white rounded-lg shadow-md overflow-hidden transition-all duration-300 hover:shadow-lg hover:translate-y-[-4px]">
        {/* Image du produit */}
        <div className="relative h-48 overflow-hidden bg-gray-100">
          <img 
            src={product.imageUrl} 
            alt={product.name}
            className="w-full h-full object-cover" 
            onError={(e) => { e.target.src = placeholderImage; }}
          />
          {product.discount && (
            <div className="absolute top-2 right-2 bg-red-500 text-white text-xs font-bold px-2 py-1 rounded">
              {product.discount} OFF
            </div>
          )}
        </div>
        
        {/* Informations du produit */}
        <div className="p-4">
          <h3 className="font-medium text-gray-900 mb-1 truncate">{product.name}</h3>
          
          <div className="flex items-center mb-2">
            {[...Array(5)].map((_, i) => (
              <Star 
                key={i}
                size={14}
                className={`${i < Math.floor(product.rating) ? 'text-yellow-400' : 'text-gray-300'}`}
                fill={i < Math.floor(product.rating) ? 'currentColor' : 'none'}
              />
            ))}
            <span className="text-xs text-gray-600 ml-1">
              ({product.reviewCount})
            </span>
          </div>
          
          <div className="flex items-center justify-between">
            <div>
              {product.originalPrice !== product.currentPrice && (
                <span className="text-gray-500 line-through text-sm mr-2">
                  ${product.originalPrice.toFixed(2)}
                </span>
              )}
              <span className="text-blue-600 font-bold">
                ${product.currentPrice.toFixed(2)}
              </span>
            </div>
            
            <button 
              className="p-1 bg-blue-50 text-blue-600 rounded-full hover:bg-blue-100 transition-colors"
              onClick={(e) => {
                e.stopPropagation();
                console.log(`Ajouté ${product.name} au panier`);
              }}
            >
              <ShoppingBag size={16} />
            </button>
          </div>
        </div>
      </div>
    );
  };
  
  return (
    <>
      <Navbar />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <button 
          onClick={handleBack}
          className="flex items-center text-blue-600 hover:text-blue-700 mb-6 font-medium"
        >
          <ArrowLeft size={20} className="mr-2" />
          Retour
        </button>
        
        {loading ? (
          <div className="flex justify-center items-center h-64">
            <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
          </div>
        ) : error ? (
          <div className="text-center py-16 bg-gray-50 rounded-lg shadow">
            <h2 className="text-2xl font-semibold text-gray-700 mb-4">Erreur</h2>
            <p className="text-gray-500 mb-6">{error}</p>
            <button 
              onClick={handleBack}
              className="bg-blue-500 hover:bg-blue-600 text-white px-6 py-2 rounded-md transition-colors"
            >
              Retour aux résultats
            </button>
          </div>
        ) : store ? (
          <>
            <div className="bg-white rounded-xl shadow-md overflow-hidden mb-12">
              <div className="md:flex">
                {/* Image de la boutique */}
                <div className="md:flex-shrink-0 md:w-1/3">
                  <div className="h-64 md:h-full w-full bg-gray-100">
                    <img 
                      src={store.image} 
                      alt={store.name} 
                      className="h-full w-full object-cover"
                      onError={(e) => { e.target.src = placeholderImage; }}
                    />
                  </div>
                </div>
                
                {/* Informations de la boutique */}
                <div className="p-8 md:w-2/3">
                  <div className="flex flex-wrap items-center mb-4">
                    <span className="bg-blue-100 text-blue-800 text-xs font-semibold px-2.5 py-0.5 rounded mr-2">Boutique</span>
                  </div>
                  
                  <h1 className="text-3xl font-bold text-gray-900 mb-4">{store.name}</h1>
                  
                  <p className="text-gray-700 mb-6">{store.description}</p>
                  
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6 bg-gray-50 p-4 rounded-lg">
                    <div className="flex items-start">
                      <MapPin className="h-5 w-5 text-blue-500 mr-3 mt-1" />
                      <div>
                        <p className="text-gray-600 text-sm">Adresse</p>
                        <p className="font-medium">{store.address || "Non spécifiée"}</p>
                      </div>
                    </div>
                    
                    <div className="flex items-start">
                      <Clock className="h-5 w-5 text-blue-500 mr-3 mt-1" />
                      <div>
                        <p className="text-gray-600 text-sm">Horaires</p>
                        <p className="font-medium">{store.hours || "Non spécifiés"}</p>
                      </div>
                    </div>
                    
                    <div className="flex items-start">
                      <Phone className="h-5 w-5 text-blue-500 mr-3 mt-1" />
                      <div>
                        <p className="text-gray-600 text-sm">Contact</p>
                        <p className="font-medium">{store.contact || "Non spécifié"}</p>
                      </div>
                    </div>
                  </div>
                  
                  <div className="flex flex-wrap gap-3">
                    <a 
                      href={`https://maps.google.com/?q=${encodeURIComponent(store.address || store.name)}`} 
                      target="_blank" 
                      rel="noopener noreferrer"
                      className="inline-flex items-center justify-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
                    >
                      <MapPin size={16} className="mr-2" />
                      Voir sur Google Maps
                    </a>
                    
                    {store.contact && (
                      <a 
                        href={`tel:${store.contact}`}
                        className="inline-flex items-center justify-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md shadow-sm text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
                      >
                        <Phone size={16} className="mr-2" />
                        Appeler la boutique
                      </a>
                    )}
                    
                    <a 
                      href="#products"
                      className="inline-flex items-center justify-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md shadow-sm text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
                    >
                      <ShoppingBag size={16} className="mr-2" />
                      Voir les produits
                    </a>
                  </div>
                </div>
              </div>
            </div>
            
            {/* Carte de localisation (Optionnel) */}
            <div className="mb-12 bg-white shadow-md rounded-xl overflow-hidden">
              <div className="p-4 border-b border-gray-200">
                <h2 className="text-xl font-bold text-gray-900">Localisation</h2>
              </div>
              <div className="h-64 bg-gray-100 relative">
                {/* Placeholder pour la carte */}
                <div className="absolute inset-0 flex items-center justify-center">
                  <div className="text-center">
                    <MapPin size={48} className="mx-auto mb-3 text-blue-500" />
                    <p className="text-gray-600">{store.address || "Adresse non disponible"}</p>
                  </div>
                </div>
                {/* Lien vers Google Maps */}
                <a 
                  href={`https://maps.google.com/?q=${encodeURIComponent(store.address || store.name)}`} 
                  target="_blank" 
                  rel="noopener noreferrer"
                  className="absolute bottom-4 right-4 inline-flex items-center justify-center px-4 py-2 bg-white rounded-md shadow-md text-blue-600 font-medium text-sm hover:bg-blue-50 transition-colors"
                >
                  <ExternalLink size={14} className="mr-1" />
                  Ouvrir dans Google Maps
                </a>
              </div>
            </div>
            
            {/* Produits de la boutique */}
            <div id="products" className="mt-16">
              <div className="flex items-center border-b border-gray-200 pb-4 mb-6">
                <ShoppingBag className="h-6 w-6 text-blue-500 mr-3" />
                <h2 className="text-2xl font-bold text-gray-900">
                  Produits disponibles dans cette boutique
                </h2>
                <span className="ml-3 bg-blue-100 text-blue-800 text-sm font-medium px-2.5 py-0.5 rounded-full">
                  {products.length} articles
                </span>
              </div>
              
              {products.length > 0 ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
                  {products.map((product) => (
                    <div 
                      key={product.id} 
                      onClick={() => handleProductClick(product.id)} 
                      className="cursor-pointer"
                    >
                      <ProductCard product={product} />
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-12 bg-gray-50 rounded-lg shadow-sm">
                  <p className="text-gray-500">Aucun produit disponible pour cette boutique.</p>
                </div>
              )}
            </div>
          </>
        ) : (
          <div className="text-center py-16 bg-gray-50 rounded-lg shadow">
            <h2 className="text-2xl font-semibold text-gray-700 mb-4">Boutique non trouvée</h2>
            <p className="text-gray-500 mb-6">
              La boutique que vous recherchez n'existe pas ou a été supprimée.
            </p>
            <button 
              onClick={handleBack}
              className="bg-blue-500 hover:bg-blue-600 text-white px-6 py-2 rounded-md transition-colors"
            >
              Retour aux résultats
            </button>
          </div>
        )}
      </div>
      <Footer />
    </>
  );
};

export default StorePage;