package com.boutique_catalogue_produits.exception;

import org.hibernate.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.reactive.resource.NoResourceFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllUncaughtExceptions(Exception exception, WebRequest request) {
        // Log détaillé avec la stack trace complète
        logger.error("=========== GLOBAL EXCEPTION HANDLER ===========");
        logger.error("Type d'exception : {}", exception.getClass().getName());
        logger.error("Message de l'exception : {}", exception.getMessage());

        // Log de la stack trace
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        logger.error("Stack Trace : {}", sw.toString());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("message", exception.getMessage());
        errorDetails.put("type", exception.getClass().getName());
        errorDetails.put("details", request.getDescription(false));

        // Déterminer le statut HTTP en fonction du type d'exception
        HttpStatus status = determineHttpStatus(exception);

        return new ResponseEntity<>(errorDetails, status);
    }

    private HttpStatus determineHttpStatus(Exception exception) {
        if (exception instanceof ResourceNotFoundException) {
            return HttpStatus.NOT_FOUND;
        } else if (exception instanceof RuntimeException) {
            return HttpStatus.BAD_REQUEST;
        } else if (exception instanceof TransactionException) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        // Par défaut
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    // Les autres méthodes de gestion d'exceptions restent les mêmes
}