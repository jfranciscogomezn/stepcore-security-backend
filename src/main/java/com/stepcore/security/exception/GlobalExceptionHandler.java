package com.stepcore.security.exception;

import com.stepcore.security.i18n.ApiMessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ApiMessageService apiMessageService;

    public record ErrorResponse(String timestamp, int status, String error, String message, String path) {}

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            final AuthenticationException ex, final HttpServletRequest request) {
        log.warn("[GlobalExceptionHandler] - AUTH: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, apiMessageService.resolve(ex, ex.getMessage()), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            final AccessDeniedException ex, final HttpServletRequest request) {
        log.warn("[GlobalExceptionHandler] - FORBIDDEN: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, apiMessageService.resolveKey("error.accessDenied"), request);
    }

    @ExceptionHandler(TenantSuspendedException.class)
    public ResponseEntity<ErrorResponse> handleTenantSuspended(
            final TenantSuspendedException ex, final HttpServletRequest request) {
        log.warn("[GlobalExceptionHandler] - TENANT_SUSPENDED: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, apiMessageService.resolve(ex, ex.getMessage()), request);
    }

    @ExceptionHandler({UserNotFoundException.class, RoleNotFoundException.class,
                        TenantNotFoundException.class, MenuNodeNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(
            final RuntimeException ex, final HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, apiMessageService.resolve(ex, ex.getMessage()), request);
    }

    @ExceptionHandler({DuplicateEmailException.class, RoleInUseException.class,
                        UserHasAssociatedRecordsException.class,
                        TenantSlugAlreadyExistsException.class, UserLimitReachedException.class,
                        MenuNodeInUseException.class,
                        AdminSelfDisableException.class, AdminSelfDeleteException.class,
                        LastTenantAdminException.class})
    public ResponseEntity<ErrorResponse> handleConflict(
            final RuntimeException ex, final HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, apiMessageService.resolve(ex, ex.getMessage()), request);
    }

    @ExceptionHandler({InvalidPasswordException.class, InvalidMenuNodeAssignmentException.class,
                        MenuNodeValidationException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(
            final RuntimeException ex, final HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, apiMessageService.resolve(ex, ex.getMessage()), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            final MethodArgumentNotValidException ex, final HttpServletRequest request) {
        final String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                apiMessageService.resolveKey("error.validationFailed", details),
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            final Exception ex, final HttpServletRequest request) {
        log.error("[GlobalExceptionHandler] - UNEXPECTED: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, apiMessageService.resolveKey("error.unexpected"), request);
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            final HttpStatus status, final String message, final HttpServletRequest request) {
        final ErrorResponse body = new ErrorResponse(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
