import React, { useState, useEffect, useRef } from "react";
import axios from "axios";
import {
  X,
  AlertTriangle,
  Package,
  Tag,
  DollarSign,
  Save,
  Store,
  Info,
  Clock,
  Calendar,
  Bookmark,
  FileText,
  Upload,
  ImageIcon,
  Edit2,
  Trash2,
  Star,
  Plus
} from "lucide-react";

const ProductForm = ({ 
  product, 
  stocks, 
  categories, 
  reductions, 
  onSubmit, 
  onCancel, 
  hideStockSelector = false, 
  initialStockId = null 
}) => {
  // Form state with default values or product values if editing
  const [formData, setFormData] = useState({
    nomProduit: "",
    Quantité: "",
    description: "",
    detail: "",
    seuil_Critique: "",
    Prix: "",
    date_expiration: "",
    id_stock: initialStockId || "",
    id_categorie: "",
    id_reduction: ""
  });

  // État pour gérer les images
  const [productImages, setProductImages] = useState([
    {
      file: null,
      preview: "",
      image_principale: true,
    },
  ]);

  const [errors, setErrors] = useState({});
  const fileInputRefs = useRef([]);
  const [loading, setLoading] = useState(false);
  
  // Fonction pour déboguer les données avant soumission
  const logFormData = (data) => {
    console.log("====== DEBUG FORM DATA ======");
    console.log("nomProduit:", data.nomProduit);
    console.log("Quantité:", data.Quantité, typeof data.Quantité);
    console.log("description:", data.description);
    console.log("detail:", data.detail);
    console.log("seuil_Critique:", data.seuil_Critique, typeof data.seuil_Critique);
    console.log("Prix:", data.Prix, typeof data.Prix);
    console.log("date_expiration:", data.date_expiration);
    console.log("id_stock:", data.id_stock, typeof data.id_stock);
    console.log("id_categorie:", data.id_categorie, typeof data.id_categorie);
    console.log("id_reduction:", data.id_reduction, typeof data.id_reduction);
    console.log("images:", data.images);
    console.log("====== END DEBUG ======");
  };

  // Debugging: Loguer les données reçues
  useEffect(() => {
    console.log("Stocks reçus:", stocks);
    console.log("Catégories reçues:", categories);
    console.log("Réductions reçues:", reductions);
    
    // Debug catégories
    if (categories && Array.isArray(categories)) {
      console.log("Détail des catégories:");
      categories.forEach(cat => {
        console.log(`ID: ${cat.idCategorie} (${typeof cat.idCategorie}), Nom: ${cat.nom}`);
      });
    }
  }, [stocks, categories, reductions]);

  // Gérer les changements des champs de formulaire
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({
      ...formData,
      [name]: value
    });
    
    // Réinitialiser l'erreur pour ce champ
    if (errors[name]) {
      setErrors({
        ...errors,
        [name]: null
      });
    }
  };

  // Ajouter un nouveau champ d'image
  const addImageField = () => {
    setProductImages([
      ...productImages,
      {
        file: null,
        preview: "",
        image_principale: false
      }
    ]);
  };

  // Supprimer un champ d'image
  const removeImageField = (index) => {
    const updatedImages = [...productImages];
    
    // Si on supprime l'image principale, définir la première comme principale
    if (updatedImages[index].image_principale && updatedImages.length > 1) {
      const newPrimaryIndex = index === 0 ? 1 : 0;
      updatedImages[newPrimaryIndex].image_principale = true;
    }
    
    // Libérer l'URL de prévisualisation si nécessaire
    if (updatedImages[index].preview && updatedImages[index].preview.startsWith("blob:")) {
      URL.revokeObjectURL(updatedImages[index].preview);
    }
    
    updatedImages.splice(index, 1);
    setProductImages(updatedImages);
  };

  // Déclencher le sélecteur de fichier
  const triggerFileInput = (index) => {
    if (fileInputRefs.current[index]) {
      fileInputRefs.current[index].click();
    }
  };

  // Gérer le téléchargement de fichier
  const handleFileUpload = (index, e) => {
    const file = e.target.files[0];
    if (!file) return;
    
    // Vérifier que c'est bien une image
    if (!file.type.startsWith('image/')) {
      setErrors({
        ...errors,
        [`image-${index}`]: "Le fichier doit être une image valide"
      });
      return;
    }
    
    // Créer l'URL de prévisualisation
    const previewUrl = URL.createObjectURL(file);
    
    // Mettre à jour l'image
    const updatedImages = [...productImages];
    
    // Libérer l'ancienne URL si nécessaire
    if (updatedImages[index].preview && updatedImages[index].preview.startsWith("blob:")) {
      URL.revokeObjectURL(updatedImages[index].preview);
    }
    
    updatedImages[index] = {
      ...updatedImages[index],
      file,
      preview: previewUrl
    };
    
    setProductImages(updatedImages);
    
    // Réinitialiser l'erreur pour cette image
    if (errors[`image-${index}`]) {
      setErrors({
        ...errors,
        [`image-${index}`]: null
      });
    }
  };

  // Gérer les changements dans les propriétés d'image
  const handleImageChange = (index, property, value) => {
    const updatedImages = [...productImages];
    
    // Si on définit cette image comme principale, désactiver toutes les autres
    if (property === "image_principale" && value === true) {
      updatedImages.forEach((img, i) => {
        if (i !== index) {
          updatedImages[i].image_principale = false;
        }
      });
    }
    
    updatedImages[index] = {
      ...updatedImages[index],
      [property]: value
    };
    
    setProductImages(updatedImages);
  };

  // Valider le formulaire
  const validateForm = () => {
    const newErrors = {};
    
    // Valider le nom du produit
    if (!formData.nomProduit.trim()) {
      newErrors.nomProduit = "Le nom du produit est requis";
    }
    
    // Valider le stock si le sélecteur n'est pas masqué
    if (!hideStockSelector && !formData.id_stock) {
      newErrors.id_stock = "Le stock est requis";
    }
    
    // Valider la catégorie
    if (!formData.id_categorie) {
      newErrors.id_categorie = "La catégorie est requise";
    }
    
    // Valider le prix
    if (!formData.Prix.trim()) {
      newErrors.Prix = "Le prix est requis";
    } else if (isNaN(parseFloat(formData.Prix)) || parseFloat(formData.Prix) <= 0) {
      newErrors.Prix = "Le prix doit être un nombre positif";
    }
    
    // Valider la quantité
    if (!formData.Quantité.trim()) {
      newErrors.Quantité = "La quantité est requise";
    } else if (isNaN(parseInt(formData.Quantité)) || parseInt(formData.Quantité) < 0) {
      newErrors.Quantité = "La quantité doit être un nombre entier positif ou zéro";
    }
    
    // Valider le seuil critique
    if (!formData.seuil_Critique.trim()) {
      newErrors.seuil_Critique = "Le seuil critique est requis";
    } else if (isNaN(parseFloat(formData.seuil_Critique)) || parseFloat(formData.seuil_Critique) < 0) {
      newErrors.seuil_Critique = "Le seuil critique doit être un nombre positif ou zéro";
    }
    
    // Valider les images
    if (!productImages.some(img => img.file || img.preview)) {
      newErrors.images = "Au moins une image est requise";
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Si les catégories ou réductions ne sont pas fournies, les charger
  useEffect(() => {
    const fetchData = async () => {
      if (!categories || categories.length === 0) {
        try {
          const response = await axios.get('/api/categories');
          console.log("Catégories chargées via API:", response.data);
          categories = response.data;
        } catch (error) {
          console.error("Error loading categories:", error);
        }
      }
      
      if (!reductions || reductions.length === 0) {
        try {
          const response = await axios.get('/api/reductions');
          reductions = response.data;
        } catch (error) {
          console.error("Error loading reductions:", error);
        }
      }
      
      if (!stocks || stocks.length === 0) {
        try {
          const response = await axios.get('/api/stocks');
          stocks = response.data;
        } catch (error) {
          console.error("Error loading stocks:", error);
        }
      }
    };
    
    fetchData();
  }, []);

  // Initialize form with product data if editing
  useEffect(() => {
    if (product) {
      setFormData({
        nomProduit: product.nomProduit || "",
        Quantité: product.quantite || "",
        description: product.description || "",
        detail: product.detail || "",
        seuil_Critique: product.seuilCritique ? product.seuilCritique.toString() : "",
        Prix: product.prix ? product.prix.toString() : "",
        date_expiration: product.dateExpiration ? formatDateForInput(product.dateExpiration) : "",
        id_stock: product.idStock || initialStockId || "",
        id_categorie: product.idCategorie || "",
        id_reduction: product.idReduction || ""
      });

      // Initialiser les images si elles existent
      if (product.images && product.images.length > 0) {
        setProductImages(
          product.images.map((img) => ({
            file: null,
            preview: img.url,
            image_principale: img.imagePrincipale
          }))
        );
      }
    }
  }, [product, initialStockId]);

  // Format date for input fields (YYYY-MM-DD)
  const formatDateForInput = (dateStr) => {
    if (!dateStr) return "";
    const date = new Date(dateStr);
    return date.toISOString().split('T')[0];
  };

  // Cleanup URL objects when component unmounts
  useEffect(() => {
    return () => {
      productImages.forEach((image) => {
        if (image.preview && image.preview.startsWith("blob:")) {
          URL.revokeObjectURL(image.preview);
        }
      });
    };
  }, [productImages]);

  // Handle form submission
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    // Vérifier qu'il y a une image principale
    const hasPrimaryImage = productImages.some((img) => img.image_principale);
    if (!hasPrimaryImage && productImages.length > 0) {
      // Si aucune image n'est définie comme principale, utiliser la première
      const updatedImages = [...productImages];
      updatedImages[0].image_principale = true;
      setProductImages(updatedImages);
    }

    // Format the data to match the expected model
    const processedData = {
      ...formData,
      Prix: parseFloat(formData.Prix),
      seuil_Critique: parseFloat(formData.seuil_Critique),
      Quantité: parseInt(formData.Quantité, 10),
      
      // Essayer de convertir l'ID de catégorie en nombre si possible
      id_categorie: (() => {
        try {
          const id = parseInt(formData.id_categorie, 10);
          return !isNaN(id) ? id : formData.id_categorie;
        } catch (e) {
          return formData.id_categorie;
        }
      })(),
      
      // Ne conserver que les images avec un fichier
      images: productImages
        .filter((img) => img.file)
        .map((img) => ({
          image_principale: img.image_principale,
          // Les objets File seront traités côté serveur
          file: img.file
        })),
    };
    
    // Déboguer les données avant soumission
    logFormData(processedData);

    console.log("Submitting product form with data:", processedData);
    
    try {
      onSubmit(processedData);
    } catch (error) {
      console.error("Error submitting form:", error);
      setErrors({
        ...errors,
        general: "Une erreur est survenue lors de l'envoi du formulaire."
      });
    }
  };

  return (
    <div className="bg-white shadow-lg rounded-lg overflow-hidden border border-gray-200">
      <div className="px-6 py-4 bg-gradient-to-r from-teal-500 to-teal-600 flex justify-between items-center">
        <h2 className="text-xl font-semibold text-white flex items-center">
          <Package size={20} className="mr-2" />
          {product ? "Modifier le Produit" : "Ajouter un Nouveau Produit"}
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
          {/* Basic Information Section */}
          <div>
            <h3 className="text-lg font-medium text-gray-900 mb-4 flex items-center">
              <Info size={18} className="mr-2 text-teal-500" />
              Informations de base
            </h3>
            
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
              {/* Product Name */}
              <div className="col-span-2">
                <label htmlFor="nomProduit" className="block text-sm font-medium text-gray-700 mb-1">
                  Nom du produit <span className="text-red-500">*</span>
                </label>
                <div className="relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Tag className="h-5 w-5 text-gray-400" aria-hidden="true" />
                  </div>
                  <input
                    type="text"
                    name="nomProduit"
                    id="nomProduit"
                    className={`block w-full pl-10 py-3 ${
                      errors.nomProduit
                        ? "border-red-300 text-red-900 placeholder-red-300 focus:outline-none focus:ring-red-500 focus:border-red-500"
                        : "border-gray-300 focus:ring-teal-500 focus:border-teal-500"
                    } rounded-lg`}
                    placeholder="Nom descriptif du produit"
                    value={formData.nomProduit}
                    onChange={handleChange}
                    maxLength={50}
                  />
                  {errors.nomProduit && (
                    <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                      <AlertTriangle className="h-5 w-5 text-red-500" aria-hidden="true" />
                    </div>
                  )}
                </div>
                {errors.nomProduit ? (
                  <p className="mt-2 text-sm text-red-600">{errors.nomProduit}</p>
                ) : (
                  <p className="mt-1 text-xs text-gray-500">
                    Nom qui identifie ce produit (50 caractères max)
                  </p>
                )}
              </div>

              {/* Stock Selection - Only if not hidden */}
              {!hideStockSelector && (
                <div>
                  <label htmlFor="id_stock" className="block text-sm font-medium text-gray-700 mb-1">
                    Stock <span className="text-red-500">*</span>
                  </label>
                  <div className="relative rounded-md shadow-sm">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <Store className="h-5 w-5 text-gray-400" aria-hidden="true" />
                    </div>
                    <select
                      id="id_stock"
                      name="id_stock"
                      className={`block w-full pl-10 py-3 ${
                        errors.id_stock
                          ? "border-red-300 text-red-900 focus:outline-none focus:ring-red-500 focus:border-red-500"
                          : "border-gray-300 focus:ring-teal-500 focus:border-teal-500"
                      } rounded-lg`}
                      value={formData.id_stock}
                      onChange={handleChange}
                    >
                      <option value="">Sélectionner un stock</option>
                      {stocks && Array.isArray(stocks) && stocks.map((stock) => (
                        <option key={`stock-${stock.id}`} value={stock.id}>
                          {stock.name}
                        </option>
                      ))}
                    </select>
                    {errors.id_stock && (
                      <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                        <AlertTriangle className="h-5 w-5 text-red-500" aria-hidden="true" />
                      </div>
                    )}
                  </div>
                  {errors.id_stock ? (
                    <p className="mt-2 text-sm text-red-600">{errors.id_stock}</p>
                  ) : (
                    <p className="mt-1 text-xs text-gray-500">
                      Sélectionnez le stock où ce produit sera disponible
                    </p>
                  )}
                </div>
              )}

              {/* Category Selection */}
              <div>
                <label htmlFor="id_categorie" className="block text-sm font-medium text-gray-700 mb-1">
                  Catégorie <span className="text-red-500">*</span>
                </label>
                <div className="relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Bookmark className="h-5 w-5 text-gray-400" aria-hidden="true" />
                  </div>
                  <select
                    id="id_categorie"
                    name="id_categorie"
                    className={`block w-full pl-10 py-3 ${
                      errors.id_categorie
                        ? "border-red-300 text-red-900 focus:outline-none focus:ring-red-500 focus:border-red-500"
                        : "border-gray-300 focus:ring-teal-500 focus:border-teal-500"
                    } rounded-lg`}
                    value={formData.id_categorie}
                    onChange={handleChange}
                  >
                    <option value="">Sélectionner une catégorie</option>
                    {categories && Array.isArray(categories) && categories.map((category) => {
                      // Essayer de convertir l'ID en nombre si c'est une chaîne
                      const idAsNumber = parseInt(category.idCategorie, 10);
                      const idToUse = !isNaN(idAsNumber) ? idAsNumber : category.idCategorie;
                      
                      return (
                        <option key={`category-${category.idCategorie}`} value={idToUse}>
                          {category.nom}
                        </option>
                      );
                    })}
                  </select>
                  {errors.id_categorie && (
                    <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                      <AlertTriangle className="h-5 w-5 text-red-500" aria-hidden="true" />
                    </div>
                  )}
                </div>
                {errors.id_categorie ? (
                  <p className="mt-2 text-sm text-red-600">{errors.id_categorie}</p>
                ) : (
                  <p className="mt-1 text-xs text-gray-500">
                    Catégorie à laquelle appartient ce produit
                  </p>
                )}
              </div>

              {/* Promotion/Reduction Selection */}
              <div>
                <label htmlFor="id_reduction" className="block text-sm font-medium text-gray-700 mb-1">
                  Promotion
                </label>
                <div className="relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Tag className="h-5 w-5 text-gray-400" aria-hidden="true" />
                  </div>
                  <select
                    id="id_reduction"
                    name="id_reduction"
                    className="block w-full pl-10 py-3 border-gray-300 focus:ring-teal-500 focus:border-teal-500 rounded-lg"
                    value={formData.id_reduction}
                    onChange={handleChange}
                  >
                    <option value="">Aucune promotion</option>
                    {reductions && Array.isArray(reductions) && reductions.map((reduction) => (
                      <option key={`reduction-${reduction.id}`} value={reduction.id}>
                        {reduction.nom}
                      </option>
                    ))}
                  </select>
                </div>
                <p className="mt-1 text-xs text-gray-500">
                  Promotion applicable à ce produit (optionnel)
                </p>
              </div>

              {/* Price */}
              <div>
                <label htmlFor="Prix" className="block text-sm font-medium text-gray-700 mb-1">
                  Prix <span className="text-red-500">*</span>
                </label>
                <div className="relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <DollarSign className="h-5 w-5 text-gray-400" aria-hidden="true" />
                  </div>
                  <input
                    type="text"
                    name="Prix"
                    id="Prix"
                    className={`block w-full pl-10 py-3 ${
                      errors.Prix
                        ? "border-red-300 text-red-900 placeholder-red-300 focus:outline-none focus:ring-red-500 focus:border-red-500"
                        : "border-gray-300 focus:ring-teal-500 focus:border-teal-500"
                    } rounded-lg`}
                    placeholder="Ex: 29.99"
                    value={formData.Prix}
                    onChange={handleChange}
                  />
                  {errors.Prix && (
                    <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                      <AlertTriangle className="h-5 w-5 text-red-500" aria-hidden="true" />
                    </div>
                  )}
                </div>
                {errors.Prix ? (
                  <p className="mt-2 text-sm text-red-600">{errors.Prix}</p>
                ) : (
                  <p className="mt-1 text-xs text-gray-500">Prix de vente en euros</p>
                )}
              </div>

              {/* Expiration Date */}
              <div>
                <label htmlFor="date_expiration" className="block text-sm font-medium text-gray-700 mb-1">
                  Date d'expiration
                </label>
                <div className="relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Calendar className="h-5 w-5 text-gray-400" aria-hidden="true" />
                  </div>
                  <input
                    type="date"
                    name="date_expiration"
                    id="date_expiration"
                    className="block w-full pl-10 py-3 border-gray-300 focus:ring-teal-500 focus:border-teal-500 rounded-lg"
                    value={formData.date_expiration}
                    onChange={handleChange}
                  />
                </div>
                <p className="mt-1 text-xs text-gray-500">
                  Date d'expiration du produit (si applicable)
                </p>
              </div>
            </div>
          </div>

          {/* Images Section */}
          <div>
            <h3 className="text-lg font-medium text-gray-900 mb-4 flex items-center">
              <ImageIcon size={18} className="mr-2 text-teal-500" />
              Images du produit <span className="text-red-500">*</span>
            </h3>
            
            <div className="flex justify-between items-center mb-4">
              <p className="text-sm text-gray-600">
                Ajoutez des images pour présenter votre produit (au moins une image est requise)
              </p>
              <button
                type="button"
                onClick={addImageField}
                className="px-3 py-2 bg-teal-100 text-teal-700 rounded-lg hover:bg-teal-200 flex items-center text-sm font-medium"
              >
                <Plus size={16} className="mr-1" />
                Ajouter une image
              </button>
            </div>

            {errors.images && (
              <div className="mb-4 p-3 bg-red-50 border border-red-300 rounded-lg">
                <p className="text-sm text-red-600">{errors.images}</p>
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {productImages.map((image, index) => (
                <div
                  key={`image-field-${index}`}
                  className="border rounded-lg p-4 relative bg-white shadow-sm"
                >
                  <div className="absolute top-2 right-2 z-10">
                    {productImages.length > 1 && (
                      <button
                        type="button"
                        onClick={() => removeImageField(index)}
                        className="text-red-500 hover:text-red-700 bg-white rounded-full p-1 shadow-sm"
                      >
                        <Trash2 size={16} />
                      </button>
                    )}
                  </div>

                  {/* Image Preview */}
                  <div
                    className={`w-full h-48 mb-3 border-2 border-dashed rounded-lg flex items-center justify-center bg-gray-50 cursor-pointer group relative overflow-hidden ${
                      image.preview
                        ? "border-transparent"
                        : "border-gray-300 hover:border-teal-300"
                    }`}
                    onClick={() => triggerFileInput(index)}
                  >
                    {image.preview ? (
                      <div className="relative w-full h-full">
                        <img
                          src={image.preview}
                          alt={`Preview ${index}`}
                          className="w-full h-full object-contain"
                        />
                        <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-30 flex items-center justify-center transition-all">
                          <Edit2
                            size={24}
                            className="text-white opacity-0 group-hover:opacity-100 transition-opacity"
                          />
                        </div>
                        {image.image_principale && (
                          <div className="absolute top-2 left-2 bg-yellow-400 text-yellow-800 p-1 rounded-md flex items-center text-xs font-medium">
                            <Star size={14} className="mr-1" />
                            Principale
                          </div>
                        )}
                      </div>
                    ) : (
                      <div className="text-center p-6">
                        <Upload className="mx-auto h-12 w-12 text-gray-400" />
                        <p className="mt-1 text-sm text-gray-600">
                          Cliquez pour importer une image
                        </p>
                        <p className="text-xs text-gray-500">
                          JPG, PNG, GIF, SVG, WEBP
                        </p>
                      </div>
                    )}
                    <input
                      type="file"
                      accept="image/*"
                      ref={(el) => (fileInputRefs.current[index] = el)}
                      onChange={(e) => handleFileUpload(index, e)}
                      className="hidden"
                    />
                  </div>

                  <div className="grid grid-cols-1 gap-y-3">
                    {/* Is Primary Image */}
                    <div>
                      <div className="flex items-start">
                        <div className="flex items-center h-5">
                          <input
                            id={`image-primary-${index}`}
                            name={`image-primary-${index}`}
                            type="checkbox"
                            checked={image.image_principale}
                            onChange={(e) =>
                              handleImageChange(
                                index,
                                "image_principale",
                                e.target.checked
                              )
                            }
                            className="focus:ring-teal-500 h-4 w-4 text-teal-600 border-gray-300 rounded"
                          />
                        </div>
                        <div className="ml-3 text-sm">
                          <label
                            htmlFor={`image-primary-${index}`}
                            className="font-medium text-gray-700"
                          >
                            Image principale
                          </label>
                          <p className="text-gray-500">
                            Utilisée comme miniature principale
                          </p>
                        </div>
                      </div>
                    </div>
                  </div>

                  {errors[`image-${index}`] && (
                    <p className="mt-2 text-sm text-red-600">
                      {errors[`image-${index}`]}
                    </p>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Inventory Section */}
          <div>
            <h3 className="text-lg font-medium text-gray-900 mb-4 flex items-center">
              <Package size={18} className="mr-2 text-teal-500" />
              Gestion de stock
            </h3>
            
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
              {/* Quantity */}
              <div>
                <label htmlFor="Quantité" className="block text-sm font-medium text-gray-700 mb-1">
                  Quantité <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="Quantité"
                  id="Quantité"
                  className={`block w-full py-3 px-4 ${
                    errors.Quantité
                      ? "border-red-300 text-red-900 placeholder-red-300 focus:outline-none focus:ring-red-500 focus:border-red-500"
                      : "border-gray-300 focus:ring-teal-500 focus:border-teal-500"
                  } rounded-lg`}
                  placeholder="Ex: 50"
                  value={formData.Quantité}
                  onChange={handleChange}
                  maxLength={50}
                />
                {errors.Quantité ? (
                  <p className="mt-2 text-sm text-red-600">{errors.Quantité}</p>
                ) : (
                  <p className="mt-1 text-xs text-gray-500">
                    Nombre d'unités actuellement en stock
                  </p>
                )}
              </div>

              {/* Critical Threshold */}
              <div>
                <label htmlFor="seuil_Critique" className="block text-sm font-medium text-gray-700 mb-1">
                  Seuil critique <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="seuil_Critique"
                  id="seuil_Critique"
                  className={`block w-full py-3 px-4 ${
                    errors.seuil_Critique
                      ? "border-red-300 text-red-900 placeholder-red-300 focus:outline-none focus:ring-red-500 focus:border-red-500"
                      : "border-gray-300 focus:ring-teal-500 focus:border-teal-500"
                  } rounded-lg`}
                  placeholder="Ex: 10.00"
                  value={formData.seuil_Critique}
                  onChange={handleChange}
                />
                {errors.seuil_Critique ? (
                  <p className="mt-2 text-sm text-red-600">{errors.seuil_Critique}</p>
                ) : (
                  <p className="mt-1 text-xs text-gray-500">
                    Quantité minimum avant alerte de stock bas
                  </p>
                )}
              </div>
            </div>
          </div>

          {/* Description Section */}
          <div>
            <h3 className="text-lg font-medium text-gray-900 mb-4 flex items-center">
              <FileText size={18} className="mr-2 text-teal-500" />
              Description du produit
            </h3>
            
            <div className="grid grid-cols-1 gap-6">
              {/* Short Description */}
              <div>
                <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
                  Description courte
                </label>
                <textarea
                  id="description"
                  name="description"
                  rows={2}
                  className="block w-full border border-gray-300 rounded-lg shadow-sm py-3 px-4 focus:ring-teal-500 focus:border-teal-500"
                  placeholder="Description courte du produit..."
                  value={formData.description}
                  onChange={handleChange}
                  maxLength={50}
                />
                <p className="mt-1 text-xs text-gray-500">
                  Brève description du produit (50 caractères max)
                </p>
              </div>

              {/* Detailed Description */}
              <div>
                <label htmlFor="detail" className="block text-sm font-medium text-gray-700 mb-1">
                  Détails
                </label>
                <textarea
                  id="detail"
                  name="detail"
                  rows={4}
                  className="block w-full border border-gray-300 rounded-lg shadow-sm py-3 px-4 focus:ring-teal-500 focus:border-teal-500"
                  placeholder="Détails complets du produit..."
                  value={formData.detail}
                  onChange={handleChange}
                  maxLength={255}
                />
                <p className="mt-1 text-xs text-gray-500">
                  Description détaillée du produit (255 caractères max)
                </p>
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
              className="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500 transition-colors duration-150"
            >
              Annuler
            </button>
            <button
              type="submit"
              className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-teal-600 hover:bg-teal-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500 transition-colors duration-150 flex items-center"
            >
              <Save size={18} className="mr-2" />
              {product ? "Mettre à Jour" : "Enregistrer"}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};

export default ProductForm;