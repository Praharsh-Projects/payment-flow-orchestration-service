package io.praharsh.payments.api;

import io.praharsh.payments.service.IdempotencyConflictException;
import io.praharsh.payments.service.InvalidPaymentTransitionException;
import io.praharsh.payments.service.PaymentNotFoundException;
import io.praharsh.payments.service.PaymentVersionConflictException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    ResponseEntity<ApiError> notFound(PaymentNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("not_found", exception.getMessage()));
    }

    @ExceptionHandler({IdempotencyConflictException.class, PaymentVersionConflictException.class})
    ResponseEntity<ApiError> conflict(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("conflict", exception.getMessage()));
    }

    @ExceptionHandler(InvalidPaymentTransitionException.class)
    ResponseEntity<ApiError> invalidTransition(InvalidPaymentTransitionException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(ApiError.of("invalid_transition", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        Map<String, String> details = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                details.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(new ApiError(
                "validation_error",
                "The request body failed validation.",
                details,
                Instant.now()
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> constraintViolation(ConstraintViolationException exception) {
        return ResponseEntity.badRequest()
                .body(ApiError.of("validation_error", exception.getMessage()));
    }
}
