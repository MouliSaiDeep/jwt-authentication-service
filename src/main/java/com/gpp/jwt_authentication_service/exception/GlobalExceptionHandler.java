package com.gpp.jwt_authentication_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, String> response = new HashMap<>();
        String error = ex.getStatusCode() == HttpStatus.UNAUTHORIZED ? "unauthorized" : "error";
        if (ex.getStatusCode() == HttpStatus.CONFLICT) error = "conflict";
        if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) error = "bad_request";
        
        response.put("error", error);
        response.put("message", ex.getReason());
        
        return new ResponseEntity<>(response, ex.getStatusCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> response = new HashMap<>();
        StringBuilder message = new StringBuilder();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            message.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
        }
        
        response.put("error", "invalid_input");
        response.put("message", message.toString());
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "unauthorized");
        response.put("message", "Invalid username or password");
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "internal_server_error");
        response.put("message", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
