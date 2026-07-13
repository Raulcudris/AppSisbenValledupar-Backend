package com.appsisben.backend.modules.audit.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long usuarioId,
        String username,
        String tablaAfectada,
        Long registroId,
        String accion,
        LocalDateTime fechaAccion,
        String ipOrigen,
        String datosAnteriores,
        String datosNuevos
) {
}
