package com.appsisben.backend.modules.callcenter.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Solicitud para asignar un caso de Call Center a un encuestador.
 *
 * @param encuestadorId identificador del encuestador asignado.
 * @param fechaProgramada fecha programada para la visita.
 * @param horaProgramada hora programada para la visita.
 * @param observacion observación inicial de asignación.
 */
public record CallCenterVisitaAsignacionRequest(
        @NotNull(message = "Debe seleccionar el encuestador")
        Long encuestadorId,

        LocalDate fechaProgramada,
        LocalTime horaProgramada,
        String observacion
) {}
