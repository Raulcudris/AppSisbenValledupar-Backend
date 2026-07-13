package com.appsisben.backend.modules.ventanilla.dto;

import java.time.LocalDate;
import java.util.List;

public record VentanillaCiudadanoTraceabilityResponse(
        String cedulaUsuario,
        String nombreUsuario,
        String telefono,
        LocalDate primeraVisita,
        LocalDate ultimaVisita,
        Long diasDesdeUltimaVisita,
        Long totalVisitas,
        Long visitasUltimos30Dias,
        Boolean ciudadanoFrecuente,
        List<VentanillaCiudadanoSolicitudSummaryResponse> solicitudes,
        List<VentanillaCiudadanoVisitResponse> historial
) {
}