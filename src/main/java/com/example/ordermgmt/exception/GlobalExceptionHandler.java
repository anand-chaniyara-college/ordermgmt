package com.example.ordermgmt.exception;

import com.example.ordermgmt.dto.LoginResponseDTO;
import com.example.ordermgmt.dto.RegistrationResponseDTO;
import com.example.ordermgmt.dto.RefreshTokenResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<RegistrationResponseDTO> handleUserAlreadyExistsException(UserAlreadyExistsException ex,
            HttpServletRequest request) {
        logger.warn("Registration failed at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(new RegistrationResponseDTO(ex.getMessage()));
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<RegistrationResponseDTO> handleRoleNotFoundException(RoleNotFoundException ex,
            HttpServletRequest request) {
        logger.warn("Registration failed at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(new RegistrationResponseDTO(ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<RefreshTokenResponseDTO> handleInvalidTokenException(InvalidTokenException ex,
            HttpServletRequest request) {
        logger.warn("Token validation failed at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new RefreshTokenResponseDTO(null, null, null, ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<LoginResponseDTO> handleInvalidCredentialsException(InvalidCredentialsException ex,
            HttpServletRequest request) {
        logger.warn("Login failed at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new LoginResponseDTO(null, null, null, ex.getMessage()));
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<LoginResponseDTO> handleAccountInactiveException(AccountInactiveException ex,
            HttpServletRequest request) {
        logger.warn("Login failed at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new LoginResponseDTO(null, null, null, ex.getMessage()));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, String>> handleAuthException(AuthException ex, HttpServletRequest request) {
        logger.warn("Authentication failed at {}: {}", request.getRequestURI(), ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", "Authentication Failed");
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleOrderNotFoundException(OrderNotFoundException ex,
            HttpServletRequest request) {
        logger.warn("Order not found at {}: {}", request.getRequestURI(), ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", "Order Not Found");
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException ex,
            HttpServletRequest request) {
        logger.warn("Resource not found at {}: {}", request.getRequestURI(), ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", "Resource Not Found");
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(InvalidOrderTransitionException.class)
    public ResponseEntity<Map<String, String>> handleInvalidOrderTransitionException(InvalidOrderTransitionException ex,
            HttpServletRequest request) {
        logger.warn("Invalid order transition at {}: {}", request.getRequestURI(), ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", "Invalid Status Transition");
        response.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientStockException(InsufficientStockException ex,
            HttpServletRequest request) {
        logger.warn("Insufficient stock at {}: {}", request.getRequestURI(), ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", "Insufficient Stock");
        response.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<Map<String, String>> handleInvalidOperationException(InvalidOperationException ex,
            HttpServletRequest request) {
        logger.warn("Invalid operation at {}: {}", request.getRequestURI(), ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", "Invalid Operation");
        response.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        logger.warn("Validation failed at {}: {}", request.getRequestURI(), errors);
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(EmailSendingException.class)
    public ResponseEntity<Map<String, String>> handleEmailSendingException(EmailSendingException ex,
            HttpServletRequest request) {
        logger.error("Email sending failed at {}: {}", request.getRequestURI(), ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", "Email Service Error");
        response.put("message", "Failed to send email. Please contact support.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {
        logger.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", "Access Denied");
        response.put("message", "You do not have permission to access this resource.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        logger.warn("Malformed JSON request at {}: {}", request.getRequestURI(), ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", "Bad Request");
        response.put("message", "Malformed JSON request");
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        logger.warn("Type mismatch at {}: {}", request.getRequestURI(), ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", "Bad Request");
        response.put("message", String.format("Parameter '%s' should be of type '%s'", ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"));
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        logger.warn("Missing request parameter at {}: {}", request.getRequestURI(), ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", "Bad Request");
        response.put("message", String.format("Required parameter '%s' is not present", ex.getParameterName()));
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGlobalException(Exception ex, HttpServletRequest request) {
        logger.error("Unexpected error occurred at {}", request.getRequestURI(), ex);
        Map<String, String> response = new HashMap<>();
        response.put("error", "Internal Server Error");
        response.put("message", "An unexpected error occurred. Please contact support.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        logger.warn("Constraint violation at {}: {}", request.getRequestURI(), ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        if (ex.getConstraintViolations() != null) {
            ex.getConstraintViolations().forEach(violation -> {
                String propertyPath = violation.getPropertyPath().toString();
                String message = violation.getMessage();
                errors.put(propertyPath, message);
            });
        }
        return ResponseEntity.badRequest().body(errors);
    }
}
