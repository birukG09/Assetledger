package com.assetledger.api;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public final class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> notFound(
            NoSuchElementException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> badRequest(Exception exception) {
        String message = exception instanceof MethodArgumentNotValidException validation
                ? validation.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "))
                : exception.getMessage();
        return response(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ErrorResponse> persistenceFailure(SQLException exception) {
        // Log full detail server-side; never return raw SQL/db-path detail to the client
        log.error("Persistence failure", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "A persistence error occurred. Please try again.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception exception) {
        // Catch-all so unmapped exceptions never leak a raw stack trace to the client
        log.error("Unhandled exception", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String message) {
        String safeMessage = message == null || message.isBlank()
                ? status.getReasonPhrase()
                : message;
        return ResponseEntity.status(status).body(
                ErrorResponse.of(safeMessage, status.value())
        );
    }
}
