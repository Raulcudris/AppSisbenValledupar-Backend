package com.appsisben.backend.modules.reports.dto;
import java.time.LocalDate;
public record VentanillaFrequentCitizenResponse(
        String cedulaUsuario,
        String nombreUsuario,
        String telefono,
        Long totalVisitas,
        Long totalSolicitudes,
        LocalDate primeraVisita,
        LocalDate ultimaVisita
) {
}
