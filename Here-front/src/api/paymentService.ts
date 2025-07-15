// src/api/paymentService.ts
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/payments';

export interface PaymentIntent {
  clientSecret: string;
  amount: number;
  currency: string;
}

const paymentService = {
  createPaymentIntent: async (amount: number, orderId: string): Promise<PaymentIntent> => {
    try {
      const response = await axios.post(`${API_BASE_URL}/create-intent`, {
        amount,
        orderId
      });
      return response.data;
    } catch (error) {
      console.error('Erreur lors de la création du PaymentIntent:', error);
      throw error;
    }
  },

  confirmPayment: async (paymentIntentId: string, orderId: string): Promise<any> => {
    try {
      const response = await axios.post(`${API_BASE_URL}/confirm`, {
        paymentIntentId,
        orderId
      });
      return response.data;
    } catch (error) {
      console.error('Erreur lors de la confirmation du paiement:', error);
      throw error;
    }
  }
};

export default paymentService;