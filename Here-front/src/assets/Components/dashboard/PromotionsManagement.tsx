import React, { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import axios from 'axios'; // Ajoutez l'import d'axios
import {
  Plus,
  Search,
  Edit,
  Trash2,
  ChevronLeft,
  ChevronRight,
  Tag,
  Percent,
  Calendar,
  CheckCircle,
  XCircle,
  Package,
} from "lucide-react";
import { auth } from "../../../config/Firebase";
import DashboardNavigation from "./DashboardNavigation";
import PromotionForm from "./PromotionForm";
import { toast } from "react-toastify";

// Créer le service API directement dans le fichier
const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },

});

// Service pour les réductions
const reductionService = {
  // Récupérer toutes les réductions
  getAllReductions: async () => {
    const response = await apiClient.get('/reductions');
    return response.data;
  },

  // Récupérer une réduction par son ID
  getReductionById: async (id) => {
    const response = await apiClient.get(`/reductions/${id}`);
    return response.data;
  },

  // Créer une nouvelle réduction
  createReduction: async (reductionData) => {
    const response = await apiClient.post('/reductions', reductionData);
    return response.data;
  },

  // Mettre à jour une réduction existante
  updateReduction: async (id, reductionData) => {
    const response = await apiClient.put(`/reductions/${id}`, reductionData);
    return response.data;
  },

  // Supprimer une réduction
  deleteReduction: async (id) => {
    const response = await apiClient.delete(`/reductions/${id}`);
    return response.data;
  },

  // Service pour les produits
  getAllProducts: async () => {
    const response = await apiClient.get('/produits');
    return response.data;
  }
};

const PromotionsManagement = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [loading, setLoading] = useState(true);
  const [promotions, setPromotions] = useState([]);
  const [products, setProducts] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editingPromotion, setEditingPromotion] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage] = useState(10);

  console.log("PromotionsManagement rendered at path:", location.pathname);

  // Check user authentication and role
  useEffect(() => {
    const unsubscribe = auth.onAuthStateChanged(async (user) => {
      if (!user) {
        navigate("/login?redirect=account/promotions");
        return;
      }

      try {
        // Load promotions and products data
        loadData();
      } catch (error) {
        console.error("Error fetching data:", error);
        toast.error("Erreur lors du chargement des données");
        setLoading(false);
      }
    });

    return () => unsubscribe();
  }, [navigate]);

  // Load data from API
  const loadData = async () => {
    setLoading(true);
    try {
      console.group('Chargement des promotions');
      console.log("Début de la récupération");
  
      const response = await apiClient.get('/reductions', {
        timeout: 10000,
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        transformResponse: [
          (data) => {
            try {
              const parsedData = JSON.parse(data);
              console.log('Données brutes parsées:', parsedData);
              return parsedData;
            } catch (e) {
              console.error('Erreur de parsing JSON:', e);
              throw new Error('Réponse invalide du serveur');
            }
          }
        ]
      });
  
      console.log('Réponse complète:', response);
      
      // Validation stricte des données
      if (Array.isArray(response.data)) {
        const validPromotions = response.data.filter(promo => 
          promo && 
          promo.id !== undefined && 
          promo.nom && 
          promo.pourcentage_reduction !== undefined &&
          promo.periode_debut &&
          promo.periode_fin
        );
        
        console.log('Promotions valides:', validPromotions);
        setPromotions(validPromotions);
      } else {
        console.error('Format de données inattendu:', response.data);
        toast.error('Format des promotions incorrect');
      }
  
      console.groupEnd();
      setLoading(false);
    } catch (error: any) {
      console.groupEnd();
      console.error('Erreur détaillée de chargement:', error);
  
      // Gestion des différents types d'erreurs
      if (error.response) {
        // Erreur de réponse du serveur
        const errorDetails = error.response.data || {};
        toast.error(
          `Erreur serveur : ${
            errorDetails.message || 
            errorDetails.error || 
            'Erreur inconnue'
          }`
        );
      } else if (error.request) {
        // Requête envoyée mais pas de réponse
        toast.error('Aucune réponse reçue du serveur');
      } else {
        // Erreur de configuration de la requête
        toast.error(`Erreur : ${error.message}`);
      }
      
      setLoading(false);
    }
  };
  // Handle adding new promotion
  const handleAddPromotion = () => {
    setEditingPromotion(null);
    setShowForm(true);
  };

  // Handle editing promotion
  const handleEditPromotion = (promotion) => {
    setEditingPromotion(promotion);
    setShowForm(true);
  };

  // Handle deleting promotion
  const handleDeletePromotion = async (promotionId) => {
    if (window.confirm("Êtes-vous sûr de vouloir supprimer cette promotion?")) {
      try {
        await reductionService.deleteReduction(promotionId);
        
        // Update local state
        setPromotions(
          promotions.filter((promotion) => promotion.id !== promotionId)
        );
        toast.success("Promotion supprimée avec succès");
      } catch (error) {
        console.error("Error deleting promotion:", error);
        toast.error("Erreur lors de la suppression de la promotion");
      }
    }
  };

  // Handle toggling promotion active status
  const handleToggleActive = async (promotionId) => {
    try {
      const promotion = promotions.find((p) => p.id === promotionId);
      const updatedPromotion = { ...promotion, actif: !promotion.actif };
      
      await reductionService.updateReduction(promotionId, updatedPromotion);
      
      // Update local state
      const updatedPromotions = promotions.map((p) =>
        p.id === promotionId ? { ...p, actif: !p.actif } : p
      );
      setPromotions(updatedPromotions);
      
      toast.success(`Promotion ${updatedPromotion.actif ? 'activée' : 'désactivée'} avec succès`);
    } catch (error) {
      console.error("Error toggling promotion status:", error);
      toast.error("Erreur lors de la modification du statut de la promotion");
    }
  };

  // Handle form submission
  // Handle form submission
