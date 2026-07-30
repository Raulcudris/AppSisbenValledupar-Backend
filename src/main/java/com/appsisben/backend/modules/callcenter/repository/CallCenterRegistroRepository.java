package com.appsisben.backend.modules.callcenter.repository;

import com.appsisben.backend.modules.callcenter.domain.CallCenterRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CallCenterRegistroRepository
        extends JpaRepository<CallCenterRegistro, Long>,
        JpaSpecificationExecutor<CallCenterRegistro> {

    @Query("""
            select c
            from CallCenterRegistro c
            left join fetch c.encuestadorAsignado ea
            left join fetch c.encuestadorProgramado ep
            where c.activo = true
              and (:excludeId is null or c.id <> :excludeId)
              and c.solicitoNuevaEncuesta = true
              and coalesce(c.encuestaRealizada, false) = false
              and (
                    c.encuestadorAsignado is not null
                    or c.encuestadorProgramado is not null
                  )
              and (
                    (
                        :ventanillaRegistroId is not null
                        and c.ventanillaRegistro.id = :ventanillaRegistroId
                    )
                    or
                    (
                        :cedulaSolicitante is not null
                        and c.cedulaSolicitante = :cedulaSolicitante
                    )
                  )
            order by c.id desc
            """)
    List<CallCenterRegistro> findAsignacionNuevaEncuestaPendiente(
            @Param("excludeId") Long excludeId,
            @Param("ventanillaRegistroId") Long ventanillaRegistroId,
            @Param("cedulaSolicitante") String cedulaSolicitante
    );

    /**
     * Calcula el resumen del módulo directamente en base de datos.
     *
     * Cuando funcionarioId es null, incluye todos los casos.
     * Cuando contiene un ID, incluye únicamente los casos asignados
     * a ese funcionario Call Center.
     */
    @Query("""
            select
                count(c) as total,
                coalesce(
                    sum(
                        case
                            when c.llamadaConectada = true then 1
                            else 0
                        end
                    ),
                    0
                ) as conectadas,
                coalesce(
                    sum(
                        case
                            when c.llamadaConectada = false then 1
                            else 0
                        end
                    ),
                    0
                ) as noConectadas,
                coalesce(
                    sum(
                        case
                            when c.activo = true then 1
                            else 0
                        end
                    ),
                    0
                ) as activos,
                coalesce(
                    sum(
                        case
                            when c.activo = true then 0
                            else 1
                        end
                    ),
                    0
                ) as inactivos
            from CallCenterRegistro c
            where (
                    :funcionarioId is null
                    or c.funcionarioCallcenterAsignado.id = :funcionarioId
                  )
            """)
    CallCenterSummaryProjection summarize(
            @Param("funcionarioId") Long funcionarioId
    );
}