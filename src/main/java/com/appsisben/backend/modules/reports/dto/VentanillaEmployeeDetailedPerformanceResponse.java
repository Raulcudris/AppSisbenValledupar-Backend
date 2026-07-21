package com.appsisben.backend.modules.reports.dto;

import java.util.List;

public record VentanillaEmployeeDetailedPerformanceResponse(
        Long funcionarioId,
        String funcionarioUsername,
        Long totalAtenciones,
        Double porcentaje,
        Double promedioDiario,
        Long pendientes,
        Long realizadas,
        Long aprobadas,
        Long rechazadas,
        Long canceladas,
        Long revisar,
        Long nacionales,
        Long extranjeros,
        List<VentanillaEmployeeDailyDetailResponse> detalleDiario
) {
}
