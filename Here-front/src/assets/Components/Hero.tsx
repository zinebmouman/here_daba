import React from 'react';
import Image from '../img/Image.png';

const Hero: React.FC = () => {
  return (
    <div className="all hero py-8 md:py-16 my-5 md:my-10 px-4 md:px-10 md:mx-16 sm:px-6 lg:px-28 lg:h-">
      <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between">
        {/* Text Content */}
        <div className="max-w-xl w-full mb-8 md:mb-0 lg:py-4">
          <p className="stext font-bold mb-4">
            Connectez-vous aux meilleurs vendeurs locaux en un clic.
          </p>
          <h1 className="text-3xl md:text-4xl font-bold text-gray-900 mb-6 leading-tight my-5">
            Découvrez les meilleures offres locales, achetez en toute simplicité !
          </h1>
          <button className="btn1 font-bold text-white px-6 py-3 rounded-full hover:bg-teal-600 transition-colors">
            Explorer les produits
          </button>
        </div>
       
        {/* Product Image */}
        <div className="w-full md:w-1/2">
          <img
            src={Image}
            alt="Produits cosmétiques locaux"
            className="w-full h-auto object-contain"
          />
        </div>
      </div>
    </div>
  );
};

export default Hero;