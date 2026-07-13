package com.appsisben.backend.modules.users.dto;

public record UserResponse(
        Long id,
        String username,
        String documento,
        String nombres,
        String apellidos,
        String email,
        String telefono,
        Boolean activo,
        String rolCodigo,
        String rolNombre
) {
}
