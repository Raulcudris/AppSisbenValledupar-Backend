package com.appsisben.backend.modules.callcenter.repository;

import com.appsisben.backend.modules.callcenter.domain.CallCenterResultadoLlamada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para consultar el catálogo de resultados de llamada
 * del módulo Call Center.
 */
public interface CallCenterResultadoLlamadaRepository extends JpaRepository<CallCenterResultadoLlamada, Long> {

    /**
     * Busca resultados activos ordenados alfabéticamente.
     *
     * @return lista de resultados activos.
     */
    List<CallCenterResultadoLlamada> findByActivoTrueOrderByNombreAsc();

    /**
     * Busca un resultado activo por código técnico.
     *
     * @param codigo código técnico del resultado.
     * @return resultado encontrado, si existe.
     */
    Optional<CallCenterResultadoLlamada> findFirstByCodigoIgnoreCaseAndActivoTrue(String codigo);
}
