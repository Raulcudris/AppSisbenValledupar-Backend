package com.appsisben.backend.modules.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 80, message = "El usuario no puede superar 80 caracteres")
        String username,

        @Size(max = 30, message = "El documento no puede superar 30 caracteres")
        String documento,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 120, message = "Los nombres no pueden superar 120 caracteres")
        String nombres,

        @Size(max = 120, message = "Los apellidos no pueden superar 120 caracteres")
        String apellidos,

        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String email,

        @Size(max = 40, message = "El teléfono no puede superar 40 caracteres")
        String telefono,

        Boolean activo,

        @NotBlank(message = "El rol es obligatorio")
        String rolCodigo
) {
}