package com.appsisben.backend.modules.ventanilla.dto;

import jakarta.validation.constraints.NotNull;

public record VentanillaActiveStatusRequest(

        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo

) {
}