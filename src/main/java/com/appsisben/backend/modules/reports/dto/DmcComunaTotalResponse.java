package com.appsisben.backend.modules.reports.dto;

public record DmcComunaTotalResponse(
        Long comunaId,
        String comunaCodigo,
        String comunaNombre,
        Long cargadas,
        Long descargadas,
        Long total
) {
}
