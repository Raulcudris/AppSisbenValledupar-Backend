package com.appsisben.backend.modules.reports.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record ReportDateRangeRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaInicio,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaFin
) {
}
