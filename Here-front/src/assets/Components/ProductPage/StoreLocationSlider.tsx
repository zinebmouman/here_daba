// src/Components/ProductPage/StoreLocationSlider.tsx
import React, { useState, useEffect, useRef } from "react";
import { ChevronLeft, ChevronRight, Store, AlertTriangle } from "lucide-react";
import StoreCard from "./StoreCard";

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
  [key: string]: any; // Pour les propriétés supplémentaires
}

interface StoreLocationSliderProps {
  stores: StoreProps[];
  title?: string;
  productId?: string | number | null;
  productName?: string | null;
}

const StoreLocationSlider: React.FC<StoreLocationSliderProps> = ({ 
  stores = [], 
  title = "where the product",
  productId = null,
  productName = null
}) => {
  // Vérification défensive pour le tableau stores
  if (!Array.isArray(stores)) {
    console.warn("StoreLocationSlider: la prop stores n'est pas un tableau");
    stores = [];
  }

  // États
  const [filteredStores, setFilteredStores] = useState<StoreProps[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [currentIndex, setCurrentIndex] = useState<number>(0);
  const [slidesToShow, setSlidesToShow] = useState<number>(4);
  const [isAnimating, setIsAnimating] = useState<boolean>(false);
  const sliderRef = useRef<HTMLDivElement>(null);

  // Filtrer les magasins qui contiennent le produit si productId est fourni
  useEffect(() => {
    if (productId) {
      setLoading(true);
      // Dans un scénario réel, vous feriez un appel API ici pour obtenir les boutiques avec ce produit
      // ou vous filtreriez le tableau stores si celui-ci contient déjà cette information
      
      // Pour l'exemple, nous allons simplement utiliser le tableau complet
      // En production, vous devriez implémenter une vérification réelle
      setTimeout(() => {
        try {
          setFilteredStores(stores);
          setError(null);
        } catch (err) {
          console.error("Erreur lors du filtrage des magasins:", err);
          setError("Impossible de charger les magasins pour ce produit");
        } finally {
          setLoading(false);
        }
      }, 500); // Simulation d'un délai d'API
    } else {
      // Si pas de productId, utiliser tous les magasins
      setFilteredStores(stores);
    }
  }, [stores, productId]);

  // Déterminer combien de slides à afficher en fonction de la largeur de la fenêtre
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth < 640) {
        setSlidesToShow(1);
      } else if (window.innerWidth < 768) {
        setSlidesToShow(2);
      } else if (window.innerWidth < 1024) {
        setSlidesToShow(3);
      } else {
        setSlidesToShow(4);
      }
    };

    // Appel initial et écouteur d'événement
    handleResize();
    window.addEventListener("resize", handleResize);

    // Nettoyage
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  // Réinitialiser l'index actuel si le tableau de magasins change
  useEffect(() => {
    setCurrentIndex(0);
  }, [filteredStores.length]);

  // Navigation vers la slide précédente
  const goToPrevious = () => {
    if (isAnimating || currentIndex === 0) return;

    setIsAnimating(true);
    setCurrentIndex((prev) => Math.max(prev - 1, 0));

    // Réinitialiser le drapeau d'animation après la transition
    setTimeout(() => {
      setIsAnimating(false);
    }, 500); // Faire correspondre ceci à la durée de transition CSS
  };

  // Navigation vers la slide suivante
  const goToNext = () => {
    if (isAnimating || currentIndex >= filteredStores.length - slidesToShow) return;

    setIsAnimating(true);
    setCurrentIndex((prev) => Math.min(prev + 1, filteredStores.length - slidesToShow));

    // Réinitialiser le drapeau d'animation après la transition
    setTimeout(() => {
      setIsAnimating(false);
    }, 500); // Faire correspondre ceci à la durée de transition CSS
  };

  const canGoBack = currentIndex > 0;
  const canGoForward = currentIndex < filteredStores.length - slidesToShow;

  // État de chargement
  if (loading) {
    return (
      <section className="all py-12 lg:mx-8">
        <div className="container mx-auto px-4">
          <h2 className="text-3xl font-bold text-gray-900 mb-8">{title}</h2>
          <div className="bg-gray-50 rounded-xl p-8 text-center shadow-sm flex flex-col items-center justify-center">
            <div className="h-12 w-12 border-4 border-t-teal-500 border-gray-200 rounded-full animate-spin mb-4"></div>
            <p className="text-gray-600">Recherche des boutiques qui proposent ce produit...</p>
          </div>
        </div>
      </section>
    );
  }

  // État d'erreur
  if (error) {
    return (
      <section className="all py-12 lg:mx-8">
        <div className="container mx-auto px-4">
          <h2 className="text-3xl font-bold text-gray-900 mb-8">{title}</h2>
          <div className="bg-red-50 rounded-xl p-8 text-center shadow-sm">
            <div className="flex justify-center mb-4">
              <AlertTriangle size={32} className="text-red-500" />
            </div>
            <p className="text-red-600 font-medium mb-2">Une erreur est survenue</p>
            <p className="text-gray-600">{error}</p>
          </div>
        </div>
      </section>
    );
  }

  // S'il n'y a pas de magasins, afficher un placeholder
  if (filteredStores.length === 0) {
    return (
      <section className="all py-12 lg:mx-8">
        <div className="container mx-auto px-4">
          <h2 className="text-3xl font-bold text-gray-900 mb-8">{title}</h2>
          <div className="bg-gray-50 rounded-xl p-8 text-center shadow-sm">
            <div className="inline-flex items-center justify-center w-16 h-16 bg-gray-100 rounded-full mb-4">
              <Store size={28} className="text-gray-400" />
            </div>
            <h3 className="text-xl font-medium text-gray-900 mb-2">
              {productId ? "Aucune boutique ne propose ce produit actuellement" : "Aucune boutique disponible"}
            </h3>
            <p className="text-gray-500 max-w-lg mx-auto">
              {productId 
                ? "Nous n'avons pas trouvé de boutiques qui proposent ce produit pour le moment. Veuillez vérifier ultérieurement."
                : "Nous n'avons pas trouvé de boutiques disponibles pour le moment."}
            </p>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="all py-12 overflow-hidden lg:mx-8">
      <div className="container mx-auto px-4">
        {/* Header with title and navigation buttons */}
        <div className="flex items-center justify-between mb-8 py-4">
          <h2 className="text-3xl font-bold text-gray-900">
            {title}
            {productName && (
              <span className="block text-base font-normal text-gray-600 mt-1">{productName}</span>
            )}
          </h2>

          <div className="flex space-x-2">
            <button
              onClick={goToPrevious}
              disabled={!canGoBack || isAnimating}
              className={`p-2 rounded-full border ${
                canGoBack && !isAnimating
                  ? "border-gray-300 text-gray-700 hover:bg-gray-100"
                  : "border-gray-200 text-gray-300 cursor-not-allowed"
              }`}
              aria-label="Previous stores"
            >
              <ChevronLeft size={20} />
            </button>
            <button
              onClick={goToNext}
              disabled={!canGoForward || isAnimating}
              className={`p-2 rounded-full border ${
                canGoForward && !isAnimating
                  ? "border-gray-300 text-gray-700 hover:bg-gray-100"
                  : "border-gray-200 text-gray-300 cursor-not-allowed"
              }`}
              aria-label="Next stores"
            >
              <ChevronRight size={20} />
            </button>
          </div>
        </div>

        {/* Progress indicator (only on mobile) */}
        <div className="block sm:hidden mb-6">
          <div className="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden">
            <div 
              className="h-full bg-teal-500 rounded-full transition-all duration-300" 
              style={{ 
                width: `${(currentIndex / Math.max(filteredStores.length - slidesToShow, 1)) * 100}%` 
              }}
            ></div>
          </div>
          <div className="mt-1 text-xs text-gray-500 text-right">
            {Math.min(currentIndex + slidesToShow, filteredStores.length)} sur {filteredStores.length}
          </div>
        </div>

        {/* Slider container with overflow for smooth transitions */}
        <div className="relative overflow-hidden" ref={sliderRef}>
          <div
            className="flex transition-transform duration-500 ease-in-out"
            style={{
              transform: `translateX(-${currentIndex * (100 / slidesToShow)}%)`,
              width: `${(filteredStores.length / Math.max(slidesToShow, 1)) * 100}%`,
            }}
          >
            {filteredStores.map((store, index) => (
              <div
                key={`store-${store?.id || store?.id_boutique || index}`}
                className="px-3"
                style={{ width: `${100 / Math.max(filteredStores.length, 1)}%` }}
              >
                <StoreCard store={store} productName={productName || undefined} />
              </div>
            ))}
          </div>
          
          {/* Shadow indicators for more content */}
          {canGoBack && (
            <div className="absolute top-0 left-0 bottom-0 w-12 bg-gradient-to-r from-white to-transparent z-10 pointer-events-none"></div>
          )}
          {canGoForward && (
            <div className="absolute top-0 right-0 bottom-0 w-12 bg-gradient-to-l from-white to-transparent z-10 pointer-events-none"></div>
          )}
        </div>
        
        {/* Store count indicator (desktop only) */}
        <div className="hidden sm:block mt-6 text-sm text-gray-500 text-right">
          {filteredStores.length} {filteredStores.length === 1 ? 'boutique trouvée' : 'boutiques trouvées'}
          {productName && ' pour ce produit'}
        </div>
      </div>
    </section>
  );
};

export default StoreLocationSlider;