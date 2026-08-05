package com.appsisben.backend.modules.callcenter.application;

import com.appsisben.backend.shared.exception.BusinessException;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Política central de estados del módulo Call Center.
 *
 * <p>Centraliza las transiciones del caso maestro y mantiene
 * compatibilidad con los servicios existentes.</p>
 *
 * <p>La llamada y la visita son procesos relacionados, pero una llamada
 * no contestada no cancela, elimina ni aplaza automáticamente una visita
 * previamente asignada o programada.</p>
 *
 * <p>Los resultados REALIZADA, NO_ATENDIDA y CANCELADA finalizan
 * la gestión del caso. REPROGRAMADA conserva el caso abierto para
 * registrar el resultado de la nueva visita.</p>
 */
public final class CallCenterStatePolicy {

    public static final String PENDIENTE_ENRUTAMIENTO =
            "PENDIENTE_ENRUTAMIENTO";

    public static final String ASIGNADO_CALLCENTER =
            "ASIGNADO_CALLCENTER";

    public static final String EN_GESTION_LLAMADA =
            "EN_GESTION_LLAMADA";

    public static final String NO_CONTACTADO =
            "NO_CONTACTADO";

    public static final String CONTACTADO_SIN_DISPOSICION =
            "CONTACTADO_SIN_DISPOSICION";

    public static final String PENDIENTE_ASIGNAR_ENCUESTADOR =
            "PENDIENTE_ASIGNAR_ENCUESTADOR";

    public static final String ASIGNADO_ENCUESTADOR =
            "ASIGNADO_ENCUESTADOR";

    public static final String VISITA_PROGRAMADA =
            "VISITA_PROGRAMADA";

    public static final String VISITA_NO_ATENDIDA =
            "VISITA_NO_ATENDIDA";

    public static final String REPROGRAMADO =
            "REPROGRAMADO";

    public static final String VISITA_REALIZADA =
            "VISITA_REALIZADA";

    public static final String CERRADO =
            "CERRADO";

    public static final String CANCELADO =
            "CANCELADO";

    private static final Set<String> KNOWN_STATES =
            Set.of(
                    PENDIENTE_ENRUTAMIENTO,
                    ASIGNADO_CALLCENTER,
                    EN_GESTION_LLAMADA,
                    NO_CONTACTADO,
                    CONTACTADO_SIN_DISPOSICION,
                    PENDIENTE_ASIGNAR_ENCUESTADOR,
                    ASIGNADO_ENCUESTADOR,
                    VISITA_PROGRAMADA,
                    VISITA_NO_ATENDIDA,
                    REPROGRAMADO,
                    VISITA_REALIZADA,
                    CERRADO,
                    CANCELADO
            );

    private static final Set<String> FINAL_STATES =
            Set.of(
                    CERRADO,
                    CANCELADO
            );

    private static final Set<String> CALL_ALLOWED_STATES =
            Set.of(
                    ASIGNADO_CALLCENTER,
                    EN_GESTION_LLAMADA,
                    NO_CONTACTADO,
                    CONTACTADO_SIN_DISPOSICION,
                    PENDIENTE_ASIGNAR_ENCUESTADOR,
                    ASIGNADO_ENCUESTADOR,
                    VISITA_PROGRAMADA,
                    REPROGRAMADO
            );

    private static final Set<String> CALL_TARGET_STATES =
            Set.of(
                    EN_GESTION_LLAMADA,
                    NO_CONTACTADO,
                    CONTACTADO_SIN_DISPOSICION,
                    PENDIENTE_ASIGNAR_ENCUESTADOR,
                    CERRADO,
                    CANCELADO
            );

    private static final Set<String> TELEPHONE_STAGE_STATES =
            Set.of(
                    EN_GESTION_LLAMADA,
                    NO_CONTACTADO,
                    CONTACTADO_SIN_DISPOSICION,
                    PENDIENTE_ASIGNAR_ENCUESTADOR
            );

    private static final Set<String> ADVANCED_FIELD_STATES =
            Set.of(
                    PENDIENTE_ASIGNAR_ENCUESTADOR,
                    ASIGNADO_ENCUESTADOR,
                    VISITA_PROGRAMADA,
                    REPROGRAMADO
            );

