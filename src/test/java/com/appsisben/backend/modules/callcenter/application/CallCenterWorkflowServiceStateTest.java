package com.appsisben.backend.modules.callcenter.application;

import com.appsisben.backend.modules.audit.application.AuditService;
import com.appsisben.backend.modules.audit.domain.AuditAction;
import com.appsisben.backend.modules.callcenter.domain.CallCenterGestionLlamada;
import com.appsisben.backend.modules.callcenter.domain.CallCenterMotivoNoContacto;
import com.appsisben.backend.modules.callcenter.domain.CallCenterRegistro;
import com.appsisben.backend.modules.callcenter.domain.CallCenterResultadoLlamada;
import com.appsisben.backend.modules.callcenter.domain.CallCenterVisita;
import com.appsisben.backend.modules.callcenter.dto.CallCenterGestionLlamadaRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterVisitaAsignacionRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterVisitaResultadoRequest;
import com.appsisben.backend.modules.callcenter.repository.CallCenterGestionLlamadaRepository;
import com.appsisben.backend.modules.callcenter.repository.CallCenterMotivoNoContactoRepository;
import com.appsisben.backend.modules.callcenter.repository.CallCenterMotivoNoDisposicionRepository;
import com.appsisben.backend.modules.callcenter.repository.CallCenterRegistroRepository;
import com.appsisben.backend.modules.callcenter.repository.CallCenterResultadoLlamadaRepository;
import com.appsisben.backend.modules.callcenter.repository.CallCenterVisitaRepository;
import com.appsisben.backend.modules.catalogs.domain.Encuestador;
import com.appsisben.backend.modules.catalogs.repository.EncuestadorRepository;
import com.appsisben.backend.modules.users.domain.User;
import com.appsisben.backend.modules.users.repository.UserRepository;
import com.appsisben.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallCenterWorkflowServiceStateTest {

    @Mock
    private CallCenterRegistroRepository
            callCenterRegistroRepository;

    @Mock
    private CallCenterGestionLlamadaRepository
            gestionLlamadaRepository;

    @Mock
    private CallCenterVisitaRepository
            visitaRepository;

    @Mock
    private CallCenterResultadoLlamadaRepository
            resultadoLlamadaRepository;

    @Mock
    private CallCenterMotivoNoContactoRepository
            motivoNoContactoRepository;

    @Mock
    private CallCenterMotivoNoDisposicionRepository
            motivoNoDisposicionRepository;

    @Mock
    private EncuestadorRepository
            encuestadorRepository;

    @Mock
    private UserRepository
            userRepository;

    @Mock
    private AuditService
            auditService;

    @InjectMocks
    private CallCenterWorkflowService service;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("admin");
        currentUser.setNombres("Administrador");
        currentUser.setActivo(true);

        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                "admin",
                                "N/A",
                                List.of(
                                        new SimpleGrantedAuthority(
                                                "ADMIN"
                                        )
                                )
                        )
                );

        when(
                userRepository
                        .findByUsernameIgnoreCase("admin")
        ).thenReturn(
                Optional.of(currentUser)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPreserveProgrammedVisitAfterMissedCall() {
        CallCenterRegistro registro =
                activeRegistro(
                        CallCenterStatePolicy
                                .VISITA_PROGRAMADA
                );

        registro.setEstadoVisita("PROGRAMADA");

        CallCenterResultadoLlamada resultado =
                resultadoLlamada(
                        "NO_CONTACTADO",
                        "No contactado",
                        CallCenterStatePolicy.NO_CONTACTADO
                );

        CallCenterMotivoNoContacto motivo =
                activeMotivoNoContacto(
                        7L,
                        "Buzón"
                );

        CallCenterGestionLlamadaRequest request =
                new CallCenterGestionLlamadaRequest(
                        LocalDate.of(2026, 7, 31),
                        null,
                        false,
                        "NO_CONTACTADO",
                        7L,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "La llamada pasó a buzón"
                );

        when(
                callCenterRegistroRepository.findById(1L)
        ).thenReturn(
                Optional.of(registro)
        );

        when(
                resultadoLlamadaRepository
                        .findFirstByCodigoIgnoreCaseAndActivoTrue(
                                "NO_CONTACTADO"
                        )
        ).thenReturn(
                Optional.of(resultado)
        );

        when(
                motivoNoContactoRepository.findById(7L)
        ).thenReturn(
                Optional.of(motivo)
        );

        when(
                gestionLlamadaRepository
                        .countByCallCenterRegistroId(1L)
        ).thenReturn(0L);

        when(
                gestionLlamadaRepository.save(
                        any(CallCenterGestionLlamada.class)
                )
        ).thenAnswer(invocation -> {
            CallCenterGestionLlamada saved =
                    invocation.getArgument(0);

            saved.setId(100L);

            return saved;
        });

        when(
                callCenterRegistroRepository.save(registro)
        ).thenReturn(registro);

        service.registrarLlamada(
                1L,
                request
        );

        assertEquals(
                CallCenterStatePolicy.VISITA_PROGRAMADA,
                registro.getEstadoCaso()
        );

        assertEquals(
                "PROGRAMADA",
                registro.getEstadoVisita()
        );

        assertFalse(
                registro.getLlamadaConectada()
        );

        assertSame(
                motivo,
                registro.getMotivoNoContacto()
        );

        verify(
                gestionLlamadaRepository
        ).save(
                any(CallCenterGestionLlamada.class)
        );

        verify(
                callCenterRegistroRepository
        ).save(registro);
    }

    @Test
    void shouldStoreConfirmedDataAndPreserveItAfterMissedCall() {
        CallCenterRegistro registro =
                activeRegistro(
                        CallCenterStatePolicy
                                .ASIGNADO_CALLCENTER
                );

        CallCenterResultadoLlamada contactado =
                resultadoLlamada(
                        "CONTACTADO",
                        "Contactado",
                        CallCenterStatePolicy
                                .EN_GESTION_LLAMADA
                );

        CallCenterResultadoLlamada noContactado =
                resultadoLlamada(
                        "NO_CONTACTADO",
                        "No contactado",
                        CallCenterStatePolicy
                                .NO_CONTACTADO
                );

        CallCenterMotivoNoContacto motivo =
                activeMotivoNoContacto(
                        8L,
                        "No contestó"
                );

        when(
                callCenterRegistroRepository.findById(1L)
        ).thenReturn(
                Optional.of(registro)
        );

        when(
                resultadoLlamadaRepository
                        .findFirstByCodigoIgnoreCaseAndActivoTrue(
                                "CONTACTADO"
                        )
        ).thenReturn(
                Optional.of(contactado)
        );

        when(
                resultadoLlamadaRepository
                        .findFirstByCodigoIgnoreCaseAndActivoTrue(
                                "NO_CONTACTADO"
                        )
        ).thenReturn(
                Optional.of(noContactado)
        );

        when(
                motivoNoContactoRepository.findById(8L)
        ).thenReturn(
                Optional.of(motivo)
        );

        when(
                gestionLlamadaRepository
                        .countByCallCenterRegistroId(1L)
        ).thenReturn(
                0L,
                1L
        );

        AtomicLong generatedId =
                new AtomicLong(100L);

        when(
                gestionLlamadaRepository.save(
                        any(CallCenterGestionLlamada.class)
                )
        ).thenAnswer(invocation -> {
            CallCenterGestionLlamada saved =
                    invocation.getArgument(0);

            saved.setId(
                    generatedId.getAndIncrement()
            );

            return saved;
        });

        when(
                callCenterRegistroRepository.save(registro)
        ).thenReturn(registro);

        CallCenterGestionLlamadaRequest connectedRequest =
                new CallCenterGestionLlamadaRequest(
                        LocalDate.of(2026, 7, 31),
                        null,
                        true,
                        "CONTACTADO",
                        null,
                        null,
                        null,
                        null,
                        true,
                        "Barrio El Prado, carrera 10 # 20-30",
                        LocalDate.of(2026, 8, 1),
                        true,
                        true,
                        "Ciudadano confirma la información"
                );

        service.registrarLlamada(
                1L,
                connectedRequest
        );

        assertTrue(
                registro.getLlamadaConectada()
        );

        assertTrue(
                registro.getSolicitoNuevaEncuesta()
        );

        assertEquals(
                "Barrio El Prado, carrera 10 # 20-30",
                registro.getDireccionTexto()
        );

        assertEquals(
                LocalDate.of(2026, 8, 1),
                registro.getFechaAplicacionInformada()
        );

        assertTrue(
                registro.getDisposicionRecibirEncuesta()
        );

        assertTrue(
                registro.getExplicoInformanteCalificado()
        );

        CallCenterGestionLlamadaRequest missedRequest =
                new CallCenterGestionLlamadaRequest(
                        LocalDate.of(2026, 7, 31),
                        null,
                        false,
                        "NO_CONTACTADO",
                        8L,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Segundo intento sin respuesta"
                );

        service.registrarLlamada(
                1L,
                missedRequest
        );

        assertFalse(
                registro.getLlamadaConectada()
        );

        assertSame(
                motivo,
                registro.getMotivoNoContacto()
        );

        /*
         * La llamada no conectada no elimina los datos
         * confirmados en el intento anterior.
         */
        assertTrue(
                registro.getSolicitoNuevaEncuesta()
        );

        assertEquals(
                "Barrio El Prado, carrera 10 # 20-30",
                registro.getDireccionTexto()
        );

        assertEquals(
                LocalDate.of(2026, 8, 1),
                registro.getFechaAplicacionInformada()
        );

        assertTrue(
                registro.getDisposicionRecibirEncuesta()
        );

        assertTrue(
                registro.getExplicoInformanteCalificado()
        );
    }

    @Test
    void shouldRejectMissedCallWithoutContactReason() {
        CallCenterRegistro registro =
                activeRegistro(
                        CallCenterStatePolicy
                                .ASIGNADO_CALLCENTER
                );

        CallCenterResultadoLlamada resultado =
                resultadoLlamada(
                        "NO_CONTACTADO",
                        "No contactado",
                        CallCenterStatePolicy
                                .NO_CONTACTADO
                );

        CallCenterGestionLlamadaRequest request =
                new CallCenterGestionLlamadaRequest(
                        LocalDate.of(2026, 7, 31),
                        null,
                        false,
                        "NO_CONTACTADO",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        when(
                callCenterRegistroRepository.findById(1L)
        ).thenReturn(
                Optional.of(registro)
        );

        when(
                resultadoLlamadaRepository
                        .findFirstByCodigoIgnoreCaseAndActivoTrue(
                                "NO_CONTACTADO"
                        )
        ).thenReturn(
                Optional.of(resultado)
        );

        assertThrows(
                BusinessException.class,
                () -> service.registrarLlamada(
                        1L,
                        request
                )
        );

        verify(
                gestionLlamadaRepository,
                never()
        ).save(any());
    }

    @Test
    void shouldCloseCaseAndAuditTelephoneClosure() {
        CallCenterRegistro registro =
                activeRegistro(
                        CallCenterStatePolicy
                                .ASIGNADO_CALLCENTER
                );

        CallCenterResultadoLlamada resultado =
                resultadoLlamada(
                        "CERRADO_TELEFONICAMENTE",
                        "Cerrado telefónicamente",
                        CallCenterStatePolicy.CERRADO
                );

        CallCenterGestionLlamadaRequest request =
                new CallCenterGestionLlamadaRequest(
                        LocalDate.of(2026, 7, 30),
                        null,
                        true,
                        "CERRADO_TELEFONICAMENTE",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Caso resuelto"
                );

        when(
                callCenterRegistroRepository.findById(1L)
        ).thenReturn(
                Optional.of(registro)
        );

        when(
                resultadoLlamadaRepository
                        .findFirstByCodigoIgnoreCaseAndActivoTrue(
                                "CERRADO_TELEFONICAMENTE"
                        )
        ).thenReturn(
                Optional.of(resultado)
        );

        when(
                gestionLlamadaRepository
                        .countByCallCenterRegistroId(1L)
        ).thenReturn(0L);

        when(
                gestionLlamadaRepository.save(
                        any(CallCenterGestionLlamada.class)
                )
        ).thenAnswer(invocation -> {
            CallCenterGestionLlamada saved =
                    invocation.getArgument(0);

            saved.setId(100L);

            return saved;
        });

        when(
                callCenterRegistroRepository.save(registro)
        ).thenReturn(registro);

        service.registrarLlamada(
                1L,
                request
        );

        assertEquals(
                CallCenterStatePolicy.CERRADO,
                registro.getEstadoCaso()
        );

        assertNotNull(
                registro.getFechaCierre()
        );

        assertSame(
                currentUser,
                registro.getUsuarioCierre()
        );

        assertTrue(
                registro.getMotivoCierre()
                        .contains(
                                "Cerrado telefónicamente"
                        )
        );

        verify(auditService).safeLogWithUser(
                eq(currentUser),
                eq(AuditAction.CREATE),
                eq("callcenter_gestion_llamada"),
                eq(100L),
                isNull(),
                anyMap()
        );

        verify(auditService).safeLogWithUser(
                eq(currentUser),
                eq(AuditAction.UPDATE),
                eq("callcenter_registro"),
                eq(1L),
                anyMap(),
                anyMap()
        );
    }

    @Test
    void shouldAllowVisitAssignmentAfterNoContact() {
        CallCenterRegistro registro =
                activeRegistro(
                        CallCenterStatePolicy
                                .NO_CONTACTADO
                );

        Encuestador encuestador =
                mock(Encuestador.class);

        when(encuestador.getId())
                .thenReturn(50L);

        when(encuestador.getNombre())
                .thenReturn("Encuestador de prueba");

        when(encuestador.getActivo())
                .thenReturn(true);

        CallCenterVisitaAsignacionRequest request =
                mock(
                        CallCenterVisitaAsignacionRequest.class
                );

        when(request.encuestadorId())
                .thenReturn(50L);

        when(request.fechaProgramada())
                .thenReturn(
                        LocalDate.of(2026, 8, 1)
                );

        when(
                callCenterRegistroRepository.findById(1L)
        ).thenReturn(
                Optional.of(registro)
        );

        when(
                encuestadorRepository.findById(50L)
        ).thenReturn(
                Optional.of(encuestador)
        );

        when(
                visitaRepository.save(
                        any(CallCenterVisita.class)
                )
        ).thenAnswer(invocation -> {
            CallCenterVisita saved =
                    invocation.getArgument(0);

            saved.setId(200L);

            return saved;
        });

        when(
                callCenterRegistroRepository.save(registro)
        ).thenReturn(registro);

        service.asignarVisita(
                1L,
                request
        );

        assertEquals(
                CallCenterStatePolicy.VISITA_PROGRAMADA,
                registro.getEstadoCaso()
        );

        assertEquals(
                "PROGRAMADA",
                registro.getEstadoVisita()
        );

        assertSame(
                encuestador,
                registro.getEncuestadorAsignado()
        );

        assertSame(
                encuestador,
                registro.getEncuestadorProgramado()
        );

        assertEquals(
                LocalDate.of(2026, 8, 1),
                registro.getFechaEncuestaProgramada()
        );

        verify(
                visitaRepository
        ).save(
                any(CallCenterVisita.class)
        );

        verify(
                callCenterRegistroRepository
        ).save(registro);
    }

    @Test
    void shouldCancelVisitCompleteMetadataAndAudit() {
        CallCenterRegistro registro =
                activeRegistro(
                        CallCenterStatePolicy
                                .VISITA_PROGRAMADA
                );

        registro.setEstadoVisita("PROGRAMADA");

        CallCenterVisita visita =
                new CallCenterVisita();

        visita.setId(10L);
        visita.setCallCenterRegistro(registro);
        visita.setEstadoVisita("PROGRAMADA");
        visita.setActivo(true);

        CallCenterVisitaResultadoRequest request =
                mock(
                        CallCenterVisitaResultadoRequest.class
                );

        when(request.estadoVisita())
                .thenReturn("CANCELADA");

        when(request.fechaVisitaReal())
                .thenReturn(
                        LocalDate.of(2026, 7, 30)
                );

        when(request.encuestaRealizada())
                .thenReturn(false);

        when(request.motivoNoEncuesta())
                .thenReturn(
                        "Ciudadano solicitó cancelar"
                );

        when(request.observacionEncuestador())
                .thenReturn(
                        "Cancelación confirmada"
                );

        when(
                visitaRepository.findById(10L)
        ).thenReturn(
                Optional.of(visita)
        );

        when(
                visitaRepository.save(visita)
        ).thenReturn(visita);

        when(
                callCenterRegistroRepository.save(registro)
        ).thenReturn(registro);

        service.actualizarResultadoVisita(
                10L,
                request
        );

        assertEquals(
                CallCenterStatePolicy.CANCELADO,
                registro.getEstadoCaso()
        );

        assertNotNull(
                registro.getFechaCierre()
        );

        assertSame(
                currentUser,
                registro.getUsuarioCierre()
        );

        assertTrue(
                registro.getMotivoCierre()
                        .contains(
                                "Ciudadano solicitó cancelar"
                        )
        );

        verify(auditService).safeLogWithUser(
                eq(currentUser),
                eq(AuditAction.UPDATE),
                eq("callcenter_visita"),
                eq(10L),
                anyMap(),
                anyMap()
        );

        verify(auditService).safeLogWithUser(
                eq(currentUser),
                eq(AuditAction.UPDATE),
                eq("callcenter_registro"),
                eq(1L),
                anyMap(),
                anyMap()
        );
    }

    private CallCenterRegistro activeRegistro(
            String estadoCaso
    ) {
        CallCenterRegistro registro =
                new CallCenterRegistro();

        registro.setId(1L);
        registro.setActivo(true);
        registro.setEstadoCaso(estadoCaso);
        registro.setEstadoVisita("PENDIENTE");
        registro.setFechaLlamada(
                LocalDate.of(2026, 7, 30)
        );

        return registro;
    }

    private CallCenterResultadoLlamada resultadoLlamada(
            String codigo,
            String nombre,
            String estadoCasoSugerido
    ) {
        CallCenterResultadoLlamada resultado =
                new CallCenterResultadoLlamada();

        resultado.setCodigo(codigo);
        resultado.setNombre(nombre);
        resultado.setEstadoCasoSugerido(
                estadoCasoSugerido
        );
        resultado.setActivo(true);

        return resultado;
    }

    private CallCenterMotivoNoContacto
    activeMotivoNoContacto(
            Long id,
            String nombre
    ) {
        CallCenterMotivoNoContacto motivo =
                mock(
                        CallCenterMotivoNoContacto.class
                );

        when(motivo.getId())
                .thenReturn(id);

        when(motivo.getNombre())
                .thenReturn(nombre);

        when(motivo.getActivo())
                .thenReturn(true);

        return motivo;
    }
}