package com.appsisben.backend.modules.roles.dto;

public record RoleResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Boolean activo
) {
}
