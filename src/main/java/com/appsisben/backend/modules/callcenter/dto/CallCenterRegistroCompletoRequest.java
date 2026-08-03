package com.appsisben.backend.modules.callcenter.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Solicitud transaccional para registrar un caso completo
 * desde el funcionario Call Center autenticado.
 *
 * <p>La operación crea el caso maestro, registra la primera
 * gestión telefónica y programa la visita del encuestador.</p>
 *
 * @param registro datos generales del ciudadano y de la solicitud.
 * @param llamada primera gestión o intento telefónico.
 * @param visita asignación y programación de la visita.
 */
public record CallCenterRegistroCompletoRequest(

        @Valid
        @NotNull(
                message = "Los datos del caso son obligatorios"
        )
        CallCenterRequest registro,

        @Valid
        @NotNull(
                message = "Los datos de la llamada son obligatorios"
        )
        CallCenterGestionLlamadaRequest llamada,

        @Valid
        @NotNull(
                message = "Los datos de la visita son obligatorios"
        )
        CallCenterVisitaAsignacionRequest visita

) {
}