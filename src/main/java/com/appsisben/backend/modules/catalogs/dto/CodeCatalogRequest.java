package com.appsisben.backend.modules.catalogs.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CodeCatalogRequest(
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 50, message = "El código no puede superar 50 caracteres")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombre,

        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String descripcion,

        Boolean activo
) {
}
