package com.appsisben.backend.modules.catalogs.repository;
import com.appsisben.backend.modules.catalogs.domain.Encuestador;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EncuestadorRepository extends JpaRepository<Encuestador, Long> {
    Optional<Encuestador> findFirstByUsuario_Id(Long usuarioId);

    Optional<Encuestador> findFirstByDocumento(String documento);
}
