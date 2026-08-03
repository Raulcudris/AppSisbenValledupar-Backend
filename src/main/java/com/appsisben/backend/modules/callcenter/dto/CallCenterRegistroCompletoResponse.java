package com.appsisben.backend.modules.callcenter.dto;

/**
 * Resultado del registro completo de un caso Call Center.
 *
 * @param registro estado final del caso maestro.
 * @param llamada primera gestión telefónica registrada.
 * @param visita visita creada y programada.
 */
public record CallCenterRegistroCompletoResponse(
        CallCenterResponse registro,
        CallCenterGestionLlamadaResponse llamada,
        CallCenterVisitaResponse visita
) {
}