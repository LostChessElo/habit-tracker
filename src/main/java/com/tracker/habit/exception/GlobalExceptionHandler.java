package com.tracker.habit.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Global exception handler that translates exceptions into consistent JSON error responses.
 *
 * <p>All handlers return an {@link ErrorResponse} body with a numeric status code,
 * a human-readable message, and a timestamp. This gives clients a predictable error
 * contract regardless of which layer the exception originates from.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles {@link ApiException}, application-level errors with an explicit HTTP status.
     *
     * @param exception the thrown {@link ApiException}
     * @return a {@link ResponseEntity} whose status matches the exception's status code
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
        ErrorResponse response = new ErrorResponse(exception.getStatus().value(), exception.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(response, exception.getStatus());
    }

    /**
     * Handles Bean Validation failures ({@code @Valid} on request bodies).
     *
     * <p>Only the first field error is surfaced to keep the response concise.
     * The message comes from the constraint annotation's {@code message} attribute.</p>
     *
     * @param exception the validation exception thrown by Spring MVC
     * @return {@code 400 BAD REQUEST} with the first constraint violation message
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .getFirst()
                .getDefaultMessage();
        ErrorResponse response = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message, LocalDateTime.now());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
