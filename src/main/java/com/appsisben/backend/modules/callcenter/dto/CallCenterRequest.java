package com.appsisben.backend.modules.callcenter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO de solicitud para crear o actualizar casos del módulo Call Center.
 *
 * Este DTO soporta el flujo legacy de llamada y visita, y también el flujo
 * formal donde el caso nace desde Ventanilla o desde registro manual como
 * pendiente de enrutamiento.
 */
public record CallCenterRequest(
        LocalDateTime marcaTemporal,

        @NotNull(message = "La fecha del caso es obligatoria")
        LocalDate fechaLlamada,

        LocalTime horaLlamada,

        @Size(max = 40, message = "El tipo de registro no puede superar los 40 caracteres")
        String tipoRegistro,

        @Size(max = 40, message = "El origen del registro no puede superar los 40 caracteres")
        String origenRegistro,

        Long ventanillaRegistroId,

        @NotBlank(message = "La cédula del solicitante es obligatoria")
        @Size(max = 30, message = "La cédula no puede superar los 30 caracteres")
        String cedulaSolicitante,

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 250, message = "El nombre completo no puede superar los 250 caracteres")
        String nombreCompleto,

        @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
        String telefono,

        /**
         * Indica si la llamada fue conectada.
         *
         * Puede ser null cuando el caso apenas se crea desde Ventanilla
         * y todavía no existe una gestión de llamada registrada.
         */
        Boolean llamadaConectada,

        Long motivoNoContactoId,

        @Size(max = 500, message = "El motivo de no contacto no puede superar los 500 caracteres")
        String motivoNoContactoTexto,

        Long encuestadorProgramadoId,
        LocalDate fechaEncuestaProgramada,

        Boolean solicitoNuevaEncuesta,

        @Size(max = 500, message = "La dirección no puede superar los 500 caracteres")
        String direccionTexto,

        Long barrioId,
        LocalDate fechaAplicacionInformada,
        Boolean disposicionRecibirEncuesta,
        Long motivoNoDisposicionId,

        @Size(max = 500, message = "El motivo de no disposición no puede superar los 500 caracteres")
        String motivoNoDisposicionTexto,

        Long encuestadorAsignadoId,
        Boolean explicoInformanteCalificado,
        Boolean verificado,

        /**
         * Estado formal del caso Call Center.
         *
         * Ejemplos: PENDIENTE_ENRUTAMIENTO, ASIGNADO_CALLCENTER,
         * EN_GESTION_LLAMADA, VISITA_PROGRAMADA, CERRADO.
         */
        @Size(max = 60, message = "El estado del caso no puede superar los 60 caracteres")
        String estadoCaso,

        @Size(max = 60, message = "El tipo de solicitud Call Center no puede superar los 60 caracteres")
        String tipoSolicitudCallcenter,

        String observacion,
        Boolean activo
) {
}