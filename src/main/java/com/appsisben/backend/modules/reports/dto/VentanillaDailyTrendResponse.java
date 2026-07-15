package com.appsisben.backend.modules.reports.dto;
import java.time.LocalDate;

public record VentanillaDailyTrendResponse(
        LocalDate fecha,
        Long total
) {
}
