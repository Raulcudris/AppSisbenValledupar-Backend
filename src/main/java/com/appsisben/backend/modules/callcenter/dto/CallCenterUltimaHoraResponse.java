package com.appsisben.backend.modules.callcenter.dto;

/**
 * Resultado de la incorporación de un ciudadano de última hora.
 *
 * @param registro caso maestro creado y actualizado.
 * @param visita visita asignada al encuestador.
 */
public record CallCenterUltimaHoraResponse(
        CallCenterResponse registro,
        CallCenterVisitaResponse visita
) {
}