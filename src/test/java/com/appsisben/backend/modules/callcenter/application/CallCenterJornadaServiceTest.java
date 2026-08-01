package com.appsisben.backend.modules.callcenter.application;

import com.appsisben.backend.modules.callcenter.dto.CallCenterAsignarFuncionarioRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterResponse;
import com.appsisben.backend.modules.callcenter.dto.CallCenterUltimaHoraRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterUltimaHoraResponse;
import com.appsisben.backend.modules.callcenter.dto.CallCenterVisitaAsignacionRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterVisitaResponse;
import com.appsisben.backend.modules.callcenter.repository.CallCenterRegistroRepository;
import com.appsisben.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallCenterJornadaServiceTest {

    @Mock
    private CallCenterRegistroRepository
            callCenterRegistroRepository;

    @Mock
    private CallCenterService
            callCenterService;

    @Mock
    private CallCenterWorkflowService
            callCenterWorkflowService;

    @InjectMocks
    private CallCenterJornadaService service;

    @Test
    void shouldCreateManualLastMinuteCitizen() {
        CallCenterUltimaHoraRequest request =
                manualRequest();

        CallCenterResponse created =
                mock(CallCenterResponse.class);

        CallCenterResponse finalRegistro =
                mock(CallCenterResponse.class);

        CallCenterVisitaResponse visita =
                mock(CallCenterVisitaResponse.class);

        when(created.id())
                .thenReturn(99L);

        when(
                callCenterRegistroRepository
                        .existsNuevaEncuestaActivaNoRealizada(
                                null,
                                "123456789"
                        )
        ).thenReturn(false);

        when(
                callCenterService.create(
                        any(CallCenterRequest.class)
                )
        ).thenReturn(created);

        when(
                callCenterService
                        .asignarFuncionarioCallcenter(
                                any(
                                        CallCenterAsignarFuncionarioRequest
                                                .class
                                )
                        )
        ).thenReturn(
                List.of(created)
        );

        when(
                callCenterWorkflowService
                        .asignarVisita(
                                eq(99L),
                                any(
                                        CallCenterVisitaAsignacionRequest
                                                .class
                                )
                        )
        ).thenReturn(visita);

        when(
                callCenterService.findById(99L)
        ).thenReturn(finalRegistro);

        CallCenterUltimaHoraResponse response =
                service.crearUltimaHora(
                        request
                );

        assertSame(
                finalRegistro,
                response.registro()
        );

        assertSame(
                visita,
                response.visita()
        );

        ArgumentCaptor<CallCenterRequest>
                createCaptor =
                ArgumentCaptor.forClass(
                        CallCenterRequest.class
                );

        verify(
                callCenterService
        ).create(
                createCaptor.capture()
        );

        CallCenterRequest createRequest =
                createCaptor.getValue();

        assertEquals(
                "BASE_ENCUESTADOR",
                createRequest.tipoRegistro()
        );

        assertEquals(
                "MANUAL",
                createRequest.origenRegistro()
        );

        assertEquals(
                "NUEVA_ENCUESTA",
                createRequest.tipoSolicitudCallcenter()
        );

        assertEquals(
                LocalDate.of(2026, 8, 1),
                createRequest.fechaLlamada()
        );

        assertEquals(
                "123456789",
                createRequest.cedulaSolicitante()
        );

        assertEquals(
                Boolean.TRUE,
                createRequest.solicitoNuevaEncuesta()
        );

        assertEquals(
                Boolean.TRUE,
                createRequest.activo()
        );

        ArgumentCaptor<CallCenterAsignarFuncionarioRequest>
                funcionarioCaptor =
                ArgumentCaptor.forClass(
                        CallCenterAsignarFuncionarioRequest.class
                );

        verify(
                callCenterService
        ).asignarFuncionarioCallcenter(
                funcionarioCaptor.capture()
        );

        assertEquals(
                10L,
                funcionarioCaptor
                        .getValue()
                        .funcionarioCallcenterId()
        );

        assertEquals(
                List.of(99L),
                funcionarioCaptor
                        .getValue()
                        .registroIds()
        );

        ArgumentCaptor<CallCenterVisitaAsignacionRequest>
                visitaCaptor =
                ArgumentCaptor.forClass(
                        CallCenterVisitaAsignacionRequest.class
                );

        verify(
                callCenterWorkflowService
        ).asignarVisita(
                eq(99L),
                visitaCaptor.capture()
        );

        assertEquals(
                20L,
                visitaCaptor
                        .getValue()
                        .encuestadorId()
        );

        assertEquals(
                LocalDate.of(2026, 8, 2),
                visitaCaptor
                        .getValue()
                        .fechaProgramada()
        );

        assertEquals(
                LocalTime.of(8, 30),
                visitaCaptor
                        .getValue()
                        .horaProgramada()
        );
    }

    @Test
    void shouldUseVentanillaOriginWhenIdIsProvided() {
        CallCenterUltimaHoraRequest request =
                new CallCenterUltimaHoraRequest(
                        LocalDate.of(2026, 8, 1),
                        55L,
                        "123456789",
                        "Ciudadano prueba",
                        "3001234567",
                        "Calle 10 número 20-30",
                        7L,
                        10L,
                        20L,
                        LocalDate.of(2026, 8, 2),
                        LocalTime.of(8, 30),
                        "Ciudadano agregado durante la jornada"
                );

        CallCenterResponse created =
                mock(CallCenterResponse.class);

        CallCenterResponse finalRegistro =
                mock(CallCenterResponse.class);

        CallCenterVisitaResponse visita =
                mock(CallCenterVisitaResponse.class);

        when(created.id())
                .thenReturn(100L);

        when(
                callCenterRegistroRepository
                        .existsNuevaEncuestaActivaNoRealizada(
                                55L,
                                "123456789"
                        )
        ).thenReturn(false);

        when(
                callCenterService.create(
                        any(CallCenterRequest.class)
                )
        ).thenReturn(created);

        when(
                callCenterService
                        .asignarFuncionarioCallcenter(
                                any(
                                        CallCenterAsignarFuncionarioRequest
                                                .class
                                )
                        )
        ).thenReturn(
                List.of(created)
        );

        when(
                callCenterWorkflowService
                        .asignarVisita(
                                eq(100L),
                                any(
                                        CallCenterVisitaAsignacionRequest
                                                .class
                                )
                        )
        ).thenReturn(visita);

        when(
                callCenterService.findById(100L)
        ).thenReturn(finalRegistro);

        service.crearUltimaHora(
                request
        );

        ArgumentCaptor<CallCenterRequest>
                createCaptor =
                ArgumentCaptor.forClass(
                        CallCenterRequest.class
                );

        verify(
                callCenterService
        ).create(
                createCaptor.capture()
        );

        assertEquals(
                "VENTANILLA",
                createCaptor
                        .getValue()
                        .origenRegistro()
        );

        assertEquals(
                55L,
                createCaptor
                        .getValue()
                        .ventanillaRegistroId()
        );
    }

    @Test
    void shouldRejectCitizenWithActivePendingSurvey() {
        CallCenterUltimaHoraRequest request =
                manualRequest();

        when(
                callCenterRegistroRepository
                        .existsNuevaEncuestaActivaNoRealizada(
                                null,
                                "123456789"
                        )
        ).thenReturn(true);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.crearUltimaHora(
                                request
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "ya tiene una nueva encuesta"
                        )
        );

        verify(
                callCenterService,
                never()
        ).create(
                any(CallCenterRequest.class)
        );

        verify(
                callCenterService,
                never()
        ).asignarFuncionarioCallcenter(
                any(
                        CallCenterAsignarFuncionarioRequest
                                .class
                )
        );

        verify(
                callCenterWorkflowService,
                never()
        ).asignarVisita(
                any(),
                any(
                        CallCenterVisitaAsignacionRequest
                                .class
                )
        );
    }

    private CallCenterUltimaHoraRequest
    manualRequest() {
        return new CallCenterUltimaHoraRequest(
                LocalDate.of(2026, 8, 1),
                null,
                " 123456789 ",
                "Ciudadano prueba",
                "3001234567",
                "Calle 10 número 20-30",
                7L,
                10L,
                20L,
                LocalDate.of(2026, 8, 2),
                LocalTime.of(8, 30),
                "Ciudadano agregado durante la jornada"
        );
    }
}