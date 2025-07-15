// src/Components/ProductPage/StoreCard.tsx
import React, { useState, useEffect, useCallback } from "react";
import { createPortal } from "react-dom";
import { X, MapPin, Navigation, Clock, Phone } from "lucide-react";
import { GoogleMap, Marker, useLoadScript, DirectionsRenderer } from "@react-google-maps/api";

// Types
interface StoreProps {
  id_boutique?: string | number;
  id?: string | number;
  nom?: string;
  name?: string;
  boutiqueImgUrl?: string;
  image?: string;
  boutique_img?: string;
  adress?: string;
  address?: string;
  horaire?: string;
  hours?: string;
  contact?: string | number;
}

interface StoreCardProps {
  store: StoreProps;
  productName?: string;
}

// ✅ CORRECTION: Retirer "directions" de la liste des libraries
const libraries: ("places" | "geometry")[] = ["places", "geometry"];

// Utiliser une constante pour la clé API
const GOOGLE_MAPS_API_KEY = "AIzaSyD_MIvm3dKZdlxJAZa1fY9kRv4wMa_rpus";

// Base URL pour les API
const BASE_URL = "http://localhost:8080";

const StoreCard: React.FC<StoreCardProps> = ({ store, productName }) => {
  // Vérification défensive pour l'objet store
  if (!store) {
    return <div className="bg-gray-50 rounded-3xl p-4 h-48 shadow-sm">Aucune donnée de boutique</div>;
  }

  // Création d'un objet normalisé pour éviter les problèmes de propriétés
  const normalizedStore = {
    id: store.id_boutique || store.id,
    name: store.nom || store.name,
    image: store.boutiqueImgUrl || store.image || 
           (store.boutique_img ? `${BASE_URL}/api/fichiers/${store.boutique_img}` : 
           `${BASE_URL}/api/placeholder/200/200`),
    address: store.adress || store.address,
    hours: store.horaire || store.hours,
    contact: store.contact ? String(store.contact) : "",
  };

  // Utiliser les données normalisées
  const { name, image, address, hours, contact } = normalizedStore;

  // États
  const [showPopup, setShowPopup] = useState<boolean>(false);
  const [imageError, setImageError] = useState<boolean>(false);
  const [storeCoordinates, setStoreCoordinates] = useState<google.maps.LatLngLiteral | null>(null);
  const [userLocation, setUserLocation] = useState<google.maps.LatLngLiteral | null>(null);
  const [distance, setDistance] = useState<string | null>(null);
  const [duration, setDuration] = useState<string | null>(null);
  const [directions, setDirections] = useState<google.maps.DirectionsResult | null>(null);
  const [mapError, setMapError] = useState<string | null>(null);
  const [isMapLoaded, setIsMapLoaded] = useState<boolean>(false);
  const [isCalculatingDistance, setIsCalculatingDistance] = useState<boolean>(false);

  // Charger l'API Google Maps
  const { isLoaded, loadError } = useLoadScript({
    googleMapsApiKey: GOOGLE_MAPS_API_KEY,
    libraries,
  });

  // Géocodage de l'adresse pour obtenir les coordonnées
  const geocodeAddress = useCallback(async () => {
    if (!address || !isLoaded) return;

    try {
      const geocoder = new window.google.maps.Geocoder();
      const result = await new Promise<google.maps.GeocoderResult>((resolve, reject) => {
        geocoder.geocode({ address }, (results, status) => {
          if (status === "OK" && results && results[0]) {
            resolve(results[0]);
          } else {
            reject(new Error(`Échec du géocodage: ${status}`));
          }
        });
      });

      const location = result.geometry.location;
      setStoreCoordinates({ 
        lat: location.lat(), 
        lng: location.lng() 
      });
      setIsMapLoaded(true);
    } catch (error) {
      console.error("Erreur de géocodage:", error);
      setMapError("Impossible de localiser l'adresse");
    }
  }, [address, isLoaded]);

  // Récupérer la position de l'utilisateur - automatiquement au chargement
  const getUserLocation = useCallback(() => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setUserLocation({
            lat: position.coords.latitude,
            lng: position.coords.longitude,
          });
        },
        (error) => {
          console.error("Erreur lors de l'obtention de la position de l'utilisateur:", error);
          // Position par défaut pour Tanger, Maroc si géolocalisation échoue
          setUserLocation({
            lat: 35.7595, // Latitude de Tanger
            lng: -5.8340  // Longitude de Tanger
          });
          setIsCalculatingDistance(false);
        },
        {
          enableHighAccuracy: false,
          timeout: 10000,
          maximumAge: 300000 // 5 minutes
        }
      );
    } else {
      // Position par défaut pour Tanger, Maroc
      setUserLocation({
        lat: 35.7595,
        lng: -5.8340
      });
      setIsCalculatingDistance(false);
    }
  }, []);

  // Calculer la distance et le temps de trajet
  const calculateDistance = useCallback(async () => {
    if (!isLoaded || !storeCoordinates || !userLocation) return;
    
    setIsCalculatingDistance(true);

    try {
      const service = new window.google.maps.DistanceMatrixService();
      const result = await new Promise<google.maps.DistanceMatrixResponse>((resolve, reject) => {
        service.getDistanceMatrix(
          {
            origins: [userLocation],
            destinations: [storeCoordinates],
            travelMode: google.maps.TravelMode.DRIVING,
            unitSystem: google.maps.UnitSystem.METRIC,
          },
          (response, status) => {
            if (status === "OK" && response) {
              resolve(response);
            } else {
              reject(new Error(`Échec de la matrice de distance: ${status}`));
            }
          }
        );
      });

      if (
        result.rows[0].elements[0].status === "OK" &&
        result.rows[0].elements[0].distance &&
        result.rows[0].elements[0].duration
      ) {
        setDistance(result.rows[0].elements[0].distance.text);
        setDuration(result.rows[0].elements[0].duration.text);
      }
    } catch (error) {
      console.error("Erreur de calcul de distance:", error);
      setMapError("Impossible de calculer la distance");
    } finally {
      setIsCalculatingDistance(false);
    }
  }, [isLoaded, storeCoordinates, userLocation]);

  // ✅ CORRECTION: Calculer l'itinéraire sans utiliser la library "directions"
  const calculateRoute = useCallback(async () => {
    if (!isLoaded || !storeCoordinates || !userLocation) return;

    try {
      const directionsService = new window.google.maps.DirectionsService();
      const result = await directionsService.route({
        origin: userLocation,
        destination: storeCoordinates,
        travelMode: google.maps.TravelMode.DRIVING,
      });

      setDirections(result);
    } catch (error) {
      console.error("Erreur d'itinéraire:", error);
      setMapError("Impossible de calculer l'itinéraire");
    }
  }, [isLoaded, storeCoordinates, userLocation]);

  // Géocodage de l'adresse lorsque l'API est chargée
  useEffect(() => {
    if (isLoaded && address) {
      geocodeAddress();
    }
  }, [isLoaded, address, geocodeAddress]);
  
  // Récupérer automatiquement la position de l'utilisateur au chargement du composant
  useEffect(() => {
    getUserLocation();
  }, [getUserLocation]);

  // Lorsque userLocation et storeCoordinates changent, calculer la distance
  useEffect(() => {
    if (userLocation && storeCoordinates) {
      calculateDistance();
    }
  }, [userLocation, storeCoordinates, calculateDistance]);

  // Gestion de l'ouverture/fermeture de la popup
  const handleCardClick = () => {
    setShowPopup(true);
    // Empêcher le défilement du corps lorsque la popup est ouverte
    document.body.style.overflow = "hidden";
  };

  const closePopup = () => {
    setShowPopup(false);
    // Rétablir le défilement du corps lorsque la popup est fermée
    document.body.style.overflow = "auto";
    setDirections(null); // Réinitialiser les directions lors de la fermeture
  };

  // Gérer les erreurs de chargement d'image
  const handleImageError = () => {
    console.log("Échec du chargement de l'image:", image);
    setImageError(true);
  };

  // Lorsque le composant se démonte, s'assurer de rétablir le défilement du corps
  useEffect(() => {
    return () => {
      document.body.style.overflow = "auto";
    };
  }, []);

  // Voir la localisation sur la carte
  const handleViewLocation = (e: React.MouseEvent<HTMLButtonElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setShowPopup(true);
    document.body.style.overflow = "hidden";
  };

  // Obtenir l'itinéraire
  const handleGetDirections = () => {
    if (!userLocation) {
      getUserLocation();
    } else {
      calculateRoute();
    }
  };

  // Créer le composant de la carte de magasin
  const storeCard = (
    <div
      className="all flex flex-col cursor-pointer transition-all duration-300 hover:shadow-lg hover:scale-[1.02] rounded-3xl overflow-hidden bg-white shadow-sm p-4"
      onClick={handleCardClick}
    >
      {/* Store Image with Zoom Effect */}
      <div className="bg-gray-50 rounded-3xl aspect-square mb-4 overflow-hidden">
        {image && !imageError ? (
          <div className="w-full h-full overflow-hidden">
            <img
              src={image}
              alt={name || "Store"}
              className="w-full h-full object-cover transition-transform duration-500 hover:scale-110"
              onError={handleImageError}
            />
          </div>
        ) : (
          <div className="w-full h-full flex items-center justify-center bg-gray-200">
            <span className="text-gray-500 text-3xl font-bold">{name ? name.charAt(0).toUpperCase() : "S"}</span>
          </div>
        )}
      </div>

      {/* Store Info */}
      <div className="flex flex-col h-full justify-between">
        <h3 className="font-bold text-gray-900 mb-2">{name}</h3>
        
        {productName && (
          <div className="flex items-center mb-2">
            <span className="text-xs bg-teal-50 text-teal-600 px-2 py-1 rounded-full">
              {productName}
            </span>
          </div>
        )}
        
        <div className="flex items-center mb-2">
          <MapPin size={14} className="text-gray-400 mr-1" />
          <span className="text-sm text-gray-600 truncate">{address || "Adresse non disponible"}</span>
        </div>
        
        {/* Distance info */}
        <div className="mb-3">
          {isCalculatingDistance ? (
            <div className="flex items-center text-gray-500 text-sm">
              <div className="w-3 h-3 rounded-full border-2 border-t-teal-500 border-gray-200 animate-spin mr-2"></div>
              Calcul de la distance...
            </div>
          ) : distance ? (
            <div className="flex items-center text-teal-600 text-sm font-medium">
              <Navigation size={14} className="mr-1 flex-shrink-0" />
              <span>{distance} • {duration}</span>
            </div>
          ) : (
            <div className="h-5"></div> // Espace réservé pour maintenir la hauteur
          )}
        </div>
        
        {/* Button "Voir sur la carte" */}
        <div className="flex items-center justify-between mt-auto">
          <button
            className="w-full inline-flex items-center justify-center px-4 py-2 bg-green-50 text-green-700 text-sm font-semibold rounded-full hover:bg-green-100 transition-colors"
            onClick={handleViewLocation}
          >
            <MapPin size={16} className="mr-1.5" />
            Voir sur la carte
          </button>
        </div>
      </div>
    </div>
  );

  // Popup à afficher seulement si showPopup est true
  if (!showPopup) {
    return storeCard;
  }

  return (
    <>
      {storeCard}
      {createPortal(
        <div className="fixed inset-0 z-[9999] bg-white flex flex-col">
          {/* Close button */}
          <button
            className="absolute top-6 right-6 z-[10000] p-2 bg-white rounded-full shadow-md hover:bg-gray-100"
            onClick={closePopup}
          >
            <X size={24} />
          </button>

          <div className="grid grid-cols-1 md:grid-cols-12 h-full">
            {/* Left side - Store details */}
            <div className="md:col-span-5 lg:col-span-4 bg-white p-6 overflow-y-auto">
              {/* Store Image */}
              <div className="aspect-square rounded-2xl overflow-hidden mb-6 max-w-sm mx-auto">
                {image && !imageError ? (
                  <img
                    src={image}
                    alt={name || "Store"}
                    className="w-full h-full object-cover"
                    onError={handleImageError}
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center bg-gray-200">
                    <span className="text-gray-500 text-6xl font-bold">{name ? name.charAt(0).toUpperCase() : "S"}</span>
                  </div>
                )}
              </div>

              {/* Store Info */}
              <h2 className="text-3xl font-bold mb-4 text-gray-900">{name}</h2>
              
              <div className="space-y-6">
                {/* Store Address */}
                <div className="flex items-start">
                  <MapPin size={22} className="text-teal-500 mr-3 mt-1 flex-shrink-0" />
                  <div>
                    <p className="text-gray-900 font-semibold mb-1">Adresse</p>
                    <p className="text-gray-700">{address || "Adresse non disponible"}</p>
                  </div>
                </div>
                
                {/* Hours */}
                <div className="flex items-start">
                  <Clock size={22} className="text-teal-500 mr-3 mt-1 flex-shrink-0" />
                  <div>
                    <p className="text-gray-900 font-semibold mb-1">Horaires d'ouverture</p>
                    {hours ? (
                      <p className="text-gray-700">{hours}</p>
                    ) : (
                      <div className="space-y-1">
                        <p className="text-gray-700">Lundi - Vendredi: 9h - 18h</p>
                        <p className="text-gray-700">Samedi: 10h - 16h</p>
                        <p className="text-gray-700">Dimanche: Fermé</p>
                      </div>
                    )}
                  </div>
                </div>
                
                {/* Contact */}
                <div className="flex items-start">
                  <Phone size={22} className="text-teal-500 mr-3 mt-1 flex-shrink-0" />
                  <div>
                    <p className="text-gray-900 font-semibold mb-1">Téléphone</p>
                    <p className="text-gray-700">{contact || "Numéro non disponible"}</p>
                  </div>
                </div>
                
                {/* Distance & Duration */}
                {distance && duration && (
                  <div className="mt-6 p-4 bg-gray-50 rounded-xl">
                    <h3 className="text-lg font-bold mb-3 flex items-center">
                      <Navigation size={18} className="mr-2 text-teal-500" />
                      Informations sur le trajet
                    </h3>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-gray-600 text-sm">Distance</p>
                        <p className="text-gray-900 font-semibold">{distance}</p>
                      </div>
                      <div>
                        <p className="text-gray-600 text-sm">Durée</p>
                        <p className="text-gray-900 font-semibold">{duration}</p>
                      </div>
                    </div>
                  </div>
                )}
                
                {/* Itinéraire Button - Mobile Only */}
                <button 
                  className="w-full bg-teal-500 hover:bg-teal-600 text-white font-bold text-lg py-3 px-6 rounded-lg flex items-center justify-center transition-colors md:hidden mt-4"
                  onClick={handleGetDirections}
                >
                  <Navigation size={22} className="mr-2" />
                  {directions ? "Itinéraire calculé" : "Obtenir l'itinéraire"}
                </button>
              </div>
            </div>

            {/* Right side - Google Map */}
            <div className="md:col-span-7 lg:col-span-8 relative flex flex-col">
              <div className="flex-grow">
                {isLoaded ? (
                  <GoogleMap
                    mapContainerStyle={{ width: "100%", height: "100%" }}
                    center={storeCoordinates || undefined}
                    zoom={15}
                    options={{
                      fullscreenControl: false,
                      streetViewControl: false,
                      mapTypeControl: false,
                      zoomControl: true,
                    }}
                  >
                    {storeCoordinates && <Marker position={storeCoordinates} title={name || undefined} />}
                    {userLocation && <Marker 
                      position={userLocation}
                      icon={{
                        url: "https://maps.google.com/mapfiles/ms/icons/blue-dot.png" 
                      }}
                      title="Votre position"
                    />}
                    {directions && <DirectionsRenderer directions={directions} />}
                  </GoogleMap>
                ) : (
                  <div className="w-full h-full bg-gray-100 flex items-center justify-center">
                    {mapError ? (
                      <p className="text-red-500">{mapError}</p>
                    ) : loadError ? (
                      <p className="text-red-500">Erreur de chargement de Google Maps</p>
                    ) : (
                      <p className="text-gray-500">Chargement de la carte...</p>
                    )}
                  </div>
                )}
              </div>
              
              {/* Get Directions Button - Desktop Only */}
              <button 
                className="hidden md:flex bg-teal-500 hover:bg-teal-600 text-white font-bold text-lg py-4 px-6 items-center justify-center transition-colors"
                onClick={handleGetDirections}
              >
                <Navigation size={24} className="mr-3" />
                {directions ? "Itinéraire calculé" : "Obtenir l'itinéraire"}
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}
    </>
  );
};

export default StoreCard;