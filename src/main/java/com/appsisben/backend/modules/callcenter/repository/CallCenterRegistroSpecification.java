package com.appsisben.backend.modules.callcenter.repository;

import com.appsisben.backend.modules.callcenter.domain.CallCenterRegistro;
import com.appsisben.backend.modules.callcenter.dto.CallCenterFilterRequest;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/**
 * Especificaciones dinámicas para consultar registros Call Center.
 *
 * Esta clase centraliza los filtros usados por el módulo Call Center para
 * consultas generales, consultas por rol, pendientes de enrutamiento y
 * asignaciones de encuestador o funcionario Call Center.
 */
public final class CallCenterRegistroSpecification {

    private CallCenterRegistroSpecification() {
    }

    /**
     * Filtra únicamente registros activos.
     *
     * @return especificación de registros activos.
     */
    public static Specification<CallCenterRegistro> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("activo"));
    }

    /**
     * Consulta registros asignados o programados para un encuestador.
     *
     * @param encuestadorId identificador del encuestador.
     * @return especificación por encuestador.
     */
    public static Specification<CallCenterRegistro> byEncuestadorAsignadoOrProgramado(Long encuestadorId) {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("activo")),
                cb.or(
                        cb.equal(root.get("encuestadorAsignado").get("id"), encuestadorId),
                        cb.equal(root.get("encuestadorProgramado").get("id"), encuestadorId)
                )
        );
    }

    /**
     * Construye filtros dinámicos para la búsqueda general de casos Call Center.
     *
     * Incluye filtros legacy, asignación de funcionario, origen, estado de visita,
     * estado formal del caso y tipo de solicitud Call Center.
     *
     * @param filter filtros recibidos desde frontend.
     * @return especificación dinámica.
     */
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
                root.fetch("usuarioCierre", JoinType.LEFT);
                query.distinct(true);
            }

            if (filter == null) {
                return cb.and(predicate, cb.isTrue(root.get("activo")));
            }

            if (filter.funcionarioCallcenterAsignadoId() != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(
                                root.get("funcionarioCallcenterAsignado").get("id"),
                                filter.funcionarioCallcenterAsignadoId()
                        )
                );
            }

            if (filter.activo() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("activo"), filter.activo()));
            } else {
                predicate = cb.and(predicate, cb.isTrue(root.get("activo")));
            }

            if (filter.fechaInicio() != null) {
                predicate = cb.and(
                        predicate,
                        cb.greaterThanOrEqualTo(root.get("fechaLlamada"), filter.fechaInicio())
                );
            }

            if (filter.fechaFin() != null) {
                predicate = cb.and(
                        predicate,
                        cb.lessThanOrEqualTo(root.get("fechaLlamada"), filter.fechaFin())
                );
            }

            if (filter.funcionarioId() != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(root.get("funcionario").get("id"), filter.funcionarioId())
                );
            }

            if (hasText(filter.cedulaSolicitante())) {
                predicate = cb.and(
                        predicate,
                        cb.like(cb.lower(root.get("cedulaSolicitante")), like(filter.cedulaSolicitante()))
                );
            }

            if (hasText(filter.nombreCompleto())) {
                predicate = cb.and(
                        predicate,
                        cb.like(cb.lower(root.get("nombreCompleto")), like(filter.nombreCompleto()))
                );
            }

            if (hasText(filter.telefono())) {
                predicate = cb.and(
                        predicate,
                        cb.like(cb.lower(root.get("telefono")), like(filter.telefono()))
                );
            }

            if (filter.llamadaConectada() != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(root.get("llamadaConectada"), filter.llamadaConectada())
                );
            }

            if (filter.motivoNoContactoId() != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(root.get("motivoNoContacto").get("id"), filter.motivoNoContactoId())
                );
            }

            if (filter.encuestadorProgramadoId() != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(root.get("encuestadorProgramado").get("id"), filter.encuestadorProgramadoId())
                );
            }

            if (filter.encuestadorAsignadoId() != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(root.get("encuestadorAsignado").get("id"), filter.encuestadorAsignadoId())
                );
            }

            if (filter.fechaEncuestaInicio() != null) {
                predicate = cb.and(
                        predicate,
                        cb.greaterThanOrEqualTo(root.get("fechaEncuestaProgramada"), filter.fechaEncuestaInicio())
                );
            }

            if (filter.fechaEncuestaFin() != null) {
                predicate = cb.and(
                        predicate,
                        cb.lessThanOrEqualTo(root.get("fechaEncuestaProgramada"), filter.fechaEncuestaFin())
                );
            }

            if (filter.solicitoNuevaEncuesta() != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(root.get("solicitoNuevaEncuesta"), filter.solicitoNuevaEncuesta())
                );
            }

            if (filter.barrioId() != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(root.get("barrio").get("id"), filter.barrioId())
                );
            }

            if (filter.comunaId() != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(root.get("barrio").get("comuna").get("id"), filter.comunaId())
                );
            }

            if (filter.disposicionRecibirEncuesta() != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(root.get("disposicionRecibirEncuesta"), filter.disposicionRecibirEncuesta())
                );
            }

            if (filter.explicoInformanteCalificado() != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(root.get("explicoInformanteCalificado"), filter.explicoInformanteCalificado())
                );
            }

            if (hasText(filter.tipoRegistro())) {
                predicate = cb.and(
                        predicate,
                        cb.equal(
                                cb.upper(root.get("tipoRegistro")),
                                filter.tipoRegistro().trim().toUpperCase(Locale.ROOT)
                        )
                );
            }

            if (hasText(filter.origenRegistro())) {
                predicate = cb.and(
                        predicate,
                        cb.equal(
                                cb.upper(root.get("origenRegistro")),
                                filter.origenRegistro().trim().toUpperCase(Locale.ROOT)
                        )
                );
            }

            if (filter.ventanillaRegistroId() != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(root.get("ventanillaRegistro").get("id"), filter.ventanillaRegistroId())
                );
            }

            if (filter.verificado() != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(root.get("verificado"), filter.verificado())
                );
            }

            if (hasText(filter.estadoVisita())) {
                predicate = cb.and(
                        predicate,
                        cb.equal(
                                cb.upper(root.get("estadoVisita")),
                                filter.estadoVisita().trim().toUpperCase(Locale.ROOT)
                        )
                );
            }

            if (filter.encuestaRealizada() != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(root.get("encuestaRealizada"), filter.encuestaRealizada())
                );
            }

            if (hasText(filter.estadoCaso())) {
                predicate = cb.and(
                        predicate,
                        cb.equal(
                                cb.upper(root.get("estadoCaso")),
                                filter.estadoCaso().trim().toUpperCase(Locale.ROOT)
                        )
                );
            }

            if (hasText(filter.tipoSolicitudCallcenter())) {
                predicate = cb.and(
                        predicate,
                        cb.equal(
                                cb.upper(root.get("tipoSolicitudCallcenter")),
                                filter.tipoSolicitudCallcenter().trim().toUpperCase(Locale.ROOT)
                        )
                );
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
                        cb.like(cb.lower(root.get("observacionEncuestador")), value),
                        cb.like(cb.lower(root.get("observacion")), value),
                        cb.like(cb.lower(root.get("estadoCaso")), value),
                        cb.like(cb.lower(root.get("tipoSolicitudCallcenter")), value)
                );

                predicate = cb.and(predicate, qPredicate);
            }

            return predicate;
        };
    }

    /**
     * Consulta los registros asignados a un funcionario Call Center.
     *
     * @param userId identificador del usuario funcionario.
     * @return especificación por funcionario asignado.
     */
    public static Specification<CallCenterRegistro> byFuncionarioCallcenterAsignado(Long userId) {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("activo")),
                cb.equal(root.get("funcionarioCallcenterAsignado").get("id"), userId)
        );
    }

    /**
     * Consulta casos pendientes de asignar a funcionario Call Center.
     *
     * Esta especificación alimenta la vista del Coordinador / Enrutador.
     * Solo retorna casos activos, de nueva encuesta, no realizados, sin
     * funcionario asignado y en estado PENDIENTE_ENRUTAMIENTO.
     *
     * @return especificación de pendientes para enrutamiento.
     */
    public static Specification<CallCenterRegistro> pendientesAsignarFuncionarioCallcenter() {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("activo")),
                cb.isTrue(root.get("solicitoNuevaEncuesta")),
                cb.or(
                        cb.isFalse(root.get("encuestaRealizada")),
                        cb.isNull(root.get("encuestaRealizada"))
                ),
                cb.isNull(root.get("funcionarioCallcenterAsignado")),
                cb.or(
                        cb.equal(root.get("estadoCaso"), "PENDIENTE_ENRUTAMIENTO"),
                        cb.isNull(root.get("estadoCaso"))
                )
        );
    }

    /**
     * Valida si un texto contiene caracteres útiles.
     *
     * @param value texto a evaluar.
     * @return true si el texto tiene contenido.
     */
    private static boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }

    /**
     * Convierte un texto en patrón LIKE insensible a mayúsculas.
     *
     * @param value texto recibido.
     * @return patrón LIKE.
     */
    private static String like(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }
}