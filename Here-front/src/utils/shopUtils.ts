// src/utils/shopUtils.ts
import { auth } from '../config/Firebase';

export const CART_STORAGE_KEY = 'HERE_SHOPPING_CART';
export const FAVORITES_STORAGE_KEY = 'HERE_FAVORITES';

// Interfaces
export interface CartItem {
  id: number | string;
  nomProduit: string;
  prix: number;
  imageUrl?: string;
  quantite: number;
  categorie?: string;
}

export interface FavoriteItem {
  id: number;
  idProduit: number;
  dateAjout: string;
  produitDetails?: {
    nom: string;
    prix: number;
    imageUrl?: string;
    categorie?: string;
  };
}

export interface Product {
  id: number;
  nomProduit: string;
  description?: string;
  prix: number;
  originalPrice?: number;
  imageUrl?: string;
  categorie?: string;
  quantite?: number;
}

// Normaliser les URLs d'image
export const normalizeImageUrl = (url?: string): string => {
  if (!url) return '/api/fichiers/placeholder.png';
  
  if (!url.startsWith('http') && !url.startsWith('/')) {
    return '/' + url;
  }
  
  return url;
};

// Récupérer l'ID utilisateur
export const getUserId = (): string => {
  const user = auth.currentUser;
  return user ? user.uid : 'anonymous';
};

// Récupérer les articles du panier
export const getCartItems = (userId?: string): CartItem[] => {
  const uid = userId || getUserId();
  const userCartKey = `${CART_STORAGE_KEY}_${uid}`;
  const cartJson = localStorage.getItem(userCartKey);
  return cartJson ? JSON.parse(cartJson) : [];
};

// Mettre à jour la quantité d'un article du panier
export const updateCartItemQuantity = (
  productId: number | string, 
  quantity: number, 
  userId?: string
): boolean => {
  try {
    if (quantity < 1) return false;
    
    const uid = userId || getUserId();
    const userCartKey = `${CART_STORAGE_KEY}_${uid}`;
    
    const cartJson = localStorage.getItem(userCartKey);
    let cart = cartJson ? JSON.parse(cartJson) : [];
    
    const updatedCart = cart.map((item: CartItem) => 
      String(item.id) === String(productId) 
        ? { ...item, quantite: quantity } 
        : item
    );
    
    localStorage.setItem(userCartKey, JSON.stringify(updatedCart));
    
    const cartUpdateEvent = new CustomEvent('cartUpdated', {
      detail: { userId: uid }
    });
    window.dispatchEvent(cartUpdateEvent);
    
    return true;
  } catch (error) {
    console.error('Erreur lors de la mise à jour de la quantité:', error);
    return false;
  }
};

// Supprimer un article du panier
export const removeFromCart = (
  productId: number | string, 
  userId?: string
): boolean => {
  try {
    const uid = userId || getUserId();
    const userCartKey = `${CART_STORAGE_KEY}_${uid}`;
    
    const cartJson = localStorage.getItem(userCartKey);
    let cart = cartJson ? JSON.parse(cartJson) : [];
    
    const updatedCart = cart.filter((item: CartItem) => 
      String(item.id) !== String(productId)
    );
    
    localStorage.setItem(userCartKey, JSON.stringify(updatedCart));
    
    const cartUpdateEvent = new CustomEvent('cartUpdated', {
      detail: { userId: uid }
    });
    window.dispatchEvent(cartUpdateEvent);
    
    return true;
  } catch (error) {
    console.error('Erreur lors de la suppression du panier:', error);
    return false;
  }
};

// Ajouter un produit au panier
export const addToCart = (
  product: any, 
  userId?: string, 
  quantity: number = 1
): boolean => {
  try {
    const uid = userId || getUserId();
    const userCartKey = `${CART_STORAGE_KEY}_${uid}`;
    
    const cartJson = localStorage.getItem(userCartKey);
    let cart = cartJson ? JSON.parse(cartJson) : [];
    
    const cartItem = {
      id: product.id,
      nomProduit: product.nomProduit || product.name,
      prix: product.prix || product.price,
      imageUrl: normalizeImageUrl(product.imageUrl),
      quantite: quantity,
      categorie: product.categorie || product.category
    };
    
    const existingItemIndex = cart.findIndex((item: CartItem) => 
      String(item.id) === String(cartItem.id)
    );
    
    if (existingItemIndex >= 0) {
      cart[existingItemIndex].quantite += quantity;
    } else {
      cart.push(cartItem);
    }
    
    localStorage.setItem(userCartKey, JSON.stringify(cart));
    
    const cartUpdateEvent = new CustomEvent('cartUpdated', {
      detail: { userId: uid }
    });
    window.dispatchEvent(cartUpdateEvent);
    
    return true;
  } catch (error) {
    console.error('Erreur lors de l\'ajout au panier:', error);
    return false;
  }
};

