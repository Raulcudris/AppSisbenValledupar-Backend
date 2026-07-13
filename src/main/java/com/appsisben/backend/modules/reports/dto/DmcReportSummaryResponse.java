package com.appsisben.backend.modules.reports.dto;

public record DmcReportSummaryResponse(
        Long totalRegistros,
        Long totalCantidad,
        Long totalCargadas,
        Long totalDescargadas,
        Long totalRechazadas
) {
}
