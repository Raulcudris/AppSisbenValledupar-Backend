package com.appsisben.backend.modules.territory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BarrioRequest(
        @NotNull(message = "La comuna es obligatoria")
        Long comunaId,

        @NotBlank(message = "El nombre del barrio es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        Boolean activo
) {
}
