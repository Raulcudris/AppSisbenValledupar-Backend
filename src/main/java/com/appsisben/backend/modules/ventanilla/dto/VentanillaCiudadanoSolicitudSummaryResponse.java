package com.appsisben.backend.modules.ventanilla.dto;

import java.time.LocalDate;

public record VentanillaCiudadanoSolicitudSummaryResponse(
        Long solicitudId,
        String solicitudNombre,
        Long cantidad,
        LocalDate ultimaFecha
) {
}