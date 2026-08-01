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

    /**
     * Estados reconocidos por el flujo.
     */
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

    /**
     * Estados finales.
     */
    private static final Set<String> FINAL_STATES =
            Set.of(
                    CERRADO,
                    CANCELADO
            );

    /**
     * Estados desde los cuales se puede registrar una llamada.
     *
     * <p>Se permite llamar aunque el encuestador ya esté asignado
     * o la visita esté programada.</p>
     */
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

    /**
     * Estados que pueden ser producidos por un resultado de llamada.
     */
    private static final Set<String> CALL_TARGET_STATES =
            Set.of(
                    EN_GESTION_LLAMADA,
                    NO_CONTACTADO,
                    CONTACTADO_SIN_DISPOSICION,
                    PENDIENTE_ASIGNAR_ENCUESTADOR,
                    CERRADO,
                    CANCELADO
            );

    /**
     * Resultados telefónicos que no deben hacer retroceder
     * un caso que ya avanzó hacia la visita.
     */
    private static final Set<String> TELEPHONE_STAGE_STATES =
            Set.of(
                    EN_GESTION_LLAMADA,
                    NO_CONTACTADO,
                    CONTACTADO_SIN_DISPOSICION,
                    PENDIENTE_ASIGNAR_ENCUESTADOR
            );

    /**
     * Estados cuyo avance debe conservarse frente a un nuevo
     * resultado telefónico.
     */
    private static final Set<String> ADVANCED_FIELD_STATES =
            Set.of(
                    PENDIENTE_ASIGNAR_ENCUESTADOR,
                    ASIGNADO_ENCUESTADOR,
                    VISITA_PROGRAMADA,
                    REPROGRAMADO
            );

    /**
     * Estados desde los cuales se permite crear la primera
     * asignación de encuestador.
     *
     * <p>No se exige que la llamada haya sido contestada.</p>
     */
    private static final Set<String> VISIT_ASSIGNMENT_ALLOWED_STATES =
            Set.of(
                    ASIGNADO_CALLCENTER,
                    EN_GESTION_LLAMADA,
                    NO_CONTACTADO,
                    CONTACTADO_SIN_DISPOSICION,
                    PENDIENTE_ASIGNAR_ENCUESTADOR
            );

    /**
     * Estados desde los cuales se puede actualizar el resultado
     * de una visita.
     */
    private static final Set<String> VISIT_UPDATE_ALLOWED_STATES =
            Set.of(
                    ASIGNADO_ENCUESTADOR,
                    VISITA_PROGRAMADA,
                    VISITA_NO_ATENDIDA,
                    REPROGRAMADO,
                    VISITA_REALIZADA
            );

    /**
     * Estados que pueden resultar de una gestión de visita.
     */
    private static final Set<String> VISIT_TARGET_STATES =
            Set.of(
                    ASIGNADO_ENCUESTADOR,
                    VISITA_PROGRAMADA,
                    VISITA_NO_ATENDIDA,
                    REPROGRAMADO,
                    CERRADO,
                    CANCELADO
            );

    /**
     * Transiciones formales permitidas.
     */
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

    /**
     * Mantiene la compatibilidad con CallCenterService.
     *
     * <p>El request general puede omitir el estado o enviar el mismo
     * estado que ya está persistido. No puede cambiarlo.</p>
     */
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

    /**
     * Mantiene la compatibilidad con la asignación administrativa
     * de funcionarios Call Center.
     *
     * <p>Se permite desde PENDIENTE_ENRUTAMIENTO y también se permite
     * repetir sobre ASIGNADO_CALLCENTER de manera idempotente.</p>
     */
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

    /**
     * Valida que el caso permita registrar una llamada.
     */
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

    /**
     * Valida el resultado de una gestión telefónica.
     *
     * <p>Cuando el caso ya avanzó hacia la visita, un resultado
     * telefónico no hace retroceder su estado.</p>
     */
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

        /*
         * Ejemplos:
         *
         * VISITA_PROGRAMADA + NO_CONTACTADO
         * conserva VISITA_PROGRAMADA.
         *
         * ASIGNADO_ENCUESTADOR + EN_GESTION_LLAMADA
         * conserva ASIGNADO_ENCUESTADOR.
         */
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

    /**
     * Valida la asignación inicial de encuestador.
     *
     * <p>La asignación no depende de que la llamada haya sido
     * contestada.</p>
     */
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

    /**
     * Valida que el caso permita actualizar el resultado de visita.
     */
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
     * Resuelve el estado general del caso a partir del resultado
     * operativo de la visita.
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
                    VISITA_NO_ATENDIDA;

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

    /**
     * Valida la transición generada por un resultado de visita.
     */
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

    /**
     * Normaliza un estado persistido.
     *
     * <p>Los registros anteriores que no tengan estado se interpretan
     * como PENDIENTE_ENRUTAMIENTO, manteniendo compatibilidad con
     * los datos existentes.</p>
     */
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

    /**
     * Valida y normaliza un estado destino.
     */
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

    /**
     * Indica si el caso se encuentra cerrado o cancelado.
     */
    public static boolean isFinalState(
            String state
    ) {
        return FINAL_STATES.contains(
                normalizeCurrentState(state)
        );
    }

    /**
     * Indica si el caso se encuentra cerrado.
     */
    public static boolean isClosedState(
            String state
    ) {
        return CERRADO.equals(
                normalizeCurrentState(state)
        );
    }

    /**
     * Indica si el caso se encuentra cancelado.
     *
     * <p>Se conserva este método público para compatibilidad con
     * cualquier servicio o prueba existente.</p>
     */
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