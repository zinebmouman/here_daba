package com.boutique_catalogue_produits.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/**
 * 🧠 Service d'extraction de features d'images avec IA
 * Utilise des techniques de deep learning pour créer des embeddings vectoriels
 */
@Service
public class ImageEmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(ImageEmbeddingService.class);

    @Value("${ai.image.model.path:models/image_model}")
    private String modelPath;

    @Value("${ai.image.input.size:224}")
    private int inputSize;

    @Value("${ai.image.embedding.dimension:512}")
    private int embeddingDimension;

    @Value("${ai.image.cache.enabled:true}")
    private boolean cacheEnabled;

    // Cache pour éviter de recalculer les embeddings
    private final Map<String, float[]> embeddingCache = new ConcurrentHashMap<>();

    // Modèle pré-entraîné simulé (en production, utilisez TensorFlow/ONNX/PyTorch)
    private ImageFeatureExtractor featureExtractor;

    @PostConstruct
    public void initialize() {
        logger.info("🧠 [IMAGE-EMBEDDING] Initialisation du service d'embedding d'images");

        try {
            // Initialiser le modèle d'IA (simulation)
            this.featureExtractor = new ImageFeatureExtractor(inputSize, embeddingDimension);
            logger.info("✅ [IMAGE-EMBEDDING] Modèle IA initialisé (dim: {})", embeddingDimension);

        } catch (Exception e) {
            logger.error("❌ [IMAGE-EMBEDDING] Erreur initialisation modèle: {}", e.getMessage());
            throw new RuntimeException("Impossible d'initialiser le modèle d'embedding", e);
        }
    }

    /**
     * 🔍 Extraire les features d'un fichier image uploadé
     */
    public float[] extractImageFeatures(MultipartFile imageFile) throws Exception {
        logger.debug("🔍 [IMAGE-EMBEDDING] Extraction features: {}", imageFile.getOriginalFilename());

        try {
            // Convertir en BufferedImage
            BufferedImage image = ImageIO.read(imageFile.getInputStream());
            if (image == null) {
                throw new Exception("Impossible de lire l'image");
            }

            // Extraire les features
            return extractFeaturesFromBufferedImage(image);

        } catch (Exception e) {
            logger.error("❌ [IMAGE-EMBEDDING] Erreur extraction fichier: {}", e.getMessage());
            throw new Exception("Erreur extraction features: " + e.getMessage(), e);
        }
    }

    /**
     * 🌐 Extraire les features d'une image depuis une URL
     */
    public float[] extractImageFeaturesFromUrl(String imageUrl) throws Exception {
        logger.debug("🌐 [IMAGE-EMBEDDING] Extraction features URL: {}", imageUrl);

        // Vérifier le cache
        if (cacheEnabled && embeddingCache.containsKey(imageUrl)) {
            logger.debug("📦 [CACHE] Features trouvées en cache pour: {}", imageUrl);
            return embeddingCache.get(imageUrl);
        }

        try {
            // Télécharger l'image
            BufferedImage image = downloadImageFromUrl(imageUrl);

            // Extraire les features
            float[] features = extractFeaturesFromBufferedImage(image);

            // Mettre en cache
            if (cacheEnabled) {
                embeddingCache.put(imageUrl, features);
            }

            return features;

        } catch (Exception e) {
            logger.error("❌ [IMAGE-EMBEDDING] Erreur extraction URL: {}", e.getMessage());
            throw new Exception("Erreur extraction features URL: " + e.getMessage(), e);
        }
    }

    /**
     * 📥 Extraire les features d'un InputStream
     */
    public float[] extractImageFeatures(InputStream imageStream) throws Exception {
        logger.debug("📥 [IMAGE-EMBEDDING] Extraction features InputStream");

        try {
            BufferedImage image = ImageIO.read(imageStream);
            if (image == null) {
                throw new Exception("Impossible de lire l'image depuis le stream");
            }

            return extractFeaturesFromBufferedImage(image);

        } catch (Exception e) {
            logger.error("❌ [IMAGE-EMBEDDING] Erreur extraction stream: {}", e.getMessage());
            throw new Exception("Erreur extraction features stream: " + e.getMessage(), e);
        }
    }

    /**
     * 🎨 Méthode principale d'extraction de features
     */
    private float[] extractFeaturesFromBufferedImage(BufferedImage image) throws Exception {
        try {
            // 1. Préprocessing de l'image
            BufferedImage preprocessedImage = preprocessImage(image);

            // 2. Extraction des features avec le modèle IA
            float[] features = featureExtractor.extractFeatures(preprocessedImage);

            // 3. Normalisation des features
            float[] normalizedFeatures = normalizeFeatures(features);

            logger.debug("✅ [IMAGE-EMBEDDING] Features extraites: {} dimensions", normalizedFeatures.length);
            return normalizedFeatures;

        } catch (Exception e) {
            logger.error("❌ [IMAGE-EMBEDDING] Erreur extraction features: {}", e.getMessage());
            throw new Exception("Erreur lors de l'extraction des features", e);
        }
    }

    /**
     * 🔧 Préprocessing de l'image
     */
    private BufferedImage preprocessImage(BufferedImage originalImage) {
        // 1. Redimensionner à la taille d'entrée du modèle
        BufferedImage resizedImage = resizeImage(originalImage, inputSize, inputSize);

        // 2. Normalisation des couleurs (0-1)
        BufferedImage normalizedImage = normalizeImageColors(resizedImage);

        // 3. Conversion en RGB si nécessaire
        return convertToRGB(normalizedImage);
    }

    /**
     * 📏 Redimensionner l'image
     */
    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        Image scaledImage = original.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();

        return resized;
    }

    /**
     * 🌈 Normalisation des couleurs
     */
    private BufferedImage normalizeImageColors(BufferedImage image) {
        // Simulation de normalisation - en production, utilisez des techniques plus avancées
        BufferedImage normalized = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);

                // Extraire R, G, B
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Normalisation simple (0-255 -> 0-255, mais peut être améliorée)
                int normalizedRGB = (r << 16) | (g << 8) | b;
                normalized.setRGB(x, y, normalizedRGB);
            }
        }

        return normalized;
    }

    /**
     * 🎨 Conversion en RGB
     */
    private BufferedImage convertToRGB(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_RGB) {
            return image;
        }

        BufferedImage rgbImage = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = rgbImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        return rgbImage;
    }

    /**
     * 📊 Normalisation des features
     */
    private float[] normalizeFeatures(float[] features) {
        // Normalisation L2
        double norm = 0.0;
        for (float feature : features) {
            norm += feature * feature;
        }
        norm = Math.sqrt(norm);

        if (norm == 0.0) {
            return features; // Éviter la division par zéro
        }

        float[] normalized = new float[features.length];
        for (int i = 0; i < features.length; i++) {
            normalized[i] = (float) (features[i] / norm);
        }

        return normalized;
    }

    /**
     * 🌐 Télécharger une image depuis une URL
     */
    private BufferedImage downloadImageFromUrl(String imageUrl) throws Exception {
        try {
            URL url = new URL(imageUrl);
            return ImageIO.read(url);
        } catch (Exception e) {
            logger.error("❌ [IMAGE-EMBEDDING] Erreur téléchargement URL {}: {}", imageUrl, e.getMessage());
            throw new Exception("Impossible de télécharger l'image: " + imageUrl, e);
        }
    }

    // =============== MÉTHODES UTILITAIRES ===============

    /**
     * 🔍 Comparer deux embeddings d'images
     */
    public double calculateSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1.length != embedding2.length) {
            throw new IllegalArgumentException("Les embeddings doivent avoir la même taille");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 🔄 Vider le cache
     */
    public void clearCache() {
        embeddingCache.clear();
        logger.info("🗑️ [IMAGE-EMBEDDING] Cache vidé ({} entrées supprimées)", embeddingCache.size());
    }

    /**
     * 📊 Statistiques du service
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", embeddingCache.size());
        stats.put("embeddingDimension", embeddingDimension);
        stats.put("inputSize", inputSize);
        stats.put("cacheEnabled", cacheEnabled);
        stats.put("modelPath", modelPath);
        return stats;
    }

    /**
     * 🧠 Extracteur de features simulé
     * En production, remplacez par TensorFlow, ONNX, ou PyTorch
     */
    private static class ImageFeatureExtractor {
        private final int inputSize;
        private final int embeddingDimension;
        private final Random random;

        public ImageFeatureExtractor(int inputSize, int embeddingDimension) {
            this.inputSize = inputSize;
            this.embeddingDimension = embeddingDimension;
            this.random = new Random(42); // Seed fixe pour la reproductibilité
        }

        /**
         * 🔬 Extraction des features (simulation)
         * En production, utilisez un vrai modèle CNN comme ResNet, EfficientNet, etc.
         */
        public float[] extractFeatures(BufferedImage image) {
            float[] features = new float[embeddingDimension];

            // SIMULATION - En production, remplacez par l'inférence d'un vrai modèle

            // 1. Extraction de features basiques de l'image
            double[] colorHistogram = extractColorHistogram(image);
            double[] textureFeatures = extractTextureFeatures(image);
            double[] shapeFeatures = extractShapeFeatures(image);

            // 2. Combinaison et projection vers l'espace d'embedding
            int idx = 0;

            // Features de couleur (premiers 1/3 des dimensions)
            for (int i = 0; i < embeddingDimension / 3 && i < colorHistogram.length; i++) {
                features[idx++] = (float) colorHistogram[i];
            }

            // Features de texture (second 1/3)
            for (int i = 0; i < embeddingDimension / 3 && i < textureFeatures.length; i++) {
                features[idx++] = (float) textureFeatures[i];
            }

            // Features de forme (dernier 1/3)
            for (int i = 0; i < embeddingDimension / 3 && i < shapeFeatures.length; i++) {
                features[idx++] = (float) shapeFeatures[i];
            }

            // Remplir le reste avec des features générées
            while (idx < embeddingDimension) {
                features[idx++] = (float) (random.nextGaussian() * 0.1);
            }

            return features;
        }

        private double[] extractColorHistogram(BufferedImage image) {
            double[] histogram = new double[64]; // 64 bins pour l'histogramme

            int width = image.getWidth();
            int height = image.getHeight();
            int totalPixels = width * height;

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int rgb = image.getRGB(x, y);

                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    // Quantification des couleurs
                    int bin = ((r / 64) * 16) + ((g / 64) * 4) + (b / 64);
                    histogram[Math.min(bin, 63)]++;
                }
            }

            // Normalisation
            for (int i = 0; i < histogram.length; i++) {
                histogram[i] /= totalPixels;
            }

            return histogram;
        }

        private double[] extractTextureFeatures(BufferedImage image) {
            double[] features = new double[32];

            // Simulation de features de texture (en production, utilisez GLCM, LBP, etc.)
            int width = image.getWidth();
            int height = image.getHeight();

            // Calcul de gradients simplifiés
            double totalGradient = 0.0;
            int gradientCount = 0;

            for (int x = 1; x < width - 1; x++) {
                for (int y = 1; y < height - 1; y++) {
                    int current = getGrayValue(image.getRGB(x, y));
                    int right = getGrayValue(image.getRGB(x + 1, y));
                    int down = getGrayValue(image.getRGB(x, y + 1));

                    double gradient = Math.sqrt(
                            Math.pow(right - current, 2) + Math.pow(down - current, 2)
                    );

                    totalGradient += gradient;
                    gradientCount++;
                }
            }

            double avgGradient = totalGradient / gradientCount;

            // Remplir les features avec des statistiques dérivées
            for (int i = 0; i < features.length; i++) {
                features[i] = avgGradient * (1.0 + 0.1 * Math.sin(i));
            }

            return features;
        }

        private double[] extractShapeFeatures(BufferedImage image) {
            double[] features = new double[32];

            // Simulation de features de forme
            int width = image.getWidth();
            int height = image.getHeight();

            // Ratio d'aspect
            double aspectRatio = (double) width / height;

            // Remplir avec des features géométriques basiques
            features[0] = aspectRatio;
            features[1] = Math.log(width * height); // Log de l'aire

            for (int i = 2; i < features.length; i++) {
                features[i] = aspectRatio * Math.cos(i) + Math.sin(i * aspectRatio);
            }

            return features;
        }

        private int getGrayValue(int rgb) {
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            return (int) (0.299 * r + 0.587 * g + 0.114 * b);
        }
    }

    // =============== MÉTHODES D'EXTENSION POUR VRAIS MODÈLES ===============

    /**
     * 🚀 Méthode pour intégrer un vrai modèle TensorFlow/ONNX
     * À implémenter quand vous ajoutez TensorFlow Java ou ONNX Runtime
     */
    public void loadPretrainedModel(String modelPath) {
        logger.info("🚀 [IMAGE-EMBEDDING] Chargement modèle pré-entraîné: {}", modelPath);

        // TODO: Implémenter le chargement d'un vrai modèle
        // Exemple avec TensorFlow Java:
        // SavedModelBundle model = SavedModelBundle.load(modelPath, "serve");
        // this.tensorflowModel = model;

        logger.warn("⚠️ [IMAGE-EMBEDDING] Modèle simulé - implémentez TensorFlow/ONNX pour de vrais embeddings");
    }

    /**
     * 🔧 Configuration avancée pour l'optimisation
     */
    public void configureOptimizations(Map<String, Object> optimizationConfig) {
        // GPU utilization, batch processing, etc.
        logger.info("🔧 [IMAGE-EMBEDDING] Configuration optimisations: {}", optimizationConfig);
    }
}