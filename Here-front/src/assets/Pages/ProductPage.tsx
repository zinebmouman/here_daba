import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import Product from "../Components/ProductPage/Product";
import ProductFeatures from "../Components/ProductPage/ProductFeatures";
import Navbar from "../Components/Navbar";
import StoreLocationSlider from "../Components/ProductPage/StoreLocationSlider";
import CustomerReviewsSlider from "../Components/ProductPage/CustomerReviewsSlider";
import RelatedProducts from "../Components/ProductPage/RelatedProducts";
import Footer from "../Components/Footer";
import axios from 'axios';

// Définir la fonction normalizeProduct modifiée
const normalizeProduct = (product) => {
  return {
    id: product.id,
    name: product.name || product.nomProduit,
    nomProduit: product.nomProduit || product.name,
    description: product.description || "", // Pas de texte par défaut
    price: product.price || product.prix,
    prix: product.prix || product.price,
    originalPrice: product.originalPrice || product.prixOriginal,
    prixOriginal: product.prixOriginal || product.originalPrice,
    discount: product.discount || product.reduction,
    reduction: product.reduction || product.discount,
    // Supprimer la valeur par défaut "Cosmétiques Locaux"
    brand: product.brand || product.marque,
    marque: product.marque || product.brand,
    category: product.category || product.categorie,
    categorie: product.categorie || product.category,
    idCategorie: product.idCategorie,
    sku: product.sku || product.reference,
    reference: product.reference || product.sku,
    images: product.images || [],
    detail: product.detail || "",
    currentPrice: product.currentPrice || product.prix,
    imageUrl: product.imageUrl || (product.images && product.images.length > 0 ? product.images[0] : null),
    rating: product.rating || 0,
    reviewCount: product.reviewCount || 0,
    quantite: product.quantite || 0 // S'assurer que quantite est présent
  };
};

