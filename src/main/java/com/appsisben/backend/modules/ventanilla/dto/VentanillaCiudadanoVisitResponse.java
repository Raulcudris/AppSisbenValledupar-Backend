package com.appsisben.backend.modules.ventanilla.dto;

import java.time.LocalDate;

public record VentanillaCiudadanoVisitResponse(
        Long id,
        LocalDate fecha,
        String numeroVentanilla,
        Long solicitudId,
        String solicitudNombre,
        Long estadoSolicitudId,
        String estadoSolicitudNombre,
        Long categoriaId,
        String categoriaNombre,
        String barrioNombre,
        String comunaNombre,
        Long funcionarioId,
        String funcionarioUsername,
        String observacion
) {
}