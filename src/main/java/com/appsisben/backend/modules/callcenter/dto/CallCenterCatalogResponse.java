package com.appsisben.backend.modules.callcenter.dto;

public record CallCenterCatalogResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Boolean activo
) {
}
