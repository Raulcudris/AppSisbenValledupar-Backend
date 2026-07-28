package com.appsisben.backend.modules.callcenter.dto;

import java.time.LocalDate;

/**
 * DTO de filtros para consultar registros del módulo Call Center.
 *
 * Este record agrupa los parámetros recibidos desde el frontend para realizar
 * búsquedas paginadas sobre los casos Call Center. Incluye filtros legacy
 * de llamada y visita, filtros de asignación por rol y filtros del flujo
 * formal del caso.
 *
 * Los campos {@code estadoCaso} y {@code tipoSolicitudCallcenter} permiten
 * consultar el avance del caso dentro del flujo:
 * pendiente de enrutamiento, asignado a funcionario, en gestión de llamada,
 * pendiente de visita, cerrado o cancelado.
 */
public record CallCenterFilterRequest(
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Long funcionarioId,
        String cedulaSolicitante,
        String nombreCompleto,
        String telefono,
        Boolean llamadaConectada,
        Long motivoNoContactoId,
        Long encuestadorProgramadoId,
        Long encuestadorAsignadoId,
        LocalDate fechaEncuestaInicio,
        LocalDate fechaEncuestaFin,
        Boolean solicitoNuevaEncuesta,
        Long barrioId,
        Long comunaId,
        Long funcionarioCallcenterAsignadoId,
        Boolean disposicionRecibirEncuesta,
        Boolean explicoInformanteCalificado,
        Boolean activo,
        String q,
        String tipoRegistro,
        String origenRegistro,
        Long ventanillaRegistroId,
        Boolean verificado,
        String estadoVisita,
        Boolean encuestaRealizada,

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
        String tipoSolicitudCallcenter
) {
}