    private static final Set<String> VISIT_ASSIGNMENT_ALLOWED_STATES =
            Set.of(
                    ASIGNADO_CALLCENTER,
                    EN_GESTION_LLAMADA,
                    NO_CONTACTADO,
                    CONTACTADO_SIN_DISPOSICION,
                    PENDIENTE_ASIGNAR_ENCUESTADOR
            );

    private static final Set<String> VISIT_UPDATE_ALLOWED_STATES =
            Set.of(
                    ASIGNADO_ENCUESTADOR,
                    VISITA_PROGRAMADA,
                    VISITA_NO_ATENDIDA,
                    REPROGRAMADO,
                    VISITA_REALIZADA
            );

    private static final Set<String> VISIT_TARGET_STATES =
            Set.of(
                    ASIGNADO_ENCUESTADOR,
                    VISITA_PROGRAMADA,
                    VISITA_NO_ATENDIDA,
                    REPROGRAMADO,
                    CERRADO,
                    CANCELADO
            );

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS =
            Map.ofEntries(
                    Map.entry(
                            PENDIENTE_ENRUTAMIENTO,
                            Set.of(
                                    ASIGNADO_CALLCENTER,
                                    CANCELADO
                            )
                    ),
                    Map.entry(
                            ASIGNADO_CALLCENTER,
                            Set.of(
                                    EN_GESTION_LLAMADA,
                                    NO_CONTACTADO,
                                    CONTACTADO_SIN_DISPOSICION,
                                    PENDIENTE_ASIGNAR_ENCUESTADOR,
                                    ASIGNADO_ENCUESTADOR,
                                    VISITA_PROGRAMADA,
                                    CERRADO,
                                    CANCELADO
                            )
                    ),
                    Map.entry(
                            EN_GESTION_LLAMADA,
                            Set.of(
                                    NO_CONTACTADO,
                                    CONTACTADO_SIN_DISPOSICION,
                                    PENDIENTE_ASIGNAR_ENCUESTADOR,
                                    ASIGNADO_ENCUESTADOR,
                                    VISITA_PROGRAMADA,
                                    CERRADO,
                                    CANCELADO
                            )
                    ),
                    Map.entry(
                            NO_CONTACTADO,
                            Set.of(
                                    EN_GESTION_LLAMADA,
                                    CONTACTADO_SIN_DISPOSICION,
                                    PENDIENTE_ASIGNAR_ENCUESTADOR,
                                    ASIGNADO_ENCUESTADOR,
                                    VISITA_PROGRAMADA,
                                    CERRADO,
                                    CANCELADO
                            )
                    ),
                    Map.entry(
                            CONTACTADO_SIN_DISPOSICION,
                            Set.of(
                                    EN_GESTION_LLAMADA,
                                    NO_CONTACTADO,
                                    PENDIENTE_ASIGNAR_ENCUESTADOR,
                                    ASIGNADO_ENCUESTADOR,
                                    VISITA_PROGRAMADA,
                                    CERRADO,
                                    CANCELADO
                            )
                    ),
                    Map.entry(
                            PENDIENTE_ASIGNAR_ENCUESTADOR,
                            Set.of(
                                    ASIGNADO_ENCUESTADOR,
                                    VISITA_PROGRAMADA,
                                    CERRADO,
                                    CANCELADO
                            )
                    ),
                    Map.entry(
                            ASIGNADO_ENCUESTADOR,
                            Set.of(
                                    VISITA_PROGRAMADA,
                                    VISITA_NO_ATENDIDA,
                                    REPROGRAMADO,
                                    VISITA_REALIZADA,
                                    CERRADO,
                                    CANCELADO
                            )
                    ),
                    Map.entry(
                            VISITA_PROGRAMADA,
                            Set.of(
                                    VISITA_NO_ATENDIDA,
                                    REPROGRAMADO,
                                    VISITA_REALIZADA,
                                    CERRADO,
                                    CANCELADO
                            )
                    ),
                    Map.entry(
                            VISITA_NO_ATENDIDA,
                            Set.of(
                                    VISITA_PROGRAMADA,
                                    REPROGRAMADO,
                                    CERRADO,
                                    CANCELADO
                            )
                    ),
                    Map.entry(
                            REPROGRAMADO,
                            Set.of(
                                    VISITA_PROGRAMADA,
                                    VISITA_NO_ATENDIDA,
                                    VISITA_REALIZADA,
                                    CERRADO,
                                    CANCELADO
                            )
                    ),
                    Map.entry(
                            VISITA_REALIZADA,
                            Set.of(
                                    CERRADO
                            )
                    ),
                    Map.entry(
                            CERRADO,
                            Set.of()
                    ),
                    Map.entry(
                            CANCELADO,
                            Set.of()
                    )
            );

