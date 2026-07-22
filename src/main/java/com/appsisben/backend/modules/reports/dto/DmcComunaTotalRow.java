package com.appsisben.backend.modules.reports.dto;

public record DmcComunaTotalRow(
        Long comunaId,
        String comunaCodigo,
        String comunaNombre,
        Long cargadas,
        Long descargadas
) {
}
