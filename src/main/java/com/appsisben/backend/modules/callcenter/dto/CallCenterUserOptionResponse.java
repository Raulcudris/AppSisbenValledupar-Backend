package com.appsisben.backend.modules.callcenter.dto;

public record CallCenterUserOptionResponse(
        Long id,
        String username,
        String nombreCompleto,
        String rolCodigo,
        Boolean activo
) {
}
