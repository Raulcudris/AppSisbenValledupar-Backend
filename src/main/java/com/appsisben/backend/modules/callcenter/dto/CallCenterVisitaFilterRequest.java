package com.appsisben.backend.modules.callcenter.dto;

import java.time.LocalDate;

/**
 * Filtros de búsqueda para consultar visitas asignadas del módulo Call Center.
 *
 * <p>Este DTO permite filtrar las visitas por texto general, estado operativo,
 * estado formal del caso maestro, condición funcional y rango de fecha
 * programada.</p>
 *
 * @param q texto de búsqueda general.
 * @param estadoVisita estado operativo de la visita.
 * @param estadoCaso estado formal del caso maestro.
 * @param condicion condición funcional de la visita o del caso.
 * @param fechaDesde fecha programada inicial.
 * @param fechaHasta fecha programada final.
 */
public record CallCenterVisitaFilterRequest(
        String q,
        String estadoVisita,
        String estadoCaso,
        String condicion,
        LocalDate fechaDesde,
        LocalDate fechaHasta
) {}