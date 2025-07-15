import React from 'react';
import { loadStripe } from '@stripe/stripe-js';
import { Elements } from '@stripe/react-stripe-js';

// Remplacez par votre clé publique Stripe
const stripePromise = loadStripe('pk_test_51RNtyS5vc4PVvLljX2swOptAdyxY3Ywl27Y4AWuMFKo99YXpSqMeswHlw40R2zaGPSzVMebf8lvZBDc9Qzp1GMry00ThgKTCRz');

interface StripeProviderProps {
  children: React.ReactNode;
}

const StripeProvider: React.FC<StripeProviderProps> = ({ children }) => {
  return (
    <Elements stripe={stripePromise}>
      {children}
    </Elements>
  );
};

export default StripeProvider;