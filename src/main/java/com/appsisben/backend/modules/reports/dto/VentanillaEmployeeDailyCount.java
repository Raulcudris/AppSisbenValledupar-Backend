package com.appsisben.backend.modules.reports.dto;

import java.time.LocalDate;

public record VentanillaEmployeeDailyCount(
        LocalDate fecha,
        Long funcionarioId,
        String funcionarioUsername,
        Long total
) {
}