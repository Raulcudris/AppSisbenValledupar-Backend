package com.appsisben.backend.modules.reports.dto;

public record DmcEncuestadorPerformanceResponse(
        Long encuestadorId,
        String encuestadorNombre,
        Long cargadas,
        Long efectivas,
        Long noEfectivas,
        Double cumplimiento,
        String desempeno,
        Long total
) {
}