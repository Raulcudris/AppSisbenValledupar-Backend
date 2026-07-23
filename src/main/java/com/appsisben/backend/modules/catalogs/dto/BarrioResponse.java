package com.appsisben.backend.modules.catalogos.dto;

import com.appsisben.backend.modules.catalogos.domain.Barrio;

import java.time.LocalDateTime;

public record BarrioResponse(
        Long id,
        String nombre,
        Boolean activo,
        Long comunaId,
        String comunaCodigo,
        String comunaNombre,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {

    public static BarrioResponse from(Barrio barrio) {
        return new BarrioResponse(
                barrio.getId(),
                barrio.getNombre(),
                barrio.getActivo(),
                barrio.getComuna() != null ? barrio.getComuna().getId() : null,
                barrio.getComuna() != null ? barrio.getComuna().getCodigo() : null,
                barrio.getComuna() != null ? barrio.getComuna().getNombre() : null,
                barrio.getCreadoEn(),
                barrio.getActualizadoEn()
        );
    }
}
