package com.appsisben.backend.modules.reports.dto;

public record VentanillaReportSummaryResponse(
        Long totalRegistros,
        Long pendientes,
        Long realizadas,
        Long aprobadas,
        Long rechazadas,
        Long canceladas,
        Long revisar,
        Long extranjeros,
        Long nacionales
) {
}