const handleFormSubmit = async (formData) => {
  try {
    // Adapter les données pour correspondre au modèle backend
    const reductionData = {
      nom: formData.nom,
      pourcentage_reduction: formData.pourcentage_reduction,
      actif: formData.actif,
      periode_debut: formData.periode_debut,
      periode_fin: formData.periode_fin
      // Supprimez la ligne référençant id_produit
    };

    let updatedPromotion;
    if (editingPromotion) {
      // Update existing promotion
      updatedPromotion = await reductionService.updateReduction(
        editingPromotion.id,
        reductionData
      );
      
      // Update local state
      const updatedPromotions = promotions.map((promotion) =>
        promotion.id === editingPromotion.id ? updatedPromotion : promotion
      );
      setPromotions(updatedPromotions);
      toast.success("Promotion mise à jour avec succès");
    } else {
      // Add new promotion
      updatedPromotion = await reductionService.createReduction(reductionData);
      
      // Update local state
      setPromotions([...promotions, updatedPromotion]);
      toast.success("Promotion ajoutée avec succès");
    }
    
    setShowForm(false);
  } catch (error) {
    console.error("Error saving promotion:", error);
    toast.error("Erreur lors de l'enregistrement de la promotion");
  }
};

  // Filter and search logic
  const filteredPromotions = promotions.filter((promotion) => {
    return promotion.nom.toLowerCase().includes(searchTerm.toLowerCase());
  });

  // Pagination logic
  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentItems = filteredPromotions.slice(
    indexOfFirstItem,
    indexOfLastItem
  );
  const totalPages = Math.ceil(filteredPromotions.length / itemsPerPage);

  // Format date function
  const formatDate = (dateString) => {
    const options = { year: "numeric", month: "short", day: "numeric" };
    return new Date(dateString).toLocaleDateString("fr-FR", options);
  };

  if (loading) {
    return (
      <div className="w-full p-6 flex justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-teal-500"></div>
      </div>
    );
  }

  if (showForm) {
    return (
      <div className="space-y-6">
        <DashboardNavigation />
        <PromotionForm
          promotion={editingPromotion}
          products={products}
          onSubmit={handleFormSubmit}
          onCancel={() => setShowForm(false)}
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Dashboard Navigation */}
      <DashboardNavigation />

      {/* Promotions Management Content */}
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-800">
              Gestion des Promotions
            </h1>
            <p className="text-gray-500 mt-1">
              Créez et gérez des réductions pour vos produits
            </p>
          </div>
          <button
            onClick={handleAddPromotion}
            className="px-4 py-2 text-sm font-medium text-white bg-teal-600 rounded-md shadow-sm hover:bg-teal-700 flex items-center"
          >
            <Plus size={16} className="inline mr-2" />
            Ajouter une Promotion
          </button>
        </div>

        {/* Search */}
        <div className="flex flex-col md:flex-row gap-4">
          <div className="relative flex-grow">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Search size={18} className="text-gray-400" />
            </div>
            <input
              type="text"
              className="block w-full pl-10 pr-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-teal-500 focus:border-teal-500"
              placeholder="Rechercher une promotion par nom"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>

        {/* Promotions Table */}
        <div className="bg-white shadow-sm rounded-lg overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Promotion
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Réduction
                  </th>
                  
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Période
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Statut
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {currentItems.length > 0 ? (
                  currentItems.map((promotion) => {
                    const product = products.find(
                      (p) => p.id === promotion.id_produit
                    );

                    return (
                      <tr key={promotion.id}>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="flex items-center">
                            <div className="h-10 w-10 flex-shrink-0 overflow-hidden rounded-md bg-red-100 flex items-center justify-center">
                              <Percent size={20} className="text-red-500" />
                            </div>
                            <div className="ml-4">
                              <div className="text-sm font-medium text-gray-900">
                                {promotion.nom}
                              </div>
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">
                            {promotion.pourcentage_reduction}%
                          </span>
                        </td>
                        
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          <div className="flex items-center">
                            <Calendar
                              size={16}
                              className="text-gray-400 mr-2"
                            />
                            {formatDate(promotion.periode_debut)} -{" "}
                            {formatDate(promotion.periode_fin)}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <button
                            onClick={() => handleToggleActive(promotion.id)}
                            className={`inline-flex items-center px-2.5 py-1.5 border border-transparent text-xs font-medium rounded 
                              ${
                                promotion.actif
                                  ? "bg-green-100 text-green-800 hover:bg-green-200"
                                  : "bg-gray-100 text-gray-800 hover:bg-gray-200"
                              }`}
                          >
                            {promotion.actif ? (
                              <>
                                <CheckCircle size={14} className="mr-1" />
                                Actif
                              </>
                            ) : (
                              <>
                                <XCircle size={14} className="mr-1" />
                                Inactif
                              </>
                            )}
                          </button>
                          </td>
                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                          <button
                            onClick={() => handleEditPromotion(promotion)}
                            className="text-teal-600 hover:text-teal-900 mr-4"
                          >
                            <Edit size={18} />
                          </button>
                          <button
                            onClick={() => handleDeletePromotion(promotion.id)}
                            className="text-red-600 hover:text-red-900"
                          >
                            <Trash2 size={18} />
                          </button>
                        </td>
                      </tr>
                    );
                  })
                ) : (
                  <tr>
                    <td
                      colSpan="6"
                      className="px-6 py-4 text-center text-sm text-gray-500"
                    >
                      Aucune promotion trouvée. Ajoutez une nouvelle promotion.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6">
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
        </div>
      </div>
    </div>
  );
};

export default PromotionsManagement;