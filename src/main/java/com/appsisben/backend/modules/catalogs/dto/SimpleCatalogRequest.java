package com.appsisben.backend.modules.catalogs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SimpleCatalogRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 180, message = "El nombre no puede superar 180 caracteres")
        String nombre,

        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String descripcion,

        Boolean activo
) {
}
