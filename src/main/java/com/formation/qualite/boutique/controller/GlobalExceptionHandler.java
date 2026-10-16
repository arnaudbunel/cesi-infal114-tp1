package com.formation.qualite.boutique.controller;

import com.formation.qualite.boutique.service.exception.InsufficientStockException;
import com.formation.qualite.boutique.service.exception.ResourceNotFoundException;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException e) {
        return errorResponse(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler({InsufficientStockException.class, IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException e) {
        return errorResponse(HttpStatus.BAD_REQUEST, e);
    }

    // Map.of refuse les valeurs null : une exception sans message renvoie le
    // libelle du statut HTTP au lieu de provoquer une erreur 500.
    private static ResponseEntity<Map<String, String>> errorResponse(HttpStatus status, RuntimeException e) {
        String message = Objects.requireNonNullElse(e.getMessage(), status.getReasonPhrase());
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
