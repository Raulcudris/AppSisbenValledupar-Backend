package com.appsisben.backend.modules.reports.dto;

public record VentanillaFuncionarioPerformanceResponse(
        Long funcionarioId,
        String funcionarioUsername,
        Long total,
        Double porcentaje,
        Double promedioDiario
) {
}
