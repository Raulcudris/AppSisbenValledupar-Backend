package com.appsisben.backend.modules.callcenter.dto;

public record CallCenterSummaryResponse(
        long totalRegistros,
        long llamadasConectadas,
        long llamadasNoConectadas,
        long activos,
        long inactivos
) {
}
