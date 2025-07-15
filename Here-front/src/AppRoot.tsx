import React, { useState, useEffect } from "react";
import App from "./App";

// Multi-Role Marketplace Preloader
const MarketplacePreloader = () => {
  return (
    <div className="preloader-container">
      <div className="preloader-content">
        {/* Main platform logo */}
        <div className="logo-container">
          <div className="logo-text">HERE</div>
          <div className="logo-shadow"></div>
        </div>

        {/* 3D city marketplace scene */}
        <div className="marketplace-scene">
          {/* Seller (Vendeur) section */}
          <div className="seller-section">
            <div className="store-building">
              <div className="store-window"></div>
              <div className="store-door"></div>
              <div className="store-sign">
                <span className="sign-text">VENDEUR</span>
              </div>

              {/* Products coming out of the store */}
              <div className="product-stream">
                <div className="product-item p1"></div>
                <div className="product-item p2"></div>
                <div className="product-item p3"></div>
                <div className="product-item p4"></div>
              </div>
            </div>
          </div>

          {/* Delivery (Livreur) section */}
          <div className="delivery-section">
            <div className="delivery-person">
              <div className="delivery-head"></div>
              <div className="delivery-body"></div>
              <div className="delivery-package"></div>
            </div>

            {/* Delivery vehicle */}
            <div className="delivery-vehicle">
              <div className="vehicle-body"></div>
              <div className="vehicle-wheel wheel-front"></div>
              <div className="vehicle-wheel wheel-back"></div>
            </div>

            {/* Delivery path */}
            <div className="delivery-path">
              <div className="path-dot d1"></div>
              <div className="path-dot d2"></div>
              <div className="path-dot d3"></div>
              <div className="path-dot d4"></div>
              <div className="path-dot d5"></div>
            </div>
          </div>

          {/* Client section */}
          <div className="client-section">
            <div className="client-house">
              <div className="house-body"></div>
              <div className="house-roof"></div>
              <div className="house-window"></div>
              <div className="house-door"></div>
            </div>

            {/* Client receiving package */}
            <div className="client-figure">
              <div className="client-head"></div>
              <div className="client-body"></div>
              <div className="client-arm"></div>
            </div>
          </div>

          {/* Connection lines between sections */}
          <div className="connection-lines">
            <div className="line line-1"></div>
            <div className="line line-2"></div>
            <div className="pathway"></div>
          </div>
        </div>

        {/* Role indicators */}
        <div className="role-indicators">
          <div className="role-badge vendeur">
            <span className="role-icon">🏪</span>
            <span className="role-name">Vendeur</span>
          </div>
          <div className="role-badge livreur">
            <span className="role-icon">🚚</span>
            <span className="role-name">Livreur</span>
          </div>
          <div className="role-badge client">
            <span className="role-icon">👤</span>
            <span className="role-name">Client</span>
          </div>
        </div>

        {/* Loading bar */}
        <div className="loading-container">
          <div className="loading-message">Connecting the marketplace</div>
          <div className="loading-progress-container">
            <div className="loading-progress-bar"></div>
          </div>
        </div>
      </div>

      {/* Inline styles */}
      <style jsx>{`
        .preloader-container {
          position: fixed;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          display: flex;
          align-items: center;
          justify-content: center;
          background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
          z-index: 9999;
          overflow: hidden;
          font-family: system-ui, -apple-system, BlinkMacSystemFont, sans-serif;
        }

        .preloader-content {
          position: relative;
          width: 100%;
          max-width: 600px;
          display: flex;
          flex-direction: column;
          align-items: center;
          padding: 20px;
        }

        /* Logo styling */
        .logo-container {
          position: relative;
          margin-bottom: 20px;
          perspective: 1000px;
        }

        .logo-text {
          font-size: 3.5rem;
          font-weight: 900;
          color: #0d9488;
          text-shadow: 0 4px 8px rgba(13, 148, 136, 0.2);
          letter-spacing: 0.2em;
          transform-style: preserve-3d;
          animation: logo-float 3s ease-in-out infinite;
        }

        .logo-shadow {
          position: absolute;
          bottom: -10px;
          left: 50%;
          transform: translateX(-50%);
          width: 80%;
          height: 10px;
          background: radial-gradient(
            ellipse at center,
            rgba(0, 0, 0, 0.2) 0%,
            rgba(0, 0, 0, 0) 80%
          );
          border-radius: 50%;
          animation: shadow-pulse 3s ease-in-out infinite;
        }

        @keyframes logo-float {
          0%,
          100% {
            transform: translateY(0) rotateX(0deg);
          }
          50% {
            transform: translateY(-10px) rotateX(5deg);
          }
        }

        @keyframes shadow-pulse {
          0%,
          100% {
            opacity: 0.3;
            transform: translateX(-50%) scale(1);
          }
          50% {
            opacity: 0.1;
            transform: translateX(-50%) scale(0.8);
          }
        }

        /* Marketplace scene */
        .marketplace-scene {
          position: relative;
          width: 100%;
          height: 220px;
          margin-bottom: 30px;
          perspective: 1200px;
        }

        /* Seller section */
        .seller-section {
          position: absolute;
          top: 30px;
          left: 30px;
          transform-style: preserve-3d;
          animation: section-bounce 3s ease-in-out infinite;
          animation-delay: 0.2s;
        }

        .store-building {
          width: 120px;
          height: 100px;
          background-color: #0d9488;
          border-radius: 8px;
          position: relative;
          box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
        }

        .store-window {
          position: absolute;
          top: 20px;
          left: 15px;
          width: 40px;
          height: 30px;
          background-color: rgba(255, 255, 255, 0.9);
          border: 2px solid rgba(0, 0, 0, 0.1);
          border-radius: 4px;
        }

        .store-door {
          position: absolute;
          bottom: 0;
          right: 30px;
          width: 30px;
          height: 50px;
          background-color: rgba(255, 255, 255, 0.9);
          border-top-left-radius: 4px;
          border-top-right-radius: 4px;
        }

        .store-sign {
          position: absolute;
          top: -20px;
          left: 10px;
          width: 100px;
          height: 20px;
          background-color: #064e3b;
          color: white;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 0.7rem;
          border-radius: 4px;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }

        .product-stream {
          position: absolute;
          bottom: 20px;
          right: -30px;
          width: 100px;
          height: 30px;
        }

        .product-item {
          position: absolute;
          width: 15px;
          height: 15px;
          border-radius: 4px;
          animation: product-move 3s linear infinite;
        }

        .p1 {
          background-color: #ef4444;
          animation-delay: 0s;
        }

        .p2 {
          background-color: #3b82f6;
          animation-delay: 0.8s;
        }

        .p3 {
          background-color: #f59e0b;
          animation-delay: 1.6s;
        }

        .p4 {
          background-color: #8b5cf6;
          animation-delay: 2.4s;
        }

        @keyframes product-move {
          0% {
            opacity: 0;
            transform: translateX(0);
          }
          20% {
            opacity: 1;
          }
          80% {
            opacity: 1;
          }
          100% {
            opacity: 0;
            transform: translateX(100px);
          }
        }

        /* Delivery section */
        .delivery-section {
          position: absolute;
          top: 100px;
          left: 50%;
          transform: translateX(-50%);
          transform-style: preserve-3d;
          animation: section-bounce 3s ease-in-out infinite;
        }

        .delivery-person {
          position: absolute;
          top: -40px;
          left: 0;
          transform: scale(0.6);
        }

        .delivery-head {
          width: 25px;
          height: 25px;
          background-color: #fcd34d;
          border-radius: 50%;
          position: absolute;
          top: 0;
          left: 0;
        }

        .delivery-body {
          width: 30px;
          height: 40px;
          background-color: #0d9488;
          border-radius: 5px;
          position: absolute;
          top: 20px;
          left: -2px;
        }

        .delivery-package {
          width: 20px;
          height: 15px;
          background-color: #f59e0b;
          position: absolute;
          top: 30px;
          right: -15px;
          border-radius: 2px;
          animation: package-bounce 1s ease-in-out infinite;
        }

        @keyframes package-bounce {
          0%,
          100% {
            transform: translateY(0);
          }
          50% {
            transform: translateY(-3px);
          }
        }

        .delivery-vehicle {
          width: 100px;
          height: 50px;
          position: relative;
          animation: vehicle-move 4s linear infinite;
        }

        .vehicle-body {
          width: 100%;
          height: 35px;
          background-color: #0284c7;
          border-radius: 8px;
          position: absolute;
          bottom: 15px;
        }

        .vehicle-wheel {
          width: 15px;
          height: 15px;
          background-color: #1e293b;
          border-radius: 50%;
          position: absolute;
          bottom: 0;
          border: 3px solid #94a3b8;
          animation: wheel-spin 1s linear infinite;
        }

        .wheel-front {
          right: 15px;
        }

        .wheel-back {
          left: 15px;
        }

        @keyframes wheel-spin {
          0% {
            transform: rotate(0deg);
          }
          100% {
            transform: rotate(360deg);
          }
        }

        @keyframes vehicle-move {
          0% {
            transform: translateX(-50px);
          }
          50% {
            transform: translateX(50px);
          }
          100% {
            transform: translateX(-50px);
          }
        }

        .delivery-path {
          position: absolute;
          bottom: -10px;
          left: -50px;
          width: 200px;
          height: 2px;
          background-color: #cbd5e1;
        }

        .path-dot {
          position: absolute;
          width: 6px;
          height: 6px;
          background-color: #0d9488;
          border-radius: 50%;
          top: -2px;
        }

        .d1 {
          left: 0%;
        }
        .d2 {
          left: 25%;
        }
        .d3 {
          left: 50%;
        }
        .d4 {
          left: 75%;
        }
        .d5 {
          left: 100%;
        }

        /* Client section */
        .client-section {
          position: absolute;
          top: 30px;
          right: 30px;
          transform-style: preserve-3d;
          animation: section-bounce 3s ease-in-out infinite;
          animation-delay: 0.4s;
        }

        .client-house {
          width: 120px;
          height: 100px;
          position: relative;
        }

        .house-body {
          width: 100%;
          height: 70px;
          background-color: #f1f5f9;
          border-radius: 8px;
          position: absolute;
          bottom: 0;
          box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
        }

        .house-roof {
          width: 140px;
          height: 50px;
          background-color: #f97316;
          position: absolute;
          top: -20px;
          left: -10px;
          clip-path: polygon(50% 0%, 0% 100%, 100% 100%);
        }

        .house-window {
          width: 30px;
          height: 30px;
          background-color: #bae6fd;
          position: absolute;
          top: 20px;
          left: 20px;
          border-radius: 4px;
          border: 2px solid white;
        }

        .house-door {
          width: 25px;
          height: 40px;
          background-color: #0d9488;
          position: absolute;
          bottom: 0;
          right: 25px;
          border-top-left-radius: 4px;
          border-top-right-radius: 4px;
        }

        .client-figure {
          position: absolute;
          bottom: 0;
          left: -20px;
          transform: scale(0.6);
        }

        .client-head {
          width: 25px;
          height: 25px;
          background-color: #fcd34d;
          border-radius: 50%;
          position: absolute;
          top: 0;
          left: 0;
        }

        .client-body {
          width: 30px;
          height: 40px;
          background-color: #64748b;
          border-radius: 5px;
          position: absolute;
          top: 20px;
          left: -2px;
        }

        .client-arm {
          width: 20px;
          height: 8px;
          background-color: #64748b;
          position: absolute;
          top: 30px;
          left: -15px;
          animation: arm-wave 2s ease-in-out infinite;
          transform-origin: right center;
        }

        @keyframes arm-wave {
          0%,
          100% {
            transform: rotate(0deg);
          }
          50% {
            transform: rotate(-30deg);
          }
        }

        /* Connection lines */
        .connection-lines {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          z-index: -1;
        }

        .line {
          position: absolute;
          height: 2px;
          background-color: #cbd5e1;
        }

        .line-1 {
          top: 80px;
          left: 150px;
          width: 120px;
          transform-origin: left center;
          transform: rotate(15deg);
        }

        .line-2 {
          top: 80px;
          right: 150px;
          width: 120px;
          transform-origin: right center;
          transform: rotate(-15deg);
        }

        .pathway {
          position: absolute;
          top: 130px;
          left: 150px;
          width: 300px;
          height: 8px;
          background-color: #e2e8f0;
          border-radius: 4px;
        }

        @keyframes section-bounce {
          0%,
          100% {
            transform: translateY(0);
          }
          50% {
            transform: translateY(-10px);
          }
        }

        /* Role indicators */
        .role-indicators {
          display: flex;
          justify-content: space-around;
          width: 100%;
          margin-bottom: 20px;
        }

        .role-badge {
          display: flex;
          flex-direction: column;
          align-items: center;
          background-color: white;
          padding: 10px 15px;
          border-radius: 10px;
          box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);
          transform: translateY(0);
          transition: transform 0.3s ease;
        }

        .role-badge:hover {
          transform: translateY(-5px);
        }

        .role-icon {
          font-size: 1.5rem;
          margin-bottom: 5px;
        }

        .role-name {
          font-size: 0.8rem;
          font-weight: 600;
          color: #334155;
        }

        /* Loading UI */
        .loading-container {
          margin-top: 10px;
          width: 100%;
          max-width: 300px;
          text-align: center;
        }

        .loading-message {
          font-size: 0.9rem;
          color: #334155;
          margin-bottom: 8px;
        }

        .loading-progress-container {
          width: 100%;
          height: 6px;
          background-color: #e2e8f0;
          border-radius: 3px;
          overflow: hidden;
        }

        .loading-progress-bar {
          height: 100%;
          width: 0%;
          background-color: #0d9488;
          border-radius: 3px;
          animation: progress 3s ease-in-out infinite;
        }

        @keyframes progress {
          0% {
            width: 0%;
          }
          50% {
            width: 70%;
          }
          75% {
            width: 85%;
          }
          100% {
            width: 100%;
          }
        }
      `}</style>
    </div>
  );
};

// Main AppRoot component
const AppRoot = () => {
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Simulate loading time or actual resource loading
    const timer = setTimeout(() => {
      setIsLoading(false);
    }, 3000); // 3 seconds to show off the animation

    return () => clearTimeout(timer);
  }, []);

  // Show preloader while loading
  if (isLoading) {
    return <MarketplacePreloader />;
  }

  // Show actual app after loading
  return <App />;
};

export default AppRoot;
