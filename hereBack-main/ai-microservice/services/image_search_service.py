# image_search_service.py - VERSION CORRIGÉE pour ne pas dépendre du nom de fichier

from sqlalchemy.orm import Session
from database.models import Produit, ProduitImage, get_db
from minio import Minio
import shutil
import os
import io
import numpy as np
from PIL import Image
import tensorflow as tf
from sklearn.metrics.pairwise import cosine_similarity
from typing import List, Dict, Optional
import requests
from dotenv import load_dotenv
import cv2
import pickle
from pathlib import Path

load_dotenv()

class ImageSearchService:
    def __init__(self):
        # Configuration MinIO
        self.minio_client = Minio(
            os.getenv("MINIO_ENDPOINT", "localhost:9000"),
            access_key=os.getenv("MINIO_ACCESS_KEY", "minioadmin"),
            secret_key=os.getenv("MINIO_SECRET_KEY", "minioadmin123"),
            secure=os.getenv("MINIO_SECURE", "false").lower() == "true"
        )
        self.bucket_name = os.getenv("MINIO_BUCKET_NAME", "boutique-images")
        self.minio_public_url = os.getenv("MINIO_PUBLIC_URL", "http://localhost:9000")
        
        # Modèle de features extraction (simulation TensorFlow)
        self.feature_model = None
        self.load_feature_extraction_model()
        
        # Cache des embeddings des images produits
        self.product_embeddings = {}
        self.load_product_embeddings()
    
    def load_feature_extraction_model(self):
        """Charger le modèle d'extraction de features (MobileNetV2)"""
        try:
            # Utiliser MobileNetV2 pré-entraîné pour l'extraction de features
            base_model = tf.keras.applications.MobileNetV2(
                weights='imagenet',
                include_top=False,
                input_shape=(224, 224, 3),
                pooling='avg'  # Global Average Pooling
            )
            self.feature_model = base_model
            print("✅ Modèle d'extraction de features chargé (MobileNetV2)")
        except Exception as e:
            print(f"⚠️ Erreur chargement modèle TensorFlow: {e}")
            self.feature_model = None
    
    def load_product_embeddings(self):
        """Charger les embeddings des produits s'ils existent"""
        try:
            embeddings_file = "models/product_embeddings.pkl"
            if os.path.exists(embeddings_file):
                with open(embeddings_file, 'rb') as f:
                    self.product_embeddings = pickle.load(f)
                print(f"✅ {len(self.product_embeddings)} embeddings produits chargés")
        except Exception as e:
            print(f"⚠️ Erreur chargement embeddings: {e}")
            self.product_embeddings = {}

    async def find_similar_products(self, image_file, top_k: int, db: Session) -> List[Dict]:
        """✅ VERSION CORRIGÉE - Trouver des produits similaires sans dépendre du nom de fichier"""
        try:
            # 1. Sauvegarder l'image uploadée temporairement
            temp_image_path = await self.save_temp_image(image_file)
            print(f"🖼️ Image temporaire sauvée: {temp_image_path}")
            
            # 2. Extraire les features de l'image uploadée
            query_features = self.extract_image_features(temp_image_path)
            print(f"🧠 Features extraites: {query_features is not None}")
            
            if query_features is None:
                print("❌ Impossible d'extraire les features de l'image")
                self.cleanup_temp_file(temp_image_path)
                return []
            
            # ✅ 3. CORRECTION PRINCIPALE: Rechercher dans TOUTES les catégories
            print("🔍 Recherche dans toutes les catégories (pas de filtre)")
            products_with_images = self.get_all_products_with_images(db)
            print(f"📦 Total produits avec images trouvés: {len(products_with_images)}")
            
            # 4. Calculer la similarité avec TOUS les produits
            similarities = []
            
            for product_data in products_with_images:
                try:
                    # Obtenir ou calculer les features du produit
                    product_features = await self.get_product_features(
                        product_data['id'], 
                        product_data['url'],
                        db
                    )
                    
                    if product_features is not None:
                        # Calculer la similarité cosinus
                        similarity = self.calculate_similarity(query_features, product_features)
                        
                        # ✅ SEUIL ABAISSÉ: Garder les produits avec similarité > 0.5 au lieu de 0.7
                        if similarity > 0.5:
                            similarities.append({
                                "product_id": str(product_data['id']),
                                "name": product_data['nom_produit'],
                                "category": product_data['id_categorie'],
                                "price": float(product_data['prix']) if product_data['prix'] else 0,
                                "similarity": float(similarity),
                                "image_url": product_data['url'],
                                "description": product_data['description'],
                                "in_stock": product_data['quantite'] > 0
                            })
                            
                            print(f"✅ Produit {product_data['id']} ({product_data['nom_produit']}): similarité = {similarity:.3f}")
                        else:
                            print(f"⚠️ Produit {product_data['id']} écarté (similarité {similarity:.3f} <= 0.5)")
                    else:
                        print(f"⚠️ Pas de features pour produit {product_data['id']}")
                
                except Exception as e:
                    print(f"⚠️ Erreur traitement produit {product_data['id']}: {e}")
                    continue
            
            # 5. Trier par similarité et retourner top_k
            similarities.sort(key=lambda x: x['similarity'], reverse=True)
            
            # 6. Nettoyer l'image temporaire
            self.cleanup_temp_file(temp_image_path)
            
            # 7. Retourner les résultats
            if not similarities:
                print("ℹ️ Aucun produit avec similarité > 0.5 trouvé")
                return []
            
            print(f"🎯 Retour de {len(similarities[:top_k])} résultats sur {top_k} demandés")
            return similarities[:top_k]
            
        except Exception as e:
            print(f"❌ Erreur recherche image: {e}")
            # Retourner liste vide en cas d'erreur
            return []
    
    def get_all_products_with_images(self, db: Session) -> List[Dict]:
        """✅ NOUVELLE MÉTHODE - Obtenir TOUS les produits avec images (pas de filtre de catégorie)"""
        try:
            print("🔍 Recherche de tous les produits avec images...")
            
            # LEFT JOIN pour inclure tous les produits avec leurs images principales
            query = db.query(
                Produit.id,
                Produit.nom_produit,
                Produit.description,
                Produit.prix,
                Produit.quantite,
                Produit.id_categorie,
                ProduitImage.url
            ).outerjoin(
                ProduitImage, 
                (Produit.id == ProduitImage.produit_id) & 
                (ProduitImage.image_principale == True)
            ).filter(
                ProduitImage.url.isnot(None),  # Seulement les produits avec des URLs d'image valides
                Produit.quantite > 0  # En stock
            )
            
            results = query.limit(100).all()  # Augmenter la limite
            
            products = []
            for row in results:
                products.append({
                    'id': row.id,
                    'nom_produit': row.nom_produit,
                    'description': row.description,
                    'prix': row.prix,
                    'quantite': row.quantite,
                    'id_categorie': row.id_categorie,
                    'url': row.url
                })
            
            print(f"✅ Trouvé {len(products)} produits avec images dans toutes les catégories")
            return products
            
        except Exception as e:
            print(f"❌ Erreur récupération tous les produits avec images: {e}")
            return []
    
    # ✅ MÉTHODE CORRIGÉE: analyze_image_content devient optionnelle
    def analyze_image_content_advanced(self, image_path: str) -> str:
        """✅ Analyse avancée du contenu de l'image (optionnelle)"""
        try:
            if self.feature_model is None:
                return "general"
            
            # Charger l'image
            image = Image.open(image_path).convert('RGB')
            image = image.resize((224, 224))
            image_array = np.array(image) / 255.0
            image_array = np.expand_dims(image_array, axis=0)
            
            # Prédictions avec ImageNet (classes générales)
            predictions = self.feature_model.predict(image_array, verbose=0)
            
            # Analyser les features pour deviner la catégorie
            # (Cette partie pourrait être améliorée avec un modèle spécialisé)
            
            # Pour l'instant, retourner "general" pour rechercher dans tout
            return "general"
            
        except Exception as e:
            print(f"⚠️ Erreur analyse avancée: {e}")
            return "general"
    
    async def save_temp_image(self, image_file) -> str:
        """Sauvegarder l'image uploadée temporairement"""
        temp_dir = "temp"
        os.makedirs(temp_dir, exist_ok=True)
        
        # ✅ Garder l'extension originale si possible
        original_filename = getattr(image_file, 'filename', 'search_image.jpg')
        file_ext = os.path.splitext(original_filename)[1] or '.jpg'
        temp_filename = f"search_image{file_ext}"
        
        temp_path = os.path.join(temp_dir, temp_filename)
        
        with open(temp_path, "wb") as buffer:
            content = await image_file.read()
            buffer.write(content)
        
        # Reset file pointer for potential reuse
        await image_file.seek(0)
        
        return temp_path
    
    def extract_image_features(self, image_path: str) -> Optional[np.ndarray]:
        """Extraire les features d'une image avec le modèle CNN"""
        try:
            if self.feature_model is None:
                print("⚠️ Modèle CNN non disponible, utilisation features basiques")
                return self.extract_basic_features(image_path)
            
            # Charger et préprocesser l'image
            image = Image.open(image_path).convert('RGB')
            image = image.resize((224, 224))
            image_array = np.array(image) / 255.0
            image_array = np.expand_dims(image_array, axis=0)
            
            # Extraire les features avec MobileNetV2
            features = self.feature_model.predict(image_array, verbose=0)
            print(f"✅ Features CNN extraites: shape {features.shape}")
            return features.flatten()
            
        except Exception as e:
            print(f"⚠️ Erreur extraction features CNN: {e}")
            return self.extract_basic_features(image_path)
    
    def extract_basic_features(self, image_path: str) -> np.ndarray:
        """Extraction de features basiques en cas d'erreur avec le modèle CNN"""
        try:
            # Utiliser OpenCV pour des features simples
            image = cv2.imread(image_path)
            if image is None:
                print("⚠️ Impossible de lire l'image, features aléatoires")
                return np.random.rand(128)
            
            # Redimensionner l'image
            image = cv2.resize(image, (224, 224))
            
            # Calculer l'histogramme des couleurs
            hist_b = cv2.calcHist([image], [0], None, [32], [0, 256])
            hist_g = cv2.calcHist([image], [1], None, [32], [0, 256])
            hist_r = cv2.calcHist([image], [2], None, [32], [0, 256])
            
            # Concatener les histogrammes
            color_features = np.concatenate([hist_b, hist_g, hist_r]).flatten()
            
            # Normaliser
            color_features = color_features / (color_features.sum() + 1e-10)
            
            # Ajouter des features de texture (moments statistiques)
            gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
            texture_features = [
                np.mean(gray),
                np.std(gray),
                np.max(gray),
                np.min(gray)
            ]
            
            # Combiner features couleur et texture
            features = np.concatenate([color_features, texture_features])
            
            # Padding pour avoir une taille fixe de 128
            if len(features) < 128:
                features = np.pad(features, (0, 128 - len(features)), 'constant')
            else:
                features = features[:128]
            
            print(f"✅ Features basiques extraites: shape {features.shape}")
            return features
            
        except Exception as e:
            print(f"⚠️ Erreur extraction features basiques: {e}")
            return np.random.rand(128)
    
    async def get_product_features(self, product_id: int, image_url: str, db: Session) -> Optional[np.ndarray]:
        """Obtenir les features d'un produit (cache ou calcul)"""
        try:
            # Vérifier le cache
            if product_id in self.product_embeddings:
                return self.product_embeddings[product_id]
            
            # Télécharger l'image depuis l'URL et extraire les features
            image_path = await self.download_image_from_url(image_url)
            
            if image_path and os.path.exists(image_path):
                features = self.extract_image_features(image_path)
                
                # Mettre en cache
                if features is not None:
                    self.product_embeddings[product_id] = features
                
                # Nettoyer l'image temporaire
                self.cleanup_temp_file(image_path)
                
                return features
            
            return None
            
        except Exception as e:
            print(f"⚠️ Erreur récupération features produit {product_id}: {e}")
            return None
    
    async def download_image_from_url(self, image_url: str) -> Optional[str]:
        """Télécharger une image depuis son URL (MinIO ou HTTP)"""
        try:
            if not image_url:
                return None
            
            # Créer un nom de fichier temporaire
            temp_dir = "temp"
            os.makedirs(temp_dir, exist_ok=True)
            
            # Extraire le nom de fichier de l'URL
            filename = os.path.basename(image_url.split('/')[-1])
            temp_path = os.path.join(temp_dir, f"product_{filename}")
            
            # Méthode 1: Essayer avec MinIO client si c'est une URL MinIO
            if self.minio_public_url in image_url:
                try:
                    # Extraire le chemin de l'objet depuis l'URL
                    url_parts = image_url.replace(self.minio_public_url, '').strip('/')
                    bucket_and_path = url_parts.split('/', 1)
                    
                    if len(bucket_and_path) >= 2:
                        object_name = bucket_and_path[1]
                        
                        self.minio_client.fget_object(self.bucket_name, object_name, temp_path)
                        
                        if os.path.exists(temp_path):
                            return temp_path
                except Exception as minio_error:
                    print(f"⚠️ Erreur MinIO: {minio_error}, essai HTTP...")
            
            # Méthode 2: Téléchargement HTTP standard
            import requests
            response = requests.get(image_url, timeout=10)
            
            if response.status_code == 200:
                with open(temp_path, 'wb') as f:
                    f.write(response.content)
                
                if os.path.exists(temp_path):
                    return temp_path
            
            return None
            
        except Exception as e:
            print(f"⚠️ Erreur téléchargement image {image_url}: {e}")
            return None
    
    def calculate_similarity(self, features1: np.ndarray, features2: np.ndarray) -> float:
        """Calculer la similarité entre deux vecteurs de features"""
        try:
            # Normaliser les vecteurs
            features1_norm = features1 / (np.linalg.norm(features1) + 1e-10)
            features2_norm = features2 / (np.linalg.norm(features2) + 1e-10)
            
            # Similarité cosinus
            similarity = np.dot(features1_norm, features2_norm)
            
            # S'assurer que la similarité est entre 0 et 1
            return max(0, min(1, similarity))
            
        except Exception as e:
            print(f"⚠️ Erreur calcul similarité: {e}")
            return 0.0
    
    def cleanup_temp_file(self, file_path: str):
        """Nettoyer un fichier temporaire"""
        try:
            if file_path and os.path.exists(file_path):
                os.remove(file_path)
                print(f"🗑️ Fichier temporaire supprimé: {file_path}")
        except Exception as e:
            print(f"⚠️ Erreur nettoyage fichier {file_path}: {e}")
    
    def save_embeddings_cache(self):
        """Sauvegarder le cache des embeddings"""
        try:
            os.makedirs("models", exist_ok=True)
            with open("models/product_embeddings.pkl", 'wb') as f:
                pickle.dump(self.product_embeddings, f)
            print(f"✅ Cache embeddings sauvegardé ({len(self.product_embeddings)} produits)")
        except Exception as e:
            print(f"⚠️ Erreur sauvegarde cache: {e}")