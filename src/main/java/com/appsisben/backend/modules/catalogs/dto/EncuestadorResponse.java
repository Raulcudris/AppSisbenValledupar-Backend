package com.appsisben.backend.modules.catalogs.dto;

public record EncuestadorResponse(
        Long id,
        String nombre,
        String documento,
        String telefono,
        Boolean activo
) {
}
