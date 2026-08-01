package com.appsisben.backend.modules.callcenter.application;

import com.appsisben.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CallCenterStatePolicyTest {

    @Test
    void shouldAssignFuncionarioFromPendingRouting() {
        String result =
                CallCenterStatePolicy
                        .validateFuncionarioAssignment(
                                CallCenterStatePolicy
                                        .PENDIENTE_ENRUTAMIENTO
                        );

        assertEquals(
                CallCenterStatePolicy.ASIGNADO_CALLCENTER,
                result
        );
    }

    @Test
    void shouldKeepFuncionarioAssignmentIdempotent() {
        String result =
                CallCenterStatePolicy
                        .validateFuncionarioAssignment(
                                CallCenterStatePolicy
                                        .ASIGNADO_CALLCENTER
                        );

        assertEquals(
                CallCenterStatePolicy.ASIGNADO_CALLCENTER,
                result
        );
    }

    @Test
    void shouldRejectFuncionarioAssignmentAfterCallStage() {
        assertThrows(
                BusinessException.class,
                () -> CallCenterStatePolicy
                        .validateFuncionarioAssignment(
                                CallCenterStatePolicy
                                        .EN_GESTION_LLAMADA
                        )
        );
    }

    @Test
    void shouldAllowGeneralRequestWithoutChangingState() {
        assertDoesNotThrow(
                () -> CallCenterStatePolicy
                        .validateGeneralRequestState(
                                CallCenterStatePolicy
                                        .ASIGNADO_CALLCENTER,
                                CallCenterStatePolicy
                                        .ASIGNADO_CALLCENTER
                        )
        );

        assertDoesNotThrow(
                () -> CallCenterStatePolicy
                        .validateGeneralRequestState(
                                CallCenterStatePolicy
                                        .ASIGNADO_CALLCENTER,
                                null
                        )
        );
    }

    @Test
    void shouldRejectGeneralRequestThatChangesState() {
        assertThrows(
                BusinessException.class,
                () -> CallCenterStatePolicy
                        .validateGeneralRequestState(
                                CallCenterStatePolicy
                                        .ASIGNADO_CALLCENTER,
                                CallCenterStatePolicy
                                        .NO_CONTACTADO
                        )
        );
    }

    @Test
    void shouldAllowCallWhenVisitIsProgrammed() {
        assertDoesNotThrow(
                () -> CallCenterStatePolicy
                        .validateCanRegisterCall(
                                CallCenterStatePolicy
                                        .VISITA_PROGRAMADA
                        )
        );
    }

    @Test
    void shouldAllowCallWhenVisitWasReprogrammed() {
        assertDoesNotThrow(
                () -> CallCenterStatePolicy
                        .validateCanRegisterCall(
                                CallCenterStatePolicy
                                        .REPROGRAMADO
                        )
        );
    }

    @Test
    void shouldRejectCallOnClosedCase() {
        assertThrows(
                BusinessException.class,
                () -> CallCenterStatePolicy
                        .validateCanRegisterCall(
                                CallCenterStatePolicy.CERRADO
                        )
        );
    }

    @Test
    void shouldRejectCallOnCancelledCase() {
        assertThrows(
                BusinessException.class,
                () -> CallCenterStatePolicy
                        .validateCanRegisterCall(
                                CallCenterStatePolicy.CANCELADO
                        )
        );
    }

    @Test
    void shouldMoveAssignedCaseToNoContacted() {
        String result =
                CallCenterStatePolicy
                        .validateCallTransition(
                                CallCenterStatePolicy
                                        .ASIGNADO_CALLCENTER,
                                CallCenterStatePolicy
                                        .NO_CONTACTADO
                        );

        assertEquals(
                CallCenterStatePolicy.NO_CONTACTADO,
                result
        );
    }

    @Test
    void shouldPreserveProgrammedVisitAfterMissedCall() {
        String result =
                CallCenterStatePolicy
                        .validateCallTransition(
                                CallCenterStatePolicy
                                        .VISITA_PROGRAMADA,
                                CallCenterStatePolicy
                                        .NO_CONTACTADO
                        );

        assertEquals(
                CallCenterStatePolicy.VISITA_PROGRAMADA,
                result
        );
    }

    @Test
    void shouldPreserveAssignedSurveyorAfterConnectedCall() {
        String result =
                CallCenterStatePolicy
                        .validateCallTransition(
                                CallCenterStatePolicy
                                        .ASIGNADO_ENCUESTADOR,
                                CallCenterStatePolicy
                                        .EN_GESTION_LLAMADA
                        );

        assertEquals(
                CallCenterStatePolicy.ASIGNADO_ENCUESTADOR,
                result
        );
    }

    @Test
    void shouldPreserveReprogrammedVisitAfterMissedCall() {
        String result =
                CallCenterStatePolicy
                        .validateCallTransition(
                                CallCenterStatePolicy
                                        .REPROGRAMADO,
                                CallCenterStatePolicy
                                        .NO_CONTACTADO
                        );

        assertEquals(
                CallCenterStatePolicy.REPROGRAMADO,
                result
        );
    }

    @Test
    void shouldAllowVisitAssignmentAfterNoContact() {
        String result =
                CallCenterStatePolicy
                        .validateEncuestadorAssignment(
                                CallCenterStatePolicy
                                        .NO_CONTACTADO,
                                true
                        );

        assertEquals(
                CallCenterStatePolicy.VISITA_PROGRAMADA,
                result
        );
    }

    @Test
    void shouldAllowVisitAssignmentBeforeAnySuccessfulCall() {
        String result =
                CallCenterStatePolicy
                        .validateEncuestadorAssignment(
                                CallCenterStatePolicy
                                        .ASIGNADO_CALLCENTER,
                                false
                        );

        assertEquals(
                CallCenterStatePolicy.ASIGNADO_ENCUESTADOR,
                result
        );
    }

    @Test
    void shouldRejectSecondVisitAssignmentWhenAlreadyProgrammed() {
        assertThrows(
                BusinessException.class,
                () -> CallCenterStatePolicy
                        .validateEncuestadorAssignment(
                                CallCenterStatePolicy
                                        .VISITA_PROGRAMADA,
                                true
                        )
        );
    }

    @Test
    void shouldResolveCompletedVisitAsClosed() {
        String result =
                CallCenterStatePolicy
                        .resolveStateFromVisit(
                                "REALIZADA",
                                true
                        );

        assertEquals(
                CallCenterStatePolicy.CERRADO,
                result
        );
    }

    @Test
    void shouldResolveUnattendedVisitAsFollowUp() {
        String result =
                CallCenterStatePolicy
                        .resolveStateFromVisit(
                                "NO_ATENDIDA",
                                false
                        );

        assertEquals(
                CallCenterStatePolicy.VISITA_NO_ATENDIDA,
                result
        );
    }

    @Test
    void shouldResolveReprogrammedVisit() {
        String result =
                CallCenterStatePolicy
                        .resolveStateFromVisit(
                                "REPROGRAMADA",
                                false
                        );

        assertEquals(
                CallCenterStatePolicy.REPROGRAMADO,
                result
        );
    }

    @Test
    void shouldResolveCancelledVisit() {
        String result =
                CallCenterStatePolicy
                        .resolveStateFromVisit(
                                "CANCELADA",
                                false
                        );

        assertEquals(
                CallCenterStatePolicy.CANCELADO,
                result
        );
    }

    @Test
    void shouldAllowClosingProgrammedVisit() {
        String result =
                CallCenterStatePolicy
                        .validateVisitTransition(
                                CallCenterStatePolicy
                                        .VISITA_PROGRAMADA,
                                CallCenterStatePolicy.CERRADO
                        );

        assertEquals(
                CallCenterStatePolicy.CERRADO,
                result
        );
    }
}