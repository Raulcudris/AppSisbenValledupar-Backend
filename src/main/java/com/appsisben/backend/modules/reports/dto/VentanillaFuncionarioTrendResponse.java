package com.appsisben.backend.modules.reports.dto;

import java.time.LocalDate;

public record VentanillaFuncionarioTrendResponse(
        LocalDate fecha,
        String funcionarioUsername,
        Long total
) {
}
