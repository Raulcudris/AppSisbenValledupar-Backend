package com.appsisben.backend.modules.catalogs.dto;

public record CodeCatalogResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Boolean activo
) {
}
