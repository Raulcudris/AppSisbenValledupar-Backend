package com.appsisben.backend.modules.territory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear o actualizar comunas.
 *
 * Nota técnica:
 * - El campo codigo se conserva como opcional para compatibilidad con formularios anteriores.
 * - En la creación, el backend genera automáticamente el código en formato C001, C002, C003...
 * - En la actualización, el código existente no se modifica para conservar la trazabilidad.
 */
public record ComunaRequest(
        @Size(max = 50, message = "El código no puede superar 50 caracteres")
        String codigo,

        @NotBlank(message = "El nombre de la comuna es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombre,

        @NotNull(message = "El estrato de la comuna es obligatorio")
        @Min(value = 1, message = "El estrato mínimo permitido es 1")
        @Max(value = 6, message = "El estrato máximo permitido es 6")
        Integer estrato,

        @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres")
        String descripcion,

        Boolean activo
) {
}
