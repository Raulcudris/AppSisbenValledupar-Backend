package com.appsisben.backend.modules.ventanilla.repository;

import com.appsisben.backend.modules.reports.dto.*;
import com.appsisben.backend.modules.ventanilla.domain.VentanillaRegistro;
import com.appsisben.backend.modules.ventanilla.dto.VentanillaUserHistorySummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VentanillaRegistroRepository extends JpaRepository<VentanillaRegistro, Long>,
        JpaSpecificationExecutor<VentanillaRegistro> {

    @Query("""
            select count(v)
            from VentanillaRegistro v
            where v.activo = true
              and (:fechaInicio is null or v.fecha >= :fechaInicio)
              and (:fechaFin is null or v.fecha <= :fechaFin)
            """)
    Long countInDateRange(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select count(v)
            from VentanillaRegistro v
            where v.activo = true
              and (:fechaInicio is null or v.fecha >= :fechaInicio)
              and (:fechaFin is null or v.fecha <= :fechaFin)
              and upper(v.estadoSolicitud.codigo) = upper(:codigo)
            """)
    Long countByEstadoCodigo(
            @Param("codigo") String codigo,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select count(v)
            from VentanillaRegistro v
            where v.activo = true
              and (:fechaInicio is null or v.fecha >= :fechaInicio)
              and (:fechaFin is null or v.fecha <= :fechaFin)
              and v.extranjero = :extranjero
            """)
    Long countByExtranjero(
            @Param("extranjero") Boolean extranjero,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select new com.appsisben.backend.modules.reports.dto.ReportGroupResponse(
                v.estadoSolicitud.id,
                v.estadoSolicitud.codigo,
                v.estadoSolicitud.nombre,
                count(v)
            )
            from VentanillaRegistro v
            where v.activo = true
              and (:fechaInicio is null or v.fecha >= :fechaInicio)
              and (:fechaFin is null or v.fecha <= :fechaFin)
            group by v.estadoSolicitud.id, v.estadoSolicitud.codigo, v.estadoSolicitud.nombre
            order by count(v) desc
            """)
    List<ReportGroupResponse> countByEstado(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select new com.appsisben.backend.modules.reports.dto.ReportGroupResponse(
                v.solicitud.id,
                v.solicitud.nombre,
                v.solicitud.nombre,
                count(v)
            )
            from VentanillaRegistro v
            where v.activo = true
              and (:fechaInicio is null or v.fecha >= :fechaInicio)
              and (:fechaFin is null or v.fecha <= :fechaFin)
            group by v.solicitud.id, v.solicitud.nombre
            order by count(v) desc
            """)
    List<ReportGroupResponse> countBySolicitud(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select new com.appsisben.backend.modules.reports.dto.ReportGroupResponse(
                v.categoria.id,
                v.categoria.nombre,
                v.categoria.nombre,
                count(v)
            )
            from VentanillaRegistro v
            where v.activo = true
              and (:fechaInicio is null or v.fecha >= :fechaInicio)
              and (:fechaFin is null or v.fecha <= :fechaFin)
            group by v.categoria.id, v.categoria.nombre
            order by count(v) desc
            """)
    List<ReportGroupResponse> countByCategoria(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select new com.appsisben.backend.modules.reports.dto.ReportGroupResponse(
                v.funcionario.id,
                v.funcionario.username,
                v.funcionario.username,
                count(v)
            )
            from VentanillaRegistro v
            where v.activo = true
              and (:fechaInicio is null or v.fecha >= :fechaInicio)
              and (:fechaFin is null or v.fecha <= :fechaFin)
            group by v.funcionario.id, v.funcionario.username
            order by count(v) desc
            """)
    List<ReportGroupResponse> countByFuncionario(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select new com.appsisben.backend.modules.reports.dto.ReportGroupResponse(
                v.barrio.id,
                v.barrio.nombre,
                v.barrio.nombre,
                count(v)
            )
            from VentanillaRegistro v
            where v.activo = true
              and (:fechaInicio is null or v.fecha >= :fechaInicio)
              and (:fechaFin is null or v.fecha <= :fechaFin)
            group by v.barrio.id, v.barrio.nombre
            order by count(v) desc
            """)
    List<ReportGroupResponse> countByBarrio(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select new com.appsisben.backend.modules.reports.dto.ReportGroupResponse(
                v.barrio.comuna.id,
                v.barrio.comuna.codigo,
                v.barrio.comuna.nombre,
                count(v)
            )
            from VentanillaRegistro v
            where v.activo = true
              and (:fechaInicio is null or v.fecha >= :fechaInicio)
              and (:fechaFin is null or v.fecha <= :fechaFin)
            group by v.barrio.comuna.id, v.barrio.comuna.codigo, v.barrio.comuna.nombre
            order by count(v) desc
            """)
    List<ReportGroupResponse> countByComuna(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select new com.appsisben.backend.modules.reports.dto.VentanillaSolicitudDailyCount(
                v.solicitud.nombre,
                v.fecha,
                count(v)
            )
            from VentanillaRegistro v
            where v.activo = true
              and v.fecha >= :fechaInicio
              and v.fecha <= :fechaFin
            group by v.solicitud.nombre, v.fecha
            order by v.solicitud.nombre asc, v.fecha asc
            """)
    List<VentanillaSolicitudDailyCount> countSolicitudesByFecha(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select count(v)
            from VentanillaRegistro v
            where upper(v.cedulaUsuario) = upper(:cedulaUsuario)
            """)
    Long countVisitsByCedula(
            @Param("cedulaUsuario") String cedulaUsuario
    );

    @Query("""
            select count(v)
            from VentanillaRegistro v
            where upper(v.cedulaUsuario) = upper(:cedulaUsuario)
              and v.fecha >= :fechaInicio
              and v.fecha <= :fechaFin
            """)
    Long countVisitsByCedulaBetween(
            @Param("cedulaUsuario") String cedulaUsuario,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select v
            from VentanillaRegistro v
            where upper(v.cedulaUsuario) = upper(:cedulaUsuario)
              and (
                    v.fecha < :fecha
                    or (v.fecha = :fecha and v.id < :id)
              )
            order by v.fecha desc, v.id desc
            """)
    List<VentanillaRegistro> findPreviousVisitsByCedula(
            @Param("cedulaUsuario") String cedulaUsuario,
            @Param("fecha") LocalDate fecha,
            @Param("id") Long id,
            Pageable pageable
    );

    @Query("""
            select count(v)
            from VentanillaRegistro v
            where v.activo = true
              and upper(v.cedulaUsuario) = upper(:cedulaUsuario)
            """)
    Long countActiveVisitsByCedula(
            @Param("cedulaUsuario") String cedulaUsuario
    );

    @Query("""
            select count(v)
            from VentanillaRegistro v
            where v.activo = true
              and upper(v.cedulaUsuario) = upper(:cedulaUsuario)
              and v.fecha >= :fechaInicio
              and v.fecha <= :fechaFin
            """)
    Long countActiveVisitsByCedulaBetween(
            @Param("cedulaUsuario") String cedulaUsuario,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
            select v
            from VentanillaRegistro v
            where v.activo = true
              and upper(v.cedulaUsuario) = upper(:cedulaUsuario)
              and (
                    v.fecha < :fecha
                    or (v.fecha = :fecha and v.id < :id)
              )
            order by v.fecha desc, v.id desc
            """)
    List<VentanillaRegistro> findPreviousActiveVisitsByCedula(
            @Param("cedulaUsuario") String cedulaUsuario,
            @Param("fecha") LocalDate fecha,
            @Param("id") Long id,
            Pageable pageable
    );

    @Query(
            value = """
                    select new com.appsisben.backend.modules.ventanilla.dto.VentanillaUserHistorySummaryResponse(
                        v.cedulaUsuario,
                        max(v.nombreUsuario),
                        max(v.telefono),
                        count(distinct v.fecha),
                        count(v),
                        min(v.fecha),
                        max(v.fecha)
                    )
                    from VentanillaRegistro v
                    where (
                            :search is null
                            or lower(v.cedulaUsuario) like lower(concat('%', :search, '%'))
                            or lower(v.nombreUsuario) like lower(concat('%', :search, '%'))
                      )
                    group by v.cedulaUsuario
                    order by max(v.fecha) desc, max(v.id) desc
                    """,
            countQuery = """
                    select count(distinct v.cedulaUsuario)
                    from VentanillaRegistro v
                    where (
                            :search is null
                            or lower(v.cedulaUsuario) like lower(concat('%', :search, '%'))
                            or lower(v.nombreUsuario) like lower(concat('%', :search, '%'))
                      )
                    """
    )
    Page<VentanillaUserHistorySummaryResponse> findUserHistorySummaries(
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            select v
            from VentanillaRegistro v
            left join fetch v.solicitud
            left join fetch v.estadoSolicitud
            left join fetch v.funcionario
            left join fetch v.categoria
            left join fetch v.barrio b
            left join fetch b.comuna
            where v.fecha = :fecha
              and upper(v.cedulaUsuario) = upper(:cedulaUsuario)
            order by case when v.activo = true then 0 else 1 end asc, v.id desc
            """)
    List<VentanillaRegistro> findDailyRequestsByCedula(
            @Param("fecha") LocalDate fecha,
            @Param("cedulaUsuario") String cedulaUsuario
    );

    @Query("""
            select v
            from VentanillaRegistro v
            left join fetch v.solicitud
            left join fetch v.estadoSolicitud
            left join fetch v.funcionario
            left join fetch v.categoria
            left join fetch v.barrio b
            left join fetch b.comuna
            where v.fecha = :fecha
              and upper(v.cedulaUsuario) = upper(:cedulaUsuario)
              and v.id <> :currentId
            order by case when v.activo = true then 0 else 1 end asc, v.id desc
            """)
    List<VentanillaRegistro> findDailyRequestsByCedulaAndIdNot(
            @Param("fecha") LocalDate fecha,
            @Param("cedulaUsuario") String cedulaUsuario,
            @Param("currentId") Long currentId
    );

    @Query("""
            select case when count(v) > 0 then true else false end
            from VentanillaRegistro v
            where v.fecha = :fecha
              and upper(v.cedulaUsuario) = upper(:cedulaUsuario)
              and v.solicitud.id = :solicitudId
            """)
    boolean existsDailySolicitudByUser(
            @Param("fecha") LocalDate fecha,
            @Param("cedulaUsuario") String cedulaUsuario,
            @Param("solicitudId") Long solicitudId
    );

    @Query("""
            select case when count(v) > 0 then true else false end
            from VentanillaRegistro v
            where v.fecha = :fecha
              and upper(v.cedulaUsuario) = upper(:cedulaUsuario)
              and v.solicitud.id = :solicitudId
              and v.id <> :currentId
            """)
    boolean existsDailySolicitudByUserAndIdNot(
            @Param("currentId") Long currentId,
            @Param("fecha") LocalDate fecha,
            @Param("cedulaUsuario") String cedulaUsuario,
            @Param("solicitudId") Long solicitudId
    );

    @Query("""
            select v
            from VentanillaRegistro v
            left join fetch v.solicitud
            left join fetch v.estadoSolicitud
            left join fetch v.funcionario
            left join fetch v.categoria
            left join fetch v.barrio b
            left join fetch b.comuna
            where upper(v.cedulaUsuario) = upper(:cedulaUsuario)
            order by v.fecha desc, v.id desc
            """)
    List<VentanillaRegistro> findTraceabilityByCedula(
            @Param("cedulaUsuario") String cedulaUsuario
    );

    @Query("""
            select v
            from VentanillaRegistro v
            left join fetch v.solicitud
            left join fetch v.estadoSolicitud
            left join fetch v.funcionario
            left join fetch v.categoria
            left join fetch v.barrio b
            left join fetch b.comuna
            where v.activo = true
              and upper(v.cedulaUsuario) = upper(:cedulaUsuario)
            order by v.fecha desc, v.id desc
            """)
    List<VentanillaRegistro> findActiveTraceabilityByCedula(
            @Param("cedulaUsuario") String cedulaUsuario
    );

    @Query("""
        select new com.appsisben.backend.modules.reports.dto.VentanillaDailyTrendResponse(
            v.fecha,
            count(v)
        )
        from VentanillaRegistro v
        where v.activo = true
          and (:fechaInicio is null or v.fecha >= :fechaInicio)
          and (:fechaFin is null or v.fecha <= :fechaFin)
        group by v.fecha
        order by v.fecha asc
        """)
    List<VentanillaDailyTrendResponse> countDailyTrend(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
        select new com.appsisben.backend.modules.reports.dto.VentanillaFuncionarioTrendResponse(
            v.fecha,
            coalesce(v.funcionario.username, 'Sin funcionario'),
            count(v)
        )
        from VentanillaRegistro v
        where v.activo = true
          and (:fechaInicio is null or v.fecha >= :fechaInicio)
          and (:fechaFin is null or v.fecha <= :fechaFin)
        group by v.fecha, v.funcionario.username
        order by v.fecha asc, v.funcionario.username asc
        """)
    List<VentanillaFuncionarioTrendResponse> countFuncionarioTrend(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
        select new com.appsisben.backend.modules.reports.dto.VentanillaFrequentCitizenResponse(
            v.cedulaUsuario,
            max(v.nombreUsuario),
            max(v.telefono),
            count(distinct v.fecha),
            count(v),
            min(v.fecha),
            max(v.fecha)
        )
        from VentanillaRegistro v
        where v.activo = true
          and (:fechaInicio is null or v.fecha >= :fechaInicio)
          and (:fechaFin is null or v.fecha <= :fechaFin)
        group by v.cedulaUsuario
        order by count(distinct v.fecha) desc, count(v) desc, max(v.fecha) desc
        """)
    List<VentanillaFrequentCitizenResponse> findFrequentCitizens(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            Pageable pageable
    );

    @Query("""
        select new com.appsisben.backend.modules.reports.dto.VentanillaEmployeeDailyCount(
            v.fecha,
            f.id,
            coalesce(f.username, 'Sin funcionario'),
            count(v)
        )
        from VentanillaRegistro v
        left join v.funcionario f
        where v.activo = true
          and (:fechaInicio is null or v.fecha >= :fechaInicio)
          and (:fechaFin is null or v.fecha <= :fechaFin)
        group by v.fecha, f.id, f.username
        order by v.fecha asc, f.username asc
        """)
List<VentanillaEmployeeDailyCount> countEmployeeDailyProductivity(
        @Param("fechaInicio") LocalDate fechaInicio,
        @Param("fechaFin") LocalDate fechaFin
);

    @Query("""
        SELECT new com.appsisben.backend.modules.reports.dto.VentanillaEmployeeDetailedPerformanceRow(
            funcionario.id,
            funcionario.username,
            COUNT(v.id),
            SUM(CASE WHEN estado.codigo = 'PENDIENTE' THEN 1 ELSE 0 END),
            SUM(CASE WHEN estado.codigo = 'REALIZADA' THEN 1 ELSE 0 END),
            SUM(CASE WHEN estado.codigo = 'APROBADA' THEN 1 ELSE 0 END),
            SUM(CASE WHEN estado.codigo = 'RECHAZADA' THEN 1 ELSE 0 END),
            SUM(CASE WHEN estado.codigo = 'CANCELADA' THEN 1 ELSE 0 END),
            SUM(CASE WHEN estado.codigo = 'REVISAR' THEN 1 ELSE 0 END),
            SUM(CASE WHEN v.extranjero = false THEN 1 ELSE 0 END),
            SUM(CASE WHEN v.extranjero = true THEN 1 ELSE 0 END)
        )
        FROM VentanillaRegistro v
        LEFT JOIN v.funcionario funcionario
        LEFT JOIN v.estadoSolicitud estado
        WHERE (:fechaInicio IS NULL OR v.fecha >= :fechaInicio)
          AND (:fechaFin IS NULL OR v.fecha <= :fechaFin)
        GROUP BY funcionario.id, funcionario.username
        ORDER BY COUNT(v.id) DESC, funcionario.username ASC
        """)
    List<VentanillaEmployeeDetailedPerformanceRow> countEmployeeDetailedPerformance(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
        SELECT new com.appsisben.backend.modules.reports.dto.VentanillaEmployeeDailyDetailResponse(
            v.fecha,
            funcionario.id,
            funcionario.username,
            COUNT(v.id)
        )
        FROM VentanillaRegistro v
        LEFT JOIN v.funcionario funcionario
        WHERE (:fechaInicio IS NULL OR v.fecha >= :fechaInicio)
          AND (:fechaFin IS NULL OR v.fecha <= :fechaFin)
        GROUP BY v.fecha, funcionario.id, funcionario.username
        ORDER BY v.fecha ASC, COUNT(v.id) DESC, funcionario.username ASC
        """)
    List<VentanillaEmployeeDailyDetailResponse> countEmployeeDetailedPerformanceDaily(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

}