package com.appsisben.backend.shared.api;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

public record ApiErrorResponse(
        boolean success,
        String code,
        String message,
        Object data,
        Map<String, String> errors,
        LocalDateTime timestamp,
        String path
) {

    public static ApiErrorResponse of(
            String code,
            String message,
            String path
    ) {
        return new ApiErrorResponse(
                false,
                code,
                message,
                null,
                Collections.emptyMap(),
                LocalDateTime.now(),
                path
        );
    }

    public static ApiErrorResponse validation(
            String message,
            Map<String, String> errors,
            String path
    ) {
        return new ApiErrorResponse(
                false,
                "VALIDATION_ERROR",
                message,
                null,
                errors != null
                        ? errors
                        : Collections.emptyMap(),
                LocalDateTime.now(),
                path
        );
    }
}