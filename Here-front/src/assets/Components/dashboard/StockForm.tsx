import React, { useState, useEffect } from "react";
import axios from "axios";
import {
  X,
  AlertTriangle,
  Store,
  Package,
  Save,
  Building,
  Info,
  Calendar
} from "lucide-react";

// Types for better type safety
interface Boutique {
  id: number;
  nom: string;
}

interface Stock {
  id?: number;
  quantiteStockDisponible: number;
  capaciteMaximaleStock: number;
  idBoutique: number;
  name?: string;
  location?: string;
}

interface StockFormProps {
  stock?: Stock | null;
  boutiques: Boutique[];
  onSubmit: (stockData: Stock) => void;
  onCancel: () => void;
}

const StockForm: React.FC<StockFormProps> = ({ 
  stock, 
  boutiques, 
  onSubmit, 
  onCancel 
}) => {
  // Form state with strict typing
  const [formData, setFormData] = useState<{
    quantiteStockDisponible: number;
    capaciteMaximaleStock: number;
    idBoutique: string;
    name: string;
  }>({
    quantiteStockDisponible: 0,
    capaciteMaximaleStock: 1000,
    idBoutique: "",
    name: ""
  });

  const [errors, setErrors] = useState<{
    quantiteStockDisponible?: string;
    capaciteMaximaleStock?: string;
    idBoutique?: string;
  }>({});

  // Initialize form with stock data if editing
  useEffect(() => {
    if (stock) {
      setFormData({
        quantiteStockDisponible: stock.quantiteStockDisponible || 0,
        capaciteMaximaleStock: stock.capaciteMaximaleStock || 1000,
        idBoutique: stock.idBoutique ? stock.idBoutique.toString() : "",
        name: stock.name || ""
      });
    }
  }, [stock]);

  // Handle form field changes
  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;

    if (name === "quantiteStockDisponible" || name === "capaciteMaximaleStock") {
      const numValue = parseInt(value, 10);
      setFormData(prev => ({
        ...prev,
        [name]: isNaN(numValue) 
          ? (name === "capaciteMaximaleStock" ? 1000 : 0)
          : numValue
      }));
    } else if (name === "idBoutique") {
      // When store is selected, update boutique ID and generate a default name
      const selectedBoutique = boutiques.find(b => b.id.toString() === value);
      setFormData(prev => ({
        ...prev,
        idBoutique: value,
        name: selectedBoutique 
          ? `Stock ${selectedBoutique.nom}` 
          : `Stock ${Date.now().toString().slice(-4)}`
      }));
    } else {
      setFormData(prev => ({ ...prev, [name]: value }));
    }

    // Clear any existing errors for this field
    if (errors[name as keyof typeof errors]) {
      setErrors(prev => ({ ...prev, [name]: undefined }));
    }
  };

  // Validate form
  const validateForm = (): boolean => {
    const newErrors: typeof errors = {};

    // Validate boutique selection
    if (!formData.idBoutique) {
      newErrors.idBoutique = "La boutique est obligatoire";
    }

    // Validate stock quantity (must be non-negative)
    if (formData.quantiteStockDisponible < 0) {
      newErrors.quantiteStockDisponible = "La quantité ne peut pas être négative";
    }

    // Validate maximum stock capacity (must be positive)
    if (formData.capaciteMaximaleStock < 1) {
      newErrors.capaciteMaximaleStock = "La capacité maximale doit être supérieure à 0";
    }

    // Ensure available quantity doesn't exceed maximum capacity
    if (formData.quantiteStockDisponible > formData.capaciteMaximaleStock) {
      newErrors.quantiteStockDisponible = "La quantité disponible ne peut pas dépasser la capacité maximale";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Handle form submission
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    // Perform validation
    if (validateForm()) {
      // Prepare data for backend submission
      const stockData: Stock = {
        quantiteStockDisponible: formData.quantiteStockDisponible,
        capaciteMaximaleStock: formData.capaciteMaximaleStock,
        idBoutique: parseInt(formData.idBoutique, 10),
        name: formData.name || `Stock ${Date.now().toString().slice(-4)}`,
        location: "" // Optional field, can be expanded later
      };

      // If editing an existing stock, include the ID
      if (stock && stock.id) {
        stockData.id = stock.id;
      }

      console.log("Données finales à soumettre:", stockData);
      onSubmit(stockData);
    }
  };

  return (
    <div className="bg-white shadow-lg rounded-lg overflow-hidden border border-gray-200">
      <div className="px-6 py-4 bg-gradient-to-r from-blue-500 to-blue-600 flex justify-between items-center">
        <h2 className="text-xl font-semibold text-white flex items-center">
          <Store size={20} className="mr-2" />
          {stock ? "Modifier le Stock" : "Ajouter un Nouveau Stock"}
        </h2>
        <button
          onClick={onCancel}
          className="text-white hover:text-gray-100 transition-colors duration-150"
          aria-label="Fermer"
        >
          <X size={24} />
        </button>
      </div>

      <form onSubmit={handleSubmit} className="p-6">
        <div className="space-y-6">
          <div>
            <h3 className="text-lg font-medium text-gray-900 mb-4 flex items-center">
              <Info size={18} className="mr-2 text-blue-500" />
              Informations de stock
            </h3>
            
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
              {/* Boutique Selection */}
              <div>
                <label htmlFor="idBoutique" className="block text-sm font-medium text-gray-700 mb-1">
                  Boutique <span className="text-red-500">*</span>
                </label>
                <div className="relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Building className="h-5 w-5 text-gray-400" aria-hidden="true" />
                  </div>
                  <select
                    id="idBoutique"
                    name="idBoutique"
                    className={`block w-full pl-10 py-3 ${
                      errors.idBoutique
                        ? "border-red-300 text-red-900 focus:outline-none focus:ring-red-500 focus:border-red-500"
                        : "border-gray-300 focus:ring-blue-500 focus:border-blue-500"
                    } rounded-lg`}
                    value={formData.idBoutique}
                    onChange={handleChange}
                  >
                    <option value="">Sélectionner une boutique</option>
                    {boutiques && boutiques.length > 0 ? (
                      boutiques.map((boutique) => (
                        <option 
                          key={boutique.id} 
                          value={boutique.id.toString()}
                        >
                          {boutique.nom}
                        </option>
                      ))
                    ) : (
                      <option disabled>Aucune boutique disponible</option>
                    )}
                  </select>
                  {errors.idBoutique && (
                    <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                      <AlertTriangle className="h-5 w-5 text-red-500" aria-hidden="true" />
                    </div>
                  )}
                </div>
                {errors.idBoutique ? (
                  <p className="mt-2 text-sm text-red-600">{errors.idBoutique}</p>
                ) : (
                  <p className="mt-1 text-xs text-gray-500">
                    Boutique à laquelle ce stock est associé
                  </p>
                )}
              </div>

              {/* Maximum Stock Capacity */}
              <div>
                <label htmlFor="capaciteMaximaleStock" className="block text-sm font-medium text-gray-700 mb-1">
                  Capacité maximale de stock <span className="text-red-500">*</span>
                </label>
                <div className="relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Package className="h-5 w-5 text-gray-400" aria-hidden="true" />
                  </div>
                  <input
                    type="number"
                    name="capaciteMaximaleStock"
                    id="capaciteMaximaleStock"
                    className={`block w-full pl-10 py-3 ${
                      errors.capaciteMaximaleStock
                        ? "border-red-300 text-red-900 focus:outline-none focus:ring-red-500 focus:border-red-500"
                        : "border-gray-300 focus:ring-blue-500 focus:border-blue-500"
                    } rounded-lg`}
                    placeholder="Ex: 1000"
                    min="1"
                    value={formData.capaciteMaximaleStock}
                    onChange={handleChange}
                  />
                  {errors.capaciteMaximaleStock && (
                    <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                      <AlertTriangle className="h-5 w-5 text-red-500" aria-hidden="true" />
                    </div>
                  )}
                </div>
                {errors.capaciteMaximaleStock ? (
                  <p className="mt-2 text-sm text-red-600">{errors.capaciteMaximaleStock}</p>
                ) : (
                  <p className="mt-1 text-xs text-gray-500">
                    Capacité maximale que peut contenir ce stock
                  </p>
                )}
              </div>

              {/* Available Stock Quantity */}
              <div>
                <label htmlFor="quantiteStockDisponible" className="block text-sm font-medium text-gray-700 mb-1">
                  Quantité de stock disponible
                </label>
                <div className="relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Calendar className="h-5 w-5 text-gray-400" aria-hidden="true" />
                  </div>
                  <input
                    type="number"
                    name="quantiteStockDisponible"
                    id="quantiteStockDisponible"
                    className={`block w-full pl-10 py-3 ${
                      errors.quantiteStockDisponible
                        ? "border-red-300 text-red-900 focus:outline-none focus:ring-red-500 focus:border-red-500"
                        : "border-gray-300 focus:ring-blue-500 focus:border-blue-500"
                    } rounded-lg`}
                    placeholder="Ex: 500"
                    min="0"
                    value={formData.quantiteStockDisponible}
                    onChange={handleChange}
                  />
                  {errors.quantiteStockDisponible && (
                    <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                      <AlertTriangle className="h-5 w-5 text-red-500" aria-hidden="true" />
                    </div>
                  )}
                </div>
                {errors.quantiteStockDisponible ? (
                  <p className="mt-2 text-sm text-red-600">{errors.quantiteStockDisponible}</p>
                ) : (
                  <p className="mt-1 text-xs text-gray-500">
                    Quantité actuellement disponible en stock
                  </p>
                )}
              </div>
            </div>
          </div>

          {/* Required Fields Note */}
          <div className="flex items-center text-sm text-gray-500 pt-4 border-t border-gray-100">
            <AlertTriangle size={16} className="mr-2 text-amber-500" />
            Les champs marqués avec <span className="text-red-500 mx-1">*</span> sont obligatoires
          </div>

          {/* Form Actions */}
          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onCancel}
              className="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors duration-150"
            >
              Annuler
            </button>
            <button
              type="submit"
              className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors duration-150 flex items-center"
            >
              <Save size={18} className="mr-2" />
              {stock ? "Mettre à Jour" : "Enregistrer"}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};

export default StockForm;