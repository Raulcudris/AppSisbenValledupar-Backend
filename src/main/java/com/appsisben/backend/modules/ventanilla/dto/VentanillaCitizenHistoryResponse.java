package com.appsisben.backend.modules.ventanilla.dto;

import java.time.LocalDate;
import java.util.List;

public record VentanillaCitizenHistoryResponse(
        String cedulaUsuario,
        String nombreUsuario,
        String telefono,
        Long totalVisitas,
        Long totalSolicitudes,
        LocalDate ultimaVisita,
        List<VentanillaDailyRequestItemResponse> solicitudes
) {
}