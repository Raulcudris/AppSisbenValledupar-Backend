package com.appsisben.backend.modules.directory.dto;

public record DirectoryContactResponse(
        Long id,
        String nombre,
        String telefono,
        String perfil,
        String email,
        String entidad,
        Boolean activo
) {
}