// Récupérer les favoris
export const getFavorites = (userId?: string): FavoriteItem[] => {
  const uid = userId || getUserId();
  const userFavoritesKey = `${FAVORITES_STORAGE_KEY}_${uid}`;
  
  const favoritesJson = localStorage.getItem(userFavoritesKey);
  return favoritesJson ? JSON.parse(favoritesJson) : [];
};

// Vérifier si un produit est dans les favoris
export const isInFavorites = (
  productId: number | string, 
  userId?: string
): boolean => {
  const uid = userId || getUserId();
  const favorites = getFavorites(uid);
  
  return favorites.some((fav: FavoriteItem) => 
    String(fav.idProduit) === String(productId)
  );
};

// Ajouter un produit aux favoris
export const addToFavorites = async (
  product: any, 
  userId?: string
): Promise<boolean> => {
  try {
    const uid = userId || getUserId();
    const userFavoritesKey = `${FAVORITES_STORAGE_KEY}_${uid}`;
    
    const favoritesJson = localStorage.getItem(userFavoritesKey);
    let favorites = favoritesJson ? JSON.parse(favoritesJson) : [];
    
    const existingIndex = favorites.findIndex((fav: FavoriteItem) => 
      String(fav.idProduit) === String(product.id)
    );
    
    if (existingIndex !== -1) {
      return false; // Déjà dans les favoris
    }
    
    const favoriteItem = {
      id: Date.now(),
      idProduit: product.id,
      dateAjout: new Date().toISOString(),
      produitDetails: {
        nom: product.name || product.nomProduit,
        prix: product.price || product.prix,
        imageUrl: normalizeImageUrl(product.imageUrl),
        categorie: product.category || product.categorie
      }
    };
    
    favorites.push(favoriteItem);
    
    localStorage.setItem(userFavoritesKey, JSON.stringify(favorites));
    
    const favoritesUpdateEvent = new CustomEvent('favoritesUpdated', {
      detail: { userId: uid }
    });
    window.dispatchEvent(favoritesUpdateEvent);
    
    return true;
  } catch (error) {
    console.error('Erreur lors de l\'ajout aux favoris:', error);
    return false;
  }
};

// Supprimer un produit des favoris
export const removeFromFavorites = (
  productId: number | string, 
  userId?: string
): boolean => {
  try {
    const uid = userId || getUserId();
    const userFavoritesKey = `${FAVORITES_STORAGE_KEY}_${uid}`;
    
    const favoritesJson = localStorage.getItem(userFavoritesKey);
    let favorites = favoritesJson ? JSON.parse(favoritesJson) : [];
    
    const updatedFavorites = favorites.filter((fav: FavoriteItem) => 
      String(fav.idProduit) !== String(productId)
    );
    
    localStorage.setItem(userFavoritesKey, JSON.stringify(updatedFavorites));
    
    const favoritesUpdateEvent = new CustomEvent('favoritesUpdated', {
      detail: { userId: uid }
    });
    window.dispatchEvent(favoritesUpdateEvent);
    
    return true;
  } catch (error) {
    console.error('Erreur lors de la suppression des favoris:', error);
    return false;
  }
};
// Obtenir le nombre d'articles dans le panier
export const getCartItemCount = (userId?: string): number => {
  try {
    const uid = userId || getUserId();
    const cartItems = getCartItems(uid);
    
    // Compter le nombre total d'articles (avec quantités)
    return cartItems.reduce((total, item) => 
      total + (item.quantite || 1), 0);
  } catch (error) {
    console.error('Erreur lors du comptage des articles du panier:', error);
    return 0;
  }
};