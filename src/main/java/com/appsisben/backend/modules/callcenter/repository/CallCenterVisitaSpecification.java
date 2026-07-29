package com.appsisben.backend.modules.callcenter.repository;

import com.appsisben.backend.modules.callcenter.domain.CallCenterRegistro;
import com.appsisben.backend.modules.callcenter.domain.CallCenterVisita;
import com.appsisben.backend.modules.callcenter.dto.CallCenterVisitaFilterRequest;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/**
 * Especificaciones dinámicas para consultar visitas del módulo Call Center.
 *
 * <p>Centraliza los filtros usados por la pantalla de Mis asignaciones,
 * permitiendo buscar por datos de la visita, datos del ciudadano y estado
 * del caso maestro.</p>
 */
public final class CallCenterVisitaSpecification {

    private CallCenterVisitaSpecification() {
    }

    /**
     * Filtra únicamente visitas activas.
     *
     * @return specification de visitas activas.
     */
    public static Specification<CallCenterVisita> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("activo"));
    }

    /**
     * Filtra visitas por encuestador asignado.
     *
     * @param encuestadorId identificador del encuestador.
     * @return specification por encuestador.
     */
    public static Specification<CallCenterVisita> byEncuestador(Long encuestadorId) {
        return (root, query, cb) -> cb.equal(root.get("encuestador").get("id"), encuestadorId);
    }

    /**
     * Aplica filtros dinámicos de búsqueda.
     *
     * @param filter filtros recibidos.
     * @return specification dinámica.
     */
    public static Specification<CallCenterVisita> byFilter(CallCenterVisitaFilterRequest filter) {
        return (root, query, cb) -> {
            Join<CallCenterVisita, CallCenterRegistro> registro = root.join("callCenterRegistro", JoinType.LEFT);

            var predicate = cb.conjunction();

            if (filter == null) {
                return predicate;
            }

            String estadoVisita = normalize(filter.estadoVisita());
            String estadoCaso = normalize(filter.estadoCaso());
            String condicion = normalize(filter.condicion());
            String q = normalizeLike(filter.q());

            if (hasText(estadoVisita) && !"TODOS".equals(estadoVisita)) {
                predicate = cb.and(predicate, cb.equal(cb.upper(root.get("estadoVisita")), estadoVisita));
            }

            if (hasText(estadoCaso) && !"TODOS".equals(estadoCaso)) {
                predicate = cb.and(predicate, cb.equal(cb.upper(registro.get("estadoCaso")), estadoCaso));
            }

            if (filter.fechaDesde() != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("fechaProgramada"), filter.fechaDesde()));
            }

            if (filter.fechaHasta() != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("fechaProgramada"), filter.fechaHasta()));
            }

            if ("ABIERTAS".equals(condicion)) {
                predicate = cb.and(
                        predicate,
                        cb.not(root.get("estadoVisita").in("REALIZADA", "CANCELADA")),
                        cb.or(
                                cb.isNull(root.get("encuestaRealizada")),
                                cb.isFalse(root.get("encuestaRealizada"))
                        ),
                        cb.or(
                                cb.isNull(registro.get("estadoCaso")),
                                cb.not(registro.get("estadoCaso").in("CERRADO", "CANCELADO"))
                        )
                );
            }

            if ("FINALIZADAS".equals(condicion)) {
                predicate = cb.and(
                        predicate,
                        cb.or(
                                cb.equal(cb.upper(root.get("estadoVisita")), "REALIZADA"),
                                cb.equal(cb.upper(root.get("estadoVisita")), "CANCELADA"),
                                cb.isTrue(root.get("encuestaRealizada")),
                                cb.equal(cb.upper(registro.get("estadoCaso")), "CERRADO"),
                                cb.equal(cb.upper(registro.get("estadoCaso")), "CANCELADO")
                        )
                );
            }

            if ("CERRADO".equals(condicion)) {
                predicate = cb.and(predicate, cb.equal(cb.upper(registro.get("estadoCaso")), "CERRADO"));
            }

            if ("CANCELADO".equals(condicion)) {
                predicate = cb.and(predicate, cb.equal(cb.upper(registro.get("estadoCaso")), "CANCELADO"));
            }

            if (hasText(q)) {
                predicate = cb.and(
                        predicate,
                        cb.or(
                                cb.like(cb.lower(registro.get("nombreCompleto")), q),
                                cb.like(cb.lower(registro.get("cedulaSolicitante")), q),
                                cb.like(cb.lower(registro.get("telefono")), q),
                                cb.like(cb.lower(registro.get("direccionTexto")), q),
                                cb.like(cb.lower(registro.get("tipoSolicitudCallcenter")), q),
                                cb.like(cb.lower(registro.get("estadoCaso")), q),
                                cb.like(cb.lower(root.get("estadoVisita")), q)
                        )
                );
            }

            return predicate;
        };
    }

    /**
     * Normaliza códigos técnicos a mayúscula.
     *
     * @param value valor recibido.
     * @return valor normalizado.
     */
    private static String normalize(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    /**
     * Normaliza texto para búsquedas LIKE.
     *
     * @param value valor recibido.
     * @return patrón LIKE.
     */
    private static String normalizeLike(String value) {
        return hasText(value) ? "%" + value.trim().toLowerCase(Locale.ROOT) + "%" : null;
    }

    /**
     * Valida si un texto contiene caracteres útiles.
     *
     * @param value valor recibido.
     * @return true si tiene texto.
     */
    private static boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }
}