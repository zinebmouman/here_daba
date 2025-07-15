// src/Components/OrderConfirmation.tsx
import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { CheckCircle, FileText, ShoppingBag, ChevronRight } from 'lucide-react';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import orderService, { OrderDetails } from '../api/orderService';

declare module 'jspdf' {
  interface jsPDF {
    autoTable: typeof autoTable;
  }
}

const OrderConfirmation = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { orderNumber } = useParams();
  const [order, setOrder] = useState<OrderDetails | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  useEffect(() => {
    const loadOrder = async () => {
      try {
        const orderDetails = location.state?.orderDetails;
        
        if (orderDetails) {
          setOrder(orderDetails);
        } else if (orderNumber) {
          const orderData = await orderService.getOrder(orderNumber);
          setOrder(orderData);
        } else {
          navigate('/');
        }
      } catch (err) {
        setError('Erreur lors du chargement de la commande');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    loadOrder();
  }, [location, navigate, orderNumber]);

  const generateInvoicePDF = () => {
    if (!order) return;
    
    try {
      const doc = new jsPDF();
      
      // En-tête
      doc.setFontSize(20);
      doc.text("Facture / Devis", 105, 20, { align: 'center' });
      
      // Informations de la commande
      doc.setFontSize(12);
      doc.text(`N° de commande: ${order.orderNumber}`, 14, 40);
      doc.text(`Date: ${new Date(order.createdAt).toLocaleDateString()}`, 14, 47);
      doc.text(`Mode de paiement: ${
        order.paymentMethod === 'card' ? 'Carte de crédit' : 'Paiement à la livraison'
      }`, 14, 54);
      
      // Informations client
      doc.text("Informations client:", 14, 68);
      doc.text(`${order.shipping.fullName}`, 14, 75);
      doc.text(`${order.shipping.address}`, 14, 82);
      doc.text(`${order.shipping.postalCode} ${order.shipping.city}`, 14, 89);
      doc.text(`${order.shipping.country}`, 14, 96);
      doc.text(`Tél: ${order.shipping.phone}`, 14, 103);
      doc.text(`Email: ${order.shipping.email}`, 14, 110);
      
      // Tableau des articles
      const tableColumn = ["Article", "Quantité", "Prix unitaire", "Total"];
      const tableRows = order.items.map(item => [
        item.nomProduit,
        item.quantite.toString(),
        `${item.prix.toFixed(2)} €`,
        `${(item.prix * item.quantite).toFixed(2)} €`
      ]);

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
      
      // Pied de page
      const finalY = (doc as any).lastAutoTable.finalY || 200;
      doc.text("Merci pour votre commande!", 105, finalY + 20, { align: 'center' });
      doc.text("Pour toute question, contactez notre service client", 105, finalY + 30, { align: 'center' });
      
      doc.save(`facture_${order.orderNumber}.pdf`);
    } catch (error) {
      console.error("Erreur lors de la génération du PDF:", error);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-teal-500"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-red-500">{error}</div>
      </div>
    );
  }

  if (!order) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-50 py-12">
      <div className="container mx-auto px-4 max-w-4xl">
        <div className="bg-white rounded-lg shadow-md p-8">
          <div className="mb-8 text-center">
            <div className="mx-auto h-16 w-16 bg-teal-100 rounded-full flex items-center justify-center">
              <CheckCircle className="h-10 w-10 text-teal-500" />
            </div>
            <h1 className="text-2xl font-bold mt-4 mb-2 text-teal-600">
              Merci pour votre commande !
            </h1>
            <p className="text-gray-600">
              {order.paymentMethod === 'cod' 
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
          
          <div className="mb-6">
            <h2 className="font-semibold text-lg mb-4">Articles commandés</h2>
            <div className="divide-y divide-gray-200">
              {order.items.map((item, index) => (
                <div key={index} className="flex py-4">
                  <div className="bg-gray-100 h-16 w-16 rounded-md flex items-center justify-center mr-4 flex-shrink-0">
                    {item.imageUrl ? (
                      <img 
                        src={item.imageUrl} 
                        alt={item.nomProduit}
                        className="w-full h-full object-cover rounded-md"
                        onError={(e) => {
                          (e.target as HTMLImageElement).style.display = 'none';
                          (e.target as HTMLImageElement).parentElement!.innerHTML = `
                            <div class="flex items-center justify-center h-full w-full">
                              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-gray-400"><path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z"></path><path d="M3 6h18"></path><path d="M16 10a4 4 0 0 1-8 0"></path></svg>
                            </div>
                          `;
                        }}
                      />
                    ) : (
                      <ShoppingBag className="text-gray-400 h-8 w-8" />
                    )}
                  </div>
                  <div className="flex-1">
                    <h3 className="font-medium">{item.nomProduit}</h3>
                    <p className="text-gray-500 text-sm">Quantité: {item.quantite}</p>
                  </div>
                  <div className="font-semibold">
                    {(item.prix * item.quantite).toFixed(2)} €
                  </div>
                </div>
              ))}
            </div>
          </div>
          
          <div className="bg-gray-50 p-4 rounded-lg mb-6">
            <h2 className="font-semibold text-lg mb-4">Récapitulatif</h2>
            <div className="space-y-2">
              <div className="flex justify-between">
                <span>Sous-total:</span>
                <span>{order.subtotal.toFixed(2)} €</span>
              </div>
              <div className="flex justify-between">
                <span>Frais de livraison:</span>
                <span>{order.shippingFee.toFixed(2)} €</span>
              </div>
              <div className="border-t border-gray-300 pt-2 mt-2">
                <div className="flex justify-between font-semibold">
                  <span>Total:</span>
                  <span>{order.total.toFixed(2)} €</span>
                </div>
              </div>
            </div>
          </div>
          
          <div className="flex flex-col sm:flex-row justify-between mt-8">
            <button
              onClick={generateInvoicePDF}
              className="mb-4 sm:mb-0 border border-gray-300 hover:bg-gray-50 text-gray-700 py-2.5 px-6 rounded-md flex items-center justify-center space-x-2 font-medium transition-colors"
            >
              <FileText size={18} />
              <span>Télécharger la facture</span>
            </button>
            
            <button
              onClick={() => navigate('/')}
              className="bg-teal-500 hover:bg-teal-600 text-white py-2.5 px-6 rounded-md flex items-center justify-center space-x-2 font-medium transition-colors"
            >
              <span>Continuer mes achats</span>
              <ChevronRight size={18} />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OrderConfirmation;