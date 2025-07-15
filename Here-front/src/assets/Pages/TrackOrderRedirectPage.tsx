import React, { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

/**
 * Composant de redirection pour les liens de suivi de commande
 * Ce composant peut être accessible depuis n'importe quelle URL comme /track-order?orderId=ABC123
 * et redirige automatiquement vers /account/track-order avec le même paramètre
 */
const TrackOrderRedirectPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const orderNumberFromUrl = searchParams.get("orderId");

  useEffect(() => {
    // Si un numéro de commande est présent, redirige vers la page de suivi de commande
    if (orderNumberFromUrl) {
      navigate(`/account/track-order?orderId=${orderNumberFromUrl}`);
    } else {
      // Sinon, redirige vers la page de suivi de commande sans paramètre
      navigate('/account/track-order');
    }
  }, [orderNumberFromUrl, navigate]);

  // Affiche un message de chargement pendant la redirection
  return (
    <div className="flex items-center justify-center min-h-screen">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-teal-500 mx-auto mb-4"></div>
        <h2 className="text-xl font-medium text-gray-700">Redirection vers le suivi de commande...</h2>
      </div>
    </div>
  );
};

export default TrackOrderRedirectPage; 