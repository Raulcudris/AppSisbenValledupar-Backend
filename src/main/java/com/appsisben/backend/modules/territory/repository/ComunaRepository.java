package com.appsisben.backend.modules.territory.repository;

import com.appsisben.backend.modules.territory.domain.Comuna;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComunaRepository extends JpaRepository<Comuna, Long> {

    @Query(
            value = """
                    select c
                    from Comuna c
                    where (:q is null
                           or lower(c.codigo) like lower(concat('%', :q, '%'))
                           or lower(c.nombre) like lower(concat('%', :q, '%'))
                           or lower(c.descripcion) like lower(concat('%', :q, '%')))
                      and (:activo is null or c.activo = :activo)
                    """,
            countQuery = """
                    select count(c)
                    from Comuna c
                    where (:q is null
                           or lower(c.codigo) like lower(concat('%', :q, '%'))
                           or lower(c.nombre) like lower(concat('%', :q, '%'))
                           or lower(c.descripcion) like lower(concat('%', :q, '%')))
                      and (:activo is null or c.activo = :activo)
                    """
    )
    Page<Comuna> search(
            @Param("q") String q,
            @Param("activo") Boolean activo,
            Pageable pageable
    );

    @Query("""
            select case when count(c) > 0 then true else false end
            from Comuna c
            where lower(c.nombre) = lower(:nombre)
              and (:excludeId is null or c.id <> :excludeId)
            """)
    boolean existsDuplicatedNombre(
            @Param("nombre") String nombre,
            @Param("excludeId") Long excludeId
    );

    @Query(
            value = """
                    select coalesce(max(cast(substring(codigo, 2) as unsigned)), 0)
                    from comuna
                    where codigo regexp '^C[0-9]+$'
                    """,
            nativeQuery = true
    )
    Long findMaxCodigoNumber();
}
