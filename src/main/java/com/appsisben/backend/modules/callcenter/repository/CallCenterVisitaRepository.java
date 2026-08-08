package com.appsisben.backend.modules.callcenter.repository;

import com.appsisben.backend.modules.callcenter.domain.CallCenterVisita;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para las visitas de encuestadores
 * asociadas al flujo del módulo Call Center.
 */
public interface CallCenterVisitaRepository
        extends JpaRepository<CallCenterVisita, Long>,
        JpaSpecificationExecutor<CallCenterVisita> {

    /**
     * Lista las visitas activas de un caso maestro.
     */
    List<CallCenterVisita>
    findByCallCenterRegistroIdAndActivoTrueOrderByFechaAsignacionDescIdDesc(
            Long callCenterRegistroId
    );

    /**
     * Busca la última visita activa registrada para un caso.
     */
    Optional<CallCenterVisita>
    findFirstByCallCenterRegistroIdAndActivoTrueOrderByIdDesc(
            Long callCenterRegistroId
    );

    /**
     * Lista las visitas activas de un encuestador.
     */
    Page<CallCenterVisita>
    findByEncuestadorIdAndActivoTrue(
            Long encuestadorId,
            Pageable pageable
    );

    /**
     * Lista todas las visitas activas del módulo.
     */
    Page<CallCenterVisita>
    findByActivoTrue(
            Pageable pageable
    );

    /**
     * Consulta las visitas que todavía deben realizarse
     * para un encuestador en una fecha específica.
     *
     * <p>Para visitas REPROGRAMADAS se utiliza
     * fechaReprogramacion.</p>
     *
     * <p>Para PENDIENTE y PROGRAMADA se utiliza
     * fechaProgramada.</p>
     *
     * <p>Se carga también el barrio asociado al caso maestro
     * para evitar consultas adicionales al construir la
     * respuesta de agenda.</p>
     */
    @Query(
            value = """
                    SELECT v
                    FROM CallCenterVisita v
                    JOIN FETCH v.callCenterRegistro r
                    JOIN FETCH v.encuestador e
                    LEFT JOIN FETCH r.barrio b
                    WHERE v.activo = true
                      AND r.activo = true
                      AND e.id = :encuestadorId
                      AND UPPER(v.estadoVisita) IN (
                          'PENDIENTE',
                          'PROGRAMADA',
                          'REPROGRAMADA'
                      )
                      AND (
                          v.encuestaRealizada IS NULL
                          OR v.encuestaRealizada = false
                      )
                      AND (
                          r.estadoCaso IS NULL
                          OR UPPER(r.estadoCaso) NOT IN (
                              'CERRADO',
                              'CANCELADO'
                          )
                      )
                      AND (
                          (
                              UPPER(v.estadoVisita) = 'REPROGRAMADA'
                              AND v.fechaReprogramacion = :fecha
                          )
                          OR
                          (
                              UPPER(v.estadoVisita) <> 'REPROGRAMADA'
                              AND v.fechaProgramada = :fecha
                          )
                      )
                    """,
            countQuery = """
                    SELECT COUNT(v)
                    FROM CallCenterVisita v
                    JOIN v.callCenterRegistro r
                    JOIN v.encuestador e
                    WHERE v.activo = true
                      AND r.activo = true
                      AND e.id = :encuestadorId
                      AND UPPER(v.estadoVisita) IN (
                          'PENDIENTE',
                          'PROGRAMADA',
                          'REPROGRAMADA'
                      )
                      AND (
                          v.encuestaRealizada IS NULL
                          OR v.encuestaRealizada = false
                      )
                      AND (
                          r.estadoCaso IS NULL
                          OR UPPER(r.estadoCaso) NOT IN (
                              'CERRADO',
                              'CANCELADO'
                          )
                      )
                      AND (
                          (
                              UPPER(v.estadoVisita) = 'REPROGRAMADA'
                              AND v.fechaReprogramacion = :fecha
                          )
                          OR
                          (
                              UPPER(v.estadoVisita) <> 'REPROGRAMADA'
                              AND v.fechaProgramada = :fecha
                          )
                      )
                    """
    )
    Page<CallCenterVisita>
    findAgendaByEncuestadorAndFecha(
            @Param("encuestadorId")
            Long encuestadorId,

            @Param("fecha")
            LocalDate fecha,

            Pageable pageable
    );
}