import React, { useState, useEffect, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import { auth } from "../../config/Firebase";
import { updateUserRole } from "./Signup/services/authService";
import { 
  CART_STORAGE_KEY, 
  getCartItems, 
  getCartItemCount as getCartCount 
} from "../../utils/shopUtils";
import {
  onAuthStateChanged,
  signOut,
  User,
  GoogleAuthProvider,
  signInWithPopup,
  signInWithEmailAndPassword,
  sendPasswordResetEmail,
} from "firebase/auth";
import { doc, updateDoc, getDoc, setDoc } from "firebase/firestore";
import { db } from "../../config/Firebase";
import {
  Search,
  Heart,
  ShoppingCart,
  Menu,
  X,
  User as UserIcon,
  Check,
  Store,
  MessageSquare,
  Settings,
  LogOut,
  ShoppingBag,
  Bell,
  ChevronDown,
  Filter,
} from "lucide-react";
import "../style/Navbar.css";
import FavoritesPanel from "../Components/FavoritesPanel";
import CartPanel from "../Components/CartPanel";
import NotificationPanel from "../Components/NotificationPanel";
import QuickSignInModal from "../Components/QuickSignInModal";

// Interface pour les résultats de recherche
interface SearchResult {
  id: string | number;
  name: string;
  type: 'category' | 'product' | 'boutique' | 'reduction';
  description?: string;
  price?: number;
  imageUrl?: string;
  pourcentage_reduction?: number;
}

interface NavbarProps {
  onSearch?: (query: string) => void;
}

const Navbar: React.FC<NavbarProps> = ({ onSearch }) => {
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [searchResults, setSearchResults] = useState<SearchResult[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const searchTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const searchResultsRef = useRef<HTMLDivElement | null>(null);
  
  // Ajout de l'état pour la recherche avancée
  const [showAdvancedSearch, setShowAdvancedSearch] = useState(false);
  const [searchType, setSearchType] = useState<string>("all");
  const [selectedCategory, setSelectedCategory] = useState<string>("");

  const [isCategoriesOpen, setIsCategoriesOpen] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isSignInModalOpen, setIsSignInModalOpen] = useState(false);
  const [isQuickSignInModalOpen, setIsQuickSignInModalOpen] = useState(false);
  const [showFavoritesPanel, setShowFavoritesPanel] = useState(false);
  const [showCartPanel, setShowCartPanel] = useState(false);
  const [showNotificationsPanel, setShowNotificationsPanel] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [staySignedIn, setStaySignedIn] = useState(false);
  const [user, setUser] = useState<User | null>(null);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [resetMessage, setResetMessage] = useState<string>("");
  const [role, setRole] = useState<string>("");
  const [cartItemCount, setCartItemCount] = useState<number>(0);
  const [showStoreIcon, setShowStoreIcon] = useState<boolean>(false);
  const [categories, setCategories] = useState<any[]>([]);

  const navigate = useNavigate();
  const userMenuRef = useRef<HTMLDivElement | null>(null);
  const notificationsRef = useRef<HTMLDivElement | null>(null);
  const categoriesRef = useRef<HTMLDivElement | null>(null);
  const advancedSearchRef = useRef<HTMLDivElement | null>(null);

  // Méthode de recherche
// Modification de la méthode performSearch dans Navbar.tsx

const performSearch = async (query: string) => {
  console.log("Début de la recherche pour:", query);

  // Limiter la longueur de recherche
  if (query.length > 50) {
    console.warn("Requête de recherche trop longue");
    setSearchResults([]);
    return;
  }

  if (!query.trim()) {
    console.log("Requête vide");
    setSearchResults([]);
    return;
  }

  setIsSearching(true);
  try {
    let results: SearchResult[] = [];

    // 🚀 NOUVELLE FONCTIONNALITÉ : Recherche intelligente avec Gemini
    try {
      // Appeler l'API de recherche intelligente
     const intelligentSearchResponse = await fetch(
  `http://localhost:8080/api/produits/search-intelligent?query=${encodeURIComponent(query)}`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          }
        }
      );

      if (intelligentSearchResponse.ok) {
        const intelligentData = await intelligentSearchResponse.json();
        console.log("Résultats Gemini AI:", intelligentData);

        // Si la recherche intelligente a trouvé des résultats
        if (intelligentData.results && intelligentData.results.length > 0) {
          // Convertir les résultats au format attendu
          const geminiResults = intelligentData.results.map((item: any) => ({
            id: item.id,
            name: item.nomProduit,
            type: 'product' as const,
            description: item.description,
            price: item.prix,
            imageUrl: item.imageUrl || item.images?.[0]?.url
          }));

          results = [...results, ...geminiResults];
          
          // Ajouter un indicateur que c'est une recherche IA
          console.log(`✨ Recherche IA activée - ${geminiResults.length} produits trouvés`);
        }
      }
    } catch (geminiError) {
      console.error("Erreur Gemini AI, fallback vers recherche classique:", geminiError);
    }

    // Continuer avec la recherche classique pour les autres types
    const endpoints = [];
    
    // Recherche de catégories
    if (searchType === "all" || searchType === "category") {
      endpoints.push({ 
        url: `/api/categories/search?nom=${encodeURIComponent(query)}`, 
        type: 'category',
        mapper: (item: any) => ({
          id: item.idCategorie,
          name: item.nom,
          type: 'category',
          description: item.description,
          imageUrl: item.imageUrl
        })
      });
    }
    
    // Recherche de boutiques
    if (searchType === "all" || searchType === "boutique") {
      endpoints.push({ 
        url: `/api/boutiques?search=${encodeURIComponent(query)}`, 
        type: 'boutique',
        mapper: (item: any) => ({
          id: item.id_boutique,
          name: item.nom,
          type: 'boutique',
          description: item.description,
          imageUrl: item.logoUrl || item.imageUrl
        })
      });
    }

    // Effectuer des recherches parallèles pour les autres types
    const searchPromises = endpoints.map(async (endpoint) => {
      try {
        const response = await fetch(endpoint.url);
        if (response.ok) {
          const data = await response.json();
          return data.map(endpoint.mapper);
        }
        return [];
      } catch (error) {
        console.error(`Erreur lors de la recherche ${endpoint.type}:`, error);
        return [];
      }
    });

    // Combiner tous les résultats
    const searchResults = await Promise.all(searchPromises);
    const additionalResults = searchResults.flat();
    
    // Fusionner avec les résultats Gemini (éviter les doublons)
    additionalResults.forEach(result => {
      if (!results.some(r => r.id === result.id && r.type === result.type)) {
        results.push(result);
      }
    });

    // Limiter à 10 résultats
    results = results.slice(0, 10);

    setSearchResults(results);
  } catch (error) {
    console.error('Erreur lors de la recherche globale:', error);
    setSearchResults([]);
  } finally {
    setIsSearching(false);
  }
};

