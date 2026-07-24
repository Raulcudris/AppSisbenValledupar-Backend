package com.appsisben.backend.modules.callcenter.repository;

import com.appsisben.backend.modules.callcenter.domain.CallCenterRegistro;
import com.appsisben.backend.modules.callcenter.dto.CallCenterFilterRequest;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class CallCenterRegistroSpecification {

    private CallCenterRegistroSpecification() {
    }

    public static Specification<CallCenterRegistro> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("activo"));
    }

    public static Specification<CallCenterRegistro> byFilter(CallCenterFilterRequest filter) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            root.fetch("funcionario", JoinType.LEFT);
            root.fetch("motivoNoContacto", JoinType.LEFT);
            root.fetch("encuestadorProgramado", JoinType.LEFT);
            root.fetch("barrio", JoinType.LEFT).fetch("comuna", JoinType.LEFT);
            root.fetch("motivoNoDisposicion", JoinType.LEFT);
            root.fetch("encuestadorAsignado", JoinType.LEFT);

            if (filter == null) {
                return predicate;
            }

            if (filter.activo() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("activo"), filter.activo()));
            } else {
                predicate = cb.and(predicate, cb.isTrue(root.get("activo")));
            }

            if (filter.fechaInicio() != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("fechaLlamada"), filter.fechaInicio()));
            }

            if (filter.fechaFin() != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("fechaLlamada"), filter.fechaFin()));
            }

            if (filter.funcionarioId() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("funcionario").get("id"), filter.funcionarioId()));
            }

            if (hasText(filter.cedulaSolicitante())) {
                predicate = cb.and(predicate, cb.like(
                        cb.lower(root.get("cedulaSolicitante")),
                        like(filter.cedulaSolicitante())
                ));
            }

            if (hasText(filter.nombreCompleto())) {
                predicate = cb.and(predicate, cb.like(
                        cb.lower(root.get("nombreCompleto")),
                        like(filter.nombreCompleto())
                ));
            }

            if (hasText(filter.telefono())) {
                predicate = cb.and(predicate, cb.like(
                        cb.lower(root.get("telefono")),
                        like(filter.telefono())
                ));
            }

            if (filter.llamadaConectada() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("llamadaConectada"), filter.llamadaConectada()));
            }

            if (filter.motivoNoContactoId() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("motivoNoContacto").get("id"), filter.motivoNoContactoId()));
            }

            if (filter.encuestadorProgramadoId() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("encuestadorProgramado").get("id"), filter.encuestadorProgramadoId()));
            }

            if (filter.encuestadorAsignadoId() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("encuestadorAsignado").get("id"), filter.encuestadorAsignadoId()));
            }

            if (filter.fechaEncuestaInicio() != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("fechaEncuestaProgramada"), filter.fechaEncuestaInicio()));
            }

            if (filter.fechaEncuestaFin() != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("fechaEncuestaProgramada"), filter.fechaEncuestaFin()));
            }

            if (filter.solicitoNuevaEncuesta() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("solicitoNuevaEncuesta"), filter.solicitoNuevaEncuesta()));
            }

            if (filter.barrioId() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("barrio").get("id"), filter.barrioId()));
            }

            if (filter.comunaId() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("barrio").get("comuna").get("id"), filter.comunaId()));
            }

            if (filter.disposicionRecibirEncuesta() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("disposicionRecibirEncuesta"), filter.disposicionRecibirEncuesta()));
            }

            if (filter.explicoInformanteCalificado() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("explicoInformanteCalificado"), filter.explicoInformanteCalificado()));
            }

            if (hasText(filter.q())) {
                String value = like(filter.q());

                var qPredicate = cb.or(
                        cb.like(cb.lower(root.get("cedulaSolicitante")), value),
                        cb.like(cb.lower(root.get("nombreCompleto")), value),
                        cb.like(cb.lower(root.get("telefono")), value),
                        cb.like(cb.lower(root.get("direccionTexto")), value),
                        cb.like(cb.lower(root.get("motivoNoContactoTexto")), value),
                        cb.like(cb.lower(root.get("motivoNoDisposicionTexto")), value)
                );

                predicate = cb.and(predicate, qPredicate);
            }

            return predicate;
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }

    private static String like(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
