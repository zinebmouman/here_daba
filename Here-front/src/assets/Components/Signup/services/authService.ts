import axios from 'axios';

// Configuration de base - à adapter selon votre environnement
const API_BASE_URL = 'http://localhost:8080'; // URL de l'API Gateway

// Créer une instance axios pour les requêtes API
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000, // Augmentation du timeout à 15 secondes
  headers: {
    'Content-Type': 'application/json'
  }
});

/**
 * Synchronise un utilisateur avec PostgreSQL via l'API
 */
export const syncUserWithPostgre = async (token: string, userData: any) => {
  console.log("🔄 Début de la synchronisation avec PostgreSQL", userData);
  
  // Vérification optionnelle de la connexion réseau
  if (!navigator.onLine) {
    console.error("❌ Pas de connexion internet");
    return { 
      error: true, 
      message: "Pas de connexion internet" 
    };
  }

  try {
    // Validation et normalisation des données
    const syncData = {
      uid: userData.uid,
      email: userData.email,
      displayName: userData.displayName || 
        `${userData.firstName || ''} ${userData.lastName || ''}`.trim(),
      role: userData.role || "client", // Rôle par défaut
      firstName: userData.firstName,
      lastName: userData.lastName,
      phone: userData.phone || '',
      createdAt: userData.createdAt || new Date().toISOString()
    };

    console.log("📤 Données envoyées pour synchronisation:", syncData);
    
    const response = await apiClient.post('/api/users/sync/sync-new-user', syncData, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    console.log("✅ Synchronisation réussie:", response.data);
    return response.data;
  } catch (error: any) {
    console.error("❌ Erreur lors de la synchronisation avec PostgreSQL:", error);
    
    // Gestion détaillée des erreurs
    if (axios.isAxiosError(error)) {
      // Gestion spécifique des erreurs réseau
      if (error.code === 'ERR_NETWORK') {
        return { 
          error: true, 
          message: "Erreur de connexion réseau. Veuillez réessayer." 
        };
      }

      console.error("❌ Détails de l'erreur:", {
        status: error.response?.status,
        data: error.response?.data,
        headers: error.response?.headers
      });
    }
    
    return { 
      error: true, 
      message: error.response?.data?.message || error.message || 'Erreur de synchronisation'
    };
  }
};

/**
 * Vérifie le token Firebase auprès du backend
 */
export const verifyTokenWithBackend = async (token: string): Promise<boolean> => {
  console.log("🔒 Envoi du token pour vérification...");
  
  try {
    const response = await apiClient.post('/api/auth/verify', token, {
      headers: { 'Content-Type': 'text/plain' }
    });
    
    // Si l'utilisateur n'est pas un vendeur, ça retourne 403 - ce n'est pas une erreur technique
    if (response.status === 403) {
      console.log("🚫 L'utilisateur n'est pas un vendeur");
      return false;
    }
    
    console.log("✅ Vérification du token réussie");
    return true;
  } catch (error) {
    // Gérer spécifiquement l'accès refusé (403)
    if (axios.isAxiosError(error) && error.response?.status === 403) {
      console.log("🚫 L'utilisateur n'est pas un vendeur");
      return false;
    }
    
    console.error("❌ Erreur technique de vérification du token:", error);
    throw error;
  }
};

/**
 * Vérifie le rôle de l'utilisateur dans Firestore
 */
export const checkUserRole = async (token: string): Promise<string> => {
  try {
    const response = await apiClient.post('/api/auth/check-firestore-role', token, {
      headers: { 'Content-Type': 'text/plain' }
    });
    
    if (!response.data) {
      return "client"; // Par défaut
    }
    
    const responseText = response.data;
    if (typeof responseText === 'string') {
      if (responseText.includes("est vendeur: true")) {
        return "vendeur";
      } else if (responseText.includes("est livreur: true")) {
        return "livreur";
      }
    }
    
    return "client";
  } catch (error) {
    console.error("❌ Erreur lors de la vérification du rôle:", error);
    return "client"; // Par défaut en cas d'erreur
  }
};

/**
 * Fonction de test pour vérifier la connectivité avec l'API
 */
export const updateUserRole = async (uid: string, newRole: string) => {
  try {
    // Utiliser le proxy configuré dans Vite
    const response = await axios.post('/api/users/update/role', null, {
      params: {
        uid: uid,
        newRole: newRole
      }
    });
    console.log("✅ Rôle mis à jour avec succès:", response.data);
    return response.data;
  } catch (error) {
    console.error("❌ Erreur lors de la mise à jour du rôle:", error);
    // Ajouter plus de détails sur l'erreur pour faciliter le débogage
    if (axios.isAxiosError(error)) {
      console.error("Détails:", {
        status: error.response?.status,
        data: error.response?.data,
        config: error.config?.url
      });
    }
    throw error; // Il est important de propager l'erreur pour la gérer dans le composant
  }
};

