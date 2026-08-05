package com.appsisben.backend.modules.callcenter.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Solicitud para modificar la programación de una visita
 * existente de Call Center.
 *
 * @param encuestadorId identificador del encuestador activo.
 * @param fechaProgramada nueva fecha programada.
 * @param horaProgramada nueva hora programada, opcional.
 */
public record CallCenterVisitaProgramacionRequest(

        @NotNull(
                message =
                        "El encuestador es obligatorio"
        )
        Long encuestadorId,

        @NotNull(
                message =
                        "La fecha programada es obligatoria"
        )
        LocalDate fechaProgramada,

        LocalTime horaProgramada
) {
}