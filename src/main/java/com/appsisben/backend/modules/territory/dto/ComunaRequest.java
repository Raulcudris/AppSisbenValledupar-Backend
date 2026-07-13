package com.appsisben.backend.modules.territory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComunaRequest(
        @NotBlank(message = "El código de la comuna es obligatorio")
        @Size(max = 50, message = "El código no puede superar 50 caracteres")
        String codigo,

        @NotBlank(message = "El nombre de la comuna es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombre,

        Boolean activo
) {
}
