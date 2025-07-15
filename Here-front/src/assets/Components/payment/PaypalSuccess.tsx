import React, { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';

import { CheckCircle, XCircle } from 'lucide-react';

const PaypalSuccess: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [loading, setLoading] = useState(true);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const confirmPayment = async () => {
      try {
        // Extraire les paramètres de l'URL
        const params = new URLSearchParams(location.search);
        const paymentId = params.get('paymentId');
        const payerId = params.get('PayerID');
        
        if (!paymentId || !payerId) {
          setError('Paramètres de paiement manquants');
          setLoading(false);
          return;
        }
        
        // Confirmer le paiement
        const response = await paymentService.confirmPaypalPayment(paymentId, payerId);
        
        if (response.data.success) {
          setSuccess(true);
          // Rediriger vers la page de confirmation après 3 secondes
          setTimeout(() => {
            navigate('/checkout/confirmation');
          }, 3000);
        } else {
          setError(response.data.message || 'Erreur lors de la confirmation du paiement');
        }
      } catch (error) {
        console.error('Erreur:', error);
        setError('Une erreur est survenue lors de la confirmation du paiement');
      } finally {
        setLoading(false);
      }
    };
    
    confirmPayment();
  }, [location, navigate]);

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-lg shadow-md p-8 max-w-md w-full text-center">
        {loading ? (
          <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-teal-500 mx-auto"></div>
        ) : success ? (
          <>
            <CheckCircle className="h-16 w-16 text-teal-500 mx-auto mb-4" />
            <h2 className="text-2xl font-bold mb-2 text-teal-600">Paiement réussi !</h2>
            <p className="text-gray-600 mb-4">
              Votre paiement a été confirmé et votre commande est en cours de traitement.
            </p>
            <p className="text-sm text-gray-500">
              Vous allez être redirigé vers la page de confirmation...
            </p>
          </>
        ) : (
          <>
            <XCircle className="h-16 w-16 text-red-500 mx-auto mb-4" />
            <h2 className="text-2xl font-bold mb-2 text-red-600">Échec du paiement</h2>
            <p className="text-gray-600 mb-4">{error}</p>
            <button
              onClick={() => navigate('/checkout')}
              className="bg-teal-500 hover:bg-teal-600 text-white py-2 px-4 rounded-md transition-colors"
            >
              Retour au paiement
            </button>
          </>
        )}
      </div>
    </div>
  );
};

export default PaypalSuccess;