package com.appsisben.backend.modules.catalogs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EncuestadorRequest(
        @NotBlank(message = "El nombre del encuestador es obligatorio")
        @Size(max = 180, message = "El nombre no puede superar 180 caracteres")
        String nombre,

        @Size(max = 30, message = "El documento no puede superar 30 caracteres")
        String documento,

        @Size(max = 40, message = "El teléfono no puede superar 40 caracteres")
        String telefono,

        Boolean activo
) {
}
