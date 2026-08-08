package com.appsisben.backend.modules.callcenter.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Filtros disponibles para la consulta de visitas
 * del flujo de Call Center.
 */
public record CallCenterVisitaFilterRequest(

        String q,

        String estadoVisita,

        String estadoCaso,

        String condicion,

        @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE
        )
        LocalDate fechaDesde,

        @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE
        )
        LocalDate fechaHasta,

        Long encuestadorId

) {
}