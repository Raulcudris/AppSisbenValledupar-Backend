package com.appsisben.backend.modules.callcenter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Solicitud administrativa para incorporar un ciudadano
 * de última hora a una jornada de encuestas.
 *
 * <p>La operación crea el caso, asigna el funcionario Call Center
 * y crea la visita del encuestador dentro de una misma transacción.</p>
 */
public record CallCenterUltimaHoraRequest(

        @NotNull(message = "La fecha del caso es obligatoria")
        LocalDate fechaCaso,

        Long ventanillaRegistroId,

        @NotBlank(message = "La cédula del solicitante es obligatoria")
        @Size(
                max = 30,
                message = "La cédula no puede superar los 30 caracteres"
        )
        String cedulaSolicitante,

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(
                max = 250,
                message = "El nombre completo no puede superar los 250 caracteres"
        )
        String nombreCompleto,

        @Size(
                max = 30,
                message = "El teléfono no puede superar los 30 caracteres"
        )
        String telefono,

        @NotBlank(message = "La dirección es obligatoria")
        @Size(
                max = 500,
                message = "La dirección no puede superar los 500 caracteres"
        )
        String direccionTexto,

        Long barrioId,

        @NotNull(
                message = "Debe seleccionar el funcionario Call Center"
        )
        Long funcionarioCallcenterId,

        @NotNull(message = "Debe seleccionar el encuestador")
        Long encuestadorId,

        @NotNull(
                message = "La fecha programada de la visita es obligatoria"
        )
        LocalDate fechaProgramada,

        LocalTime horaProgramada,

        String observacion
) {
}