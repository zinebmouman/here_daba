import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { auth } from '../../config/Firebase';
import { 
  CreditCard, 
  ShoppingBag, 
  ChevronLeft, 
  ChevronRight, 
  Truck,
  Home,
  CheckCircle,
  AlertTriangle,
  FileText,
  X,
  Calendar,
  Lock
} from 'lucide-react';

// Importation correcte pour jsPDF et autoTable
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';

// Déclarer autoTable comme une propriété de jsPDF
declare module 'jspdf' {
  interface jsPDF {
    autoTable: typeof autoTable;
  }
}

interface CartItem {
  id: number;
  nomProduit: string;
  prix: number;
  imageUrl?: string;
  quantite: number;
  categorie?: string;
}

interface ShippingFormData {
  fullName: string;
  address: string;
  city: string;
  postalCode: string;
  country: string;
  phone: string;
  email: string;
}

// Formulaire de carte de crédit
interface CreditCardForm {
  cardNumber: string;
  cardHolder: string;
  expiryDate: string;
  cvv: string;
}

interface Order {
  orderNumber: string;
  createdAt: string;
  items: CartItem[];
  shipping: ShippingFormData;
  paymentMethod: string;
  subtotal: number;
  shippingFee: number;
  total: number;
}

// Modal de confirmation intégré - couleur verte et bouton OK
const ConfirmationModal = ({ 
  isOpen, 
  onClose, 
  onHomePage,
  paymentMethod
}: { 
  isOpen: boolean; 
  onClose: () => void; 
  onHomePage: () => void;
  paymentMethod: string;
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg max-w-xl w-full relative">
        {/* Bouton de fermeture */}
        <button 
          onClick={onClose}
          className="absolute top-4 right-4 text-gray-500 hover:text-gray-700"
        >
          <X size={24} />
        </button>

        {/* Contenu du modal - Couleur verte */}
        <div className="p-8 text-center">
          <div className="mb-6 text-center">
            <div className="mx-auto h-20 w-20 bg-teal-100 rounded-full flex items-center justify-center">
              <CheckCircle className="h-12 w-12 text-teal-500" />
            </div>
          </div>
          <h2 className="text-3xl font-bold mb-4 text-teal-600">FÉLICITATIONS</h2>
          <h3 className="text-xl mb-3">Votre commande a été confirmée.</h3>
          <p className="text-gray-600 mb-6">
            {paymentMethod === 'cod' 
              ? 'Votre commande a été enregistrée avec succès. Vous paierez à la livraison.' 
              : 'Votre paiement a été traité avec succès et votre commande est en cours de préparation.'}
          </p>

          {/* Bouton OK - couleur verte pour correspondre à l'application */}
          <button
            onClick={onClose}
            className="w-full bg-teal-500 hover:bg-teal-600 text-white py-3 rounded-md transition-colors"
          >
            OK
          </button>
        </div>
      </div>
    </div>
  );
};

const CART_STORAGE_KEY = 'HERE_SHOPPING_CART';
// URL de l'API backend
const API_BASE_URL = 'http://localhost:8080/api';

