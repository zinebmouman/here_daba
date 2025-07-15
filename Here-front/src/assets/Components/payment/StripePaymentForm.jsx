import React, { useState, useEffect } from 'react';
import { CardElement, useStripe, useElements } from '@stripe/react-stripe-js';
import { auth } from '../../config/Firebase';
import { Lock } from 'lucide-react';
import { createStripePaymentIntent } from '../../utils/shopUtils';

// Styles pour le composant CardElement
const CARD_ELEMENT_OPTIONS = {
  style: {
    base: {
      fontSize: '16px',
      color: '#424770',
      fontFamily: 'Arial, sans-serif',
      '::placeholder': {
        color: '#aab7c4',
      },
    },
    invalid: {
      color: '#9e2146',
      iconColor: '#9e2146',
    },
  },
  hidePostalCode: true,
};

const StripePaymentForm = ({ amount, orderData, onSuccess, onError }) => {
  const stripe = useStripe();
  const elements = useElements();
  const [loading, setLoading] = useState(false);
  const [cardError, setCardError] = useState('');
  const [clientSecret, setClientSecret] = useState('');

  // Créer un PaymentIntent lors du chargement du composant
  useEffect(() => {
    const initPaymentIntent = async () => {
      try {
        if (!amount || amount <= 0) return;
        
        // Utiliser la fonction depuis shopUtils pour créer l'intent de paiement
        const response = await createStripePaymentIntent(amount, {
          orderId: orderData.orderNumber || 'pending'
        });
        
        setClientSecret(response.clientSecret);
      } catch (error) {
        console.error('Erreur lors de la création du PaymentIntent:', error);
        onError('Impossible d\'initialiser le paiement. Veuillez réessayer.');
      }
    };

    if (amount > 0) {
      initPaymentIntent();
    }
  }, [amount, orderData.orderNumber, onError]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    
    if (!stripe || !elements || !clientSecret) {
      return;
    }
    
    setLoading(true);
    setCardError('');
    
    try {
      const cardElement = elements.getElement(CardElement);
      
      // Confirmer le paiement avec Stripe.js
      const { error, paymentIntent } = await stripe.confirmCardPayment(clientSecret, {
        payment_method: {
          card: cardElement,
          billing_details: {
            name: orderData.shipping.fullName,
            email: orderData.shipping.email,
            address: {
              line1: orderData.shipping.address,
              city: orderData.shipping.city,
              postal_code: orderData.shipping.postalCode,
              country: orderData.shipping.country
            }
          }
        }
      });
      
      if (error) {
        setCardError(error.message);
        onError(error.message);
      } else if (paymentIntent.status === 'succeeded') {
        // Le paiement a réussi, informer le parent
        onSuccess({
          paymentId: paymentIntent.id,
          orderNumber: orderData.orderNumber,
          status: 'completed'
        });
      } else {
        // Le paiement est dans un autre état (requires_action, processing, etc.)
        onError(`Le paiement est dans l'état: ${paymentIntent.status}. Veuillez vérifier votre compte bancaire.`);
      }
    } catch (err) {
      console.error('Erreur lors du traitement du paiement:', err);
      onError('Une erreur inattendue s'est produite lors du traitement du paiement.');
    } finally {
      setLoading(false);
    }
  };

  const handleCardChange = (event) => {
    // Effacer les messages d'erreur lorsque l'utilisateur modifie les détails de carte
    if (event.error) {
      setCardError(event.error.message);
    } else {
      setCardError('');
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="bg-white p-4 rounded-md border border-gray-200">
        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Informations de carte
          </label>
          <CardElement 
            options={CARD_ELEMENT_OPTIONS} 
            onChange={handleCardChange}
            className="p-3 border border-gray-300 rounded-md"
          />
        </div>
        
        {cardError && (
          <div className="text-red-500 text-sm my-2">
            {cardError}
          </div>
        )}
        
        <div className="flex items-center text-gray-600 text-sm mt-4">
          <Lock className="h-4 w-4 text-gray-500 mr-2" />
          <span>Toutes les informations de paiement sont sécurisées et cryptées</span>
        </div>
      </div>
      
      <button
        type="submit"
        disabled={!stripe || !clientSecret || loading}
        className={`w-full bg-teal-500 hover:bg-teal-600 text-white py-3 rounded-lg font-medium transition-colors ${
          (!stripe || !clientSecret || loading) ? 'opacity-70 cursor-not-allowed' : ''
        }`}
      >
        {loading ? (
          <div className="flex items-center justify-center">
            <div className="animate-spin h-5 w-5 border-2 border-white border-r-transparent rounded-full mr-2"></div>
            <span>Traitement en cours...</span>
          </div>
        ) : (
          'Payer maintenant'
        )}
      </button>
    </form>
  );
};

export default StripePaymentForm;