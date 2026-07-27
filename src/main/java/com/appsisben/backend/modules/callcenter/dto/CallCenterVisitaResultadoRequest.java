package com.appsisben.backend.modules.callcenter.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Solicitud para registrar el resultado de una visita de encuestador.
 *
 * @param estadoVisita estado final o parcial de la visita.
 * @param fechaVisitaReal fecha real de visita.
 * @param horaVisitaReal hora real de visita.
 * @param encuestaRealizada indica si la encuesta fue realizada.
 * @param motivoNoEncuesta motivo por el cual no se realizó la encuesta.
 * @param fechaReprogramacion fecha de reprogramación cuando aplique.
 * @param observacionEncuestador observación registrada por el encuestador.
 */
public record CallCenterVisitaResultadoRequest(
        @NotBlank(message = "El estado de la visita es obligatorio")
        String estadoVisita,

        LocalDate fechaVisitaReal,
        LocalTime horaVisitaReal,
        Boolean encuestaRealizada,
        String motivoNoEncuesta,
        LocalDate fechaReprogramacion,
        String observacionEncuestador
) {}
