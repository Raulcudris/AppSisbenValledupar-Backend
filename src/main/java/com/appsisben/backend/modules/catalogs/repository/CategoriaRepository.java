package com.appsisben.backend.modules.catalogs.repository;

import com.appsisben.backend.modules.catalogs.domain.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}
