package com.appsisben.backend.modules.territory.dto;

public record ComunaResponse(
        Long id,
        String codigo,
        String nombre,
        Boolean activo
) {
}
