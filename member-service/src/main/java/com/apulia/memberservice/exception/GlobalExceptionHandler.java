package com.apulia.memberservice.exception;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private BulkheadRegistry bulkheadRegistry;

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    private ErrorResponse buildError(HttpStatus status, String message) {
        return new ErrorResponse(
                LocalDateTime.now().withNano(0).format(FORMATTER),
                status.value(),
                status.getReasonPhrase(),
                message
        );
    }

    private long bulkheadRetryAfterSeconds() {
        return bulkheadRegistry.getAllBulkheads().stream()
                .findFirst()
                .map(b -> Math.max(1, b.getBulkheadConfig().getMaxWaitDuration().toSeconds()))
                .orElse(1L);
    }

    private long rateLimiterRetryAfterSeconds() {
        return rateLimiterRegistry.getAllRateLimiters().stream()
                .findFirst()
                .map(r -> Math.max(1, r.getRateLimiterConfig().getLimitRefreshPeriod().toSeconds()))
                .orElse(1L);
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMemberNotFound(MemberNotFoundException ex) {
        ErrorResponse error = buildError(HttpStatus.NOT_FOUND, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = buildError(HttpStatus.BAD_REQUEST, "Bad request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Validation error");

        ErrorResponse error = buildError(HttpStatus.BAD_REQUEST, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format(
                "Invalid value '%s' for parameter '%s': expected a numeric value",
                ex.getValue(), ex.getName()
        );
        ErrorResponse error = buildError(HttpStatus.BAD_REQUEST, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        ErrorResponse error = buildError(HttpStatus.BAD_REQUEST, "Malformed JSON request body");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        ErrorResponse error = buildError(
                status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getReason()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpClientError(HttpClientErrorException ex) {
        logger.warn("HTTP Client error: {} - {}", ex.getStatusCode(), ex.getMessage());
        ErrorResponse error = buildError(HttpStatus.BAD_REQUEST, "Request resulted in an error");
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpServerError(HttpServerErrorException ex) {
        logger.error("HTTP Server error: {} - {}", ex.getStatusCode(), ex.getMessage());
        ErrorResponse error = buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred");
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    @ExceptionHandler(BulkheadFullException.class)
    public ResponseEntity<ErrorResponse> handleBulkheadFull(BulkheadFullException ex) {
        logger.warn("Bulkhead is full: {}", ex.getMessage());
        long retryAfterSeconds = bulkheadRetryAfterSeconds();
        String message = "Service is currently busy, please retry after " + retryAfterSeconds + " second(s)";
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now().withNano(0).format(FORMATTER),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "TOO_MANY_REQUESTS",
                message,
                retryAfterSeconds
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(retryAfterSeconds))
                .body(error);
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ErrorResponse> handleRequestNotPermitted(RequestNotPermitted ex) {
        logger.warn("Rate limit exceeded: {}", ex.getMessage());
        long retryAfterSeconds = rateLimiterRetryAfterSeconds();
        String message = "Too many requests, please retry after " + retryAfterSeconds + " second(s)";
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now().withNano(0).format(FORMATTER),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "TOO_MANY_REQUESTS",
                message,
                retryAfterSeconds
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(retryAfterSeconds))
                .body(error);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex) {
        logger.warn("No handler found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        ErrorResponse error = buildError(HttpStatus.NOT_FOUND, "The requested endpoint does not exist");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        logger.error("Unexpected server error", ex);
        ErrorResponse error = buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected server error"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}