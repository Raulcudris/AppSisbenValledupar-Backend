package com.appsisben.backend.modules.catalogos.dto;

import com.appsisben.backend.modules.catalogos.domain.Comuna;

public record ComunaResponse(
        Long id,
        String codigo,
        String nombre,
        Boolean activo
) {

    public static ComunaResponse from(Comuna comuna) {
        return new ComunaResponse(
                comuna.getId(),
                comuna.getCodigo(),
                comuna.getNombre(),
                comuna.getActivo()
        );
    }
}
