package com.appsisben.backend.modules.catalogos.repository;

import com.appsisben.backend.modules.catalogos.domain.Comuna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComunaRepository extends JpaRepository<Comuna, Long> {

    @Query("""
            select c
            from Comuna c
            where (:activo is null or c.activo = :activo)
              and (
                    :q is null
                    or lower(c.codigo) like lower(concat('%', :q, '%'))
                    or lower(c.nombre) like lower(concat('%', :q, '%'))
              )
            order by c.codigo asc, c.nombre asc
            """)
    List<Comuna> search(
            @Param("q") String q,
            @Param("activo") Boolean activo
    );
}
