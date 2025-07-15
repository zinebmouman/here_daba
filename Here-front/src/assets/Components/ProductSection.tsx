import React, { useRef, useState, useEffect } from 'react';
import ProductCard from './ProductCard'; 
import axios from 'axios';
import '../style/productSection.css';

interface Product {
  id: number;
  name: string;
  originalPrice: number;
  currentPrice: number;
  discount?: string;
  rating: number;
  reviewCount: number;
  imageUrl: string; // Image devient requise
}

const ProductSection: React.FC = () => {
  const sliderRef = useRef<HTMLDivElement>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        setLoading(true);
        try {
          const productsResponse = await axios.get('/api/produits');
          
          // Afficher le premier produit pour voir sa structure exacte
          if (productsResponse.data && productsResponse.data.length > 0) {
            console.log("Structure du premier produit:", productsResponse.data[0]);
          }
          
          // Traiter la réponse pour obtenir les produits avec leurs images
          const fetchedProducts = await Promise.all(
            productsResponse.data.map(async (product: any) => {
              try {
                const imagesResponse = await axios.get(`/api/produits/${product.id}/images`);
                const images = imagesResponse.data;
                
                // Vérifier si le produit et ses images existent
                if (!product || !product.id || !images || images.length === 0) {
                  return null;
                }

                // Trouver l'image principale ou prendre la première
                const mainImage = images.find((img: any) => img.imagePrincipale) || images[0];
                
                // Vérifier que l'image a bien une URL
                if (!mainImage || !mainImage.url) {
                  return null;
                }

                // Utiliser nomProduit comme propriété principale pour le nom
                const productName = product.nomProduit || product.nom || product.name;
                console.log(`Produit ${product.id} - Nom: ${productName}`);

                return {
                  id: product.id,
                  name: productName || `Produit #${product.id}`,
                  originalPrice: product.prixOriginal || product.prix || product.price || 0,
                  currentPrice: product.prix || product.price || 0,
                  discount: product.reduction ? `${product.reduction}% OFF` : undefined,
                  rating: product.note || product.rating || 0,
                  reviewCount: product.nombreAvis || product.reviewCount || 0,
                  imageUrl: mainImage.url // S'assurer d'avoir une URL d'image
                };
              } catch (error) {
                console.error(`Erreur lors du chargement des images pour le produit ${product.id}:`, error);
                return null;
              }
            })
          );
          
          // Filtrer les produits null et sans image
          const validProducts = fetchedProducts
            .filter(product => product !== null && product.imageUrl) as Product[];
          
          setProducts(validProducts);
        } catch (apiError) {
          console.error("Erreur lors de l'appel à l'API:", apiError);
          setError("Impossible de charger les produits");
        }
        
        setLoading(false);
      } catch (err) {
        console.error("Erreur lors du chargement des produits:", err);
        setError("Impossible de charger les produits");
        setLoading(false);
      }
    };

    fetchProducts();
  }, []);

  const scrollLeft = () => {
    if (sliderRef.current) {
      sliderRef.current.scrollBy({ 
        left: -300,
        behavior: 'smooth' 
      });
    }
  };

  const scrollRight = () => {
    if (sliderRef.current) {
      sliderRef.current.scrollBy({ 
        left: 300,
        behavior: 'smooth'
      });
    }
  };

  if (loading) {
    return (
      <section className="all py-8 px-16 sm:px-6 lg:px-16">
        <div className="max-w-7xl mx-auto">
          <h2 className="text-2xl font-bold text-gray-900 mb-6">Nos produits</h2>
          <div className="flex justify-center items-center h-64">
            <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-gray-900"></div>
          </div>
        </div>
      </section>
    );
  }

  if (error) {
    return (
      <section className="all py-8 px-16 sm:px-6 lg:px-16">
        <div className="max-w-7xl mx-auto">
          <h2 className="text-2xl font-bold text-gray-900 mb-6">Nos produits</h2>
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
            <p>{error}</p>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="all py-8 px-16 sm:px-6 lg:px-16">
      <div className="max-w-7xl mx-auto">
        {/* Header with title */}
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold text-gray-900">Nos produits</h2>
          
          {/* Navigation Arrows */}
          <div className="flex justify-center space-x-4">
            <button className="p-2 bg-gray-200 rounded-full hover:bg-gray-300 transition-colors" onClick={scrollLeft}>
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-gray-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <button className="p-2 bg-gray-200 rounded-full hover:bg-gray-300 transition-colors" onClick={scrollRight}>
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-gray-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
        </div>
        
        {/* Horizontal scrollable product slider */}
        {products.length > 0 ? (
          <div 
            ref={sliderRef}
            className="flex overflow-x-auto space-x-6 pb-4 scrollbar-hide"
            style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
          >
            {products.map((product) => (
              <div key={product.id} className="flex-shrink-0 w-[200px] product-card-wrapper">
                <ProductCard 
                  id={product.id}
                  name={product.name}
                  originalPrice={product.originalPrice}
                  currentPrice={product.currentPrice}
                  discount={product.discount}
                  rating={product.rating}
                  reviewCount={product.reviewCount}
                  imageUrl={product.imageUrl}
                />
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center text-gray-500 py-8">
            Aucun produit disponible.
          </div>
        )}
      </div>
    </section>
  );
};

export default ProductSection;