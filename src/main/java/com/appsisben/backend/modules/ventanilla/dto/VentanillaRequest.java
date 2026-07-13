package com.appsisben.backend.modules.ventanilla.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record VentanillaRequest(
        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotBlank(message = "El número de ventanilla es obligatorio")
        @Size(max = 80, message = "El número de ventanilla no puede superar 80 caracteres")
        String numeroVentanilla,

        @NotBlank(message = "La cédula del usuario es obligatoria")
        @Pattern(regexp = "^[0-9]+$", message = "La cédula debe contener solo números")
        @Size(max = 30, message = "La cédula no puede superar 30 caracteres")
        String cedulaUsuario,

        @NotBlank(message = "El nombre del usuario es obligatorio")
        @Size(max = 250, message = "El nombre no puede superar 250 caracteres")
        String nombreUsuario,

        @Size(max = 40, message = "El teléfono no puede superar 40 caracteres")
        String telefono,

        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId,

        @Size(max = 350, message = "La dirección no puede superar 350 caracteres")
        String direccion,

        @NotNull(message = "El barrio es obligatorio")
        Long barrioId,

        Boolean extranjero,

        @NotNull(message = "La solicitud es obligatoria")
        Long solicitudId,

        @NotNull(message = "El estado de solicitud es obligatorio")
        Long estadoSolicitudId,

        String observacion
) {
}
