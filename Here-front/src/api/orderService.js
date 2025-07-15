// src/api/orderService.js
import axios from 'axios';
import apiClient, { API_BASE_URL } from './apiConfig';

/**
 * Service pour gérer les opérations liées aux commandes
 */
export const orderService = {
  /**
   * Récupérer les détails de suivi d'une commande par son numéro
   * @param {string} orderNumber - Le numéro de commande à suivre
   * @returns {Promise} - Promesse résolue avec les données de suivi
   */
  async trackOrder(orderNumber) {
    try {
      const response = await apiClient.get(`/api/orders/track/${orderNumber}`);
      return response.data;
    } catch (error) {
      console.error('Erreur lors du suivi de la commande:', error);
      throw error;
    }
  },

  /**
   * Récupérer toutes les commandes d'un utilisateur
   * @returns {Promise} - Promesse résolue avec les commandes de l'utilisateur
   */
  async getUserOrders() {
    try {
      const response = await apiClient.get('/api/orders/user');
      return response.data;
    } catch (error) {
      console.error('Erreur lors de la récupération des commandes:', error);
      throw error;
    }
  },

  /**
   * Récupérer les détails d'une commande par son ID
   * @param {string} orderId - L'ID de la commande
   * @returns {Promise} - Promesse résolue avec les détails de la commande
   */
  async getOrderDetails(orderId) {
    try {
      const response = await apiClient.get(`/api/orders/${orderId}`);
      return response.data;
    } catch (error) {
      console.error('Erreur lors de la récupération des détails de la commande:', error);
      throw error;
    }
  }
};

export default orderService;