import os
from dotenv import load_dotenv

load_dotenv()

class Settings:
    # Configuration base de données (même que Spring Boot)
    DATABASE_URL = "postgresql://postgres:zineb123@localhost:5432/boutiqueproduit"
    
    # Configuration IA
    MODEL_PATH = "models/"
    TEMP_PATH = "temp/"
    
    # Configuration API
    HOST = "0.0.0.0"
    PORT = 8000

settings = Settings()