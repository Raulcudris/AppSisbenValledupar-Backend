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

@Service
@RequiredArgsConstructor
public class CallCenterService {

    private static final String TABLE_NAME = "callcenter_registro";

    private final CallCenterRegistroRepository repository;
    private final CallCenterMotivoNoContactoRepository motivoNoContactoRepository;
    private final CallCenterMotivoNoDisposicionRepository motivoNoDisposicionRepository;
    private final UserRepository userRepository;
    private final EncuestadorRepository encuestadorRepository;
    private final BarrioRepository barrioRepository;
    private final VentanillaRegistroRepository ventanillaRegistroRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<CallCenterResponse> findAll(Pageable pageable) {
        Page<CallCenterRegistro> page = findPageForCurrentUser(
                CallCenterRegistroSpecification.activeOnly(),
                pageable
        );

        return PageResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<CallCenterResponse> search(CallCenterFilterRequest filter, Pageable pageable) {
        Page<CallCenterRegistro> page = findPageForCurrentUser(
                CallCenterRegistroSpecification.byFilter(filter),
                pageable
        );

        return PageResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<CallCenterResponse> misAsignaciones(Pageable pageable) {
        Page<CallCenterRegistro> page;

        if (currentUserHasRole("FUNCIONARIO_ENCUESTADOR")) {
            Encuestador encuestador = currentEncuestador();
            page = repository.findAll(
                    CallCenterRegistroSpecification.byEncuestadorAsignadoOrProgramado(encuestador.getId()),
                    pageable
            );
        } else if (currentUserHasRole("FUNCIONARIO_CALLCENTER")) {
            User user = currentUser();
            page = repository.findAll(
                    CallCenterRegistroSpecification.byFuncionarioCallcenterAsignado(user.getId()),
                    pageable
            );
        } else {
            page = repository.findAll(
                    CallCenterRegistroSpecification.activeOnly(),
                    pageable
            );
        }

        return PageResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<CallCenterResponse> pendientesAsignarFuncionario(Pageable pageable) {
        Page<CallCenterRegistro> page = repository.findAll(
                CallCenterRegistroSpecification.pendientesAsignarFuncionarioCallcenter(),
                pageable
        );

        return PageResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<CallCenterResponse> misRegistrosCallcenter(Pageable pageable) {
        if (currentUserHasRole("FUNCIONARIO_CALLCENTER")) {
            User user = currentUser();

            Page<CallCenterRegistro> page = repository.findAll(
                    CallCenterRegistroSpecification.byFuncionarioCallcenterAsignado(user.getId()),
                    pageable
            );

            return PageResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
        }

        Page<CallCenterRegistro> page = repository.findAll(
                CallCenterRegistroSpecification.activeOnly(),
                pageable
        );

        return PageResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public CallCenterResponse findById(Long id) {
        CallCenterRegistro entity = findEntity(id);
        validateCurrentFuncionarioCallcenterCanAccess(entity);

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<CallCenterCatalogResponse> findMotivosNoContacto() {
        return motivoNoContactoRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(this::toCatalogResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CallCenterCatalogResponse> findMotivosNoDisposicion() {
        return motivoNoDisposicionRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(this::toCatalogResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CallCenterUserOptionResponse> findFuncionariosCallcenter() {
        return userRepository.findAll()
                .stream()
                .filter(this::isUserActivo)
                .filter(user -> hasRoleCode(user, "FUNCIONARIO_CALLCENTER"))
                .map(this::toUserOptionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CallCenterSummaryResponse summary() {
        List<CallCenterRegistro> all = findSummaryRecordsForCurrentUser();

        long total = all.size();
        long conectadas = all.stream().filter(item -> Boolean.TRUE.equals(item.getLlamadaConectada())).count();
        long noConectadas = all.stream().filter(item -> Boolean.FALSE.equals(item.getLlamadaConectada())).count();
        long activos = all.stream().filter(item -> Boolean.TRUE.equals(item.getActivo())).count();
        long inactivos = all.stream().filter(item -> !Boolean.TRUE.equals(item.getActivo())).count();

        return new CallCenterSummaryResponse(total, conectadas, noConectadas, activos, inactivos);
    }

    @Transactional
    public CallCenterResponse create(CallCenterRequest request) {
        CallCenterRegistro entity = new CallCenterRegistro();
        User user = currentUser();

        entity.setFuncionario(user);
        entity.setCreadoPor(user);
        entity.setActivo(true);
        entity.setTipoRegistro("LLAMADA");
        entity.setOrigenRegistro("MANUAL");
        entity.setEstadoVisita("PENDIENTE");

        if (currentUserHasRole("FUNCIONARIO_CALLCENTER")) {
            entity.setFuncionarioCallcenterAsignado(user);
            entity.setFechaAsignacionCallcenter(LocalDateTime.now());
            entity.setUsuarioAsignaCallcenter(user);
        }

        apply(entity, request, user);

        CallCenterRegistro saved = repository.save(entity);

        auditService.safeLog(AuditAction.CREATE, TABLE_NAME, saved.getId(), null, snapshot(saved));

        return toResponse(saved);
    }

    @Transactional
    public CallCenterResponse update(Long id, CallCenterRequest request) {
        CallCenterRegistro entity = findEntity(id);
        validateCurrentFuncionarioCallcenterCanAccess(entity);

        Map<String, Object> before = snapshot(entity);

        apply(entity, request, currentUser());

        auditService.safeLog(AuditAction.UPDATE, TABLE_NAME, entity.getId(), before, snapshot(entity));

        return toResponse(entity);
    }

    @Transactional
    public List<CallCenterResponse> asignarFuncionarioCallcenter(CallCenterAsignarFuncionarioRequest request) {
        User funcionario = findUser(request.funcionarioCallcenterId());

        if (!hasRoleCode(funcionario, "FUNCIONARIO_CALLCENTER")) {
            throw new BusinessException("El usuario seleccionado no tiene rol FUNCIONARIO_CALLCENTER");
        }

        User user = currentUser();
        List<CallCenterRegistro> registros = repository.findAllById(request.registroIds());

        if (registros.size() != request.registroIds().size()) {
            throw new BusinessException("Uno o más registros no existen");
        }

        for (CallCenterRegistro registro : registros) {
            validateRegistroAsignableAFuncionarioCallcenter(registro, funcionario);

            Map<String, Object> before = snapshot(registro);

            registro.setFuncionarioCallcenterAsignado(funcionario);
            registro.setFechaAsignacionCallcenter(LocalDateTime.now());
            registro.setUsuarioAsignaCallcenter(user);
            registro.setActualizadoPor(user);

            auditService.safeLog(AuditAction.UPDATE, TABLE_NAME, registro.getId(), before, snapshot(registro));
        }

        return registros.stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<CallCenterResponse> asignarEncuestador(CallCenterAsignarEncuestadorRequest request) {
        Encuestador encuestador = findEncuestador(request.encuestadorId());
        User user = currentUser();
        List<CallCenterRegistro> registros = repository.findAllById(request.registroIds());

        if (registros.size() != request.registroIds().size()) {
            throw new BusinessException("Uno o más registros no existen");
        }

        for (CallCenterRegistro registro : registros) {
            validateRegistroAsignableAEncuestador(registro, encuestador, user);

            Map<String, Object> before = snapshot(registro);

            registro.setEncuestadorAsignado(encuestador);
            registro.setEncuestadorProgramado(encuestador);

            if (request.fechaEncuestaProgramada() != null) {
                registro.setFechaEncuestaProgramada(request.fechaEncuestaProgramada());
            }

            if (!hasText(registro.getEstadoVisita())) {
                registro.setEstadoVisita("PENDIENTE");
            }

            registro.setActualizadoPor(user);

            auditService.safeLog(AuditAction.UPDATE, TABLE_NAME, registro.getId(), before, snapshot(registro));
        }

        return registros.stream().map(this::toResponse).toList();
    }

    @Transactional
    public CallCenterResponse updateVisita(Long id, CallCenterVisitaRequest request) {
        CallCenterRegistro entity = findEntity(id);
        User user = currentUser();

        validateCurrentFuncionarioCallcenterCanAccess(entity);

        if (currentUserHasRole("FUNCIONARIO_ENCUESTADOR")) {
            Encuestador encuestador = currentEncuestador();

            boolean assigned = entity.getEncuestadorAsignado() != null
                    && encuestador.getId().equals(entity.getEncuestadorAsignado().getId());

            boolean programmed = entity.getEncuestadorProgramado() != null
                    && encuestador.getId().equals(entity.getEncuestadorProgramado().getId());

            if (!assigned && !programmed) {
                throw new BusinessException("El registro no está asignado al encuestador autenticado");
            }
        }

        Map<String, Object> before = snapshot(entity);

        entity.setEstadoVisita(cleanEstadoVisita(request.estadoVisita()));
        entity.setFechaVisitaReal(request.fechaVisitaReal());
        entity.setHoraVisitaReal(request.horaVisitaReal());
        entity.setEncuestaRealizada(request.encuestaRealizada());
        entity.setMotivoNoEncuesta(clean(request.motivoNoEncuesta()));
        entity.setFechaReprogramacion(request.fechaReprogramacion());
        entity.setObservacionEncuestador(clean(request.observacionEncuestador()));

        if (request.verificado() != null) {
            entity.setVerificado(request.verificado());
        }

        entity.setActualizadoPor(user);

        auditService.safeLog(AuditAction.UPDATE, TABLE_NAME, entity.getId(), before, snapshot(entity));

        return toResponse(entity);
    }

    @Transactional
    public CallCenterResponse activate(Long id) {
        CallCenterRegistro entity = findEntity(id);
        validateCurrentFuncionarioCallcenterCanAccess(entity);

        Map<String, Object> before = snapshot(entity);

        entity.setActivo(true);
        entity.setActualizadoPor(currentUser());

        auditService.safeLog(AuditAction.UPDATE, TABLE_NAME, entity.getId(), before, snapshot(entity));

        return toResponse(entity);
    }

    @Transactional
    public CallCenterResponse deactivate(Long id) {
        CallCenterRegistro entity = findEntity(id);
        validateCurrentFuncionarioCallcenterCanAccess(entity);

        Map<String, Object> before = snapshot(entity);

        entity.setActivo(false);
        entity.setActualizadoPor(currentUser());

        auditService.safeLog(AuditAction.UPDATE, TABLE_NAME, entity.getId(), before, snapshot(entity));

        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        deactivate(id);
    }

    private void apply(CallCenterRegistro entity, CallCenterRequest request, User user) {
        validateRequest(entity, request);

        VentanillaRegistro ventanillaRegistro = findVentanillaRegistro(request.ventanillaRegistroId());
        String origenRegistro = normalizeOrigenRegistro(request.origenRegistro(), ventanillaRegistro);

        entity.setMarcaTemporal(request.marcaTemporal());
        entity.setFechaLlamada(request.fechaLlamada());
        entity.setHoraLlamada(request.horaLlamada());
        entity.setTipoRegistro(normalizeTipoRegistro(request.tipoRegistro()));
        entity.setOrigenRegistro(origenRegistro);
        entity.setVentanillaRegistro(ventanillaRegistro);

        entity.setCedulaSolicitante(clean(request.cedulaSolicitante()));
        entity.setNombreCompleto(upper(request.nombreCompleto()));
        entity.setTelefono(clean(request.telefono()));
        entity.setLlamadaConectada(request.llamadaConectada());
        entity.setObservacion(clean(request.observacion()));
        entity.setVerificado(request.verificado());
        entity.setActualizadoPor(user);

        if (!hasText(entity.getEstadoVisita())) {
            entity.setEstadoVisita("PENDIENTE");
        }

        if (request.activo() != null) {
            entity.setActivo(request.activo());
        }

        entity.setMotivoNoContacto(findMotivoNoContacto(request.motivoNoContactoId()));
        entity.setMotivoNoContactoTexto(clean(request.motivoNoContactoTexto()));
        entity.setEncuestadorProgramado(findEncuestador(request.encuestadorProgramadoId()));
        entity.setFechaEncuestaProgramada(request.fechaEncuestaProgramada());

        entity.setSolicitoNuevaEncuesta(request.solicitoNuevaEncuesta());
        entity.setDireccionTexto(clean(request.direccionTexto()));
        entity.setBarrio(findBarrio(request.barrioId()));
        entity.setFechaAplicacionInformada(request.fechaAplicacionInformada());
        entity.setDisposicionRecibirEncuesta(request.disposicionRecibirEncuesta());
        entity.setMotivoNoDisposicion(findMotivoNoDisposicion(request.motivoNoDisposicionId()));
        entity.setMotivoNoDisposicionTexto(clean(request.motivoNoDisposicionTexto()));
        entity.setEncuestadorAsignado(findEncuestador(request.encuestadorAsignadoId()));
        entity.setExplicoInformanteCalificado(request.explicoInformanteCalificado());
    }

    private void validateRequest(CallCenterRegistro entity, CallCenterRequest request) {
        if (request.ventanillaRegistroId() != null
                && hasText(request.origenRegistro())
                && !"VENTANILLA".equals(request.origenRegistro().trim().toUpperCase(Locale.ROOT))) {
            throw new BusinessException("Si se relaciona un registro de ventanilla, el origen debe ser VENTANILLA");
        }

        if ("VENTANILLA".equalsIgnoreCase(clean(request.origenRegistro()))
                && request.ventanillaRegistroId() == null) {
            throw new BusinessException("Debe seleccionar un registro de ventanilla para el origen VENTANILLA");
        }

        if (Boolean.FALSE.equals(request.llamadaConectada())) {
            boolean hasMotivo = request.motivoNoContactoId() != null || hasText(request.motivoNoContactoTexto());

            if (!hasMotivo) {
                throw new BusinessException("Debe registrar el motivo por el cual no se logró conectar la llamada");
            }
        }

        if (Boolean.TRUE.equals(request.llamadaConectada())
                && Boolean.FALSE.equals(request.disposicionRecibirEncuesta())) {
            boolean hasMotivo = request.motivoNoDisposicionId() != null || hasText(request.motivoNoDisposicionTexto());

            if (!hasMotivo) {
                throw new BusinessException("Debe registrar el motivo por el cual no se confirmó la disposición");
            }
        }

        validateAsignacionNuevaEncuestaPendiente(entity, request);
    }

    private void validateAsignacionNuevaEncuestaPendiente(CallCenterRegistro entity, CallCenterRequest request) {
        if (!Boolean.TRUE.equals(request.solicitoNuevaEncuesta())) {
            return;
        }

        boolean hasEncuestador = request.encuestadorAsignadoId() != null
                || request.encuestadorProgramadoId() != null;

        if (!hasEncuestador) {
            return;
        }

        validateCambioEncuestadorEnRegistroPendiente(entity, request);

        String cedula = clean(request.cedulaSolicitante());
        Long ventanillaRegistroId = request.ventanillaRegistroId();

        if (!hasText(cedula) && ventanillaRegistroId == null) {
            return;
        }

        List<CallCenterRegistro> pendientes = repository.findAsignacionNuevaEncuestaPendiente(
                entity.getId(),
                ventanillaRegistroId,
                cedula
        );

        if (pendientes.isEmpty()) {
            return;
        }

        CallCenterRegistro pendiente = pendientes.get(0);
        String encuestadorNombre = getNombreEncuestadorAsignado(pendiente);

        throw new BusinessException(
                "Este usuario ya tiene una nueva encuesta pendiente asignada al encuestador "
                        + encuestadorNombre
                        + ". Mientras la encuesta no esté marcada como realizada, no puede asignarse a otro encuestador."
        );
    }

    private void validateCambioEncuestadorEnRegistroPendiente(CallCenterRegistro entity, CallCenterRequest request) {
        if (entity.getId() == null) {
            return;
        }

        if (!Boolean.TRUE.equals(entity.getSolicitoNuevaEncuesta())) {
            return;
        }

        if (Boolean.TRUE.equals(entity.getEncuestaRealizada())) {
            return;
        }

        boolean currentHasEncuestador = entity.getEncuestadorAsignado() != null
                || entity.getEncuestadorProgramado() != null;

        if (!currentHasEncuestador) {
            return;
        }

        boolean changedAsignado = request.encuestadorAsignadoId() != null
                && entity.getEncuestadorAsignado() != null
                && !request.encuestadorAsignadoId().equals(entity.getEncuestadorAsignado().getId());

        boolean changedProgramado = request.encuestadorProgramadoId() != null
                && entity.getEncuestadorProgramado() != null
                && !request.encuestadorProgramadoId().equals(entity.getEncuestadorProgramado().getId());

        boolean assigningNewWhenOnlyProgrammedExists = request.encuestadorAsignadoId() != null
                && entity.getEncuestadorAsignado() == null
                && entity.getEncuestadorProgramado() != null
                && !request.encuestadorAsignadoId().equals(entity.getEncuestadorProgramado().getId());

        boolean programmingNewWhenOnlyAssignedExists = request.encuestadorProgramadoId() != null
                && entity.getEncuestadorProgramado() == null
                && entity.getEncuestadorAsignado() != null
                && !request.encuestadorProgramadoId().equals(entity.getEncuestadorAsignado().getId());

        if (changedAsignado || changedProgramado || assigningNewWhenOnlyProgrammedExists || programmingNewWhenOnlyAssignedExists) {
            throw new BusinessException(
                    "Este usuario ya tiene una nueva encuesta pendiente asignada al encuestador "
                            + getNombreEncuestadorAsignado(entity)
                            + ". Mientras la encuesta no esté marcada como realizada, no puede asignarse a otro encuestador."
            );
        }
    }

    private void validateRegistroAsignableAFuncionarioCallcenter(CallCenterRegistro registro, User funcionario) {
        if (!Boolean.TRUE.equals(registro.getActivo())) {
            throw new BusinessException("Solo se pueden asignar registros activos");
        }

        if (!Boolean.TRUE.equals(registro.getSolicitoNuevaEncuesta())) {
            throw new BusinessException("Solo se pueden asignar registros de nueva encuesta");
        }

        if (Boolean.TRUE.equals(registro.getEncuestaRealizada())) {
            throw new BusinessException("No se puede asignar un registro con encuesta realizada");
        }

        if (registro.getFuncionarioCallcenterAsignado() != null
                && !funcionario.getId().equals(registro.getFuncionarioCallcenterAsignado().getId())) {
            throw new BusinessException(
                    "El registro " + registro.getId() + " ya está asignado a otro funcionario Call Center"
            );
        }
    }

    private void validateRegistroAsignableAEncuestador(CallCenterRegistro registro, Encuestador encuestador, User user) {
        if (currentUserHasRole("FUNCIONARIO_CALLCENTER")
                && (registro.getFuncionarioCallcenterAsignado() == null
                || !user.getId().equals(registro.getFuncionarioCallcenterAsignado().getId()))) {
            throw new BusinessException(
                    "El registro " + registro.getId() + " no está asignado al funcionario Call Center autenticado"
            );
        }

        if (!Boolean.TRUE.equals(registro.getActivo())) {
            throw new BusinessException("Solo se pueden asignar registros activos");
        }

        if (Boolean.TRUE.equals(registro.getEncuestaRealizada())) {
            throw new BusinessException("No se puede asignar encuestador a una encuesta ya realizada");
        }

        if (registro.getEncuestadorAsignado() != null
                && !encuestador.getId().equals(registro.getEncuestadorAsignado().getId())) {
            throw new BusinessException(
                    "El registro " + registro.getId() + " ya está asignado a otro encuestador"
            );
        }
    }

    private Page<CallCenterRegistro> findPageForCurrentUser(
            Specification<CallCenterRegistro> specification,
            Pageable pageable
    ) {
        if (currentUserHasRole("FUNCIONARIO_CALLCENTER")) {
            User user = currentUser();

            specification = specification.and(
                    CallCenterRegistroSpecification.byFuncionarioCallcenterAsignado(user.getId())
            );
        }

        return repository.findAll(specification, pageable);
    }

    private List<CallCenterRegistro> findSummaryRecordsForCurrentUser() {
        if (currentUserHasRole("FUNCIONARIO_CALLCENTER")) {
            User user = currentUser();

            return repository.findAll(
                    CallCenterRegistroSpecification.byFuncionarioCallcenterAsignado(user.getId())
            );
        }

        return repository.findAll();
    }

    private void validateCurrentFuncionarioCallcenterCanAccess(CallCenterRegistro registro) {
        if (!currentUserHasRole("FUNCIONARIO_CALLCENTER")) {
            return;
        }

        User user = currentUser();

        boolean assignedToCurrentUser = registro.getFuncionarioCallcenterAsignado() != null
                && user.getId().equals(registro.getFuncionarioCallcenterAsignado().getId());

        if (!assignedToCurrentUser) {
            throw new BusinessException(
                    "El registro no está asignado al funcionario Call Center autenticado"
            );
        }
    }

    private CallCenterRegistro findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro Call Center no encontrado"));
    }

    private CallCenterMotivoNoContacto findMotivoNoContacto(Long id) {
        if (id == null) return null;

        return motivoNoContactoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Motivo de no contacto no encontrado"));
    }

    private CallCenterMotivoNoDisposicion findMotivoNoDisposicion(Long id) {
        if (id == null) return null;

        return motivoNoDisposicionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Motivo de no disposición no encontrado"));
    }

    private Encuestador findEncuestador(Long id) {
        if (id == null) return null;

        return encuestadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encuestador no encontrado"));
    }

    private Barrio findBarrio(Long id) {
        if (id == null) return null;

        return barrioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Barrio no encontrado"));
    }

    private VentanillaRegistro findVentanillaRegistro(Long id) {
        if (id == null) return null;

        return ventanillaRegistroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de ventanilla no encontrado"));
    }

    private User findUser(Long id) {
        if (id == null) {
            throw new BusinessException("Debe seleccionar un usuario");
        }

        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            throw new BusinessException("No hay usuario autenticado");
        }

        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    private Encuestador currentEncuestador() {
        User user = currentUser();

        if (!hasText(user.getDocumento())) {
            throw new BusinessException("El usuario autenticado no tiene documento registrado");
        }

        return encuestadorRepository.findFirstByDocumento(user.getDocumento())
                .orElseThrow(() -> new BusinessException(
                        "El usuario autenticado no está vinculado a un encuestador por documento"
                ));
    }

    private boolean currentUserHasRole(String roleCode) {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(authority -> roleCode.equals(authority.getAuthority())
                        || ("ROLE_" + roleCode).equals(authority.getAuthority()));
    }

    private boolean hasRoleCode(User user, String roleCode) {
        return user != null
                && user.getRole() != null
                && roleCode.equalsIgnoreCase(user.getRole().getCodigo());
    }

    private String normalizeTipoRegistro(String tipoRegistro) {
        String value = hasText(tipoRegistro)
                ? tipoRegistro.trim().toUpperCase(Locale.ROOT)
                : "LLAMADA";

        if ("LLAMADA".equals(value) || "BASE_ENCUESTADOR".equals(value)) {
            return value;
        }

        throw new BusinessException("Tipo de registro no válido");
    }

    private String normalizeOrigenRegistro(String origenRegistro, VentanillaRegistro ventanillaRegistro) {
        String value = hasText(origenRegistro)
                ? origenRegistro.trim().toUpperCase(Locale.ROOT)
                : null;

        if (ventanillaRegistro != null) {
            return "VENTANILLA";
        }

        if (value == null) {
            return "MANUAL";
        }

        if ("VENTANILLA".equals(value) || "MANUAL".equals(value) || "IMPORTACION".equals(value)) {
            return value;
        }

        throw new BusinessException("Origen de registro no válido");
    }

    private String cleanEstadoVisita(String estadoVisita) {
        String value = hasText(estadoVisita)
                ? estadoVisita.trim().toUpperCase(Locale.ROOT)
                : "PENDIENTE";

        if (
                value.equals("PENDIENTE") ||
                        value.equals("PROGRAMADA") ||
                        value.equals("REALIZADA") ||
                        value.equals("NO_ATENDIDA") ||
                        value.equals("REPROGRAMADA") ||
                        value.equals("CANCELADA")
        ) {
            return value;
        }

        throw new BusinessException("Estado de visita no válido");
    }

    private String getNombreEncuestadorAsignado(CallCenterRegistro entity) {
        if (entity.getEncuestadorAsignado() != null && hasText(entity.getEncuestadorAsignado().getNombre())) {
            return entity.getEncuestadorAsignado().getNombre();
        }

        if (entity.getEncuestadorProgramado() != null && hasText(entity.getEncuestadorProgramado().getNombre())) {
            return entity.getEncuestadorProgramado().getNombre();
        }

        return "sin encuestador registrado";
    }

    private String fullName(User user) {
        if (user == null) {
            return null;
        }

        String nombres = user.getNombres() != null ? user.getNombres() : "";
        String apellidos = user.getApellidos() != null ? user.getApellidos() : "";
        String nombreCompleto = (nombres + " " + apellidos).trim();

        return nombreCompleto.isBlank() ? user.getUsername() : nombreCompleto;
    }

    private CallCenterUserOptionResponse toUserOptionResponse(User user) {
        return new CallCenterUserOptionResponse(
                user.getId(),
                user.getUsername(),
                fullName(user),
                user.getRole() != null ? user.getRole().getCodigo() : null,
                isUserActivo(user)
        );
    }

    private boolean isUserActivo(User user) {
        if (user == null || user.getActivo() == null) {
            return false;
        }

        String value = String.valueOf(user.getActivo()).trim().toUpperCase(Locale.ROOT);

        return value.equals("TRUE")
                || value.equals("1")
                || value.equals("SI")
                || value.equals("SÍ")
                || value.equals("ACTIVO")
                || value.equals("A");
    }

    private CallCenterResponse toResponse(CallCenterRegistro entity) {
        return new CallCenterResponse(
                entity.getId(),
                entity.getMarcaTemporal(),
                entity.getFechaLlamada(),
                entity.getHoraLlamada(),
                entity.getTipoRegistro(),
                entity.getOrigenRegistro(),
                entity.getVentanillaRegistro() != null ? entity.getVentanillaRegistro().getId() : null,
                entity.getVentanillaRegistro() != null ? entity.getVentanillaRegistro().getNumeroVentanilla() : null,
                entity.getVentanillaRegistro() != null ? entity.getVentanillaRegistro().getFecha() : null,

                entity.getFuncionario() != null ? entity.getFuncionario().getId() : null,
                entity.getFuncionario() != null ? entity.getFuncionario().getUsername() : null,

                entity.getFuncionarioCallcenterAsignado() != null ? entity.getFuncionarioCallcenterAsignado().getId() : null,
                entity.getFuncionarioCallcenterAsignado() != null ? entity.getFuncionarioCallcenterAsignado().getUsername() : null,
                fullName(entity.getFuncionarioCallcenterAsignado()),
                entity.getFechaAsignacionCallcenter(),
                entity.getUsuarioAsignaCallcenter() != null ? entity.getUsuarioAsignaCallcenter().getId() : null,
                entity.getUsuarioAsignaCallcenter() != null ? entity.getUsuarioAsignaCallcenter().getUsername() : null,

                entity.getCedulaSolicitante(),
                entity.getNombreCompleto(),
                entity.getTelefono(),

                entity.getLlamadaConectada(),

                entity.getMotivoNoContacto() != null ? entity.getMotivoNoContacto().getId() : null,
                entity.getMotivoNoContacto() != null ? entity.getMotivoNoContacto().getCodigo() : null,
                entity.getMotivoNoContacto() != null ? entity.getMotivoNoContacto().getNombre() : null,
                entity.getMotivoNoContactoTexto(),

                entity.getEncuestadorProgramado() != null ? entity.getEncuestadorProgramado().getId() : null,
                entity.getEncuestadorProgramado() != null ? entity.getEncuestadorProgramado().getNombre() : null,
                entity.getFechaEncuestaProgramada(),

                entity.getSolicitoNuevaEncuesta(),

                entity.getDireccionTexto(),
                entity.getBarrio() != null ? entity.getBarrio().getId() : null,
                entity.getBarrio() != null ? entity.getBarrio().getNombre() : null,
                entity.getBarrio() != null && entity.getBarrio().getComuna() != null
                        ? entity.getBarrio().getComuna().getId()
                        : null,
                entity.getBarrio() != null && entity.getBarrio().getComuna() != null
                        ? entity.getBarrio().getComuna().getNombre()
                        : null,

                entity.getFechaAplicacionInformada(),
                entity.getDisposicionRecibirEncuesta(),

                entity.getMotivoNoDisposicion() != null ? entity.getMotivoNoDisposicion().getId() : null,
                entity.getMotivoNoDisposicion() != null ? entity.getMotivoNoDisposicion().getCodigo() : null,
                entity.getMotivoNoDisposicion() != null ? entity.getMotivoNoDisposicion().getNombre() : null,
                entity.getMotivoNoDisposicionTexto(),

                entity.getEncuestadorAsignado() != null ? entity.getEncuestadorAsignado().getId() : null,
                entity.getEncuestadorAsignado() != null ? entity.getEncuestadorAsignado().getNombre() : null,

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
                entity.getActivo()
        );
    }

    private CallCenterCatalogResponse toCatalogResponse(CallCenterMotivoNoContacto entity) {
        return new CallCenterCatalogResponse(
                entity.getId(),
                entity.getCodigo(),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.getActivo()
        );
    }

    private CallCenterCatalogResponse toCatalogResponse(CallCenterMotivoNoDisposicion entity) {
        return new CallCenterCatalogResponse(
                entity.getId(),
                entity.getCodigo(),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.getActivo()
        );
    }

    private Map<String, Object> snapshot(CallCenterRegistro entity) {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("id", entity.getId());
        data.put("marcaTemporal", entity.getMarcaTemporal());
        data.put("fechaLlamada", entity.getFechaLlamada());
        data.put("horaLlamada", entity.getHoraLlamada());
        data.put("tipoRegistro", entity.getTipoRegistro());
        data.put("origenRegistro", entity.getOrigenRegistro());
        data.put("ventanillaRegistroId", entity.getVentanillaRegistro() != null ? entity.getVentanillaRegistro().getId() : null);
        data.put("funcionarioId", entity.getFuncionario() != null ? entity.getFuncionario().getId() : null);
        data.put("funcionarioUsername", entity.getFuncionario() != null ? entity.getFuncionario().getUsername() : null);
        data.put("funcionarioCallcenterAsignadoId", entity.getFuncionarioCallcenterAsignado() != null ? entity.getFuncionarioCallcenterAsignado().getId() : null);
        data.put("funcionarioCallcenterAsignadoUsername", entity.getFuncionarioCallcenterAsignado() != null ? entity.getFuncionarioCallcenterAsignado().getUsername() : null);
        data.put("funcionarioCallcenterAsignadoNombre", fullName(entity.getFuncionarioCallcenterAsignado()));
        data.put("fechaAsignacionCallcenter", entity.getFechaAsignacionCallcenter());
        data.put("usuarioAsignaCallcenterId", entity.getUsuarioAsignaCallcenter() != null ? entity.getUsuarioAsignaCallcenter().getId() : null);
        data.put("usuarioAsignaCallcenterUsername", entity.getUsuarioAsignaCallcenter() != null ? entity.getUsuarioAsignaCallcenter().getUsername() : null);
        data.put("cedulaSolicitante", entity.getCedulaSolicitante());
        data.put("nombreCompleto", entity.getNombreCompleto());
        data.put("telefono", entity.getTelefono());
        data.put("llamadaConectada", entity.getLlamadaConectada());
        data.put("motivoNoContactoId", entity.getMotivoNoContacto() != null ? entity.getMotivoNoContacto().getId() : null);
        data.put("motivoNoContactoTexto", entity.getMotivoNoContactoTexto());
        data.put("encuestadorProgramadoId", entity.getEncuestadorProgramado() != null ? entity.getEncuestadorProgramado().getId() : null);
        data.put("fechaEncuestaProgramada", entity.getFechaEncuestaProgramada());
        data.put("solicitoNuevaEncuesta", entity.getSolicitoNuevaEncuesta());
        data.put("direccionTexto", entity.getDireccionTexto());
        data.put("barrioId", entity.getBarrio() != null ? entity.getBarrio().getId() : null);
        data.put("fechaAplicacionInformada", entity.getFechaAplicacionInformada());
        data.put("disposicionRecibirEncuesta", entity.getDisposicionRecibirEncuesta());
        data.put("motivoNoDisposicionId", entity.getMotivoNoDisposicion() != null ? entity.getMotivoNoDisposicion().getId() : null);
        data.put("motivoNoDisposicionTexto", entity.getMotivoNoDisposicionTexto());
        data.put("encuestadorAsignadoId", entity.getEncuestadorAsignado() != null ? entity.getEncuestadorAsignado().getId() : null);
        data.put("explicoInformanteCalificado", entity.getExplicoInformanteCalificado());
        data.put("verificado", entity.getVerificado());
        data.put("estadoVisita", entity.getEstadoVisita());
        data.put("fechaVisitaReal", entity.getFechaVisitaReal());
        data.put("horaVisitaReal", entity.getHoraVisitaReal());
        data.put("encuestaRealizada", entity.getEncuestaRealizada());
        data.put("motivoNoEncuesta", entity.getMotivoNoEncuesta());
        data.put("fechaReprogramacion", entity.getFechaReprogramacion());
        data.put("observacionEncuestador", entity.getObservacionEncuestador());
        data.put("observacion", entity.getObservacion());
        data.put("activo", entity.getActivo());

        return data;
    }

    private String clean(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String upper(String value) {
        return hasText(value) ? value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT) : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }
}