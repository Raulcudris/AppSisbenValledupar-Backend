package com.appsisben.backend.modules.ventanilla.dto;

import java.time.LocalDate;
import java.util.List;

public record VentanillaUserHistoryResponse(
        String cedulaUsuario,
        String nombreUsuario,
        String telefono,
        Long totalVisitas,
        Long totalSolicitudes,
        LocalDate primeraVisita,
        LocalDate ultimaVisita,
        List<VentanillaUserHistoryItemResponse> solicitudes
) {
}