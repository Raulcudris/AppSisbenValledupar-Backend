package com.appsisben.backend.modules.callcenter.application;

import com.appsisben.backend.modules.audit.application.AuditService;
import com.appsisben.backend.modules.callcenter.domain.CallCenterRegistro;
import com.appsisben.backend.modules.callcenter.dto.CallCenterAsignarEncuestadorRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterAsignarFuncionarioRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterVisitaRequest;
import com.appsisben.backend.modules.callcenter.repository.CallCenterMotivoNoContactoRepository;
import com.appsisben.backend.modules.callcenter.repository.CallCenterMotivoNoDisposicionRepository;
import com.appsisben.backend.modules.callcenter.repository.CallCenterRegistroRepository;
import com.appsisben.backend.modules.catalogs.domain.Encuestador;
import com.appsisben.backend.modules.catalogs.repository.EncuestadorRepository;
import com.appsisben.backend.modules.roles.domain.Role;
import com.appsisben.backend.modules.territory.repository.BarrioRepository;
import com.appsisben.backend.modules.users.domain.User;
import com.appsisben.backend.modules.users.repository.UserRepository;
import com.appsisben.backend.modules.ventanilla.repository.VentanillaRegistroRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallCenterServiceStateTest {

    @Mock
    private CallCenterRegistroRepository repository;

    @Mock
    private CallCenterMotivoNoContactoRepository
            motivoNoContactoRepository;

    @Mock
    private CallCenterMotivoNoDisposicionRepository
            motivoNoDisposicionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncuestadorRepository encuestadorRepository;

    @Mock
    private BarrioRepository barrioRepository;

    @Mock
    private VentanillaRegistroRepository
            ventanillaRegistroRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private CallCenterService service;

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
                userRepository.findByUsernameIgnoreCase(
                        "admin"
                )
        ).thenReturn(
                Optional.of(currentUser)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectStateChangeFromGeneralUpdate() {
        CallCenterRegistro registro =
                activeRegistro(
                        CallCenterStatePolicy
                                .ASIGNADO_CALLCENTER
                );

        when(repository.findById(1L))
                .thenReturn(Optional.of(registro));

        CallCenterRequest request =
                emptyGeneralUpdateRequest();

        when(request.estadoCaso())
                .thenReturn(
                        CallCenterStatePolicy
                                .NO_CONTACTADO
                );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.update(
                                1L,
                                request
                        )
                );

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("estado")
        );

        assertEquals(
                CallCenterStatePolicy
                        .ASIGNADO_CALLCENTER,
                registro.getEstadoCaso()
        );

        verify(
                auditService,
                never()
        ).safeLog(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void shouldRejectFuncionarioAssignmentAfterTelephoneStageStarted() {
        User funcionario =
                activeFuncionarioCallcenter();

        CallCenterRegistro registro =
                activeRegistro(
                        CallCenterStatePolicy
                                .NO_CONTACTADO
                );

        CallCenterAsignarFuncionarioRequest request =
                mock(
                        CallCenterAsignarFuncionarioRequest.class
                );

        when(request.funcionarioCallcenterId())
                .thenReturn(20L);

        when(request.registroIds())
                .thenReturn(List.of(1L));

        when(userRepository.findById(20L))
                .thenReturn(Optional.of(funcionario));

        when(repository.findAllById(List.of(1L)))
                .thenReturn(List.of(registro));

        assertThrows(
                BusinessException.class,
                () -> service
                        .asignarFuncionarioCallcenter(
                                request
                        )
        );

        verify(
                auditService,
                never()
        ).safeLog(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void shouldAdvanceToProgrammedVisitWhenAssigningEncuestador() {
        Encuestador encuestador =
                new Encuestador();

        encuestador.setId(30L);
        encuestador.setNombre(
                "Encuestador activo"
        );
        encuestador.setActivo(true);

        CallCenterRegistro registro =
                activeRegistro(
                        CallCenterStatePolicy
                                .PENDIENTE_ASIGNAR_ENCUESTADOR
                );

        CallCenterAsignarEncuestadorRequest request =
                mock(
                        CallCenterAsignarEncuestadorRequest.class
                );

        when(request.encuestadorId())
                .thenReturn(30L);

        when(request.registroIds())
                .thenReturn(List.of(1L));

        when(request.fechaEncuestaProgramada())
                .thenReturn(
                        LocalDate.of(
                                2026,
                                8,
                                1
                        )
                );

        when(encuestadorRepository.findById(30L))
                .thenReturn(
                        Optional.of(encuestador)
                );

        when(repository.findAllById(List.of(1L)))
                .thenReturn(List.of(registro));

        service.asignarEncuestador(request);

        assertEquals(
                CallCenterStatePolicy
                        .VISITA_PROGRAMADA,
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

        verify(auditService).safeLog(
                any(),
                eq("callcenter_registro"),
                eq(1L),
                any(),
                any()
        );
    }

    @Test
    void shouldCompleteFinalMetadataWhenLegacyVisitIsCancelled() {
        CallCenterRegistro registro =
                activeRegistro(
                        CallCenterStatePolicy
                                .VISITA_PROGRAMADA
                );

        when(repository.findById(1L))
                .thenReturn(Optional.of(registro));

        CallCenterVisitaRequest request =
                mock(CallCenterVisitaRequest.class);

        when(request.estadoVisita())
                .thenReturn("CANCELADA");

        when(request.fechaVisitaReal())
                .thenReturn(
                        LocalDate.of(
                                2026,
                                7,
                                30
                        )
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

        when(request.verificado())
                .thenReturn(true);

        service.updateVisita(
                1L,
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
                                "Visita cancelada"
                        )
        );
    }

    @Test
    void shouldRejectOperationalCallFieldFromGeneralUpdate() {
        CallCenterRegistro registro =
                activeRegistro(
                        CallCenterStatePolicy
                                .NO_CONTACTADO
                );

        registro.setLlamadaConectada(false);

        when(repository.findById(1L))
                .thenReturn(Optional.of(registro));

        CallCenterRequest request =
                emptyGeneralUpdateRequest();

        when(request.llamadaConectada())
                .thenReturn(true);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.update(
                                1L,
                                request
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "llamadaConectada"
                        )
        );

        assertFalse(
                registro.getLlamadaConectada()
        );

        verify(
                auditService,
                never()
        ).safeLog(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void shouldRejectEncuestadorChangeFromGeneralUpdate() {
        CallCenterRegistro registro =
                activeRegistro(
                        CallCenterStatePolicy
                                .PENDIENTE_ASIGNAR_ENCUESTADOR
                );

        when(repository.findById(1L))
                .thenReturn(Optional.of(registro));

        CallCenterRequest request =
                emptyGeneralUpdateRequest();

        when(request.encuestadorAsignadoId())
                .thenReturn(30L);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.update(
                                1L,
                                request
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "encuestadorAsignadoId"
                        )
        );

        assertNull(
                registro.getEncuestadorAsignado()
        );

        verify(
                encuestadorRepository,
                never()
        ).findById(any());

        verify(
                auditService,
                never()
        ).safeLog(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void shouldPreserveOperationalFieldsWhenGeneralUpdateOmitsThem() {
        CallCenterRegistro registro =
                activeRegistro(
                        CallCenterStatePolicy
                                .NO_CONTACTADO
                );

        registro.setLlamadaConectada(false);

        registro.setObservacion(
                "Último intento sin contacto"
        );

        registro.setVerificado(true);
        registro.setSolicitoNuevaEncuesta(false);

        when(repository.findById(1L))
                .thenReturn(Optional.of(registro));

        CallCenterRequest request =
                emptyGeneralUpdateRequest();

        service.update(
                1L,
                request
        );

        assertFalse(
                registro.getLlamadaConectada()
        );

        assertEquals(
                "Último intento sin contacto",
                registro.getObservacion()
        );

        assertTrue(
                registro.getVerificado()
        );

        assertFalse(
                registro.getSolicitoNuevaEncuesta()
        );

        assertEquals(
                CallCenterStatePolicy.NO_CONTACTADO,
                registro.getEstadoCaso()
        );

        verify(auditService).safeLog(
                any(),
                eq("callcenter_registro"),
                eq(1L),
                any(),
                any()
        );
    }

    /**
     * Crea un request general sin campos operativos.
     *
     * Mockito devuelve 0 o false para algunos wrappers cuando
     * no se configuran explícitamente. En una petición JSON real,
     * un campo omitido se deserializa como null.
     */
    private CallCenterRequest emptyGeneralUpdateRequest() {
        CallCenterRequest request =
                mock(CallCenterRequest.class);

        lenient()
                .when(request.ventanillaRegistroId())
                .thenReturn((Long) null);

        lenient()
                .when(request.motivoNoContactoId())
                .thenReturn((Long) null);

        lenient()
                .when(request.encuestadorProgramadoId())
                .thenReturn((Long) null);

        lenient()
                .when(request.barrioId())
                .thenReturn((Long) null);

        lenient()
                .when(request.motivoNoDisposicionId())
                .thenReturn((Long) null);

        lenient()
                .when(request.encuestadorAsignadoId())
                .thenReturn((Long) null);

        lenient()
                .when(request.llamadaConectada())
                .thenReturn((Boolean) null);

        lenient()
                .when(request.solicitoNuevaEncuesta())
                .thenReturn((Boolean) null);

        lenient()
                .when(request.disposicionRecibirEncuesta())
                .thenReturn((Boolean) null);

        lenient()
                .when(request.explicoInformanteCalificado())
                .thenReturn((Boolean) null);

        lenient()
                .when(request.verificado())
                .thenReturn((Boolean) null);

        lenient()
                .when(request.activo())
                .thenReturn((Boolean) null);

        return request;
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
                LocalDate.of(
                        2026,
                        7,
                        30
                )
        );

        return registro;
    }

    private User activeFuncionarioCallcenter() {
        Role role =
                new Role();

        role.setId(10L);
        role.setCodigo(
                "FUNCIONARIO_CALLCENTER"
        );
        role.setNombre(
                "Funcionario Call Center"
        );
        role.setActivo(true);

        User user =
                new User();

        user.setId(20L);
        user.setUsername(
                "funcionario.callcenter"
        );
        user.setNombres(
                "Funcionario"
        );
        user.setActivo(true);
        user.setRole(role);

        return user;
    }
}