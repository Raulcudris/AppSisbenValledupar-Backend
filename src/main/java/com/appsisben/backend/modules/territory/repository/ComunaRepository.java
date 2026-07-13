package com.appsisben.backend.modules.territory.repository;

import com.appsisben.backend.modules.territory.domain.Comuna;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComunaRepository extends JpaRepository<Comuna, Long> {
}
