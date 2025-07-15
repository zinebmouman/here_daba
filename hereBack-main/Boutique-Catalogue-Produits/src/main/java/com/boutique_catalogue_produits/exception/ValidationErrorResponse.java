package com.boutique_catalogue_produits.exception;

import java.util.Date;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidationErrorResponse {
    private Date timestamp;
    private String message;
    private Map<String, String> errors;
}