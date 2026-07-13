package com.appsisben.backend.modules.catalogs.repository;

import com.appsisben.backend.modules.catalogs.domain.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoSolicitudRepository extends JpaRepository<EstadoSolicitud, Long> {
}