    private CallCenterStatePolicy() {
        throw new IllegalStateException(
                "Clase de política no instanciable"
        );
    }

    public static void validateGeneralRequestState(
            String currentState,
            String requestedState
    ) {
        String current =
                normalizeCurrentState(currentState);

        if (
                requestedState == null
                        || requestedState.trim().isEmpty()
        ) {
            return;
        }

        String requested =
                requireKnownTargetState(requestedState);

        if (!current.equals(requested)) {
            throw new BusinessException(
                    "El estado del caso no puede modificarse "
                            + "desde la actualización general. "
                            + "Estado actual: "
                            + current
                            + ". Estado solicitado: "
                            + requested
                            + "."
            );
        }
    }

    public static String validateFuncionarioAssignment(
            String currentState
    ) {
        String current =
                normalizeCurrentState(currentState);

        if (
                !PENDIENTE_ENRUTAMIENTO.equals(current)
                        && !ASIGNADO_CALLCENTER.equals(current)
        ) {
            throw new BusinessException(
                    "El caso no puede asignarse a un funcionario "
                            + "Call Center desde el estado "
                            + current
            );
        }

        if (ASIGNADO_CALLCENTER.equals(current)) {
            return ASIGNADO_CALLCENTER;
        }

        validateTransition(
                current,
                ASIGNADO_CALLCENTER,
                "asignación de funcionario Call Center"
        );

        return ASIGNADO_CALLCENTER;
    }

    public static void validateCanRegisterCall(
            String currentState
    ) {
        String current =
                normalizeCurrentState(currentState);

        if (isFinalState(current)) {
            throw new BusinessException(
                    "No se puede registrar una llamada sobre un caso "
                            + "cerrado o cancelado"
            );
        }

        if (!CALL_ALLOWED_STATES.contains(current)) {
            throw new BusinessException(
                    "No se puede registrar una llamada cuando el caso "
                            + "se encuentra en estado "
                            + current
            );
        }
    }

    public static String validateCallTransition(
            String currentState,
            String targetState
    ) {
        String current =
                normalizeCurrentState(currentState);

        validateCanRegisterCall(current);

        String target =
                requireKnownTargetState(targetState);

        if (!CALL_TARGET_STATES.contains(target)) {
            throw new BusinessException(
                    "El estado sugerido por el resultado de llamada "
                            + "no está permitido: "
                            + target
            );
        }

        if (
                ADVANCED_FIELD_STATES.contains(current)
                        && TELEPHONE_STAGE_STATES.contains(target)
        ) {
            return current;
        }

        if (current.equals(target)) {
            return current;
        }

        validateTransition(
                current,
                target,
                "resultado de llamada"
        );

        return target;
    }

    public static String validateEncuestadorAssignment(
            String currentState,
            boolean programmed
    ) {
        String current =
                normalizeCurrentState(currentState);

        if (isFinalState(current)) {
            throw new BusinessException(
                    "No se puede asignar un encuestador a un caso "
                            + "cerrado o cancelado"
            );
        }

        if (!VISIT_ASSIGNMENT_ALLOWED_STATES.contains(current)) {
            throw new BusinessException(
                    "No se puede asignar una visita cuando el caso "
                            + "se encuentra en estado "
                            + current
            );
        }

        String target =
                programmed
                        ? VISITA_PROGRAMADA
                        : ASIGNADO_ENCUESTADOR;

        if (current.equals(target)) {
            return current;
        }

        validateTransition(
                current,
                target,
                "asignación de encuestador"
        );

        return target;
    }

