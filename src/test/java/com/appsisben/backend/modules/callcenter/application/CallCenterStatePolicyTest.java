package com.appsisben.backend.modules.callcenter.application;

import com.appsisben.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallCenterStatePolicyTest {

    @Test
    void shouldAssignFuncionarioFromPendingRouting() {
        assertEquals(
                CallCenterStatePolicy.ASIGNADO_CALLCENTER,
                CallCenterStatePolicy.validateFuncionarioAssignment(
                        CallCenterStatePolicy.PENDIENTE_ENRUTAMIENTO
                )
        );
    }

    @Test
    void shouldAllowIdempotentFuncionarioAssignment() {
        assertEquals(
                CallCenterStatePolicy.ASIGNADO_CALLCENTER,
                CallCenterStatePolicy.validateFuncionarioAssignment(
                        CallCenterStatePolicy.ASIGNADO_CALLCENTER
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "EN_GESTION_LLAMADA",
            "NO_CONTACTADO",
            "CONTACTADO_SIN_DISPOSICION",
            "PENDIENTE_ASIGNAR_ENCUESTADOR",
            "ASIGNADO_ENCUESTADOR",
            "VISITA_PROGRAMADA",
            "CERRADO",
            "CANCELADO"
    })
    void shouldRejectFuncionarioAssignmentFromAdvancedState(
            String currentState
    ) {
        assertThrows(
                BusinessException.class,
                () -> CallCenterStatePolicy
                        .validateFuncionarioAssignment(currentState)
        );
    }

    @ParameterizedTest
    @CsvSource({
            "ASIGNADO_CALLCENTER, EN_GESTION_LLAMADA",
            "ASIGNADO_CALLCENTER, NO_CONTACTADO",
            "NO_CONTACTADO, NO_CONTACTADO",
            "NO_CONTACTADO, EN_GESTION_LLAMADA",
            "NO_CONTACTADO, CONTACTADO_SIN_DISPOSICION",
            "CONTACTADO_SIN_DISPOSICION, NO_CONTACTADO",
            "CONTACTADO_SIN_DISPOSICION, EN_GESTION_LLAMADA",
            "EN_GESTION_LLAMADA, PENDIENTE_ASIGNAR_ENCUESTADOR",
            "NO_CONTACTADO, PENDIENTE_ASIGNAR_ENCUESTADOR",
            "CONTACTADO_SIN_DISPOSICION, PENDIENTE_ASIGNAR_ENCUESTADOR",
            "EN_GESTION_LLAMADA, CERRADO",
            "EN_GESTION_LLAMADA, CANCELADO"
    })
    void shouldAllowValidCallTransition(
            String currentState,
            String targetState
    ) {
        assertEquals(
                targetState,
                CallCenterStatePolicy.validateCallTransition(
                        currentState,
                        targetState
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "PENDIENTE_ENRUTAMIENTO",
            "PENDIENTE_ASIGNAR_ENCUESTADOR",
            "ASIGNADO_ENCUESTADOR",
            "VISITA_PROGRAMADA",
            "VISITA_NO_ATENDIDA",
            "REPROGRAMADO",
            "CERRADO",
            "CANCELADO"
    })
    void shouldRejectCallOutsideTelephoneStage(
            String currentState
    ) {
        assertThrows(
                BusinessException.class,
                () -> CallCenterStatePolicy
                        .validateCanRegisterCall(currentState)
        );
    }

    @Test
    void shouldAssignEncuestadorWithoutProgramming() {
        assertEquals(
                CallCenterStatePolicy.ASIGNADO_ENCUESTADOR,
                CallCenterStatePolicy.validateEncuestadorAssignment(
                        CallCenterStatePolicy
                                .PENDIENTE_ASIGNAR_ENCUESTADOR,
                        false
                )
        );
    }

    @Test
    void shouldAssignEncuestadorWithProgramming() {
        assertEquals(
                CallCenterStatePolicy.VISITA_PROGRAMADA,
                CallCenterStatePolicy.validateEncuestadorAssignment(
                        CallCenterStatePolicy
                                .PENDIENTE_ASIGNAR_ENCUESTADOR,
                        true
                )
        );
    }

    @ParameterizedTest
    @CsvSource({
            "ASIGNADO_ENCUESTADOR, ASIGNADO_ENCUESTADOR",
            "ASIGNADO_ENCUESTADOR, VISITA_PROGRAMADA",
            "VISITA_PROGRAMADA, VISITA_NO_ATENDIDA",
            "VISITA_NO_ATENDIDA, REPROGRAMADO",
            "REPROGRAMADO, VISITA_PROGRAMADA",
            "VISITA_PROGRAMADA, CERRADO",
            "VISITA_PROGRAMADA, CANCELADO",
            "REPROGRAMADO, CERRADO",
            "REPROGRAMADO, CANCELADO"
    })
    void shouldAllowValidVisitTransition(
            String currentState,
            String targetState
    ) {
        assertEquals(
                targetState,
                CallCenterStatePolicy.validateVisitTransition(
                        currentState,
                        targetState
                )
        );
    }

    @Test
    void shouldRejectVisitRollbackToAssignedEncuestador() {
        assertThrows(
                BusinessException.class,
                () -> CallCenterStatePolicy.validateVisitTransition(
                        CallCenterStatePolicy.VISITA_PROGRAMADA,
                        CallCenterStatePolicy.ASIGNADO_ENCUESTADOR
                )
        );
    }

    @ParameterizedTest
    @CsvSource({
            "PENDIENTE, false, ASIGNADO_ENCUESTADOR",
            "PROGRAMADA, false, VISITA_PROGRAMADA",
            "NO_ATENDIDA, false, VISITA_NO_ATENDIDA",
            "REPROGRAMADA, false, REPROGRAMADO",
            "REALIZADA, false, CERRADO",
            "CANCELADA, false, CANCELADO",
            "PROGRAMADA, true, CERRADO"
    })
    void shouldResolveCaseStateFromVisit(
            String visitState,
            boolean surveyCompleted,
            String expectedState
    ) {
        assertEquals(
                expectedState,
                CallCenterStatePolicy.resolveStateFromVisit(
                        visitState,
                        surveyCompleted
                )
        );
    }

    @Test
    void shouldRejectUnknownVisitState() {
        assertThrows(
                BusinessException.class,
                () -> CallCenterStatePolicy.resolveStateFromVisit(
                        "ESTADO_DESCONOCIDO",
                        false
                )
        );
    }

    @Test
    void shouldAllowGeneralRequestWithoutState() {
        assertDoesNotThrow(
                () -> CallCenterStatePolicy
                        .validateGeneralRequestState(
                                CallCenterStatePolicy.NO_CONTACTADO,
                                null
                        )
        );
    }

    @Test
    void shouldAllowGeneralRequestWithSameState() {
        assertDoesNotThrow(
                () -> CallCenterStatePolicy
                        .validateGeneralRequestState(
                                CallCenterStatePolicy.NO_CONTACTADO,
                                " no_contactado "
                        )
        );
    }

    @Test
    void shouldRejectGeneralRequestStateChange() {
        assertThrows(
                BusinessException.class,
                () -> CallCenterStatePolicy
                        .validateGeneralRequestState(
                                CallCenterStatePolicy.NO_CONTACTADO,
                                CallCenterStatePolicy.CERRADO
                        )
        );
    }

    @Test
    void shouldRecognizeFinalStates() {
        assertTrue(
                CallCenterStatePolicy.isFinalState(
                        CallCenterStatePolicy.CERRADO
                )
        );

        assertTrue(
                CallCenterStatePolicy.isFinalState(
                        CallCenterStatePolicy.CANCELADO
                )
        );

        assertFalse(
                CallCenterStatePolicy.isFinalState(
                        CallCenterStatePolicy.NO_CONTACTADO
                )
        );
    }

    @Test
    void shouldInterpretNullLegacyStateAsPendingRouting() {
        assertEquals(
                CallCenterStatePolicy.PENDIENTE_ENRUTAMIENTO,
                CallCenterStatePolicy.normalizeCurrentState(null)
        );
    }

    @Test
    void shouldRejectUnknownPersistedState() {
        assertThrows(
                BusinessException.class,
                () -> CallCenterStatePolicy
                        .normalizeCurrentState("DESCONOCIDO")
        );
    }
}