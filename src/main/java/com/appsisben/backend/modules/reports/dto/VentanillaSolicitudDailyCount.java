package com.appsisben.backend.modules.reports.dto;

import java.time.LocalDate;

public record VentanillaSolicitudDailyCount(
        String solicitudNombre,
        LocalDate fecha,
        Long cantidad
) {
}