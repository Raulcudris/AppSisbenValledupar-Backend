package com.appsisben.backend.modules.callcenter.application;

import com.appsisben.backend.modules.audit.application.AuditService;
import com.appsisben.backend.modules.audit.domain.AuditAction;
import com.appsisben.backend.modules.callcenter.domain.CallCenterMotivoNoContacto;
import com.appsisben.backend.modules.callcenter.domain.CallCenterMotivoNoDisposicion;
import com.appsisben.backend.modules.callcenter.domain.CallCenterRegistro;
import com.appsisben.backend.modules.callcenter.dto.CallCenterAsignarEncuestadorRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterAsignarFuncionarioRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterCatalogResponse;
import com.appsisben.backend.modules.callcenter.dto.CallCenterFilterRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterResponse;
import com.appsisben.backend.modules.callcenter.dto.CallCenterSummaryResponse;
import com.appsisben.backend.modules.callcenter.dto.CallCenterUserOptionResponse;
import com.appsisben.backend.modules.callcenter.dto.CallCenterVisitaRequest;
import com.appsisben.backend.modules.callcenter.repository.CallCenterMotivoNoContactoRepository;
import com.appsisben.backend.modules.callcenter.repository.CallCenterMotivoNoDisposicionRepository;
import com.appsisben.backend.modules.callcenter.repository.CallCenterRegistroRepository;
import com.appsisben.backend.modules.callcenter.repository.CallCenterRegistroSpecification;
import com.appsisben.backend.modules.callcenter.repository.CallCenterSummaryProjection;
import com.appsisben.backend.modules.catalogs.domain.Encuestador;
import com.appsisben.backend.modules.catalogs.repository.EncuestadorRepository;
import com.appsisben.backend.modules.territory.domain.Barrio;
import com.appsisben.backend.modules.territory.repository.BarrioRepository;
import com.appsisben.backend.modules.users.domain.User;
import com.appsisben.backend.modules.users.repository.UserRepository;
import com.appsisben.backend.modules.ventanilla.domain.VentanillaRegistro;
import com.appsisben.backend.modules.ventanilla.repository.VentanillaRegistroRepository;
import com.appsisben.backend.shared.api.PageResponse;
import com.appsisben.backend.shared.exception.BusinessException;
import com.appsisben.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CallCenterService {

    private static final String TABLE_NAME =
            "callcenter_registro";

    private static final String MOTIVO_CIERRE_ENCUESTA_REALIZADA =
            "Encuesta realizada por encuestador";

    private final CallCenterRegistroRepository repository;

    private final CallCenterMotivoNoContactoRepository
            motivoNoContactoRepository;

    private final CallCenterMotivoNoDisposicionRepository
            motivoNoDisposicionRepository;

    private final UserRepository userRepository;
    private final EncuestadorRepository encuestadorRepository;
    private final BarrioRepository barrioRepository;

    private final VentanillaRegistroRepository
            ventanillaRegistroRepository;

    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<CallCenterResponse> findAll(
            Pageable pageable
    ) {
        Page<CallCenterRegistro> page =
                findPageForCurrentUser(
                        CallCenterRegistroSpecification.activeOnly(),
                        pageable
                );

        return PageResponse.from(
                page,
                page.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<CallCenterResponse> search(
            CallCenterFilterRequest filter,
            Pageable pageable
    ) {
        Page<CallCenterRegistro> page =
                findPageForCurrentUser(
                        CallCenterRegistroSpecification.byFilter(filter),
                        pageable
                );

        return PageResponse.from(
                page,
                page.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<CallCenterResponse> misAsignaciones(
            Pageable pageable
    ) {
        Specification<CallCenterRegistro> specification;

        if (currentUserHasRole("FUNCIONARIO_ENCUESTADOR")) {
            Encuestador encuestador =
                    currentEncuestador();

            specification =
                    CallCenterRegistroSpecification
                            .byEncuestadorAsignadoOrProgramado(
                                    encuestador.getId()
                            );

        } else if (
                currentUserHasRole("FUNCIONARIO_CALLCENTER")
        ) {
            User user = currentUser();

            specification =
                    CallCenterRegistroSpecification
                            .byFuncionarioCallcenterAsignado(
                                    user.getId()
                            );

        } else {
            specification =
                    CallCenterRegistroSpecification.activeOnly();
        }

        Specification<CallCenterRegistro> effectiveSpecification =
                CallCenterRegistroSpecification
                        .withResponseGraph()
                        .and(specification);

        Page<CallCenterRegistro> page =
                repository.findAll(
                        effectiveSpecification,
                        pageable
                );

        return PageResponse.from(
                page,
                page.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<CallCenterResponse>
    pendientesAsignarFuncionario(
            Pageable pageable
    ) {
        Specification<CallCenterRegistro> specification =
                CallCenterRegistroSpecification
                        .withResponseGraph()
                        .and(
                                CallCenterRegistroSpecification
                                        .pendientesAsignarFuncionarioCallcenter()
                        );

        Page<CallCenterRegistro> page =
                repository.findAll(
                        specification,
                        pageable
                );

        return PageResponse.from(
                page,
                page.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<CallCenterResponse>
    misRegistrosCallcenter(
            CallCenterFilterRequest filter,
            Pageable pageable
    ) {
        Specification<CallCenterRegistro> specification =
                isEmptyMisRegistrosFilter(filter)
                        ? CallCenterRegistroSpecification.activeOnly()
                        : CallCenterRegistroSpecification.byFilter(filter);

        if (
                currentUserHasRole(
                        "FUNCIONARIO_CALLCENTER"
                )
        ) {
            User user = currentUser();

            specification =
                    specification.and(
                            CallCenterRegistroSpecification
                                    .byFuncionarioCallcenterAsignado(
                                            user.getId()
                                    )
                    );
        }

        specification =
                CallCenterRegistroSpecification
                        .withResponseGraph()
                        .and(specification);

        Page<CallCenterRegistro> page =
                repository.findAll(
                        specification,
                        pageable
                );

        return PageResponse.from(
                page,
                page.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public CallCenterResponse findById(
            Long id
    ) {
        CallCenterRegistro entity =
                findEntity(id);

        validateCurrentFuncionarioCallcenterCanAccess(
                entity
        );

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<CallCenterCatalogResponse>
    findMotivosNoContacto() {
        return motivoNoContactoRepository
                .findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(this::toCatalogResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CallCenterCatalogResponse>
    findMotivosNoDisposicion() {
        return motivoNoDisposicionRepository
                .findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(this::toCatalogResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CallCenterUserOptionResponse>
    findFuncionariosCallcenter() {
        return userRepository
                .findByActivoTrueAndRoleActivoTrueAndRoleCodigoIgnoreCaseOrderByUsernameAsc(
                        "FUNCIONARIO_CALLCENTER"
                )
                .stream()
                .map(this::toUserOptionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CallCenterSummaryResponse summary() {
        Long funcionarioId = null;

        if (
                currentUserHasRole(
                        "FUNCIONARIO_CALLCENTER"
                )
        ) {
            funcionarioId =
                    currentUser().getId();
        }

        CallCenterSummaryProjection summary =
                repository.summarize(
                        funcionarioId
                );

        if (summary == null) {
            return new CallCenterSummaryResponse(
                    0L,
                    0L,
                    0L,
                    0L,
                    0L
            );
        }

        return new CallCenterSummaryResponse(
                safeLong(summary.getTotal()),
                safeLong(summary.getConectadas()),
                safeLong(summary.getNoConectadas()),
                safeLong(summary.getActivos()),
                safeLong(summary.getInactivos())
        );
    }

    @Transactional
    public CallCenterResponse create(
            CallCenterRequest request
    ) {
        CallCenterRegistro entity =
                new CallCenterRegistro();

        User user = currentUser();

        entity.setFuncionario(user);
        entity.setCreadoPor(user);
        entity.setActivo(true);
        entity.setTipoRegistro("LLAMADA");
        entity.setOrigenRegistro("MANUAL");
        entity.setEstadoVisita("PENDIENTE");

        entity.setEstadoCaso(
                CallCenterStatePolicy
                        .PENDIENTE_ENRUTAMIENTO
        );

        entity.setTipoSolicitudCallcenter(
                "NUEVA_ENCUESTA"
        );

        apply(
                entity,
                request,
                user,
                true
        );

        if (
                currentUserHasRole(
                        "FUNCIONARIO_CALLCENTER"
                )
        ) {
            entity.setFuncionarioCallcenterAsignado(
                    user
            );

            entity.setFechaAsignacionCallcenter(
                    LocalDateTime.now()
            );

            entity.setUsuarioAsignaCallcenter(
                    user
            );

            entity.setEstadoCaso(
                    CallCenterStatePolicy
                            .ASIGNADO_CALLCENTER
            );
        }

        CallCenterRegistro saved =
                repository.save(entity);

        auditService.safeLog(
                AuditAction.CREATE,
                TABLE_NAME,
                saved.getId(),
                null,
                snapshot(saved)
        );

        return toResponse(saved);
    }

    @Transactional
    public CallCenterResponse update(
            Long id,
            CallCenterRequest request
    ) {
        CallCenterRegistro entity =
                findEntity(id);

        validateCurrentFuncionarioCallcenterCanAccess(
                entity
        );

        validateCaseIsOpen(
                entity,
                "No se puede actualizar un caso cerrado o cancelado"
        );

        validateProtectedUpdateFields(
                entity,
                request
        );

        Map<String, Object> before =
                snapshot(entity);

        apply(
                entity,
                request,
                currentUser(),
                false
        );

        auditService.safeLog(
                AuditAction.UPDATE,
                TABLE_NAME,
                entity.getId(),
                before,
                snapshot(entity)
        );

        return toResponse(entity);
    }

    @Transactional
    public List<CallCenterResponse>
    asignarFuncionarioCallcenter(
            CallCenterAsignarFuncionarioRequest request
    ) {
        User funcionario =
                findActiveFuncionarioCallcenter(
                        request.funcionarioCallcenterId()
                );

        User user =
                currentUser();

        List<CallCenterRegistro> registros =
                repository.findAllById(
                        request.registroIds()
                );

        if (
                registros.size()
                        != request.registroIds().size()
        ) {
            throw new BusinessException(
                    "Uno o más registros no existen"
            );
        }

        for (
                CallCenterRegistro registro
                : registros
        ) {
            validateRegistroAsignableAFuncionarioCallcenter(
                    registro,
                    funcionario
            );

            Map<String, Object> before =
                    snapshot(registro);

            String targetState =
                    CallCenterStatePolicy
                            .validateFuncionarioAssignment(
                                    registro.getEstadoCaso()
                            );

            registro.setFuncionarioCallcenterAsignado(
                    funcionario
            );

            registro.setFechaAsignacionCallcenter(
                    LocalDateTime.now()
            );

            registro.setUsuarioAsignaCallcenter(
                    user
            );

            registro.setEstadoCaso(
                    targetState
            );

            registro.setActualizadoPor(
                    user
            );

            auditService.safeLog(
                    AuditAction.UPDATE,
                    TABLE_NAME,
                    registro.getId(),
                    before,
                    snapshot(registro)
            );
        }

        return registros
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<CallCenterResponse>
    asignarEncuestador(
            CallCenterAsignarEncuestadorRequest request
    ) {
        Encuestador encuestador =
                findRequiredActiveEncuestador(
                        request.encuestadorId()
                );

        User user =
                currentUser();

        List<CallCenterRegistro> registros =
                repository.findAllById(
                        request.registroIds()
                );

        if (
                registros.size()
                        != request.registroIds().size()
        ) {
            throw new BusinessException(
                    "Uno o más registros no existen"
            );
        }

        for (
                CallCenterRegistro registro
                : registros
        ) {
            validateRegistroAsignableAEncuestador(
                    registro,
                    encuestador,
                    user
            );

            Map<String, Object> before =
                    snapshot(registro);

            boolean programmed =
                    request.fechaEncuestaProgramada()
                            != null;

            String targetState =
                    CallCenterStatePolicy
                            .validateEncuestadorAssignment(
                                    registro.getEstadoCaso(),
                                    programmed
                            );

            registro.setEncuestadorAsignado(
                    encuestador
            );

            registro.setEncuestadorProgramado(
                    encuestador
            );

            registro.setFechaEncuestaProgramada(
                    request.fechaEncuestaProgramada()
            );

            registro.setEstadoVisita(
                    programmed
                            ? "PROGRAMADA"
                            : "PENDIENTE"
            );

            registro.setEstadoCaso(
                    targetState
            );

            registro.setActualizadoPor(
                    user
            );

            auditService.safeLog(
                    AuditAction.UPDATE,
                    TABLE_NAME,
                    registro.getId(),
                    before,
                    snapshot(registro)
            );
        }

        return registros
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CallCenterResponse updateVisita(
            Long id,
            CallCenterVisitaRequest request
    ) {
        CallCenterRegistro entity =
                findEntity(id);

        User user =
                currentUser();

        validateCurrentFuncionarioCallcenterCanAccess(
                entity
        );

        CallCenterStatePolicy
                .validateCanUpdateVisit(
                        entity.getEstadoCaso()
                );

        if (
                currentUserHasRole(
                        "FUNCIONARIO_ENCUESTADOR"
                )
        ) {
            Encuestador encuestador =
                    currentEncuestador();

            boolean assigned =
                    entity.getEncuestadorAsignado()
                            != null
                            && encuestador.getId()
                            .equals(
                                    entity
                                            .getEncuestadorAsignado()
                                            .getId()
                            );

            boolean programmed =
                    entity.getEncuestadorProgramado()
                            != null
                            && encuestador.getId()
                            .equals(
                                    entity
                                            .getEncuestadorProgramado()
                                            .getId()
                            );

            if (!assigned && !programmed) {
                throw new BusinessException(
                        "El registro no está asignado al "
                                + "encuestador autenticado"
                );
            }
        }

        Map<String, Object> before =
                snapshot(entity);

        String estadoVisita =
                cleanEstadoVisita(
                        request.estadoVisita()
                );

        entity.setEstadoVisita(
                estadoVisita
        );

        entity.setFechaVisitaReal(
                request.fechaVisitaReal()
        );

        entity.setHoraVisitaReal(
                request.horaVisitaReal()
        );

        entity.setEncuestaRealizada(
                request.encuestaRealizada()
        );

        entity.setMotivoNoEncuesta(
                clean(
                        request.motivoNoEncuesta()
                )
        );

        entity.setFechaReprogramacion(
                request.fechaReprogramacion()
        );

        entity.setObservacionEncuestador(
                clean(
                        request.observacionEncuestador()
                )
        );

        if (request.verificado() != null) {
            entity.setVerificado(
                    request.verificado()
            );
        }

        String targetState =
                CallCenterStatePolicy
                        .resolveStateFromVisit(
                                estadoVisita,
                                request.encuestaRealizada()
                        );

        targetState =
                CallCenterStatePolicy
                        .validateVisitTransition(
                                entity.getEstadoCaso(),
                                targetState
                        );

        entity.setEstadoCaso(
                targetState
        );

        applyFinalStateMetadata(
                entity,
                targetState,
                user,
                request.motivoNoEncuesta()
        );

        entity.setActualizadoPor(
                user
        );

        auditService.safeLog(
                AuditAction.UPDATE,
                TABLE_NAME,
                entity.getId(),
                before,
                snapshot(entity)
        );

        return toResponse(entity);
    }

    @Transactional
    public CallCenterResponse activate(
            Long id
    ) {
        CallCenterRegistro entity =
                findEntity(id);

        validateCurrentFuncionarioCallcenterCanAccess(
                entity
        );

        Map<String, Object> before =
                snapshot(entity);

        entity.setActivo(true);

        entity.setActualizadoPor(
                currentUser()
        );

        auditService.safeLog(
                AuditAction.ACTIVATE,
                TABLE_NAME,
                entity.getId(),
                before,
                snapshot(entity)
        );

        return toResponse(entity);
    }

    @Transactional
    public CallCenterResponse deactivate(
            Long id
    ) {
        CallCenterRegistro entity =
                findEntity(id);

        validateCurrentFuncionarioCallcenterCanAccess(
                entity
        );

        Map<String, Object> before =
                snapshot(entity);

        entity.setActivo(false);

        entity.setActualizadoPor(
                currentUser()
        );

        auditService.safeLog(
                AuditAction.DEACTIVATE,
                TABLE_NAME,
                entity.getId(),
                before,
                snapshot(entity)
        );

        return toResponse(entity);
    }

    @Transactional
    public void delete(
            Long id
    ) {
        deactivate(id);
    }

    private void apply(
            CallCenterRegistro entity,
            CallCenterRequest request,
            User user,
            boolean includeOperationalFields
    ) {
        validateRequest(
                entity,
                request,
                includeOperationalFields
        );

        CallCenterStatePolicy
                .validateGeneralRequestState(
                        entity.getEstadoCaso(),
                        request.estadoCaso()
                );

        VentanillaRegistro ventanillaRegistro =
                resolveVentanillaRegistro(
                        request.ventanillaRegistroId(),
                        entity.getVentanillaRegistro()
                );

        String origenRegistro =
                normalizeOrigenRegistro(
                        request.origenRegistro(),
                        ventanillaRegistro
                );

        /*
         * Campos generales permitidos tanto en creación
         * como en actualización general.
         */
        entity.setMarcaTemporal(
                request.marcaTemporal()
        );

        entity.setTipoRegistro(
                normalizeTipoRegistro(
                        request.tipoRegistro()
                )
        );

        entity.setOrigenRegistro(
                origenRegistro
        );

        entity.setVentanillaRegistro(
                ventanillaRegistro
        );

        entity.setCedulaSolicitante(
                clean(
                        request.cedulaSolicitante()
                )
        );

        entity.setNombreCompleto(
                upper(
                        request.nombreCompleto()
                )
        );

        entity.setTelefono(
                clean(
                        request.telefono()
                )
        );

        entity.setTipoSolicitudCallcenter(
                normalizeTipoSolicitudCallcenter(
                        request.tipoSolicitudCallcenter(),
                        includeOperationalFields
                                ? request.solicitoNuevaEncuesta()
                                : entity.getSolicitoNuevaEncuesta()
                )
        );

        entity.setDireccionTexto(
                clean(
                        request.direccionTexto()
                )
        );

        entity.setBarrio(
                resolveBarrio(
                        request.barrioId(),
                        entity.getBarrio()
                )
        );

        entity.setFechaAplicacionInformada(
                request.fechaAplicacionInformada()
        );

        entity.setActualizadoPor(
                user
        );

        /*
         * Estos campos solo se incorporan durante la creación.
         *
         * En una actualización general se conservan los valores
         * existentes, porque pertenecen al flujo formal de
         * llamadas, asignaciones y visitas.
         */
        if (includeOperationalFields) {
            entity.setFechaLlamada(
                    request.fechaLlamada()
            );

            entity.setHoraLlamada(
                    request.horaLlamada()
            );

            entity.setLlamadaConectada(
                    request.llamadaConectada()
            );

            entity.setObservacion(
                    clean(
                            request.observacion()
                    )
            );

            entity.setVerificado(
                    request.verificado()
            );

            if (
                    !hasText(
                            entity.getEstadoVisita()
                    )
            ) {
                entity.setEstadoVisita(
                        "PENDIENTE"
                );
            }

            if (request.activo() != null) {
                entity.setActivo(
                        request.activo()
                );
            }

            entity.setMotivoNoContacto(
                    resolveMotivoNoContacto(
                            request.motivoNoContactoId(),
                            entity.getMotivoNoContacto()
                    )
            );

            entity.setMotivoNoContactoTexto(
                    clean(
                            request.motivoNoContactoTexto()
                    )
            );

            entity.setEncuestadorProgramado(
                    resolveEncuestador(
                            request.encuestadorProgramadoId(),
                            entity.getEncuestadorProgramado()
                    )
            );

            entity.setFechaEncuestaProgramada(
                    request.fechaEncuestaProgramada()
            );

            entity.setSolicitoNuevaEncuesta(
                    request.solicitoNuevaEncuesta()
            );

            entity.setDisposicionRecibirEncuesta(
                    request.disposicionRecibirEncuesta()
            );

            entity.setMotivoNoDisposicion(
                    resolveMotivoNoDisposicion(
                            request.motivoNoDisposicionId(),
                            entity.getMotivoNoDisposicion()
                    )
            );

            entity.setMotivoNoDisposicionTexto(
                    clean(
                            request.motivoNoDisposicionTexto()
                    )
            );

            entity.setEncuestadorAsignado(
                    resolveEncuestador(
                            request.encuestadorAsignadoId(),
                            entity.getEncuestadorAsignado()
                    )
            );

            entity.setExplicoInformanteCalificado(
                    request.explicoInformanteCalificado()
            );
        }
    }

    private void validateRequest(
            CallCenterRegistro entity,
            CallCenterRequest request,
            boolean includeOperationalFields
    ) {
        if (
                request.ventanillaRegistroId()
                        != null
                        && hasText(
                        request.origenRegistro()
                )
                        && !"VENTANILLA".equals(
                        request.origenRegistro()
                                .trim()
                                .toUpperCase(
                                        Locale.ROOT
                                )
                )
        ) {
            throw new BusinessException(
                    "Si se relaciona un registro de ventanilla, "
                            + "el origen debe ser VENTANILLA"
            );
        }

        if (
                "VENTANILLA".equalsIgnoreCase(
                        clean(
                                request.origenRegistro()
                        )
                )
                        && request.ventanillaRegistroId()
                        == null
        ) {
            throw new BusinessException(
                    "Debe seleccionar un registro de ventanilla "
                            + "para el origen VENTANILLA"
            );
        }

        /*
         * Las reglas siguientes solo aplican cuando los
         * campos operativos realmente serán incorporados.
         */
        if (!includeOperationalFields) {
            return;
        }

        if (
                Boolean.FALSE.equals(
                        request.llamadaConectada()
                )
        ) {
            boolean hasMotivo =
                    request.motivoNoContactoId()
                            != null
                            || hasText(
                            request.motivoNoContactoTexto()
                    );

            if (!hasMotivo) {
                throw new BusinessException(
                        "Debe registrar el motivo por el cual "
                                + "no se logró conectar la llamada"
                );
            }
        }

        if (
                Boolean.TRUE.equals(
                        request.llamadaConectada()
                )
                        && Boolean.FALSE.equals(
                        request.disposicionRecibirEncuesta()
                )
        ) {
            boolean hasMotivo =
                    request.motivoNoDisposicionId()
                            != null
                            || hasText(
                            request.motivoNoDisposicionTexto()
                    );

            if (!hasMotivo) {
                throw new BusinessException(
                        "Debe registrar el motivo por el cual "
                                + "no se confirmó la disposición"
                );
            }
        }

        validateAsignacionNuevaEncuestaPendiente(
                entity,
                request
        );
    }

    private void validateProtectedUpdateFields(
            CallCenterRegistro entity,
            CallCenterRequest request
    ) {
        validateProtectedValue(
                "fechaLlamada",
                request.fechaLlamada(),
                entity.getFechaLlamada()
        );

        validateProtectedValue(
                "horaLlamada",
                request.horaLlamada(),
                entity.getHoraLlamada()
        );

        validateProtectedValue(
                "llamadaConectada",
                request.llamadaConectada(),
                entity.getLlamadaConectada()
        );

        validateProtectedValue(
                "motivoNoContactoId",
                request.motivoNoContactoId(),
                entity.getMotivoNoContacto() != null
                        ? entity.getMotivoNoContacto()
                        .getId()
                        : null
        );

        validateProtectedText(
                "motivoNoContactoTexto",
                request.motivoNoContactoTexto(),
                entity.getMotivoNoContactoTexto()
        );

        validateProtectedValue(
                "encuestadorProgramadoId",
                request.encuestadorProgramadoId(),
                entity.getEncuestadorProgramado() != null
                        ? entity.getEncuestadorProgramado()
                        .getId()
                        : null
        );

        validateProtectedValue(
                "fechaEncuestaProgramada",
                request.fechaEncuestaProgramada(),
                entity.getFechaEncuestaProgramada()
        );

        validateProtectedValue(
                "solicitoNuevaEncuesta",
                request.solicitoNuevaEncuesta(),
                entity.getSolicitoNuevaEncuesta()
        );

        validateProtectedValue(
                "disposicionRecibirEncuesta",
                request.disposicionRecibirEncuesta(),
                entity.getDisposicionRecibirEncuesta()
        );

        validateProtectedValue(
                "motivoNoDisposicionId",
                request.motivoNoDisposicionId(),
                entity.getMotivoNoDisposicion() != null
                        ? entity.getMotivoNoDisposicion()
                        .getId()
                        : null
        );

        validateProtectedText(
                "motivoNoDisposicionTexto",
                request.motivoNoDisposicionTexto(),
                entity.getMotivoNoDisposicionTexto()
        );

        validateProtectedValue(
                "encuestadorAsignadoId",
                request.encuestadorAsignadoId(),
                entity.getEncuestadorAsignado() != null
                        ? entity.getEncuestadorAsignado()
                        .getId()
                        : null
        );

        validateProtectedValue(
                "explicoInformanteCalificado",
                request.explicoInformanteCalificado(),
                entity.getExplicoInformanteCalificado()
        );

        validateProtectedValue(
                "verificado",
                request.verificado(),
                entity.getVerificado()
        );

        validateProtectedText(
                "observacion",
                request.observacion(),
                entity.getObservacion()
        );

        validateProtectedValue(
                "activo",
                request.activo(),
                entity.getActivo()
        );
    }

    private void validateProtectedValue(
            String fieldName,
            Object requestedValue,
            Object currentValue
    ) {
        /*
         * Nulo significa que el campo fue omitido.
         */
        if (requestedValue == null) {
            return;
        }

        if (
                !Objects.equals(
                        requestedValue,
                        currentValue
                )
        ) {
            throwProtectedFieldException(
                    fieldName
            );
        }
    }

    private void validateProtectedText(
            String fieldName,
            String requestedValue,
            String currentValue
    ) {
        /*
         * Un texto nulo o vacío se interpreta como omitido.
         */
        if (!hasText(requestedValue)) {
            return;
        }

        String requested =
                clean(requestedValue);

        String current =
                clean(currentValue);

        if (
                !Objects.equals(
                        requested,
                        current
                )
        ) {
            throwProtectedFieldException(
                    fieldName
            );
        }
    }

    private void throwProtectedFieldException(
            String fieldName
    ) {
        throw new BusinessException(
                "El campo operativo '"
                        + fieldName
                        + "' no puede modificarse desde "
                        + "la actualización general del caso"
        );
    }

    private void validateAsignacionNuevaEncuestaPendiente(
            CallCenterRegistro entity,
            CallCenterRequest request
    ) {
        if (
                !Boolean.TRUE.equals(
                        request.solicitoNuevaEncuesta()
                )
        ) {
            return;
        }

        boolean hasEncuestador =
                request.encuestadorAsignadoId()
                        != null
                        || request.encuestadorProgramadoId()
                        != null;

        if (!hasEncuestador) {
            return;
        }

        validateCambioEncuestadorEnRegistroPendiente(
                entity,
                request
        );

        String cedula =
                clean(
                        request.cedulaSolicitante()
                );

        Long ventanillaRegistroId =
                request.ventanillaRegistroId();

        if (
                !hasText(cedula)
                        && ventanillaRegistroId
                        == null
        ) {
            return;
        }

        List<CallCenterRegistro> pendientes =
                repository
                        .findAsignacionNuevaEncuestaPendiente(
                                entity.getId(),
                                ventanillaRegistroId,
                                cedula
                        );

        if (pendientes.isEmpty()) {
            return;
        }

        CallCenterRegistro pendiente =
                pendientes.get(0);

        throw new BusinessException(
                "Este usuario ya tiene una nueva encuesta "
                        + "pendiente asignada al encuestador "
                        + getNombreEncuestadorAsignado(
                        pendiente
                )
                        + ". Mientras la encuesta no esté marcada "
                        + "como realizada, no puede asignarse "
                        + "a otro encuestador."
        );
    }

    private void validateCambioEncuestadorEnRegistroPendiente(
            CallCenterRegistro entity,
            CallCenterRequest request
    ) {
        if (entity.getId() == null) {
            return;
        }

        if (
                !Boolean.TRUE.equals(
                        entity.getSolicitoNuevaEncuesta()
                )
        ) {
            return;
        }

        if (
                Boolean.TRUE.equals(
                        entity.getEncuestaRealizada()
                )
        ) {
            return;
        }

        boolean currentHasEncuestador =
                entity.getEncuestadorAsignado()
                        != null
                        || entity.getEncuestadorProgramado()
                        != null;

        if (!currentHasEncuestador) {
            return;
        }

        boolean changedAsignado =
                request.encuestadorAsignadoId()
                        != null
                        && entity.getEncuestadorAsignado()
                        != null
                        && !request.encuestadorAsignadoId()
                        .equals(
                                entity
                                        .getEncuestadorAsignado()
                                        .getId()
                        );

        boolean changedProgramado =
                request.encuestadorProgramadoId()
                        != null
                        && entity.getEncuestadorProgramado()
                        != null
                        && !request.encuestadorProgramadoId()
                        .equals(
                                entity
                                        .getEncuestadorProgramado()
                                        .getId()
                        );

        boolean assigningNewWhenOnlyProgrammedExists =
                request.encuestadorAsignadoId()
                        != null
                        && entity.getEncuestadorAsignado()
                        == null
                        && entity.getEncuestadorProgramado()
                        != null
                        && !request.encuestadorAsignadoId()
                        .equals(
                                entity
                                        .getEncuestadorProgramado()
                                        .getId()
                        );

        boolean programmingNewWhenOnlyAssignedExists =
                request.encuestadorProgramadoId()
                        != null
                        && entity.getEncuestadorProgramado()
                        == null
                        && entity.getEncuestadorAsignado()
                        != null
                        && !request.encuestadorProgramadoId()
                        .equals(
                                entity
                                        .getEncuestadorAsignado()
                                        .getId()
                        );

        if (
                changedAsignado
                        || changedProgramado
                        || assigningNewWhenOnlyProgrammedExists
                        || programmingNewWhenOnlyAssignedExists
        ) {
            throw new BusinessException(
                    "Este usuario ya tiene una nueva encuesta "
                            + "pendiente asignada al encuestador "
                            + getNombreEncuestadorAsignado(
                            entity
                    )
                            + ". Mientras la encuesta no esté marcada "
                            + "como realizada, no puede asignarse "
                            + "a otro encuestador."
            );
        }
    }

    private void
    validateRegistroAsignableAFuncionarioCallcenter(
            CallCenterRegistro registro,
            User funcionario
    ) {
        CallCenterStatePolicy
                .validateFuncionarioAssignment(
                        registro.getEstadoCaso()
                );

        if (
                !Boolean.TRUE.equals(
                        registro.getActivo()
                )
        ) {
            throw new BusinessException(
                    "Solo se pueden asignar registros activos"
            );
        }

        if (
                registro
                        .getFuncionarioCallcenterAsignado()
                        != null
                        && !funcionario.getId()
                        .equals(
                                registro
                                        .getFuncionarioCallcenterAsignado()
                                        .getId()
                        )
        ) {
            throw new BusinessException(
                    "El registro "
                            + registro.getId()
                            + " ya está asignado a otro "
                            + "funcionario Call Center"
            );
        }
    }

    private void validateRegistroAsignableAEncuestador(
            CallCenterRegistro registro,
            Encuestador encuestador,
            User user
    ) {
        CallCenterStatePolicy
                .validateEncuestadorAssignment(
                        registro.getEstadoCaso(),
                        false
                );

        if (
                currentUserHasRole(
                        "FUNCIONARIO_CALLCENTER"
                )
                        && (
                        registro
                                .getFuncionarioCallcenterAsignado()
                                == null
                                || !user.getId()
                                .equals(
                                        registro
                                                .getFuncionarioCallcenterAsignado()
                                                .getId()
                                )
                )
        ) {
            throw new BusinessException(
                    "El registro "
                            + registro.getId()
                            + " no está asignado al funcionario "
                            + "Call Center autenticado"
            );
        }

        if (
                !Boolean.TRUE.equals(
                        registro.getActivo()
                )
        ) {
            throw new BusinessException(
                    "Solo se pueden asignar registros activos"
            );
        }

        if (
                Boolean.TRUE.equals(
                        registro.getEncuestaRealizada()
                )
        ) {
            throw new BusinessException(
                    "No se puede asignar encuestador a "
                            + "una encuesta ya realizada"
            );
        }

        if (
                registro.getEncuestadorAsignado()
                        != null
                        && !encuestador.getId()
                        .equals(
                                registro
                                        .getEncuestadorAsignado()
                                        .getId()
                        )
        ) {
            throw new BusinessException(
                    "El registro "
                            + registro.getId()
                            + " ya está asignado a otro encuestador"
            );
        }
    }

    private void applyFinalStateMetadata(
            CallCenterRegistro registro,
            String targetState,
            User user,
            String motivoNoEncuesta
    ) {
        if (
                !CallCenterStatePolicy
                        .isFinalState(
                                targetState
                        )
        ) {
            return;
        }

        registro.setFechaCierre(
                LocalDateTime.now()
        );

        registro.setUsuarioCierre(
                user
        );

        if (
                CallCenterStatePolicy
                        .isClosedState(
                                targetState
                        )
        ) {
            registro.setMotivoCierre(
                    MOTIVO_CIERRE_ENCUESTA_REALIZADA
            );

            return;
        }

        String motivo =
                clean(
                        motivoNoEncuesta
                );

        registro.setMotivoCierre(
                motivo != null
                        ? limitLength(
                        "Visita cancelada: "
                                + motivo,
                        500
                )
                        : "Visita cancelada"
        );
    }

    private boolean isEmptyMisRegistrosFilter(
            CallCenterFilterRequest filter
    ) {
        if (filter == null) {
            return true;
        }

        return !hasText(filter.q())
                && !hasUsefulFilterValue(
                filter.estadoCaso(),
                "TODOS"
        )
                && !hasUsefulFilterValue(
                filter.tipoSolicitudCallcenter(),
                "TODOS"
        )
                && !hasUsefulFilterValue(
                filter.condicion(),
                "TODOS"
        );
    }

    private boolean hasUsefulFilterValue(
            String value,
            String ignoredValue
    ) {
        return hasText(value)
                && !ignoredValue.equalsIgnoreCase(
                value.trim()
        );
    }

    private Page<CallCenterRegistro>
    findPageForCurrentUser(
            Specification<CallCenterRegistro> specification,
            Pageable pageable
    ) {
        Specification<CallCenterRegistro>
                effectiveSpecification =
                CallCenterRegistroSpecification
                        .withResponseGraph()
                        .and(specification);

        if (
                currentUserHasRole(
                        "FUNCIONARIO_CALLCENTER"
                )
        ) {
            User user =
                    currentUser();

            effectiveSpecification =
                    effectiveSpecification.and(
                            CallCenterRegistroSpecification
                                    .byFuncionarioCallcenterAsignado(
                                            user.getId()
                                    )
                    );
        }

        return repository.findAll(
                effectiveSpecification,
                pageable
        );
    }

    private void
    validateCurrentFuncionarioCallcenterCanAccess(
            CallCenterRegistro registro
    ) {
        if (
                !currentUserHasRole(
                        "FUNCIONARIO_CALLCENTER"
                )
        ) {
            return;
        }

        User user =
                currentUser();

        boolean assignedToCurrentUser =
                registro
                        .getFuncionarioCallcenterAsignado()
                        != null
                        && user.getId().equals(
                        registro
                                .getFuncionarioCallcenterAsignado()
                                .getId()
                );

        if (!assignedToCurrentUser) {
            throw new BusinessException(
                    "El registro no está asignado al "
                            + "funcionario Call Center autenticado"
            );
        }
    }

    private CallCenterRegistro findEntity(
            Long id
    ) {
        return repository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Registro Call Center no encontrado"
                        )
                );
    }

    private CallCenterMotivoNoContacto
    resolveMotivoNoContacto(
            Long id,
            CallCenterMotivoNoContacto current
    ) {
        if (id == null) {
            return null;
        }

        if (
                current != null
                        && id.equals(
                        current.getId()
                )
        ) {
            return current;
        }

        CallCenterMotivoNoContacto motivo =
                motivoNoContactoRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Motivo de no contacto no encontrado"
                                )
                        );

        if (
                !Boolean.TRUE.equals(
                        motivo.getActivo()
                )
        ) {
            throw new BusinessException(
                    "El motivo de no contacto seleccionado está inactivo"
            );
        }

        return motivo;
    }

    private CallCenterMotivoNoDisposicion
    resolveMotivoNoDisposicion(
            Long id,
            CallCenterMotivoNoDisposicion current
    ) {
        if (id == null) {
            return null;
        }

        if (
                current != null
                        && id.equals(
                        current.getId()
                )
        ) {
            return current;
        }

        CallCenterMotivoNoDisposicion motivo =
                motivoNoDisposicionRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Motivo de no disposición no encontrado"
                                )
                        );

        if (
                !Boolean.TRUE.equals(
                        motivo.getActivo()
                )
        ) {
            throw new BusinessException(
                    "El motivo de no disposición seleccionado está inactivo"
            );
        }

        return motivo;
    }

    private Encuestador resolveEncuestador(
            Long id,
            Encuestador current
    ) {
        if (id == null) {
            return null;
        }

        if (
                current != null
                        && id.equals(
                        current.getId()
                )
        ) {
            return current;
        }

        Encuestador encuestador =
                encuestadorRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Encuestador no encontrado"
                                )
                        );

        validateEncuestadorActivo(
                encuestador
        );

        return encuestador;
    }

    private Encuestador
    findRequiredActiveEncuestador(
            Long id
    ) {
        if (id == null) {
            throw new BusinessException(
                    "Debe seleccionar un encuestador"
            );
        }

        Encuestador encuestador =
                encuestadorRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Encuestador no encontrado"
                                )
                        );

        validateEncuestadorActivo(
                encuestador
        );

        return encuestador;
    }

    private void validateEncuestadorActivo(
            Encuestador encuestador
    ) {
        if (
                encuestador == null
                        || !Boolean.TRUE.equals(
                        encuestador.getActivo()
                )
        ) {
            throw new BusinessException(
                    "El encuestador seleccionado está inactivo"
            );
        }
    }

    private Barrio resolveBarrio(
            Long id,
            Barrio current
    ) {
        if (id == null) {
            return null;
        }

        if (
                current != null
                        && id.equals(
                        current.getId()
                )
        ) {
            return current;
        }

        Barrio barrio =
                barrioRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Barrio no encontrado"
                                )
                        );

        if (
                !Boolean.TRUE.equals(
                        barrio.getActivo()
                )
        ) {
            throw new BusinessException(
                    "El barrio seleccionado está inactivo"
            );
        }

        if (barrio.getComuna() == null) {
            throw new BusinessException(
                    "El barrio seleccionado no tiene una comuna asociada"
            );
        }

        if (
                !Boolean.TRUE.equals(
                        barrio.getComuna()
                                .getActivo()
                )
        ) {
            throw new BusinessException(
                    "La comuna asociada al barrio está inactiva"
            );
        }

        return barrio;
    }

    private VentanillaRegistro
    resolveVentanillaRegistro(
            Long id,
            VentanillaRegistro current
    ) {
        if (id == null) {
            return null;
        }

        if (
                current != null
                        && id.equals(
                        current.getId()
                )
        ) {
            return current;
        }

        VentanillaRegistro registro =
                ventanillaRegistroRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Registro de ventanilla no encontrado"
                                )
                        );

        if (
                !Boolean.TRUE.equals(
                        registro.getActivo()
                )
        ) {
            throw new BusinessException(
                    "El registro de ventanilla seleccionado está inactivo"
            );
        }

        return registro;
    }

    private User findUser(
            Long id
    ) {
        if (id == null) {
            throw new BusinessException(
                    "Debe seleccionar un usuario"
            );
        }

        return userRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Usuario no encontrado"
                        )
                );
    }

    private User
    findActiveFuncionarioCallcenter(
            Long id
    ) {
        User funcionario =
                findUser(id);

        if (
                !Boolean.TRUE.equals(
                        funcionario.getActivo()
                )
        ) {
            throw new BusinessException(
                    "El funcionario Call Center seleccionado está inactivo"
            );
        }

        if (funcionario.getRole() == null) {
            throw new BusinessException(
                    "El usuario seleccionado no tiene un rol asociado"
            );
        }

        if (
                !Boolean.TRUE.equals(
                        funcionario
                                .getRole()
                                .getActivo()
                )
        ) {
            throw new BusinessException(
                    "El rol del funcionario Call Center está inactivo"
            );
        }

        if (
                !"FUNCIONARIO_CALLCENTER"
                        .equalsIgnoreCase(
                                funcionario
                                        .getRole()
                                        .getCodigo()
                        )
        ) {
            throw new BusinessException(
                    "El usuario seleccionado no tiene rol "
                            + "FUNCIONARIO_CALLCENTER"
            );
        }

        return funcionario;
    }

    private User currentUser() {
        if (
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        == null
        ) {
            throw new BusinessException(
                    "No hay usuario autenticado"
            );
        }

        String username =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        if (
                username == null
                        || username.isBlank()
                        || "anonymousUser"
                        .equals(username)
        ) {
            throw new BusinessException(
                    "No hay usuario autenticado"
            );
        }

        return userRepository
                .findByUsernameIgnoreCase(
                        username
                )
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Usuario autenticado no encontrado"
                        )
                );
    }

    private Encuestador currentEncuestador() {
        User user =
                currentUser();

        if (
                !hasText(
                        user.getDocumento()
                )
        ) {
            throw new BusinessException(
                    "El usuario autenticado no tiene documento registrado"
            );
        }

        Encuestador encuestador =
                encuestadorRepository
                        .findFirstByDocumento(
                                user.getDocumento()
                        )
                        .orElseThrow(
                                () -> new BusinessException(
                                        "El usuario autenticado no está "
                                                + "vinculado a un encuestador "
                                                + "por documento"
                                )
                        );

        validateEncuestadorActivo(
                encuestador
        );

        return encuestador;
    }

    private boolean currentUserHasRole(
            String roleCode
    ) {
        if (
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        == null
        ) {
            return false;
        }

        boolean byAuthority =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getAuthorities()
                        .stream()
                        .anyMatch(
                                authority ->
                                        roleCode.equals(
                                                authority
                                                        .getAuthority()
                                        )
                                                || (
                                                "ROLE_"
                                                        + roleCode
                                        ).equals(
                                                authority
                                                        .getAuthority()
                                        )
                        );

        if (byAuthority) {
            return true;
        }

        try {
            User user =
                    currentUser();

            return user.getRole() != null
                    && roleCode.equalsIgnoreCase(
                    user.getRole()
                            .getCodigo()
            );

        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void validateCaseIsOpen(
            CallCenterRegistro registro,
            String message
    ) {
        if (
                CallCenterStatePolicy
                        .isFinalState(
                                registro.getEstadoCaso()
                        )
        ) {
            throw new BusinessException(
                    message
            );
        }
    }

    private String normalizeTipoRegistro(
            String tipoRegistro
    ) {
        String value =
                hasText(tipoRegistro)
                        ? tipoRegistro
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        )
                        : "LLAMADA";

        if (
                "LLAMADA".equals(value)
                        || "BASE_ENCUESTADOR"
                        .equals(value)
        ) {
            return value;
        }

        throw new BusinessException(
                "Tipo de registro no válido"
        );
    }

    private String normalizeOrigenRegistro(
            String origenRegistro,
            VentanillaRegistro ventanillaRegistro
    ) {
        String value =
                hasText(origenRegistro)
                        ? origenRegistro
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        )
                        : null;

        if (ventanillaRegistro != null) {
            return "VENTANILLA";
        }

        if (value == null) {
            return "MANUAL";
        }

        if (
                "VENTANILLA".equals(value)
                        || "MANUAL".equals(value)
                        || "IMPORTACION".equals(value)
        ) {
            return value;
        }

        throw new BusinessException(
                "Origen de registro no válido"
        );
    }

    private String cleanEstadoVisita(
            String estadoVisita
    ) {
        String value =
                hasText(estadoVisita)
                        ? estadoVisita
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        )
                        : "PENDIENTE";

        if (
                value.equals("PENDIENTE")
                        || value.equals("PROGRAMADA")
                        || value.equals("REALIZADA")
                        || value.equals("NO_ATENDIDA")
                        || value.equals("REPROGRAMADA")
                        || value.equals("CANCELADA")
        ) {
            return value;
        }

        throw new BusinessException(
                "Estado de visita no válido"
        );
    }

    private String
    normalizeTipoSolicitudCallcenter(
            String tipoSolicitudCallcenter,
            Boolean solicitoNuevaEncuesta
    ) {
        String value =
                hasText(
                        tipoSolicitudCallcenter
                )
                        ? tipoSolicitudCallcenter
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        )
                        : null;

        if (
                !hasText(value)
                        && Boolean.TRUE.equals(
                        solicitoNuevaEncuesta
                )
        ) {
            return "NUEVA_ENCUESTA";
        }

        if (!hasText(value)) {
            return "OTRO";
        }

        if (
                value.equals("NUEVA_ENCUESTA")
                        || value.equals("INCLUSION")
                        || value.equals("VERIFICACION")
                        || value.equals("OTRO")
        ) {
            return value;
        }

        throw new BusinessException(
                "Tipo de solicitud Call Center no válido"
        );
    }

    private String getNombreEncuestadorAsignado(
            CallCenterRegistro entity
    ) {
        if (
                entity.getEncuestadorAsignado()
                        != null
                        && hasText(
                        entity
                                .getEncuestadorAsignado()
                                .getNombre()
                )
        ) {
            return entity
                    .getEncuestadorAsignado()
                    .getNombre();
        }

        if (
                entity.getEncuestadorProgramado()
                        != null
                        && hasText(
                        entity
                                .getEncuestadorProgramado()
                                .getNombre()
                )
        ) {
            return entity
                    .getEncuestadorProgramado()
                    .getNombre();
        }

        return "sin encuestador registrado";
    }

    private String fullName(
            User user
    ) {
        if (user == null) {
            return null;
        }

        String nombres =
                user.getNombres() != null
                        ? user.getNombres()
                        : "";

        String apellidos =
                user.getApellidos() != null
                        ? user.getApellidos()
                        : "";

        String nombreCompleto =
                (
                        nombres
                                + " "
                                + apellidos
                ).trim();

        return nombreCompleto.isBlank()
                ? user.getUsername()
                : nombreCompleto;
    }

    private CallCenterUserOptionResponse
    toUserOptionResponse(
            User user
    ) {
        return new CallCenterUserOptionResponse(
                user.getId(),
                user.getUsername(),
                fullName(user),
                user.getRole() != null
                        ? user.getRole()
                        .getCodigo()
                        : null,
                Boolean.TRUE.equals(
                        user.getActivo()
                )
        );
    }

    private CallCenterResponse toResponse(
            CallCenterRegistro entity
    ) {
        return new CallCenterResponse(
                entity.getId(),
                entity.getMarcaTemporal(),
                entity.getFechaLlamada(),
                entity.getHoraLlamada(),
                entity.getTipoRegistro(),
                entity.getOrigenRegistro(),

                entity.getVentanillaRegistro()
                        != null
                        ? entity
                        .getVentanillaRegistro()
                        .getId()
                        : null,

                entity.getVentanillaRegistro()
                        != null
                        ? entity
                        .getVentanillaRegistro()
                        .getNumeroVentanilla()
                        : null,

                entity.getVentanillaRegistro()
                        != null
                        ? entity
                        .getVentanillaRegistro()
                        .getFecha()
                        : null,

                entity.getFuncionario()
                        != null
                        ? entity
                        .getFuncionario()
                        .getId()
                        : null,

                entity.getFuncionario()
                        != null
                        ? entity
                        .getFuncionario()
                        .getUsername()
                        : null,

                entity.getFuncionarioCallcenterAsignado()
                        != null
                        ? entity
                        .getFuncionarioCallcenterAsignado()
                        .getId()
                        : null,

                entity.getFuncionarioCallcenterAsignado()
                        != null
                        ? entity
                        .getFuncionarioCallcenterAsignado()
                        .getUsername()
                        : null,

                fullName(
                        entity
                                .getFuncionarioCallcenterAsignado()
                ),

                entity.getFechaAsignacionCallcenter(),

                entity.getUsuarioAsignaCallcenter()
                        != null
                        ? entity
                        .getUsuarioAsignaCallcenter()
                        .getId()
                        : null,

                entity.getUsuarioAsignaCallcenter()
                        != null
                        ? entity
                        .getUsuarioAsignaCallcenter()
                        .getUsername()
                        : null,

                entity.getCedulaSolicitante(),
                entity.getNombreCompleto(),
                entity.getTelefono(),
                entity.getLlamadaConectada(),

                entity.getMotivoNoContacto()
                        != null
                        ? entity
                        .getMotivoNoContacto()
                        .getId()
                        : null,

                entity.getMotivoNoContacto()
                        != null
                        ? entity
                        .getMotivoNoContacto()
                        .getCodigo()
                        : null,

                entity.getMotivoNoContacto()
                        != null
                        ? entity
                        .getMotivoNoContacto()
                        .getNombre()
                        : null,

                entity.getMotivoNoContactoTexto(),

                entity.getEncuestadorProgramado()
                        != null
                        ? entity
                        .getEncuestadorProgramado()
                        .getId()
                        : null,

                entity.getEncuestadorProgramado()
                        != null
                        ? entity
                        .getEncuestadorProgramado()
                        .getNombre()
                        : null,

                entity.getFechaEncuestaProgramada(),
                entity.getSolicitoNuevaEncuesta(),
                entity.getDireccionTexto(),

                entity.getBarrio()
                        != null
                        ? entity
                        .getBarrio()
                        .getId()
                        : null,

                entity.getBarrio()
                        != null
                        ? entity
                        .getBarrio()
                        .getNombre()
                        : null,

                entity.getBarrio()
                        != null
                        && entity
                        .getBarrio()
                        .getComuna()
                        != null
                        ? entity
                        .getBarrio()
                        .getComuna()
                        .getId()
                        : null,

                entity.getBarrio()
                        != null
                        && entity
                        .getBarrio()
                        .getComuna()
                        != null
                        ? entity
                        .getBarrio()
                        .getComuna()
                        .getNombre()
                        : null,

                entity.getFechaAplicacionInformada(),
                entity.getDisposicionRecibirEncuesta(),

                entity.getMotivoNoDisposicion()
                        != null
                        ? entity
                        .getMotivoNoDisposicion()
                        .getId()
                        : null,

                entity.getMotivoNoDisposicion()
                        != null
                        ? entity
                        .getMotivoNoDisposicion()
                        .getCodigo()
                        : null,

                entity.getMotivoNoDisposicion()
                        != null
                        ? entity
                        .getMotivoNoDisposicion()
                        .getNombre()
                        : null,

                entity.getMotivoNoDisposicionTexto(),

                entity.getEncuestadorAsignado()
                        != null
                        ? entity
                        .getEncuestadorAsignado()
                        .getId()
                        : null,

                entity.getEncuestadorAsignado()
                        != null
                        ? entity
                        .getEncuestadorAsignado()
                        .getNombre()
                        : null,

                entity.getExplicoInformanteCalificado(),
                entity.getVerificado(),
                entity.getEstadoVisita(),
                entity.getFechaVisitaReal(),
                entity.getHoraVisitaReal(),
                entity.getEncuestaRealizada(),
                entity.getMotivoNoEncuesta(),
                entity.getFechaReprogramacion(),
                entity.getObservacionEncuestador(),
                entity.getObservacion(),
                entity.getEstadoCaso(),
                entity.getTipoSolicitudCallcenter(),
                entity.getFechaCierre(),
                entity.getMotivoCierre(),

                entity.getUsuarioCierre()
                        != null
                        ? entity
                        .getUsuarioCierre()
                        .getId()
                        : null,

                entity.getUsuarioCierre()
                        != null
                        ? entity
                        .getUsuarioCierre()
                        .getUsername()
                        : null,

                entity.getActivo()
        );
    }

    private CallCenterCatalogResponse
    toCatalogResponse(
            CallCenterMotivoNoContacto entity
    ) {
        return new CallCenterCatalogResponse(
                entity.getId(),
                entity.getCodigo(),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.getActivo()
        );
    }

    private CallCenterCatalogResponse
    toCatalogResponse(
            CallCenterMotivoNoDisposicion entity
    ) {
        return new CallCenterCatalogResponse(
                entity.getId(),
                entity.getCodigo(),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.getActivo()
        );
    }

    private Map<String, Object> snapshot(
            CallCenterRegistro entity
    ) {
        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "id",
                entity.getId()
        );

        data.put(
                "marcaTemporal",
                entity.getMarcaTemporal()
        );

        data.put(
                "fechaLlamada",
                entity.getFechaLlamada()
        );

        data.put(
                "horaLlamada",
                entity.getHoraLlamada()
        );

        data.put(
                "tipoRegistro",
                entity.getTipoRegistro()
        );

        data.put(
                "origenRegistro",
                entity.getOrigenRegistro()
        );

        data.put(
                "ventanillaRegistroId",
                entity.getVentanillaRegistro()
                        != null
                        ? entity
                        .getVentanillaRegistro()
                        .getId()
                        : null
        );

        data.put(
                "funcionarioId",
                entity.getFuncionario()
                        != null
                        ? entity
                        .getFuncionario()
                        .getId()
                        : null
        );

        data.put(
                "funcionarioUsername",
                entity.getFuncionario()
                        != null
                        ? entity
                        .getFuncionario()
                        .getUsername()
                        : null
        );

        data.put(
                "funcionarioCallcenterAsignadoId",
                entity
                        .getFuncionarioCallcenterAsignado()
                        != null
                        ? entity
                        .getFuncionarioCallcenterAsignado()
                        .getId()
                        : null
        );

        data.put(
                "funcionarioCallcenterAsignadoUsername",
                entity
                        .getFuncionarioCallcenterAsignado()
                        != null
                        ? entity
                        .getFuncionarioCallcenterAsignado()
                        .getUsername()
                        : null
        );

        data.put(
                "fechaAsignacionCallcenter",
                entity.getFechaAsignacionCallcenter()
        );

        data.put(
                "cedulaSolicitante",
                entity.getCedulaSolicitante()
        );

        data.put(
                "nombreCompleto",
                entity.getNombreCompleto()
        );

        data.put(
                "telefono",
                entity.getTelefono()
        );

        data.put(
                "llamadaConectada",
                entity.getLlamadaConectada()
        );

        data.put(
                "estadoCaso",
                entity.getEstadoCaso()
        );

        data.put(
                "estadoVisita",
                entity.getEstadoVisita()
        );

        data.put(
                "encuestaRealizada",
                entity.getEncuestaRealizada()
        );

        data.put(
                "encuestadorAsignadoId",
                entity.getEncuestadorAsignado()
                        != null
                        ? entity
                        .getEncuestadorAsignado()
                        .getId()
                        : null
        );

        data.put(
                "encuestadorProgramadoId",
                entity.getEncuestadorProgramado()
                        != null
                        ? entity
                        .getEncuestadorProgramado()
                        .getId()
                        : null
        );

        data.put(
                "fechaEncuestaProgramada",
                entity.getFechaEncuestaProgramada()
        );

        data.put(
                "fechaCierre",
                entity.getFechaCierre()
        );

        data.put(
                "motivoCierre",
                entity.getMotivoCierre()
        );

        data.put(
                "usuarioCierreId",
                entity.getUsuarioCierre()
                        != null
                        ? entity
                        .getUsuarioCierre()
                        .getId()
                        : null
        );

        data.put(
                "observacion",
                entity.getObservacion()
        );

        data.put(
                "activo",
                entity.getActivo()
        );

        return data;
    }

    private long safeLong(
            Long value
    ) {
        return value != null
                ? value
                : 0L;
    }

    private String clean(
            String value
    ) {
        return hasText(value)
                ? value.trim()
                : null;
    }

    private String upper(
            String value
    ) {
        return hasText(value)
                ? value
                .trim()
                .replaceAll(
                        "\\s+",
                        " "
                )
                .toUpperCase(
                        Locale.ROOT
                )
                : null;
    }

    private String limitLength(
            String value,
            int maxLength
    ) {
        if (value == null) {
            return null;
        }

        if (
                value.length()
                        <= maxLength
        ) {
            return value;
        }

        return value.substring(
                0,
                maxLength
        );
    }

    private boolean hasText(
            String value
    ) {
        return value != null
                && !value.trim().isBlank();
    }
}