    public static void validateCanUpdateVisit(
            String currentState
    ) {
        String current =
                normalizeCurrentState(currentState);

        if (isFinalState(current)) {
            throw new BusinessException(
                    "No se puede actualizar una visita de un caso "
                            + "cerrado o cancelado"
            );
        }

        if (!VISIT_UPDATE_ALLOWED_STATES.contains(current)) {
            throw new BusinessException(
                    "No se puede actualizar la visita cuando el caso "
                            + "se encuentra en estado "
                            + current
            );
        }
    }

    /**
     * Resuelve el estado del caso según el resultado de visita.
     *
     * <p>REALIZADA y NO_ATENDIDA cierran el caso.</p>
     *
     * <p>CANCELADA cancela el caso.</p>
     *
     * <p>REPROGRAMADA mantiene el caso abierto.</p>
     */
    public static String resolveStateFromVisit(
            String estadoVisita,
            Boolean encuestaRealizada
    ) {
        if (Boolean.TRUE.equals(encuestaRealizada)) {
            return CERRADO;
        }

        String normalizedVisitState =
                normalize(estadoVisita);

        if (normalizedVisitState == null) {
            throw new BusinessException(
                    "El estado de la visita es obligatorio"
            );
        }

        return switch (normalizedVisitState) {
            case "PENDIENTE" ->
                    ASIGNADO_ENCUESTADOR;

            case "PROGRAMADA" ->
                    VISITA_PROGRAMADA;

            case "REALIZADA" ->
                    CERRADO;

            case "NO_ATENDIDA" ->
                    CERRADO;

            case "REPROGRAMADA" ->
                    REPROGRAMADO;

            case "CANCELADA" ->
                    CANCELADO;

            default ->
                    throw new BusinessException(
                            "Estado de visita no permitido: "
                                    + normalizedVisitState
                    );
        };
    }

    public static String validateVisitTransition(
            String currentState,
            String targetState
    ) {
        String current =
                normalizeCurrentState(currentState);

        validateCanUpdateVisit(current);

        String target =
                requireKnownTargetState(targetState);

        if (!VISIT_TARGET_STATES.contains(target)) {
            throw new BusinessException(
                    "El resultado de visita no puede llevar "
                            + "el caso al estado "
                            + target
            );
        }

        if (current.equals(target)) {
            return current;
        }

        validateTransition(
                current,
                target,
                "resultado de visita"
        );

        return target;
    }

    public static String normalizeCurrentState(
            String state
    ) {
        String normalized =
                normalize(state);

        if (normalized == null) {
            return PENDIENTE_ENRUTAMIENTO;
        }

        if (!KNOWN_STATES.contains(normalized)) {
            throw new BusinessException(
                    "El caso contiene un estado no reconocido: "
                            + normalized
            );
        }

        return normalized;
    }

    public static String requireKnownTargetState(
            String state
    ) {
        String normalized =
                normalize(state);

        if (normalized == null) {
            throw new BusinessException(
                    "El estado destino del caso es obligatorio"
            );
        }

        if (!KNOWN_STATES.contains(normalized)) {
            throw new BusinessException(
                    "Estado de caso no reconocido: "
                            + normalized
            );
        }

        return normalized;
    }

    public static boolean isFinalState(
            String state
    ) {
        return FINAL_STATES.contains(
                normalizeCurrentState(state)
        );
    }

    public static boolean isClosedState(
            String state
    ) {
        return CERRADO.equals(
                normalizeCurrentState(state)
        );
    }

    public static boolean isCancelledState(
            String state
    ) {
        return CANCELADO.equals(
                normalizeCurrentState(state)
        );
    }

    private static void validateTransition(
            String currentState,
            String targetState,
            String operation
    ) {
        String current =
                normalizeCurrentState(currentState);

        String target =
                requireKnownTargetState(targetState);

        Set<String> allowedTargets =
                ALLOWED_TRANSITIONS.getOrDefault(
                        current,
                        Set.of()
                );

        if (!allowedTargets.contains(target)) {
            throw new BusinessException(
                    "No se permite la transición de "
                            + current
                            + " a "
                            + target
                            + " durante la operación "
                            + operation
            );
        }
    }

    private static String normalize(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized =
                value.trim()
                        .toUpperCase(Locale.ROOT);

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}