package com.appsisben.backend.modules.audit.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record AuditFilterRequest(
        Long usuarioId,
        String username,
        String tablaAfectada,
        Long registroId,
        String accion,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaInicio,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaFin
) {
}