// Ajouter un indicateur visuel pour la recherche IA dans renderSearchResults
const renderSearchResults = () => {
  if (!searchResults.length || !searchQuery) return null;

  return (
    <div 
      ref={searchResultsRef} 
      className="absolute top-full left-0 right-0 mt-1 bg-white rounded-lg shadow-lg border border-gray-200 z-50 max-h-96 overflow-y-auto"
    >
      {/* Indicateur de recherche IA */}
      <div className="px-4 py-2 bg-teal-50 border-b border-teal-100 flex items-center justify-between">
        <span className="text-sm text-teal-700">
          🤖 Recherche intelligente activée
        </span>
        <span className="text-xs text-teal-600">
          Powered by Gemini AI
        </span>
      </div>

      {searchResults.map((result) => (
        <div
          key={`${result.type}-${result.id}`}
          className="px-4 py-3 hover:bg-gray-50 cursor-pointer border-b border-gray-100 last:border-b-0 flex items-center"
          onClick={() => handleResultClick(result)}
        >
          {result.imageUrl && (
            <img 
              src={result.imageUrl} 
              alt={result.name} 
              className="w-12 h-12 object-cover rounded-md mr-4" 
            />
          )}
          <div className="flex-1">
            <div className="font-medium text-gray-900">{result.name}</div>
            <div className="text-sm text-gray-500">
              {result.type === 'product' && result.price ? 
                `${result.price} MAD` : 
                result.type === 'reduction' ? 
                `${result.pourcentage_reduction}% de réduction` : 
                result.type}
            </div>
            {result.description && (
              <div className="text-sm text-gray-500 truncate mt-1">
                {result.description}
              </div>
            )}
          </div>
          {/* Badge IA pour les produits trouvés par Gemini */}
          {result.type === 'product' && (
            <div className="ml-2">
              <span className="text-xs bg-teal-100 text-teal-700 px-2 py-1 rounded-full">
                IA
              </span>
            </div>
          )}
        </div>
      ))}
      
      {isSearching && (
        <div className="px-4 py-3 text-center text-gray-500">
          <div className="flex items-center justify-center">
            <div className="animate-spin rounded-full h-5 w-5 border-t-2 border-b-2 border-teal-500 mr-2"></div>
            Recherche intelligente en cours...
          </div>
        </div>
      )}
      
      {!isSearching && searchResults.length === 0 && (
        <div className="px-4 py-3 text-center text-gray-500">
          Aucun résultat trouvé
        </div>
      )}
      
      <div 
        className="p-3 text-center text-teal-600 font-medium cursor-pointer hover:bg-gray-50 border-t border-gray-100"
        onClick={() => {
          handleSearchSubmit(new Event('submit') as any);
        }}
      >
        Voir tous les résultats pour "{searchQuery}"
      </div>
    </div>
  );
};

  // Gestion du changement de recherche
  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const query = e.target.value;
    setSearchQuery(query);

    // Annuler le timeout précédent
    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current);
    }

    // Déclencher une nouvelle recherche après 300ms de délai
    searchTimeoutRef.current = setTimeout(() => {
      performSearch(query);
    }, 300);
  };

  // Gestion de la soumission de recherche
  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // Construire l'URL de recherche avec les filtres
    let searchUrl = `/search?q=${encodeURIComponent(searchQuery)}`;
    
    if (searchType !== "all") {
      searchUrl += `&type=${searchType}`;
    }
    
    if (selectedCategory) {
      searchUrl += `&category=${selectedCategory}`;
    }
    
    // Naviguer vers la page de résultats
    navigate(searchUrl);
    
    // Fermer les résultats et réinitialiser la recherche
    setSearchResults([]);
    setShowAdvancedSearch(false);
    
    // Appeler le callback onSearch si fourni
    if (onSearch) {
      onSearch(searchQuery);
    }
  };

  // Gestion du clic sur un résultat
  const handleResultClick = (result: SearchResult) => {
    setSearchResults([]);
    setSearchQuery('');
    
    switch (result.type) {
      case 'category':
        navigate(`/search?category=${result.id}&type=category`, { 
          state: { 
            categoryDetails: result 
          } 
        });
        break;
      case 'product':
        navigate(`/produits/${result.id}`, { 
          state: { 
            productDetails: result 
          } 
        });
        break;
      case 'boutique':
        navigate(`/boutiques/${result.id}`, { 
          state: { 
            boutiqueDetails: result 
          } 
        });
        break;
      case 'reduction':
        navigate(`/promotions/${result.id}`, { 
          state: { 
            reductionDetails: result 
          } 
        });
        break;
    }
  };

  // Rendu des résultats de recherche


  // Rendu du panneau de recherche avancée
  const renderAdvancedSearch = () => {
    if (!showAdvancedSearch) return null;

    return (
      <div 
        ref={advancedSearchRef} 
        className="absolute top-full left-0 right-0 mt-1 bg-white rounded-lg shadow-lg border border-gray-200 z-50 p-4"
      >
        <div className="mb-3">
          <label className="block text-sm font-medium text-gray-700 mb-1">Type de recherche</label>
          <select 
            className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-teal-500"
            value={searchType}
            onChange={(e) => setSearchType(e.target.value)}
          >
            <option value="all">Tous</option>
            <option value="product">Produits</option>
            <option value="category">Catégories</option>
            <option value="boutique">Boutiques</option>
            <option value="reduction">Promotions</option>
          </select>
        </div>
        
        {(searchType === "all" || searchType === "product") && (
          <div className="mb-3">
            <label className="block text-sm font-medium text-gray-700 mb-1">Catégorie</label>
            <select 
              className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-teal-500"
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
            >
              <option value="">Toutes les catégories</option>
              {categories.map((category) => (
                <option key={category.idCategorie} value={category.idCategorie}>
                  {category.nom}
                </option>
              ))}
            </select>
          </div>
        )}
        
        <div className="flex justify-end">
          <button 
            onClick={() => setShowAdvancedSearch(false)}
            className="px-4 py-2 text-gray-500 mr-2"
          >
            Annuler
          </button>
          <button 
            onClick={() => handleSearchSubmit(new Event('submit') as any)}
            className="px-4 py-2 bg-teal-500 text-white rounded-md hover:bg-teal-600"
          >
            Rechercher
          </button>
        </div>
      </div>
    );
  };
  
  // Fonction pour basculer l'affichage de la recherche avancée
  const toggleAdvancedSearch = () => {
    setShowAdvancedSearch(!showAdvancedSearch);
    if (showAdvancedSearch) {
      setSearchResults([]); // Masquer les résultats de recherche
    }
  };

  // Fonction pour naviguer vers les produits d'une catégorie
  const handleCategoryClick = (categoryId: any, categoryName: any) => {
    navigate(`/search?category=${categoryId}&type=product`);
    setIsCategoriesOpen(false);
  };

  // Hook d'effet pour l'authentification
  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (currentUser) => {
      setUser(currentUser);
      if (currentUser) {
        const userRef = doc(db, "users", currentUser.uid);
        const userDoc = await getDoc(userRef);
        if (userDoc.exists()) {
          const userData = userDoc.data();
          setRole(userData.role);
          setShowStoreIcon(userData.role === "vendeur");
        }
      }
    });

    return () => unsubscribe();
  }, []);
