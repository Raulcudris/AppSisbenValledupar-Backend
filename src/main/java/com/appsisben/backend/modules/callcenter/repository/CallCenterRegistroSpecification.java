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

    public static Specification<CallCenterRegistro> byEncuestadorAsignadoOrProgramado(Long encuestadorId) {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("activo")),
                cb.or(
                        cb.equal(root.get("encuestadorAsignado").get("id"), encuestadorId),
                        cb.equal(root.get("encuestadorProgramado").get("id"), encuestadorId)
                )
        );
    }

    public static Specification<CallCenterRegistro> byFilter(CallCenterFilterRequest filter) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("funcionario", JoinType.LEFT);
                root.fetch("motivoNoContacto", JoinType.LEFT);
                root.fetch("encuestadorProgramado", JoinType.LEFT);
                root.fetch("barrio", JoinType.LEFT).fetch("comuna", JoinType.LEFT);
                root.fetch("motivoNoDisposicion", JoinType.LEFT);
                root.fetch("encuestadorAsignado", JoinType.LEFT);
                root.fetch("ventanillaRegistro", JoinType.LEFT);
                root.fetch("funcionarioCallcenterAsignado", JoinType.LEFT);
                root.fetch("usuarioAsignaCallcenter", JoinType.LEFT);
                query.distinct(true);
            }

            if (filter == null) {
                return cb.and(predicate, cb.isTrue(root.get("activo")));
            }
            if (filter.funcionarioCallcenterAsignadoId() != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(root.get("funcionarioCallcenterAsignado").get("id"), filter.funcionarioCallcenterAsignadoId())
                );
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
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("cedulaSolicitante")), like(filter.cedulaSolicitante())));
            }

            if (hasText(filter.nombreCompleto())) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("nombreCompleto")), like(filter.nombreCompleto())));
            }

            if (hasText(filter.telefono())) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("telefono")), like(filter.telefono())));
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

            if (hasText(filter.tipoRegistro())) {
                predicate = cb.and(predicate, cb.equal(
                        cb.upper(root.get("tipoRegistro")),
                        filter.tipoRegistro().trim().toUpperCase(Locale.ROOT)
                ));
            }

            if (hasText(filter.origenRegistro())) {
                predicate = cb.and(predicate, cb.equal(
                        cb.upper(root.get("origenRegistro")),
                        filter.origenRegistro().trim().toUpperCase(Locale.ROOT)
                ));
            }

            if (filter.ventanillaRegistroId() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("ventanillaRegistro").get("id"), filter.ventanillaRegistroId()));
            }

            if (filter.verificado() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("verificado"), filter.verificado()));
            }

            if (hasText(filter.estadoVisita())) {
                predicate = cb.and(predicate, cb.equal(
                        cb.upper(root.get("estadoVisita")),
                        filter.estadoVisita().trim().toUpperCase(Locale.ROOT)
                ));
            }

            if (filter.encuestaRealizada() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("encuestaRealizada"), filter.encuestaRealizada()));
            }

            if (hasText(filter.q())) {
                String value = like(filter.q());

                var qPredicate = cb.or(
                        cb.like(cb.lower(root.get("cedulaSolicitante")), value),
                        cb.like(cb.lower(root.get("nombreCompleto")), value),
                        cb.like(cb.lower(root.get("telefono")), value),
                        cb.like(cb.lower(root.get("direccionTexto")), value),
                        cb.like(cb.lower(root.get("motivoNoContactoTexto")), value),
                        cb.like(cb.lower(root.get("motivoNoDisposicionTexto")), value),
                        cb.like(cb.lower(root.get("observacionEncuestador")), value)
                );

                predicate = cb.and(predicate, qPredicate);
            }

            return predicate;
        };
    }

    public static Specification<CallCenterRegistro> byFuncionarioCallcenterAsignado(Long userId) {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("activo")),
                cb.equal(root.get("funcionarioCallcenterAsignado").get("id"), userId)
        );
    }

    public static Specification<CallCenterRegistro> pendientesAsignarFuncionarioCallcenter() {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("activo")),
                cb.isTrue(root.get("solicitoNuevaEncuesta")),
                cb.or(
                        cb.isFalse(root.get("encuestaRealizada")),
                        cb.isNull(root.get("encuestaRealizada"))
                ),
                cb.isNull(root.get("funcionarioCallcenterAsignado"))
        );
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }

    private static String like(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
