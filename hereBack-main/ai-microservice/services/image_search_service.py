# image_search_service.py

import os
import io
import pickle
import asyncio
from typing import List, Dict, Optional
from pathlib import Path
from urllib.parse import urlparse

import cv2
import numpy as np
import requests
import tensorflow as tf
from PIL import Image
from sklearn.metrics.pairwise import cosine_similarity
from sqlalchemy.orm import Session
from minio import Minio
from dotenv import load_dotenv

from database.models import Produit, ProduitImage, get_db

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
        # URL publique MinIO utilisée par l'émulateur Android
        self.minio_public_url = os.getenv("MINIO_PUBLIC_URL", "http://192.168.41.39:9000")

        # Modèle de features extraction (MobileNetV2)
        self.feature_model: Optional[tf.keras.Model] = None
        self.load_feature_extraction_model()

        # Cache des embeddings des images produits
        self.product_embeddings: Dict[int, np.ndarray] = {}
        self.load_product_embeddings()

    def load_feature_extraction_model(self):
        """Charger le modèle d'extraction de features (MobileNetV2)"""
        try:
            base_model = tf.keras.applications.MobileNetV2(
                weights='imagenet',
                include_top=False,
                input_shape=(224, 224, 3),
                pooling='avg'
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
        """Trouver des produits similaires sans dépendre du nom de fichier"""
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

            # 3. Rechercher dans toutes les catégories
            products_with_images = self.get_all_products_with_images(db)
            print(f"📦 Total produits avec images trouvés: {len(products_with_images)}")

            # 4. Calculer la similarité
            similarities = []
            for product_data in products_with_images:
                product_features = await self.get_product_features(
                    int(product_data['id']),
                    product_data['url'],
                    db
                )
                if product_features is not None:
                    sim = self.calculate_similarity(query_features, product_features)
                    if sim > 0.5:
                        similarities.append({
                            "product_id": str(product_data['id']),
                            "name": product_data['nom_produit'],
                            "category": product_data['id_categorie'],
                            "price": float(product_data['prix']),
                            "similarity": float(sim),
                            "image_url": product_data['url'],
                            "description": product_data['description'],
                            "in_stock": product_data['quantite'] > 0
                        })
                        print(f"✅ Produit {product_data['id']} ({product_data['nom_produit']}): similarité = {sim:.3f}")
                    else:
                        print(f"⚠️ Produit {product_data['id']} écarté (similarité {sim:.3f} ≤ 0.5)")
                else:
                    print(f"⚠️ Pas de features pour produit {product_data['id']}")

            # 5. Trier et retourner top_k
            similarities.sort(key=lambda x: x['similarity'], reverse=True)
            self.cleanup_temp_file(temp_image_path)

            if not similarities:
                print("ℹ️ Aucun produit avec similarité > 0.5 trouvé")
                return []

            print(f"🎯 Retour de {len(similarities[:top_k])} résultats sur {top_k} demandés")
            return similarities[:top_k]

        except Exception as e:
            print(f"❌ Erreur recherche image: {e}")
            return []

    def get_all_products_with_images(self, db: Session) -> List[Dict]:
        """Obtenir tous les produits avec images principales"""
        try:
            print("🔍 Recherche de tous les produits avec images...")
            query = (
                db.query(
                    Produit.id,
                    Produit.nom_produit,
                    Produit.description,
                    Produit.prix,
                    Produit.quantite,
                    Produit.id_categorie,
                    ProduitImage.url
                )
                .outerjoin(
                    ProduitImage,
                    (Produit.id == ProduitImage.produit_id) & (ProduitImage.image_principale == True)
                )
                .filter(ProduitImage.url.isnot(None), Produit.quantite > 0)
                .limit(100)
                .all()
            )

            products = []
            for row in query:
                raw = row.url or ""
                parsed = urlparse(raw)
                # Toujours reconstruire l'URL via minio_public_url + path
                url = f"{self.minio_public_url}{parsed.path}"
                products.append({
                    'id': row.id,
                    'nom_produit': row.nom_produit,
                    'description': row.description or "",
                    'prix': float(row.prix) if row.prix else 0.0,
                    'quantite': row.quantite or 0,
                    'id_categorie': row.id_categorie,
                    'url': url
                })

            print(f"✅ Trouvé {len(products)} produits avec images")
            return products

        except Exception as e:
            print(f"❌ Erreur récupération produits avec images: {e}")
            return []

    async def save_temp_image(self, image_file) -> str:
        """Sauvegarder l'image uploadée temporairement"""
        temp_dir = "temp"
        os.makedirs(temp_dir, exist_ok=True)
        original = getattr(image_file, 'filename', 'upload.jpg')
        ext = os.path.splitext(original)[1] or '.jpg'
        path = os.path.join(temp_dir, f"search_image{ext}")
        content = await image_file.read()
        with open(path, 'wb') as f:
            f.write(content)
        await image_file.seek(0)
        return path

    def extract_image_features(self, image_path: str) -> Optional[np.ndarray]:
        """Extraire les features d'une image avec le modèle CNN"""
        try:
            if self.feature_model:
                img = Image.open(image_path).convert('RGB').resize((224, 224))
                arr = np.expand_dims(np.array(img) / 255.0, axis=0)
                feats = self.feature_model.predict(arr, verbose=0).flatten()
                print(f"✅ Features CNN extraites: shape {feats.shape}")
                return feats
            else:
                return self.extract_basic_features(image_path)
        except Exception as e:
            print(f"⚠️ Erreur extraction features CNN: {e}")
            return self.extract_basic_features(image_path)

    def extract_basic_features(self, image_path: str) -> np.ndarray:
        """Extraction de features basiques si le CNN échoue"""
        try:
            img = cv2.imread(image_path)
            if img is None:
                return np.random.rand(128)
            img = cv2.resize(img, (224, 224))
            hb = cv2.calcHist([img], [0], None, [32], [0,256])
            hg = cv2.calcHist([img], [1], None, [32], [0,256])
            hr = cv2.calcHist([img], [2], None, [32], [0,256])
            color = np.concatenate([hb, hg, hr]).flatten()
            color /= (color.sum() + 1e-10)
            gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
            stats = np.array([gray.mean(), gray.std(), gray.max(), gray.min()])
            feats = np.concatenate([color, stats])
            if feats.size < 128:
                feats = np.pad(feats, (0,128-feats.size), 'constant')
            return feats[:128]
        except Exception as e:
            print(f"⚠️ Erreur extraction basique: {e}")
            return np.random.rand(128)

    async def get_product_features(self, product_id: int, image_url: str, db: Session) -> Optional[np.ndarray]:
        """Obtenir ou calculer les features d'un produit"""
        if product_id in self.product_embeddings:
            return self.product_embeddings[product_id]
        image_path = await self.download_image_from_url(image_url)
        if image_path:
            feats = self.extract_image_features(image_path)
            if feats is not None:
                self.product_embeddings[product_id] = feats
            self.cleanup_temp_file(image_path)
            return feats
        return None

    async def download_image_from_url(self, image_url: str) -> Optional[str]:
        """Télécharger une image depuis MinIO (préféré) ou HTTP"""
        if not image_url:
            return None

        # Parser l'URL et extraire le chemin d'objet
        parsed = urlparse(image_url)
        path = parsed.path.lstrip("/")
        if path.startswith(self.bucket_name + "/"):
            object_name = path[len(self.bucket_name) + 1:]
        else:
            object_name = path

        temp_dir = "temp"
        os.makedirs(temp_dir, exist_ok=True)
        filename = os.path.basename(object_name)
        temp_path = os.path.join(temp_dir, f"product_{filename}")

        # 1) Tenter MinIO client
        loop = asyncio.get_event_loop()
        try:
            await loop.run_in_executor(
                None,
                self.minio_client.fget_object,
                self.bucket_name,
                object_name,
                temp_path
            )
            if os.path.exists(temp_path):
                return temp_path
        except Exception as e:
            print(f"⚠️ MinIO fget_object a échoué pour {object_name}: {e}")

        # 2) Fallback HTTP
        try:
            resp = requests.get(image_url, timeout=10)
            if resp.status_code == 200:
                with open(temp_path, "wb") as f:
                    f.write(resp.content)
                return temp_path
        except Exception as e:
            print(f"⚠️ Erreur téléchargement HTTP {image_url}: {e}")

        return None

    def calculate_similarity(self, f1: np.ndarray, f2: np.ndarray) -> float:
        """Calculer la similarité cosinus entre deux vecteurs"""
        try:
            n1 = f1 / (np.linalg.norm(f1) + 1e-10)
            n2 = f2 / (np.linalg.norm(f2) + 1e-10)
            sim = float(np.dot(n1, n2))
            return max(0.0, min(1.0, sim))
        except Exception as e:
            print(f"⚠️ Erreur calcul similarité: {e}")
            return 0.0

    def cleanup_temp_file(self, file_path: str):
        """Supprimer un fichier temporaire"""
        try:
            if file_path and os.path.exists(file_path):
                os.remove(file_path)
                print(f"🗑️ Fichier temporaire supprimé: {file_path}")
        except Exception as e:
            print(f"⚠️ Erreur suppression fichier {file_path}: {e}")

    def save_embeddings_cache(self):
        """Sauvegarder le cache des embeddings"""
        try:
            os.makedirs("models", exist_ok=True)
            with open("models/product_embeddings.pkl", "wb") as f:
                pickle.dump(self.product_embeddings, f)
            print(f"✅ Cache embeddings sauvegardé ({len(self.product_embeddings)} produits)")
        except Exception as e:
            print(f"⚠️ Erreur sauvegarde cache: {e}")
