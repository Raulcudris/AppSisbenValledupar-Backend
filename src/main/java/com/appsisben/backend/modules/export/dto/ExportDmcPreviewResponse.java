package com.appsisben.backend.modules.export.dto;

import java.time.LocalDate;

public record ExportDmcPreviewResponse(
        Long id,
        LocalDate fecha,
        String funcionarioUsername,
        String tipoDmcCodigo,
        String tipoDmcNombre,
        String encuestadorNombre,
        Number cantidad,
        String barrioNombre,
        String comunaNombre,
        String observacion
) {
}