package com.appsisben.backend.modules.reports.dto;

import java.time.LocalDate;
import java.util.List;

public record VentanillaSolicitudPreviewResponse(
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String tipoAgrupacion,
        List<String> fechas,
        Long totalGeneral,
        List<VentanillaSolicitudPreviewRowResponse> filas
) {
}