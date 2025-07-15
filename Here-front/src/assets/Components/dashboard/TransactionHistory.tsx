import React, { useState, useEffect } from "react";
import axios from "axios";
import {
  X,
  AlertTriangle,
  Calendar,
  FileText,
  Filter,
  PlusCircle,
  MinusCircle,
  ChevronLeft,
  ChevronRight,
  Clock,
  Package,
  DollarSign,
  Users,
  Info,
} from "lucide-react";

// Define types for props and transactions
interface Product {
  id: string;
  nomProduit: string;
  quantite?: number;
  prix?: number;
  idStock?: string;
  description?: string;
}

interface Transaction {
  id: string;
  productId: string;
  stockId: string;
  type: string;
  quantity: number;
  transactionDate: string; // Utiliser transactionDate au lieu de date
  notes?: string;
  prixUnitaire?: number;
  revenuTotal?: number;
}

interface TransactionHistoryProps {
  product: Product;
  transactions: Transaction[];
  onClose: () => void;
  onAddTransaction?: (transaction: any) => void;
}

const TransactionHistory: React.FC<TransactionHistoryProps> = ({
  product,
  transactions,
  onClose,
  onAddTransaction
}) => {
  const [filterType, setFilterType] = useState<"all" | "add" | "remove">("all");
  const [currentPage, setCurrentPage] = useState(1);
  const [productRevenue, setProductRevenue] = useState<number>(0);
  const [totalStockRevenue, setTotalStockRevenue] = useState<number>(0);
  const [localTransactions, setLocalTransactions] = useState<Transaction[]>([]);
  const transactionsPerPage = 8;

  // Fonction pour sauvegarder les transactions dans localStorage
  const saveTransactionsToLocalStorage = (productId: string, transactionsList: Transaction[]) => {
    try {
      localStorage.setItem(`transactions_${productId}`, JSON.stringify(transactionsList));
      console.log(`${transactionsList.length} transactions sauvegardées localement pour le produit ${productId}`);
    } catch (error) {
      console.error("Erreur lors de la sauvegarde des transactions dans localStorage:", error);
    }
  };

  // Fonction pour charger les transactions depuis localStorage
  const loadTransactionsFromLocalStorage = (productId: string): Transaction[] => {
    try {
      const savedData = localStorage.getItem(`transactions_${productId}`);
      if (savedData) {
        const parsedData = JSON.parse(savedData);
        console.log(`${parsedData.length} transactions chargées depuis localStorage pour le produit ${productId}`);
        return parsedData;
      }
      return [];
    } catch (error) {
      console.error("Erreur lors du chargement des transactions depuis localStorage:", error);
      return [];
    }
  };
  
  // Fonction pour normaliser les IDs
  const normalizeId = (id: any): any => {
    if (id === null || id === undefined) return null;
    
    if (typeof id === 'number') return id;
    
    if (typeof id === 'string') {
      // Si l'ID a un format comme "STK-001", extraire seulement les chiffres
      if (id.includes('-')) {
        const cleanId = id.replace(/\D/g, '');
        return cleanId ? parseInt(cleanId, 10) : id;
      }
      
      // Sinon essayer de le convertir directement
      const numericId = parseInt(id, 10);
      return !isNaN(numericId) ? numericId : id;
    }
    
    return id;
  };

  // Afficher la structure des données pour le débogage
  useEffect(() => {
    if (transactions.length > 0) {
      console.log("Structure d'une transaction:", JSON.stringify(transactions[0], null, 2));
      console.log("Propriétés disponibles:", Object.keys(transactions[0]));
    }
  }, [transactions]);

  // Fonction pour normaliser le type de transaction
  const normalizeTransactionType = (type: string): "add" | "remove" => {
    if (!type) return "remove";
    
    const lowerType = String(type).toLowerCase();
    
    if (lowerType.includes("add") || 
        lowerType.includes("ajout") || 
        lowerType.includes("entr") || 
        lowerType.includes("depot") ||
        lowerType.includes("stock")) {
      return "add";
    }
    
    return "remove";
  };

  // Fonction pour déterminer si une transaction est une entrée
  const isAddTransaction = (transaction: Transaction): boolean => {
    if (!transaction) return false;
    
    if (transaction.notes && String(transaction.notes).toLowerCase().includes("ajout")) {
      return true;
    }
    
    return normalizeTransactionType(transaction.type) === "add";
  };

  // Préparer les transactions avec les dates formatées et types normalisés
  const prepareTransactions = (transactions: Transaction[]) => {
    return transactions.map(transaction => {
      const normalizedType = isAddTransaction(transaction) ? "add" : "remove";
      
      let displayDate = "Date non spécifiée";
      
      // Utiliser transactionDate au lieu de date
      if (transaction.transactionDate) {
        try {
          const date = new Date(transaction.transactionDate);
          
          if (!isNaN(date.getTime())) {
            displayDate = date.toLocaleDateString("fr-FR", {
              year: "numeric",
              month: "short",
              day: "numeric",
              hour: "2-digit",
              minute: "2-digit"
            });
          } else {
            console.log("Date invalide:", transaction.transactionDate);
          }
        } catch (e) {
          console.error("Erreur lors du formatage de la date:", e);
          displayDate = "Erreur de date";
        }
      }

      return {
        ...transaction,
        normalizedType,
        displayDate
      };
    });
  };

  // Fonction pour calculer le revenu généré pour un produit spécifique
  const calculateProductRevenue = (productId: string, transactions: Transaction[]): number => {
    return transactions
      .filter(t => t.productId === productId && !isAddTransaction(t))
      .reduce((total, t) => {
        if (t.revenuTotal) return total + Number(t.revenuTotal);
        
        const price = t.prixUnitaire || product.prix || 0;
        return total + (Number(price) * t.quantity);
      }, 0);
  };

  // Fonction pour calculer le revenu total pour tous les produits dans un stock
  const calculateTotalStockRevenue = (stockId: string, allTransactions: Transaction[]): number => {
    return allTransactions
      .filter(t => t.stockId === stockId && !isAddTransaction(t))
      .reduce((total, t) => {
        if (t.revenuTotal) return total + Number(t.revenuTotal);
        
        const price = t.prixUnitaire || 0;
        return total + (Number(price) * t.quantity);
      }, 0);
  };

  // Useeffect pour gérer le chargement des données et la persistance
  useEffect(() => {
    if (!product) return;

    // Si aucune transaction n'est reçue en props, essayer de charger depuis localStorage
    let transactionsToUse = [...transactions];
    
    if (transactions.length === 0) {
      const loadedTransactions = loadTransactionsFromLocalStorage(product.id);
      if (loadedTransactions.length > 0) {
        setLocalTransactions(loadedTransactions);
        transactionsToUse = loadedTransactions;
      }
    } else {
      // Si des transactions sont fournies, les sauvegarder dans localStorage
      saveTransactionsToLocalStorage(product.id, transactions);
    }

    // Calculer les revenus côté client
    const calculatedProductRevenue = calculateProductRevenue(product.id, transactionsToUse);
    setProductRevenue(calculatedProductRevenue);

    if (product.idStock) {
      const calculatedStockRevenue = calculateTotalStockRevenue(product.idStock, transactionsToUse);
      setTotalStockRevenue(calculatedStockRevenue);
    }

    // Essayer également de charger depuis l'API si disponible
    const loadRevenuesFromAPI = async () => {
      try {
        // Normaliser l'ID du produit
        const productId = normalizeId(product.id);
        console.log(`ID du produit normalisé: ${productId}`);
        
        // Essayer de charger le revenu du produit depuis l'API
        try {
          console.log(`Tentative de chargement du revenu pour le produit ${productId}...`);
          const productRevenueResponse = await axios.get(`/api/stock-transactions/produit/${productId}/revenue`);
          
          if (productRevenueResponse.data !== undefined) {
            const revenueValue = typeof productRevenueResponse.data === 'string' 
              ? parseFloat(productRevenueResponse.data)
              : productRevenueResponse.data;
              
            if (!isNaN(revenueValue)) {
              console.log(`Revenu du produit chargé depuis l'API: ${revenueValue}`);
              setProductRevenue(revenueValue);
            }
          }
        } catch (error) {
          console.warn(`Impossible de charger le revenu du produit depuis l'API: ${error.message}`);
        }

        // Normaliser l'ID du stock
        if (product.idStock) {
          const stockId = normalizeId(product.idStock);
          console.log(`ID du stock normalisé: ${stockId}`);
          
          try {
            console.log(`Tentative de chargement du revenu pour le stock ${stockId}...`);
            const stockRevenueResponse = await axios.get(`/api/stock-transactions/stock/${stockId}/revenue`);
            
            if (stockRevenueResponse.data !== undefined) {
              const revenueValue = typeof stockRevenueResponse.data === 'string' 
                ? parseFloat(stockRevenueResponse.data)
                : stockRevenueResponse.data;
                
              if (!isNaN(revenueValue)) {
                console.log(`Revenu du stock chargé depuis l'API: ${revenueValue}`);
                setTotalStockRevenue(revenueValue);
              }
            }
          } catch (error) {
            console.warn(`Impossible de charger le revenu du stock depuis l'API: ${error.message}`);
          }
        }
      } catch (error) {
        console.log("Impossible de charger les revenus depuis l'API, utilisation des calculs côté client");
      }
    };

    loadRevenuesFromAPI();
  }, [product, transactions]);

  if (!product) return null;

  // Utiliser les transactions de l'état ou du localStorage si aucune transaction n'est fournie
  const allTransactions = transactions.length > 0 ? transactions : localTransactions;

  // Filter transactions for this product
  const filteredTransactions = allTransactions
    .filter((t) => t.productId === product.id)
    .filter((t) => {
      if (filterType === "all") return true;
      return filterType === (isAddTransaction(t) ? "add" : "remove");
    });

  // Préparer les transactions avec les dates formatées et les trier
  const productTransactions = prepareTransactions(filteredTransactions)
    .sort((a, b) => {
      // Pour le tri, utiliser transactionDate au lieu de date
      if (a.transactionDate && b.transactionDate) {
        const dateA = new Date(a.transactionDate).getTime();
        const dateB = new Date(b.transactionDate).getTime();
        
        if (!isNaN(dateA) && !isNaN(dateB)) {
          return dateB - dateA;
        }
      }
      
      // Retomber sur l'ID si les dates ne sont pas utilisables
      if (a.id && b.id) {
        const idA = parseInt(a.id.toString());
        const idB = parseInt(b.id.toString());
        
        if (!isNaN(idA) && !isNaN(idB)) {
          return idB - idA;
        }
        
        return a.id > b.id ? -1 : 1;
      }
      
      return 0;
    });

  // Calculate total added and removed quantities
  const totalAdded = filteredTransactions
    .filter(t => isAddTransaction(t))
    .reduce((sum, t) => sum + t.quantity, 0);
  
  const totalRemoved = filteredTransactions
    .filter(t => !isAddTransaction(t))
    .reduce((sum, t) => sum + t.quantity, 0);

  // Calculate stock movement balance
  const stockMovementBalance = totalAdded - totalRemoved;
  
  // Calculate pagination
  const indexOfLastTransaction = currentPage * transactionsPerPage;
  const indexOfFirstTransaction = indexOfLastTransaction - transactionsPerPage;
  
  // Fonctions sécurisées pour la pagination
  function indexOfFirstItem() {
    return Math.max(0, indexOfFirstTransaction);
  }
  
  function indexOfLastItem() {
    return Math.min(indexOfLastTransaction, productTransactions.length);
  }
  
  const currentTransactions = productTransactions.slice(
    indexOfFirstItem(),
    indexOfLastItem()
  );
  
  // Calcul sécurisé du nombre de pages
  const totalPages = Math.max(1, Math.ceil(
    productTransactions.length / transactionsPerPage
  ));

  return (
    <div className="bg-white shadow-lg rounded-lg overflow-hidden border border-gray-200">
      <div className="px-6 py-4 bg-gradient-to-r from-teal-500 to-teal-600 flex justify-between items-center">
        <h2 className="text-xl font-semibold text-white flex items-center">
          <FileText size={20} className="mr-2" />
          Historique des transactions
        </h2>
        <button
          onClick={onClose}
          className="text-white hover:text-gray-100 transition-colors duration-150"
          aria-label="Fermer"
        >
          <X size={24} />
        </button>
      </div>

      <div className="p-6">
        <div className="space-y-6">
          {/* Product Info avec explication de la balance des mouvements */}
          <div className="bg-gray-50 p-4 rounded-lg border border-gray-200 flex items-center">
            <div className="h-12 w-12 flex-shrink-0 rounded-lg bg-teal-100 flex items-center justify-center mr-4">
              <Package size={20} className="text-teal-600" />
            </div>
            <div>
              <h3 className="text-lg font-medium text-gray-900">
                {product.nomProduit}
              </h3>
              <div className="flex flex-wrap items-center mt-1 text-sm text-gray-600">
                {product.prix && (
                  <>
                    <DollarSign size={14} className="mr-1" />
                    {parseFloat(product.prix.toString()).toFixed(2)} €
                    <span className="mx-2">•</span>
                  </>
                )}
                <div className="flex items-center">
                  <Package size={14} className="mr-1" />
                  <span>Stock actuel: {product.quantite || 0}</span> 
                </div>
                <div className="flex items-center ml-2">
                  <span className={`text-xs p-1 rounded ${stockMovementBalance >= 0 ? 'bg-blue-50 text-blue-700' : 'bg-amber-50 text-amber-700'}`}>
                    (Balance des mouvements: {stockMovementBalance >= 0 ? '+' : ''}{stockMovementBalance})
                  </span>
                </div>
              </div>
              <div className="mt-1 text-xs text-gray-500">
                <span><strong>Balance des mouvements</strong> = Total des entrées - Total des sorties</span>
              </div>
            </div>
          </div>

          {/* Filter Tabs */}
          <div className="border-b border-gray-200">
            <div className="flex space-x-1">
              <button
                onClick={() => setFilterType("all")}
                className={`py-2 px-4 border-b-2 font-medium text-sm ${
                  filterType === "all"
                    ? "border-teal-500 text-teal-600"
                    : "border-transparent text-gray-500 hover:text-gray-700"
                }`}
              >
                Toutes les transactions
              </button>
              <button
                onClick={() => setFilterType("add")}
                className={`py-2 px-4 border-b-2 font-medium text-sm ${
                  filterType === "add"
                    ? "border-green-500 text-green-600"
                    : "border-transparent text-gray-500 hover:text-gray-700"
                }`}
              >
                Entrées
              </button>
              <button
                onClick={() => setFilterType("remove")}
                className={`py-2 px-4 border-b-2 font-medium text-sm ${
                  filterType === "remove"
                    ? "border-red-500 text-red-600"
                    : "border-transparent text-gray-500 hover:text-gray-700"
                }`}
              >
                Sorties
              </button>
            </div>
          </div>

          {/* Transactions Table */}
          <div className="overflow-x-auto rounded-lg border border-gray-200">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Date
                  </th>
                  <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Type
                  </th>
                  <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Quantité
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Détails
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {currentTransactions.length > 0 ? (
                  currentTransactions.map((transaction) => (
                    <tr
                      key={transaction.id}
                      className="hover:bg-gray-50 transition-colors"
                    >
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center text-sm text-gray-900">
                          <Calendar size={14} className="mr-2 text-gray-400" />
                          {transaction.displayDate}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-center">
                        <span
                          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                            transaction.normalizedType === "add"
                              ? "bg-green-100 text-green-800"
                              : "bg-red-100 text-red-800"
                          }`}
                        >
                          {transaction.normalizedType === "add" ? (
                            <PlusCircle size={12} className="mr-1" />
                          ) : (
                            <MinusCircle size={12} className="mr-1" />
                          )}
                          {transaction.normalizedType === "add" ? "Entrée" : "Sortie"}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-center text-sm font-medium text-gray-900">
                        {transaction.quantity}
                        {transaction.normalizedType === "remove" && (
                          <span className="text-xs text-gray-500 block">
                            ({
                              ((transaction.prixUnitaire || product.prix || 0) * transaction.quantity).toFixed(2)
                            } €)
                          </span>
                        )}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {transaction.notes || "-"}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td
                      colSpan={4}
                      className="px-6 py-10 text-center text-sm text-gray-500"
                    >
                      <FileText
                        size={24}
                        className="mx-auto mb-2 text-gray-300"
                      />
                      <p>
                        Aucune transaction{" "}
                        {filterType !== "all" &&
                          (filterType === "add"
                            ? "d'entrée"
                            : "de sortie")}{" "}
                        trouvée
                      </p>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-gray-200 pt-4">
              <div className="flex-1 flex justify-between items-center">
                <button
                  onClick={() =>
                    setCurrentPage((prev) => Math.max(prev - 1, 1))
                  }
                  disabled={currentPage === 1}
                  className={`relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 ${
                    currentPage === 1 ? "opacity-50 cursor-not-allowed" : ""
                  }`}
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
                  className={`relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 ${
                    currentPage === totalPages
                      ? "opacity-50 cursor-not-allowed"
                      : ""
                  }`}
                >
                  Suivant
                  <ChevronRight size={16} className="ml-2" />
                </button>
              </div>
            </div>
          )}

          {/* Transaction Summary avec explication de la balance */}
          <div className="bg-blue-50 border border-blue-100 rounded-lg p-4 flex items-start">
            <Info
              size={20}
              className="text-blue-500 mr-3 flex-shrink-0 mt-0.5"
            />
            <div className="text-sm text-blue-800">
              <h4 className="font-medium mb-1">Résumé des transactions</h4>
              <ul className="space-y-1">
                <li>
                  Total des entrées:{" "}
                  <span className="font-medium">
                    {totalAdded}
                  </span>{" "}
                  unités
                </li>
                <li>
                  Total des sorties:{" "}
                  <span className="font-medium">
                    {totalRemoved}
                  </span>{" "}
                  unités
                </li>
                <li>
                  Revenu généré pour ce produit:{" "}
                  <span className="font-medium text-green-600">
                    {productRevenue.toFixed(2)} €
                  </span>
                </li>
                <li>
                  Revenus Totaux du stock:{" "}
                  <span className="font-medium text-blue-600">
                    {totalStockRevenue.toFixed(2)} €
                  </span>
                </li>
                <li>
                  Balance des mouvements:{" "}
                  <span className={`font-medium ${stockMovementBalance >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                    {stockMovementBalance >= 0 ? '+' : ''}{stockMovementBalance}
                  </span>{" "}
                  unités
                </li>
                <li>
                  Capacité actuelle:{" "}
                  <span className="font-medium">
                    {product.quantite || 0}
                  </span>{" "}
                  unités sur{" "}
                  <span className="font-medium">
                    {Math.max((product.quantite || 0) + totalRemoved - totalAdded, 0)}
                  </span>{" "}
                  initiales
                  ({(product.quantite !== undefined && (product.quantite + totalRemoved - totalAdded) > 0) ? 
                    Math.round((product.quantite / Math.max(product.quantite + totalRemoved - totalAdded, 1)) * 100) : 100}% utilisé)
                </li>
                <li>
                  Dernière transaction:{" "}
                  <span className="font-medium">
                    {productTransactions.length > 0
                      ? productTransactions[0].displayDate
                      : "-"}
                  </span>
                </li>
              </ul>
            </div>
          </div>

          {/* Actions */}
          <div className="flex justify-end pt-4">
            <button
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500 transition-colors duration-150"
            >
              Fermer
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TransactionHistory;