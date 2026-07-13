package com.appsisben.backend.modules.ventanilla.dto;

import java.time.LocalDate;

public record VentanillaUserHistoryItemResponse(
        Long id,
        LocalDate fecha,
        String numeroVentanilla,
        Long funcionarioId,
        String funcionarioUsername,
        String cedulaUsuario,
        String nombreUsuario,
        String telefono,
        Long categoriaId,
        String categoriaNombre,
        String direccion,
        Long barrioId,
        String barrioNombre,
        String comunaNombre,
        Boolean extranjero,
        Long solicitudId,
        String solicitudNombre,
        Long estadoSolicitudId,
        String estadoSolicitudNombre,
        String observacion
) {
}
