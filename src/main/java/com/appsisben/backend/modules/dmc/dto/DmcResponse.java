package com.appsisben.backend.modules.dmc.dto;

import java.time.LocalDate;

public record DmcResponse(
        Long id,
        LocalDate fecha,
        Long funcionarioId,
        String funcionarioUsername,
        Long tipoDmcId,
        String tipoDmcCodigo,
        String tipoDmcNombre,
        Long encuestadorId,
        String encuestadorNombre,
        Integer cantidad,
        String observacion,
        Long barrioId,
        String barrioNombre,
        String comunaNombre
) {
}
