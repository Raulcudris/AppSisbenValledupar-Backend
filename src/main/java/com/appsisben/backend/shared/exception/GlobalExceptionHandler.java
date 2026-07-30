package com.appsisben.backend.shared.exception;
import com.appsisben.backend.shared.api.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        ApiErrorResponse.of(
                                "RESOURCE_NOT_FOUND",
                                safeMessage(
                                        exception.getMessage(),
                                        "El recurso solicitado no fue encontrado"
                                ),
                                request.getRequestURI()
                        )
                );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        ApiErrorResponse.of(
                                "BUSINESS_RULE_VIOLATION",
                                safeMessage(
                                        exception.getMessage(),
                                        "La operación no cumple las reglas de negocio"
                                ),
                                request.getRequestURI()
                        )
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError fieldError :
                exception.getBindingResult().getFieldErrors()) {

            errors.putIfAbsent(
                    fieldError.getField(),
                    safeMessage(
                            fieldError.getDefaultMessage(),
                            "Valor inválido"
                    )
            );
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        ApiErrorResponse.validation(
                                "Existen campos inválidos en la solicitud",
                                errors,
                                request.getRequestURI()
                        )
                );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        exception.getConstraintViolations()
                .forEach(violation -> errors.putIfAbsent(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        ApiErrorResponse.validation(
                                "Existen parámetros inválidos en la solicitud",
                                errors,
                                request.getRequestURI()
                        )
                );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(
                exception.getParameterName(),
                "El parámetro es obligatorio"
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        ApiErrorResponse.validation(
                                "Faltan parámetros obligatorios",
                                errors,
                                request.getRequestURI()
                        )
                );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(
                exception.getName(),
                "El valor enviado no tiene el formato esperado"
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        ApiErrorResponse.validation(
                                "Uno de los parámetros tiene un formato inválido",
                                errors,
                                request.getRequestURI()
                        )
                );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        ApiErrorResponse.of(
                                "INVALID_REQUEST_BODY",
                                "El cuerpo de la solicitud no tiene un formato válido",
                                request.getRequestURI()
                        )
                );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        String databaseMessage = mostSpecificMessage(exception);
        String normalizedMessage = databaseMessage.toLowerCase();

        String code;
        String message;
        HttpStatus status;

        if (normalizedMessage.contains("duplicate")
                || normalizedMessage.contains("duplicate entry")) {

            code = "DUPLICATE_RECORD";
            message = "Ya existe un registro con los mismos datos";
            status = HttpStatus.CONFLICT;

        } else if (normalizedMessage.contains("foreign key constraint")
                || normalizedMessage.contains("constraint fails")) {

            code = "REFERENCE_CONSTRAINT";
            message = "La operación hace referencia a información inexistente o en uso";
            status = HttpStatus.CONFLICT;

        } else if (normalizedMessage.contains("cannot be null")
                || normalizedMessage.contains("not-null")) {

            code = "REQUIRED_VALUE";
            message = "No se proporcionó un valor obligatorio";
            status = HttpStatus.BAD_REQUEST;

        } else {

            code = "DATA_INTEGRITY_ERROR";
            message = "La operación no cumple las restricciones de integridad";
            status = HttpStatus.CONFLICT;
        }

        log.warn(
                "Restricción de integridad. Path={}, detalle={}",
                request.getRequestURI(),
                databaseMessage
        );

        return ResponseEntity
                .status(status)
                .body(
                        ApiErrorResponse.of(
                                code,
                                message,
                                request.getRequestURI()
                        )
                );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        String message = safeMessage(
                exception.getReason(),
                "No fue posible completar la operación"
        );

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(
                        ApiErrorResponse.of(
                                "HTTP_ERROR",
                                message,
                                request.getRequestURI()
                        )
                );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(
                        ApiErrorResponse.of(
                                "METHOD_NOT_ALLOWED",
                                "El método HTTP utilizado no está permitido para este recurso",
                                request.getRequestURI()
                        )
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Error interno no controlado. Método={}, path={}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ApiErrorResponse.of(
                                "INTERNAL_SERVER_ERROR",
                                "Ocurrió un error interno al procesar la solicitud",
                                request.getRequestURI()
                        )
                );
    }

    private String mostSpecificMessage(
            DataIntegrityViolationException exception
    ) {
        Throwable cause = exception.getMostSpecificCause();

        if (cause == null || cause.getMessage() == null) {
            return "";
        }

        return cause.getMessage();
    }

    private String safeMessage(
            String value,
            String defaultMessage
    ) {
        if (value == null || value.isBlank()) {
            return defaultMessage;
        }

        return value.trim();
    }
}