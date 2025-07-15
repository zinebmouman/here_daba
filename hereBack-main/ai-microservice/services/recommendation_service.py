# services/recommendation_service.py - VERSION CORRIGÉE pour les images

from sqlalchemy.orm import Session
from services.data_service import DataService
from typing import List, Dict
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import pandas as pd

class RecommendationService:
    def __init__(self):
        self.data_service = DataService()
        self.vectorizer = TfidfVectorizer(max_features=1000, stop_words='english')
        self.product_vectors = None
        self.products_df = None
    
    def get_personalized_recommendations(self, user_id: str, db: Session, num_recommendations: int = 10) -> List[Dict]:
        """✅ CORRIGÉ - Recommandations personnalisées avec URLs d'images"""
        try:
            print(f"🎯 [RECOMMENDATIONS] Début recommandations pour user {user_id}")
            
            # 1. Obtenir les préférences utilisateur
            user_prefs = self.data_service.get_user_preferences(user_id, db)
            print(f"👤 [RECOMMENDATIONS] Préférences: {user_prefs}")
            
            # 2. Obtenir tous les produits AVEC IMAGES
            all_products = self.data_service.get_all_products(db)
            
            if not all_products:
                print("⚠️ [RECOMMENDATIONS] Aucun produit trouvé")
                return []
            
            print(f"📦 [RECOMMENDATIONS] {len(all_products)} produits récupérés")
            
            # 3. Convertir en DataFrame pour faciliter le traitement
            products_df = pd.DataFrame(all_products)
            
            # 4. Debug des images
            products_with_images = products_df[products_df['image_url'].notna()]
            products_without_images = products_df[products_df['image_url'].isna()]
            
            print(f"🖼️ [RECOMMENDATIONS] Produits avec images: {len(products_with_images)}")
            print(f"❌ [RECOMMENDATIONS] Produits sans images: {len(products_without_images)}")
            
            # 5. Calculer les scores de recommandation
            recommendations = []
            
            for _, product in products_df.iterrows():
                score = self.calculate_recommendation_score(product, user_prefs)
                
                if score > 0.3:  # Seuil minimum
                    # ✅ CORRECTION PRINCIPALE: Inclure toutes les variantes d'URL d'image
                    image_url = product.get('image_url')
                    
                    recommendation = {
                        'id': str(product['id']),
                        'title': product['nom_produit'],
                        'description': product['description'],
                        'price': float(product['prix']) if product['prix'] else 0.0,
                        'category': product['id_categorie'],
                        'score': float(score),
                        'reason': self.generate_recommendation_reason(product, user_prefs),
                        'in_stock': int(product['quantite']) > 0,
                        # ✅ TOUTES LES VARIANTES D'URLS D'IMAGES
                        'image_url': image_url,  # URL principale
                        'url': image_url,  # URL alternative
                        'minio_url': image_url,  # URL MinIO
                    }
                    
                    recommendations.append(recommendation)
                    
                    # Debug pour les premières recommandations
                    if len(recommendations) <= 5:
                        print(f"🖼️ [RECOMMENDATIONS] Produit {product['id']} ({product['nom_produit']}): "
                              f"score={score:.3f}, image={image_url or 'NULL'}")
            
            # 6. Trier par score et limiter
            recommendations.sort(key=lambda x: x['score'], reverse=True)
            final_recommendations = recommendations[:num_recommendations]
            
            print(f"✅ [RECOMMENDATIONS] {len(final_recommendations)} recommandations générées")
            
            # Debug final détaillé
            print("🎯 [RECOMMENDATIONS] Résumé des recommandations finales:")
            for i, rec in enumerate(final_recommendations):
                has_image = rec.get('image_url') is not None
                print(f"   #{i+1}: {rec['title']} - score={rec['score']:.3f}, "
                      f"image={'✅' if has_image else '❌'} ({rec.get('image_url', 'NULL')})")
            
            return final_recommendations
            
        except Exception as e:
            print(f"❌ [RECOMMENDATIONS] Erreur recommandations personnalisées: {e}")
            import traceback
            traceback.print_exc()
            return self.get_fallback_recommendations(db, num_recommendations)
    
    def calculate_recommendation_score(self, product: pd.Series, user_prefs: Dict) -> float:
        """Calculer le score de recommandation pour un produit"""
        score = 0.3  # Score de base
        
        # Bonus si dans les catégories préférées (40% du score)
        if product['id_categorie'] in user_prefs['preferred_categories']:
            category_index = user_prefs['preferred_categories'].index(product['id_categorie'])
            # Plus la catégorie est en haut, plus le bonus est élevé
            category_bonus = 0.4 * (1 - category_index * 0.1)
            score += category_bonus
        
        # Bonus si le produit correspond aux produits récents (30% du score)
        product_name_lower = str(product['nom_produit']).lower()
        for recent_product in user_prefs['recent_products']:
            if recent_product and any(word in product_name_lower for word in str(recent_product).lower().split()):
                score += 0.3
                break
        
        # Bonus si en stock (15% du score)
        if product['quantite'] and int(product['quantite']) > 0:
            score += 0.15
        
        # Bonus si prix raisonnable (15% du score)
        if product['prix'] and 100 <= float(product['prix']) <= 5000:
            score += 0.15
        
        return min(score, 1.0)
    
    def generate_recommendation_reason(self, product: pd.Series, user_prefs: Dict) -> str:
        """Générer une raison pour la recommandation"""
        if product['id_categorie'] in user_prefs['preferred_categories']:
            return f"Recommandé car vous aimez la catégorie {product['id_categorie']}"
        elif user_prefs['total_favorites'] > 0:
            return "Basé sur vos préférences précédentes"
        else:
            return "Produit populaire dans cette catégorie"
    
    def get_content_based_recommendations(self, query_product_id: int, db: Session, num_recommendations: int = 5) -> List[Dict]:
        """✅ CORRIGÉ - Recommandations basées sur le contenu avec images"""
        try:
            print(f"🔍 [CONTENT_RECOMMENDATIONS] Recherche similaires à produit {query_product_id}")
            
            # Obtenir tous les produits avec images
            all_products = self.data_service.get_all_products(db)
            
            if not all_products:
                return []
            
            # Trouver le produit de référence
            query_product = next((p for p in all_products if p['id'] == query_product_id), None)
            
            if not query_product:
                print(f"❌ [CONTENT_RECOMMENDATIONS] Produit {query_product_id} non trouvé")
                return []
            
            # Filtrer les produits de la même catégorie
            same_category_products = [
                p for p in all_products 
                if p['id_categorie'] == query_product['id_categorie'] and p['id'] != query_product_id
            ]
            
            # Calculer la similarité basée sur les tags et descriptions
            recommendations = []
            
            for product in same_category_products[:num_recommendations]:
                similarity = self.calculate_content_similarity(query_product, product)
                
                # ✅ CORRECTION: Inclure l'URL d'image
                recommendation = {
                    'id': str(product['id']),
                    'title': product['nom_produit'],
                    'price': float(product['prix']) if product['prix'] else 0.0,
                    'category': product['id_categorie'],
                    'similarity': float(similarity),
                    # ✅ URLS D'IMAGES INCLUSES
                    'image_url': product.get('image_url'),
                    'url': product.get('url'),
                    'minio_url': product.get('minio_url'),
                }
                
                recommendations.append(recommendation)
            
            # Trier par similarité
            recommendations.sort(key=lambda x: x['similarity'], reverse=True)
            
            print(f"✅ [CONTENT_RECOMMENDATIONS] {len(recommendations)} recommandations similaires")
            return recommendations
            
        except Exception as e:
            print(f"❌ [CONTENT_RECOMMENDATIONS] Erreur recommandations basées contenu: {e}")
            return []
    
    def calculate_content_similarity(self, product1: Dict, product2: Dict) -> float:
        """Calculer la similarité entre deux produits"""
        # Combinaison de text pour chaque produit
        text1 = f"{product1['nom_produit']} {product1['description']} {' '.join(product1.get('tags', []))}"
        text2 = f"{product2['nom_produit']} {product2['description']} {' '.join(product2.get('tags', []))}"
        
        # Calculer similarité TF-IDF
        try:
            tfidf_matrix = self.vectorizer.fit_transform([text1, text2])
            similarity = cosine_similarity(tfidf_matrix[0:1], tfidf_matrix[1:2])[0][0]
            return float(similarity)
        except:
            # Fallback: similarité basique basée sur la catégorie et le prix
            if product1['id_categorie'] == product2['id_categorie']:
                price1 = float(product1.get('prix', 0))
                price2 = float(product2.get('prix', 0))
                price_diff = abs(price1 - price2)
                price_similarity = max(0, 1 - price_diff / 1000)  # Normaliser par 1000
                return price_similarity * 0.8
            return 0.3
    
    def get_fallback_recommendations(self, db: Session, num_recommendations: int) -> List[Dict]:
        """✅ CORRIGÉ - Recommandations de fallback avec images"""
        try:
            print("🔄 [FALLBACK_RECOMMENDATIONS] Utilisation des recommandations de base")
            
            # Obtenir les produits les plus récents en stock AVEC IMAGES
            all_products = self.data_service.get_all_products(db)
            
            # Filtrer les produits en stock et trier par quantité
            in_stock_products = [p for p in all_products if p.get('quantite', 0) > 0]
            in_stock_products.sort(key=lambda x: x.get('quantite', 0), reverse=True)
            
            print(f"🔄 [FALLBACK] {len(in_stock_products)} produits en stock disponibles")
            
            recommendations = []
            for i, product in enumerate(in_stock_products[:num_recommendations]):
                image_url = product.get('image_url')
                
                # ✅ CORRECTION: Inclure l'URL d'image dans le fallback
                recommendation = {
                    'id': str(product['id']),
                    'title': product['nom_produit'],
                    'description': product['description'],
                    'price': float(product['prix']) if product['prix'] else 0.0,
                    'category': product['id_categorie'],
                    'score': float(0.7 - i * 0.05),
                    'reason': 'Produit populaire en stock',
                    'in_stock': True,
                    'fallback': True,
                    # ✅ URLS D'IMAGES INCLUSES
                    'image_url': image_url,
                    'url': image_url,
                    'minio_url': image_url,
                }
                
                recommendations.append(recommendation)
                
                # Debug
                print(f"🔄 [FALLBACK] Produit {product['id']}: {product['nom_produit']} - "
                      f"image={'✅' if image_url else '❌'} ({image_url or 'NULL'})")
            
            print(f"✅ [FALLBACK_RECOMMENDATIONS] {len(recommendations)} recommandations de base")
            return recommendations
            
        except Exception as e:
            print(f"❌ [FALLBACK_RECOMMENDATIONS] Erreur fallback recommendations: {e}")
            import traceback
            traceback.print_exc()
            return []
    
    def get_boutique_recommendations(self, user_id: str, boutique_id: str, db: Session, num_recommendations: int = 5) -> List[Dict]:
        """✅ NOUVEAU - Recommandations spécifiques à une boutique avec images"""
        try:
            print(f"🏪 [BOUTIQUE_RECOMMENDATIONS] User {user_id}, Boutique {boutique_id}")
            
            # Obtenir les préférences utilisateur
            user_prefs = self.data_service.get_user_preferences(user_id, db)
            
            # Obtenir tous les produits avec images
            all_products = self.data_service.get_all_products(db)
            
            # Filtrer par boutique si nécessaire (ajout future)
            # Pour l'instant, utiliser tous les produits
            
            # Appliquer l'algorithme de recommandation standard
            recommendations = []
            for product in all_products:
                score = self.calculate_recommendation_score(pd.Series(product), user_prefs)
                
                if score > 0.4:  # Seuil plus élevé pour les boutiques
                    recommendations.append({
                        'id': str(product['id']),
                        'title': product['nom_produit'],
                        'description': product['description'],
                        'price': float(product['prix']) if product['prix'] else 0.0,
                        'category': product['id_categorie'],
                        'score': float(score),
                        'reason': f"Recommandé pour la boutique {boutique_id}",
                        'in_stock': int(product.get('quantite', 0)) > 0,
                        'image_url': product.get('image_url'),
                        'url': product.get('url'),
                        'minio_url': product.get('minio_url'),
                    })
            
            # Trier et limiter
            recommendations.sort(key=lambda x: x['score'], reverse=True)
            final_recommendations = recommendations[:num_recommendations]
            
            print(f"✅ [BOUTIQUE_RECOMMENDATIONS] {len(final_recommendations)} recommandations boutique")
            return final_recommendations
            
        except Exception as e:
            print(f"❌ [BOUTIQUE_RECOMMENDATIONS] Erreur: {e}")
            return self.get_fallback_recommendations(db, num_recommendations)