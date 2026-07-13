package com.appsisben.backend.modules.catalogs.dto;

public record SimpleCatalogResponse(
        Long id,
        String nombre,
        String descripcion,
        Boolean activo
) {
}