const Checkout: React.FC = () => {
  const navigate = useNavigate();
  const [cartItems, setCartItems] = useState<CartItem[]>([]);
  const [subtotal, setSubtotal] = useState<number>(0);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [userId, setUserId] = useState<string | null>(null);
  const [step, setStep] = useState<'shipping' | 'payment' | 'confirmation'>('shipping');
  const [paymentMethod, setPaymentMethod] = useState<'card' | 'paypal' | 'cod'>('card');
  const [notification, setNotification] = useState<{message: string; type: 'success' | 'error' | 'info'} | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [showConfirmationModal, setShowConfirmationModal] = useState(false);
  const [order, setOrder] = useState<Order | null>(null);
  
  // Formulaire de carte de crédit
  const [cardForm, setCardForm] = useState<CreditCardForm>({
    cardNumber: '',
    cardHolder: '',
    expiryDate: '',
    cvv: ''
  });
  
  // Validation du formulaire de carte
  const [cardFormErrors, setCardFormErrors] = useState<Record<string, string>>({});
  
  // Frais de livraison
  const deliveryFee = 5.99;
  
  // Formulaire d'adresse de livraison
  const [shippingForm, setShippingForm] = useState<ShippingFormData>({
    fullName: '',
    address: '',
    city: '',
    postalCode: '',
    country: 'Maroc',
    phone: '',
    email: ''
  });
  
  useEffect(() => {
    // Vérifier si l'utilisateur est connecté
    const unsubscribe = auth.onAuthStateChanged((user) => {
      if (user) {
        setUserId(user.uid);
        
        // Pré-remplir l'email de l'utilisateur
        if (user.email) {
          setShippingForm(prev => ({
            ...prev,
            email: user.email || ''
          }));
        }
        
        // Charger les articles du panier
        loadCartItems(user.uid);
      } else {
        // Rediriger vers la page de connexion si l'utilisateur n'est pas connecté
        navigate('/sign-in', { state: { redirect: '/checkout' } });
      }
      setIsLoading(false);
    });

    return () => unsubscribe();
  }, [navigate]);

  // Charger les articles du panier depuis localStorage
  const loadCartItems = (uid: string) => {
    const userCartKey = `${CART_STORAGE_KEY}_${uid}`;
    const cartJson = localStorage.getItem(userCartKey);
    
    if (cartJson) {
      try {
        const items = JSON.parse(cartJson);
        setCartItems(items);
        
        // Calculer le sous-total
        const total = items.reduce((sum: number, item: CartItem) => 
          sum + (item.prix * item.quantite), 0);
        setSubtotal(total);
      } catch (e) {
        console.error("Erreur lors du chargement du panier:", e);
        setCartItems([]);
        setSubtotal(0);
        showNotification('Erreur lors du chargement du panier', 'error');
      }
    } else {
      setCartItems([]);
      setSubtotal(0);
    }
  };

  // Afficher une notification
  const showNotification = (message: string, type: 'success' | 'error' | 'info') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 5000);
  };

  // Retourner au panier
  const goBackToCart = () => {
    navigate('/cart');
  };

  // Gérer les changements dans le formulaire d'adresse
  const handleShippingChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setShippingForm(prev => ({
      ...prev,
      [name]: value
    }));
  };
  
  // Gérer les changements dans le formulaire de carte
  const handleCardChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    let formattedValue = value;
    
    // Formatage spécifique selon le champ
    if (name === 'cardNumber') {
      // Retirer tout sauf les chiffres
      const numbersOnly = value.replace(/\D/g, '');
      // Formatter avec des espaces tous les 4 chiffres
      formattedValue = numbersOnly.replace(/(\d{4})(?=\d)/g, '$1 ').trim();
      // Limiter à 19 caractères (16 chiffres + 3 espaces)
      formattedValue = formattedValue.slice(0, 19);
    } 
    else if (name === 'expiryDate') {
      // Retirer tout sauf les chiffres
      const numbersOnly = value.replace(/\D/g, '');
      // Format MM/YY
      if (numbersOnly.length > 2) {
        formattedValue = `${numbersOnly.slice(0, 2)}/${numbersOnly.slice(2, 4)}`;
      } else {
        formattedValue = numbersOnly;
      }
    }
    else if (name === 'cvv') {
      // Limiter à 3-4 chiffres
      formattedValue = value.replace(/\D/g, '').slice(0, 4);
    }
    
    setCardForm(prev => ({
      ...prev,
      [name]: formattedValue
    }));
    
    // Effacer l'erreur si le champ a une valeur
    if (formattedValue) {
      setCardFormErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  // Valider le formulaire d'adresse
  const validateShippingForm = () => {
    // Vérification de tous les champs requis
    const { fullName, address, city, postalCode, country, phone, email } = shippingForm;
    
    if (!fullName || !address || !city || !postalCode || !country || !phone || !email) {
      showNotification('Veuillez remplir tous les champs', 'error');
      return false;
    }
    
    // Validation de l'email avec regex
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      showNotification('Adresse email invalide', 'error');
      return false;
    }
    
    return true;
  };
  
  // Valider le formulaire de carte
  const validateCardForm = () => {
    const errors: Record<string, string> = {};
    let isValid = true;
    
    // Vérification du numéro de carte
    if (!cardForm.cardNumber.trim()) {
      errors.cardNumber = 'Le numéro de carte est requis';
      isValid = false;
    } else if (cardForm.cardNumber.replace(/\s/g, '').length < 16) {
      errors.cardNumber = 'Numéro de carte invalide';
      isValid = false;
    }
    
    // Vérification du nom du titulaire
    if (!cardForm.cardHolder.trim()) {
      errors.cardHolder = 'Le nom du titulaire est requis';
      isValid = false;
    }
    
    // Vérification de la date d'expiration
    if (!cardForm.expiryDate.trim()) {
      errors.expiryDate = 'La date d\'expiration est requise';
      isValid = false;
    } else {
      const [month, year] = cardForm.expiryDate.split('/');
      const currentYear = new Date().getFullYear() % 100; // Obtenir les 2 derniers chiffres de l'année
      const currentMonth = new Date().getMonth() + 1; // Les mois commencent à 0
      
      if (!month || !year || month.length !== 2 || year.length !== 2) {
        errors.expiryDate = 'Format invalide (MM/YY)';
        isValid = false;
      } else if (parseInt(year) < currentYear || (parseInt(year) === currentYear && parseInt(month) < currentMonth)) {
        errors.expiryDate = 'Carte expirée';
        isValid = false;
      } else if (parseInt(month) < 1 || parseInt(month) > 12) {
        errors.expiryDate = 'Mois invalide';
        isValid = false;
      }
    }
    
    // Vérification du CVV
    if (!cardForm.cvv.trim()) {
      errors.cvv = 'Le code de sécurité est requis';
      isValid = false;
    } else if (cardForm.cvv.length < 3) {
      errors.cvv = 'Code de sécurité invalide';
      isValid = false;
    }
    
    setCardFormErrors(errors);
    
    if (!isValid) {
      showNotification('Veuillez corriger les erreurs dans le formulaire de paiement', 'error');
    }
    
    return isValid;
  };

  // Passer à l'étape suivante
  const nextStep = () => {
    if (step === 'shipping') {
      if (validateShippingForm()) {
        setStep('payment');
      }
    } else if (step === 'payment') {
      // Valider selon la méthode de paiement
      if (paymentMethod === 'card') {
        if (validateCardForm()) {
          processPayment();
        }
      } else {
        // Pour PayPal et paiement à la livraison, pas de validation supplémentaire
        processPayment();
      }
    }
  };

  // Étape précédente
  const prevStep = () => {
    if (step === 'payment') {
      setStep('shipping');
    }
  };

  // Traiter le paiement
  const processPayment = async () => {
    setIsProcessing(true);
    
    try {
      // Créer un objet commande
      const orderData = {
        userId: userId,
        items: cartItems,
        shipping: shippingForm,
        paymentMethod: paymentMethod,
        subtotal: subtotal,
        shippingFee: deliveryFee,
        total: subtotal + deliveryFee
      };
      
      // Ajouter les infos de carte pour la méthode de paiement par carte
      if (paymentMethod === 'card') {
        Object.assign(orderData, {
          cardDetails: {
            cardNumber: cardForm.cardNumber.replace(/\s/g, '').slice(-4), // Garder uniquement les 4 derniers chiffres
            cardHolder: cardForm.cardHolder,
            expiryDate: cardForm.expiryDate
          }
        });
      }
      
      // Envoyer la commande au backend
      let response;
      
      // Dans une véritable implémentation, décommentez ce code pour l'intégration avec le backend
      // if (paymentMethod === 'paypal') {
      //   response = await axios.post(`${API_BASE_URL}/orders/paypal`, orderData);
      //   window.location.href = response.data.redirectUrl;
      // } else if (paymentMethod === 'card') {
      //   response = await axios.post(`${API_BASE_URL}/orders/card`, orderData);
      // } else if (paymentMethod === 'cod') {
      //   response = await axios.post(`${API_BASE_URL}/orders/cod`, orderData);
      // }
      
      // Simuler une réponse de commande réussie
      simulateSuccessfulOrder(orderData);
    } catch (error) {
      console.error("Erreur lors du traitement du paiement:", error);
      showNotification('Une erreur est survenue lors du traitement du paiement', 'error');
      setIsProcessing(false);
    }
  };
  
  // Simuler une commande réussie (à remplacer par une vraie API)
  const simulateSuccessfulOrder = (orderData: any) => {
    // Créer un numéro de commande aléatoire
    const orderNumber = `ORD-${Math.floor(100000 + Math.random() * 900000)}`;
    
    // Créer un objet commande
    const newOrder: Order = {
      orderNumber: orderNumber,
      createdAt: new Date().toISOString(),
      items: cartItems,
      shipping: shippingForm,
      paymentMethod: paymentMethod,
      subtotal: subtotal,
      shippingFee: deliveryFee,
      total: subtotal + deliveryFee
    };
    
    // Mettre à jour l'état de la commande
    setOrder(newOrder);
    
    // Après "traitement", montrer le modal de confirmation
    setTimeout(() => {
      setIsProcessing(false);
      setShowConfirmationModal(true);
      clearCart();
    }, 2000);
  };
  
  // Vider le panier après commande
  const clearCart = () => {
    if (userId) {
      const userCartKey = `${CART_STORAGE_KEY}_${userId}`;
      localStorage.setItem(userCartKey, JSON.stringify([]));
      
      // Déclencher l'événement pour mettre à jour le compteur dans la navbar
      const cartUpdateEvent = new CustomEvent('cartUpdated', {
        detail: { userId }
      });
      window.dispatchEvent(cartUpdateEvent);
    }
  };

  // Générer un PDF de la facture/devis - Solution complète pour l'erreur autoTable
  const generateInvoicePDF = () => {
    if (!order) return;
    
    try {
      // Créer un nouveau document PDF
      const doc = new jsPDF();
      
      // Ajouter un titre
      doc.setFontSize(20);
      doc.text("Facture / Devis", 105, 20, { align: 'center' });
      
      // Ajouter informations de la commande
      doc.setFontSize(12);
      doc.text(`N° de commande: ${order.orderNumber}`, 14, 40);
      doc.text(`Date: ${new Date(order.createdAt).toLocaleDateString()}`, 14, 47);
      doc.text(`Mode de paiement: ${
        order.paymentMethod === 'card' ? 'Carte de crédit' :
        order.paymentMethod === 'paypal' ? 'PayPal' : 
        'Paiement à la livraison'
      }`, 14, 54);
      
      // Informations du client
      doc.text("Informations client:", 14, 68);
      doc.text(`${order.shipping.fullName}`, 14, 75);
      doc.text(`${order.shipping.address}`, 14, 82);
      doc.text(`${order.shipping.postalCode} ${order.shipping.city}`, 14, 89);
      doc.text(`${order.shipping.country}`, 14, 96);
      doc.text(`Tél: ${order.shipping.phone}`, 14, 103);
      doc.text(`Email: ${order.shipping.email}`, 14, 110);
      
      // Préparation des données pour le tableau
      const tableColumn = ["Article", "Quantité", "Prix unitaire", "Total"];
      const tableRows = order.items.map(item => [
        item.nomProduit,
        item.quantite.toString(),
        `${item.prix.toFixed(2)} €`,
        `${(item.prix * item.quantite).toFixed(2)} €`
      ]);

      // Appel de autoTable avec une alternative sécurisée
      try {
        (doc as any).autoTable({
          startY: 120,
          head: [tableColumn],
          body: tableRows,
          foot: [
            ['', '', 'Sous-total', `${order.subtotal.toFixed(2)} €`],
            ['', '', 'Frais de livraison', `${order.shippingFee.toFixed(2)} €`],
            ['', '', 'Total', `${order.total.toFixed(2)} €`]
          ],
          theme: 'striped',
          headStyles: { fillColor: [60, 179, 113] }
        });
        
        // Informations supplémentaires
        const finalY = (doc as any).lastAutoTable.finalY || 200;
        doc.text("Merci pour votre commande!", 105, finalY + 20, { align: 'center' });
        doc.text("Pour toute question, contactez notre service client", 105, finalY + 30, { align: 'center' });
      } catch (e) {
        console.error("Erreur avec autoTable, création d'un tableau simple", e);
        // En cas d'erreur avec autoTable, créer un tableau simple
        let y = 120;
        doc.setFontSize(10);
        
        // En-tête
        doc.setTextColor(255, 255, 255);
        doc.setFillColor(60, 179, 113);
        doc.rect(14, y, 182, 10, 'F');
        doc.text("Article", 20, y + 7);
        doc.text("Quantité", 80, y + 7);
        doc.text("Prix unitaire", 110, y + 7);
        doc.text("Total", 170, y + 7);
        y += 15;
        
        // Lignes
        doc.setTextColor(0, 0, 0);
        order.items.forEach(item => {
          doc.text(item.nomProduit, 20, y);
          doc.text(item.quantite.toString(), 80, y);
          doc.text(`${item.prix.toFixed(2)} €`, 110, y);
          doc.text(`${(item.prix * item.quantite).toFixed(2)} €`, 170, y);
          y += 10;
        });
        
        // Totaux
        y += 5;
        doc.text("Sous-total", 110, y);
        doc.text(`${order.subtotal.toFixed(2)} €`, 170, y);
        y += 10;
        doc.text("Frais de livraison", 110, y);
        doc.text(`${order.shippingFee.toFixed(2)} €`, 170, y);
        y += 10;
        doc.setFont("helvetica", "bold");
        doc.text("Total", 110, y);
        doc.text(`${order.total.toFixed(2)} €`, 170, y);
        
        // Informations supplémentaires
        y += 25;
        doc.setFont("helvetica", "normal");
        doc.text("Merci pour votre commande!", 105, y, { align: 'center' });
        doc.text("Pour toute question, contactez notre service client", 105, y + 10, { align: 'center' });
      }
      
      // Enregistrer le PDF
      doc.save(`facture_${order.orderNumber}.pdf`);
      showNotification('Facture téléchargée avec succès', 'success');
    } catch (error) {
      console.error("Erreur lors de la génération du PDF:", error);
      showNotification('Erreur lors de la génération de la facture', 'error');
    }
  };
  
  // Finaliser la commande et retourner à la page d'accueil
  const finishOrder = () => {
    setShowConfirmationModal(false);
    navigate('/');
  };
  
  // Gérer la fermeture du modal - maintenant on reste sur la même page
  const handleCloseModal = () => {
    setShowConfirmationModal(false);
    setStep('confirmation');
  };

  // Affichage du chargement
  if (isLoading) {
    return (
      <div className="container mx-auto p-8 flex justify-center items-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-teal-500"></div>
      </div>
    );
  }

  // Si le panier est vide, rediriger vers la page du panier
  if (cartItems.length === 0 && step !== 'confirmation' && !showConfirmationModal) {
    return (
      <div className="container mx-auto p-8 text-center">
        <div className="bg-white rounded-lg shadow-md p-8 max-w-md mx-auto">
          <ShoppingBag className="h-16 w-16 mx-auto text-gray-300 mb-4" />
          <h2 className="text-xl font-semibold mb-2">Votre panier est vide</h2>
          <p className="text-gray-500 mb-6">Vous devez ajouter des produits au panier avant de procéder au paiement.</p>
          <button
            onClick={() => navigate('/produits')}
            className="px-6 py-3 bg-teal-500 text-white rounded-full hover:bg-teal-600 transition-colors"
          >
            Explorer les produits
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-gray-50 min-h-screen">
      {/* Notification */}
      {notification && (
        <div 
          className={`fixed top-4 left-1/2 transform -translate-x-1/2 z-50 px-4 py-3 rounded-lg shadow-lg flex items-center space-x-2 max-w-md w-full ${
            notification.type === 'success' ? 'bg-green-100 text-green-800 border-l-4 border-green-500' : 
            notification.type === 'error' ? 'bg-red-100 text-red-800 border-l-4 border-red-500' :
            'bg-blue-100 text-blue-800 border-l-4 border-blue-500'
          }`}
        >
          {notification.type === 'success' ? (
            <CheckCircle className="h-5 w-5 text-green-500 flex-shrink-0" />
          ) : notification.type === 'error' ? (
            <AlertTriangle className="h-5 w-5 text-red-500 flex-shrink-0" />
          ) : (
            <CheckCircle className="h-5 w-5 text-blue-500 flex-shrink-0" />
          )}
          <p>{notification.message}</p>
        </div>
      )}
      
      {/* Modal de confirmation intégré - avec le paymentMethod passé */}
      <ConfirmationModal 
        isOpen={showConfirmationModal}
        onClose={handleCloseModal}
        onHomePage={finishOrder}
        paymentMethod={paymentMethod}
      />
      
      <div className="container mx-auto px-4 py-8">
        {/* En-tête */}
        <button 
          onClick={goBackToCart}
          className="text-teal-600 flex items-center hover:text-teal-700 mb-6"
        >
          <ChevronLeft className="h-5 w-5 mr-1" />
          Retour au panier
        </button>
        
        <h1 className="text-3xl font-bold mb-8">Finaliser votre commande</h1>
        
        {/* Indicateur d'étape */}
        <div className="flex items-center justify-center mb-10 relative">
          {/* Ligne de progression */}
          <div className="absolute top-1/2 w-full h-1 bg-gray-200 -z-10"></div>
          
          {/* Étape 1 */}
          <div className="flex-1 flex flex-col items-center">
            <div className={`rounded-full w-10 h-10 flex items-center justify-center text-lg font-medium ${
              step === 'shipping' ? 'bg-teal-500 text-white' : 'bg-gray-200 text-gray-600'
            }`}>
              1
            </div>
            <span className={`mt-2 ${step === 'shipping' ? 'text-teal-600 font-medium' : 'text-gray-500'}`}>
              Livraison
            </span>
          </div>
          
          {/* Ligne de progression active */}
          <div className={`h-1 flex-grow ${
            step === 'shipping' ? 'bg-gray-200' : 'bg-teal-500'
          } max-w-[100px]`}></div>
          
          {/* Étape 2 */}
          <div className="flex-1 flex flex-col items-center">
            <div className={`rounded-full w-10 h-10 flex items-center justify-center text-lg font-medium ${
              step === 'payment' ? 'bg-teal-500 text-white' : 'bg-gray-200 text-gray-600'
            }`}>
              2
            </div>
            <span className={`mt-2 ${step === 'payment' ? 'text-teal-600 font-medium' : 'text-gray-500'}`}>
              Paiement
            </span>
          </div>
          
          {/* Ligne de progression active */}
          <div className={`h-1 flex-grow ${
            step === 'confirmation' ? 'bg-teal-500' : 'bg-gray-200'
          } max-w-[100px]`}></div>
          
          {/* Étape 3 */}
          <div className="flex-1 flex flex-col items-center">
            <div className={`rounded-full w-10 h-10 flex items-center justify-center text-lg font-medium ${
              step === 'confirmation' ? 'bg-teal-500 text-white' : 'bg-gray-200 text-gray-600'
            }`}>
              3
            </div>
            <span className={`mt-2 ${step === 'confirmation' ? 'text-teal-600 font-medium' : 'text-gray-500'}`}>
              Confirmation
            </span>
          </div>
        </div>
        
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Formulaire de commande */}
          <div className="lg:col-span-2">
            {/* Étape 1: Adresse de livraison */}
            {step === 'shipping' && (
              <div className="bg-white rounded-lg shadow-sm p-6">
                <h2 className="text-xl font-semibold mb-6">Adresse de livraison</h2>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="col-span-2">
                    <label className="block text-sm font-medium text-gray-700 mb-1">Nom complet*</label>
                    <input 
                      type="text"
                      name="fullName" 
                      value={shippingForm.fullName}
                      onChange={handleShippingChange}
                      className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-teal-500"
                      placeholder="John Doe"
                      required
                    />
                  </div>
                  
                  <div className="col-span-2">
                    <label className="block text-sm font-medium text-gray-700 mb-1">Adresse*</label>
                    <input 
                      type="text"
                      name="address" 
                      value={shippingForm.address}
                      onChange={handleShippingChange}
                      className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-teal-500"
                      placeholder="123 Rue de l'Exemple"
                      required
                    />
                  </div>
                  
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Ville*</label>
                    <input 
                      type="text"
                      name="city" 
                      value={shippingForm.city}
                      onChange={handleShippingChange}
                      className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-teal-500"
                      placeholder="Paris"
                      required
                    />
                  </div>
                  
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Code postal*</label>
                    <input 
                      type="text"
                      name="postalCode" 
                      value={shippingForm.postalCode}
                      onChange={handleShippingChange}
                      className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-teal-500"
                      placeholder="75001"
                      required
                    />
                  </div>
                  
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Pays*</label>
                    <select 
                      name="country" 
                      value={shippingForm.country}
                      onChange={handleShippingChange}
                      className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-teal-500"
                      required
                    >
                      <option value="Maroc">Maroc</option>
                      <option value="France">France</option>
                      <option value="Belgique">Belgique</option>
                      <option value="Suisse">Suisse</option>
                      <option value="Canada">Canada</option>
                    </select>
                  </div>
                  
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Téléphone*</label>
                    <input 
                      type="text"
                      name="phone" 
                      value={shippingForm.phone}
                      onChange={handleShippingChange}
                      className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-teal-500"
                      placeholder="06 12 34 56 78"
                      required
                    />
                  </div>
                  
                  <div className="col-span-2">
                    <label className="block text-sm font-medium text-gray-700 mb-1">Email*</label>
                    <input 
                      type="email"
                      name="email" 
                      value={shippingForm.email}
                      onChange={handleShippingChange}
                      className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-teal-500"
                      placeholder="example@email.com"
                      required
                    />
                  </div>
                </div>
                
                <div className="mt-8 flex justify-end">
                  <button
                    onClick={nextStep}
                    className="bg-teal-500 hover:bg-teal-600 text-white py-2.5 px-6 rounded-md flex items-center space-x-1 font-medium transition-colors"
                  >
                    <span>Continuer vers le paiement</span>
                    <ChevronRight size={16} />
                  </button>
                </div>
              </div>
            )}
            
            {/* Étape 2: Paiement */}
            {step === 'payment' && (
              <div className="bg-white rounded-lg shadow-sm p-6">
                <h2 className="text-xl font-semibold mb-6">Méthode de paiement</h2>
                
                <div className="space-y-4 mb-6">
                  {/* Option Carte de crédit */}
                  <div 
                    className={`border rounded-md p-4 cursor-pointer ${
                      paymentMethod === 'card' ? 'border-teal-500 bg-teal-50' : 'border-gray-200'
                    }`}
                    onClick={() => setPaymentMethod('card')}
                  >
                    <div className="flex items-center">
                      <div className={`rounded-full w-5 h-5 border-2 flex items-center justify-center ${
                        paymentMethod === 'card' ? 'border-teal-500' : 'border-gray-300'
                      }`}>
                        {paymentMethod === 'card' && (
                          <div className="w-3 h-3 bg-teal-500 rounded-full"></div>
                        )}
                      </div>
                      <div className="ml-3 flex items-center">
                        <CreditCard className="text-gray-600 mr-2" size={20} />
                        <span className="font-medium">Carte de crédit</span>
                      </div>
                    </div>
                    
                    {/* Formulaire de carte de crédit - visible uniquement quand sélectionné */}
                    {paymentMethod === 'card' && (
                      <div className="mt-4 border-t pt-4 pl-8">
                        <div className="bg-white p-4 rounded-md space-y-4">
                          {/* Numéro de carte */}
                          <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">
                              Numéro de carte*
                            </label>
                            <div className="relative">
                              <input
                                type="text"
                                name="cardNumber"
                                value={cardForm.cardNumber}
                                onChange={handleCardChange}
                                className={`w-full border ${cardFormErrors.cardNumber ? 'border-red-300' : 'border-gray-300'} rounded-md px-3 py-2 pl-10 focus:outline-none focus:ring-2 focus:ring-teal-500`}
                                placeholder="1234 5678 9012 3456"
                                maxLength={19}
                                required
                              />
                              <CreditCard className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={16} />
                            </div>
                            {cardFormErrors.cardNumber && (
                              <p className="text-red-500 text-xs mt-1">{cardFormErrors.cardNumber}</p>
                            )}
                          </div>
                          
                          {/* Titulaire de la carte */}
                          <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">
                              Titulaire de la carte*
                            </label>
                            <input
                              type="text"
                              name="cardHolder"
                              value={cardForm.cardHolder}
                              onChange={handleCardChange}
                              className={`w-full border ${cardFormErrors.cardHolder ? 'border-red-300' : 'border-gray-300'} rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-teal-500`}
                              placeholder="JOHN DOE"
                              required
                            />
                            {cardFormErrors.cardHolder && (
                              <p className="text-red-500 text-xs mt-1">{cardFormErrors.cardHolder}</p>
                            )}
                          </div>
                          
                          <div className="grid grid-cols-2 gap-4">
                            {/* Date d'expiration */}
                            <div>
                              <label className="block text-sm font-medium text-gray-700 mb-1">
                                Date d'expiration*
                              </label>
                              <div className="relative">
                                <input
                                  type="text"
                                  name="expiryDate"
                                  value={cardForm.expiryDate}
                                  onChange={handleCardChange}
                                  className={`w-full border ${cardFormErrors.expiryDate ? 'border-red-300' : 'border-gray-300'} rounded-md px-3 py-2 pl-10 focus:outline-none focus:ring-2 focus:ring-teal-500`}
                                  placeholder="MM/YY"
                                  maxLength={5}
                                  required
                                />
                                <Calendar className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={16} />
                              </div>
                              {cardFormErrors.expiryDate && (
                                <p className="text-red-500 text-xs mt-1">{cardFormErrors.expiryDate}</p>
                              )}
                            </div>
                            
                            {/* CVC */}
                            <div>
                              <label className="block text-sm font-medium text-gray-700 mb-1">
                                Code de sécurité*
                              </label>
                              <div className="relative">
                                <input
                                  type="text"
                                  name="cvv"
                                  value={cardForm.cvv}
                                  onChange={handleCardChange}
                                  className={`w-full border ${cardFormErrors.cvv ? 'border-red-300' : 'border-gray-300'} rounded-md px-3 py-2 pl-10 focus:outline-none focus:ring-2 focus:ring-teal-500`}
                                  placeholder="123"
                                  maxLength={4}
                                  required
                                />
                                <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={16} />
                              </div>
                              {cardFormErrors.cvv && (
                                <p className="text-red-500 text-xs mt-1">{cardFormErrors.cvv}</p>
                              )}
                            </div>
                          </div>
                          
                          <div className="flex items-center text-gray-600 text-sm mt-2">
                            <Lock className="h-4 w-4 text-gray-500 mr-2" />
                            <span>Toutes les informations de paiement sont sécurisées et cryptées</span>
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                  
                  {/* Option PayPal */}
                  <div 
                    className={`border rounded-md p-4 cursor-pointer ${
                      paymentMethod === 'paypal' ? 'border-teal-500 bg-teal-50' : 'border-gray-200'
                    }`}
                    onClick={() => setPaymentMethod('paypal')}
                  >
                    <div className="flex items-center">
                      <div className={`rounded-full w-5 h-5 border-2 flex items-center justify-center ${
                        paymentMethod === 'paypal' ? 'border-teal-500' : 'border-gray-300'
                      }`}>
                        {paymentMethod === 'paypal' && (
                          <div className="w-3 h-3 bg-teal-500 rounded-full"></div>
                        )}
                      </div>
                      <div className="ml-3">
                        <span className="font-medium">PayPal</span>
                      </div>
                    </div>
                    
                    {/* Message PayPal - visible uniquement quand sélectionné */}
                    {paymentMethod === 'paypal' && (
                      <div className="mt-4 pl-8">
                        <div className="bg-gray-50 p-4 rounded-md text-center">
                          <img 
                            src="https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_111x69.jpg" 
                            alt="PayPal" 
                            className="h-10 mx-auto mb-4"
                          />
                          <p className="text-gray-600">
                            Vous serez redirigé vers PayPal pour finaliser votre paiement en toute sécurité.
                          </p>
                        </div>
                      </div>
                    )}
                  </div>
                  
                  {/* Option Paiement à la livraison */}
                  <div 
                    className={`border rounded-md p-4 cursor-pointer ${
                      paymentMethod === 'cod' ? 'border-teal-500 bg-teal-50' : 'border-gray-200'
                    }`}
                    onClick={() => setPaymentMethod('cod')}
                  >
                    <div className="flex items-center">
                      <div className={`rounded-full w-5 h-5 border-2 flex items-center justify-center ${
                        paymentMethod === 'cod' ? 'border-teal-500' : 'border-gray-300'
                      }`}>
                        {paymentMethod === 'cod' && (
                          <div className="w-3 h-3 bg-teal-500 rounded-full"></div>
                        )}
                      </div>
                      <div className="ml-3 flex items-center">
                        <Home className="text-gray-600 mr-2" size={20} />
                        <span className="font-medium">Paiement à la livraison</span>
                      </div>
                    </div>
                    
                    {paymentMethod === 'cod' && (
                      <div className="mt-2 ml-8 text-sm text-gray-600">
                        Vous paierez en espèces ou par carte lors de la livraison.
                      </div>
                    )}
                  </div>
                </div>
                
                <div className="mt-8 flex justify-between">
                  <button
                    onClick={prevStep}
                    className="border border-gray-300 hover:bg-gray-50 text-gray-700 py-2.5 px-6 rounded-md flex items-center space-x-1 font-medium transition-colors"
                  >
                    <ChevronLeft size={16} />
                    <span>Retour</span>
                  </button>
                  
                  <button
                    onClick={nextStep}
                    disabled={isProcessing}
                    className={`bg-teal-500 hover:bg-teal-600 text-white py-2.5 px-6 rounded-md flex items-center space-x-2 font-medium transition-colors ${
                      isProcessing ? 'opacity-70 cursor-not-allowed' : ''
                    }`}
                  >
                    {isProcessing ? (
                      <>
                        <div className="animate-spin h-5 w-5 border-2 border-white border-r-transparent rounded-full mr-2"></div>
                        <span>Traitement en cours...</span>
                      </>
                    ) : (
                      <>
                        <span>Payer maintenant</span>
                        <ChevronRight size={16} />
                      </>
                    )}
                  </button>
                </div>
              </div>
            )}
            
            {/* Étape 3: Confirmation */}
            {step === 'confirmation' && !showConfirmationModal && order && (
              <div className="bg-white rounded-lg shadow-sm p-6">
                <div className="mb-6 text-center">
                  <div className="mx-auto h-16 w-16 bg-teal-100 rounded-full flex items-center justify-center">
                    <CheckCircle className="h-10 w-10 text-teal-500" />
                  </div>
                  <h2 className="text-2xl font-bold mt-4 mb-2 text-teal-600">Merci pour votre commande !</h2>
                  <p className="text-gray-600">
                    {paymentMethod === 'cod' 
                      ? 'Votre commande a été enregistrée avec succès. Vous paierez à la livraison.' 
                      : 'Votre paiement a été traité avec succès et votre commande est en cours de préparation.'}
                  </p>
                </div>
                
                <div className="border-t border-b border-gray-200 py-4 my-6">
                  <div className="flex justify-between items-center mb-4">
                    <span className="font-medium">Numéro de commande:</span>
                    <span className="text-teal-600 font-medium">{order.orderNumber}</span>
                  </div>
                  
                  <div className="flex justify-between items-center">
                    <span className="font-medium">Date:</span>
                    <span>{new Date(order.createdAt).toLocaleDateString()}</span>
                  </div>
                </div>
                
                <div className="space-y-3 mb-6">
                  <h3 className="font-medium">Détails d'expédition:</h3>
                  <p>
                    {order.shipping.fullName}<br />
                    {order.shipping.address}<br />
                    {order.shipping.postalCode} {order.shipping.city}<br />
                    {order.shipping.country}
                  </p>
                  
                  <div className="flex items-center text-teal-600 mt-4">
                    <Truck className="mr-2" size={16} />
                    <span>Livraison estimée sous 3-5 jours ouvrables</span>
                  </div>
                </div>
                
                <div className="mt-8 flex justify-between">
                  <button
                    onClick={generateInvoicePDF}
                    className="border border-gray-300 hover:bg-gray-50 text-gray-700 py-2.5 px-6 rounded-md flex items-center space-x-2 font-medium transition-colors"
                  >
                    <FileText size={16} />
                    <span>Télécharger Facture</span>
                  </button>
                  
                  <button
                    onClick={finishOrder}
                    className="bg-teal-500 hover:bg-teal-600 text-white py-2.5 px-6 rounded-md font-medium transition-colors"
                  >
                    Continuer mes achats
                  </button>
                </div>
              </div>
            )}
          </div>
          
          {/* Récapitulatif de commande */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg shadow-sm p-6 sticky top-8">
              <h2 className="text-xl font-semibold mb-4">Récapitulatif de la commande</h2>
              
              <div className="max-h-72 overflow-y-auto mb-4">
                {cartItems.map((item) => (
                  <div key={item.id} className="flex items-center py-3 border-b border-gray-100 last:border-b-0">
                    <div className="bg-gray-100 h-16 w-16 rounded-md flex items-center justify-center mr-3 flex-shrink-0 overflow-hidden">
                      {item.imageUrl ? (
                        <img 
                          src={item.imageUrl} 
                          alt={item.nomProduit}
                          className="w-full h-full object-cover"
                          onError={(e) => {
                            (e.target as HTMLImageElement).onerror = null;
                            (e.target as HTMLImageElement).style.display = 'none';
                            const parent = (e.target as HTMLElement).parentNode;
                            if (parent) {
                              const icon = document.createElement('div');
                              icon.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-gray-400"><path d="M2 22 22 2M2 2l20 20M10 9V5.5a2.5 2.5 0 0 1 5 0V9"/><path d="M12 13V6.5M8.5 13h7M8.5 17h7"/></svg>';
                              icon.className = 'flex items-center justify-center h-full w-full';
                              parent.appendChild(icon);
                            }
                          }}
                        />
                      ) : (
                        <ShoppingBag className="text-gray-400 h-8 w-8" />
                      )}
                    </div>
                    <div className="flex-1 min-w-0">
                      <h3 className="font-medium text-sm truncate">{item.nomProduit}</h3>
                      <p className="text-gray-500 text-xs">Qté: {item.quantite}</p>
                      <p className="text-teal-600 font-medium mt-1">{(item.prix * item.quantite).toFixed(2)} €</p>
                    </div>
                  </div>
                ))}
              </div>
              
              <div className="space-y-3 mb-6">
                <div className="flex justify-between text-gray-600">
                  <span>Sous-total</span>
                  <span>{subtotal.toFixed(2)} €</span>
                </div>
                
                <div className="flex justify-between text-gray-600">
                  <span>Frais de livraison</span>
                  <span>{deliveryFee.toFixed(2)} €</span>
                </div>
                
                <div className="border-t pt-3 mt-3"></div>
                <div className="flex justify-between font-semibold text-lg">
                  <span>Total</span>
                  <span>{(subtotal + deliveryFee).toFixed(2)} €</span>
                </div>
              </div>
              
              {step === 'shipping' && (
                <div className="bg-gray-50 rounded-lg p-4 text-sm">
                  <div className="flex items-start">
                    <Truck className="h-5 w-5 text-teal-500 mr-2 flex-shrink-0 mt-0.5" />
                    <p className="text-gray-600">
                      Livraison disponible partout au Maroc et à l'international. Les délais de livraison estimés sont de 3-5 jours ouvrables.
                    </p>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Checkout;