// Fonction pour obtenir le nombre d'articles dans le panier
const getCartItemCount = (userId: string | undefined) => {
  if (!userId) return 0;
  
  try {
    // Utiliser la même clé de stockage que dans shopUtils
    const userCartKey = `${CART_STORAGE_KEY}_${userId}`;
    const cartJSON = localStorage.getItem(userCartKey);
    
    if (!cartJSON) return 0;
    
    const cart = JSON.parse(cartJSON);
    
    // Si le panier est un tableau, comptez les quantités
    if (Array.isArray(cart)) {
      return cart.reduce((total, item) => total + (item.quantite || 1), 0);
    }
    
    return 0;
  } catch (error) {
    console.error('Erreur lors de la récupération du nombre d\'articles dans le panier:', error);
    return 0;
  }
};





  // Récupération des catégories
  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const response = await fetch('/api/categories');
        if (response.ok) {
          const data = await response.json();
          setCategories(data);
        } else {
          console.error('Erreur lors de la récupération des catégories:', response.statusText);
        }
      } catch (error) {
        console.error('Erreur lors de la récupération des catégories:', error);
      }
    };

    fetchCategories();
  }, []);

  // Méthodes d'interaction
  const toggleCategories = () => {
    setIsCategoriesOpen(!isCategoriesOpen);
  };

  const toggleMobileMenu = () => {
    setIsMobileMenuOpen(!isMobileMenuOpen);
  };

  const toggleNotifications = (e?: React.MouseEvent) => {
    if (e) {
      e.stopPropagation();
    }
    
    if (!user) {
      openQuickSignInModal();
      return;
    }
    setShowNotificationsPanel(!showNotificationsPanel);
    // Fermer les autres panneaux si ouverts
    if (showFavoritesPanel) setShowFavoritesPanel(false);
    if (showCartPanel) setShowCartPanel(false);
  };

  const openSignInModal = () => {
    setIsSignInModalOpen(true);
  };

  const closeSignInModal = () => {
    setIsSignInModalOpen(false);
    setResetMessage("");
  };

  const openQuickSignInModal = () => {
    setIsQuickSignInModalOpen(true);
  };

  const closeQuickSignInModal = () => {
    setIsQuickSignInModalOpen(false);
  };

  const toggleFavoritesPanel = () => {
    if (!user) {
      openQuickSignInModal();
      return;
    }
    setShowFavoritesPanel(!showFavoritesPanel);
    // Fermer les autres panneaux si ouverts
    if (showCartPanel) setShowCartPanel(false);
    if (showNotificationsPanel) setShowNotificationsPanel(false);
  };

  const toggleCartPanel = () => {
    if (!user) {
      openQuickSignInModal();
      return;
    }
    setShowCartPanel(!showCartPanel);
    // Fermer les autres panneaux si ouverts
    if (showFavoritesPanel) setShowFavoritesPanel(false);
    if (showNotificationsPanel) setShowNotificationsPanel(false);
  };

  // Méthodes d'authentification
  const handleLogout = async () => {
    try {
      await signOut(auth);
      setShowUserMenu(false);
      navigate("/");
    } catch (error) {
      console.error("Erreur lors de la déconnexion:", error);
    }
  };

  const handleSignIn = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await signInWithEmailAndPassword(auth, email, password);
      closeSignInModal();
      navigate("/");
    } catch (error) {
      console.error("Erreur de connexion:", error);
    }
  };

  const handleGoogleLogin = async () => {
    try {
      const provider = new GoogleAuthProvider();
      await signInWithPopup(auth, provider);
      closeSignInModal();
      navigate("/");
    } catch (error) {
      console.error("Google Sign-In Error:", error);
    }
  };

  const handleForgotPassword = async () => {
    try {
      await sendPasswordResetEmail(auth, email);
      setResetMessage(
        "Vérifiez votre boîte de réception pour réinitialiser votre mot de passe."
      );
    } catch (error) {
      console.error(
        "Erreur lors de l'envoi de l'email de réinitialisation:",
        error
      );
      setResetMessage("Erreur lors de l'envoi de l'email de réinitialisation.");
    }
  };

  const handleSignUpClick = () => {
    closeSignInModal();
    navigate("/sign-up");
  };

  const handleRoleChange = async (newRole: string) => {
    setRole(newRole);
    console.log(`Rôle changé en: ${newRole}`);

    if (user) {
      try {
        const userRef = doc(db, "users", user.uid);

        // Vérifier si le document existe
        const docSnap = await getDoc(userRef);

        if (docSnap.exists()) {
          // Le document existe, mettre à jour
          await updateDoc(userRef, { role: newRole });
        } else {
          // Le document n'existe pas, le créer
          await setDoc(userRef, {
            role: newRole,
            email: user.email,
            displayName: user.displayName || "",
            photoURL: user.photoURL || "",
            createdAt: new Date(),
          });
        }

        // Appeler la fonction updateUserRole lors du changement de rôle
        await updateUserRole(user.uid, newRole);

        setShowStoreIcon(newRole === "vendeur");

        if (newRole === "vendeur") {
          // Naviguer vers la page de compte avec redirection du tableau de bord
          navigate("/account?redirect=dashboard");
        } else if (newRole === "livreur") {
          // Naviguer vers la page de compte avec redirection du compte
          navigate("/account?redirect=account");
        }
      } catch (error) {
        console.error("Erreur lors de la mise à jour du rôle:", error);
        alert("Erreur lors de la mise à jour du rôle. Veuillez réessayer.");
      }
    }
  };

  // Gestion des clics en dehors des éléments
  const handleClickOutside = (event: MouseEvent) => {
    const target = event.target as HTMLElement;
    
    // Fermeture des autres menus et panneaux
    if (showUserMenu && !target.closest(".user-menu")) {
      setShowUserMenu(false);
    }
    if (showNotificationsPanel && !target.closest(".notifications-menu")) {
      setShowNotificationsPanel(false);
    }
    if (isCategoriesOpen && !target.closest(".categories-dropdown")) {
      setIsCategoriesOpen(false);
    }
    
    // Fermeture de la recherche avancée
    if (showAdvancedSearch && !target.closest(".advanced-search-container") && !target.closest(".advanced-search-toggle")) {
      setShowAdvancedSearch(false);
    }
    
    // Fermeture des résultats de recherche
    if (searchResults.length > 0 && !target.closest(".search-container")) {
      setSearchResults([]);
    }
  };

  useEffect(() => {
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [showUserMenu, showNotificationsPanel, isCategoriesOpen, searchResults, showAdvancedSearch]);
// Effet pour gérer le nombre d'articles dans le panier
useEffect(() => {
  const updateCartCount = () => {
    if (user) {
      // Utilisez la fonction existante de shopUtils ou votre fonction mise à jour
      const count = getCartItemCount(user.uid);
      setCartItemCount(count);
    } else {
      setCartItemCount(0);
    }
  };
  
  // Mettre à jour initialement
  updateCartCount();
  
  // Écouter les événements de mise à jour du panier
  const handleCartUpdate = () => {
    updateCartCount();
  };
  
  window.addEventListener('cartUpdated', handleCartUpdate);
  
  return () => {
    window.removeEventListener('cartUpdated', handleCartUpdate);
  };
}, [user]);
  // Rendu du menu utilisateur
  const renderUserMenu = () => {
    if (!showUserMenu) return null;

    return (
      <div
        ref={userMenuRef}
        className="absolute right-0 top-full mt-2 w-64 bg-white rounded-lg shadow-lg py-2 z-50"
      >
        <Link to="/account">
          <div className="px-4 py-3 border-b border-gray-100 hover:bg-gray-50 rounded-t-lg">
            <div className="flex items-center">
              <div className="bg-teal-100 rounded-full h-10 w-10 flex items-center justify-center text-teal-500 mr-3">
                <UserIcon className="h-5 w-5" />
              </div>
              <div>
                <div className="font-medium">
                  {user?.displayName || user?.email || "Utilisateur"}
                </div>
                <div className="text-xs text-gray-500">Voir votre profil</div>
              </div>
            </div>
          </div>
        </Link>

        <Link
          to="/achats"
          className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
          onClick={() => setShowUserMenu(false)}
        >
          <ShoppingBag className="h-4 w-4 mr-3 text-gray-400" />
          Achats et avis
        </Link>

        <Link
          to="/messages"
          className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
          onClick={() => setShowUserMenu(false)}
        >
          <MessageSquare className="h-4 w-4 mr-3 text-gray-400" />
          Messages
        </Link>

        <Link
          to="/client-profile-editor"
          className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
          onClick={() => setShowUserMenu(false)}
        >
          <Settings className="h-4 w-4 mr-3 text-gray-400" />
          Paramètres du compte
        </Link>

        <div className="border-t border-gray-100 my-1"></div>

        {/* Afficher les options de rôle uniquement si aucun rôle n'est défini */}
        {role !== "vendeur" && role !== "livreur" && (
          <>
            <button
              onClick={() => handleRoleChange("vendeur")}
              className="flex items-center w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
            >
              <Store className="h-4 w-4 mr-3 text-gray-400" />
              Vendre avec HERE
            </button>

            <button
              onClick={() => handleRoleChange("livreur")}
              className="flex items-center w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
            >
              <Store className="h-4 w-4 mr-3 text-gray-400" />
              Livrer avec HERE
            </button>
          </>
        )}

        <div className="border-t border-gray-100 my-1"></div>

        <button
          onClick={handleLogout}
          className="flex items-center w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
        >
          <LogOut className="h-4 w-4 mr-3 text-gray-400" />
          Se déconnecter
        </button>
      </div>
    );
  };

  // Rendu du dropdown des catégories
  const renderCategoriesDropdown = () => {
    if (!isCategoriesOpen) return null;

    return (
      <div 
        ref={categoriesRef} 
        className="absolute left-0 mt-2 w-64 bg-white rounded-lg shadow-lg py-2 z-50 max-h-96 overflow-y-auto"
      >
        {categories && categories.length > 0 ? (
          categories.map((category: any) => (
            <div
              key={category.idCategorie}
              className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 cursor-pointer"
              onClick={() => handleCategoryClick(category.idCategorie, category.nom)}
            >
              {category.nom}
            </div>
          ))
        ) : (
          <div className="px-4 py-2 text-sm text-gray-500">
            Chargement des catégories...
          </div>
        )}
      </div>
    );
  };

  // Booléen pour les notifications non lues
  const hasUnreadNotifications = true;

  return (
    <div className="relative all">
      <header className="w-full px-4 md:px-12 pt-4 flex flex-wrap items-center justify-between bg-white">
        {/* Section mobile */}
        <div className="lg:hidden flex items-center space-x-3">
          <button
            className="block lg:hidden hover:bg-gray-100 p-2 rounded-full transition-colors"
            onClick={toggleMobileMenu}
          >
            {isMobileMenuOpen ? (
              <X className="h-6 w-6" />
            ) : (
              <Menu className="h-6 w-6" />
            )}
          </button>

          <Link to="/" className="flex items-center">
            <img 
              src="/assets/img/HERE.jpg" 
              alt="HERE Logo" 
              className="h-10 w-auto object-contain 
                hover:scale-105 
                transition-transform 
                duration-300 
                ease-in-out 
                rounded-lg 
                shadow-sm 
                hover:shadow-md"
            />
          </Link>

          {user ? (
            <div className="relative flex items-center user-menu">
              {role === "vendeur" && (
                <Link
                  to="/account/stores"
                  className="text-teal-600 flex items-center mr-3"
                >
                  <Store className="h-6 w-6" />
                </Link>
              )}
              <button
                onClick={() => setShowUserMenu(!showUserMenu)}
                className="flex items-center space-x-2 text-sm hover:text-teal-600 transition-colors"
              >
                <div className="bg-teal-100 rounded-full h-8 w-8 flex items-center justify-center text-teal-500">
                  <UserIcon className="h-4 w-4" />
                </div>
              </button>
              {renderUserMenu()}
            </div>
          ) : (
            <div className="space-x-4">
              <button
                className="text-sm hover:text-teal-600 transition-colors"
                onClick={openSignInModal}
              >
                Sign in
              </button>
              <Link
                to="/sign-up"
                className="text-sm Signup rounded-full p-1 px-3 hover:bg-teal-600 transition-colors"
              >
                Sign up
              </Link>
            </div>
          )}
        </div>

        {/* Section desktop logo et catégories */}
        <div className="hidden lg:flex items-center space-x-2 lg:space-x-4 ml-[-20px]">
          <Link to="/" className="flex items-center">
            <img 
              src="/src/assets/img/HEREE.png" 
              alt="HERE Logo" 
              className="h-20 w-auto object-contain"
            />
          </Link>

          <div className="relative categories-dropdown">
            <button
              onClick={toggleCategories}
              className="flex items-center space-x-1 text-sm hover:text-teal-600 transition-colors"
            >
              <Menu className="h-5 w-5" />
              <span>Categories</span>
              <ChevronDown className={`h-4 w-4 transition-transform ${isCategoriesOpen ? 'rotate-180' : ''}`} />
            </button>
            {renderCategoriesDropdown()}
          </div>
        </div>

        {/* Barre de recherche desktop avec recherche avancée */}
        <form
          onSubmit={handleSearchSubmit}
          className="hidden lg:block flex-grow relative mx-4 search-container advanced-search-container"
        >
          <div className="flex">
            <input
              type="text"
              placeholder="Rechercher produits, catégories, boutiques..."
              className="w-full border border-gray-300 rounded-l-full px-4 py-2 focus:outline-none focus:ring-2 focus:ring-teal-500"
              value={searchQuery}
              onChange={handleSearchChange}
            />
            <button
              type="button"
              onClick={toggleAdvancedSearch}
              className="bg-gray-100 text-gray-600 px-3 flex items-center justify-center hover:bg-gray-200 transition-colors advanced-search-toggle"
              title="Recherche avancée"
            >
              <Filter className="h-5 w-5" />
            </button>
            <button
              type="submit"
              className="bg-teal-500 text-white rounded-r-full px-6 flex items-center justify-center hover:bg-teal-600 transition-colors"
            >
              <Search className="h-5 w-5" />
            </button>
          </div>
          {renderSearchResults()}
          {renderAdvancedSearch()}
        </form>

        {/* Section desktop navigation et authentification */}
        <div className="hidden lg:flex items-center space-x-4">
          {user ? (
            <div className="relative flex items-center user-menu">
              {role === "vendeur" && (
                <Link
                  to="/account/stores"
                  className="text-teal-600 flex items-center mr-3"
                >
                  <Store className="h-6 w-6" />
                </Link>
              )}
              <button
                onClick={() => setShowUserMenu(!showUserMenu)}
                className="flex items-center space-x-2 text-sm hover:text-teal-600 transition-colors"
              >
                <div className="bg-teal-100 rounded-full h-8 w-8 flex items-center justify-center text-teal-500">
                  <UserIcon className="h-4 w-4" />
                </div>
              </button>
              {renderUserMenu()}
            </div>
          ) : (
            <div className="space-x-4">
              <Link
                to="/sign-up"
                className="text-sm Signup rounded-full p-1 px-3 hover:bg-teal-600 transition-colors"
              >
                Sign up
              </Link>
              <button
                className="text-sm hover:text-teal-600 transition-colors"
                onClick={openSignInModal}
              >
                Sign in
              </button>
            </div>
          )}

          <div className="flex space-x-3 px-5">
            <button
              className="flex-initial p-2 hover:bg-gray-100 rounded-full transition-colors"
              onClick={toggleFavoritesPanel}
            >
              <Heart className="h-5 w-5" />
            </button>

           {/* Notification Bell */}
          <div className="relative notifications-menu">
            <button
              className="flex-initial p-2 hover:bg-gray-100 rounded-full transition-colors"
              onClick={toggleNotifications}
            >
              <Bell className="h-5 w-5" />
              {hasUnreadNotifications && (
                <span className="absolute -top-0 -right-0 bg-teal-500 text-white rounded-full h-3 w-3 flex items-center justify-center text-xs"></span>
              )}
            </button>
            {showNotificationsPanel && (
              <NotificationPanel
                isOpen={showNotificationsPanel}
                onClose={() => setShowNotificationsPanel(false)}
                vendeurId={role === "vendeur" ? user?.uid : undefined}
              />
            )}
          </div>

          <button
  className="flex-initial relative p-2 hover:bg-gray-100 rounded-full transition-colors"
  onClick={toggleCartPanel}
>
  <ShoppingCart className="h-5 w-5" />
  {cartItemCount > 0 && (
    <span className="absolute -top-2 -right-2 bg-teal-500 text-white rounded-full h-5 w-5 flex items-center justify-center text-xs">
      {cartItemCount > 99 ? '99+' : cartItemCount}
    </span>
  )}
</button>
        </div>
      </div>

        {/* Barre de recherche mobile avec recherche avancée */}
        <div className="w-full mt-4 lg:hidden">
          <form onSubmit={handleSearchSubmit} className="relative search-container advanced-search-container">
            <div className="flex">
              <input
                type="text"
                placeholder="Rechercher produits, catégories, boutiques..."
                className="w-full border border-gray-300 rounded-l-full px-4 py-2 focus:outline-none focus:ring-2 focus:ring-teal-500"
                value={searchQuery}
                onChange={handleSearchChange}
              />
              <button
                type="button"
                onClick={toggleAdvancedSearch}
                className="bg-gray-100 text-gray-600 px-3 flex items-center justify-center hover:bg-gray-200 transition-colors advanced-search-toggle"
                title="Recherche avancée"
              >
                <Filter className="h-5 w-5" />
              </button>
              <button
                type="submit"
                className="bg-teal-500 text-white rounded-r-full px-6 flex items-center justify-center hover:bg-teal-600 transition-colors"
              >
                <Search className="h-5 w-5" />
              </button>
            </div>
            {renderSearchResults()}
            {renderAdvancedSearch()}
          </form>
        </div>
      </header>

      {/* Mobile Menu (affiché uniquement lorsque isMobileMenuOpen est true) */}
      {isMobileMenuOpen && (
        <div className="lg:hidden bg-white shadow-lg z-30 absolute w-full">
          <nav className="py-3">
            <ul className="space-y-1">
              {categories.slice(0, 10).map((category: any) => (
                <li key={category.idCategorie}>
                  <div
                    className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 cursor-pointer"
                    onClick={() => handleCategoryClick(category.idCategorie, category.nom)}
                  >
                    {category.nom}
                  </div>
                </li>
              ))}
              <li className="border-t border-gray-100 my-1"></li>
              <li>
                <div
                  className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 cursor-pointer"
                  onClick={toggleFavoritesPanel}
                >
                  <Heart className="h-4 w-4 mr-3" />
                  Mes favoris
                </div>
              </li>
              <li>
              <div
  className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 cursor-pointer"
  onClick={toggleCartPanel}
>
  <div className="relative">
    <ShoppingCart className="h-4 w-4 mr-3" />
    {cartItemCount > 0 && (
      <span className="absolute -top-2 -right-0 bg-teal-500 text-white rounded-full h-4 w-4 flex items-center justify-center text-xs">
        {cartItemCount > 9 ? '9+' : cartItemCount}
      </span>
    )}
  </div>
  Mon panier
</div>
              </li>
              <li>
                <div
                  className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 cursor-pointer"
                  onClick={(e) => {
                    e.stopPropagation();
                    toggleNotifications(e);
                  }}
                >
                  <Bell className="h-4 w-4 mr-3" />
                  Notifications
                </div>
              </li>
            </ul>
          </nav>
        </div>
      )}

      {/* Quick Sign In Modal */}
      <QuickSignInModal
        isOpen={isQuickSignInModalOpen}
        onClose={closeQuickSignInModal}
        onSignIn={openSignInModal}
      />

      {/* Main Sign In Modal */}
      {isSignInModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div
            className="absolute inset-0 modalS bg-opacity-5"
            onClick={closeSignInModal}
          ></div>
          <div className="relative bg-white rounded-lg w-full max-w-md p-6 mx-4">
            <button
              onClick={closeSignInModal}
              className="absolute top-0 -right-11 text-amber-50 hover:text-gray-900"
              aria-label="Close"
            >
              <X size={30} />
            </button>

            <div className="flex justify-between items-center mb-6">
              <h2 className="text-2xl font-bold">Sign in</h2>
              <button
                onClick={handleSignUpClick}
                className="px-4 py-2 border border-gray-300 rounded-full text-sm hover:bg-gray-50 hover:border-green-500"
              >
                Sign up
              </button>
            </div>

            <form onSubmit={handleSignIn}>
              <div className="mb-4">
                <label
                  htmlFor="email"
                  className="block text-sm font-medium text-gray-700 mb-1"
                >
                  Email Address
                </label>
                <input
                  id="email"
                  type="email"
                  placeholder="johnsmith@gmail.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full border Inputs rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-teal-500"
                  required
                />
              </div>

              <div className="mb-4">
                <label
                  htmlFor="password"
                  className="block text-sm font-medium text-gray-700 mb-1"
                >
                  Password
                </label>
                <input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full border Inputs rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-teal-500"
                  required
                />
              </div>

              <div className="flex justify-between items-center mb-6">
                <div className="flex items-center">
                  <div className="relative flex items-center">
                    <input
                      id="staySignedIn"
                      type="checkbox"
                      checked={staySignedIn}
                      onChange={() => setStaySignedIn(!staySignedIn)}
                      className="opacity-0 absolute h-4 w-4 cursor-pointer"
                    />
                    <div
                      className={`w-4 h-4 border ${
                        staySignedIn
                          ? "bg-teal-500 border-teal-500"
                          : "border-gray-300"
                      } rounded flex items-center justify-center`}
                    >
                      {staySignedIn && (
                        <div className="text-white">
                          <Check size={12} />
                        </div>
                      )}
                    </div>
                    <label
                      htmlFor="staySignedIn"
                      className="ml-2 block text-sm text-gray-700 cursor-pointer"
                    >
                      Stay signed in
                    </label>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={handleForgotPassword}
                  className="text-sm text-gray-600 hover:text-teal-500 bg-transparent border-0"
                >
                  Forgot your password?
                </button>
              </div>

              <button
                type="submit"
                className="w-full bg-teal-500 text-white py-3 rounded-full hover:bg-teal-600 transition-colors"
              >
                Sign In
              </button>
            </form>

            {resetMessage && (
              <div className="mt-4 text-sm text-gray-600">{resetMessage}</div>
            )}

            <div className="my-6 flex items-center">
              <div className="flex-grow border-t border-gray-300"></div>
              <span className="px-3 text-gray-500 text-sm">OR</span>
              <div className="flex-grow border-t border-gray-300"></div>
            </div>

            <div className="space-y-3">
              <button
                type="button"
                onClick={handleGoogleLogin}
                className="w-full flex items-center justify-center space-x-2 border border-gray-300 rounded-full py-2 px-4 hover:bg-gray-50 transition-colors"
              >
                <span className="text-red-500">
                  <svg
                    viewBox="0 0 24 24"
                    width="24"
                    height="24"
                    xmlns="http://www.w3.org/2000/svg"
                  >
                    <g transform="matrix(1, 0, 0, 1, 27.009001, -39.238998)">
                      <path
                        fill="#4285F4"
                        d="M -3.264 51.509 C -3.264 50.719 -3.334 49.969 -3.454 49.239 L -14.754 49.239 L -14.754 53.749 L -8.284 53.749 C -8.574 55.229 -9.424 56.479 -10.684 57.329 L -10.684 60.329 L -6.824 60.329 C -4.564 58.239 -3.264 55.159 -3.264 51.509 Z"
                      />
                      <path
                        fill="#34A853"
                        d="M -14.754 63.239 C -11.514 63.239 -8.804 62.159 -6.824 60.329 L -10.684 57.329 C -11.764 58.049 -13.134 58.489 -14.754 58.489 C -17.884 58.489 -20.534 56.379 -21.484 53.529 L -25.464 53.529 L -25.464 56.619 C -23.494 60.539 -19.444 63.239 -14.754 63.239 Z"
                      />
                      <path
                        fill="#FBBC05"
                        d="M -21.484 53.529 C -21.734 52.809 -21.864 52.039 -21.864 51.239 C -21.864 50.439 -21.724 49.669 -21.484 48.949 L -21.484 45.859 L -25.464 45.859 C -26.284 47.479 -26.754 49.299 -26.754 51.239 C -26.754 53.179 -26.284 54.999 -25.464 56.619 L -21.484 53.529 Z"
                      />
                      <path
                        fill="#EA4335"
                        d="M -14.754 43.989 C -12.984 43.989 -11.404 44.599 -10.154 45.789 L -6.734 42.369 C -8.804 40.429 -11.514 39.239 -14.754 39.239 C -19.444 39.239 -23.494 41.939 -25.464 45.859 L -21.484 48.949 C -20.534 46.099 -17.884 43.989 -14.754 43.989 Z"
                      />
                    </g>
                  </svg>
                </span>
                <span>Login with google</span>
              </button>
            </div>
          </div>
        </div>
      )}

<FavoritesPanel
  isOpen={showFavoritesPanel}
  onClose={() => setShowFavoritesPanel(false)}
  userId={user?.uid}
  authToken={user?.getIdToken ? user.getIdToken() : undefined}
/>
<CartPanel
  isOpen={showCartPanel}
  onClose={() => setShowCartPanel(false)}
  userId={user?.uid}
  authToken={user?.getIdToken ? user.getIdToken() : undefined}
/>
      
     
    </div>
  );
};

export default Navbar;