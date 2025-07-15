package com.boutique_catalogue_produits.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private static final Logger logger = LoggerFactory.getLogger(DebugController.class);

    @GetMapping("/test")
    public ResponseEntity<String> testAccess() {
        logger.info("Test d'accès réussi");
        return ResponseEntity.ok("Accès autorisé");
    }
    @GetMapping("/test-categories")
    public ResponseEntity<String> testCategories() {
        return ResponseEntity.ok("Categories endpoint is accessible");
    }
    @PostMapping("/echo")
    public ResponseEntity<Object> echoRequest(
            @RequestBody(required = false) Object body,
            @RequestHeader Map<String, String> headers) {

        logger.info("En-têtes reçus: {}", headers);
        logger.info("Corps reçu: {}", body);

        Map<String, Object> response = new HashMap<>();
        response.put("headers", headers);
        response.put("body", body);

        return ResponseEntity.ok(response);
    }
}