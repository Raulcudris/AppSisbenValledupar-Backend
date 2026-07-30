package com.appsisben.backend.modules.callcenter.repository;

/**
 * Proyección interna para el resumen agregado del módulo Call Center.
 */
public interface CallCenterSummaryProjection {

    Long getTotal();

    Long getConectadas();

    Long getNoConectadas();

    Long getActivos();

    Long getInactivos();
}