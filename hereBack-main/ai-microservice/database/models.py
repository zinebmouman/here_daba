from sqlalchemy import create_engine, Column, Integer, String, Numeric, Text, Boolean, BigInteger, Date, DateTime
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
import os
from dotenv import load_dotenv

load_dotenv()

# Configuration de la base de données
DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://postgres:zineb123@localhost:5432/boutiqueproduit")

engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

# Modèles correspondant à votre structure de base de données
class Produit(Base):
    __tablename__ = "produits"
    
    id = Column(BigInteger, primary_key=True, index=True)
    nom_produit = Column(String, index=True)
    description = Column(String)
    detail = Column(Text)
    prix = Column(Numeric)
    quantite = Column(Integer)
    seuil_critique = Column(Numeric)
    id_categorie = Column(String)
    id_stock = Column(BigInteger)
    id_reduction = Column(BigInteger)
    date_expiration = Column(Date)

class ProduitImage(Base):
    __tablename__ = "produit_images"
    
    id = Column(BigInteger, primary_key=True, index=True)
    produit_id = Column(BigInteger, index=True)
    chemin_fichier = Column(String)
    url = Column(String)
    minio_url = Column(String)
    image_principale = Column(Boolean, default=False)
    content_type = Column(String)
    file_size = Column(BigInteger)
    date_creation = Column(DateTime)

class Boutique(Base):
    __tablename__ = "boutique"
    
    id_boutique = Column(Integer, primary_key=True, index=True)
    nom = Column(String)
    adress = Column(Text)
    ville = Column(String)
    pays = Column(String)
    contact = Column(BigInteger)
    vendeur_id = Column(String)
    localisation = Column(String)
    boutique_img_url = Column(String)

class Categorie(Base):
    __tablename__ = "categorie"
    
    id_categorie = Column(String, primary_key=True, index=True)
    nom = Column(String)
    description = Column(String)
    icon = Column(String)

class FavorisProduit(Base):
    __tablename__ = "favoris_produit"
    
    id = Column(BigInteger, primary_key=True, index=True)
    id_client = Column(String, index=True)
    id_produit = Column(BigInteger, index=True)
    date_ajout = Column(DateTime)

# Fonction pour obtenir la session de base de données
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()