package com.appsisben.backend.modules.callcenter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record CallCenterRequest(
        LocalDateTime marcaTemporal,

        @NotNull(message = "La fecha de llamada es obligatoria")
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

        @NotNull(message = "Debe indicar si la llamada fue conectada")
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
        String observacion,
        Boolean activo
) {
}