const ProductPage = () => {
  const { id } = useParams();
  const [loading, setLoading] = useState(true);
  const [product, setProduct] = useState(null);
  const [stores, setStores] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [relatedProducts, setRelatedProducts] = useState([]);
  const [error, setError] = useState(null);

  const baseUrl = "http://localhost:8080";
  const placeholderImage = `${baseUrl}/api/fichiers/placeholder.png`;

  useEffect(() => {
    const fetchProductData = async () => {
      try {
        setLoading(true);
        
        if (id) {
          // Charger les détails du produit
          const productResponse = await axios.get(`${baseUrl}/api/produits/${id}`);
          const productData = productResponse.data;
          
          console.log("Données brutes du produit:", productData);
          console.log("Nom du produit depuis l'API:", productData.nomProduit);
          console.log("Description depuis l'API:", productData.description);
          console.log("Detail depuis l'API:", productData.detail);
          
          // Charger les images du produit
          const imagesResponse = await axios.get(`${baseUrl}/api/produits/${id}/images`);
          const images = imagesResponse.data;

          // Charger les informations de catégorie
          let categoryName = "";
          if (productData.idCategorie) {
            try {
              const categoryResponse = await axios.get(`${baseUrl}/api/categories/${productData.idCategorie}`);
              categoryName = categoryResponse.data.nom;
            } catch (error) {
              console.error("Erreur lors du chargement de la catégorie:", error);
            }
          }
          
          // Charger les informations de réduction
          let realDiscount = null;
          let originalPrice = null;
          
          if (productData.idReduction) {
            try {
              const reductionResponse = await axios.get(`${baseUrl}/api/reductions/${productData.idReduction}`);
              const reductionData = reductionResponse.data;
              
              if (reductionData.pourcentage && reductionData.pourcentage > 0) {
                realDiscount = `${reductionData.pourcentage}%`;
                originalPrice = productData.prix / (1 - reductionData.pourcentage / 100);
              }
            } catch (error) {
              console.error("Erreur lors du chargement de la réduction:", error);
            }
          }
          
          // Construire l'objet produit
          const formattedProduct = {
            id: productData.id,
            nomProduit: productData.nomProduit, // Garder le nom original
            name: productData.nomProduit, // Dupliquer pour compatibilité
            description: productData.description || "", // Pas de valeur par défaut
            prix: productData.prix, // Garder le prix original
            price: productData.prix, // Dupliquer pour compatibilité
            originalPrice: originalPrice,
            prixOriginal: originalPrice, // Dupliquer pour compatibilité
            discount: realDiscount,
            reduction: realDiscount, // Dupliquer pour compatibilité
            // Suppression de la valeur par défaut "Cosmétiques Locaux"
            brand: productData.marque,
            marque: productData.marque,
            category: categoryName,
            categorie: categoryName, // Dupliquer pour compatibilité
            idCategorie: productData.idCategorie,
            sku: productData.reference || "123456789",
            reference: productData.reference || "123456789", // Dupliquer pour compatibilité
            images: images.length > 0 
              ? images.map(img => img.url)
              : [placeholderImage, placeholderImage, placeholderImage],
            detail: productData.detail || "",
            quantite: productData.quantite || 0 // Ajouter la quantité
          };
          
          console.log("Produit formaté:", formattedProduct);
          console.log("Description dans le produit formaté:", formattedProduct.description);
          console.log("Detail dans le produit formaté:", formattedProduct.detail);
          
          // Normaliser le produit pour garantir la cohérence
          const normalizedProduct = normalizeProduct(formattedProduct);
          console.log("Produit normalisé:", normalizedProduct);
          console.log("Description dans le produit normalisé:", normalizedProduct.description);
          console.log("Detail dans le produit normalisé:", normalizedProduct.detail);
          
          setProduct(normalizedProduct);

          // Charger les boutiques
          try {
            const boutiquesResponse = await axios.get(`${baseUrl}/api/boutiques`);
            const formattedStores = boutiquesResponse.data.map(boutique => {
              let imageUrl = placeholderImage;
              
              // Gestion correcte des URLs des images de boutique
              if (boutique.boutique_img) {
                // Si l'image est juste un nom de fichier (ex: "boutique_1745146708489.jpg")
                if (!boutique.boutique_img.startsWith('http') && !boutique.boutique_img.startsWith('/')) {
                  imageUrl = `${baseUrl}/api/fichiers/${boutique.boutique_img}`;
                } else if (boutique.boutique_img.startsWith('http')) {
                  // Si c'est déjà une URL complète
                  imageUrl = boutique.boutique_img;
                } else if (boutique.boutique_img.startsWith('/')) {
                  // Si c'est un chemin absolu
                  imageUrl = `${baseUrl}${boutique.boutique_img}`;
                }
              }

              return {
                id: boutique.id_boutique,
                name: boutique.nom,
                distance: "100m",
                image: imageUrl,
                address: boutique.adress,
                hours: boutique.horaire || "9am - 6pm",
                ville: boutique.ville,
                contact: boutique.contact
              };
            });
            setStores(formattedStores);
          } catch (error) {
            console.error("Erreur lors du chargement des boutiques:", error);
          }

          // Charger les avis clients
          try {
            const reviewsResponse = await axios.get(`${baseUrl}/api/avis/produit/${id}`);
            const formattedReviews = reviewsResponse.data.map(review => ({
              id: review.id,
              name: review.nomClient,
              avatar: review.avatarUrl || placeholderImage,
              comment: review.commentaire,
              rating: review.note,
              date: review.dateCreation
            }));
            setReviews(formattedReviews);
          } catch (error) {
            console.error("Erreur lors du chargement des avis:", error);
            // Garder un tableau vide au lieu de définir des valeurs par défaut
            setReviews([]);
          }

          // Charger les produits connexes
          if (productData.idCategorie) {
            try {
              const relatedResponse = await axios.get(`${baseUrl}/api/produits/categorie/${productData.idCategorie}`);
              const relatedProductsList = relatedResponse.data
                .filter(p => p.id !== parseInt(id))
                .slice(0, 8);

              const formattedRelatedProducts = await Promise.all(
                relatedProductsList.map(async (p) => {
                  let prodImages = [];
                  try {
                    const imagesRes = await axios.get(`${baseUrl}/api/produits/${p.id}/images`);
                    prodImages = imagesRes.data;
                  } catch (error) {
                    console.error(`Erreur lors du chargement des images pour le produit ${p.id}:`, error);
                  }
                  
                  const mainImage = prodImages.find(img => img.imagePrincipale) || prodImages[0];
                  
                  let discount = null;
                  let originalPrice = null;
                  
                  if (p.idReduction) {
                    try {
                      const reductionRes = await axios.get(`${baseUrl}/api/reductions/${p.idReduction}`);
                      const reductionData = reductionRes.data;
                      
                      if (reductionData.pourcentage && reductionData.pourcentage > 0) {
                        discount = `${reductionData.pourcentage}%`;
                        originalPrice = p.prix / (1 - reductionData.pourcentage / 100);
                      }
                    } catch (error) {
                      console.error(`Erreur lors du chargement de la réduction pour le produit ${p.id}:`, error);
                    }
                  }
                  
                  // Utiliser à la fois nomProduit et name pour assurer la compatibilité
                  const relatedProduct = {
                    id: p.id,
                    nomProduit: p.nomProduit,
                    name: p.nomProduit,
                    originalPrice: originalPrice,
                    prixOriginal: originalPrice,
                    currentPrice: p.prix,
                    prix: p.prix,
                    discount: discount,
                    reduction: discount,
                    rating: 4.5,
                    reviewCount: 0,
                    imageUrl: mainImage ? mainImage.url : placeholderImage,
                  };
                  
                  // Normaliser pour garantir la cohérence
                  return normalizeProduct(relatedProduct);
                })
              );
              
              console.log("Produits connexes normalisés:", formattedRelatedProducts);
              setRelatedProducts(formattedRelatedProducts);
            } catch (error) {
              console.error("Erreur lors du chargement des produits connexes:", error);
            }
          }
        }
        
        setLoading(false);
      } catch (err) {
        console.error("Erreur lors du chargement des données:", err);
        setError("Impossible de charger les détails du produit");
        setLoading(false);
      }
    };

    fetchProductData();
  }, [id]);

  if (error) {
    return (
      <>
        <Navbar />
        <div className="max-w-6xl mx-auto px-4 py-8">
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
            <p>{error}</p>
          </div>
        </div>
        <Footer />
      </>
    );
  }

  return (
    <>
      <Navbar />
      {loading ? (
        <div className="py-12 text-center">
          <p>Loading product...</p>
        </div>
      ) : (
        <>
          <Product product={product} />
          
          {/* Utiliser soit description, soit detail pour afficher les informations */}
          <ProductFeatures description={product.description || product.detail || ""} />
          
          {/* Afficher les sliders seulement s'il y a des données */}
          {stores.length > 0 && <StoreLocationSlider stores={stores} />}
          {reviews.length > 0 && <CustomerReviewsSlider reviews={reviews} />}
          {relatedProducts.length > 0 && <RelatedProducts products={relatedProducts} />}
        </>
      )}
      <Footer />
    </>
  );
};

export default ProductPage;