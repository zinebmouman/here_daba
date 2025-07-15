package com.servicepaiement.service;

import com.servicepaiement.dto.*;
import com.servicepaiement.exception.PaymentProcessingException;
import com.servicepaiement.exception.ResourceNotFoundException;
import com.servicepaiement.model.*;
import com.servicepaiement.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RabbitMQSender rabbitMQSender;

    /**
     * Traite un paiement par carte
     */
    @Transactional
    public PaymentResponseDTO processCardPayment(CardPaymentRequestDTO request) {
        try {
            // Créer la commande d'abord
            String orderNumber = orderService.generateOrderNumber();
            Order order = orderService.createInitialOrder(
                    request.getPaymentRequest().getUserId(),
                    orderNumber,
                    request.getPaymentRequest().getItems(),
                    request.getPaymentRequest().getTotal()
            );

            // Simuler le traitement du paiement (à remplacer par une intégration réelle avec Stripe)
            Payment payment = new Payment();
            payment.setPaymentId("card_" + UUID.randomUUID().toString());
            payment.setUserId(request.getPaymentRequest().getUserId());
            payment.setTransactionId("tr_" + UUID.randomUUID().toString());
            payment.setStatus(PaymentStatus.COMPLETED); // Simulation de succès
            payment.setPaymentMethod(PaymentMethod.CARD);
            payment.setAmount(request.getPaymentRequest().getTotal());
            payment.setCurrency("MAD");
            payment.setOrder(order);
            payment.setCreatedAt(LocalDateTime.now());

            Payment savedPayment = paymentRepository.save(payment);

            // Mettre à jour le statut de la commande
            order.setStatus(OrderStatus.PROCESSING);
            order.setPaymentMethod(PaymentMethod.CARD);

            // Envoyer un événement pour notifier de la commande
            rabbitMQSender.sendOrderEvent(order);

            // Créer la réponse
            PaymentResponseDTO response = new PaymentResponseDTO();
            response.setPaymentId(savedPayment.getPaymentId());
            response.setOrderNumber(order.getOrderNumber());
            response.setStatus(savedPayment.getStatus());
            response.setMethod(savedPayment.getPaymentMethod());
            response.setAmount(savedPayment.getAmount());
            response.setCurrency(savedPayment.getCurrency());
            response.setCreatedAt(savedPayment.getCreatedAt());
            response.setMessage("Paiement traité avec succès");

            return response;

        } catch (Exception e) {
            throw new PaymentProcessingException("Erreur lors du traitement du paiement par carte: " + e.getMessage(), e);
        }
    }
    /**
     * Traite un paiement réussi reçu via webhook
     */
    @Transactional
    public void handleSuccessfulPayment(String paymentIntentId) {
        try {
            // Pour une intégration réelle, vous récupéreriez le payment intent de Stripe
            // PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            // Trouver le paiement correspondant dans notre base de données
            // Dans une implémentation réelle, vous auriez stocké le paymentIntentId
            // Ici nous simulons une recherche
            System.out.println("Traitement du paiement réussi pour payment intent: " + paymentIntentId);

            // Si vous avez un mapping entre payment intent et vos paiements:
            // Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
            //        .orElse(null);

            // Si vous n'avez pas encore ce mapping, vous pouvez simuler:
            List<Payment> pendingPayments = paymentRepository.findAll().stream()
                    .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                    .collect(Collectors.toList());

            if (!pendingPayments.isEmpty()) {
                Payment payment = pendingPayments.get(0); // Simuler une correspondance
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(paymentIntentId);
                payment.setUpdatedAt(LocalDateTime.now());

                Payment updatedPayment = paymentRepository.save(payment);

                // Mettre à jour la commande associée
                Order order = updatedPayment.getOrder();
                if (order != null) {
                    order.setStatus(OrderStatus.PROCESSING);
                    // Envoyer événement de mise à jour
                    rabbitMQSender.sendOrderEvent(order);
                }

                System.out.println("Paiement mis à jour avec succès: " + payment.getPaymentId());
            } else {
                System.out.println("Aucun paiement en attente trouvé pour traitement");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement du paiement réussi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Traite un paiement échoué reçu via webhook
     */
    @Transactional
    public void handleFailedPayment(String paymentIntentId) {
        try {
            // Similaire à handleSuccessfulPayment mais pour les échecs
            System.out.println("Traitement du paiement échoué pour payment intent: " + paymentIntentId);

            List<Payment> pendingPayments = paymentRepository.findAll().stream()
                    .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                    .collect(Collectors.toList());

            if (!pendingPayments.isEmpty()) {
                Payment payment = pendingPayments.get(0); // Simuler une correspondance
                payment.setStatus(PaymentStatus.FAILED);
                payment.setTransactionId(paymentIntentId);
                payment.setUpdatedAt(LocalDateTime.now());

                Payment updatedPayment = paymentRepository.save(payment);

                // Mettre à jour la commande associée
                Order order = updatedPayment.getOrder();
                if (order != null) {
                    // Mettre à jour le statut de la commande
                    order.setStatus(OrderStatus.PENDING); // ou autre statut approprié
                    // Envoyer événement de mise à jour
                    rabbitMQSender.sendOrderEvent(order);
                }

                System.out.println("Paiement marqué comme échoué: " + payment.getPaymentId());
            } else {
                System.out.println("Aucun paiement en attente trouvé pour traitement d'échec");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement du paiement échoué: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * Crée une commande PayPal (redirection)
     */
    @Transactional
    public PaymentResponseDTO createPayPalOrder(PaypalPaymentRequestDTO request) {
        try {
            // Créer la commande d'abord
            String orderNumber = orderService.generateOrderNumber();
            Order order = orderService.createInitialOrder(
                    request.getPaymentRequest().getUserId(),
                    orderNumber,
                    request.getPaymentRequest().getItems(),
                    request.getPaymentRequest().getTotal()
            );

            // Simuler le traitement du paiement PayPal (à remplacer par une intégration réelle)
            Payment payment = new Payment();
            payment.setPaymentId("paypal_" + UUID.randomUUID().toString());
            payment.setUserId(request.getPaymentRequest().getUserId());
            payment.setStatus(PaymentStatus.PENDING); // En attente de la redirection
            payment.setPaymentMethod(PaymentMethod.PAYPAL);
            payment.setAmount(request.getPaymentRequest().getTotal());
            payment.setCurrency("MAD");
            payment.setOrder(order);
            payment.setCreatedAt(LocalDateTime.now());

            Payment savedPayment = paymentRepository.save(payment);

            // Créer la réponse avec URL de redirection
            PaymentResponseDTO response = new PaymentResponseDTO();
            response.setPaymentId(savedPayment.getPaymentId());
            response.setOrderNumber(order.getOrderNumber());
            response.setStatus(savedPayment.getStatus());
            response.setMethod(savedPayment.getPaymentMethod());
            response.setAmount(savedPayment.getAmount());
            response.setCurrency(savedPayment.getCurrency());
            response.setCreatedAt(savedPayment.getCreatedAt());
            response.setRedirectUrl("http://localhost:8080/api/payments/paypal/success?paymentId=" + savedPayment.getPaymentId());
            response.setMessage("Redirection vers PayPal");

            return response;

        } catch (Exception e) {
            throw new PaymentProcessingException("Erreur lors de la création de la commande PayPal: " + e.getMessage(), e);
        }
    }

    /**
     * Capture un paiement PayPal après redirection
     */
    @Transactional
    public PaymentResponseDTO capturePayPalPayment(String paymentId, String payerId) {
        try {
            Payment payment = paymentRepository.findByPaymentId(paymentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Paiement non trouvé avec l'ID: " + paymentId));

            // Simuler la capture du paiement (à remplacer par une intégration réelle)
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setTransactionId("paypal_" + UUID.randomUUID().toString());
            payment.setUpdatedAt(LocalDateTime.now());

            Payment savedPayment = paymentRepository.save(payment);

            // Mettre à jour le statut de la commande
            Order order = savedPayment.getOrder();
            order.setStatus(OrderStatus.PROCESSING);

            // Envoyer un événement pour notifier de la commande
            rabbitMQSender.sendOrderEvent(order);

            // Créer la réponse
            PaymentResponseDTO response = new PaymentResponseDTO();
            response.setPaymentId(savedPayment.getPaymentId());
            response.setOrderNumber(order.getOrderNumber());
            response.setStatus(savedPayment.getStatus());
            response.setMethod(savedPayment.getPaymentMethod());
            response.setAmount(savedPayment.getAmount());
            response.setCurrency(savedPayment.getCurrency());
            response.setCreatedAt(savedPayment.getCreatedAt());
            response.setMessage("Paiement PayPal traité avec succès");

            return response;

        } catch (Exception e) {
            throw new PaymentProcessingException("Erreur lors de la capture du paiement PayPal: " + e.getMessage(), e);
        }
    }

    /**
     * Traite un paiement à la livraison
     */
    @Transactional
    public PaymentResponseDTO processCodPayment(CodPaymentRequestDTO request) {
        try {
            // Créer la commande d'abord
            String orderNumber = orderService.generateOrderNumber();
            Order order = orderService.createInitialOrder(
                    request.getPaymentRequest().getUserId(),
                    orderNumber,
                    request.getPaymentRequest().getItems(),
                    request.getPaymentRequest().getTotal()
            );

            // Créer un enregistrement de paiement en attente
            Payment payment = new Payment();
            payment.setPaymentId("cod_" + UUID.randomUUID().toString());
            payment.setUserId(request.getPaymentRequest().getUserId());
            payment.setStatus(PaymentStatus.PENDING); // Sera payé à la livraison
            payment.setPaymentMethod(PaymentMethod.COD);
            payment.setAmount(request.getPaymentRequest().getTotal());
            payment.setCurrency("MAD");
            payment.setOrder(order);
            payment.setCreatedAt(LocalDateTime.now());

            Payment savedPayment = paymentRepository.save(payment);

            // Mettre à jour le statut de la commande
            order.setStatus(OrderStatus.PROCESSING);
            order.setPaymentMethod(PaymentMethod.COD);

            // Envoyer un événement pour notifier de la commande
            rabbitMQSender.sendOrderEvent(order);

            // Créer la réponse
            PaymentResponseDTO response = new PaymentResponseDTO();
            response.setPaymentId(savedPayment.getPaymentId());
            response.setOrderNumber(order.getOrderNumber());
            response.setStatus(savedPayment.getStatus());
            response.setMethod(savedPayment.getPaymentMethod());
            response.setAmount(savedPayment.getAmount());
            response.setCurrency(savedPayment.getCurrency());
            response.setCreatedAt(savedPayment.getCreatedAt());
            response.setMessage("Commande créée - Paiement à la livraison");

            return response;

        } catch (Exception e) {
            throw new PaymentProcessingException("Erreur lors du traitement du paiement à la livraison: " + e.getMessage(), e);
        }
    }

    /**
     * Récupère le statut d'un paiement
     */
    public PaymentResponseDTO getPaymentStatus(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement non trouvé avec l'ID: " + paymentId));

        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setPaymentId(payment.getPaymentId());
        response.setOrderNumber(payment.getOrder().getOrderNumber());
        response.setStatus(payment.getStatus());
        response.setMethod(payment.getPaymentMethod());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setCreatedAt(payment.getCreatedAt());

        return response;
    }

    /**
     * Récupère l'historique des paiements d'un utilisateur
     */
    public List<PaymentResponseDTO> getUserPayments(String userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);

        return payments.stream()
                .map(payment -> {
                    PaymentResponseDTO dto = new PaymentResponseDTO();
                    dto.setPaymentId(payment.getPaymentId());
                    dto.setOrderNumber(payment.getOrder().getOrderNumber());
                    dto.setStatus(payment.getStatus());
                    dto.setMethod(payment.getPaymentMethod());
                    dto.setAmount(payment.getAmount());
                    dto.setCurrency(payment.getCurrency());
                    dto.setCreatedAt(payment.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}