package com.appsisben.backend.shared.exception;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class DatabaseConstraintExceptionHandler {

    private static final String DMC_DUPLICATE_CONSTRAINT = "uq_dmc_fecha_tipo_encuestador";

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex
    ) {
        String technicalMessage = getTechnicalMessage(ex);
        String lowerMessage = technicalMessage.toLowerCase();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("timestamp", LocalDateTime.now());

        if (
                lowerMessage.contains(DMC_DUPLICATE_CONSTRAINT)
                        || lowerMessage.contains("duplicate entry")
        ) {
            body.put("status", HttpStatus.CONFLICT.value());
            body.put("error", "DMC_DUPLICATE_RECORD");
            body.put(
                    "message",
                    "No se puede guardar este registro porque ya existe un registro DMC guardado previamente con la misma fecha, tipo DMC y encuestador."
            );

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(body);
        }

        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "DATA_INTEGRITY_CONFLICT");
        body.put(
                "message",
                "No fue posible guardar la información porque existe un conflicto de integridad en los datos."
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(body);
    }

    private String getTechnicalMessage(DataIntegrityViolationException ex) {
        Throwable mostSpecificCause = ex.getMostSpecificCause();

        if (mostSpecificCause != null && mostSpecificCause.getMessage() != null) {
            return mostSpecificCause.getMessage();
        }

        return ex.getMessage() != null ? ex.getMessage() : "";
    }
}