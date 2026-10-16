package com.formation.qualite.boutique.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.formation.qualite.boutique.service.exception.InsufficientStockException;
import com.formation.qualite.boutique.service.exception.ResourceNotFoundException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private static final String ERROR_KEY = "error";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_shouldReturn404WithMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleNotFound(new ResourceNotFoundException("Client introuvable : 9"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry(ERROR_KEY, "Client introuvable : 9");
    }

    @Test
    void handleBadRequest_shouldReturn400WithMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleBadRequest(new InsufficientStockException("Stock insuffisant pour le produit : Webcam HD"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry(ERROR_KEY, "Stock insuffisant pour le produit : Webcam HD");
    }

    @Test
    void handleBadRequest_exceptionWithoutMessage_shouldUseStatusReason() {
        ResponseEntity<Map<String, String>> response = handler.handleBadRequest(new IllegalStateException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry(ERROR_KEY, "Bad Request");
    }
}
