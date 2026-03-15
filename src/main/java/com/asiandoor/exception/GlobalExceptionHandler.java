package com.asiandoor.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import com.asiandoor.dto.ApiErrorResponseDTO;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, ex);
    }

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public Object handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, ex);
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
    public Object handleValidation(Exception ex, HttpServletRequest request) {
        String message;
        if (ex instanceof MethodArgumentNotValidException manv) {
            message = formatValidationErrors(manv.getBindingResult().getFieldErrors());
        } else {
            BindException be = (BindException) ex;
            message = formatValidationErrors(be.getBindingResult().getFieldErrors());
        }
        return buildResponse(HttpStatus.BAD_REQUEST, message, request, ex);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Object handleConstraintViolation(ConstraintViolationException ex,
                                            HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, message, request, ex);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Object handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                     HttpServletRequest request) {
        String message = "Invalid value for parameter '" + ex.getName() + "'.";
        return buildResponse(HttpStatus.BAD_REQUEST, message, request, ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleForbidden(AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Access is denied.", request, ex);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleDataIntegrity(DataIntegrityViolationException ex,
                                      HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT,
                "The request could not be completed due to conflicting data.",
                request,
                ex);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                           HttpServletRequest request) {
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method is not supported for this endpoint.",
                request,
                ex);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Object handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                              HttpServletRequest request) {
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported media type.",
                request,
                ex);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResourceFound(NoResourceFoundException ex,
                                        HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND,
                "Requested resource was not found.",
                request,
                ex);
    }

    @ExceptionHandler(Exception.class)
    public Object handleUnexpected(Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                request,
                ex);
    }

    private Object buildResponse(HttpStatus status,
                                 String message,
                                 HttpServletRequest request,
                                 Exception ex) {
        if (status.is5xxServerError()) {
            log.error("{} {} failed: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        } else {
            log.warn("{} {} -> {}: {}", request.getMethod(), request.getRequestURI(), status.value(), ex.getMessage());
        }

        if (isApiRequest(request)) {
            return buildApi(status, message, request.getRequestURI());
        }

        return buildView(status, message, request.getRequestURI());
    }

    private ResponseEntity<ApiErrorResponseDTO> buildApi(HttpStatus status, String message, String path) {
        ApiErrorResponseDTO body = new ApiErrorResponseDTO();
        body.setTimestamp(LocalDateTime.now());
        body.setStatus(status.value());
        body.setError(status.getReasonPhrase());
        body.setMessage(message);
        body.setPath(path);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private ModelAndView buildView(HttpStatusCode status, String message, String path) {
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(status);
        mav.addObject("timestamp", LocalDateTime.now());
        mav.addObject("status", status.value());
        mav.addObject("error", HttpStatus.valueOf(status.value()).getReasonPhrase());
        mav.addObject("message", message);
        mav.addObject("path", path);
        return mav;
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        String contentType = request.getContentType();
        String requestedWith = request.getHeader("X-Requested-With");

        return uri.startsWith("/api/")
                || uri.startsWith("/cart")
                || uri.startsWith("/orders")
                || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE))
                || (contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }

    private String formatValidationErrors(java.util.List<FieldError> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) {
            return "Validation failed for one or more fields.";
        }

        return fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
    }
}
