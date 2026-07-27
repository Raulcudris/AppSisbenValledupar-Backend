package com.appsisben.backend.modules.callcenter.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Solicitud para registrar una gestión de llamada dentro de un caso
 * de Call Center.
 *
 * @param fechaLlamada fecha de la llamada. Si llega nula, el servicio usa la fecha actual.
 * @param horaLlamada hora de la llamada. Si llega nula, el servicio usa la hora actual.
 * @param llamadaConectada indica si la llamada fue contestada.
 * @param resultadoLlamada código técnico del resultado de la llamada.
 * @param motivoNoContactoId motivo de no contacto, cuando aplique.
 * @param motivoNoDisposicionId motivo de no disposición, cuando aplique.
 * @param fechaReprogramacionLlamada fecha para reprogramar una llamada.
 * @param horaReprogramacionLlamada hora para reprogramar una llamada.
 * @param observacion observación operativa de la llamada.
 */
public record CallCenterGestionLlamadaRequest(
        LocalDate fechaLlamada,
        LocalTime horaLlamada,
        Boolean llamadaConectada,

        @NotBlank(message = "El resultado de la llamada es obligatorio")
        String resultadoLlamada,

        Long motivoNoContactoId,
        Long motivoNoDisposicionId,
        LocalDate fechaReprogramacionLlamada,
        LocalTime horaReprogramacionLlamada,
        String observacion
) {}
