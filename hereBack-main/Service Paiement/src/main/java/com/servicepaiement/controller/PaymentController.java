package com.servicepaiement.controller;


import com.servicepaiement.dto.*;
import com.servicepaiement.service.PaymentService;

// Ajoutez ces imports Stripe
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;
    // Traiter un paiement par carte
    @PostMapping("/card")
    public ResponseEntity<PaymentResponseDTO> processCardPayment(
            @RequestBody CardPaymentRequestDTO request) {
        return ResponseEntity.ok(paymentService.processCardPayment(request));
    }
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        try {
            // Vérifier que la demande vient bien de Stripe
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);

            // Traiter l'événement en fonction de son type
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().get();
                    paymentService.handleSuccessfulPayment(paymentIntent.getId());
                    break;

                case "payment_intent.payment_failed":
                    paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().get();
                    paymentService.handleFailedPayment(paymentIntent.getId());
                    break;

                // Vous pouvez ajouter d'autres types d'événements ici
                default:
                    // Événement non traité
                    break;
            }

            return ResponseEntity.ok("Webhook processed successfully");
        } catch (SignatureVerificationException e) {
            // La signature est invalide, la demande n'est pas de Stripe
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook: " + e.getMessage());
        }
    }
    // Créer une commande PayPal
    @PostMapping("/paypal")
    public ResponseEntity<PaymentResponseDTO> createPayPalOrder(
            @RequestBody PaypalPaymentRequestDTO request) {
        return ResponseEntity.ok(paymentService.createPayPalOrder(request));
    }

    // Capturer un paiement PayPal après redirection
    @GetMapping("/paypal/success")
    public ResponseEntity<PaymentResponseDTO> capturePayPalPayment(
            @RequestParam("paymentId") String paymentId,
            @RequestParam("token") String token,
            @RequestParam("PayerID") String payerId) {
        return ResponseEntity.ok(paymentService.capturePayPalPayment(paymentId, payerId));
    }

    // Annuler un paiement PayPal
    @GetMapping("/paypal/cancel")
    public ResponseEntity<String> cancelPayPalPayment(
            @RequestParam("paymentId") String paymentId) {
        return ResponseEntity.ok("Paiement annulé");
    }

    // Traiter un paiement à la livraison
    @PostMapping("/cod")
    public ResponseEntity<PaymentResponseDTO> processCodPayment(
            @RequestBody CodPaymentRequestDTO request) {
        return ResponseEntity.ok(paymentService.processCodPayment(request));
    }

    // Vérifier le statut d'un paiement
    @GetMapping("/{paymentId}/status")
    public ResponseEntity<PaymentResponseDTO> checkPaymentStatus(
            @PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(paymentId));
    }

    // Récupérer l'historique des paiements d'un utilisateur
    @GetMapping("/user")
    public ResponseEntity<List<PaymentResponseDTO>> getUserPayments(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(paymentService.getUserPayments(userId));
    }
}
