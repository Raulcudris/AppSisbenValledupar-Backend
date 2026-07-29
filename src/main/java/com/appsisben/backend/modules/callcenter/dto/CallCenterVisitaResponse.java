package com.appsisben.backend.modules.callcenter.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Respuesta con la información de una visita asignada o ejecutada
 * dentro del módulo Call Center.
 *
 * @param id identificador de la visita.
 * @param callCenterRegistroId identificador del caso maestro.
 * @param encuestadorId identificador del encuestador.
 * @param encuestadorNombre nombre del encuestador.
 * @param usuarioAsignaId identificador del usuario que asignó.
 * @param usuarioAsignaUsername nombre de usuario que asignó.
 * @param fechaAsignacion fecha y hora de asignación.
 * @param fechaProgramada fecha programada.
 * @param horaProgramada hora programada.
 * @param estadoVisita estado de la visita.
 * @param fechaVisitaReal fecha real de visita.
 * @param horaVisitaReal hora real de visita.
 * @param encuestaRealizada indica si la encuesta fue realizada.
 * @param motivoNoEncuesta motivo por el cual no fue realizada.
 * @param fechaReprogramacion fecha de reprogramación.
 * @param observacionEncuestador observación del encuestador.
 * @param activo indica si la visita está activa.
 * @param creadoEn fecha de creación.
 * @param cedulaSolicitante cédula del ciudadano asociado al caso maestro.
 * @param nombreCompleto nombre completo del ciudadano asociado al caso maestro.
 * @param telefono teléfono del ciudadano asociado al caso maestro.
 * @param direccionTexto dirección registrada en el caso maestro.
 * @param barrioNombre nombre del barrio asociado al caso maestro.
 * @param comunaNombre nombre de la comuna asociada al caso maestro.
 * @param tipoSolicitudCallcenter tipo de solicitud Call Center del caso maestro.
 * @param estadoCaso estado formal del caso maestro.
 */
public record CallCenterVisitaResponse(
        Long id,
        Long callCenterRegistroId,
        Long encuestadorId,
        String encuestadorNombre,
        Long usuarioAsignaId,
        String usuarioAsignaUsername,
        LocalDateTime fechaAsignacion,
        LocalDate fechaProgramada,
        LocalTime horaProgramada,
        String estadoVisita,
        LocalDate fechaVisitaReal,
        LocalTime horaVisitaReal,
        Boolean encuestaRealizada,
        String motivoNoEncuesta,
        LocalDate fechaReprogramacion,
        String observacionEncuestador,
        Boolean activo,
        LocalDateTime creadoEn,

        String cedulaSolicitante,
        String nombreCompleto,
        String telefono,
        String direccionTexto,
        String barrioNombre,
        String comunaNombre,
        String tipoSolicitudCallcenter,
        String estadoCaso
) {}