package com.notificationsmessage.service;

// Ajoutez cette exception personnalisée
class VendeurNotFoundException extends RuntimeException {
    public VendeurNotFoundException(String message) {
        super(message);
    }
}