// src/components/CustomerReviewsSlider.jsx
import React, { useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";

const CustomerReviewsSlider = ({ reviews = [] }) => {
  const [currentIndex, setCurrentIndex] = useState(0);

  // Go to previous review
  const goToPrevious = () => {
    setCurrentIndex((prev) => (prev === 0 ? reviews.length - 1 : prev - 1));
  };

  // Go to next review
  const goToNext = () => {
    setCurrentIndex((prev) => (prev === reviews.length - 1 ? 0 : prev + 1));
  };

  return (
    <section className="py-12 mx-6">
      <div className="container mx-auto px-4">
        <div className="bg-[#F6F7FB] rounded-[64px] p-12 flex flex-wrap">
          {/* Left Column - Title and Navigation */}
          <div className="w-full pr-4 md:w-1/2 md:pr-10 mb-8 md:mb-0">
            <h2 className="text-3xl font-bold text-gray-900 mb-24">
              What our Customers
              <br /> are Saying
            </h2>

            {/* Navigation Buttons */}
            <div className="flex space-x-4">
              <button
                onClick={goToPrevious}
                className="w-12 h-12 flex items-center justify-center rounded-full border border-gray-300"
                aria-label="Previous review"
              >
                <ChevronLeft size={18} />
              </button>
              <button
                onClick={goToNext}
                className="w-12 h-12 flex items-center justify-center rounded-full border border-gray-300"
                aria-label="Next review"
              >
                <ChevronRight size={18} />
              </button>
            </div>
          </div>

          {/* Right Column - Reviews */}
          <div className="w-full md:w-1/2 flex flex-col">
            {reviews.map((review, index) => (
              <div
                key={index}
                className={`transition-opacity duration-300 ${
                  index === currentIndex
                    ? "opacity-100 block"
                    : "opacity-0 hidden"
                }`}
              >
                {/* Avatar */}
                <div className="w-20 h-20 rounded-full border-2 border-teal-500 overflow-hidden mb-4 ml-auto mr-auto">
                  {review.avatar ? (
                    <img
                      src={review.avatar}
                      alt={review.name}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="w-full h-full bg-teal-100 flex items-center justify-center text-teal-500 text-xl font-bold">
                      {review.name?.charAt(0)}
                    </div>
                  )}
                </div>

                <div className="text-center">
                  {/* Customer Name */}
                  <h3 className="font-medium text-lg mb-4">{review.name}</h3>

                  {/* Review Text */}
                  <p className="text-gray-700 mb-6">{review.comment}</p>

                  {/* Pagination Dots */}
                  <div className="flex justify-center space-x-2">
                    {reviews.map((_, dotIndex) => (
                      <span
                        key={dotIndex}
                        className={`inline-block ${
                          dotIndex === currentIndex
                            ? "w-6 border border-teal-500"
                            : "w-2 bg-teal-500"
                        } h-2 rounded-full`}
                      />
                    ))}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
};

export default CustomerReviewsSlider;
