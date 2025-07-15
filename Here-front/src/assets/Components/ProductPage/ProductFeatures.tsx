import React from "react";
import { FileText } from "lucide-react";

// Composant pour afficher la description du produit
const ProductFeatures = ({ description }) => {
  // Convertir la description en string au cas où elle serait d'un autre type
  const descriptionText = String(description || "");
  
  // Vérifier si la description existe et n'est pas vide
  const hasDescription = descriptionText.trim().length > 0;
  
  // Ne rien afficher si la description n'existe pas ou est vide
  if (!hasDescription) {
    return null;
  }

  return (
    <section className="py-16 max-w-6xl mx-auto px-4">
      <div className="flex flex-col md:flex-row md:justify-center md:gap-16">
        {/* Title on the left */}
        <div className="md:w-1/3 mb-10 md:mb-0">
          <h2 className="text-3xl font-bold text-gray-900 sticky top-24">
            Explore the Features
          </h2>
        </div>
        
        {/* Description on the right */}
        <div className="md:w-1/2">
          <div className="flex items-start gap-5">
            <div className="flex-shrink-0">
              <FileText size={24} />
            </div>
            <div>
              <h3 className="font-semibold text-xl mb-3 text-gray-800">
                Description
              </h3>
              <p className="text-gray-600">{descriptionText}</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

export default ProductFeatures;