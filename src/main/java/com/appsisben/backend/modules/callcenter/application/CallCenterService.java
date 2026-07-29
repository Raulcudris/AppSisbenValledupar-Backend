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

/**
 * Servicio principal del módulo Call Center.
 *
 * <p>Centraliza las operaciones sobre el caso maestro de Call Center:
 * consulta, creación, actualización, asignación a funcionario, asignación
 * legacy de encuestador, activación, inactivación y auditoría.</p>
 *
 * <p>El flujo formal de llamadas y visitas se complementa con
 * {@link CallCenterWorkflowService}. Este servicio mantiene compatibilidad
 * con pantallas y datos legacy almacenados en {@link CallCenterRegistro}.</p>
 */
@Service
@RequiredArgsConstructor
public class CallCenterService {

    private static final String TABLE_NAME = "callcenter_registro";
    private static final String ESTADO_CERRADO = "CERRADO";
    private static final String ESTADO_CANCELADO = "CANCELADO";
    private static final String MOTIVO_CIERRE_ENCUESTA_REALIZADA = "Encuesta realizada por encuestador";

    private final CallCenterRegistroRepository repository;
    private final CallCenterMotivoNoContactoRepository motivoNoContactoRepository;
    private final CallCenterMotivoNoDisposicionRepository motivoNoDisposicionRepository;
    private final UserRepository userRepository;
    private final EncuestadorRepository encuestadorRepository;
    private final BarrioRepository barrioRepository;
    private final VentanillaRegistroRepository ventanillaRegistroRepository;
    private final AuditService auditService;

