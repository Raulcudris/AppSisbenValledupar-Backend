package com.appsisben.backend.modules.dmc.repository;

import com.appsisben.backend.modules.dmc.domain.DmcRegistro;
import com.appsisben.backend.modules.dmc.dto.DmcFilterRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class DmcSpecification {

    private DmcSpecification() {
    }

    public static Specification<DmcRegistro> activeOnly() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isTrue(root.get("activo"));
    }

    public static Specification<DmcRegistro> byFilter(DmcFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            boolean incluirInactivos = filter != null
                    && Boolean.TRUE.equals(filter.incluirInactivos());

            if (!incluirInactivos) {
                predicates.add(criteriaBuilder.isTrue(root.get("activo")));
            } else if (filter.activo() != null) {
                predicates.add(criteriaBuilder.equal(root.get("activo"), filter.activo()));
            }

            if (filter == null) {
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            }

            if (filter.fechaInicio() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fecha"), filter.fechaInicio()));
            }

            if (filter.fechaFin() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fecha"), filter.fechaFin()));
            }

            if (filter.funcionarioId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("funcionario").get("id"), filter.funcionarioId()));
            }

            if (filter.tipoDmcId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("tipoDmc").get("id"), filter.tipoDmcId()));
            }

            if (filter.encuestadorId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("encuestador").get("id"), filter.encuestadorId()));
            }

            if (filter.barrioId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("barrio").get("id"), filter.barrioId()));
            }

            if (filter.comunaId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("barrio").get("comuna").get("id"), filter.comunaId()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}