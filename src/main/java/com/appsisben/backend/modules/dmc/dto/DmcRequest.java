package com.appsisben.backend.modules.dmc.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DmcRequest(
        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotNull(message = "El tipo DMC es obligatorio")
        Long tipoDmcId,

        @NotNull(message = "El encuestador es obligatorio")
        Long encuestadorId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 0, message = "La cantidad no puede ser negativa")
        Integer cantidad,

        String observacion,

        @NotNull(message = "El barrio es obligatorio")
        Long barrioId
) {
}
