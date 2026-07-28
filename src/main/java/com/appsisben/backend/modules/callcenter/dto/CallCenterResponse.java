package com.appsisben.backend.modules.callcenter.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO de respuesta para los registros del módulo Call Center.
 *
 * Este DTO expone la información principal del caso maestro, los datos
 * legacy de llamada y visita, la asignación al funcionario Call Center
 * y los campos del flujo formal del caso.
 *
 * Los campos del flujo formal permiten que el frontend identifique:
 * estado del caso, tipo de solicitud, cierre del caso y usuario que cerró.
 */
public record CallCenterResponse(
        Long id,
        LocalDateTime marcaTemporal,
        LocalDate fechaLlamada,
        LocalTime horaLlamada,
        String tipoRegistro,
        String origenRegistro,

        Long ventanillaRegistroId,
        String ventanillaNumeroVentanilla,
        LocalDate ventanillaFecha,

        Long funcionarioId,
        String funcionarioUsername,

        Long funcionarioCallcenterAsignadoId,
        String funcionarioCallcenterAsignadoUsername,
        String funcionarioCallcenterAsignadoNombre,
        LocalDateTime fechaAsignacionCallcenter,
        Long usuarioAsignaCallcenterId,
        String usuarioAsignaCallcenterUsername,

        String cedulaSolicitante,
        String nombreCompleto,
        String telefono,

        Boolean llamadaConectada,

        Long motivoNoContactoId,
        String motivoNoContactoCodigo,
        String motivoNoContactoNombre,
        String motivoNoContactoTexto,

        Long encuestadorProgramadoId,
        String encuestadorProgramadoNombre,
        LocalDate fechaEncuestaProgramada,

        Boolean solicitoNuevaEncuesta,

        String direccionTexto,
        Long barrioId,
        String barrioNombre,
        Long comunaId,
        String comunaNombre,

        LocalDate fechaAplicacionInformada,
        Boolean disposicionRecibirEncuesta,

        Long motivoNoDisposicionId,
        String motivoNoDisposicionCodigo,
        String motivoNoDisposicionNombre,
        String motivoNoDisposicionTexto,

        Long encuestadorAsignadoId,
        String encuestadorAsignadoNombre,

        Boolean explicoInformanteCalificado,
        Boolean verificado,
        String estadoVisita,
        LocalDate fechaVisitaReal,
        LocalTime horaVisitaReal,
        Boolean encuestaRealizada,
        String motivoNoEncuesta,
        LocalDate fechaReprogramacion,
        String observacionEncuestador,

        String observacion,

        /**
         * Estado formal del caso Call Center.
         *
         * Ejemplos:
         * PENDIENTE_ENRUTAMIENTO, ASIGNADO_CALLCENTER,
         * EN_GESTION_LLAMADA, VISITA_PROGRAMADA, CERRADO.
         */
        String estadoCaso,

        /**
         * Tipo de solicitud que originó el caso.
         *
         * Ejemplos:
         * NUEVA_ENCUESTA, INCLUSION, VERIFICACION, OTRO.
         */
        String tipoSolicitudCallcenter,

        /**
         * Fecha y hora en que se cerró el caso, cuando aplique.
         */
        LocalDateTime fechaCierre,

        /**
         * Motivo o descripción del cierre del caso, cuando aplique.
         */
        String motivoCierre,

        /**
         * Identificador del usuario que cerró el caso, cuando aplique.
         */
        Long usuarioCierreId,

        /**
         * Nombre de usuario que cerró el caso, cuando aplique.
         */
        String usuarioCierreUsername,

        Boolean activo
) {
}