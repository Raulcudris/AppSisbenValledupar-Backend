package com.appsisben.backend.modules.reports.dto;

import java.time.LocalDate;

public record VentanillaEmployeeDailyDetailResponse(
        LocalDate fecha,
        Long funcionarioId,
        String funcionarioUsername,
        Long total
) {
}
