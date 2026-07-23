package com.appsisben.backend.modules.catalogos.repository;

import com.appsisben.backend.modules.catalogos.domain.Barrio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BarrioRepository extends JpaRepository<Barrio, Long> {

    @Query("""
            select b
            from Barrio b
            join fetch b.comuna c
            where (:activo is null or b.activo = :activo)
              and (:comunaId is null or c.id = :comunaId)
              and (
                    :q is null
                    or lower(b.nombre) like lower(concat('%', :q, '%'))
                    or lower(c.codigo) like lower(concat('%', :q, '%'))
                    or lower(c.nombre) like lower(concat('%', :q, '%'))
              )
            order by c.codigo asc, c.nombre asc, b.nombre asc
            """)
    List<Barrio> search(
            @Param("q") String q,
            @Param("comunaId") Long comunaId,
            @Param("activo") Boolean activo
    );

    @Query("""
            select case when count(b) > 0 then true else false end
            from Barrio b
            where lower(b.nombre) = lower(:nombre)
              and b.comuna.id = :comunaId
              and (:excludeId is null or b.id <> :excludeId)
            """)
    boolean existsDuplicatedNameInComuna(
            @Param("nombre") String nombre,
            @Param("comunaId") Long comunaId,
            @Param("excludeId") Long excludeId
    );
}
