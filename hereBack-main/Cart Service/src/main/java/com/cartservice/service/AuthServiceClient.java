package com.cartservice.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "Authentification")
public interface AuthServiceClient {

    @PostMapping("/api/auth/verify")
    ResponseEntity<String> verifyToken(@RequestBody String token);
    @GetMapping("/api/auth/users/{userId}/email")
    String getUserEmail(@PathVariable String userId);
}