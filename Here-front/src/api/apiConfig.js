// Créez un fichier config.js ou apiConfig.js
export const API_BASE_URL = 'http://localhost:8080';

// Puis utilisez-le dans vos appels Axios
import axios from 'axios';
import { API_BASE_URL } from './config';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true
});

// Exemple d'utilisation
const loadBoutiques = async () => {
  try {
    const response = await apiClient.get('/api/boutiques');
    // Traitement de la réponse
  } catch (error) {
    console.error('Erreur lors du chargement des boutiques:', error);
  }
};