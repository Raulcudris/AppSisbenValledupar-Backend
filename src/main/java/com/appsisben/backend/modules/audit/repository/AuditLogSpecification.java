package com.appsisben.backend.modules.audit.repository;
import com.appsisben.backend.modules.audit.domain.AuditLog;
import com.appsisben.backend.modules.audit.dto.AuditFilterRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class AuditLogSpecification {

    private AuditLogSpecification() {
    }

    public static Specification<AuditLog> byFilter(AuditFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.usuarioId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("usuario").get("id"), filter.usuarioId()));
            }

            if (hasText(filter.username())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.upper(root.get("usuario").get("username")),
                        like(filter.username())
                ));
            }

            if (hasText(filter.tablaAfectada())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.upper(root.get("tablaAfectada")),
                        like(filter.tablaAfectada())
                ));
            }

            if (filter.registroId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("registroId"), filter.registroId()));
            }

            if (hasText(filter.accion())) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.upper(root.get("accion")),
                        filter.accion().trim().toUpperCase()
                ));
            }

            if (filter.fechaInicio() != null) {
                LocalDateTime start = filter.fechaInicio().atStartOfDay();
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaAccion"), start));
            }

            if (filter.fechaFin() != null) {
                LocalDateTime end = filter.fechaFin().plusDays(1).atStartOfDay().minusNanos(1);
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fechaAccion"), end));
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
