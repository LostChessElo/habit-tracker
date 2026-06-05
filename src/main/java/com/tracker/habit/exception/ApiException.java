package com.tracker.habit.exception;

import org.springframework.http.HttpStatus;

/**
 * Runtime exception that carries an HTTP status code alongside its message.
 *
 * <p>Throw this anywhere in the service or repository layer to signal an
 * application-level error. {@link GlobalExceptionHandler} catches it and
 * converts it into a structured JSON error response with the appropriate
 * HTTP status.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 *     throw new ApiException(HttpStatus.NOT_FOUND, "Habit not found.");
 *     throw new ApiException(HttpStatus.CONFLICT, "Email already registered.");
 * </pre>
 */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    /**
     * Creates a new {@code ApiException}.
     *
     * @param status  the HTTP status code that should be returned to the client
     * @param message a human-readable description of the error
     */
    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * Returns the HTTP status associated with this exception.
     *
     * @return the HTTP status code
     */
    public HttpStatus getStatus() { return status; }
}
