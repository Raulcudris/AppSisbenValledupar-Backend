package com.appsisben.backend.modules.auth.dto;

public record AuthUserResponse(
        Long id,
        String username,
        String nombres,
        String apellidos,
        String rolCodigo,
        String rolNombre
) {
}
