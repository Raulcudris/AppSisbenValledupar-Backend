package com.appsisben.backend.modules.ventanilla.dto;

import java.util.List;

public record VentanillaDailyValidationResponse(
        String estado,
        String titulo,
        String mensaje,
        Boolean puedeContinuar,
        Boolean requiereConfirmacion,
        Long totalSolicitudesMismaFecha,
        List<VentanillaDailyRequestItemResponse> solicitudesMismaFecha
) {
}