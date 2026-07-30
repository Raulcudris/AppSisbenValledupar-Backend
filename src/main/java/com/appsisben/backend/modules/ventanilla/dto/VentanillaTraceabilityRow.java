package com.appsisben.backend.modules.ventanilla.dto;

import java.time.LocalDate;

public record VentanillaTraceabilityRow(
        Long id,
        String cedulaUsuario,
        LocalDate fecha
) {
}