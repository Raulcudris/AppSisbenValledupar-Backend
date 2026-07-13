package com.appsisben.backend.modules.ventanilla.dto;


import java.time.LocalDate;

public record VentanillaUserHistorySummaryResponse(
        String cedulaUsuario,
        String nombreUsuario,
        String telefono,
        Long totalVisitas,
        Long totalSolicitudes,
        LocalDate primeraVisita,
        LocalDate ultimaVisita
) {
}