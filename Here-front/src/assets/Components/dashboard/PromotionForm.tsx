import React, { useState, useEffect } from "react";
import {
  X,
  AlertTriangle,
  Percent,
  Calendar,
  Tag,
  CheckCircle,
  ToggleLeft,
  ToggleRight,
  Save
} from "lucide-react";

const PromotionForm = ({ promotion, onSubmit, onCancel }) => {
  // Form state with default values or promotion values if editing
  const [formData, setFormData] = useState({
    nom: "",
    pourcentage_reduction: "",
    actif: true,
    periode: {
      date_debut: "",
      date_fin: "",
    }
  });

  const [errors, setErrors] = useState({});
  const [formattedPeriod, setFormattedPeriod] = useState("");

  // Initialize form with promotion data if editing
  useEffect(() => {
    if (promotion) {
      // Format dates for input fields (YYYY-MM-DD)
      const formatDateForInput = (dateStr) => {
        if (!dateStr) return "";
        const date = new Date(dateStr);
        return date.toISOString().split('T')[0];
      };

      setFormData({
        nom: promotion.nom || "",
        pourcentage_reduction: promotion.pourcentage_reduction
          ? promotion.pourcentage_reduction.toString()
          : "",
        actif: promotion.actif !== undefined ? promotion.actif : true,
        periode: {
          date_debut: formatDateForInput(promotion.periode_debut),
          date_fin: formatDateForInput(promotion.periode_fin),
        }
      });

      // Set formatted period for display
      if (promotion.periode_debut && promotion.periode_fin) {
        const startFormatted = new Date(promotion.periode_debut).toLocaleDateString("fr-FR");
        const endFormatted = new Date(promotion.periode_fin).toLocaleDateString("fr-FR");
        setFormattedPeriod(`Du ${startFormatted} au ${endFormatted}`);
      }
    }
  }, [promotion]);

  // Convert date from YYYY-MM-DD to DD/MM/YYYY format for display
  const formatDateForDisplay = (dateStr) => {
    if (!dateStr) return "";
    const date = new Date(dateStr);
    return date.toLocaleDateString("fr-FR", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
  };

  // Handle form field changes
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;

    if (name.startsWith("periode.")) {
      // Handle nested periode object
      const periodField = name.split(".")[1];
      setFormData({
        ...formData,
        periode: {
          ...formData.periode,
          [periodField]: value,
        },
      });

      // Update formatted period string whenever dates change
      const newPeriod = {
        ...formData.periode,
        [periodField]: value,
      };

      if (newPeriod.date_debut && newPeriod.date_fin) {
        const startFormatted = formatDateForDisplay(newPeriod.date_debut);
        const endFormatted = formatDateForDisplay(newPeriod.date_fin);
        setFormattedPeriod(`Du ${startFormatted} au ${endFormatted}`);
      }
    } else {
      // Handle regular form fields
      setFormData({
        ...formData,
        [name]: type === "checkbox" ? checked : value,
      });
    }

    // Clear error for the field when user modifies it
    if (errors[name]) {
      setErrors({ ...errors, [name]: null });
    }
  };

  // Toggle active status
  const toggleActive = () => {
    setFormData({
      ...formData,
      actif: !formData.actif,
    });
  };

  // Validate form
  const validateForm = () => {
    const newErrors = {};

    if (!formData.nom.trim())
      newErrors.nom = "Le nom de la promotion est requis";

    if (!formData.pourcentage_reduction) {
      newErrors.pourcentage_reduction = "Le pourcentage de réduction est requis";
    } else if (
      isNaN(parseFloat(formData.pourcentage_reduction)) ||
      parseFloat(formData.pourcentage_reduction) <= 0 ||
      parseFloat(formData.pourcentage_reduction) > 100
    ) {
      newErrors.pourcentage_reduction =
        "Le pourcentage doit être un nombre entre 0 et 100";
    }

    if (!formData.periode.date_debut) {
      newErrors["periode.date_debut"] = "La date de début est requise";
    }

    if (!formData.periode.date_fin) {
      newErrors["periode.date_fin"] = "La date de fin est requise";
    }

    if (formData.periode.date_debut && formData.periode.date_fin) {
      const start = new Date(formData.periode.date_debut);
      const end = new Date(formData.periode.date_fin);

      if (start > end) {
        newErrors["periode.date_fin"] =
          "La date de fin doit être postérieure à la date de début";
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Handle form submission
  const handleSubmit = (e) => {
    e.preventDefault();

    if (validateForm()) {
      // Format the data to match the backend model
      const processedData = {
        nom: formData.nom,
        pourcentage_reduction: parseFloat(formData.pourcentage_reduction),
        actif: formData.actif,
        periode_debut: formData.periode.date_debut,
        periode_fin: formData.periode.date_fin
      };

      onSubmit(processedData);
    }
  };

  return (
    <div className="bg-white shadow-lg rounded-lg overflow-hidden border border-gray-200">
      <div className="px-6 py-4 bg-gradient-to-r from-teal-500 to-teal-600 flex justify-between items-center">
        <h2 className="text-xl font-semibold text-white">
          {promotion
            ? "Modifier la Promotion"
            : "Ajouter une Nouvelle Promotion"}
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
          {/* Promotion Name */}
          <div>
            <label
              htmlFor="nom"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Nom de la Promotion *
            </label>
            <div className="relative rounded-md shadow-sm">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Tag className="h-5 w-5 text-gray-400" aria-hidden="true" />
              </div>
              <input
                type="text"
                name="nom"
                id="nom"
                className={`block w-full pl-10 py-3 ${
                  errors.nom
                    ? "border-red-300 text-red-900 placeholder-red-300 focus:outline-none focus:ring-red-500 focus:border-red-500"
                    : "border-gray-300 focus:ring-teal-500 focus:border-teal-500"
                } rounded-lg`}
                placeholder="Ex: Offre d'été, Soldes de fin d'année, etc."
                value={formData.nom}
                onChange={handleChange}
                maxLength={50}
              />
              {errors.nom && (
                <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                  <AlertTriangle
                    className="h-5 w-5 text-red-500"
                    aria-hidden="true"
                  />
                </div>
              )}
            </div>
            {errors.nom ? (
              <p className="mt-2 text-sm text-red-600">{errors.nom}</p>
            ) : (
              <p className="mt-1 text-xs text-gray-500">
                Nom qui identifie cette promotion (50 caractères max)
              </p>
            )}
          </div>

          {/* Percentage */}
          <div>
            <label
              htmlFor="pourcentage_reduction"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Pourcentage de Réduction *
            </label>
            <div className="relative rounded-md shadow-sm">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Percent className="h-5 w-5 text-gray-400" aria-hidden="true" />
              </div>
              <input
                type="number"
                name="pourcentage_reduction"
                id="pourcentage_reduction"
                className={`block w-full pl-10 py-3 ${
                  errors.pourcentage_reduction
                    ? "border-red-300 text-red-900 placeholder-red-300 focus:outline-none focus:ring-red-500 focus:border-red-500"
                    : "border-gray-300 focus:ring-teal-500 focus:border-teal-500"
                } rounded-lg`}
                placeholder="Ex: 20"
                min="0.01"
                max="100"
                step="0.01"
                value={formData.pourcentage_reduction}
                onChange={handleChange}
              />
             {errors.pourcentage_reduction && (
                <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                  <AlertTriangle
                    className="h-5 w-5 text-red-500"
                    aria-hidden="true"
                  />
                </div>
              )}
            </div>
            {errors.pourcentage_reduction ? (
              <p className="mt-2 text-sm text-red-600">
                {errors.pourcentage_reduction}
              </p>
            ) : (
              <p className="mt-1 text-xs text-gray-500">
                Valeur entre 0.01% et 100%
              </p>
            )}
          </div>

          {/* Period */}
          <div>
            <label
              htmlFor="periode"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Période de validité *
            </label>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label
                  htmlFor="periode.date_debut"
                  className="block text-xs text-gray-500 mb-1"
                >
                  Date de début
                </label>
                <div className="relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Calendar
                      className="h-5 w-5 text-gray-400"
                      aria-hidden="true"
                    />
                  </div>
                  <input
                    type="date"
                    name="periode.date_debut"
                    id="periode.date_debut"
                    className={`block w-full pl-10 py-3 ${
                      errors["periode.date_debut"]
                        ? "border-red-300 text-red-900 focus:outline-none focus:ring-red-500 focus:border-red-500"
                        : "border-gray-300 focus:ring-teal-500 focus:border-teal-500"
                    } rounded-lg`}
                    value={formData.periode.date_debut}
                    onChange={handleChange}
                  />
                  {errors["periode.date_debut"] && (
                    <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                      <AlertTriangle
                        className="h-5 w-5 text-red-500"
                        aria-hidden="true"
                      />
                    </div>
                  )}
                </div>
                {errors["periode.date_debut"] && (
                  <p className="mt-1 text-sm text-red-600">
                    {errors["periode.date_debut"]}
                  </p>
                )}
              </div>

              <div>
                <label
                  htmlFor="periode.date_fin"
                  className="block text-xs text-gray-500 mb-1"
                >
                  Date de fin
                </label>
                <div className="relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Calendar
                      className="h-5 w-5 text-gray-400"
                      aria-hidden="true"
                    />
                  </div>
                  <input
                    type="date"
                    name="periode.date_fin"
                    id="periode.date_fin"
                    className={`block w-full pl-10 py-3 ${
                      errors["periode.date_fin"]
                        ? "border-red-300 text-red-900 focus:outline-none focus:ring-red-500 focus:border-red-500"
                        : "border-gray-300 focus:ring-teal-500 focus:border-teal-500"
                    } rounded-lg`}
                    value={formData.periode.date_fin}
                    onChange={handleChange}
                  />
                  {errors["periode.date_fin"] && (
                    <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                      <AlertTriangle
                        className="h-5 w-5 text-red-500"
                        aria-hidden="true"
                      />
                    </div>
                  )}
                </div>
                {errors["periode.date_fin"] && (
                  <p className="mt-1 text-sm text-red-600">
                    {errors["periode.date_fin"]}
                  </p>
                )}
              </div>
            </div>
            {formattedPeriod &&
              !errors["periode.date_debut"] &&
              !errors["periode.date_fin"] && (
                <div className="mt-2 px-3 py-2 bg-teal-50 border border-teal-100 rounded-md text-sm text-teal-700 flex items-center">
                  <CheckCircle size={16} className="mr-2 text-teal-500" />
                  {formattedPeriod}
                </div>
              )}
          </div>

          {/* Active Status */}
          <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-sm font-medium text-gray-700">
                  Statut de la promotion
                </h3>
                <p className="text-xs text-gray-500 mt-1">
                  {formData.actif
                    ? "La promotion est actuellement active et sera appliquée pendant la période définie"
                    : "La promotion est actuellement inactive et ne sera pas appliquée même pendant la période définie"}
                </p>
              </div>
              <button
                type="button"
                onClick={toggleActive}
                className={`relative inline-flex flex-shrink-0 h-6 w-11 border-2 border-transparent rounded-full cursor-pointer transition-colors ease-in-out duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 ${
                  formData.actif
                    ? "bg-teal-500 focus:ring-teal-500"
                    : "bg-gray-300 focus:ring-gray-400"
                }`}
                aria-pressed={formData.actif}
                aria-labelledby="active-status"
              >
                <span className="sr-only">Toggle active status</span>
                <span
                  aria-hidden="true"
                  className={`pointer-events-none inline-block h-5 w-5 rounded-full bg-white shadow transform ring-0 transition ease-in-out duration-200 ${
                    formData.actif ? "translate-x-5" : "translate-x-0"
                  }`}
                ></span>
              </button>
            </div>
            <div className="mt-3 flex items-center text-sm">
              {formData.actif ? (
                <ToggleRight size={18} className="text-teal-500 mr-2" />
              ) : (
                <ToggleLeft size={18} className="text-gray-500 mr-2" />
              )}
              <span>{formData.actif ? "Active" : "Inactive"}</span>
            </div>
          </div>

          {/* Required Fields Note */}
          <div className="flex items-center text-sm text-gray-500 mt-2 pt-4 border-t border-gray-100">
            <AlertTriangle size={16} className="mr-2 text-amber-500" />
            Les champs marqués avec * sont obligatoires
          </div>

          {/* Form Actions */}
          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onCancel}
              className="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500 transition-colors duration-150"
            >
              Annuler
            </button>
            <button
              type="submit"
              className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-teal-600 hover:bg-teal-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500 transition-colors duration-150 flex items-center"
            >
              <Save size={18} className="mr-2" />
              {promotion ? "Mettre à Jour" : "Enregistrer"}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};

export default PromotionForm;