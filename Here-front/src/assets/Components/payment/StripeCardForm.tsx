import React, { useState } from 'react';
import { CardElement, useStripe, useElements } from '@stripe/react-stripe-js';
import { paymentService } from '../../services/api.service';

interface StripeCardFormProps {
  amount: number;
  orderId: string;
  onSuccess: (paymentId: string) => void;
  onError: (error: string) => void;
}

const StripeCardForm: React.FC<StripeCardFormProps> = ({ 
  amount, 
  orderId, 
  onSuccess, 
  onError 
}) => {
  const stripe = useStripe();
  const elements = useElements();
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();

    if (!stripe || !elements) {
      return;
    }

    const cardElement = elements.getElement(CardElement);
    if (!cardElement) {
      onError('Impossible de trouver l\'élément de carte');
      return;
    }

    setLoading(true);

    try {
      // 1. Créer un PaymentIntent côté serveur
      const { data } = await paymentService.initStripePayment(amount, orderId);
      const { clientSecret } = data;

      // 2. Confirmer le paiement avec Stripe.js
      const result = await stripe.confirmCardPayment(clientSecret, {
        payment_method: {
          card: cardElement,
        }
      });

      if (result.error) {
        onError(result.error.message || 'Une erreur est survenue lors du paiement');
      } else if (result.paymentIntent?.status === 'succeeded') {
        onSuccess(result.paymentIntent.id);
      }
    } catch (error) {
      onError('Erreur lors de la communication avec le serveur');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <div className="mb-4">
        <CardElement 
          options={{
            style: {
              base: {
                fontSize: '16px',
                color: '#424770',
                '::placeholder': {
                  color: '#aab7c4',
                },
              },
              invalid: {
                color: '#9e2146',
              },
            },
          }}
        />
      </div>
      <button 
        type="submit" 
        disabled={!stripe || loading}
        className={`w-full bg-teal-500 hover:bg-teal-600 text-white py-3 rounded-lg font-medium ${loading ? 'opacity-70 cursor-not-allowed' : ''}`}
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

export default StripeCardForm;