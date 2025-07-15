// src/api/cartService.ts
import { apiClient } from './apiConfig';

export interface CartItemDTO {
  id?: number;
  nomProduit: string;
  prix: number;
  quantite: number;
  imageUrl?: string;
  categorie?: string;
  productId: number;
}

export interface CartResponseDTO {
  userId: string;
  items: CartItemDTO[];
  subtotal: number;
}

const cartService = {
  async getCart(userId: string): Promise<CartResponseDTO> {
    try {
      console.log(`Récupération du panier pour l'utilisateur: ${userId}`);
      const response = await apiClient.get(`/api/cart/${userId}`);
      console.log('Réponse de l\'API getCart:', response.data);
      return response.data;
    } catch (error) {
      console.error('Erreur lors de la récupération du panier:', error);
      throw error;
    }
  },

  async addItem(userId: string, item: CartItemDTO): Promise<CartItemDTO> {
    try {
      console.log(`Ajout d'un article au panier pour l'utilisateur: ${userId}`, item);
      const response = await apiClient.post(`/api/cart/${userId}/items`, item);
      console.log('Réponse de l\'API addItem:', response.data);
      return response.data;
    } catch (error) {
      console.error('Erreur lors de l\'ajout au panier:', error);
      throw error;
    }
  },

  async updateItem(userId: string, itemId: number, item: CartItemDTO): Promise<CartItemDTO> {
    try {
      console.log(`Mise à jour d'un article du panier pour l'utilisateur: ${userId}, itemId: ${itemId}`, item);
      const response = await apiClient.put(`/api/cart/${userId}/items/${itemId}`, item);
      console.log('Réponse de l\'API updateItem:', response.data);
      return response.data;
    } catch (error) {
      console.error('Erreur lors de la mise à jour du panier:', error);
      throw error;
    }
  },

  async removeItem(userId: string, itemId: number): Promise<void> {
    try {
      console.log(`Suppression d'un article du panier pour l'utilisateur: ${userId}, itemId: ${itemId}`);
      await apiClient.delete(`/api/cart/${userId}/items/${itemId}`);
      console.log('Article supprimé avec succès');
    } catch (error) {
      console.error('Erreur lors de la suppression de l\'article:', error);
      throw error;
    }
  },

  async clearCart(userId: string): Promise<void> {
    try {
      console.log(`Vidage du panier pour l'utilisateur: ${userId}`);
      await apiClient.delete(`/api/cart/${userId}`);
      console.log('Panier vidé avec succès');
    } catch (error) {
      console.error('Erreur lors du vidage du panier:', error);
      throw error;
    }
  },

  async prepareCheckout(userId: string): Promise<CartResponseDTO> {
    try {
      console.log(`Préparation du checkout pour l'utilisateur: ${userId}`);
      const response = await apiClient.post(`/api/cart/${userId}/checkout`);
      console.log('Réponse de l\'API prepareCheckout:', response.data);
      return response.data;
    } catch (error) {
      console.error('Erreur lors de la préparation du checkout:', error);
      throw error;
    }
  }
};

export default cartService;