    /**
     * Consulta los registros activos visibles para el usuario autenticado.
     *
     * @param pageable configuración de paginación.
     * @return página de casos Call Center.
     */
    @Transactional(readOnly = true)
    public PageResponse<CallCenterResponse> findAll(Pageable pageable) {
        Page<CallCenterRegistro> page = findPageForCurrentUser(
                CallCenterRegistroSpecification.activeOnly(),
                pageable
        );

        return PageResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    /**
     * Busca registros Call Center aplicando filtros y permisos por rol.
     *
     * @param filter filtros de búsqueda.
     * @param pageable configuración de paginación.
     * @return página de casos filtrados.
     */
    @Transactional(readOnly = true)
    public PageResponse<CallCenterResponse> search(CallCenterFilterRequest filter, Pageable pageable) {
        Page<CallCenterRegistro> page = findPageForCurrentUser(
                CallCenterRegistroSpecification.byFilter(filter),
                pageable
        );

        return PageResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    /**
     * Lista las asignaciones visibles según el rol autenticado.
     *
     * <p>Para encuestadores retorna sus registros asignados o programados.
     * Para funcionarios Call Center retorna sus casos asignados. Para perfiles
     * administrativos retorna registros activos.</p>
     *
     * @param pageable configuración de paginación.
     * @return página de asignaciones.
     */
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

    /**
     * Lista casos pendientes de asignar a funcionario Call Center.
     *
     * @param pageable configuración de paginación.
     * @return casos pendientes de enrutamiento.
     */
    @Transactional(readOnly = true)
    public PageResponse<CallCenterResponse> pendientesAsignarFuncionario(Pageable pageable) {
        Page<CallCenterRegistro> page = repository.findAll(
                CallCenterRegistroSpecification.pendientesAsignarFuncionarioCallcenter(),
                pageable
        );

        return PageResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    /**
     * Lista los registros asignados al funcionario Call Center autenticado
     * aplicando filtros dinámicos desde base de datos.
     *
     * <p>Si el usuario autenticado es FUNCIONARIO_CALLCENTER, la consulta se
     * limita a los casos asignados a ese usuario. Para perfiles administrativos
     * se consultan los registros activos que cumplan los filtros.</p>
     *
     * @param filter filtros de búsqueda.
     * @param pageable configuración de paginación.
     * @return página de registros del funcionario.
     */
    @Transactional(readOnly = true)
    public PageResponse<CallCenterResponse> misRegistrosCallcenter(
            CallCenterFilterRequest filter,
            Pageable pageable
    ) {
        Specification<CallCenterRegistro> specification = CallCenterRegistroSpecification.byFilter(filter);

        if (currentUserHasRole("FUNCIONARIO_CALLCENTER")) {
            User user = currentUser();

            specification = specification.and(
                    CallCenterRegistroSpecification.byFuncionarioCallcenterAsignado(user.getId())
            );
        }

        Page<CallCenterRegistro> page = repository.findAll(specification, pageable);

        return PageResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    /**
     * Consulta un caso por identificador.
     *
     * @param id identificador del caso.
     * @return caso encontrado.
     */
    @Transactional(readOnly = true)
    public CallCenterResponse findById(Long id) {
        CallCenterRegistro entity = findEntity(id);
        validateCurrentFuncionarioCallcenterCanAccess(entity);

        return toResponse(entity);
    }

    /**
     * Lista motivos activos de no contacto.
     *
     * @return catálogo de motivos de no contacto.
     */
    @Transactional(readOnly = true)
    public List<CallCenterCatalogResponse> findMotivosNoContacto() {
        return motivoNoContactoRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(this::toCatalogResponse)
                .toList();
    }

    /**
     * Lista motivos activos de no disposición.
     *
     * @return catálogo de motivos de no disposición.
     */
    @Transactional(readOnly = true)
    public List<CallCenterCatalogResponse> findMotivosNoDisposicion() {
        return motivoNoDisposicionRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(this::toCatalogResponse)
                .toList();
    }

    /**
     * Lista usuarios activos con rol FUNCIONARIO_CALLCENTER.
     *
     * @return funcionarios Call Center disponibles.
     */
    @Transactional(readOnly = true)
    public List<CallCenterUserOptionResponse> findFuncionariosCallcenter() {
        return userRepository.findAll()
                .stream()
                .filter(this::isUserActivo)
                .filter(user -> hasRoleCode(user, "FUNCIONARIO_CALLCENTER"))
                .map(this::toUserOptionResponse)
                .toList();
    }

    /**
     * Construye el resumen de registros visibles para el usuario autenticado.
     *
     * @return resumen general de Call Center.
     */
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

    /**
     * Crea un caso Call Center.
     *
     * <p>Cuando el caso nace desde Ventanilla o desde carga manual administrativa,
     * se inicializa como pendiente de enrutamiento. Si el usuario autenticado es
     * funcionario Call Center, el caso puede quedar asignado directamente a él.</p>
     *
     * @param request datos del caso.
     * @return caso creado.
     */
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
        entity.setEstadoCaso("PENDIENTE_ENRUTAMIENTO");
        entity.setTipoSolicitudCallcenter("NUEVA_ENCUESTA");

        if (currentUserHasRole("FUNCIONARIO_CALLCENTER")) {
            entity.setFuncionarioCallcenterAsignado(user);
            entity.setFechaAsignacionCallcenter(LocalDateTime.now());
            entity.setUsuarioAsignaCallcenter(user);
            entity.setEstadoCaso("ASIGNADO_CALLCENTER");
        }

        apply(entity, request, user);

        if (currentUserHasRole("FUNCIONARIO_CALLCENTER") && !isClosedOrCancelled(entity.getEstadoCaso())) {
            entity.setEstadoCaso("ASIGNADO_CALLCENTER");
        }

        CallCenterRegistro saved = repository.save(entity);

        auditService.safeLog(AuditAction.CREATE, TABLE_NAME, saved.getId(), null, snapshot(saved));

        return toResponse(saved);
    }

    /**
     * Actualiza un caso Call Center.
     *
     * <p>No permite modificar casos cerrados o cancelados.</p>
     *
     * @param id identificador del caso.
     * @param request datos a actualizar.
     * @return caso actualizado.
     */
    @Transactional
    public CallCenterResponse update(Long id, CallCenterRequest request) {
        CallCenterRegistro entity = findEntity(id);
        validateCurrentFuncionarioCallcenterCanAccess(entity);
        validateCaseIsOpen(entity, "No se puede actualizar un caso cerrado o cancelado");

        Map<String, Object> before = snapshot(entity);

        apply(entity, request, currentUser());

        auditService.safeLog(AuditAction.UPDATE, TABLE_NAME, entity.getId(), before, snapshot(entity));

        return toResponse(entity);
    }

    /**
     * Asigna casos Call Center a un funcionario Call Center.
     *
     * <p>Esta acción corresponde al Coordinador / Enrutador. Al asignar el caso,
     * el estado formal cambia a ASIGNADO_CALLCENTER.</p>
     *
     * @param request datos de asignación.
     * @return registros actualizados.
     */
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
            registro.setEstadoCaso("ASIGNADO_CALLCENTER");
            registro.setActualizadoPor(user);

            auditService.safeLog(AuditAction.UPDATE, TABLE_NAME, registro.getId(), before, snapshot(registro));
        }

        return registros.stream().map(this::toResponse).toList();
    }

    /**
     * Asigna encuestador sobre el registro maestro legacy.
     *
     * <p>Este método se conserva para compatibilidad con flujos anteriores.
     * El flujo formal recomendado de visitas está en {@link CallCenterWorkflowService}.</p>
     *
     * @param request datos de asignación.
     * @return registros actualizados.
     */
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

    /**
     * Actualiza datos de visita legacy directamente en el caso maestro.
     *
     * <p>No permite actualizar visitas de casos cerrados o cancelados. Si la
     * visita se marca como REALIZADA o encuestaRealizada = true, cierra el caso
     * maestro para mantener consistencia con el flujo formal.</p>
     *
     * @param id identificador del caso.
     * @param request datos de visita.
     * @return caso actualizado.
     */
    @Transactional
    public CallCenterResponse updateVisita(Long id, CallCenterVisitaRequest request) {
        CallCenterRegistro entity = findEntity(id);
        User user = currentUser();

        validateCurrentFuncionarioCallcenterCanAccess(entity);
        validateCaseIsOpen(entity, "No se puede actualizar la visita de un caso cerrado o cancelado");

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

        closeCaseFromLegacyVisitIfNeeded(entity, user);

        entity.setActualizadoPor(user);

        auditService.safeLog(AuditAction.UPDATE, TABLE_NAME, entity.getId(), before, snapshot(entity));

        return toResponse(entity);
    }

    /**
     * Activa un caso Call Center.
     *
     * @param id identificador del caso.
     * @return caso actualizado.
     */
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

    /**
     * Inactiva un caso Call Center.
     *
     * @param id identificador del caso.
     * @return caso actualizado.
     */
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

    /**
     * Elimina lógicamente un caso Call Center.
     *
     * @param id identificador del caso.
     */
    @Transactional
    public void delete(Long id) {
        deactivate(id);
    }

    /**
     * Aplica los datos recibidos en el request sobre la entidad Call Center.
     *
     * <p>Este método actualiza datos generales del ciudadano, origen del caso,
     * información legacy de llamada/visita y campos del flujo formal.</p>
     *
     * @param entity entidad destino.
     * @param request datos enviados por frontend.
     * @param user usuario que realiza la operación.
     */
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

        entity.setEstadoCaso(normalizeEstadoCaso(request.estadoCaso(), entity.getEstadoCaso()));
        entity.setTipoSolicitudCallcenter(
                normalizeTipoSolicitudCallcenter(
                        request.tipoSolicitudCallcenter(),
                        request.solicitoNuevaEncuesta()
                )
        );

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

    /**
     * Valida coherencia general del request antes de aplicarlo.
     *
     * @param entity entidad actual.
     * @param request datos recibidos.
     */
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

    /**
     * Valida que no exista otra nueva encuesta pendiente para el mismo ciudadano
     * con encuestador asignado.
     *
     * @param entity entidad actual.
     * @param request datos recibidos.
     */
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

    /**
     * Valida que un registro pendiente no cambie de encuestador mientras
     * la encuesta no esté realizada.
     *
     * @param entity entidad actual.
     * @param request datos recibidos.
     */
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

        if (
                changedAsignado
                        || changedProgramado
                        || assigningNewWhenOnlyProgrammedExists
                        || programmingNewWhenOnlyAssignedExists
        ) {
            throw new BusinessException(
                    "Este usuario ya tiene una nueva encuesta pendiente asignada al encuestador "
                            + getNombreEncuestadorAsignado(entity)
                            + ". Mientras la encuesta no esté marcada como realizada, no puede asignarse a otro encuestador."
            );
        }
    }

