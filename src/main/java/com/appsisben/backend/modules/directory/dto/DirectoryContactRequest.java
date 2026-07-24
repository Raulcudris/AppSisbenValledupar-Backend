package com.appsisben.backend.modules.directory.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DirectoryContactRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 180, message = "El nombre no puede superar 180 caracteres")
        String nombre,

        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 40, message = "El teléfono no puede superar 40 caracteres")
        String telefono,

        @NotBlank(message = "El perfil es obligatorio")
        @Size(max = 120, message = "El perfil no puede superar 120 caracteres")
        String perfil,

        @Email(message = "El email no tiene un formato válido")
        @Size(max = 150, message = "El email no puede superar 150 caracteres")
        String email,

        @Size(max = 180, message = "La entidad no puede superar 180 caracteres")
        String entidad,

        Boolean activo
) {
}
