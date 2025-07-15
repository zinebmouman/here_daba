# services/data_service.py - VERSION CORRIGÉE pour les images

from sqlalchemy.orm import Session
from database.models import Produit, ProduitImage, Boutique, Categorie, FavorisProduit, get_db
from typing import List, Optional
import pandas as pd
from minio import Minio
import os
from dotenv import load_dotenv
from sqlalchemy import and_

load_dotenv()

class DataService:
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
    
    def get_all_products(self, db: Session) -> List[dict]:
        """✅ CORRIGÉ - Récupérer tous les produits avec leurs images principales"""
        try:
            # ✅ REQUÊTE CORRIGÉE avec LEFT JOIN pour obtenir l'image principale depuis produit_image
            query = db.query(
                Produit.id,
                Produit.nom_produit,
                Produit.description,
                Produit.detail,
                Produit.prix,
                Produit.quantite,
                Produit.id_categorie,
                ProduitImage.url.label('image_url'),
                ProduitImage.chemin_fichier.label('image_path')
            ).outerjoin(
                ProduitImage,
                and_(
                    Produit.id == ProduitImage.produit_id,
                    ProduitImage.image_principale == True
                )
            ).all()

            result = []
            for row in query:
                # ✅ CORRECTION: Bien récupérer l'URL d'image et la convertir pour l'émulateur
                image_url = row.image_url
                if image_url:
                    # Convertir localhost vers 10.0.2.2 pour Android Emulator
                    image_url = image_url.replace('localhost:9000', '10.0.2.2:9000')
                    image_url = image_url.replace('127.0.0.1:9000', '10.0.2.2:9000')
                
                product_data = {
                    'id': row.id,
                    'nom_produit': row.nom_produit,
                    'description': row.description or '',
                    'detail': row.detail or '',
                    'prix': float(row.prix) if row.prix else 0.0,
                    'quantite': row.quantite or 0,
                    'id_categorie': row.id_categorie,
                    # ✅ CORRECTION PRINCIPALE: Inclure l'URL d'image dans tous les champs
                    'url': image_url,  # URL pour les services backend
                    'image_url': image_url,  # URL pour la compatibilité mobile
                    'minio_url': image_url,  # URL MinIO
                    'tags': self.extract_tags_from_product_data(row)
                }
                result.append(product_data)
            
            print(f"✅ [DATA_SERVICE] Récupéré {len(result)} produits")
            
            # Debug: Afficher quelques exemples avec leurs images
            products_with_images = [p for p in result if p.get('image_url')]
            products_without_images = [p for p in result if not p.get('image_url')]
            
            print(f"🖼️ [DATA_SERVICE] Produits avec images: {len(products_with_images)}")
            print(f"❌ [DATA_SERVICE] Produits sans images: {len(products_without_images)}")
            
            for i, product in enumerate(products_with_images[:3]):
                print(f"✅ [DATA_SERVICE] Produit {product['id']}: {product['nom_produit']} - Image: {product['image_url']}")
            
            if products_without_images:
                for i, product in enumerate(products_without_images[:3]):
                    print(f"❌ [DATA_SERVICE] Produit {product['id']}: {product['nom_produit']} - PAS D'IMAGE")
            
            return result

        except Exception as e:
            print(f"❌ [DATA_SERVICE] Erreur récupération produits: {e}")
            import traceback
            traceback.print_exc()
            return []
    
    def get_product_with_image(self, product_id: int, db: Session) -> Optional[dict]:
        """✅ NOUVEAU - Récupérer un produit spécifique avec son image"""
        try:
            result = db.query(
                Produit.id,
                Produit.nom_produit,
                Produit.description,
                Produit.detail,
                Produit.prix,
                Produit.quantite,
                Produit.id_categorie,
                ProduitImage.url.label('image_url')
            ).outerjoin(
                ProduitImage,
                and_(
                    Produit.id == ProduitImage.produit_id,
                    ProduitImage.image_principale == True
                )
            ).filter(Produit.id == product_id).first()
            
            if not result:
                return None
            
            # Convertir pour l'émulateur
            image_url = result.image_url
            if image_url:
                image_url = image_url.replace('localhost:9000', '10.0.2.2:9000')
            
            return {
                'id': result.id,
                'nom_produit': result.nom_produit,
                'description': result.description or '',
                'detail': result.detail or '',
                'prix': float(result.prix) if result.prix else 0.0,
                'quantite': result.quantite or 0,
                'id_categorie': result.id_categorie,
                'url': image_url,
                'image_url': image_url,
                'minio_url': image_url,
            }
            
        except Exception as e:
            print(f"❌ [DATA_SERVICE] Erreur récupération produit {product_id}: {e}")
            return None

    def get_user_preferences(self, user_id: str, db: Session) -> dict:
        """Obtenir les préférences utilisateur basées sur ses favoris"""
        try:
            # Récupérer les favoris de l'utilisateur
            favoris = db.query(
                FavorisProduit.id_produit,
                Produit.id_categorie,
                Produit.nom_produit
            ).join(
                Produit, FavorisProduit.id_produit == Produit.id
            ).filter(
                FavorisProduit.id_client == user_id
            ).all()
            
            categories = [fav.id_categorie for fav in favoris if fav.id_categorie]
            recent_products = [fav.nom_produit for fav in favoris if fav.nom_produit]
            
            # Compter les catégories préférées
            category_counts = {}
            for cat in categories:
                category_counts[cat] = category_counts.get(cat, 0) + 1
            
            preferred_categories = sorted(category_counts.keys(), 
                                        key=lambda x: category_counts[x], reverse=True)
            
            return {
                'user_id': user_id,
                'preferred_categories': preferred_categories[:3],  # Top 3 catégories
                'recent_products': recent_products[-10:],  # 10 produits récents
                'total_favorites': len(favoris)
            }
            
        except Exception as e:
            print(f"❌ Erreur récupération préférences user {user_id}: {e}")
            return {
                'user_id': user_id,
                'preferred_categories': ['electronique', 'vetements'],
                'recent_products': [],
                'total_favorites': 0
            }
    
    def get_products_by_category(self, category: str, db: Session) -> List[dict]:
        """Récupérer les produits d'une catégorie spécifique avec images"""
        try:
            # ✅ CORRECTION: Inclure les images dans la requête par catégorie
            query = db.query(
                Produit.id,
                Produit.nom_produit,
                Produit.description,
                Produit.prix,
                Produit.quantite,
                Produit.id_categorie,
                ProduitImage.url.label('image_url')
            ).outerjoin(
                ProduitImage,
                and_(
                    Produit.id == ProduitImage.produit_id,
                    ProduitImage.image_principale == True
                )
            ).filter(Produit.id_categorie == category).all()
            
            result = []
            for row in query:
                image_url = row.image_url
                if image_url:
                    image_url = image_url.replace('localhost:9000', '10.0.2.2:9000')
                
                result.append({
                    'id': row.id,
                    'nom_produit': row.nom_produit,
                    'description': row.description or '',
                    'prix': float(row.prix) if row.prix else 0.0,
                    'quantite': row.quantite or 0,
                    'id_categorie': row.id_categorie,
                    'url': image_url,
                    'image_url': image_url,
                    'minio_url': image_url,
                })
            
            return result
            
        except Exception as e:
            print(f"❌ Erreur récupération produits catégorie {category}: {e}")
            return []
    
    def get_similar_products_by_image(self, image_category: str, db: Session, limit: int = 5) -> List[dict]:
        """✅ CORRIGÉ - Trouver des produits similaires avec images"""
        try:
            similar_products = db.query(
                Produit.id,
                Produit.nom_produit,
                Produit.prix,
                Produit.id_categorie,
                ProduitImage.url.label('image_url')
            ).outerjoin(
                ProduitImage, 
                and_(
                    Produit.id == ProduitImage.produit_id,
                    ProduitImage.image_principale == True
                )
            ).filter(
                Produit.id_categorie == image_category,
                Produit.quantite > 0  # En stock
            ).limit(limit).all()
            
            results = []
            for i, product in enumerate(similar_products):
                # Convertir l'URL pour l'émulateur
                image_url = product.image_url
                if image_url:
                    image_url = image_url.replace('localhost:9000', '10.0.2.2:9000')
                
                similarity_score = 0.95 - (i * 0.05)  # Score décroissant
                results.append({
                    'product_id': str(product.id),
                    'name': product.nom_produit,
                    'price': float(product.prix) if product.prix else 0.0,
                    'category': product.id_categorie,
                    'similarity': similarity_score,
                    'image_url': image_url  # ✅ URL d'image incluse
                })
            
            return results
            
        except Exception as e:
            print(f"❌ Erreur recherche produits similaires: {e}")
            return []
    
    def search_products_by_text(self, query: str, db: Session) -> List[dict]:
        """✅ CORRIGÉ - Recherche textuelle avec images"""
        try:
            search_term = f"%{query.lower()}%"
            
            results = db.query(
                Produit.id,
                Produit.nom_produit,
                Produit.description,
                Produit.detail,
                Produit.prix,
                Produit.quantite,
                Produit.id_categorie,
                ProduitImage.url.label('image_url')
            ).outerjoin(
                ProduitImage,
                and_(
                    Produit.id == ProduitImage.produit_id,
                    ProduitImage.image_principale == True
                )
            ).filter(
                (Produit.nom_produit.ilike(search_term)) |
                (Produit.description.ilike(search_term)) |
                (Produit.detail.ilike(search_term))
            ).limit(20).all()
            
            products = []
            for row in results:
                image_url = row.image_url
                if image_url:
                    image_url = image_url.replace('localhost:9000', '10.0.2.2:9000')
                
                products.append({
                    'id': row.id,
                    'nom_produit': row.nom_produit,
                    'description': row.description or '',
                    'detail': row.detail or '',
                    'prix': float(row.prix) if row.prix else 0.0,
                    'quantite': row.quantite or 0,
                    'id_categorie': row.id_categorie,
                    'url': image_url,
                    'image_url': image_url,
                    'minio_url': image_url,
                    'tags': self.extract_tags_from_product_data(row)
                })
            
            return products
            
        except Exception as e:
            print(f"❌ Erreur recherche textuelle: {e}")
            return []
    
    def extract_tags_from_product_data(self, product_row) -> List[str]:
        """✅ CORRIGÉ - Extraire des tags depuis les données du produit"""
        tags = []
        
        if hasattr(product_row, 'nom_produit') and product_row.nom_produit:
            # Extraire des mots-clés du nom
            words = product_row.nom_produit.lower().split()
            tags.extend([word for word in words if len(word) > 2])
        
        if hasattr(product_row, 'id_categorie') and product_row.id_categorie:
            tags.append(product_row.id_categorie.lower())
        
        # Tags spécifiques basés sur le contenu
        if hasattr(product_row, 'description') and product_row.description:
            desc_lower = product_row.description.lower()
            if 'samsung' in desc_lower:
                tags.append('samsung')
            if 'iphone' in desc_lower:
                tags.append('iphone')
            if 'caftan' in desc_lower or 'djellaba' in desc_lower:
                tags.extend(['traditionnel', 'marocain'])
        
        return list(set(tags))  # Supprimer les doublons
    
    def extract_tags_from_product(self, product) -> List[str]:
        """Extraire des tags depuis un objet Produit (rétrocompatibilité)"""
        tags = []
        
        if product.nom_produit:
            words = product.nom_produit.lower().split()
            tags.extend([word for word in words if len(word) > 2])
        
        if product.id_categorie:
            tags.append(product.id_categorie.lower())
        
        if product.description:
            desc_lower = product.description.lower()
            if 'samsung' in desc_lower:
                tags.append('samsung')
            if 'iphone' in desc_lower:
                tags.append('iphone')
            if 'caftan' in desc_lower or 'djellaba' in desc_lower:
                tags.extend(['traditionnel', 'marocain'])
        
        return list(set(tags))
    
    def product_to_dict(self, product: Produit) -> dict:
        """Convertir un objet Produit en dictionnaire avec image"""
        # Récupérer l'image principale
        main_image = None
        if hasattr(product, 'images') and product.images:
            main_image = next(
                (img.url for img in product.images if img.image_principale), 
                None
            )
        
        # Convertir pour l'émulateur
        if main_image:
            main_image = main_image.replace('localhost:9000', '10.0.2.2:9000')
        
        return {
            'id': product.id,
            'nom_produit': product.nom_produit,
            'description': product.description,
            'detail': product.detail,
            'prix': float(product.prix) if product.prix else 0.0,
            'quantite': product.quantite,
            'id_categorie': product.id_categorie,
            'url': main_image,
            'image_url': main_image,
            'minio_url': main_image,
            'tags': self.extract_tags_from_product(product)
        }
    
    def check_image_exists_in_minio(self, image_path: str) -> bool:
        """Vérifier si une image existe dans MinIO"""
        try:
            self.minio_client.stat_object(self.bucket_name, image_path)
            return True
        except:
            return False
    
    def get_image_url(self, image_path: str) -> str:
        """Obtenir l'URL complète d'une image MinIO convertie pour l'émulateur"""
        if image_path:
            url = f"{self.minio_public_url}/{self.bucket_name}/{image_path}"
            # Convertir pour l'émulateur Android
            return url.replace('localhost:9000', '10.0.2.2:9000')
        return None