package com.example.authentification.controller;

import com.example.authentification.dto.VendeurDTO;
import com.example.authentification.model.Vendeur;
import com.example.authentification.repository.VendeurRepository;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/vendeurs")
public class VendeurApiController {

    @Autowired
    private VendeurRepository vendeurRepository;

    @Autowired
    private Firestore firestore;

    @GetMapping("/{vendeurId}")
    public ResponseEntity<VendeurDTO> getVendeurById(@PathVariable String vendeurId) {
        try {
            // Rechercher d'abord dans PostgreSQL
            Optional<Vendeur> vendeurOpt = vendeurRepository.findByIdVendeur(vendeurId);

            if (vendeurOpt.isPresent()) {
                Vendeur vendeur = vendeurOpt.get();

                // Tenter de récupérer l'email depuis Firestore
                String email = "default@example.com";
                try {
                    DocumentSnapshot document = firestore.collection("users").document(vendeurId).get().get();
                    if (document.exists()) {
                        String firebaseEmail = document.getString("email");
                        System.out.println("📧 Email dans Firestore pour " + vendeurId + ": " + firebaseEmail);
                        if (firebaseEmail != null && !firebaseEmail.isEmpty()) {
                            email = firebaseEmail;
                        }
                    }
                } catch (Exception e) {
                    // Ignorer les erreurs Firestore et continuer avec l'email par défaut
                    System.out.println("Erreur lors de la récupération de l'email depuis Firestore: " + e.getMessage());
                }

                VendeurDTO dto = new VendeurDTO(
                        vendeur.getIdVendeur(),
                        vendeur.getRole(),
                        vendeur.getNom(),
                        email
                );
                return ResponseEntity.ok(dto);
            }

            // Si non trouvé dans PostgreSQL, vérifier Firestore
            try {
                DocumentSnapshot document = firestore.collection("users").document(vendeurId).get().get();
                if (document.exists() && "vendeur".equals(document.getString("role"))) {
                    String nom = document.getString("displayName");
                    String email = document.getString("email");

                    VendeurDTO dto = new VendeurDTO(
                            vendeurId,
                            "vendeur",
                            nom != null ? nom : "Vendeur Sans Nom",
                            email != null ? email : "default@example.com"
                    );
                    return ResponseEntity.ok(dto);
                }
            } catch (Exception e) {
                System.out.println("Erreur lors de la vérification dans Firestore: " + e.getMessage());
            }

            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.out.println("Erreur générale dans getVendeurById: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}