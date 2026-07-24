package com.appsisben.backend.modules.catalogs.repository;
import com.appsisben.backend.modules.catalogs.domain.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
}
