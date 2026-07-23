package com.appsisben.backend.modules.catalogos.dto;

public record BarrioRequest(
        String nombre,
        Long comunaId,
        Boolean activo
) {
}
