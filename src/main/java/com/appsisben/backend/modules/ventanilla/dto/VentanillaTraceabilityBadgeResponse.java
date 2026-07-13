package com.appsisben.backend.modules.ventanilla.dto;

import java.time.LocalDate;

public record VentanillaTraceabilityBadgeResponse(
        String nivel,
        String etiqueta,
        String color,
        String descripcion,
        Long totalVisitas,
        Long visitasUltimos30Dias,
        LocalDate ultimaVisitaAnterior,
        Long diasDesdeUltimaVisitaAnterior,
        Boolean ciudadanoFrecuente
) {
}