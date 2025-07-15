import React, { useState, useEffect, useCallback } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import axios from "axios";
import {
  Plus,
  Search,
  Edit,
  Trash2,
  ChevronLeft,
  ChevronRight,
  AlertTriangle,
  Package,
  Store,
  PlusCircle,
  MinusCircle,
  ChevronDown,
  ChevronUp,
  FileText,
  Settings,
  ArrowLeft,
  Layers,
  X,
  DollarSign
} from "lucide-react";
import { auth } from "../../../config/Firebase";
import DashboardNavigation from "./DashboardNavigation";
import StockForm from "./StockForm";
import ProductForm from "./ProductForm";
import TransactionHistory from "./TransactionHistory";

const StockManagement = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [loading, setLoading] = useState(true);
  const [stocks, setStocks] = useState([]);
  const [stores, setStores] = useState([]);
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [reductions, setReductions] = useState([]);
  const [expandedStockId, setExpandedStockId] = useState(null);
  const [showStockForm, setShowStockForm] = useState(false);
  const [showProductForm, setShowProductForm] = useState(false);
  const [selectedStock, setSelectedStock] = useState(null);
  const [editingStock, setEditingStock] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [storeFilter, setStoreFilter] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage] = useState(8);
  const [showTransactionModal, setShowTransactionModal] = useState(false);
  const [showTransactionHistory, setShowTransactionHistory] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [transactionType, setTransactionType] = useState("add"); // "add" or "remove"
  const [transactionQuantity, setTransactionQuantity] = useState(1);
  const [transactions, setTransactions] = useState([]);
  const [error, setError] = useState(null);
  
  
  // État pour stocker l'ID du vendeur connecté
  const [currentVendeurId, setCurrentVendeurId] = useState(null);
  
  // État pour les statistiques par stock
  const [stockStatsByStock, setStockStatsByStock] = useState({});
  
  // État pour stocker les capacités maximales des stocks
  const [stocksMaxCapacity, setStocksMaxCapacity] = useState({});

  // États pour stocker les revenus globaux
  const [productRevenues, setProductRevenues] = useState({});
  const [stockRevenues, setStockRevenues] = useState({});
  
  // État pour forcer le rendu après une transaction
  const [forceUpdate, setForceUpdate] = useState(0);

  // Déterminer si une transaction est une entrée (pour la réutilisation de code)
  const isAddTransaction = useCallback((transaction) => {
    if (!transaction.type) return false;
    
    const lowerType = transaction.type.toLowerCase();
    if (transaction.notes && transaction.notes.toLowerCase().includes("ajout")) {
      return true;
    }
    
    return lowerType.includes("add") || 
           lowerType.includes("ajout") || 
           lowerType.includes("entr") || 
           lowerType.includes("depot") ||
           lowerType.includes("stock");
  }, []);

  // Récupérer l'ID du vendeur depuis Firebase auth
  const fetchVendeurId = async (user) => {
    if (!user) return null;
    
    try {
      // Récupérer l'ID du vendeur depuis le user de Firebase
      // Cette partie dépend de votre implémentation - ajustez selon votre structure
      // Par exemple, si vous stockez l'ID vendeur dans les claims ou dans Firestore
      const token = await user.getIdTokenResult();
      const vendeurId = token.claims.vendeurId || user.uid;
      
      console.log("ID Vendeur récupéré:", vendeurId);
      return vendeurId;
    } catch (error) {
      console.error("Erreur lors de la récupération de l'ID vendeur:", error);
      return user.uid; // Fallback sur l'UID Firebase
    }
  };

  // Charger les boutiques du vendeur connecté
  const loadBoutiques = async (vendeurId) => {
    try {
      console.log("Chargement des boutiques pour le vendeur:", vendeurId);
      
      // Utiliser l'endpoint de l'API qui filtre par vendeur
      const response = await axios.get(`/api/boutiques/vendeur/${vendeurId}`, {
        headers: {
          'X-Vendeur-ID': vendeurId
        }
      });
      
      console.log("Réponse brute des boutiques:", response);
      console.log("Données de boutiques:", response.data);
      
      // Mapper correctement les données
      const boutiquesData = response.data.map((boutique) => ({
        id: boutique.id_boutique || boutique.id, // Utiliser id_boutique ou id
        nom: boutique.nom 
      }));
      
      console.log("Boutiques mappées:", boutiquesData);
      
      if (boutiquesData.length === 0) {
        console.warn("Aucune boutique valide trouvée pour ce vendeur");
        setError("Aucune boutique disponible pour votre compte");
      }
      
      setStores(boutiquesData);
      return boutiquesData;
    } catch (error) {
      console.error("Erreur lors du chargement des boutiques:", error);
      
      // Log détaillé de l'erreur
      if (axios.isAxiosError(error)) {
        console.error('Détails de l\'erreur:', {
          status: error.response?.status,
          data: error.response?.data,
          headers: error.response?.headers
        });
      }
      
      setError("Erreur lors du chargement des boutiques");
      return [];
    }
  };

  // Check user authentication and role
  useEffect(() => {
    const unsubscribe = auth.onAuthStateChanged(async (user) => {
      if (!user) {
        navigate("/login?redirect=account/stock");
        return;
      }

      try {
        setLoading(true);
        
        // Récupérer l'ID du vendeur connecté
        const vendeurId = await fetchVendeurId(user);
        setCurrentVendeurId(vendeurId);
        
        // Charger les boutiques du vendeur
        await loadBoutiques(vendeurId);
        
        // Puis charger le reste des données
        await loadData(vendeurId);
        
        setLoading(false);
      } catch (error) {
        console.error("Error fetching data:", error);
        setError("Erreur lors du chargement des données");
        setLoading(false);
      }
    });

    return () => unsubscribe();
  }, [navigate]);

  // Recalculer les revenus lorsque les transactions changent
  useEffect(() => {
    // Seulement si nous avons des produits et des transactions
    if (products.length > 0 && transactions.length > 0) {
      console.log("Recalcul des revenus basé sur", transactions.length, "transactions");
      calculateAllRevenues();
    }
  }, [transactions, products, forceUpdate]);

  // Fonction pour calculer le revenu généré pour un produit spécifique
  const calculateProductRevenue = useCallback((productId, allTransactions) => {
    // Trouver toutes les transactions de ce produit
    const productTransactions = allTransactions.filter(t => t.productId === productId);
    
    // Calculer les sorties (ventes)
    const salesTransactions = productTransactions.filter(t => !isAddTransaction(t));
    
    // Trouver le produit
    const product = products.find(p => p.id === productId);
    const productPrice = product ? parseFloat(product.prix || 0) : 0;
    
    // Calculer revenus et nombre total de ventes
    let totalRevenue = 0;
    let totalSold = 0;
    
    salesTransactions.forEach(transaction => {
      totalSold += transaction.quantity;
      
      // Utiliser le prix unitaire de la transaction s'il existe, sinon le prix du produit
      const price = transaction.prixUnitaire ? parseFloat(transaction.prixUnitaire) : productPrice;
      totalRevenue += transaction.quantity * price;
    });
    
    return {
      totalSold,
      revenue: totalRevenue
    };
  }, [products, isAddTransaction]);

  // Fonction pour calculer tous les revenus
  const calculateAllRevenues = useCallback(() => {
    console.log("Calcul de tous les revenus...");
    // Calculer les revenus pour chaque produit
    const newProductRevenues = {};
    products.forEach(product => {
      const { revenue } = calculateProductRevenue(product.id, transactions);
      newProductRevenues[product.id] = revenue;
    });
    
    // Mettre à jour les revenus des produits
    setProductRevenues(newProductRevenues);
    
    // Calculer les revenus pour chaque stock
    const newStockRevenues = {};
    stocks.forEach(stock => {
      let stockTotalRevenue = 0;
      const stockProducts = products.filter(p => p.idStock === stock.id);
      
      stockProducts.forEach(product => {
        stockTotalRevenue += newProductRevenues[product.id] || 0;
      });
      
      newStockRevenues[stock.id] = stockTotalRevenue;
    });
    
    // Mettre à jour les revenus des stocks
    setStockRevenues(newStockRevenues);
    
    console.log("Nouveaux revenus produits:", newProductRevenues);
    console.log("Nouveaux revenus stocks:", newStockRevenues);
  }, [products, stocks, transactions, calculateProductRevenue]);

  // Calculer les statistiques de stock
  const calculateStockStats = useCallback((productsData, stockId = null) => {
    // Filtrer les produits pour ce stock spécifique si un ID est fourni
    const filteredProducts = stockId 
      ? productsData.filter(product => product.idStock === stockId)
      : productsData;

    if (stockId && (stocksMaxCapacity[stockId] === undefined || stocksMaxCapacity[stockId] === null)) {
      console.error(`ERREUR CRITIQUE : Capacité maximale manquante pour le stock ${stockId}`);
      throw new Error(`Capacité maximale non définie pour le stock ${stockId}. Cette valeur est obligatoire.`);
    }
    
    // Récupérer la capacité maximale de ce stock depuis l'état stocksMaxCapacity
    const maxCapacity = stocksMaxCapacity[stockId];
  
    console.log(`Calcul des stats pour stock ${stockId} - Capacité maximale: ${maxCapacity}`);
    // Initialiser les statistiques
    let availableItems = 0;

    // Calculer pour les produits filtrés
    filteredProducts.forEach(product => {
      // Ajouter au stock disponible
      availableItems += product.quantite || 0;
    });

    // Utiliser la capacité maximale définie
    const totalCapacity = maxCapacity;
    
    // Calculer le pourcentage d'utilisation basé sur la capacité maximale
    const usagePercentage = totalCapacity > 0 
      ? Math.round((availableItems / totalCapacity) * 100) 
      : 0;

    return {
      totalCapacity,
      availableStock: availableItems,
      usagePercentage
    };
  }, [stocksMaxCapacity]);

  // Load data from APIs directly
  const loadData = async (vendeurId) => {
    setLoading(true);
    setError(null);
    try {
      console.log("Chargement des données pour le vendeur:", vendeurId);
      
      // Récupérer les stocks du vendeur
      const stocksResponse = await axios.get(`/api/stocks/vendeur/${vendeurId}`, {
        headers: {
          'X-Vendeur-ID': vendeurId,
          'Cache-Control': 'no-cache, no-store, must-revalidate',
          'Pragma': 'no-cache',
          'Expires': '0'
        }
      });
      
      const stocksData = stocksResponse.data;
      setStocks(stocksData);
      
      // Extraire les capacités maximales des stocks
      const capacities = {};
      stocksData.forEach(stock => {
        if (stock.capaciteMaximaleStock === undefined || stock.capaciteMaximaleStock === null) {
          console.error(`ERREUR CRITIQUE : Capacité maximale manquante pour le stock ${stock.id}`);
          throw new Error(`La capacité maximale du stock ${stock.id} est obligatoire et ne peut pas être null.`);
        }
        // Utiliser la capacité maximale définie dans la base de données
        capacities[stock.id] = stock.capaciteMaximaleStock;
        console.log(`Stock ${stock.id} - Capacité maximale: ${stock.capaciteMaximaleStock}`);
      });
      setStocksMaxCapacity(capacities);
      
      console.log("Capacités maximales extraites:", capacities);

      // Récupérer les catégories
      const categoriesResponse = await axios.get('/api/categories', {
        headers: {
          'Cache-Control': 'no-cache, no-store, must-revalidate',
          'Pragma': 'no-cache',
          'Expires': '0'
        }
      });
      setCategories(categoriesResponse.data);

      // Récupérer les réductions
      const reductionsResponse = await axios.get('/api/reductions', {
        headers: {
          'Cache-Control': 'no-cache, no-store, must-revalidate',
          'Pragma': 'no-cache',
          'Expires': '0'
        }
      });
      setReductions(reductionsResponse.data);

      // Récupérer les transactions
      // Ici, vous pourriez filtrer par vendeur si l'API le supporte
      const timestamp = new Date().getTime();
      const transactionsResponse = await axios.get(`/api/stock-transactions?_t=${timestamp}`, {
        headers: { 
          'X-Vendeur-ID': vendeurId,
          'Cache-Control': 'no-cache, no-store, must-revalidate',
          'Pragma': 'no-cache',
          'Expires': '0'
        }
      });
      setTransactions(transactionsResponse.data);

      // Récupérer les produits des stocks du vendeur en garantissant des données fraîches
      // Ajoutez un paramètre timestamp pour éviter le cache du navigateur
      let allProducts = [];
      for (const stock of stocksData) {
        try {
          const timestamp = new Date().getTime(); // Ajouter un timestamp pour éviter le cache
          const productsResponse = await axios.get(`/api/produits/stock/${stock.id}?_t=${timestamp}`, {
            headers: { 
              'X-Vendeur-ID': vendeurId,
              'Cache-Control': 'no-cache, no-store, must-revalidate',
              'Pragma': 'no-cache',
              'Expires': '0'
            }
          });
          allProducts = [...allProducts, ...productsResponse.data];
        } catch (error) {
          console.error(`Erreur lors du chargement des produits pour le stock ${stock.id}:`, error);
        }
      }
      
      // Trier les produits par nom pour un ordre cohérent
      allProducts.sort((a, b) => a.nomProduit.localeCompare(b.nomProduit));
      setProducts(allProducts);

      // Calculer les statistiques pour chaque stock
      const statsMap = {};
      stocksData.forEach(stock => {
        statsMap[stock.id] = calculateStockStats(
          allProducts.filter(p => p.idStock === stock.id), 
          stock.id
        );
      });
      setStockStatsByStock(statsMap);

      // Si un stock est déjà ouvert, charger ses produits
      if (expandedStockId) {
        await loadStockProducts(expandedStockId, vendeurId);
      }

      // Forcer le recalcul des revenus après le chargement des données
      setTimeout(() => {
        calculateAllRevenues();
      }, 0);

      setLoading(false);
      setForceUpdate(prev => prev + 1); // Forcer un rendu
    } catch (error) {
      console.error("Error loading data:", error);
      setError("Erreur lors du chargement des données");
      setLoading(false);
    }
  };

  // Charger les produits d'un stock spécifique - AMÉLIORÉ
  const loadStockProducts = async (stockId, vendeurId = currentVendeurId) => {
    try {
      console.log(`Chargement des produits frais pour le stock ${stockId}`);
      const timestamp = new Date().getTime(); // Timestamp unique pour éviter le cache
      
      // Utiliser des en-têtes stricts anti-cache
      const response = await axios.get(`/api/produits/stock/${stockId}?_t=${timestamp}`, {
        headers: { 
          'X-Vendeur-ID': vendeurId,
          'Cache-Control': 'no-cache, no-store, must-revalidate',
          'Pragma': 'no-cache',
          'Expires': '0'
        }
      });
      
      // Extraire les données de la réponse
      const freshProducts = response.data;
      
      // Log des produits reçus pour le débogage
      console.log(`Réception de ${freshProducts.length} produits frais pour le stock ${stockId}`);
      
      // Préserver l'ordre des produits dans le tableau
      setProducts(prevProducts => {
        // Créer une map des produits existants par ID
        const existingProductsMap = {};
        prevProducts.forEach(product => {
          if (product.idStock !== stockId) {
            // Garder les produits des autres stocks
            existingProductsMap[product.id] = product;
          }
        });
        
        // Mettre à jour les produits de ce stock tout en préservant l'ordre
        const updatedProducts = [];
        
        // D'abord ajouter les produits des autres stocks
        prevProducts.forEach(product => {
          if (product.idStock !== stockId) {
            updatedProducts.push(product);
          }
        });
        
        // Ensuite, ajouter les produits mis à jour du stock actuel dans le même ordre qu'avant
        // Ou à la fin s'ils sont nouveaux
        freshProducts.sort((a, b) => a.nomProduit.localeCompare(b.nomProduit)); // Trier par nom pour un ordre cohérent
        updatedProducts.push(...freshProducts);
        
        return updatedProducts;
      });
      
      // Mettre à jour les statistiques du stock
      const updatedStats = calculateStockStats(freshProducts, stockId);
      setStockStatsByStock(prev => ({
        ...prev,
        [stockId]: updatedStats
      }));
      
      // Forcer un re-rendu pour s'assurer que les changements sont appliqués
      setForceUpdate(prev => prev + 1);
      
      return freshProducts; // Retourner les données pour utilisation par d'autres fonctions
    } catch (error) {
      console.error(`Error loading products for stock ${stockId}:`, error);
      setError(`Erreur lors du chargement des produits du stock ${stockId}`);
      return [];
    }
  };

  // Toggle expanded stock
  const toggleExpandStock = async (stockId) => {
    if (expandedStockId === stockId) {
      setExpandedStockId(null);
    } else {
      setExpandedStockId(stockId);
      // Charger les produits du stock - avec await pour garantir le chargement
      await loadStockProducts(stockId);
      // Fermer l'historique des transactions
      setShowTransactionHistory(false);
      setSelectedProduct(null);
    }
  };

  // Handle adding new stock
  const handleAddStock = () => {
    setEditingStock(null);
    setShowStockForm(true);
  };

  // Handle adding product to stock
  const handleAddProductToStock = (stock) => {
    setSelectedStock(stock);
    setShowProductForm(true);
  };

  // Handle editing stock
  const handleEditStock = (stock) => {
    setEditingStock(stock);
    setShowStockForm(true);
  };

  // Handle deleting stock
  const handleDeleteStock = async (stockId) => {
    if (window.confirm("Êtes-vous sûr de vouloir supprimer ce stock?")) {
      try {
        await axios.delete(`/api/stocks/${stockId}`, {
          headers: { 'X-Vendeur-ID': currentVendeurId }
        });
        setStocks(stocks.filter((stock) => stock.id !== stockId));
        // Supprimer également les capacités maximales et statistiques
        setStocksMaxCapacity(prev => {
          const updated = {...prev};
          delete updated[stockId];
          return updated;
        });
        setStockStatsByStock(prev => {
          const updated = {...prev};
          delete updated[stockId];
          return updated;
        });
        // Supprimer les revenus du stock
        setStockRevenues(prev => {
          const updated = {...prev};
          delete updated[stockId];
          return updated;
        });
        alert("Stock supprimé avec succès");
      } catch (error) {
        console.error("Error deleting stock:", error);
        alert("Erreur lors de la suppression du stock");
      }
    }
  };

  // Quick increment/decrement by 1 - AMÉLIORÉ
