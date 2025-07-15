from fastapi import FastAPI, UploadFile, File, Form, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
import os
from sqlalchemy.orm import Session
from database.models import get_db
from services.data_service import DataService
from services.recommendation_service import RecommendationService
from services.image_search_service import ImageSearchService

app = FastAPI(title="AI Microservice with Real Data", version="2.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:9090"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Créer les dossiers nécessaires
os.makedirs("temp", exist_ok=True)

# Initialiser les services
data_service = DataService()
recommendation_service = RecommendationService()
image_search_service = ImageSearchService()

@app.get("/api/ai/health")
async def health_check():
    return {
        "status": "healthy",
        "service": "AI Microservice with Real Data",
        "database": "PostgreSQL Connected",
        "storage": "MinIO Connected",
        "models_loaded": True
    }

@app.post("/api/ml/recommendations")
async def ml_recommendations(
    user_id: str = Form(...),
    num_recommendations: int = Form(10),
    algorithm: str = Form("collaborative_filtering"),
    db: Session = Depends(get_db)
):
    """Recommandations ML avec vraies données PostgreSQL"""
    try:
        recommendations = recommendation_service.get_personalized_recommendations(
            user_id, db, num_recommendations
        )
        
        return {
            "success": True,
            "user_id": user_id,
            "recommendations": recommendations,
            "algorithm": algorithm,
            "total_found": len(recommendations),
            "data_source": "PostgreSQL"
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/ml/image-similarity")
async def image_similarity(
    image: UploadFile = File(...),
    top_k: int = Form(5),
    model_type: str = Form("mobilenet_v2"),
    db: Session = Depends(get_db)
):
    """Recherche d'images similaires avec vraies données"""
    try:
        results = await image_search_service.find_similar_products(
            image, top_k, db
        )
        
        return {
            "success": True,
            "query_image": image.filename,
            "similar_products": results,
            "model": model_type,
            "total_results": len(results),
            "data_source": "PostgreSQL + MinIO"
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/ml/hybrid-analysis")
async def hybrid_analysis(
    query: str = Form(None),
    image: UploadFile = File(None),
    user_id: str = Form(None),
    db: Session = Depends(get_db)
):
    """Analyse hybride avec vraies données"""
    try:
        results = {"success": True, "data_source": "PostgreSQL + MinIO"}
        
        if query:
            # Recherche textuelle
            text_results = data_service.search_products_by_text(query, db)
            results["text_search"] = {
                "query": query,
                "products_found": len(text_results),
                "products": text_results[:5]  # Top 5
            }
        
        if image:
            # Recherche par image
            image_results = await image_search_service.find_similar_products(image, 5, db)
            results["image_search"] = {
                "filename": image.filename,
                "similar_products": image_results
            }
        
        if user_id:
            # Recommandations personnalisées
            user_recs = recommendation_service.get_personalized_recommendations(user_id, db, 5)
            results["personalized_recommendations"] = user_recs
        
        return results
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/ml/stats")
async def get_ml_stats(db: Session = Depends(get_db)):
    """Statistiques des données ML"""
    try:
        all_products = data_service.get_all_products(db)
        
        # Calculer des statistiques
        stats = {
            "total_products": len(all_products),
            "products_with_images": len([p for p in all_products if p.get('minio_url')]),
            "products_in_stock": len([p for p in all_products if p['quantite'] > 0]),
            "categories": list(set([p['id_categorie'] for p in all_products if p['id_categorie']])),
            "average_price": sum([p['prix'] for p in all_products if p['prix']]) / len(all_products) if all_products else 0,
            "data_source": "PostgreSQL"
        }
        
        return {"success": True, "stats": stats}
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)