// src/Pages/SearchResultsPage.jsx - Version avec filtrage spécifique et Gemini AI
import React, { useState, useEffect, useCallback } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { MapPin, Navigation, Store, Filter, ChevronLeft, ChevronRight, ShoppingBag } from "lucide-react";
import Navbar from "../Components/Navbar.tsx";
import Footer from "../Components/Footer.tsx";
import ProductCard2 from "../Components/ProductCard2.tsx";
import StoreCard from "../Components/ProductPage/StoreCard";
import CategoryCard from "../Components/CategoryCard";
import axios from "axios";
import "../style/Navbar.css";

const SearchResultsPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const queryParams = new URLSearchParams(location.search);
  const searchQuery = queryParams.get("q") || "";
  const categoryId = queryParams.get("category") || "";
  const [activeTab, setActiveTab] = useState("products");

  // États pour les résultats de recherche
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [stores, setStores] = useState([]);
  const [relatedProducts, setRelatedProducts] = useState([]);
  const [userLocation, setUserLocation] = useState(null);
  const [storesWithDistance, setStoresWithDistance] = useState([]);
  const [loading, setLoading] = useState(true);
  const [hasRequestedLocation, setHasRequestedLocation] = useState(false);
  const [locationError, setLocationError] = useState(null);

  // États pour les filtres
  const [priceRange, setPriceRange] = useState({ min: 0, max: 1000 });
  const [sortBy, setSortBy] = useState("relevance");
  const [filterOpen, setFilterOpen] = useState(false);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [visibleItems, setVisibleItems] = useState(4);

  // Base URL et placeholder
  const baseUrl = "http://localhost:8080";
  const placeholderImage = `${baseUrl}/api/fichiers/placeholder.png`;

  // Demander automatiquement la localisation lorsque l'utilisateur accède à l'onglet Boutiques
  useEffect(() => {
    if (activeTab === "stores" && !hasRequestedLocation && !userLocation) {
      requestUserLocation();
    }
  }, [activeTab, hasRequestedLocation, userLocation]);

  // Fonction pour déterminer le nombre d'articles visibles selon la taille de l'écran
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth < 640) {
        setVisibleItems(1);
      } else if (window.innerWidth < 768) {
        setVisibleItems(2);
      } else if (window.innerWidth < 1024) {
        setVisibleItems(3);
      } else {
        setVisibleItems(4);
      }
    };

    handleResize();
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  // Fonction pour demander la localisation de l'utilisateur
  const requestUserLocation = useCallback(() => {
    setHasRequestedLocation(true);
    setLoading(true);
    
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setUserLocation({
            lat: position.coords.latitude,
            lng: position.coords.longitude,
          });
          setLocationError(null);
          setLoading(false);
        },
        (error) => {
          console.error("Erreur de géolocalisation:", error);
          setLocationError("Impossible d'obtenir votre position. Veuillez vérifier vos paramètres de localisation.");
          setLoading(false);
        },
        { timeout: 10000, enableHighAccuracy: true }
      );
    } else {
      setLocationError("La géolocalisation n'est pas supportée par votre navigateur");
      setLoading(false);
    }
  }, []);

  // Fonction pour formater les données de produit
  const formatProduct = async (product) => {
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
          discount = `${reductionData.pourcentage}% OFF`;
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
      rating: 4.5, // Valeur fictive, à remplacer par une vraie donnée
      reviewCount: 12, // Valeur fictive, à remplacer par une vraie donnée
      imageUrl: mainImage ? mainImage.url : placeholderImage,
      idCategorie: product.idCategorie,
    };
  };

  // Fonction pour obtenir l'icône correspondante par nom
  const getIconByName = (iconName) => {
    switch (iconName) {
      case "ShoppingBag":
        return ShoppingBag;
      case "Store":
        return Store;
      case "MapPin":
        return MapPin;
      default:
        return Store;
    }
  };

  // NOUVELLE FONCTION : Recherche avec Gemini AI
  const fetchSearchResultsWithGemini = async (searchQuery) => {
    try {
      // Appeler l'API de recherche intelligente
      const intelligentSearchResponse = await axios.post(
  `${baseUrl}/api/produits/search-intelligent?query=${encodeURIComponent(searchQuery)}`
);

      if (intelligentSearchResponse.data && intelligentSearchResponse.data.results) {
        const geminiResults = intelligentSearchResponse.data.results;
        console.log(`✨ Gemini AI a trouvé ${geminiResults.length} produits`);
        
        // Formater les résultats Gemini
        const formattedGeminiProducts = await Promise.all(
          geminiResults.map(async (product) => {
            // Si le produit a déjà des images, les utiliser
            let images = product.images || [];
            
            // Sinon, essayer de les charger
            if (!images.length && product.id) {
              try {
                const imagesResponse = await axios.get(`${baseUrl}/api/produits/${product.id}/images`);
                images = imagesResponse.data;
              } catch (error) {
                console.error(`Erreur lors du chargement des images pour le produit ${product.id}:`, error);
              }
            }
            
            const mainImage = images.find(img => img.imagePrincipale) || images[0];
            
            return {
              id: product.id,
              name: product.nomProduit,
              originalPrice: product.prix * 1.2,
              currentPrice: product.prix,
              discount: null,
              rating: 4.5,
              reviewCount: 12,
              imageUrl: mainImage ? mainImage.url : placeholderImage,
              idCategorie: product.idCategorie,
              isFromGemini: true // Marqueur pour identifier les résultats Gemini
            };
          })
        );
        
        return {
          products: formattedGeminiProducts,
          aiEnhanced: intelligentSearchResponse.data.aiEnhanced
        };
      }
    } catch (error) {
      console.error("Erreur Gemini AI:", error);
      return null;
    }
  };

  // Fonction pour charger les résultats de recherche
  useEffect(() => {
    const fetchSearchResults = async () => {
      setLoading(true);
      try {
        if (searchQuery) {
          // 🚀 D'abord essayer avec Gemini AI
          const geminiResults = await fetchSearchResultsWithGemini(searchQuery);
          
          if (geminiResults && geminiResults.products.length > 0) {
            setProducts(geminiResults.products);
            
            // Ajouter un indicateur que la recherche a été améliorée par l'IA
            console.log("Recherche améliorée par Gemini AI");
          } else {
            // Fallback vers la recherche classique si Gemini ne trouve rien
            console.log("Fallback vers la recherche classique");
            
            // Nouveau code: on commence par chercher les boutiques qui ont le produit recherché
            // Cette API devrait retourner uniquement les boutiques qui ont des produits correspondant à la recherche
            try {
              const storesResponse = await axios.get(`${baseUrl}/api/boutiques/search-with-products?q=${encodeURIComponent(searchQuery)}`);
              const storesData = storesResponse.data;
              
              if (storesData && storesData.length > 0) {
                // Formater les données des boutiques
                const formattedStores = storesData.map(store => {
                  let imageUrl = placeholderImage;
                  
                  if (store.boutique_img) {
                    if (!store.boutique_img.startsWith('http') && !store.boutique_img.startsWith('/')) {
                      imageUrl = `${baseUrl}/api/fichiers/${store.boutique_img}`;
                    } else if (store.boutique_img.startsWith('http')) {
                      imageUrl = store.boutique_img;
                    } else if (store.boutique_img.startsWith('/')) {
                      imageUrl = `${baseUrl}${store.boutique_img}`;
                    }
                  }

                  return {
                    id: store.id_boutique,
                    name: store.nom,
                    image: imageUrl,
                    address: store.adress,
                    hours: store.horaire || "9h - 18h",
                    contact: store.contact,
                    // Si les données sont disponibles, on ajoute les produits et catégories spécifiques
                    products: store.products || [],
                    categories: store.categories || []
                  };
                });
                
                setStores(formattedStores);
                
                // Extraire les produits et catégories uniques des boutiques trouvées
                let uniqueProducts = [];
                let uniqueCategories = [];
                
                storesData.forEach(store => {
                  // Si la boutique a des produits correspondant à la recherche
                  if (store.products && Array.isArray(store.products)) {
                    store.products.forEach(product => {
                      if (!uniqueProducts.some(p => p.id === product.id)) {
                        uniqueProducts.push(product);
                      }
                    });
                  }
                  
                  // Si la boutique a des catégories correspondant à la recherche
                  if (store.categories && Array.isArray(store.categories)) {
                    store.categories.forEach(category => {
                      if (!uniqueCategories.some(c => c.id === category.id)) {
                        uniqueCategories.push(category);
                      }
                    });
                  }
                });
                
                // Formater les produits uniques
                if (uniqueProducts.length > 0) {
                  const formattedProducts = await Promise.all(
                    uniqueProducts.map(formatProduct)
                  );
                  setProducts(formattedProducts);
                } else {
                  // Si on n'a pas récupéré directement les produits, chercher par nom de produit
                  // Cette partie est similaire à votre code d'origine
                  const productsResponse = await axios.get(`${baseUrl}/api/produits?search=${encodeURIComponent(searchQuery)}`);
                  const productsData = productsResponse.data;
                  
                  if (productsData && productsData.length > 0) {
                    const formattedProducts = await Promise.all(
                      productsData.map(formatProduct)
                    );
                    
                    setProducts(formattedProducts);
                  } else {
                    setProducts([]);
                  }
                }
                
                // Formater les catégories uniques
                if (uniqueCategories.length > 0) {
                  const formattedCategories = uniqueCategories.map(cat => ({
                    id: cat.idCategorie || cat.id,
                    name: cat.nom || cat.name,
                    description: cat.description,
                    icon: cat.icon,
                    type: 'category'
                  }));
                  
                  setCategories(formattedCategories);
                } else {
                  // Si on n'a pas récupéré directement les catégories, chercher par nom de catégorie
                  const categoriesResponse = await axios.get(`${baseUrl}/api/categories/search?nom=${encodeURIComponent(searchQuery)}`);
                  const categoriesData = categoriesResponse.data.map(cat => ({
                    id: cat.idCategorie,
                    name: cat.nom,
                    description: cat.description,
                    icon: cat.icon,
                    type: 'category'
                  }));
                  
                  setCategories(categoriesData);
                }
              } else {
                // Si aucune boutique trouvée, on recherche quand même des produits et catégories
                // Cette partie est similaire à votre code d'origine
                setStores([]);
                
                // Rechercher des produits par nom
                const productsResponse = await axios.get(`${baseUrl}/api/produits?search=${encodeURIComponent(searchQuery)}`);
                const productsData = productsResponse.data;
                
                if (productsData && productsData.length > 0) {
                  const formattedProducts = await Promise.all(
                    productsData.map(formatProduct)
                  );
                  
                  setProducts(formattedProducts);
                } else {
                  setProducts([]);
                }
                
                // Rechercher des catégories par nom
                const categoriesResponse = await axios.get(`${baseUrl}/api/categories/search?nom=${encodeURIComponent(searchQuery)}`);
                const categoriesData = categoriesResponse.data.map(cat => ({
                  id: cat.idCategorie,
                  name: cat.nom,
                  description: cat.description,
                  icon: cat.icon,
                  type: 'category'
                }));
                
                setCategories(categoriesData);
              }
            } catch (error) {
              console.error("Erreur lors de la recherche de boutiques avec produits:", error);
              
              // En cas d'erreur, utiliser le code d'origine pour récupérer les données
              console.log("Utilisation de la méthode de recherche alternative");
              
              // Rechercher des boutiques par nom
              try {
                const storesResponse = await axios.get(`${baseUrl}/api/boutiques?search=${encodeURIComponent(searchQuery)}`);
                const storesData = storesResponse.data;
                
                // Formater les données des boutiques
                const formattedStores = storesData.map(store => {
                  let imageUrl = placeholderImage;
                  
                  if (store.boutique_img) {
                    if (!store.boutique_img.startsWith('http') && !store.boutique_img.startsWith('/')) {
                      imageUrl = `${baseUrl}/api/fichiers/${store.boutique_img}`;
                    } else if (store.boutique_img.startsWith('http')) {
                      imageUrl = store.boutique_img;
                    } else if (store.boutique_img.startsWith('/')) {
                      imageUrl = `${baseUrl}${store.boutique_img}`;
                    }
                  }

                  return {
                    id: store.id_boutique,
                    name: store.nom,
                    image: imageUrl,
                    address: store.adress,
                    hours: store.horaire || "9h - 18h",
                    contact: store.contact,
                  };
                });
                
                setStores(formattedStores);
              } catch (error) {
                console.error("Erreur lors de la recherche de boutiques par nom:", error);
                setStores([]);
              }
              
              // Rechercher des produits par nom
              try {
                const productsResponse = await axios.get(`${baseUrl}/api/produits?search=${encodeURIComponent(searchQuery)}`);
                const productsData = productsResponse.data;
                
                if (productsData && productsData.length > 0) {
                  const formattedProducts = await Promise.all(
                    productsData.map(formatProduct)
                  );
                  
                  setProducts(formattedProducts);
                } else {
                  setProducts([]);
                }
              } catch (error) {
                console.error("Erreur lors de la recherche de produits:", error);
                setProducts([]);
              }
              
              // Rechercher des catégories par nom
              try {
                const categoriesResponse = await axios.get(`${baseUrl}/api/categories/search?nom=${encodeURIComponent(searchQuery)}`);
                const categoriesData = categoriesResponse.data.map(cat => ({
                  id: cat.idCategorie,
                  name: cat.nom,
                  description: cat.description,
                  icon: cat.icon,
                  type: 'category'
                }));
                
                setCategories(categoriesData);
              } catch (error) {
                console.error("Erreur lors de la recherche de catégories:", error);
                setCategories([]);
              }
            }
          }
        } else if (categoryId) {
          // Recherche par ID de catégorie
          const categoryResponse = await axios.get(`${baseUrl}/api/categories/${categoryId}`);
          const categoryData = categoryResponse.data;
          
          setCategories([{
            id: categoryData.idCategorie,
            name: categoryData.nom,
            description: categoryData.description,
            icon: categoryData.icon,
            type: 'category'
          }]);
          
          // Récupérer les produits de cette catégorie
          const productsResponse = await axios.get(`${baseUrl}/api/produits/categorie/${categoryId}`);
          const productsData = productsResponse.data;
          
          if (productsData && productsData.length > 0) {
            const formattedProducts = await Promise.all(
              productsData.map(formatProduct)
            );
            
            setProducts(formattedProducts);
            
            // Récupérer les boutiques qui ont des produits dans cette catégorie
            try {
              const storesWithCategoryResponse = await axios.get(`${baseUrl}/api/boutiques/by-category/${categoryId}`);
              const storesData = storesWithCategoryResponse.data;
              
              const formattedStores = storesData.map(store => {
                let imageUrl = placeholderImage;
                
                if (store.boutique_img) {
                  if (!store.boutique_img.startsWith('http') && !store.boutique_img.startsWith('/')) {
                    imageUrl = `${baseUrl}/api/fichiers/${store.boutique_img}`;
                  } else if (store.boutique_img.startsWith('http')) {
                    imageUrl = store.boutique_img;
                  } else if (store.boutique_img.startsWith('/')) {
                    imageUrl = `${baseUrl}${store.boutique_img}`;
                  }
                }

                return {
                  id: store.id_boutique,
                  name: store.nom,
                  image: imageUrl,
                  address: store.adress,
                  hours: store.horaire || "9h - 18h",
                  contact: store.contact,
                };
              });
              
              setStores(formattedStores);
            } catch (error) {
              console.error("Erreur lors de la récupération des boutiques par catégorie:", error);
              setStores([]);
            }
          } else {
            setProducts([]);
            setStores([]);
          }
        } else {
          // Si aucun terme de recherche ou catégorie, afficher des produits par défaut
          try {
            const productsResponse = await axios.get(`${baseUrl}/api/produits`);
            const productsData = productsResponse.data;
            
            if (productsData && productsData.length > 0) {
              const formattedProducts = await Promise.all(
                productsData.slice(0, 12).map(formatProduct)
              );
              
              setProducts(formattedProducts);
            }
          } catch (error) {
            console.error("Erreur lors de la récupération des produits par défaut:", error);
          }
          
          // Récupérer toutes les catégories
          try {
            const categoriesResponse = await axios.get(`${baseUrl}/api/categories`);
            const categoriesData = categoriesResponse.data.map(cat => ({
              id: cat.idCategorie,
              name: cat.nom,
              description: cat.description,
              icon: cat.icon,
              type: 'category'
            }));
            
            setCategories(categoriesData);
          } catch (error) {
            console.error("Erreur lors de la récupération des catégories:", error);
          }
          
          // Récupérer toutes les boutiques
          fetchAllStores();
        }
      } catch (error) {
        console.error("Erreur lors de la récupération des résultats:", error);
      } finally {
        setLoading(false);
      }
    };

    const fetchAllStores = async () => {
      try {
        // Récupérer toutes les boutiques
        const storesResponse = await axios.get(`${baseUrl}/api/boutiques`);
        const storesData = storesResponse.data.map(store => {
          let imageUrl = placeholderImage;
          
          if (store.boutique_img) {
            if (!store.boutique_img.startsWith('http') && !store.boutique_img.startsWith('/')) {
              imageUrl = `${baseUrl}/api/fichiers/${store.boutique_img}`;
            } else if (store.boutique_img.startsWith('http')) {
              imageUrl = store.boutique_img;
            } else if (store.boutique_img.startsWith('/')) {
              imageUrl = `${baseUrl}${store.boutique_img}`;
            }
          }

          return {
            id: store.id_boutique,
            name: store.nom,
            image: imageUrl,
            address: store.adress,
            hours: store.horaire || "9h - 18h",
            contact: store.contact,
          };
        });
        
        setStores(storesData);
      } catch (error) {
        console.error("Erreur lors de la récupération des boutiques:", error);
      }
    };

    fetchSearchResults();
  }, [categoryId, searchQuery, baseUrl, placeholderImage]);

  // Calcul des distances pour les boutiques si la localisation est disponible
  useEffect(() => {
    if (!userLocation || !window.google || !window.google.maps || stores.length === 0) return;

    const calculateDistances = async () => {
      const geocoder = new window.google.maps.Geocoder();
      const distanceMatrixService = new window.google.maps.DistanceMatrixService();
      
      const storesWithCoordinates = await Promise.all(
        stores.map(async (store) => {
          try {
            if (!store.address) return store;
            
            const result = await new Promise((resolve, reject) => {
              geocoder.geocode({ address: store.address }, (results, status) => {
                if (status === "OK" && results[0]) {
                  resolve(results[0]);
                } else {
                  reject(new Error(`Geocoding failed: ${status}`));
                }
              });
            });
            
            return {
              ...store,
              coordinates: {
                lat: result.geometry.location.lat(),
                lng: result.geometry.location.lng()
              }
            };
          } catch (error) {
            console.error(`Erreur pour la boutique ${store.name}:`, error);
            return store;
          }
        })
      );
      
      const storesWithValidCoordinates = storesWithCoordinates.filter(
        store => store.coordinates
      );
      
      if (storesWithValidCoordinates.length === 0) {
        setStoresWithDistance(stores);
        return;
      }
      
      try {
        const destinations = storesWithValidCoordinates.map(store => store.coordinates);
        
        const result = await new Promise((resolve, reject) => {
          distanceMatrixService.getDistanceMatrix(
            {
              origins: [userLocation],
              destinations: destinations,
              travelMode: window.google.maps.TravelMode.DRIVING,
              unitSystem: window.google.maps.UnitSystem.METRIC,
            },
            (response, status) => {
              if (status === "OK" && response) {
                resolve(response);
              } else {
                reject(new Error(`Distance Matrix failed: ${status}`));
              }
            }
          );
        });
        
        const storesWithDistanceInfo = storesWithValidCoordinates.map((store, index) => {
          const element = result.rows[0].elements[index];
          
          if (element.status === "OK") {
            return {
              ...store,
              distance: element.distance.text,
              duration: element.duration.text,
              distanceValue: element.distance.value
            };
          }
          
          return store;
        });
        
        // Trier par distance et combiner avec les magasins sans coordonnées
        const sortedStores = storesWithDistanceInfo
          .filter(store => store.distanceValue !== undefined)
          .sort((a, b) => a.distanceValue - b.distanceValue);
        
        const storesWithoutDistance = stores.filter(
          store => !storesWithDistanceInfo.some(s => s.id === store.id)
        );
        
        setStoresWithDistance([...sortedStores, ...storesWithoutDistance]);
      } catch (error) {
        console.error("Erreur lors du calcul des distances:", error);
        setStoresWithDistance(stores);
      }
    };
    
    calculateDistances();
  }, [userLocation, stores]);

  // Reste du code (fonctions de tri, navigation, etc.) inchangé...
  // Fonction pour gérer le tri
  const handleSort = (value) => {
    setSortBy(value);
    
    let sortedProducts = [...products];
    
    switch (value) {
      case "price-low":
        sortedProducts.sort((a, b) => a.currentPrice - b.currentPrice);
        break;
      case "price-high":
        sortedProducts.sort((a, b) => b.currentPrice - a.currentPrice);
        break;
      case "rating":
        sortedProducts.sort((a, b) => b.rating - a.rating);
        break;
      default:
        // Par défaut, ne pas changer l'ordre
        break;
    }
    
    setProducts(sortedProducts);
  };

  // Navigation pour le carrousel de produits connexes
  const goToPrevious = () => {
    if (currentIndex > 0) {
      setCurrentIndex(currentIndex - 1);
    }
  };

  const goToNext = () => {
    if (currentIndex < relatedProducts.length - visibleItems) {
      setCurrentIndex(currentIndex + 1);
    }
  };

  // Fonction pour filtrer par gamme de prix
  const handlePriceRangeChange = (min, max) => {
    setPriceRange({ min, max });
  };

  // Fonction pour naviguer vers la page de catégorie
  const handleCategoryClick = (categoryId) => {
    navigate(`/search?category=${categoryId}`);
  };

  // Fonction pour naviguer vers la page de produit
  const handleProductClick = (productId) => {
    navigate(`/produits/${productId}`);
  };

  // Fonction pour naviguer vers la page de boutique
  const handleStoreClick = (storeId) => {
    navigate(`/boutiques/${storeId}`);
  };

  // Filtrer les produits en fonction des filtres appliqués
  const filteredProducts = products.filter(
    product => product.currentPrice >= priceRange.min && product.currentPrice <= priceRange.max
  );

  return (
    <>
      <Navbar />
      <div className="container mx-auto px-4 py-8">
        {/* En-tête avec titre de recherche */}
        <div className="mb-6">
          {categoryId && categories.length > 0 ? (
            <h1 className="text-3xl font-bold">Catégorie: {categories[0].name}</h1>
          ) : (
            <h1 className="text-3xl font-bold">Résultats pour "{searchQuery}"</h1>
          )}
          <p className="text-gray-600 mt-2">
            {filteredProducts.length} produits trouvés {stores.length > 0 ? `• ${stores.length} boutiques trouvées` : ''}
            {categories.length > 0 ? ` • ${categories.length} catégories trouvées` : ''}
          </p>
        </div>

        {/* Onglets de navigation */}
        <div className="flex border-b mb-6">
          <button
            className={`px-4 py-2 font-medium ${
              activeTab === "products" ? "border-b-2 border-teal-500 text-teal-600" : "text-gray-600"
            }`}
            onClick={() => setActiveTab("products")}
          >
            Produits {filteredProducts.length > 0 && `(${filteredProducts.length})`}
          </button>
          <button
            className={`px-4 py-2 font-medium ${
              activeTab === "categories" ? "border-b-2 border-teal-500 text-teal-600" : "text-gray-600"
            }`}
            onClick={() => setActiveTab("categories")}
          >
            Catégories {categories.length > 0 && `(${categories.length})`}
          </button>
          <button
            className={`px-4 py-2 font-medium ${
              activeTab === "stores" ? "border-b-2 border-teal-500 text-teal-600" : "text-gray-600"
            }`}
            onClick={() => setActiveTab("stores")}
          >
            Boutiques {stores.length > 0 && `(${stores.length})`}
          </button>
        </div>

        {/* Filtres (uniquement affichés dans l'onglet Produits) */}
        {activeTab === "products" && (
          <div className="flex justify-between items-center mb-6">
            <button
              className="flex items-center text-gray-700 hover:text-teal-600"
              onClick={() => setFilterOpen(!filterOpen)}
            >
              <Filter size={20} className="mr-2" />
              Filtres
            </button>
            
            <div className="flex items-center">
              <label htmlFor="sort" className="mr-2 text-gray-700">
                Trier par:
              </label>
              <select
                id="sort"
                value={sortBy}
                onChange={(e) => handleSort(e.target.value)}
                className="border rounded-md px-3 py-1"
              >
                <option value="relevance">Pertinence</option>
                <option value="price-low">Prix croissant</option>
                <option value="price-high">Prix décroissant</option>
                <option value="rating">Note</option>
              </select>
            </div>
          </div>
        )}

        {/* Section des filtres étendus */}
        {filterOpen && activeTab === "products" && (
          <div className="bg-gray-50 p-4 rounded-lg mb-6">
            <div className="flex items-center gap-4">
              <div className="flex-1">
                <label className="block text-sm font-medium text-gray-700 mb-1">Prix min</label>
                <input
                  type="number"
                  placeholder="Min"
                  value={priceRange.min}
                  onChange={(e) => handlePriceRangeChange(parseFloat(e.target.value) || 0, priceRange.max)}
                  className="border rounded-md px-3 py-1 w-full"
                />
              </div>
              <div className="flex-1">
                <label className="block text-sm font-medium text-gray-700 mb-1">Prix max</label>
                <input
                  type="number"
                  placeholder="Max"
                  value={priceRange.max}
                  onChange={(e) => handlePriceRangeChange(priceRange.min, parseFloat(e.target.value) || 1000)}
                  className="border rounded-md px-3 py-1 w-full"
                />
              </div>
              <button
                onClick={() => handlePriceRangeChange(0, 1000)}
                className="text-teal-600 hover:text-teal-800 mt-6"
              >
                Réinitialiser
              </button>
            </div>
          </div>
        )}

        {loading ? (
          <div className="flex justify-center items-center h-64">
            <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-teal-500"></div>
          </div>
        ) : (
          <>
            {/* Contenu des onglets */}
            {activeTab === "products" && (
              <>
                {/* Bannière Gemini AI si des résultats IA sont présents */}
                {products.some(p => p.isFromGemini) && (
                  <div className="mb-4 bg-gradient-to-r from-teal-50 to-blue-50 border border-teal-200 rounded-lg p-4">
                    <div className="flex items-center">
                      <span className="text-2xl mr-2">🤖</span>
                      <div>
                        <h3 className="font-medium text-teal-800">Résultats optimisés par l'IA</h3>
                        <p className="text-sm text-teal-600">
                          Gemini AI a analysé votre recherche pour vous proposer les meilleurs produits
                        </p>
                      </div>
                    </div>
                  </div>
                )}
                
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
                  {filteredProducts.length > 0 ? (
                    filteredProducts.map((product) => (
                      <div 
                        key={product.id} 
                        onClick={() => handleProductClick(product.id)} 
                        className="cursor-pointer relative"
                      >
                        {/* Badge IA pour les produits trouvés par Gemini */}
                        {product.isFromGemini && (
                          <div className="absolute top-2 right-2 z-10">
                            <span className="bg-teal-500 text-white text-xs px-2 py-1 rounded-full shadow-md">
                              IA
                            </span>
                          </div>
                        )}
                        <ProductCard2
                          name={product.name}
                          originalPrice={product.originalPrice}
                          currentPrice={product.currentPrice}
                          discount={product.discount}
                          rating={product.rating}
                          reviewCount={product.reviewCount}
                          imageUrl={product.imageUrl}
                          onAddToCart={(e) => {
                            e.stopPropagation();
                            console.log(`Added ${product.name} to cart`);
                          }}
                          onAddToFavorite={(e) => {
                            e.stopPropagation();
                            console.log(`Added ${product.name} to favorites`);
                          }}
                        />
                      </div>
                    ))
                  ) : (
                    <div className="col-span-full text-center py-12">
                      <p className="text-gray-500">Aucun produit trouvé.</p>
                      {searchQuery && (
                        <p className="mt-2 text-gray-400">
                          Essayez de modifier votre recherche ou consultez les catégories disponibles.
                        </p>
                      )}
                    </div>
                  )}
                </div>
              </>
            )}

            {activeTab === "categories" && (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-6">
                {categories.length > 0 ? (
                  categories.map((category) => {
                    // Utiliser le composant CategoryCard avec l'icône appropriée
                    const IconComponent = category.icon ? getIconByName(category.icon) : Store;
                    return (
                      <div key={category.id} onClick={() => handleCategoryClick(category.id)}>
                        <CategoryCard
                          Icon={IconComponent}
                          Label={category.name}
                        />
                      </div>
                    );
                  })
                ) : (
                  <div className="col-span-full text-center py-12">
                    <p className="text-gray-500">Aucune catégorie trouvée.</p>
                  </div>
                )}
              </div>
            )}

            {activeTab === "stores" && (
              <>
                {!hasRequestedLocation && (
                  <div className="bg-gray-100 p-6 rounded-lg mb-8 text-center">
                    <p className="text-gray-800 mb-4">
                      Découvrez les boutiques à proximité qui vendent "{searchQuery}"
                    </p>
                    <button 
                      onClick={requestUserLocation}
                      className="bg-green-600 hover:bg-green-700 text-white px-6 py-3 rounded-full flex items-center justify-center mx-auto"
                    >
                      <MapPin className="mr-2" size={20} />
                      Utiliser ma localisation
                    </button>
                  </div>
                )}

                {locationError && (
                  <div className="bg-red-100 p-4 rounded-lg mb-8">
                    <p className="text-red-600">{locationError}</p>
                    <button 
                      onClick={requestUserLocation}
                      className="mt-2 text-teal-600 hover:underline"
                    >
                      Réessayer
                    </button>
                  </div>
                )}

                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                  {storesWithDistance.length > 0 ? (
                    storesWithDistance.map(store => (
                      <div 
                        key={store.id} 
                        className="cursor-pointer"
                        onClick={() => handleStoreClick(store.id)}
                      >
                        <StoreCard 
                          store={store} 
                          productName={searchQuery.length > 0 ? searchQuery : null}
                        />
                      </div>
                    ))
                  ) : stores.length > 0 ? (
                    stores.map(store => (
                      <div 
                        key={store.id} 
                        className="cursor-pointer"
                        onClick={() => handleStoreClick(store.id)}
                      >
                        <StoreCard 
                          store={store}
                          productName={searchQuery.length > 0 ? searchQuery : null}
                        />
                      </div>
                    ))
                  ) : (
                    <div className="col-span-full text-center py-12">
                      <p className="text-gray-500">Aucune boutique trouvée avec "{searchQuery}".</p>
                    </div>
                  )}
                </div>
              </>
            )}

            {/* Produits connexes */}
            {activeTab === "products" && relatedProducts.length > 0 && (
              <div className="mt-12">
                <div className="flex items-center justify-between mb-6">
                  <h2 className="text-2xl font-bold">Vous pourriez aussi aimer</h2>
                  
                  <div className="flex space-x-2">
                    <button
                      onClick={goToPrevious}
                      disabled={currentIndex === 0}
                      className={`p-2 rounded-full border ${
                        currentIndex > 0
                          ? "border-gray-300 text-gray-700 hover:bg-gray-100"
                          : "border-gray-200 text-gray-300 cursor-not-allowed"
                      }`}
                      aria-label="Previous products"
                    >
                      <ChevronLeft size={20} />
                    </button>
                    <button
                      onClick={goToNext}
                      disabled={currentIndex >= relatedProducts.length - visibleItems}
                      className={`p-2 rounded-full border ${
                        currentIndex < relatedProducts.length - visibleItems
                          ? "border-gray-300 text-gray-700 hover:bg-gray-100"
                          : "border-gray-200 text-gray-300 cursor-not-allowed"
                      }`}
                      aria-label="Next products"
                    >
                      <ChevronRight size={20} />
                    </button>
                  </div>
                </div>
                
                <div className="relative overflow-hidden">
                  <div
                    className="flex transition-transform duration-500 ease-in-out"
                    style={{
                      transform: `translateX(-${currentIndex * (100 / visibleItems)}%)`,
                      width: `${(relatedProducts.length / visibleItems) * 100}%`,
                    }}
                  >
                    {relatedProducts.map((product) => (
                      <div
                        key={product.id}
                        className="px-3"
                        style={{ width: `${100 / relatedProducts.length}%` }}
                        onClick={() => handleProductClick(product.id)}
                      >
                        <ProductCard2
                          name={product.name}
                          originalPrice={product.originalPrice}
                          currentPrice={product.currentPrice}
                          discount={product.discount}
                          rating={product.rating}
                          reviewCount={product.reviewCount}
                          imageUrl={product.imageUrl}
                          onAddToCart={(e) => {
                            e.stopPropagation();
                            console.log(`Added ${product.name} to cart`);
                          }}
                          onAddToFavorite={(e) => {
                            e.stopPropagation();
                            console.log(`Added ${product.name} to favorites`);
                          }}
                        />
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {/* Message si aucun résultat global */}
            {!loading && products.length === 0 && categories.length === 0 && stores.length === 0 && (
              <div className="text-center py-16 bg-gray-50 rounded-lg">
                <h2 className="text-2xl font-semibold text-gray-700 mb-4">Aucun résultat trouvé</h2>
                <p className="text-gray-500 mb-6">
                  Nous n'avons trouvé aucun résultat correspondant à "{searchQuery}".
                </p>
                <div className="mt-4 space-y-4">
                  <p className="text-gray-600">Suggestions:</p>
                  <ul className="text-gray-500 list-disc list-inside">
                    <li>Vérifiez l'orthographe des mots-clés</li>
                    <li>Essayez des mots-clés plus généraux</li>
                    <li>Utilisez moins de mots-clés</li>
                  </ul>
                  <button 
                    onClick={() => navigate("/")}
                    className="mt-6 bg-teal-500 hover:bg-teal-600 text-white px-6 py-2 rounded-full"
                  >
                    Retour à l'accueil
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
      <Footer />
    </>
  );
};

export default SearchResultsPage;