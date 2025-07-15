import React, { useState, useEffect, useRef } from "react";
import {
  MapPin,
  Phone,
  Clock,
  Home,
  FileImage,
  FileText,
  Globe,
  Upload,
  X,
  MapIcon,
  Building,
  Hash,
  User,
  Calendar,
  ClipboardCheck,
  CheckCircle,
  AlertCircle,
  Info,
  Plus,
  Minus,
  Search,
  Navigation,
  ChevronUp,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Crosshair,
  Copy,
  CornerUpRight,
  Eye,
  EyeOff,
  Check,
  ArrowRight,
  Bookmark,
  ShoppingBag,
} from "lucide-react";
import { auth } from "../../../config/Firebase";

const BoutiqueForm = ({ boutique, onSubmit, onCancel }) => {
  const [formStep, setFormStep] = useState(1);
  const [formData, setFormData] = useState({
    id_boutique: "",
    nom: "",
    adress: "",
    contact: "",
    horaire: "",
    ville: "",
    localisation: "",
    codePostal: "",
    pays: "",
    boutique_img: "",
    autorisation_image: "",
    numero_patente: "",
  });

  const [errors, setErrors] = useState({});
  const [preview, setPreview] = useState({
    boutique_img: null,
    autorisation_image: null,
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showMap, setShowMap] = useState(false);
  const [mapLoaded, setMapLoaded] = useState(false);
  const [mapPosition, setMapPosition] = useState({ lat: 48.8566, lng: 2.3522 }); // Paris par défaut
  const [addressFromCoords, setAddressFromCoords] = useState("");
  const [searching, setSearching] = useState(false);
  const [searchAddress, setSearchAddress] = useState("");
  const [copiedToClipboard, setCopiedToClipboard] = useState(false);
  const [formCompletion, setFormCompletion] = useState(0);

  const mapRef = useRef(null);
  const positionStepSize = 0.0001; // Pas pour les ajustements de position

  // Initialize form with boutique data if provided (for editing)
  useEffect(() => {
    if (boutique) {
      setFormData({
        id_boutique: boutique.id_boutique || "",
        nom: boutique.nom || "",
        adress: boutique.adress || "",
        contact: boutique.contact || "",
        horaire: boutique.horaire || "",
        ville: boutique.ville || "",
        localisation: boutique.localisation || "",
        codePostal: boutique.codePostal || "",
        pays: boutique.pays || "",
        boutique_img: boutique.boutique_img || "",
        autorisation_image: boutique.autorisation_image || "",
        numero_patente: boutique.numero_patente || "",
      });

      // Si une localisation existe, initialiser la carte avec
      if (boutique.localisation) {
        const [lat, lng] = boutique.localisation
          .split(",")
          .map((coord) => parseFloat(coord.trim()));
        if (!isNaN(lat) && !isNaN(lng)) {
          setMapPosition({ lat, lng });
          setFormData((prev) => ({ ...prev, localisation: `${lat},${lng}` }));
        }
      }
    }
  }, [boutique]);

  // Calculate form completion percentage
  useEffect(() => {
    const requiredFields = [
      "nom",
      "adress",
      "contact",
      "ville",
      "codePostal",
      "pays",
      "numero_patente",
    ];

    const completedFields = requiredFields.filter(
      (field) => formData[field] && formData[field].toString().trim() !== ""
    );

    // Add image fields to the calculation
    if (preview.autorisation_image) completedFields.push("autorisation_image");
    if (formData.localisation) completedFields.push("localisation");

    const totalRequiredFields = requiredFields.length + 2; // +2 for the images and location
    const percentage = Math.floor(
      (completedFields.length / totalRequiredFields) * 100
    );

    setFormCompletion(percentage);
  }, [formData, preview]);

  // Set initial map position from localisation field
  useEffect(() => {
    if (formData.localisation && !mapPosition) {
      const [lat, lng] = formData.localisation
        .split(",")
        .map((coord) => parseFloat(coord.trim()));
      if (!isNaN(lat) && !isNaN(lng)) {
        setMapPosition({ lat, lng });
      }
    }
  }, [formData.localisation, mapPosition]);

  // Clean up URL objects when component unmounts
  useEffect(() => {
    return () => {
      if (preview.boutique_img) URL.revokeObjectURL(preview.boutique_img);
      if (preview.autorisation_image)
        URL.revokeObjectURL(preview.autorisation_image);
    };
  }, [preview]);

  // Effet pour gérer le message de copie dans le presse-papier
  useEffect(() => {
    if (copiedToClipboard) {
      const timer = setTimeout(() => {
        setCopiedToClipboard(false);
      }, 2000);
      return () => clearTimeout(timer);
    }
  }, [copiedToClipboard]);

  // Handle form input changes
  const handleInputChange = (e) => {
    const { name, value } = e.target;

    // Handle special cases for different field types
    if (name === "contact" || name === "codePostal") {
      // For numeric fields, only allow numbers
      const numericValue = value.replace(/\D/g, "");
      setFormData({ ...formData, [name]: numericValue });
    } else {
      setFormData({ ...formData, [name]: value });
    }

    // Clear error for the field when user modifies it
    if (errors[name]) {
      setErrors({ ...errors, [name]: null });
    }

    // Si l'utilisateur change manuellement les coordonnées, mettre à jour l'état de la carte
    if (name === "localisation") {
      const parts = value.split(",");
      if (parts.length === 2) {
        const lat = parseFloat(parts[0].trim());
        const lng = parseFloat(parts[1].trim());
        if (!isNaN(lat) && !isNaN(lng)) {
          setMapPosition({ lat, lng });
        }
      }
    }
  };

  // Handle file upload changes
  const handleFileChange = (e) => {
    const { name, files } = e.target;
    if (files && files[0]) {
      // Create a FormData object for the file
      const fileData = new FormData();
      fileData.append("file", files[0]);

      // Store the file in formData for later processing
      setFormData((prev) => ({
        ...prev,
        [name]: files[0].name,
        // Store the file object for later upload
        [`${name}_file`]: files[0],
      }));

      // Create preview URL
      const previewUrl = URL.createObjectURL(files[0]);
      setPreview((prev) => ({ ...prev, [name]: previewUrl }));

      // Clear any error
      if (errors[name]) {
        setErrors({ ...errors, [name]: null });
      }
    }
  };

  // Utiliser ma position actuelle (via l'API Geolocation native)
  const handleUseMyLocation = () => {
    if (navigator.geolocation) {
      setSearching(true);
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const { latitude, longitude } = position.coords;
          setMapPosition({ lat: latitude, lng: longitude });
          setFormData((prev) => ({
            ...prev,
            localisation: `${latitude},${longitude}`,
          }));

          // Afficher la carte après avoir obtenu la position
          setShowMap(true);
          setSearching(false);

          // Simuler l'obtention de l'adresse à partir des coordonnées (géocodage inverse)
          setTimeout(() => {
            setAddressFromCoords("123 Rue Example, Ville, Pays");
          }, 1000);
        },
        (error) => {
          console.error("Erreur de géolocalisation:", error);
          alert(
            "Impossible d'obtenir votre position. Veuillez vérifier vos paramètres de localisation."
          );
          setSearching(false);
        }
      );
    } else {
      alert("La géolocalisation n'est pas supportée par votre navigateur.");
    }
  };

  // Fonction pour rechercher les coordonnées à partir de l'adresse
  const handleSearchAddress = (e) => {
    e.preventDefault();
    if (!searchAddress.trim()) return;

    setSearching(true);

    // Simule un appel à une API de géocodage
    setTimeout(() => {
      // Générer des coordonnées fictives (Paris avec une légère variation)
      const lat = 48.8566 + (Math.random() - 0.5) * 0.01;
      const lng = 2.3522 + (Math.random() - 0.5) * 0.01;

      setMapPosition({ lat, lng });
      setFormData((prev) => ({
        ...prev,
        localisation: `${lat},${lng}`,
      }));

      setShowMap(true);
      setSearching(false);

      // Simuler l'adresse trouvée
      setAddressFromCoords(searchAddress);
    }, 1000);
  };

  // Fonctions pour ajuster la position manuellement
  const movePosition = (direction) => {
    const { lat, lng } = mapPosition;
    let newLat = lat;
    let newLng = lng;

    switch (direction) {
      case "up":
        newLat += positionStepSize;
        break;
      case "down":
        newLat -= positionStepSize;
        break;
      case "left":
        newLng -= positionStepSize;
        break;
      case "right":
        newLng += positionStepSize;
        break;
      default:
        break;
    }

    // Mettre à jour la position
    setMapPosition({ lat: newLat, lng: newLng });
    setFormData((prev) => ({
      ...prev,
      localisation: `${newLat},${newLng}`,
    }));
  };

  // Fonction pour copier les coordonnées dans le presse-papier
  const copyCoordinates = () => {
    if (formData.localisation) {
      navigator.clipboard.writeText(formData.localisation);
      setCopiedToClipboard(true);
    }
  };

  // Validation function
  const validateForm = () => {
    const newErrors = {};
    const requiredFields = [
      "nom",
      "adress",
      "ville",
      "pays",
      "numero_patente",
      "contact",
      "codePostal",
    ];

    requiredFields.forEach((field) => {
      if (!formData[field] || !formData[field].toString().trim()) {
        newErrors[field] = `Ce champ est requis`;
      }
    });

    // Vérifier l'image d'autorisation (obligatoire)
    if (!formData.autorisation_image && !preview.autorisation_image) {
      newErrors.autorisation_image = "L'image d'autorisation est requise";
    }

    // Validate contact is numeric
    if (formData.contact && !/^\d+$/.test(formData.contact)) {
      newErrors.contact =
        "Le numéro de contact doit contenir uniquement des chiffres";
    }

    // Validate postal code is numeric
    if (formData.codePostal && !/^\d+$/.test(formData.codePostal)) {
      newErrors.codePostal =
        "Le code postal doit contenir uniquement des chiffres";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Handle form step navigation
  const nextStep = () => {
    // Validate current step before moving forward
    if (formStep === 1) {
      const basicInfoErrors = {};
      ["nom", "adress", "contact", "ville", "codePostal", "pays"].forEach(
        (field) => {
          if (!formData[field] || !formData[field].toString().trim()) {
            basicInfoErrors[field] = `Ce champ est requis`;
          }
        }
      );

      if (Object.keys(basicInfoErrors).length > 0) {
        setErrors(basicInfoErrors);
        return;
      }
    }

    setFormStep((prev) => prev + 1);
  };

  const prevStep = () => {
    setFormStep((prev) => prev - 1);
  };

  // Handle form submission with better error handling and file uploads
  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);

    // Validate required fields
    if (!validateForm()) {
      // Scroll to the first error
      const firstErrorField = Object.keys(errors)[0];
      if (firstErrorField) {
        document
          .getElementById(firstErrorField)
          ?.scrollIntoView({ behavior: "smooth", block: "center" });
      }

      setIsSubmitting(false);
      return;
    }

    try {
      console.log("Vérification de l'authentification...");

      // Vérifier si l'utilisateur est authentifié
      const user = auth.currentUser;
      if (!user) {
        console.error(
          "⚠️ Utilisateur non authentifié lors de la soumission du formulaire"
        );
        alert(
          "Vous devez être connecté pour effectuer cette action. Veuillez vous reconnecter."
        );
        setIsSubmitting(false);
        return;
      }

      console.log("✅ Utilisateur authentifié:", user.uid);

      // Créer un objet pour la soumission finale
      const submissionData = { ...formData };

      // IMPORTANT: Assurer que le champ contact est de type INT pour correspondre au schéma
      if (
        typeof submissionData.contact !== "string" ||
        submissionData.contact.trim() === ""
      ) {
        submissionData.contact = 0; // Valeur par défaut
      } else {
        submissionData.contact = parseInt(submissionData.contact.trim(), 10);
      }

      // Assurer que le code postal est de type INT
      if (
        typeof submissionData.codePostal !== "string" ||
        submissionData.codePostal.trim() === ""
      ) {
        submissionData.codePostal = 0; // Valeur par défaut
      } else {
        submissionData.codePostal = parseInt(
          submissionData.codePostal.trim(),
          10
        );
      }

      // Gérer les fichiers pour les images
      if (formData.boutique_img_file) {
        console.log("Simulation de téléchargement de l'image de boutique...");
        submissionData.boutique_img = `boutique_${Date.now()}.jpg`;
        delete submissionData.boutique_img_file;
      }

      if (formData.autorisation_image_file) {
        console.log(
          "Simulation de téléchargement de l'image d'autorisation..."
        );
        submissionData.autorisation_image = `autorisation_${Date.now()}.jpg`;
        delete submissionData.autorisation_image_file;
      }

      console.log("Récupération d'un token d'authentification frais...");
      const idToken = await user.getIdToken(true);
      console.log("✅ Token obtenu avec succès");

      console.log("Données préparées pour la soumission:", submissionData);

      // MODIFICATION ICI: Si nous sommes en mode édition, envoyez directement les données
      // sinon, envoyez-les dans la structure formData/idToken/userId
      if (boutique) {
        // Mode édition (mise à jour)
        console.log("Mode mise à jour - envoi direct des données");
        onSubmit(submissionData);
      } else {
        // Mode création
        console.log("Mode création - envoi de la structure avec formData");
        onSubmit({
          formData: submissionData,
          idToken,
          userId: user.uid,
        });
      }
    } catch (error) {
      console.error("❌ Erreur lors de la soumission du formulaire:", error);
      alert(`Une erreur est survenue: ${error.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Composant de légende pour les champs requis
  const RequiredFieldNote = () => (
    <div className="flex items-center text-sm text-gray-600 mb-6 bg-blue-50 p-3 rounded-lg border-l-4 border-blue-400">
      <AlertCircle size={16} className="text-teal-500 mr-2" />
      <span>
        Les champs marqués d'un{" "}
        <span className="text-red-500 font-medium">*</span> sont obligatoires
      </span>
    </div>
  );

  // Construire l'URL OpenStreetMap pour la iframe
  const getOpenStreetMapEmbedURL = () => {
    if (!mapPosition) return "";

    const { lat, lng } = mapPosition;
    const zoom = 16; // Niveau de zoom

    return `https://www.openstreetmap.org/export/embed.html?bbox=${
      lng - 0.005
    },${lat - 0.005},${lng + 0.005},${
      lat + 0.005
    }&layer=mapnik&marker=${lat},${lng}`;
  };

  // Progress indicator component
  const ProgressBar = () => (
    <div className="mb-8">
      <div className="flex justify-between mb-2">
        <span className="text-sm font-medium text-gray-700">
          Progression du formulaire
        </span>
        <span className="text-sm font-medium text-teal-600">
          {formCompletion}%
        </span>
      </div>
      <div className="w-full bg-gray-200 rounded-full h-2.5">
        <div
          className="bg-gradient-to-r from-teal-500 to-teal-600 h-2.5 rounded-full transition-all duration-500"
          style={{ width: `${formCompletion}%` }}
        ></div>
      </div>
    </div>
  );

  // Step indicator component
  const StepIndicator = () => (
    <div className="flex mb-8 w-full justify-center">
      <div className="flex items-center">
        <div
          className={`flex items-center justify-center w-10 h-10 rounded-full ${
            formStep >= 1
              ? "bg-teal-500 text-white"
              : "bg-gray-200 text-gray-600"
          }`}
        >
          1
        </div>
        <div
          className={`h-1 w-10 ${formStep > 1 ? "bg-teal-500" : "bg-gray-200"}`}
        ></div>
        <div
          className={`flex items-center justify-center w-10 h-10 rounded-full ${
            formStep >= 2
              ? "bg-teal-500 text-white"
              : "bg-gray-200 text-gray-600"
          }`}
        >
          2
        </div>
        <div
          className={`h-1 w-10 ${formStep > 2 ? "bg-teal-500" : "bg-gray-200"}`}
        ></div>
        <div
          className={`flex items-center justify-center w-10 h-10 rounded-full ${
            formStep >= 3
              ? "bg-teal-500 text-white"
              : "bg-gray-200 text-gray-600"
          }`}
        >
          3
        </div>
      </div>
    </div>
  );

  return (
    <div className="bg-white shadow-xl rounded-2xl overflow-hidden border border-gray-200 transition-all duration-300 max-w-5xl mx-auto">
      <div className="bg-gradient-to-r from-indigo-600 to-purple-600 px-6 py-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-bold text-white flex items-center">
              <ShoppingBag className="mr-3" size={28} />
              {boutique
                ? "Modifier la Boutique"
                : "Créer une Nouvelle Boutique"}
            </h2>
            <p className="text-purple-100 mt-1">
              Configurez votre espace de vente et atteignez de nouveaux clients
            </p>
          </div>
          <div className="hidden md:flex items-center bg-white bg-opacity-20 rounded-lg px-4 py-2 text-black backdrop-blur-sm">
            <Bookmark className="mr-2" size={18} />
            <span>{formCompletion}% complété</span>
          </div>
        </div>
      </div>

      <div className="p-6 md:p-8">
        <ProgressBar />
        <StepIndicator />

        <form onSubmit={handleSubmit}>
          {/* Step 1: Informations de base */}
          {formStep === 1 && (
            <div className="space-y-6 animate-fadeIn">
              <div className="bg-teal-50 p-4 rounded-lg border-l-4 border-teal-500 mb-6">
                <h4 className="text-lg font-semibold text-teal-800 mb-2 flex items-center">
                  <User className="mr-2 text-teal-600" size={20} />
                  Informations Principales
                </h4>
                <p className="text-teal-700 text-sm">
                  Commençons par les informations essentielles de votre
                  boutique.
                </p>
              </div>

              <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                {/* Nom de la boutique */}
                <div className="col-span-2">
                  <label
                    htmlFor="nom"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Nom de la boutique <span className="text-red-500">*</span>
                  </label>
                  <div className="mt-1 relative rounded-md shadow-sm">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <ShoppingBag className="h-5 w-5 text-gray-400" />
                    </div>
                    <input
                      type="text"
                      name="nom"
                      id="nom"
                      value={formData.nom}
                      onChange={handleInputChange}
                      required
                      className={`pl-10 focus:ring-teal-500 focus:border-teal-500 block w-full shadow-sm sm:text-sm ${
                        errors.nom
                          ? "border-red-300 bg-red-50"
                          : "border-gray-300"
                      } rounded-lg py-3`}
                      placeholder="Nom attractif de votre boutique"
                    />
                    {errors.nom && (
                      <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                        <AlertCircle className="h-5 w-5 text-red-500" />
                      </div>
                    )}
                  </div>
                  {errors.nom && (
                    <p className="mt-1 text-sm text-red-600">{errors.nom}</p>
                  )}
                </div>

                {/* Contact */}
                <div>
                  <label
                    htmlFor="contact"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Téléphone <span className="text-red-500">*</span>
                  </label>
                  <div className="mt-1 relative rounded-md shadow-sm">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <Phone className="h-5 w-5 text-gray-400" />
                    </div>
                    <input
                      type="tel"
                      name="contact"
                      id="contact"
                      value={formData.contact}
                      onChange={handleInputChange}
                      required
                      className={`pl-10 focus:ring-teal-500 focus:border-teal-500 block w-full shadow-sm sm:text-sm ${
                        errors.contact
                          ? "border-red-300 bg-red-50"
                          : "border-gray-300"
                      } rounded-lg py-3`}
                      placeholder="Numéro de téléphone"
                    />
                    {errors.contact && (
                      <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                        <AlertCircle className="h-5 w-5 text-red-500" />
                      </div>
                    )}
                  </div>
                  {errors.contact && (
                    <p className="mt-1 text-sm text-red-600">
                      {errors.contact}
                    </p>
                  )}
                  <p className="mt-1 text-xs text-gray-500">
                    Format: Chiffres uniquement, sans espaces ni caractères
                    spéciaux.
                  </p>
                </div>

                {/* Horaires */}
                <div>
                  <label
                    htmlFor="horaire"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Horaires d'ouverture
                  </label>
                  <div className="mt-1 relative rounded-md shadow-sm">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <Clock className="h-5 w-5 text-gray-400" />
                    </div>
                    <input
                      type="text"
                      name="horaire"
                      id="horaire"
                      value={formData.horaire}
                      onChange={handleInputChange}
                      className={`pl-10 focus:ring-teal-500 focus:border-teal-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-lg py-3`}
                      placeholder="Ex: Lun-Ven: 9h-18h, Sam: 10h-16h"
                    />
                  </div>
                  <p className="mt-1 text-xs text-gray-500">
                    Format recommandé: Jour: hh-hh, Jour: hh-hh
                  </p>
                </div>

                {/* Adresse */}
                <div className="col-span-2">
                  <label
                    htmlFor="adress"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Adresse <span className="text-red-500">*</span>
                  </label>
                  <div className="mt-1 relative rounded-md shadow-sm">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <MapPin className="h-5 w-5 text-gray-400" />
                    </div>
                    <input
                      type="text"
                      name="adress"
                      id="adress"
                      value={formData.adress}
                      onChange={handleInputChange}
                      required
                      className={`pl-10 focus:ring-teal-500 focus:border-teal-500 block w-full shadow-sm sm:text-sm ${
                        errors.adress
                          ? "border-red-300 bg-red-50"
                          : "border-gray-300"
                      } rounded-lg py-3`}
                      placeholder="Adresse complète de la boutique"
                    />
                    {errors.adress && (
                      <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                        <AlertCircle className="h-5 w-5 text-red-500" />
                      </div>
                    )}
                  </div>
                  {errors.adress && (
                    <p className="mt-1 text-sm text-red-600">
                      {errors.adress}
                    </p>
                  )}
                </div>

                {/* Ville */}
                <div>
                  <label
                    htmlFor="ville"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Ville <span className="text-red-500">*</span>
                  </label>
                  <div className="mt-1 relative rounded-md shadow-sm">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <Building className="h-5 w-5 text-gray-400" />
                    </div>
                    <input
                      type="text"
                      name="ville"
                      id="ville"
                      value={formData.ville}
                      onChange={handleInputChange}
                      required
                      className={`pl-10 focus:ring-teal-500 focus:border-teal-500 block w-full shadow-sm sm:text-sm ${
                        errors.ville
                          ? "border-red-300 bg-red-50"
                          : "border-gray-300"
                      } rounded-lg py-3`}
                      placeholder="Ville"
                    />
                    {errors.ville && (
                      <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                        <AlertCircle className="h-5 w-5 text-red-500" />
                      </div>
                    )}
                  </div>
                  {errors.ville && (
                    <p className="mt-1 text-sm text-red-600">{errors.ville}</p>
                  )}
                </div>

                {/* Code Postal */}
                <div>
                  <label
                    htmlFor="codePostal"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Code Postal <span className="text-red-500">*</span>
                  </label>
                  <div className="mt-1 relative rounded-md shadow-sm">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <Hash className="h-5 w-5 text-gray-400" />
                    </div>
                    <input
                      type="text"
                      name="codePostal"
                      id="codePostal"
                      value={formData.codePostal}
                      onChange={handleInputChange}
                      required
                      className={`pl-10 focus:ring-teal-500 focus:border-teal-500 block w-full shadow-sm sm:text-sm ${
                        errors.codePostal
                          ? "border-red-300 bg-red-50"
                          : "border-gray-300"
                      } rounded-lg py-3`}
                      placeholder="Code postal"
                    />
                    {errors.codePostal && (
                      <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                        <AlertCircle className="h-5 w-5 text-red-500" />
                      </div>
                    )}
                  </div>
                  {errors.codePostal && (
                    <p className="mt-1 text-sm text-red-600">
                      {errors.codePostal}
                    </p>
                  )}
                </div>

                {/* Pays */}
                <div>
                  <label
                    htmlFor="pays"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Pays <span className="text-red-500">*</span>
                  </label>
                  <div className="mt-1 relative rounded-md shadow-sm">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <Globe className="h-5 w-5 text-gray-400" />
                    </div>
                    <input
                      type="text"
                      name="pays"
                      id="pays"
                      value={formData.pays}
                      onChange={handleInputChange}
                      required
                      className={`pl-10 focus:ring-teal-500 focus:border-teal-500 block w-full shadow-sm sm:text-sm ${
                        errors.pays
                          ? "border-red-300 bg-red-50"
                          : "border-gray-300"
                      } rounded-lg py-3`}
                      placeholder="Pays"
                    />
                    {errors.pays && (
                      <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                        <AlertCircle className="h-5 w-5 text-red-500" />
                      </div>
                    )}
                  </div>
                  {errors.pays && (
                    <p className="mt-1 text-sm text-red-600">{errors.pays}</p>
                  )}
                </div>
              </div>

              <div className="flex justify-end pt-6">
                <button
                  type="button"
                  onClick={nextStep}
                  className="px-5 py-3 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-teal-600 hover:bg-teal-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500 transition-colors flex items-center"
                >
                  Continuer
                  <ArrowRight className="ml-2 h-5 w-5" />
                </button>
              </div>
            </div>
          )}

          {/* Step 2: Localisation */}
          {formStep === 2 && (
            <div className="space-y-6 animate-fadeIn">
              <div className="bg-teal-50 p-4 rounded-lg border-l-4 border-teal-500 mb-6">
                <h4 className="text-lg font-semibold text-teal-800 mb-2 flex items-center">
                  <MapIcon className="mr-2 text-teal-600" size={20} />
                  Localisation sur la Carte
                </h4>
                <p className="text-teal-700 text-sm">
                  Ajoutez les coordonnées précises de votre boutique pour que
                  les clients puissent vous trouver facilement.
                </p>
              </div>

              <div className="mb-6">
                <div className="flex flex-wrap gap-3 mb-4">
                  <button
                    type="button"
                    onClick={handleUseMyLocation}
                    className={`inline-flex items-center px-4 py-3 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white ${
                      searching
                        ? "bg-blue-400"
                        : "bg-blue-600 hover:bg-blue-700"
                    } focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors`}
                    disabled={searching}
                  >
                    <Navigation className="mr-2 h-5 w-5" />
                    {searching ? "Localisation..." : "Utiliser ma position"}
                  </button>

                  <button
                    type="button"
                    onClick={() => setShowMap(!showMap)}
                    className="inline-flex items-center px-4 py-3 border border-gray-300 rounded-lg shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500 transition-colors"
                  >
                    {showMap ? (
                      <Minus className="mr-2 h-5 w-5" />
                    ) : (
                      <Plus className="mr-2 h-5 w-5" />
                    )}
                    {showMap ? "Masquer la carte" : "Afficher la carte"}
                  </button>
                </div>

                {/* Zone de recherche d'adresse */}
                <div className="mt-4 flex gap-2">
                  <div className="relative flex-grow">
                    <input
                      type="text"
                      value={searchAddress}
                      onChange={(e) => setSearchAddress(e.target.value)}
                      placeholder="Rechercher une adresse..."
                      className="pl-10 pr-4 py-3 block w-full shadow-sm text-sm border-gray-300 rounded-lg focus:ring-teal-500 focus:border-teal-500"
                    />
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <Search className="h-5 w-5 text-gray-400" />
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={handleSearchAddress}
                    disabled={searching || !searchAddress.trim()}
                    className={`inline-flex items-center px-4 py-2 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white ${
                      searching || !searchAddress.trim()
                        ? "bg-teal-400"
                        : "bg-teal-600 hover:bg-teal-700"
                    } focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500 transition-colors`}
                  >
                    {searching ? "Recherche..." : "Rechercher"}
                  </button>
                </div>

                {/* Champ de coordonnées */}
                <div className="mt-4">
                  <label
                    htmlFor="localisation"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Coordonnées (latitude, longitude)
                  </label>
                  <div className="mt-1 flex rounded-md shadow-sm">
                    <div className="relative flex-grow">
                      <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                        <MapPin className="h-5 w-5 text-gray-400" />
                      </div>
                      <input
                        type="text"
                        name="localisation"
                        id="localisation"
                        value={formData.localisation}
                        onChange={handleInputChange}
                        className="pl-10 focus:ring-teal-500 focus:border-teal-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-l-lg py-3"
                        placeholder="Format: 48.8566,2.3522"
                      />
                    </div>
                    <button
                      type="button"
                      onClick={copyCoordinates}
                      className="inline-flex items-center px-3 py-2 border border-l-0 border-gray-300 bg-gray-50 text-gray-500 sm:text-sm rounded-r-lg hover:bg-gray-100 transition-colors"
                    >
                      {copiedToClipboard ? (
                        <CheckCircle className="h-5 w-5 text-green-500" />
                      ) : (
                        <Copy className="h-5 w-5" />
                      )}
                    </button>
                  </div>
                  <p className="mt-1 text-xs text-gray-500">
                    Coordonnées géographiques de votre boutique
                  </p>
                </div>

                {/* Zone d'info */}
                <div className="mt-3 text-sm text-gray-600 flex items-start p-3 bg-blue-50 rounded-lg">
                  <Info className="h-5 w-5 text-teal-500 mr-2 flex-shrink-0 mt-0.5" />
                  <p>
                    Entrez votre adresse et cliquez sur "Rechercher" pour
                    obtenir les coordonnées GPS, ou utilisez le bouton "Utiliser
                    ma position" pour localiser automatiquement votre
                    emplacement actuel.
                  </p>
                </div>
              </div>

              {/* Carte avec contrôles de positionnement */}
              {showMap && (
                <div className="mt-4 border rounded-lg overflow-hidden shadow-md">
                  <div className="relative">
                    {/* Carte OpenStreetMap */}
                    <div className="w-full h-96 bg-gray-100">
                      {mapPosition ? (
                        <iframe
                          width="100%"
                          height="100%"
                          frameBorder="0"
                          scrolling="no"
                          marginHeight="0"
                          marginWidth="0"
                          src={getOpenStreetMapEmbedURL()}
                          style={{ border: "none" }}
                          title="Localisation de la boutique"
                        ></iframe>
                      ) : (
                        <div className="w-full h-full flex items-center justify-center">
                          <div className="text-center p-8">
                            <MapIcon className="mx-auto h-12 w-12 text-gray-400 mb-4" />
                            <h3 className="text-gray-900 font-medium">
                              Aucune localisation définie
                            </h3>
                            <p className="text-gray-500 mt-2">
                              Utilisez les boutons ci-dessus pour définir la
                              localisation de votre boutique.
                            </p>
                          </div>
                        </div>
                      )}
                    </div>

                    {/* Overlay central avec crosshair/marqueur */}
                    {mapPosition && (
                      <div className="absolute inset-0 pointer-events-none flex items-center justify-center">
                        <div className="bg-teal-500 bg-opacity-75 rounded-full p-1 shadow-lg">
                          <Crosshair className="h-6 w-6 text-white" />
                        </div>
                      </div>
                    )}

                    {/* Contrôles d'ajustement de position */}
                    {mapPosition && (
                      <div className="absolute right-4 top-4 bg-white shadow-lg rounded-lg p-2">
                        <div className="flex flex-col items-center space-y-1">
                          <button
                            type="button"
                            onClick={() => movePosition("up")}
                            className="p-1 hover:bg-teal-100 rounded transition-colors"
                            title="Déplacer vers le nord"
                          >
                            <ChevronUp className="h-5 w-5 text-gray-700" />
                          </button>

                          <div className="flex items-center space-x-1">
                            <button
                              type="button"
                              onClick={() => movePosition("left")}
                              className="p-1 hover:bg-teal-100 rounded transition-colors"
                              title="Déplacer vers l'ouest"
                            >
                              <ChevronLeft className="h-5 w-5 text-gray-700" />
                            </button>

                            <div className="p-1 rounded bg-teal-100">
                              <Crosshair className="h-4 w-4 text-teal-700" />
                            </div>

                            <button
                              type="button"
                              onClick={() => movePosition("right")}
                              className="p-1 hover:bg-teal-100 rounded transition-colors"
                              title="Déplacer vers l'est"
                            >
                              <ChevronRight className="h-5 w-5 text-gray-700" />
                            </button>
                          </div>

                          <button
                            type="button"
                            onClick={() => movePosition("down")}
                            className="p-1 hover:bg-teal-100 rounded transition-colors"
                            title="Déplacer vers le sud"
                          >
                            <ChevronDown className="h-5 w-5 text-gray-700" />
                          </button>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Légende et informations */}
                  {mapPosition && (
                    <div className="bg-gray-50 p-4 text-sm border-t border-gray-200">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center text-gray-700">
                          <Crosshair className="h-4 w-4 text-teal-600 mr-2" />
                          Position actuelle:
                          <span className="font-mono ml-2 bg-gray-100 px-2 py-1 rounded text-teal-700">
                            {mapPosition.lat.toFixed(6)},{" "}
                            {mapPosition.lng.toFixed(6)}
                          </span>
                        </div>
                        <div className="text-xs text-gray-500">
                          Utilisez les flèches pour ajuster précisément la
                          position
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              )}

              <div className="flex justify-between pt-6">
                <button
                  type="button"
                  onClick={prevStep}
                  className="px-5 py-3 border border-gray-300 rounded-lg shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500 transition-colors"
                >
                  Retour
                </button>
                <button
                  type="button"
                  onClick={nextStep}
                  className="px-5 py-3 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-teal-600 hover:bg-teal-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500 transition-colors flex items-center"
                >
                  Continuer
                  <ArrowRight className="ml-2 h-5 w-5" />
                </button>
              </div>
            </div>
          )}

          {/* Step 3: Documents et Images */}
          {formStep === 3 && (
            <div className="space-y-6 animate-fadeIn">
              <div className="bg-teal-50 p-4 rounded-lg border-l-4 border-teal-500 mb-6">
                <h4 className="text-lg font-semibold text-teal-800 mb-2 flex items-center">
                  <FileImage className="mr-2 text-teal-600" size={20} />
                  Documents et Images
                </h4>
                <p className="text-teal-700 text-sm">
                  Téléchargez les documents nécessaires pour votre boutique.
                </p>
              </div>

              <div className="grid grid-cols-1 gap-8 sm:grid-cols-2">
                {/* Image de la boutique */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Image de la boutique
                  </label>
                  <div
                    className="border-2 border-dashed border-gray-300 rounded-lg p-6 flex flex-col items-center justify-center hover:border-teal-500 transition-colors cursor-pointer bg-gray-50 h-64"
                    onClick={() =>
                      document.getElementById("boutique_img").click()
                    }
                  >
                    {preview.boutique_img ? (
                      <div className="relative w-full h-full">
                        <img
                          src={preview.boutique_img}
                          alt="Aperçu de la boutique"
                          className="mx-auto h-full object-contain rounded-md"
                        />
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            setPreview((prev) => ({
                              ...prev,
                              boutique_img: null,
                            }));
                            setFormData((prev) => ({
                              ...prev,
                              boutique_img: "",
                              boutique_img_file: null,
                            }));
                          }}
                          className="absolute top-2 right-2 bg-red-500 text-white p-1 rounded-full hover:bg-red-600 transition-colors shadow"
                        >
                          <X size={14} />
                        </button>
                      </div>
                    ) : (
                      <>
                        <Upload className="h-12 w-12 text-teal-400 mb-3" />
                        <div className="text-center space-y-1">
                          <p className="text-sm text-gray-600">
                            Cliquez pour télécharger une image
                          </p>
                          <p className="text-xs text-gray-500">
                            JPG, PNG, GIF (max. 2MB)
                          </p>
                        </div>
                      </>
                    )}
                    <input
                      id="boutique_img"
                      name="boutique_img"
                      type="file"
                      accept="image/*"
                      onChange={handleFileChange}
                      className="sr-only"
                    />
                  </div>
                  <p className="mt-2 text-sm text-gray-600">
                    Cette image sera affichée sur votre page boutique et dans
                    les résultats de recherche.
                  </p>
                </div>

                {/* Documents d'autorisation */}
                <div className="space-y-6">
                  {/* Numéro de patente */}
                  <div>
                    <label
                      htmlFor="numero_patente"
                      className="block text-sm font-medium text-gray-700"
                    >
                      Numéro de patente <span className="text-red-500">*</span>
                    </label>
                    <div className="mt-1 relative rounded-md shadow-sm">
                      <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                        <ClipboardCheck className="h-5 w-5 text-gray-400" />
                      </div>
                      <input
                        type="text"
                        name="numero_patente"
                        id="numero_patente"
                        value={formData.numero_patente}
                        onChange={handleInputChange}
                        required
                        className={`pl-10 focus:ring-teal-500 focus:border-teal-500 block w-full shadow-sm sm:text-sm ${
                          errors.numero_patente
                            ? "border-red-300 bg-red-50"
                            : "border-gray-300"
                        } rounded-lg py-3`}
                        placeholder="Numéro d'autorisation légale"
                      />
                      {errors.numero_patente && (
                        <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                          <AlertCircle className="h-5 w-5 text-red-500" />
                        </div>
                      )}
                    </div>
                    {errors.numero_patente && (
                      <p className="mt-1 text-sm text-red-600">
                        {errors.numero_patente}
                      </p>
                    )}
                  </div>

                  {/* Image d'autorisation */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Image d'autorisation{" "}
                      <span className="text-red-500">*</span>
                    </label>
                    <div
                      className={`border-2 border-dashed rounded-lg p-4 flex flex-col items-center justify-center transition-colors cursor-pointer h-40 ${
                        errors.autorisation_image
                          ? "border-red-300 bg-red-50"
                          : "border-gray-300 bg-gray-50 hover:border-teal-500"
                      }`}
                      onClick={() =>
                        document.getElementById("autorisation_image").click()
                      }
                    >
                      {preview.autorisation_image ? (
                        <div className="relative w-full h-full">
                          <img
                            src={preview.autorisation_image}
                            alt="Aperçu de l'autorisation"
                            className="mx-auto h-full object-contain rounded-md"
                          />
                          <button
                            type="button"
                            onClick={(e) => {
                              e.stopPropagation();
                              setPreview((prev) => ({
                                ...prev,
                                autorisation_image: null,
                              }));
                              setFormData((prev) => ({
                                ...prev,
                                autorisation_image: "",
                                autorisation_image_file: null,
                              }));
                            }}
                            className="absolute top-2 right-2 bg-red-500 text-white p-1 rounded-full hover:bg-red-600 transition-colors shadow"
                          >
                            <X size={14} />
                          </button>
                        </div>
                      ) : (
                        <>
                          <FileText className="h-10 w-10 text-teal-400 mb-2" />
                          <div className="text-center space-y-1">
                            <p className="text-sm text-gray-600">
                              Document d'autorisation
                            </p>
                            <p className="text-xs text-gray-500">
                              Obligatoire pour l'inscription
                            </p>
                          </div>
                        </>
                      )}
                      <input
                        id="autorisation_image"
                        name="autorisation_image"
                        type="file"
                        accept="image/*,.pdf"
                        onChange={handleFileChange}
                        className="sr-only"
                      />
                    </div>
                    {errors.autorisation_image && (
                      <p className="mt-1 text-sm text-red-600">
                        {errors.autorisation_image}
                      </p>
                    )}
                    <p className="mt-2 text-sm text-gray-600">
                      Document justificatif montrant le numéro de patente
                      (format PDF ou image)
                    </p>
                  </div>
                </div>
              </div>

              <RequiredFieldNote />

              <div className="flex justify-between pt-6 border-t border-gray-200">
                <button
                  type="button"
                  onClick={prevStep}
                  className="px-5 py-3 border border-gray-300 rounded-lg shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-colors"
                >
                  Retour
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-6 py-3 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-gradient-to-r from-teal-500 to-teal-700 hover:from-teal-600 hover:to-teal-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500 transition-all flex items-center"
                >
                  {isSubmitting ? (
                    <>
                      <div className="mr-2 h-5 w-5 border-t-2 border-b-2 border-white rounded-full animate-spin"></div>
                      Traitement...
                    </>
                  ) : (
                    <>
                      <CheckCircle className="mr-2 h-5 w-5" />
                      {boutique
                        ? "Mettre à jour la boutique"
                        : "Créer ma boutique"}
                    </>
                  )}
                </button>
              </div>
            </div>
          )}
        </form>
      </div>

      {/* Animated footer */}
      <div className="bg-gradient-to-r from-teal-50 to-teal-100 py-4 px-6 border-t border-gray-200">
        <div className="flex justify-between items-center">
          <button
            type="button"
            onClick={onCancel}
            className="text-gray-600 hover:text-gray-900 text-sm font-medium flex items-center"
          >
            <X className="mr-1 h-4 w-4" />
            Annuler
          </button>
          <div className="text-xs text-gray-500">
            {boutique ? "Modification" : "Création"} d'une boutique •{" "}
            {new Date().toLocaleDateString()}
          </div>
        </div>
      </div>
    </div>
  );
};

export default BoutiqueForm;