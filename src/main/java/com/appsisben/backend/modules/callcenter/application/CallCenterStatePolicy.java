package com.appsisben.backend.modules.callcenter.application;

import com.appsisben.backend.shared.exception.BusinessException;

import java.util.Map;
import java.util.Set;

/**
 * Política central de estados del módulo Call Center.
 *
 * Controla las etapas del flujo, evita retrocesos y restringe
 * cada operación a los estados desde los cuales está permitida.
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

    /**
     * Estado legacy reconocido para registros históricos.
     *
     * El flujo formal actual cierra el caso cuando la visita
     * o la encuesta se marcan como realizadas.
     */
    public static final String VISITA_REALIZADA =
            "VISITA_REALIZADA";

    public static final String CERRADO =
            "CERRADO";

    public static final String CANCELADO =
            "CANCELADO";

    private static final Map<String, Integer> STAGES =
            Map.ofEntries(
                    Map.entry(PENDIENTE_ENRUTAMIENTO, 1),
                    Map.entry(ASIGNADO_CALLCENTER, 2),

                    Map.entry(EN_GESTION_LLAMADA, 3),
                    Map.entry(NO_CONTACTADO, 3),
                    Map.entry(CONTACTADO_SIN_DISPOSICION, 3),

                    Map.entry(PENDIENTE_ASIGNAR_ENCUESTADOR, 4),

                    Map.entry(ASIGNADO_ENCUESTADOR, 5),

                    Map.entry(VISITA_PROGRAMADA, 6),
                    Map.entry(VISITA_NO_ATENDIDA, 6),
                    Map.entry(REPROGRAMADO, 6),
                    Map.entry(VISITA_REALIZADA, 6),

                    Map.entry(CERRADO, 7),
                    Map.entry(CANCELADO, 7)
            );

    private static final Set<String> CALL_SOURCE_STATES =
            Set.of(
                    ASIGNADO_CALLCENTER,
                    EN_GESTION_LLAMADA,
                    NO_CONTACTADO,
                    CONTACTADO_SIN_DISPOSICION
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

    private static final Set<String> VISIT_SOURCE_STATES =
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

    private static final Set<String> FINAL_STATES =
            Set.of(
                    CERRADO,
                    CANCELADO
            );

    private CallCenterStatePolicy() {
    }

    /**
     * Valida que el request general no cambie el estado del caso.
     *
     * El frontend puede omitir el estado o enviar exactamente
     * el mismo estado persistido.
     */
    public static void validateGeneralRequestState(
            String currentState,
            String requestedState
    ) {
        String current =
                normalizeCurrentState(currentState);

        if (isBlank(requestedState)) {
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
     * Valida la asignación de un caso a funcionario Call Center.
     *
     * Se permite desde PENDIENTE_ENRUTAMIENTO. También se permite
     * repetir de forma idempotente sobre ASIGNADO_CALLCENTER.
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

        validateForwardTransition(
                current,
                ASIGNADO_CALLCENTER
        );

        return ASIGNADO_CALLCENTER;
    }

    /**
     * Valida que el caso se encuentre dentro de la etapa telefónica.
     */
    public static void validateCanRegisterCall(
            String currentState
    ) {
        String current =
                normalizeCurrentState(currentState);

        if (!CALL_SOURCE_STATES.contains(current)) {
            throw new BusinessException(
                    "No se puede registrar una nueva llamada "
                            + "cuando el caso se encuentra en estado "
                            + current
            );
        }
    }

    /**
     * Valida la transición generada por un resultado de llamada.
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

        validateForwardTransition(
                current,
                target
        );

        return target;
    }

    /**
     * Valida una nueva asignación de encuestador.
     */
    public static String validateEncuestadorAssignment(
            String currentState,
            boolean programmed
    ) {
        String current =
                normalizeCurrentState(currentState);

        if (!PENDIENTE_ASIGNAR_ENCUESTADOR.equals(current)) {
            throw new BusinessException(
                    "El caso no puede asignarse a un encuestador "
                            + "desde el estado "
                            + current
            );
        }

        String target = programmed
                ? VISITA_PROGRAMADA
                : ASIGNADO_ENCUESTADOR;

        validateForwardTransition(
                current,
                target
        );

        return target;
    }

    /**
     * Valida que el caso permita registrar un resultado de visita.
     */
    public static void validateCanUpdateVisit(
            String currentState
    ) {
        String current =
                normalizeCurrentState(currentState);

        if (!VISIT_SOURCE_STATES.contains(current)) {
            throw new BusinessException(
                    "No se puede actualizar la visita cuando "
                            + "el caso se encuentra en estado "
                            + current
            );
        }
    }

    /**
     * Resuelve el estado del caso a partir del resultado operativo
     * de una visita.
     */
    public static String resolveStateFromVisit(
            String visitState,
            Boolean surveyCompleted
    ) {
        if (Boolean.TRUE.equals(surveyCompleted)) {
            return CERRADO;
        }

        String normalizedVisitState =
                normalize(visitState);

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

            default -> throw new BusinessException(
                    "Estado de visita no permitido: "
                            + normalizedVisitState
            );
        };
    }

    /**
     * Valida una transición producida por el resultado de visita.
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

        validateForwardTransition(
                current,
                target
        );

        return target;
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

    /**
     * Normaliza un estado persistido.
     *
     * Los registros legacy sin estado se interpretan como
     * PENDIENTE_ENRUTAMIENTO.
     */
    public static String normalizeCurrentState(
            String state
    ) {
        String normalized =
                normalize(state);

        if (normalized == null) {
            return PENDIENTE_ENRUTAMIENTO;
        }

        if (!STAGES.containsKey(normalized)) {
            throw new BusinessException(
                    "El caso contiene un estado no reconocido: "
                            + normalized
            );
        }

        return normalized;
    }

    /**
     * Valida un estado destino enviado por una acción operativa.
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

        if (!STAGES.containsKey(normalized)) {
            throw new BusinessException(
                    "Estado de caso no reconocido: "
                            + normalized
            );
        }

        return normalized;
    }

    private static void validateForwardTransition(
            String currentState,
            String targetState
    ) {
        String current =
                normalizeCurrentState(currentState);

        String target =
                requireKnownTargetState(targetState);

        Integer currentStage =
                STAGES.get(current);

        Integer targetStage =
                STAGES.get(target);

        if (targetStage < currentStage) {
            throw new BusinessException(
                    "No se permite retroceder el caso desde "
                            + current
                            + " hacia "
                            + target
            );
        }

        if (
                FINAL_STATES.contains(current)
                        && !current.equals(target)
        ) {
            throw new BusinessException(
                    "El caso se encuentra en un estado final: "
                            + current
            );
        }
    }

    private static String normalize(
            String value
    ) {
        if (isBlank(value)) {
            return null;
        }

        return value.trim().toUpperCase();
    }

    private static boolean isBlank(
            String value
    ) {
        return value == null
                || value.trim().isEmpty();
    }
}