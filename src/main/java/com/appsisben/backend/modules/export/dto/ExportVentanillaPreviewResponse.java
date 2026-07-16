package com.appsisben.backend.modules.export.dto;

import java.time.LocalDate;

public record ExportVentanillaPreviewResponse(
        Long id,
        LocalDate fecha,
        String numeroVentanilla,
        String funcionarioUsername,
        String cedulaUsuario,
        String nombreUsuario,
        String telefono,
        String categoriaNombre,
        String direccion,
        String barrioNombre,
        String comunaNombre,
        Boolean extranjero,
        String solicitudNombre,
        String estadoSolicitudNombre,
        String estadoRegistro,
        String observacion
) {
}