// Quick increment/decrement by 1 - AMÉLIORÉ// Quick increment/decrement by 1 - AMÉLIORÉ
// Remplacez votre fonction actuelle handleQuickAdjust par celle-ci
const handleQuickAdjust = async (product, type) => {
  const changeAmount = 1;

  if (type === "remove" && product.quantite <= 0) {
    alert("La quantité ne peut pas être négative");
    return;
  }

  try {
    console.log(`Démarrage de l'ajustement rapide: ${type.toUpperCase()} ${changeAmount} unité(s) pour ${product.nomProduit}`);
    
    // 1. D'abord ajuster la quantité du produit via l'API produit
    try {
      // Utiliser l'endpoint d'ajustement de produit qui est plus stable
      const ajustementResponse = await axios.patch(
        `/api/produits/${product.id}/ajustement?quantite=${type === "add" ? changeAmount : -changeAmount}`,
        {},
        {
          headers: { 
            'X-Vendeur-ID': currentVendeurId,
            'Cache-Control': 'no-cache, no-store, must-revalidate'
          }
        }
      );
      
      console.log("Ajustement quantité réussi:", ajustementResponse.data);
      
      // 2. Mettre à jour le produit localement
      setProducts(prevProducts => {
        return prevProducts.map(p => {
          if (p.id === product.id) {
            return {
              ...p,
              quantite: type === 'add' ? p.quantite + changeAmount : p.quantite - changeAmount
            };
          }
          return p;
        });
      });
      
      // 3. Essayer de créer l'entrée de transaction pour l'historique
      // Si cette étape échoue, l'ajustement de quantité reste valide
      try {
        const transactionData = {
          productId: product.id,
          stockId: product.idStock,
          type: type.toUpperCase(),
          quantity: changeAmount,
          prix: product.prix?.toString() || '0',
          notes: `${type === "add" ? "Ajout rapide de" : "Retrait rapide de"} ${changeAmount} unité`
        };
        
        console.log("Création de transaction:", transactionData);
        
        const response = await axios.post('/api/stock-transactions', transactionData, {
          headers: {
            'Content-Type': 'application/json',
            'X-Vendeur-ID': currentVendeurId
          }
        });
        
        if (response.data) {
          console.log("Transaction réussie:", response.data);
          // Ajouter la nouvelle transaction à la liste locale
          setTransactions(prev => [response.data, ...prev]);
        }
      } catch (transactionError) {
        // Si l'enregistrement de la transaction échoue, ce n'est pas critique
        // car la quantité a déjà été ajustée correctement
        console.error("Erreur lors de la création de la transaction:", transactionError);
        console.log("L'ajustement a été appliqué mais l'historique n'a pas pu être créé");
      }
      
      // 4. Mettre à jour les revenus si c'est une transaction de retrait
      if (type === "remove") {
        const productPrice = parseFloat(product.prix || 0);
        const transactionRevenue = changeAmount * productPrice;
        
        // Mise à jour immédiate des revenus
        setProductRevenues(prev => ({
          ...prev,
          [product.id]: (prev[product.id] || 0) + transactionRevenue
        }));
        
        setStockRevenues(prev => ({
          ...prev,
          [product.idStock]: (prev[product.idStock] || 0) + transactionRevenue
        }));
      }
      
      // 5. Mettre à jour les statistiques du stock
      setStockStatsByStock(prevStats => {
        const currentStats = prevStats[product.idStock] || {
          totalCapacity: stocksMaxCapacity[product.idStock] || 0,
          availableStock: 0,
          usagePercentage: 0
        };
        
        // Ajuster le stock disponible
        const newAvailableStock = currentStats.availableStock + (type === 'add' ? changeAmount : -changeAmount);
        
        // Recalculer le pourcentage d'utilisation
        const newUsagePercentage = currentStats.totalCapacity > 0 
          ? Math.round((newAvailableStock / currentStats.totalCapacity) * 100) 
          : 0;
        
        return {
          ...prevStats,
          [product.idStock]: {
            ...currentStats,
            availableStock: newAvailableStock,
            usagePercentage: newUsagePercentage
          }
        };
      });
      
      // 6. Forcer un re-rendu pour appliquer les changements visuels
      setForceUpdate(prev => prev + 1);
      
    } catch (ajustementError) {
      console.error("Erreur lors de l'ajustement de la quantité:", ajustementError);
      
      // Afficher un message d'erreur approprié
      if (ajustementError.response) {
        if (ajustementError.response.status === 400) {
          alert("Erreur: " + (ajustementError.response.data.message || "Données invalides"));
        } else {
          alert("Erreur lors de l'ajustement du stock");
        }
      } else {
        alert("Erreur lors de l'ajustement du stock");
      }
    }
  } catch (error) {
    console.error("Erreur complète:", error);
    alert("Erreur lors de l'ajustement du stock");
  }
};
  
  // Open transaction modal for custom quantity
  const handleOpenTransactionModal = (product, type) => {
    setSelectedProduct(product);
    setTransactionType(type);
    setTransactionQuantity(1);
    setShowTransactionModal(true);
  };

  // Open transaction history
