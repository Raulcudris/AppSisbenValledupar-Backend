package com.appsisben.backend.modules.territory.dto;

public record BarrioResponse(
        Long id,
        Long comunaId,
        String comunaNombre,
        String nombre,
        Boolean activo
) {
}
