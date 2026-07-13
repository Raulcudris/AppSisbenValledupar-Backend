package com.appsisben.backend.modules.reports.dto;

public record ReportGroupResponse(
        Long id,
        String codigo,
        String nombre,
        Long total
) {
}