// Modification pour handleOpenTransactionHistory
const handleOpenTransactionHistory = async (product) => {
  setSelectedProduct(product);
  
  try {
    // S'assurer que l'ID est un nombre
    const productId = Number(product.id);
    
    // Ajouter un timestamp pour éviter le cache
    const timestamp = new Date().getTime();
    const response = await axios.get(`/api/stock-transactions/produit/${productId}?_t=${timestamp}`, {
      headers: { 
        'X-Vendeur-ID': currentVendeurId,
        'Cache-Control': 'no-cache'
      }
    });
    
    // Si la réponse est réussie, utiliser les données (même si c'est un tableau vide)
    const transactionsData = response.data || [];
    
    setTransactions(prev => {
      const allTransactions = [...transactionsData, ...prev];
      const uniqueTransactions = [];
      const ids = new Set();
      
      allTransactions.forEach(transaction => {
        if (!ids.has(transaction.id)) {
          ids.add(transaction.id);
          uniqueTransactions.push(transaction);
        }
      });
      
      return uniqueTransactions;
    });
    
    // Toujours afficher l'historique même s'il est vide
    setShowTransactionHistory(true);
    
  } catch (error) {
    console.error("Erreur lors du chargement des transactions:", error);
    
    // Créer un historique vide en cas d'erreur
    setTransactions([]);
    setShowTransactionHistory(true);
    
    // Notification optionnelle
    if (error.response?.status === 404) {
      console.warn("Aucune transaction trouvée pour ce produit");
    }
  }
};

  // Handle transaction submission - AMÉLIORÉ
  const handleSubmitTransaction = async () => {
    if (!selectedProduct) return;

    // Validate input
    const quantityToAdjust = parseFloat(transactionQuantity);
    if (isNaN(quantityToAdjust) || quantityToAdjust <= 0) {
      alert("Veuillez entrer une quantité valide");
      return;
    }

    // If decreasing and adjustment would result in negative stock, show warning
    if (transactionType === "remove" && selectedProduct.quantite < quantityToAdjust) {
      alert("La quantité restante ne peut pas être négative");
      return;
    }

    try {
      console.log(`Démarrage de la transaction: ${transactionType.toUpperCase()} ${quantityToAdjust} unité(s) pour ${selectedProduct.nomProduit}`);
      
      // 1. Appeler l'API pour ajuster le stock
      const response = await axios.patch(
        `/api/produits/${selectedProduct.id}/ajustement?quantite=${transactionType === "add" ? quantityToAdjust : -quantityToAdjust}`,
        {},
        {
          headers: { 
            'X-Vendeur-ID': currentVendeurId,
            'Cache-Control': 'no-cache, no-store, must-revalidate'
          }
        }
      );

      console.log(`Ajustement stock réussi avec réponse:`, response.data);

      // 2. Mettre à jour le produit localement sans changer sa position
      setProducts(prevProducts => {
        return prevProducts.map(p => {
          if (p.id === selectedProduct.id) {
            return {
              ...p,
              quantite: transactionType === 'add' 
                ? p.quantite + quantityToAdjust 
                : p.quantite - quantityToAdjust
            };
          }
          return p;
        });
      });

      // 3. Créer une transaction
      const transactionData = {
        productId: selectedProduct.id,
        stockId: selectedProduct.idStock,
        type: transactionType.toUpperCase(),
        quantity: quantityToAdjust,
        prix: selectedProduct.prix?.toString() || '0',
        notes: `${transactionType === "add" ? "Ajout de" : "Retrait de"} ${quantityToAdjust} unité${quantityToAdjust > 1 ? "s" : ""}`
      };

      const transactionResponse = await axios.post('/api/stock-transactions', transactionData, {
        headers: {
          'Content-Type': 'application/json',
          'X-Vendeur-ID': currentVendeurId
        }
      });
      const newTransaction = transactionResponse.data;
      
      console.log(`Transaction enregistrée:`, newTransaction);
      
      // 4. Ajouter la nouvelle transaction à la liste
      setTransactions(prevTransactions => [newTransaction, ...prevTransactions]);
      
      // 5. Si c'est une transaction REMOVE, mettre à jour les revenus du produit
      if (transactionType === "remove") {
        const productPrice = parseFloat(selectedProduct.prix || 0);
        const transactionRevenue = quantityToAdjust * productPrice;
        
        // Mise à jour immédiate des revenus
        setProductRevenues(prev => ({
          ...prev,
          [selectedProduct.id]: (prev[selectedProduct.id] || 0) + transactionRevenue
        }));
        
        setStockRevenues(prev => ({
          ...prev,
          [selectedProduct.idStock]: (prev[selectedProduct.idStock] || 0) + transactionRevenue
        }));
        
        console.log(`Revenu mis à jour`);
      }
      
      // 6. Mettre à jour les statistiques du stock sans recharger tous les produits
      setStockStatsByStock(prevStats => {
        const currentStats = prevStats[selectedProduct.idStock] || {
          totalCapacity: stocksMaxCapacity[selectedProduct.idStock] || 0,
          availableStock: 0,
          usagePercentage: 0
        };
        
        // Ajuster le stock disponible
        const newAvailableStock = currentStats.availableStock + (transactionType === 'add' ? quantityToAdjust : -quantityToAdjust);
        
        // Recalculer le pourcentage d'utilisation
        const newUsagePercentage = currentStats.totalCapacity > 0 
          ? Math.round((newAvailableStock / currentStats.totalCapacity) * 100) 
          : 0;
        
        return {
          ...prevStats,
          [selectedProduct.idStock]: {
            ...currentStats,
            availableStock: newAvailableStock,
            usagePercentage: newUsagePercentage
          }
        };
      });
      
      // 7. Fermer le modal et notifier l'utilisateur
      setShowTransactionModal(false);
      alert(`${transactionType === "add" ? "Ajout" : "Retrait"} de stock effectué avec succès`);
      
      // 8. Forcer un re-rendu 
      setForceUpdate(prev => prev + 1);
      
      console.log(`Transaction terminée avec succès`);
      
    } catch (error) {
      console.error("Error submitting transaction:", error);
      alert("Erreur lors de l'enregistrement de la transaction");
    }
  };

  // Handle stock form submission
  const handleStockFormSubmit = async (formData) => {
    try {
      // Logs de débogage
      console.log("handleStockFormSubmit reçoit:", formData);
      
      // Préparation des données à envoyer au backend
      // S'assurer que la structure correspond exactement à StockDTO
      const stockData = {
        id: formData.id || null,
        name: formData.name || "",
        quantiteStockDisponible: formData.quantiteStockDisponible || 0,
        capaciteMaximaleStock: formData.capaciteMaximaleStock,
        idBoutique: formData.idBoutique,
        location: formData.location || "",
        //idVendeur: currentVendeurId // Ajouter l'ID du vendeur connecté
      };

      console.log("Données préparées à envoyer au serveur:", stockData);

      // Vérification supplémentaire des types
      if (typeof stockData.idBoutique !== 'number') {
        console.error("idBoutique n'est pas un nombre:", stockData.idBoutique);
        alert("Erreur: L'ID de la boutique doit être un nombre.");
        return;
      }

      if (typeof stockData.capaciteMaximaleStock !== 'number') {
        console.error("capaciteMaximaleStock n'est pas un nombre:", stockData.capaciteMaximaleStock);
        alert("Erreur: La capacité maximale du stock doit être un nombre.");
        return;
      }

      if (editingStock) {
        // Update existing stock
        console.log(`PUT sur /api/stocks/${editingStock.id}`, stockData);
        const response = await axios.put(`/api/stocks/${editingStock.id}`, stockData, {
          headers: { 
            'X-Vendeur-ID': currentVendeurId,
            'Cache-Control': 'no-cache, no-store, must-revalidate'
          }
        });
        console.log("Réponse du serveur (PUT):", response.data);
        
        // Mettre à jour la liste des stocks
        setStocks(stocks.map(stock => stock.id === editingStock.id ? response.data : stock));
        
        // Mettre à jour la capacité maximale du stock
        setStocksMaxCapacity(prev => ({
          ...prev,
          [editingStock.id]: response.data.capaciteMaximaleStock
        }));
        
        // Mise à jour des statistiques
        setStockStatsByStock(prev => ({
          ...prev,
          [editingStock.id]: {
            totalCapacity: response.data.capaciteMaximaleStock,
            availableStock: response.data.quantiteStockDisponible || 0,
            usagePercentage: calculateUsagePercentage(response.data)
          }
        }));
        
        alert("Stock mis à jour avec succès");
      } else {
        // Add new stock
        console.log("POST sur /api/stocks", stockData);
        const response = await axios.post('/api/stocks', stockData, {
          headers: {
            'X-Vendeur-ID': currentVendeurId,
            'Cache-Control': 'no-cache, no-store, must-revalidate'
          }
        });
        console.log("Réponse du serveur (POST):", response.data);
        
        const newStock = response.data;
        setStocks([...stocks, newStock]);
        
        // Mettre à jour la capacité maximale du stock
        setStocksMaxCapacity(prev => ({
          ...prev,
          [newStock.id]: newStock.capaciteMaximaleStock
        }));
        
        // Initialiser les statistiques pour le nouveau stock
        setStockStatsByStock(prevStats => ({
          ...prevStats,
          [newStock.id]: {
            totalCapacity: newStock.capaciteMaximaleStock,
            availableStock: newStock.quantiteStockDisponible || 0,
            usagePercentage: calculateUsagePercentage(newStock)
          }
        }));
        
        // Initialiser les revenus du stock
        setStockRevenues(prev => ({
          ...prev,
          [newStock.id]: 0
        }));
        
        alert("Stock ajouté avec succès");
      }
      
      // Recharger les données pour s'assurer que tout est à jour
      await loadData(currentVendeurId);
      
      setShowStockForm(false);
      setEditingStock(null);
    } catch (error) {
      console.error("Error submitting stock form:", error);
      
      // Inspection détaillée de l'erreur
      console.log("Error details:", {
        status: error.response?.status,
        statusText: error.response?.statusText,
        data: error.response?.data
      });
      
      // Affichage d'un message d'erreur plus détaillé
      let errorMessage = "Erreur lors de l'enregistrement du stock";
      
      if (error.response) {
        if (error.response.status === 400) {
          errorMessage = "Données invalides: ";
          
          if (error.response.data && typeof error.response.data === 'object') {
            // Erreurs de validation
            const validationErrors = Object.entries(error.response.data)
              .map(([field, message]) => `${field}: ${message}`)
              .join(', ');
            
            errorMessage += validationErrors || "Veuillez vérifier les données saisies";
          } else {
            errorMessage += error.response.data || "Veuillez vérifier les données saisies";
          }
        } else if (error.response.status === 500) {
          errorMessage = "Erreur serveur: " + (error.response.data?.message || "Une erreur interne s'est produite");
        }
      }
      
      alert(errorMessage);
    }
  };

  // Fonction utilitaire pour calculer le pourcentage d'utilisation
  const calculateUsagePercentage = (stock) => {
    if (!stock.capaciteMaximaleStock || stock.capaciteMaximaleStock <= 0) {
      return 0;
    }

    const percentage = (stock.quantiteStockDisponible / stock.capaciteMaximaleStock) * 100;
    return Math.min(Math.round(percentage), 100); // Limiter à 100% maximum
  };

  // Handle product form submission - AMÉLIORÉ
  const handleProductFormSubmit = async (productData) => {
    try {
      // Créer un objet FormData
      const formData = new FormData();
      
      // Ajouter l'ID du vendeur
      formData.append("idVendeur", currentVendeurId);
      
      // Ajouter les champs textuels
      formData.append("nomProduit", productData.nomProduit);
      formData.append("quantite", productData.Quantité.toString());
      formData.append("description", productData.description || "");
      formData.append("detail", productData.detail || "");
      
      // Utiliser seuilCritique pour enregistrer le seuil (même si dans le formulaire c'est seuil_Critique)
      // C'est ce champ qui sera utilisé pour déterminer le statut du produit
      const seuil = productData.seuil_Critique || productData.seuilCritique || 0;
      formData.append("seuilCritique", seuil.toString());
      console.log(`Seuil critique enregistré pour ${productData.nomProduit}: ${seuil}`);
      
      formData.append("prix", productData.Prix.toString());
      
      if (productData.date_expiration) {
        formData.append("dateExpiration", productData.date_expiration);
      }
      
      formData.append("idStock", productData.id_stock.toString());
      formData.append("idCategorie", productData.id_categorie.toString());
      
      if (productData.id_reduction) {
        formData.append("idReduction", productData.id_reduction.toString());
      }
      
      // Ajouter les fichiers d'images et déterminer l'image principale
      let primaryImageIndex = 0;
      if (productData.images && productData.images.length > 0) {
        // Trouver l'index de l'image principale
        primaryImageIndex = productData.images.findIndex(img => img.image_principale);
        if (primaryImageIndex === -1) primaryImageIndex = 0;
        
        // Ajouter les fichiers d'images
        productData.images.forEach((image, index) => {
          if (image.file) {
            formData.append("images", image.file);
          }
        });
        
        // Ajouter l'index de l'image principale
        formData.append("imagePrincipale", primaryImageIndex.toString());
      }
      
      // Envoyer la requête
      const response = await axios.post('/api/produits/avec-images', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
          'X-Vendeur-ID': currentVendeurId,
          'Cache-Control': 'no-cache, no-store, must-revalidate'
        }
      });
      
      console.log("Produit créé avec succès:", response.data);
      
      // Mettre à jour la liste des produits
      const newProduct = response.data;
      
      // Ajouter le nouveau produit à la liste, en le triant par nom
      setProducts(prevProducts => {
        const updatedProducts = [...prevProducts, newProduct];
        return updatedProducts.sort((a, b) => a.nomProduit.localeCompare(b.nomProduit));
      });
      
      // Initialiser les revenus du produit
      setProductRevenues(prev => ({
        ...prev,
        [newProduct.id]: 0
      }));
      
      // Mettre à jour les statistiques du stock
      setStockStatsByStock(prevStats => ({
        ...prevStats,
        [newProduct.idStock]: calculateStockStats(
          [...products, newProduct], 
          newProduct.idStock
        )
      }));
      
      // Fermer le formulaire et actualiser les données
      setShowProductForm(false);
      setSelectedStock(null);
      
      // Afficher un message de succès
      alert("Produit ajouté avec succès!");
      
    } catch (error) {
      console.error("Erreur lors de l'envoi du formulaire:", error);
      if (error.response) {
        console.error("Détails de l'erreur:", error.response.data);
        alert(`Erreur: ${error.response.data.message || "Une erreur est survenue"}`);
      } else {
        alert("Erreur lors de l'ajout du produit. Veuillez réessayer.");
      }
    }
  };

  // Filter products by stock
  const getProductsByStock = (stockId) => {
    return products.filter((product) => product.idStock === stockId);
  };

  // Get the calculated revenue for a product from the state
  const getProductRevenue = (productId) => {
    return productRevenues[productId] || 0;
  };

  // Get the calculated revenue for a stock from the state
  const getStockRevenue = (stockId) => {
    return stockRevenues[stockId] || 0;
  };

  // Filter and search logic for stocks
  const filteredStocks = stocks.filter((stock) => {
    // Vérifier si stock et stock.name existent avant d'appeler toLowerCase()
    const matchesSearch = 
      stock && 
      stock.name && 
      stock.name.toLowerCase().includes(searchTerm.toLowerCase());

    // Note: nous n'avons plus besoin de filtrer par vendeur puisque tous les stocks
    // chargés appartiennent déjà au vendeur connecté
    const matchesStore = storeFilter === "" || (stock && stock.idBoutique.toString() === storeFilter.toString());

    return matchesSearch && matchesStore;
  });

  // Pagination logic
  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentStocks = filteredStocks.slice(indexOfFirstItem, indexOfLastItem);
  const totalPages = Math.ceil(filteredStocks.length / itemsPerPage);

  // Get store name from ID
  const getStoreName = (storeId) => {
    if (!storeId) return "Boutique inconnue";

    // Conversion en string pour comparaison cohérente
    const storeIdStr = storeId.toString();
    const store = stores.find((s) => s.id.toString() === storeIdStr);
    return store ? store.nom : "Boutique inconnue";
  };

  // Get stock status - CORRIGÉ pour correspondre au seuil critique exact
  const getStockStatus = (product) => {
    if (!product) return { status: "unknown", label: "Inconnu", color: "gray" };
    
    // S'assurer que les valeurs sont des nombres
    const quantite = Number(product.quantite || 0);
    const seuilCritique = Number(product.seuilCritique || 0);
    
    console.log(`Vérification du statut pour ${product.nomProduit}: Quantité=${quantite}, Seuil=${seuilCritique}`);
    
    if (quantite <= 0) {
      return { status: "outOfStock", label: "Épuisé", color: "red" };
    }
    
    if (quantite <= seuilCritique) {
      // Statut "Critique" quand on est égal ou inférieur au seuil
      return { status: "critical", label: "Critique", color: "red" };
    }
    
    // Statut "En stock" quand on est supérieur au seuil critique
    return { status: "ok", label: "En stock", color: "green" };
  };

  // Composant pour afficher les statistiques d'un stock spécifique
  const StockStatsDisplay = ({ stockId }) => {
    // Récupérer les statistiques pour ce stock
    const stats = stockStatsByStock[stockId] || {
      totalCapacity: 0,
      availableStock: 0,
      usagePercentage: 0
    };

    // Utiliser le revenu du stock depuis l'état global
    const totalRevenue = getStockRevenue(stockId);

    return (
      <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200 mb-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {/* Capacité Maximale */}
          <div className="bg-gray-50 p-3 rounded-lg border border-gray-100">
            <h3 className="text-gray-500 text-xs mb-1">Capacité Maximale</h3>
            <div className="text-xl font-bold text-gray-900">{stats.totalCapacity}</div>
          </div>
          
          {/* Stock Disponible */}
          <div className="bg-gray-50 p-3 rounded-lg border border-gray-100">
            <h3 className="text-gray-500 text-xs mb-1">Stock Disponible</h3>
            <div className="text-xl font-bold text-green-600">{stats.availableStock}</div>
          </div>
          
          {/* Revenus Totaux */}
          <div className="bg-gray-50 p-3 rounded-lg border border-gray-100">
            <h3 className="text-gray-500 text-xs mb-1 flex items-center">
              <DollarSign size={14} className="mr-1 text-green-500" />
              Revenus Totaux
            </h3>
            <div className="text-xl font-bold text-green-600">{totalRevenue.toFixed(2)} €</div>
          </div>
        </div>
        
        {/* Progress Bar */}
        <div className="mt-4">
          <div className="flex justify-between items-center mb-1">
            <span className="text-xs text-gray-600">Utilisation du stock</span>
            <span className="text-xs font-medium text-gray-800">{stats.usagePercentage}%</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div 
              className="bg-blue-600 h-2 rounded-full" 
              style={{ width: `${stats.usagePercentage}%` }}
            ></div>
          </div>
        </div>
      </div>
    );
  };
  
  // Fonction pour rafraîchir manuellement les données - AJOUT
  const handleRefreshData = async () => {
    try {
      setLoading(true);
      await loadData(currentVendeurId);
      setLoading(false);
      alert("Données rafraîchies avec succès");
    } catch (error) {
      console.error("Erreur lors du rafraîchissement des données:", error);
      setError("Erreur lors du rafraîchissement des données");
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="w-full h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500 mb-4"></div>
          <p className="text-gray-600">Chargement des données de stock...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="w-full h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center p-8 bg-white rounded-lg shadow-md max-w-md">
          <AlertTriangle size={48} className="mx-auto text-red-500 mb-4" />
          <h2 className="text-xl font-semibold text-gray-800 mb-2">Une erreur est survenue</h2>
          <p className="text-gray-600 mb-4">{error}</p>
          <button 
            onClick={() => loadData(currentVendeurId)}
            className="px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 transition-colors"
          >
            Réessayer
          </button>
        </div>
      </div>
    );
  }

  if (showStockForm) {
    return (
      <div className="min-h-screen bg-gray-50 pb-10">
        <DashboardNavigation />
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8">
          <div className="mb-6">
            <button
              onClick={() => {
                setShowStockForm(false);
                setEditingStock(null);
              }}
              className="flex items-center text-blue-600 hover:text-blue-800 font-medium"
            >
              <ArrowLeft size={18} className="mr-1" />
              Retour à la gestion des stocks
            </button>
          </div>
          <StockForm
            stock={editingStock}
            boutiques={stores}
            onSubmit={handleStockFormSubmit}
            onCancel={() => {
              setShowStockForm(false);
              setEditingStock(null);
            }}
          />
        </div>
      </div>
    );
  }

  if (showProductForm) {
    return (
      <div className="min-h-screen bg-gray-50 pb-10">
        <DashboardNavigation />
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8">
          <div className="mb-6">
            <button
              onClick={() => {
                setShowProductForm(false);
                setSelectedStock(null);
              }}
              className="flex items-center text-blue-600 hover:text-blue-800 font-medium"
            >
              <ArrowLeft size={18} className="mr-1" />
              Retour à la gestion des stocks
            </button>
          </div>

          <div className="mb-4 p-4 bg-blue-50 rounded-lg flex items-center border border-blue-100">
            <Store className="mr-2 text-blue-500" size={20} />
            <span className="font-medium">
              Ajout d'un produit au stock:{" "}
              <span className="text-blue-700">{selectedStock?.name}</span>
            </span>
          </div>

          <ProductForm
            hideStockSelector={true}
            initialStockId={selectedStock?.id}
            stocks={stocks}
            categories={categories}
            reductions={reductions}
            onSubmit={handleProductFormSubmit}
            onCancel={() => {
              setShowProductForm(false);
              setSelectedStock(null);
            }}
          />
        </div>
      </div>
    );
  }
  return (
    <>
      <div className="min-h-screen bg-gray-50 pb-10">
        {/* Dashboard Navigation */}
        <DashboardNavigation />

        {/* Stock Management Content */}
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8">
          {/* Header */}
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-6">
            <div className="flex items-start">
              <div className="mr-3 bg-blue-500 p-3 rounded-lg flex-shrink-0 text-white shadow-md">
                <Layers size={24} />
              </div>
              <div>
                <h1 className="text-2xl font-bold text-gray-800 flex items-center">
                  Gestion de Mes Stocks
                </h1>
                <p className="text-gray-500 mt-1">
                  Gérez vos produits et suivez les mouvements de stock en temps réel
                </p>
              </div>
            </div>
            <div className="flex space-x-2">
              {/* Bouton de rafraîchissement - AJOUT */}
              <button
                onClick={handleRefreshData}
                className="px-4 py-2.5 text-sm font-medium text-blue-600 bg-blue-50 rounded-lg hover:bg-blue-100 transition-colors"
                title="Rafraîchir les données"
              >
                Rafraîchir
              </button>
              
              <button
                onClick={handleAddStock}
                className="px-5 py-2.5 text-sm font-medium text-white bg-teal-500 rounded-lg shadow-md hover:bg-blue-700 flex items-center justify-center transition-all duration-200 transform hover:scale-105"
              >
                <Plus size={16} className="inline mr-2" />
                <span>Ajouter un Stock</span>
              </button>
            </div>
          </div>

          {/* Search and Filter */}
          <div className="bg-white p-4 rounded-xl shadow border border-gray-100 mb-6">
            <div className="flex flex-col md:flex-row gap-4">
              <div className="relative flex-grow">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Search size={18} className="text-gray-400" />
                </div>
                <input
                  type="text"
                  className="block w-full pl-10 pr-3 py-2.5 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all"
                  placeholder="Rechercher un stock par nom..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>

              {stores.length > 1 && (
                <div className="flex-shrink-0 md:w-64">
                  <select
                    className="block w-full pl-3 pr-10 py-2.5 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all appearance-none bg-white"
                    value={storeFilter}
                    onChange={(e) => setStoreFilter(e.target.value)}
                    style={{
                      backgroundImage:
                        "url(\"data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3e%3cpath stroke='%236b7280' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M6 8l4 4 4-4'/%3e%3c/svg%3e\")",
                      backgroundPosition: "right 0.5rem center",
                      backgroundRepeat: "no-repeat",
                      backgroundSize: "1.5em 1.5em",
                    }}
                  >
                    <option value="">Toutes mes boutiques</option>
                    {stores.map((store) => (
                      <option key={store.id} value={store.id}>
                        {store.nom}
                      </option>
                    ))}
                  </select>
                </div>
              )}
            </div>
          </div>
          {/* Stocks List */}
          {filteredStocks.length > 0 ? (
              <div className="space-y-4 mb-6">
                {currentStocks.map((stock) => (
                  <div
                    key={stock.id}
                    className="bg-white rounded-xl shadow border border-gray-100 overflow-hidden transition-all duration-300"
                  >
                    {/* Stock Header */}
                    <div
                      className={`p-4 flex items-center justify-between cursor-pointer hover:bg-gray-50 ${
                        expandedStockId === stock.id
                          ? "bg-blue-50 border-b border-blue-100"
                          : ""
                      }`}
                      onClick={() => toggleExpandStock(stock.id)}
                    >
                      <div className="flex items-center space-x-4">
                        <div className="bg-blue-500 p-3 rounded-lg text-white">
                          <Store size={20} />
                        </div>
                        <div>
                       <h3 className="font-semibold text-lg text-gray-900">
                            {stock.name}
                          </h3>
                          <p className="text-gray-500">
                            {getStoreName(stock.idBoutique)}
                          </p>
                        </div>
                      </div>

                      <div className="flex items-center space-x-2">
                        <span className="px-3 py-1.5 bg-blue-100 text-blue-700 rounded-full text-sm font-medium">
                          {getProductsByStock(stock.id).length} produits
                        </span>
                        
                        {/* Total Stock Revenue */}
                        <span className="px-3 py-1.5 bg-green-100 text-green-700 rounded-full text-sm font-medium flex items-center">
                          <DollarSign size={14} className="mr-1" />
                          {getStockRevenue(stock.id).toFixed(2)} €
                        </span>

                        <div className="flex items-center space-x-1">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              handleAddProductToStock(stock);
                            }}
                            className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                            title="Ajouter un produit"
                          >
                            <PlusCircle size={20} />
                          </button>

                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              handleEditStock(stock);
                            }}
                            className="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
                            title="Modifier le stock"
                          >
                            <Edit size={18} />
                          </button>

                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              handleDeleteStock(stock.id);
                            }}
                            className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                            title="Supprimer le stock"
                          >
                            <Trash2 size={18} />
                          </button>
                        </div>

                        {expandedStockId === stock.id ? (
                          <ChevronUp size={20} className="text-gray-400" />
                        ) : (
                          <ChevronDown size={20} className="text-gray-400" />
                        )}
                      </div>
                    </div>

                    {/* Expanded Stock View - Products Table */}
                    {expandedStockId === stock.id && (
                      <div className="p-4 animate-fadeIn">
                        {/* Afficher les statistiques pour ce stock spécifique */}
                        <StockStatsDisplay stockId={stock.id} />
                        
                        <div className="mb-4 flex justify-between items-center">
                          <h4 className="font-medium text-gray-800 flex items-center">
                            <Package size={16} className="mr-2 text-blue-500" />
                            Produits dans ce stock
                          </h4>
                          <div className="flex space-x-2">
                            {/* Bouton pour rafraîchir les produits du stock - AJOUT */}
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                loadStockProducts(stock.id);
                              }}
                              className="px-3 py-1.5 bg-blue-50 text-blue-600 text-sm rounded-lg flex items-center hover:bg-blue-100 transition-colors"
                              title="Rafraîchir les produits"
                            >
                              Rafraîchir
                            </button>
                            
                            <button
                              onClick={() => handleAddProductToStock(stock)}
                              className="px-3 py-1.5 bg-teal-500 text-white text-sm rounded-lg flex items-center hover:bg-blue-700 transition-colors shadow-sm"
                            >
                              <Plus size={16} className="mr-1" /> Ajouter un produit
                            </button>
                          </div>
                        </div>

                        <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
                          <div className="overflow-x-auto">
                            <table className="min-w-full divide-y divide-gray-200">
                              <thead className="bg-gray-50">
                                <tr>
                                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    Produit
                                  </th>
                                  <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    Prix
                                  </th>
                                  <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    Quantité
                                  </th>
                                  <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    Seuil
                                  </th>
                                  <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    Statut
                                  </th>
                                  <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    Revenu généré
                                  </th>
                                  <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    Actions
                                  </th>
                                </tr>
                              </thead>
                              <tbody className="bg-white divide-y divide-gray-200">
                                {getProductsByStock(stock.id).length > 0 ? (
                                  getProductsByStock(stock.id).map((product) => {
                                    const stockStatus = getStockStatus(product);
                                    // Utiliser le revenu persistant du produit
                                    const productRevenue = getProductRevenue(product.id);

                                    return (
                                      <tr
                                        key={product.id}
                                        className="hover:bg-gray-50"
                                      >
                                        <td className="px-4 py-3 whitespace-nowrap">
                                          <div className="flex items-center">
                                            <div className="h-10 w-10 flex-shrink-0 bg-gray-100 rounded-lg flex items-center justify-center">
                                              {product.images && product.images.length > 0 ? (
                                                <img 
                                                  src={product.images.find(img => img.imagePrincipale)?.url || product.images[0].url} 
                                                  alt={product.nomProduit}
                                                  className="h-10 w-10 object-cover rounded-lg"
                                                />
                                              ) : (
                                                <Package
                                                  size={18}
                                                  className="text-gray-500"
                                                />
                                              )}
                                            </div>
                                            <div className="ml-4">
                                              <div className="text-sm font-medium text-gray-900">
                                                {product.nomProduit}
                                              </div>
                                              <div className="text-xs text-gray-500">
                                                {product.description}
                                              </div>
                                            </div>
                                          </div>
                                        </td>
                                        <td className="px-4 py-3 whitespace-nowrap text-center">
                                          <div className="text-sm font-medium text-gray-900">
                                            {parseFloat(product.prix).toFixed(2)} €
                                          </div>
                                        </td>
                                        <td className="px-4 py-3 whitespace-nowrap text-center">
                                          <div className="text-sm font-medium text-gray-900">
                                            {product.quantite}
                                          </div>
                                        </td>
                                        <td className="px-4 py-3 whitespace-nowrap text-center">
                                          <div className="text-sm text-gray-500">
                                            {product.seuilCritique}
                                          </div>
                                        </td>
                                        <td className="px-4 py-3 whitespace-nowrap text-center">
                                          <span
                                            className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-${stockStatus.color}-100 text-${stockStatus.color}-800`}
                                          >
                                            {stockStatus.status === "critical" && (
                                              <AlertTriangle
                                                size={12}
                                                className="mr-1"
                                              />
                                            )}
                                            {stockStatus.label}
                                          </span>
                                        </td>
                                        <td className="px-4 py-3 whitespace-nowrap text-center">
                                          <div className="flex flex-col items-center">
                                            <span className="text-sm font-medium text-green-600">
                                              {productRevenue.toFixed(2)} €
                                            </span>
                                          </div>
                                        </td>
                                        <td className="px-4 py-3 whitespace-nowrap text-center">
                                          <div className="flex items-center justify-center space-x-1">
                                            <button
                                              onClick={() =>
                                                handleQuickAdjust(product, "add")
                                              }
                                              className="p-1.5 text-green-600 hover:bg-green-50 rounded-lg transition-colors border border-green-200"
                                              title="Ajouter 1 unité"
                                            >
                                              <PlusCircle size={16} />
                                            </button>
                                            <button
                                              onClick={() =>
                                                handleQuickAdjust(product, "remove")
                                              }
                                              className="p-1.5 text-red-600 hover:bg-red-50 rounded-lg transition-colors border border-red-200"
                                              title="Retirer 1 unité"
                                            >
                                              <MinusCircle size={16} />
                                            </button>
                                            <button
                                              onClick={() =>
                                                handleOpenTransactionModal(
                                                  product,
                                                  "add"
                                                )
                                              }
                                              className="p-1.5 bg-blue-50 text-blue-600 hover:bg-blue-100 rounded-lg transition-colors"
                                              title="Ajustement personnalisé"
                                            >
                                              <Settings size={16} />
                                            </button>
                                            <button
                                              onClick={() =>
                                                handleOpenTransactionHistory(
                                                  product
                                                )
                                              }
                                              className="p-1.5 bg-gray-50 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
                                              title="Voir l'historique"
                                            >
                                              <FileText size={16} />
                                            </button>
                                          </div>
                                        </td>
                                      </tr>
                                    );
                                  })
                                ) : (
                                  <tr>
                                    <td
                                      colSpan="7"
                                      className="px-4 py-8 text-center text-gray-500"
                                    >
                                      <div className="flex flex-col items-center justify-center">
                                        <Package
                                          size={32}
                                          className="text-gray-300 mb-3"
                                        />
                                        <p className="mb-2">
                                          Aucun produit dans ce stock
                                        </p>
                                        <button
                                          onClick={() =>
                                            handleAddProductToStock(stock)
                                          }
                                          className="mt-1 px-3 py-1.5 bg-blue-100 text-blue-700 rounded-lg hover:bg-blue-200 transition-colors text-sm font-medium"
                                        >
                                          <Plus size={14} className="inline mr-1" />{" "}
                                          Ajouter un produit
                                        </button>
                                      </div>
                                    </td>
                                  </tr>
                                )}
                              </tbody>
                            </table>
                          </div>
                        </div>

                        {/* Transaction History (conditionally rendered) */}
                        {showTransactionHistory &&
                          selectedProduct &&
                          selectedProduct.idStock === stock.id && (
                            <div className="mt-6">
                              <TransactionHistory
                                transactions={transactions.filter(
                                  (t) => t.productId === selectedProduct.id
                                )}
                                onClose={() => {
                                  setShowTransactionHistory(false);
                                  setSelectedProduct(null);
                                }}
                                product={selectedProduct}
                              />
                            </div>
                          )}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div className="bg-white rounded-xl shadow-md p-8 text-center">
                <div className="flex flex-col items-center justify-center py-12">
                  <div className="bg-blue-100 p-4 rounded-full mb-4">
                    <Layers size={36} className="text-blue-500" />
                  </div>
                  <h3 className="text-lg font-medium text-gray-900 mb-2">
                    Aucun stock trouvé
                  </h3>
                  <p className="text-gray-500 mb-6 max-w-md mx-auto">
                    {searchTerm || storeFilter
                      ? "Aucun résultat ne correspond à votre recherche. Modifiez vos critères ou créez un nouveau stock."
                      : "Commencez par créer un stock pour gérer vos produits. Les stocks vous permettent d'organiser vos inventaires par boutique ou entrepôt."}
                  </p>
                  <button
                    onClick={handleAddStock}
                    className="px-5 py-2.5 bg-teal-500 text-white rounded-lg flex items-center hover:bg-blue-700 transition-colors shadow-md"
                  >
                    <Plus size={18} className="mr-2" /> Créer un nouveau stock
                  </button>
                </div>
              </div>
            )}

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="bg-white px-4 py-3 flex items-center justify-between rounded-lg shadow border border-gray-100">
                <div className="flex-1 flex justify-between items-center">
                <button
                    onClick={() => setCurrentPage((prev) => Math.max(prev - 1, 1))}
                    disabled={currentPage === 1}
                    className={`relative inline-flex items-center px-4 py-2 border ${
                      currentPage === 1
                        ? "border-gray-200 text-gray-400"
                        : "border-gray-300 text-gray-700 hover:bg-gray-50"
                    } text-sm font-medium rounded-md`}
                  >
                    <ChevronLeft size={16} className="mr-2" />
                    Précédent
                  </button>
                  <div className="text-sm text-gray-700">
                    Page {currentPage} sur {totalPages}
                  </div>
                  <button
                    onClick={() =>
                      setCurrentPage((prev) => Math.min(prev + 1, totalPages))
                    }
                    disabled={currentPage === totalPages}
                    className={`relative inline-flex items-center px-4 py-2 border ${
                      currentPage === totalPages
                        ? "border-gray-200 text-gray-400"
                        : "border-gray-300 text-gray-700 hover:bg-gray-50"
                    } text-sm font-medium rounded-md`}
                  >
                    Suivant
                    <ChevronRight size={16} className="ml-2" />
                  </button>
                </div>
              </div>
            )}
        </div>
      </div>

      {/* Transaction Modal */}
      {showTransactionModal && selectedProduct && (
        <div className="fixed inset-0 bg-gray-800 bg-opacity-75 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full animate-fadeIn transform transition-all">
            <div
              className={`rounded-t-lg p-4 ${
                transactionType === "add"
                  ? "bg-gradient-to-r from-green-500 to-green-600"
                  : "bg-gradient-to-r from-red-500 to-red-600"
              }`}
            >
              <div className="flex justify-between items-center">
                <h3 className="text-lg font-medium text-white flex items-center">
                  {transactionType === "add"
                    ? "Ajouter du stock"
                    : "Retirer du stock"}
                </h3>
                <button
                  onClick={() => setShowTransactionModal(false)}
                  className="text-white hover:bg-white hover:bg-opacity-20 rounded-full p-1 transition-colors"
                >
                  <X size={18} />
                </button>
              </div>
            </div>

            <div className="p-5">
              <div className="mb-4 bg-gray-50 p-4 rounded-lg border border-gray-200">
                <div className="flex items-center">
                  <div className="h-12 w-12 flex-shrink-0 bg-gray-100 rounded-lg flex items-center justify-center">
                    {selectedProduct.images && selectedProduct.images.length > 0 ? (
                      <img 
                        src={selectedProduct.images.find(img => img.imagePrincipale)?.url || selectedProduct.images[0].url} 
                        alt={selectedProduct.nomProduit}
                        className="h-12 w-12 object-cover rounded-lg"
                      />
                    ) : (
                      <Package size={20} className="text-gray-500" />
                    )}
                  </div>
                  <div className="ml-4">
                    <div className="text-lg font-medium text-gray-900">
                      {selectedProduct.nomProduit}
                    </div>
                    <div className="text-sm text-gray-600">
                      Stock actuel: {selectedProduct.quantite}
                    </div>
                  </div>
                </div>
              </div>

              <div className="mb-5">
                <label
                  htmlFor="quantity"
                  className="block text-sm font-medium text-gray-700 mb-2"
                >
                  Quantité à {transactionType === "add" ? "ajouter" : "retirer"}
                </label>
                <div className="flex">
                  <button
                    onClick={() =>
                      setTransactionQuantity(
                        Math.max(1, transactionQuantity - 1)
                      )
                    }
                    className="px-3 py-2 border border-gray-300 bg-gray-50 rounded-l-lg text-gray-600 hover:bg-gray-100"
                  >
                    <MinusCircle size={18} />
                  </button>
                  <input
                    type="number"
                    id="quantity"
                    value={transactionQuantity}
                    onChange={(e) =>
                      setTransactionQuantity(
                        Math.max(1, Number(e.target.value))
                      )
                    }
                    min="1"
                    step="1"
                    className="block w-full border-y border-gray-300 py-2 text-center text-gray-900 focus:ring-blue-500 focus:border-blue-500 text-lg font-medium"
                  />
                  <button
                    onClick={() =>
                      setTransactionQuantity(transactionQuantity + 1)
                    }
                    className="px-3 py-2 border border-gray-300 bg-gray-50 rounded-r-lg text-gray-600 hover:bg-gray-100"
                  >
                    <PlusCircle size={18} />
                  </button>
                </div>
              </div>

              <div className="flex items-center justify-end space-x-3 pt-4 border-t border-gray-200">
                <button
                  type="button"
                  className="px-4 py-2 bg-gray-100 text-gray-800 rounded-lg hover:bg-gray-200 transition-colors font-medium"
                  onClick={() => setShowTransactionModal(false)}
                >
                  Annuler
                </button>
                <button
                  type="button"
                  className={`px-4 py-2 text-white rounded-lg font-medium flex items-center ${
                    transactionType === "add"
                      ? "bg-green-600 hover:bg-green-700"
                      : "bg-red-600 hover:bg-red-700"
                  }`}
                  onClick={handleSubmitTransaction}
                >
                  {transactionType === "add" ? (
                    <Plus size={18} className="mr-1" />
                  ) : (
                    <MinusCircle size={18} className="mr-1" />
                  )}
                  Confirmer
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default StockManagement;