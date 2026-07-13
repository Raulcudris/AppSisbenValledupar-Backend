package com.appsisben.backend.modules.dmc.repository;

import com.appsisben.backend.modules.dmc.domain.DmcRegistro;
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
            where (:fechaInicio is null or d.fecha >= :fechaInicio)
              and (:fechaFin is null or d.fecha <= :fechaFin)
            """)
    Long countInDateRange(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select coalesce(sum(d.cantidad), 0)
            from DmcRegistro d
            where (:fechaInicio is null or d.fecha >= :fechaInicio)
              and (:fechaFin is null or d.fecha <= :fechaFin)
            """)
    Long sumCantidadInDateRange(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select coalesce(sum(d.cantidad), 0)
            from DmcRegistro d
            where (:fechaInicio is null or d.fecha >= :fechaInicio)
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
            where (:fechaInicio is null or d.fecha >= :fechaInicio)
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
            where (:fechaInicio is null or d.fecha >= :fechaInicio)
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
            where (:fechaInicio is null or d.fecha >= :fechaInicio)
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
            where (:fechaInicio is null or d.fecha >= :fechaInicio)
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
            where (:fechaInicio is null or d.fecha >= :fechaInicio)
              and (:fechaFin is null or d.fecha <= :fechaFin)
            group by d.barrio.comuna.id, d.barrio.comuna.codigo, d.barrio.comuna.nombre
            order by coalesce(sum(d.cantidad), 0) desc
            """)
    List<ReportGroupResponse> sumCantidadByComuna(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );
}
