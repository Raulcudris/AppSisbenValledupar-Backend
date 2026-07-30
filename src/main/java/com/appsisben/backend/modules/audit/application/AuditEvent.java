package com.appsisben.backend.modules.audit.application;

import java.time.LocalDateTime;

public record AuditEvent(
        Long usuarioId,
        String accion,
        String tablaAfectada,
        Long registroId,
        LocalDateTime fechaAccion,
        String ipOrigen,
        String datosAnteriores,
        String datosNuevos
) {
}