    /**
     * Valida que un registro pueda ser asignado a un funcionario Call Center.
     *
     * @param registro caso Call Center.
     * @param funcionario funcionario destino.
     */
    private void validateRegistroAsignableAFuncionarioCallcenter(CallCenterRegistro registro, User funcionario) {
        if (!Boolean.TRUE.equals(registro.getActivo())) {
            throw new BusinessException("Solo se pueden asignar registros activos");
        }

        if (isClosedOrCancelled(registro.getEstadoCaso())) {
            throw new BusinessException("No se puede asignar un caso cerrado o cancelado");
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

    /**
     * Valida que un registro pueda ser asignado a un encuestador.
     *
     * @param registro caso Call Center.
     * @param encuestador encuestador destino.
     * @param user usuario autenticado.
     */
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

        if (isClosedOrCancelled(registro.getEstadoCaso())) {
            throw new BusinessException("No se puede asignar encuestador a un caso cerrado o cancelado");
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

    /**
     * Aplica el filtro base correspondiente al usuario autenticado.
     *
     * @param specification especificación base.
     * @param pageable configuración de paginación.
     * @return página filtrada por permisos.
     */
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

    /**
     * Obtiene registros usados para el resumen según el usuario autenticado.
     *
     * @return registros visibles para resumen.
     */
    private List<CallCenterRegistro> findSummaryRecordsForCurrentUser() {
        if (currentUserHasRole("FUNCIONARIO_CALLCENTER")) {
            User user = currentUser();

            return repository.findAll(
                    CallCenterRegistroSpecification.byFuncionarioCallcenterAsignado(user.getId())
            );
        }

        return repository.findAll();
    }

    /**
     * Valida que el funcionario Call Center solo acceda a registros asignados.
     *
     * @param registro caso Call Center.
     */
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

    /**
     * Busca la entidad principal de Call Center.
     *
     * @param id identificador.
     * @return entidad encontrada.
     */
    private CallCenterRegistro findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro Call Center no encontrado"));
    }

    /**
     * Busca un motivo de no contacto.
     *
     * @param id identificador.
     * @return motivo encontrado o null.
     */
    private CallCenterMotivoNoContacto findMotivoNoContacto(Long id) {
        if (id == null) {
            return null;
        }

        return motivoNoContactoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Motivo de no contacto no encontrado"));
    }

    /**
     * Busca un motivo de no disposición.
     *
     * @param id identificador.
     * @return motivo encontrado o null.
     */
    private CallCenterMotivoNoDisposicion findMotivoNoDisposicion(Long id) {
        if (id == null) {
            return null;
        }

        return motivoNoDisposicionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Motivo de no disposición no encontrado"));
    }

    /**
     * Busca un encuestador.
     *
     * @param id identificador.
     * @return encuestador encontrado o null.
     */
    private Encuestador findEncuestador(Long id) {
        if (id == null) {
            return null;
        }

        return encuestadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encuestador no encontrado"));
    }

    /**
     * Busca un barrio.
     *
     * @param id identificador.
     * @return barrio encontrado o null.
     */
    private Barrio findBarrio(Long id) {
        if (id == null) {
            return null;
        }

        return barrioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Barrio no encontrado"));
    }

    /**
     * Busca un registro de Ventanilla.
     *
     * @param id identificador.
     * @return registro de Ventanilla encontrado o null.
     */
    private VentanillaRegistro findVentanillaRegistro(Long id) {
        if (id == null) {
            return null;
        }

        return ventanillaRegistroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de ventanilla no encontrado"));
    }

    /**
     * Busca un usuario por identificador.
     *
     * @param id identificador.
     * @return usuario encontrado.
     */
    private User findUser(Long id) {
        if (id == null) {
            throw new BusinessException("Debe seleccionar un usuario");
        }

        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    /**
     * Obtiene el usuario autenticado desde Spring Security.
     *
     * @return usuario autenticado.
     */
    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            throw new BusinessException("No hay usuario autenticado");
        }

        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    /**
     * Obtiene el encuestador vinculado al usuario autenticado por documento.
     *
     * @return encuestador autenticado.
     */
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

    /**
     * Verifica si el usuario autenticado tiene un rol específico.
     *
     * <p>Primero revisa las authorities del token y luego valida contra el rol
     * persistido del usuario en base de datos.</p>
     *
     * @param roleCode código del rol.
     * @return true si el usuario tiene el rol indicado.
     */
    private boolean currentUserHasRole(String roleCode) {
        boolean byAuthority = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(authority -> roleCode.equals(authority.getAuthority())
                        || ("ROLE_" + roleCode).equals(authority.getAuthority()));

        if (byAuthority) {
            return true;
        }

        try {
            return hasRoleCode(currentUser(), roleCode);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * Valida si un usuario tiene un rol determinado.
     *
     * @param user usuario.
     * @param roleCode código de rol.
     * @return true si coincide.
     */
    private boolean hasRoleCode(User user, String roleCode) {
        return user != null
                && user.getRole() != null
                && roleCode.equalsIgnoreCase(user.getRole().getCodigo());
    }

    /**
     * Normaliza el tipo de registro.
     *
     * @param tipoRegistro tipo recibido.
     * @return tipo normalizado.
     */
    private String normalizeTipoRegistro(String tipoRegistro) {
        String value = hasText(tipoRegistro)
                ? tipoRegistro.trim().toUpperCase(Locale.ROOT)
                : "LLAMADA";

        if ("LLAMADA".equals(value) || "BASE_ENCUESTADOR".equals(value)) {
            return value;
        }

        throw new BusinessException("Tipo de registro no válido");
    }

    /**
     * Normaliza el origen del registro.
     *
     * @param origenRegistro origen recibido.
     * @param ventanillaRegistro registro de Ventanilla asociado.
     * @return origen normalizado.
     */
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

    /**
     * Normaliza el estado de visita legacy.
     *
     * @param estadoVisita estado recibido.
     * @return estado normalizado.
     */
    private String cleanEstadoVisita(String estadoVisita) {
        String value = hasText(estadoVisita)
                ? estadoVisita.trim().toUpperCase(Locale.ROOT)
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

        throw new BusinessException("Estado de visita no válido");
    }

    /**
     * Obtiene el nombre del encuestador asignado o programado.
     *
     * @param entity caso Call Center.
     * @return nombre del encuestador.
     */
    private String getNombreEncuestadorAsignado(CallCenterRegistro entity) {
        if (entity.getEncuestadorAsignado() != null && hasText(entity.getEncuestadorAsignado().getNombre())) {
            return entity.getEncuestadorAsignado().getNombre();
        }

        if (entity.getEncuestadorProgramado() != null && hasText(entity.getEncuestadorProgramado().getNombre())) {
            return entity.getEncuestadorProgramado().getNombre();
        }

        return "sin encuestador registrado";
    }

    /**
     * Construye nombre completo de usuario.
     *
     * @param user usuario.
     * @return nombre completo o username.
     */
    private String fullName(User user) {
        if (user == null) {
            return null;
        }

        String nombres = user.getNombres() != null ? user.getNombres() : "";
        String apellidos = user.getApellidos() != null ? user.getApellidos() : "";
        String nombreCompleto = (nombres + " " + apellidos).trim();

        return nombreCompleto.isBlank() ? user.getUsername() : nombreCompleto;
    }

    /**
     * Convierte usuario a opción de selección.
     *
     * @param user usuario.
     * @return opción de usuario.
     */
    private CallCenterUserOptionResponse toUserOptionResponse(User user) {
        return new CallCenterUserOptionResponse(
                user.getId(),
                user.getUsername(),
                fullName(user),
                user.getRole() != null ? user.getRole().getCodigo() : null,
                isUserActivo(user)
        );
    }

    /**
     * Valida si un usuario se considera activo.
     *
     * @param user usuario.
     * @return true si está activo.
     */
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

    /**
     * Convierte una entidad CallCenterRegistro en DTO de respuesta.
     *
     * <p>Incluye información del caso maestro, datos del ciudadano, trazabilidad
     * de asignación, datos legacy de visita y campos del flujo formal.</p>
     *
     * @param entity entidad de Call Center.
     * @return DTO de respuesta para frontend.
     */
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

                entity.getEstadoCaso(),
                entity.getTipoSolicitudCallcenter(),
                entity.getFechaCierre(),
                entity.getMotivoCierre(),
                entity.getUsuarioCierre() != null ? entity.getUsuarioCierre().getId() : null,
                entity.getUsuarioCierre() != null ? entity.getUsuarioCierre().getUsername() : null,

                entity.getActivo()
        );
    }

    /**
     * Convierte motivo de no contacto a DTO de catálogo.
     *
     * @param entity entidad.
     * @return catálogo.
     */
    private CallCenterCatalogResponse toCatalogResponse(CallCenterMotivoNoContacto entity) {
        return new CallCenterCatalogResponse(
                entity.getId(),
                entity.getCodigo(),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.getActivo()
        );
    }

    /**
     * Convierte motivo de no disposición a DTO de catálogo.
     *
     * @param entity entidad.
     * @return catálogo.
     */
    private CallCenterCatalogResponse toCatalogResponse(CallCenterMotivoNoDisposicion entity) {
        return new CallCenterCatalogResponse(
                entity.getId(),
                entity.getCodigo(),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.getActivo()
        );
    }

    /**
     * Construye una fotografía de datos para auditoría.
     *
     * @param entity entidad auditada.
     * @return mapa con datos relevantes.
     */
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
        data.put("estadoCaso", entity.getEstadoCaso());
        data.put("tipoSolicitudCallcenter", entity.getTipoSolicitudCallcenter());
        data.put("fechaCierre", entity.getFechaCierre());
        data.put("motivoCierre", entity.getMotivoCierre());
        data.put("usuarioCierreId", entity.getUsuarioCierre() != null ? entity.getUsuarioCierre().getId() : null);
        data.put("usuarioCierreUsername", entity.getUsuarioCierre() != null ? entity.getUsuarioCierre().getUsername() : null);
        data.put("activo", entity.getActivo());

        return data;
    }

    /**
     * Normaliza el estado formal del caso Call Center.
     *
     * @param estadoCaso estado recibido desde frontend.
     * @param currentEstadoCaso estado actual de la entidad.
     * @return estado normalizado.
     */
    private String normalizeEstadoCaso(String estadoCaso, String currentEstadoCaso) {
        String value = hasText(estadoCaso)
                ? estadoCaso.trim().toUpperCase(Locale.ROOT)
                : clean(currentEstadoCaso);

        if (!hasText(value)) {
            return "PENDIENTE_ENRUTAMIENTO";
        }

        if (
                value.equals("PENDIENTE_ENRUTAMIENTO")
                        || value.equals("ASIGNADO_CALLCENTER")
                        || value.equals("EN_GESTION_LLAMADA")
                        || value.equals("NO_CONTACTADO")
                        || value.equals("CONTACTADO_SIN_DISPOSICION")
                        || value.equals("PENDIENTE_ASIGNAR_ENCUESTADOR")
                        || value.equals("ASIGNADO_ENCUESTADOR")
                        || value.equals("VISITA_PROGRAMADA")
                        || value.equals("VISITA_REALIZADA")
                        || value.equals("VISITA_NO_ATENDIDA")
                        || value.equals("REPROGRAMADO")
                        || value.equals(ESTADO_CERRADO)
                        || value.equals(ESTADO_CANCELADO)
        ) {
            return value;
        }

        throw new BusinessException("Estado de caso Call Center no válido");
    }

    /**
     * Normaliza el tipo de solicitud del caso Call Center.
     *
     * @param tipoSolicitudCallcenter tipo recibido desde frontend.
     * @param solicitoNuevaEncuesta indicador legacy de nueva encuesta.
     * @return tipo de solicitud normalizado.
     */
    private String normalizeTipoSolicitudCallcenter(
            String tipoSolicitudCallcenter,
            Boolean solicitoNuevaEncuesta
    ) {
        String value = hasText(tipoSolicitudCallcenter)
                ? tipoSolicitudCallcenter.trim().toUpperCase(Locale.ROOT)
                : null;

        if (!hasText(value) && Boolean.TRUE.equals(solicitoNuevaEncuesta)) {
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

        throw new BusinessException("Tipo de solicitud Call Center no válido");
    }

    /**
     * Valida que un caso pueda ser modificado operativamente.
     *
     * @param registro caso Call Center.
     * @param message mensaje de error.
     */
    private void validateCaseIsOpen(CallCenterRegistro registro, String message) {
        if (isClosedOrCancelled(registro.getEstadoCaso())) {
            throw new BusinessException(message);
        }
    }

    /**
     * Cierra el caso maestro cuando la visita legacy marca encuesta realizada.
     *
     * <p>Este método protege el flujo antiguo updateVisita para que tenga el
     * mismo comportamiento del flujo formal de visitas.</p>
     *
     * @param entity caso maestro.
     * @param user usuario que realiza el cierre.
     */
    private void closeCaseFromLegacyVisitIfNeeded(CallCenterRegistro entity, User user) {
        String estadoVisita = hasText(entity.getEstadoVisita())
                ? entity.getEstadoVisita().trim().toUpperCase(Locale.ROOT)
                : "";

        boolean shouldClose = Boolean.TRUE.equals(entity.getEncuestaRealizada())
                || "REALIZADA".equals(estadoVisita);

        if (!shouldClose) {
            return;
        }

        entity.setEstadoCaso(ESTADO_CERRADO);
        entity.setFechaCierre(LocalDateTime.now());
        entity.setMotivoCierre(MOTIVO_CIERRE_ENCUESTA_REALIZADA);
        entity.setUsuarioCierre(user);
    }

    /**
     * Indica si un caso está cerrado o cancelado.
     *
     * @param estadoCaso estado formal del caso.
     * @return true si el caso no permite nuevas modificaciones operativas.
     */
    private boolean isClosedOrCancelled(String estadoCaso) {
        String value = hasText(estadoCaso)
                ? estadoCaso.trim().toUpperCase(Locale.ROOT)
                : "";

        return ESTADO_CERRADO.equals(value) || ESTADO_CANCELADO.equals(value);
    }

    /**
     * Limpia texto convirtiendo valores vacíos en null.
     *
     * @param value valor recibido.
     * @return texto limpio o null.
     */
    private String clean(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * Limpia, compacta y convierte texto a mayúscula.
     *
     * @param value valor recibido.
     * @return texto normalizado en mayúscula o null.
     */
    private String upper(String value) {
        return hasText(value) ? value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT) : null;
    }

    /**
     * Valida si un texto contiene caracteres diferentes a espacios.
     *
     * @param value valor recibido.
     * @return true si contiene texto.
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }
}