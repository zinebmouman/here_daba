from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from config.settings import settings
import logging
from sqlalchemy import text

logger = logging.getLogger(__name__)

# Créer le moteur de base de données
engine = create_engine(
    settings.DATABASE_URL,
    pool_size=10,
    max_overflow=20,
    pool_recycle=3600
)

# Créer la session
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

def get_database():
    """Obtenir une session de base de données"""
    db = SessionLocal()
    try:
        yield db
    except Exception as e:
        logger.error(f"❌ Erreur base de données: {e}")
        db.rollback()
        raise
    finally:
        db.close()

def test_connection():
    """Tester la connexion à la base de données"""
    try:
        db = SessionLocal()
        db.execute(text("SELECT 1"))
        db.close()
        logger.info("✅ Connexion PostgreSQL réussie")
        return True
    except Exception as e:
        logger.error(f"❌ Échec connexion PostgreSQL: {e}")
        return False