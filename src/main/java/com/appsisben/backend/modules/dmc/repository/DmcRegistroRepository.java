package com.appsisben.backend.modules.dmc.repository;

import com.appsisben.backend.modules.dmc.domain.DmcRegistro;
import com.appsisben.backend.modules.reports.dto.DmcComunaTotalRow;
import com.appsisben.backend.modules.reports.dto.DmcEncuestadorPerformanceRow;
import com.appsisben.backend.modules.reports.dto.ReportGroupResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DmcRegistroRepository extends JpaRepository<DmcRegistro, Long>,
        JpaSpecificationExecutor<DmcRegistro> {

    @Query("""
            select count(d)
            from DmcRegistro d
            where d.activo = true
              and (:fechaInicio is null or d.fecha >= :fechaInicio)
              and (:fechaFin is null or d.fecha <= :fechaFin)
            """)
    Long countInDateRange(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select coalesce(sum(d.cantidad), 0)
            from DmcRegistro d
            where d.activo = true
              and (:fechaInicio is null or d.fecha >= :fechaInicio)
              and (:fechaFin is null or d.fecha <= :fechaFin)
            """)
    Long sumCantidadInDateRange(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select coalesce(sum(d.cantidad), 0)
            from DmcRegistro d
            where d.activo = true
              and (:fechaInicio is null or d.fecha >= :fechaInicio)
              and (:fechaFin is null or d.fecha <= :fechaFin)
              and upper(d.tipoDmc.codigo) = upper(:codigo)
            """)
    Long sumCantidadByTipoCodigo(
            @Param("codigo") String codigo,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select new com.appsisben.backend.modules.reports.dto.ReportGroupResponse(
                d.tipoDmc.id,
                d.tipoDmc.codigo,
                d.tipoDmc.nombre,
                coalesce(sum(d.cantidad), 0)
            )
            from DmcRegistro d
            where d.activo = true
              and (:fechaInicio is null or d.fecha >= :fechaInicio)
              and (:fechaFin is null or d.fecha <= :fechaFin)
            group by d.tipoDmc.id, d.tipoDmc.codigo, d.tipoDmc.nombre
            order by coalesce(sum(d.cantidad), 0) desc
            """)
    List<ReportGroupResponse> sumCantidadByTipoDmc(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select new com.appsisben.backend.modules.reports.dto.ReportGroupResponse(
                d.encuestador.id,
                d.encuestador.nombre,
                d.encuestador.nombre,
                coalesce(sum(d.cantidad), 0)
            )
            from DmcRegistro d
            where d.activo = true
              and (:fechaInicio is null or d.fecha >= :fechaInicio)
              and (:fechaFin is null or d.fecha <= :fechaFin)
            group by d.encuestador.id, d.encuestador.nombre
            order by coalesce(sum(d.cantidad), 0) desc
            """)
    List<ReportGroupResponse> sumCantidadByEncuestador(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select new com.appsisben.backend.modules.reports.dto.ReportGroupResponse(
                d.funcionario.id,
                d.funcionario.username,
                d.funcionario.username,
                coalesce(sum(d.cantidad), 0)
            )
            from DmcRegistro d
            where d.activo = true
              and (:fechaInicio is null or d.fecha >= :fechaInicio)
              and (:fechaFin is null or d.fecha <= :fechaFin)
            group by d.funcionario.id, d.funcionario.username
            order by coalesce(sum(d.cantidad), 0) desc
            """)
    List<ReportGroupResponse> sumCantidadByFuncionario(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select new com.appsisben.backend.modules.reports.dto.ReportGroupResponse(
                d.barrio.id,
                d.barrio.nombre,
                d.barrio.nombre,
                coalesce(sum(d.cantidad), 0)
            )
            from DmcRegistro d
            where d.activo = true
              and (:fechaInicio is null or d.fecha >= :fechaInicio)
              and (:fechaFin is null or d.fecha <= :fechaFin)
            group by d.barrio.id, d.barrio.nombre
            order by coalesce(sum(d.cantidad), 0) desc
            """)
    List<ReportGroupResponse> sumCantidadByBarrio(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select new com.appsisben.backend.modules.reports.dto.ReportGroupResponse(
                d.barrio.comuna.id,
                d.barrio.comuna.codigo,
                d.barrio.comuna.nombre,
                coalesce(sum(d.cantidad), 0)
            )
            from DmcRegistro d
            where d.activo = true
              and (:fechaInicio is null or d.fecha >= :fechaInicio)
              and (:fechaFin is null or d.fecha <= :fechaFin)
            group by d.barrio.comuna.id, d.barrio.comuna.codigo, d.barrio.comuna.nombre
            order by coalesce(sum(d.cantidad), 0) desc
            """)
    List<ReportGroupResponse> sumCantidadByComuna(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select new com.appsisben.backend.modules.reports.dto.DmcEncuestadorPerformanceRow(
                d.encuestador.id,
                d.encuestador.nombre,
                coalesce(sum(
                    case
                        when upper(d.tipoDmc.codigo) in ('CARGADA', 'CARGADAS', 'CARGADO', 'CARGADOS')
                        then d.cantidad
                        else 0
                    end
                ), 0),
                coalesce(sum(
                    case
                        when upper(d.tipoDmc.codigo) in ('DESCARGADA', 'DESCARGADAS', 'EFECTIVA', 'EFECTIVAS')
                        then d.cantidad
                        else 0
                    end
                ), 0)
            )
            from DmcRegistro d
            where d.activo = true
              and (:fechaInicio is null or d.fecha >= :fechaInicio)
              and (:fechaFin is null or d.fecha <= :fechaFin)
            group by d.encuestador.id, d.encuestador.nombre
            order by coalesce(sum(d.cantidad), 0) desc, d.encuestador.nombre asc
            """)
    List<DmcEncuestadorPerformanceRow> countDmcEncuestadorPerformance(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select new com.appsisben.backend.modules.reports.dto.DmcComunaTotalRow(
                comuna.id,
                coalesce(comuna.codigo, 'COMUNA_N/A'),
                coalesce(comuna.nombre, 'COMUNA_N/A'),
                coalesce(sum(
                    case
                        when upper(d.tipoDmc.codigo) in ('CARGADA', 'CARGADAS', 'CARGADO', 'CARGADOS')
                        then d.cantidad
                        else 0
                    end
                ), 0),
                coalesce(sum(
                    case
                        when upper(d.tipoDmc.codigo) in ('DESCARGADA', 'DESCARGADAS', 'EFECTIVA', 'EFECTIVAS')
                        then d.cantidad
                        else 0
                    end
                ), 0)
            )
            from DmcRegistro d
            left join d.barrio barrio
            left join barrio.comuna comuna
            where d.activo = true
              and (:fechaInicio is null or d.fecha >= :fechaInicio)
              and (:fechaFin is null or d.fecha <= :fechaFin)
            group by comuna.id, comuna.codigo, comuna.nombre
            order by coalesce(sum(d.cantidad), 0) desc, coalesce(comuna.nombre, 'COMUNA_N/A') asc
            """)
    List<DmcComunaTotalRow> countDmcComunaTotals(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );
}