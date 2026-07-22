package com.appsisben.backend.modules.reports.dto;

public record DmcEncuestadorPerformanceRow(
        Long encuestadorId,
        String encuestadorNombre,
        Long cargadas,
        Long efectivas
) {
}