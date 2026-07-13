package com.appsisben.backend.modules.users.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "La nueva contraseña es obligatoria")
        String newPassword,

        @NotBlank(message = "La confirmación de contraseña es obligatoria")
        String confirmPassword
) {
}