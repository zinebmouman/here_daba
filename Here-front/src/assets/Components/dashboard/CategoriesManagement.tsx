import React, { useState, useEffect } from "react";
import { Plus, Edit, Trash2, Tag, AlertCircle, Search } from "lucide-react";
import { auth } from "../../../config/Firebase";
import CategoryForm from "./CategoryForm";
import CategoryIcon from "./CategoryIcon";
import DashboardNavigation from "./DashboardNavigation";

const CategoriesManagement = () => {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [editingCategory, setEditingCategory] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");

  // Charger les catégories - maintenant indépendantes des boutiques
 // Modifiez ces lignes dans votre composant CategoriesManagement.tsx

// Fonction loadCategories simplifiée pour le débogage

const loadCategories = async () => {
  try {
    console.group('🚀 Chargement des Catégories');
    console.time('loadCategories');
    
    setLoading(true);
    setError("");
    
    const user = auth.currentUser;
    
    console.log('👤 Utilisateur connecté:', {
      uid: user?.uid,
      email: user?.email,
      isAuthenticated: !!user
    });

    // VERSION SIMPLIFIÉE - sans token d'authentification
    const apiUrl = `/api/categories`;
    console.log('🌐 URL API:', apiUrl);
    
    // Effectuer un appel sans headers d'authentification pour tester
    const response = await fetch(apiUrl);
    
    console.log('📥 Réponse reçue:', {
      status: response.status,
      ok: response.ok
    });

    if (!response.ok) {
      if (response.status === 404) {
        console.warn("🔍 Aucune catégorie trouvée");
        setCategories([]);
      } else {
        const errorText = await response.text();
        console.error('❌ Erreur serveur:', {
          status: response.status,
          message: errorText
        });
        
        throw new Error(`Erreur HTTP: ${response.status} - ${errorText}`);
      }
    } else {
      const categoriesData = await response.json();
      console.log('📊 Catégories chargées:', {
        nombre: categoriesData.length,
        premières: categoriesData.slice(0, 3)
      });
      
      setCategories(categoriesData);
    }
  } catch (error) {
    console.error('🚨 Erreur complète:', error);
    setError(`Impossible de charger les catégories : ${error.message}`);
  } finally {
    console.timeEnd('loadCategories');
    console.groupEnd();
    setLoading(false);
  }
};
// Modification de l'useEffect existant
useEffect(() => {
    loadCategories();
}, []);

  // Filtrer les catégories en fonction de la recherche
  const getFilteredCategories = () => {
    if (!searchQuery.trim()) {
      return categories;
    }
    
    return categories.filter(category => 
      category.nom?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (category.description && category.description.toLowerCase().includes(searchQuery.toLowerCase()))
    );
  };

  // Gérer la création d'une catégorie
  const handleCreateCategory = async (categoryData) => {
    try {
      const user = auth.currentUser;
      if (!user) {
        console.error("No authenticated user");
        return;
      }

      const idToken = await user.getIdToken(true);
      const response = await fetch('/api/categories', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${idToken}`,
          'X-Vendeur-ID': user.uid
        },
        body: JSON.stringify(categoryData)
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`HTTP error: ${response.status} - ${errorText}`);
      }

      const savedCategory = await response.json();
      
      // Ajout dans l'état local pour l'UI
      setCategories([...categories, savedCategory]);
      setShowForm(false);
    } catch (error) {
      console.error("Error creating category:", error);
      setError("Failed to create category: " + error.message);
    }
  };

  // Gérer la mise à jour d'une catégorie
  const handleUpdateCategory = async (updatedCategory) => {
    try {
      if (!editingCategory) return;

      const user = auth.currentUser;
      if (!user) return;

      const idToken = await user.getIdToken(true);
      const response = await fetch(`/api/categories/${updatedCategory.idCategorie}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${idToken}`,
          'X-Vendeur-ID': user.uid
        },
        body: JSON.stringify(updatedCategory)
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`HTTP error: ${response.status} - ${errorText}`);
      }

      const savedCategory = await response.json();
      
      // Mise à jour locale pour l'UI
      const updatedCategories = categories.map(cat => 
        cat.idCategorie === savedCategory.idCategorie ? savedCategory : cat
      );
      
      setCategories(updatedCategories);
      setEditingCategory(null);
      setShowForm(false);
    } catch (error) {
      console.error("Error updating category:", error);
      setError("Failed to update category: " + error.message);
    }
  };

  // Gérer la modification
  const handleEditCategory = (category) => {
    setEditingCategory(category);
    setShowForm(true);
  };

  // Gérer la suppression
  const handleDeleteCategory = async (categoryId) => {
    try {
      const user = auth.currentUser;
      if (!user) return;

      const idToken = await user.getIdToken(true);
      const response = await fetch(`/api/categories/${categoryId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${idToken}`,
          'X-Vendeur-ID': user.uid
        }
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`HTTP error: ${response.status} - ${errorText}`);
      }

      // Mettre à jour l'état local
      setCategories(categories.filter(cat => cat.idCategorie !== categoryId));
    } catch (error) {
      console.error("Error deleting category:", error);
      setError("Failed to delete category: " + error.message);
    }
  };

  // Afficher le chargement
  if (loading) {
    return (
      <div className="space-y-6">
        <DashboardNavigation />
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-teal-500"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Dashboard Navigation */}
      <DashboardNavigation />

      {/* Header with add button */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">
            Categories Management
          </h1>
          <p className="text-gray-500 mt-1">Manage your product categories</p>
        </div>
        <button
          onClick={() => {
            setEditingCategory(null);
            setShowForm(true);
          }}
          className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-teal-600 hover:bg-teal-700"
        >
          <Plus size={18} className="mr-2" />
          Add Category
        </button>
      </div>
      
      {/* Barre de recherche */}
      <div className="relative">
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
          <Search className="h-5 w-5 text-gray-400" />
        </div>
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Search categories..."
          className="pl-10 block w-full sm:text-sm border-gray-300 rounded-md focus:ring-teal-500 focus:border-teal-500"
        />
      </div>
      
      {/* Afficher les erreurs */}
      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded flex items-start">
          <AlertCircle className="h-5 w-5 mr-2 mt-0.5 flex-shrink-0" />
          <div>{error}</div>
        </div>
      )}
      
      {/* Formulaire de catégorie */}
      {showForm && (
        <CategoryForm
          category={editingCategory}
          onSubmit={editingCategory ? handleUpdateCategory : handleCreateCategory}
          onCancel={() => {
            setShowForm(false);
            setEditingCategory(null);
          }}
        />
      )}

      {/* Categories List */}
      <div className="bg-white shadow-md rounded-lg overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Icon
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Name
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Description
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {getFilteredCategories().length > 0 ? (
                getFilteredCategories().map((category) => (
                  <tr key={category.idCategorie} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <CategoryIcon 
                        category={{ 
                          icon: category.icon || "tag", 
                          customIcon: category.customIcon 
                        }} 
                      />
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">
                        {category.nom}
                      </div>
                    </td>
                    <td className="px-6 py-4 max-w-xs">
                      <div className="text-sm text-gray-500 truncate">
                        {category.description || "No description"}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <button
                        onClick={() => handleEditCategory(category)}
                        className="text-teal-600 hover:text-teal-900 mr-4"
                      >
                        <Edit size={18} />
                      </button>
                      <button
                        onClick={() => handleDeleteCategory(category.idCategorie)}
                        className="text-red-600 hover:text-red-900"
                      >
                        <Trash2 size={18} />
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td
                    colSpan="4"
                    className="px-6 py-10 text-center text-gray-500"
                  >
                    <div className="flex flex-col items-center">
                      <Tag size={40} className="text-gray-300 mb-2" />
                      <p className="text-lg font-medium text-gray-500 mb-1">
                        No categories found
                      </p>
                      <p className="text-sm text-gray-400">
                        Add a new category to get started
                      </p>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default CategoriesManagement;