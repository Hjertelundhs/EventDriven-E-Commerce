package com.eventdrivencommerce.inventory.api;

import com.eventdrivencommerce.inventory.application.exception.ConcurrentInventoryModificationException;
import com.eventdrivencommerce.inventory.application.exception.DuplicateReservationException;
import com.eventdrivencommerce.inventory.application.exception.InventoryNotFoundException;
import com.eventdrivencommerce.inventory.application.exception.ReservationNotFoundException;
import com.eventdrivencommerce.inventory.domain.exception.InsufficientStockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String ERROR_BASE = "https://errors.event-driven-commerce.test/inventory/";

    @ExceptionHandler({InventoryNotFoundException.class, ReservationNotFoundException.class})
    ResponseEntity<ProblemDetail> notFound(RuntimeException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "resource-not-found", "Inventory resource not found",
                exception.getMessage(), request);
    }

    @ExceptionHandler(InsufficientStockException.class)
    ResponseEntity<ProblemDetail> insufficientStock(InsufficientStockException exception,
                                                    HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "insufficient-stock", "Insufficient stock",
                exception.getMessage(), request);
    }

    @ExceptionHandler({ConcurrentInventoryModificationException.class, DuplicateReservationException.class})
    ResponseEntity<ProblemDetail> conflict(RuntimeException exception, HttpServletRequest request) {
        String type = exception instanceof DuplicateReservationException
                ? "duplicate-reservation" : "concurrent-modification";
        return response(HttpStatus.CONFLICT, type, "Inventory conflict", exception.getMessage(), request);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    ResponseEntity<ProblemDetail> semanticError(RuntimeException exception, HttpServletRequest request) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "invalid-inventory-operation",
                "Inventory operation is not valid", exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> invalidRequest(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "request-validation", "Request validation failed",
                "One or more request fields are invalid.", request);
        List<Violation> violations = exception.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> new Violation(error.getField(), error.getCode(), error.getDefaultMessage()))
                .toList();
        problem.setProperty("violations", violations);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> invalidParameter(ConstraintViolationException exception, HttpServletRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "parameter-validation", "Parameter validation failed",
                "One or more request parameters are invalid.", request);
        problem.setProperty("violations", exception.getConstraintViolations().stream()
                .map(value -> new Violation(value.getPropertyPath().toString(),
                        value.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
                        value.getMessage()))
                .toList());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> unreadableRequest(HttpMessageNotReadableException exception,
                                                    HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "malformed-request", "Malformed request",
                "The request body is missing or cannot be parsed.", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unhandled inventory service error", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal server error",
                "The request could not be completed.", request);
    }

    private static ResponseEntity<ProblemDetail> response(HttpStatus status, String type, String title,
                                                          String detail, HttpServletRequest request) {
        return ResponseEntity.status(status).body(problem(status, type, title, detail, request));
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title,
                                         String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(ERROR_BASE + type));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }
        return problem;
    }

    private record Violation(String field, String code, String message) {}
}
