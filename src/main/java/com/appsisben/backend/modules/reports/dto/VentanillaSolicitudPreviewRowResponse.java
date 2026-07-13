package com.appsisben.backend.modules.reports.dto;

import java.math.BigDecimal;
import java.util.Map;

public record VentanillaSolicitudPreviewRowResponse(
        String solicitud,
        Map<String, Long> cantidadesPorFecha,
        Long totalGeneral,
        BigDecimal porcentaje
) {
}