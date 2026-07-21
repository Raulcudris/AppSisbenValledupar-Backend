package com.appsisben.backend.modules.dmc.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record DmcFilterRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaInicio,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaFin,

        Long funcionarioId,
        Long tipoDmcId,
        Long encuestadorId,
        Long barrioId,
        Long comunaId,

        Boolean incluirInactivos,
        Boolean activo
) {
}