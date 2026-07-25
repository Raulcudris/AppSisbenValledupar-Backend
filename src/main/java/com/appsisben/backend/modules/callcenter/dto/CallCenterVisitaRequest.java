package com.appsisben.backend.modules.callcenter.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record CallCenterVisitaRequest(
        String estadoVisita,

        LocalDate fechaVisitaReal,

        LocalTime horaVisitaReal,

        Boolean encuestaRealizada,

        @Size(max = 500, message = "El motivo de no encuesta no puede superar los 500 caracteres")
        String motivoNoEncuesta,

        LocalDate fechaReprogramacion,

        String observacionEncuestador,

        Boolean verificado
) {
}