package com.appsisben.backend.modules.callcenter.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Información necesaria para presentar la agenda
 * diaria de un encuestador.
 *
 * @param id identificador de la visita.
 * @param encuestadorId identificador del encuestador.
 * @param encuestadorNombre nombre del encuestador.
 * @param fechaAgenda fecha operativa de la visita.
 * @param horaProgramada hora programada.
 * @param cedulaSolicitante cédula del ciudadano.
 * @param nombreCompleto nombre completo del ciudadano.
 * @param direccionTexto dirección del ciudadano.
 * @param barrioNombre nombre del barrio.
 * @param telefono teléfono del ciudadano.
 */
public record CallCenterAgendaVisitaResponse(
        Long id,
        Long encuestadorId,
        String encuestadorNombre,
        LocalDate fechaAgenda,
        LocalTime horaProgramada,
        String cedulaSolicitante,
        String nombreCompleto,
        String direccionTexto,
        String barrioNombre,
        String telefono
) {
}