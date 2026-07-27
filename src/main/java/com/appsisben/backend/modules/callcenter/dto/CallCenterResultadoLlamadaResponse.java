package com.appsisben.backend.modules.callcenter.dto;

/**
 * Respuesta del catálogo de resultados de llamada del módulo Call Center.
 *
 * @param id identificador del resultado.
 * @param codigo código técnico.
 * @param nombre nombre visible.
 * @param descripcion descripción funcional.
 * @param estadoCasoSugerido estado sugerido para el caso maestro.
 * @param activo indica si el resultado está activo.
 */
public record CallCenterResultadoLlamadaResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        String estadoCasoSugerido,
        Boolean activo
) {}
