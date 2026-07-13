package com.appsisben.backend.modules.catalogs.repository;

import com.appsisben.backend.modules.catalogs.domain.Encuestador;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EncuestadorRepository extends JpaRepository<Encuestador, Long> {
}
