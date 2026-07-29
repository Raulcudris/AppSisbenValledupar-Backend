package com.appsisben.backend.modules.callcenter.repository;
import com.appsisben.backend.modules.callcenter.domain.CallCenterVisita;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para las visitas de encuestadores asociadas al flujo
 * del módulo Call Center.
 */
public interface CallCenterVisitaRepository
        extends JpaRepository<CallCenterVisita, Long>, JpaSpecificationExecutor<CallCenterVisita> {

    /**
     * Lista las visitas activas de un caso maestro.
     *
     * @param callCenterRegistroId identificador del caso maestro.
     * @return visitas activas del caso.
     */
    List<CallCenterVisita> findByCallCenterRegistroIdAndActivoTrueOrderByFechaAsignacionDescIdDesc(
            Long callCenterRegistroId
    );

    /**
     * Busca la última visita activa registrada para un caso.
     *
     * @param callCenterRegistroId identificador del caso maestro.
     * @return última visita activa, si existe.
     */
    Optional<CallCenterVisita> findFirstByCallCenterRegistroIdAndActivoTrueOrderByIdDesc(
            Long callCenterRegistroId
    );

    /**
     * Lista las visitas activas de un encuestador.
     *
     * @param encuestadorId identificador del encuestador.
     * @param pageable configuración de paginación.
     * @return página de visitas asignadas al encuestador.
     */
    Page<CallCenterVisita> findByEncuestadorIdAndActivoTrue(Long encuestadorId, Pageable pageable);

    /**
     * Lista todas las visitas activas del módulo.
     *
     * @param pageable configuración de paginación.
     * @return página de visitas activas.
     */
    Page<CallCenterVisita> findByActivoTrue(Pageable pageable);
}