package com.appsisben.backend.modules.territory.repository;

import com.appsisben.backend.modules.territory.domain.Barrio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BarrioRepository extends JpaRepository<Barrio, Long> {

    @Query(
            value = """
                    select b
                    from Barrio b
                    join b.comuna c
                    where (:q is null
                           or lower(b.nombre) like lower(concat('%', :q, '%'))
                           or lower(c.nombre) like lower(concat('%', :q, '%'))
                           or lower(c.codigo) like lower(concat('%', :q, '%')))
                      and (:comunaId is null or c.id = :comunaId)
                      and (:activo is null or b.activo = :activo)
                    """,
            countQuery = """
                    select count(b)
                    from Barrio b
                    join b.comuna c
                    where (:q is null
                           or lower(b.nombre) like lower(concat('%', :q, '%'))
                           or lower(c.nombre) like lower(concat('%', :q, '%'))
                           or lower(c.codigo) like lower(concat('%', :q, '%')))
                      and (:comunaId is null or c.id = :comunaId)
                      and (:activo is null or b.activo = :activo)
                    """
    )
    Page<Barrio> search(
            @Param("q") String q,
            @Param("comunaId") Long comunaId,
            @Param("activo") Boolean activo,
            Pageable pageable
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
