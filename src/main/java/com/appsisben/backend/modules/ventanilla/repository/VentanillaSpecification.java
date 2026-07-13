package com.appsisben.backend.modules.ventanilla.repository;

import com.appsisben.backend.modules.ventanilla.domain.VentanillaRegistro;
import com.appsisben.backend.modules.ventanilla.dto.VentanillaFilterRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class VentanillaSpecification {

    private VentanillaSpecification() {
    }

    public static Specification<VentanillaRegistro> activeOnly() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isTrue(root.get("activo"));
    }

    public static Specification<VentanillaRegistro> byFilter(
            VentanillaFilterRequest filter,
            boolean allowInactiveRecords
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!allowInactiveRecords) {
                predicates.add(criteriaBuilder.isTrue(root.get("activo")));
            } else if (filter != null && filter.activo() != null) {
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

            if (hasText(filter.numeroVentanilla())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.upper(root.get("numeroVentanilla")),
                        like(filter.numeroVentanilla())
                ));
            }

            if (hasText(filter.cedulaUsuario())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.upper(root.get("cedulaUsuario")),
                        like(filter.cedulaUsuario())
                ));
            }

            if (hasText(filter.nombreUsuario())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.upper(root.get("nombreUsuario")),
                        like(filter.nombreUsuario())
                ));
            }

            if (filter.funcionarioId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("funcionario").get("id"), filter.funcionarioId()));
            }

            if (filter.categoriaId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("categoria").get("id"), filter.categoriaId()));
            }

            if (filter.solicitudId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("solicitud").get("id"), filter.solicitudId()));
            }

            if (filter.estadoSolicitudId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("estadoSolicitud").get("id"), filter.estadoSolicitudId()));
            }

            if (filter.barrioId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("barrio").get("id"), filter.barrioId()));
            }

            if (filter.comunaId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("barrio").get("comuna").get("id"), filter.comunaId()));
            }

            if (filter.extranjero() != null) {
                predicates.add(criteriaBuilder.equal(root.get("extranjero"), filter.extranjero()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String like(String value) {
        return "%" + value.trim().toUpperCase() + "%";
    }
}