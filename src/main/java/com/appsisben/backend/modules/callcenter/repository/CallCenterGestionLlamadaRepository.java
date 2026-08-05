package com.appsisben.backend.modules.callcenter.repository;

import com.appsisben.backend.modules.callcenter.domain.CallCenterGestionLlamada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para las gestiones o intentos de llamada asociados
 * a un caso maestro de Call Center.
 */
public interface CallCenterGestionLlamadaRepository
        extends JpaRepository<CallCenterGestionLlamada, Long> {

    /**
     * Lista las gestiones activas de un caso, ordenadas por número de intento.
     *
     * @param callCenterRegistroId identificador del caso maestro.
     * @return gestiones activas del caso.
     */
    List<CallCenterGestionLlamada>
    findByCallCenterRegistroIdAndActivoTrueOrderByIntentoNumeroAscIdAsc(
            Long callCenterRegistroId
    );

    /**
     * Obtiene la última gestión activa registrada para un caso.
     *
     * @param callCenterRegistroId identificador del caso maestro.
     * @return última gestión activa, cuando exista.
     */
    Optional<CallCenterGestionLlamada>
    findFirstByCallCenterRegistroIdAndActivoTrueOrderByIntentoNumeroDescIdDesc(
            Long callCenterRegistroId
    );

    /**
     * Cuenta la cantidad de gestiones registradas para un caso.
     *
     * @param callCenterRegistroId identificador del caso maestro.
     * @return total de gestiones asociadas al caso.
     */
    long countByCallCenterRegistroId(
            Long callCenterRegistroId
    );
}