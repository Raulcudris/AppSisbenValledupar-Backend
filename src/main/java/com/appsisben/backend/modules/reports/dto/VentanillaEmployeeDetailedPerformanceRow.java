package com.appsisben.backend.modules.reports.dto;


public record VentanillaEmployeeDetailedPerformanceRow(
        Long funcionarioId,
        String funcionarioUsername,
        Long totalAtenciones,
        Long pendientes,
        Long realizadas,
        Long aprobadas,
        Long rechazadas,
        Long canceladas,
        Long revisar,
        Long nacionales,
        Long extranjeros
) {
}