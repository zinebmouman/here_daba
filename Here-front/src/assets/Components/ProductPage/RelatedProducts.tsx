// src/components/RelatedProducts.jsx
import React, { useState, useRef } from "react";
import { ChevronRight, ChevronLeft } from "lucide-react";
import ProductCard2 from "../ProductCard2";

const RelatedProducts = ({ products = [] }) => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isAnimating, setIsAnimating] = useState(false);
  const [visibleItems, setVisibleItems] = useState(4);

  // Determine number of visible items based on screen size
  React.useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth < 640) {
        setVisibleItems(1);
      } else if (window.innerWidth < 768) {
        setVisibleItems(2);
      } else if (window.innerWidth < 1024) {
        setVisibleItems(3);
      } else {
        setVisibleItems(4);
      }
    };

    handleResize();
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  // Navigation functions
  const goToPrevious = () => {
    if (isAnimating || currentIndex === 0) return;

    setIsAnimating(true);
    setCurrentIndex((prev) => Math.max(prev - 1, 0));

    setTimeout(() => {
      setIsAnimating(false);
    }, 500);
  };

  const goToNext = () => {
    if (isAnimating || currentIndex >= products.length - visibleItems) return;

    setIsAnimating(true);
    setCurrentIndex((prev) =>
      Math.min(prev + 1, products.length - visibleItems)
    );

    setTimeout(() => {
      setIsAnimating(false);
    }, 500);
  };

  const canGoBack = currentIndex > 0;
  const canGoForward = currentIndex < products.length - visibleItems;

  // Handle product card actions
  const handleAddToCart = (productId) => {
    console.log(`Added product ${productId} to cart`);
  };

  const handleAddToFavorite = (productId) => {
    console.log(`Added product ${productId} to favorites`);
  };

  return (
    <section className="py-12">
      <div className="container mx-auto px-4">
        <div className="flex items-center justify-between mb-8">
          <h2 className="text-3xl font-bold text-gray-900">Related Products</h2>

          <div className="flex space-x-2">
            <button
              onClick={goToPrevious}
              disabled={!canGoBack || isAnimating}
              className={`p-2 rounded-full border ${
                canGoBack && !isAnimating
                  ? "border-gray-300 text-gray-700 hover:bg-gray-100"
                  : "border-gray-200 text-gray-300 cursor-not-allowed"
              }`}
              aria-label="Previous products"
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
              aria-label="Next products"
            >
              <ChevronRight size={20} />
            </button>
          </div>
        </div>

        {/* Products slider */}
        <div className="relative overflow-hidden">
          <div
            className="flex transition-transform duration-500 ease-in-out"
            style={{
              transform: `translateX(-${currentIndex * (100 / visibleItems)}%)`,
              width: `${(products.length / visibleItems) * 100}%`,
            }}
          >
            {products.map((product, index) => (
              <div
                key={product.id}
                className="px-3"
                style={{ width: `${100 / products.length}%` }}
              >
                <ProductCard2
                  id={product.id.toString()}
                  name={product.name}
                  originalPrice={product.originalPrice}
                  currentPrice={product.currentPrice}
                  discount={product.discount}
                  rating={product.rating}
                  reviewCount={product.reviewCount}
                  imageUrl={product.imageUrl}
                  onAddToCart={(e) => {
                    e.stopPropagation();
                    handleAddToCart(product.id);
                  }}
                  onAddToFavorite={(e) => {
                    e.stopPropagation();
                    handleAddToFavorite(product.id);
                  }}
                />
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
};

export default RelatedProducts;
