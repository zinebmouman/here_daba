import React, { useEffect, useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { Swiper, SwiperSlide } from 'swiper/react';
import { Navigation } from 'swiper/modules';
import { 
  ShoppingBag, 
  Laptop, 
  Utensils, 
  Home, 
  Smile, 
  Dumbbell, 
  Star, 
  Dog, 
  Shirt, 
  Watch, 
  Car, 
  Music, 
  BookOpen, 
  Smartphone, 
  Tv, 
  Gift 
} from 'lucide-react';
import CategoryCard from './CategoryCard';
import '../style/Navbar.css';

// Import Swiper styles
import 'swiper/css';
import 'swiper/css/navigation';

interface Category {
  id: string;
  name: string;
  icon?: string;
  imageUrl?: string;
}

// Map pour associer les noms de catégories aux icônes Lucide
const iconMap: Record<string, React.ElementType> = {
  'Mode': ShoppingBag,
  'Électronique': Laptop,
  'Alimentation': Utensils,
  'Maison': Home,
  'Beauté': Smile,
  'Sport': Dumbbell,
  'Enfants': Star,
  'Animaux': Dog,
  'Vêtements': Shirt,
  'Montres': Watch,
  'Automobile': Car,
  'Musique': Music,
  'Livres': BookOpen,
  'Téléphonie': Smartphone,
  'Multimédia': Tv,
  'Cadeaux': Gift,
  // Par défaut pour les autres catégories
  'default': ShoppingBag
};

const PopularCategories: React.FC = () => {
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const swiperRef = useRef(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchCategories = async () => {
      try {
        setLoading(true);
        const response = await axios.get('/api/categories');
        const fetchedCategories = response.data.map((cat: any) => ({
          id: cat.idCategorie || cat.id,
          name: cat.nom || cat.name,
          icon: cat.icon || null,
          imageUrl: cat.customIcon || null
        }));
        
        setCategories(fetchedCategories);
        setLoading(false);
      } catch (error) {
        console.error("Erreur lors du chargement des catégories:", error);
        setLoading(false);
      }
    };
    
    fetchCategories();
  }, []);

  // Function to handle category click and navigate to search results
  const handleCategoryClick = (categoryId: string, categoryName: string) => {
    navigate(`/search?category=${categoryId}&type=product`, { 
      state: { 
        categoryName: categoryName 
      } 
    });
  };

  if (loading) {
    return (
      <section className="all py-8 px-16 sm:px-6 lg:px-16">
        <div className="max-w-7xl mx-auto">
          <h2 className="text-2xl font-bold text-gray-900 mb-6">Nos Catégories Populaire</h2>
          <div className="flex justify-center items-center h-40">
            <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-gray-900"></div>
          </div>
        </div>
      </section>
    );
  }

  // Fonction pour obtenir l'icône appropriée en fonction du nom de la catégorie
  const getIconComponent = (categoryName: string) => {
    // Convertir à minuscules pour une comparaison moins stricte
    const normalizedName = categoryName.toLowerCase();
    
    // Chercher une correspondance approximative
    for (const [key, icon] of Object.entries(iconMap)) {
      if (normalizedName.includes(key.toLowerCase()) || key.toLowerCase().includes(normalizedName)) {
        return icon;
      }
    }
    
    // Retourner l'icône par défaut si aucune correspondance
    return iconMap.default;
  };

  return (
    <section className="all py-8 px-16 sm:px-6 lg:px-16">
      <div className="max-w-7xl mx-auto">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold text-gray-900">Nos Catégories Populaire</h2>
          
          <div className="flex space-x-2">
            <button 
              className="p-2 bg-gray-200 rounded-full hover:bg-gray-300 transition-colors" 
              onClick={() => swiperRef.current?.swiper.slidePrev()}
              aria-label="Catégorie précédente"
            >
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-gray-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <button 
              className="p-2 bg-gray-200 rounded-full hover:bg-gray-300 transition-colors"
              onClick={() => swiperRef.current?.swiper.slideNext()}
              aria-label="Catégorie suivante"
            >
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-gray-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
        </div>
        
        <Swiper
          ref={swiperRef}
          modules={[Navigation]}
          spaceBetween={16}
          slidesPerView={8}
          breakpoints={{
            0: {
              slidesPerView: 3,
              spaceBetween: 10,
            },
            640: {
              slidesPerView: 5,
              spaceBetween: 16,
            },
            1024: {
              slidesPerView: 8,
              spaceBetween: 16,
            },
          }}
        >
          {categories.map((category) => {
            const IconComponent = getIconComponent(category.name);
            return (
              <SwiperSlide key={category.id}>
                <div className="bg-gray-50 rounded-xl">
                  <CategoryCard 
                    Icon={IconComponent}
                    Label={category.name}
                    categoryId={category.id}
                    imageUrl={category.imageUrl}
                    onClick={() => handleCategoryClick(category.id, category.name)}
                  />
                </div>
              </SwiperSlide>
            );
          })}
        </Swiper>
      </div>
    </section>
  );
};

export default PopularCategories;