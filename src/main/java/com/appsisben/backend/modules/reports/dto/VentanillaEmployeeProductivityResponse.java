package com.appsisben.backend.modules.reports.dto;

import java.time.LocalDate;

public record VentanillaEmployeeProductivityResponse(
        String periodo,
        LocalDate fechaInicioPeriodo,
        LocalDate fechaFinPeriodo,
        Long funcionarioId,
        String funcionarioUsername,
        Long totalAtenciones,
        Double porcentaje,
        Double promedioDiario
) {
}