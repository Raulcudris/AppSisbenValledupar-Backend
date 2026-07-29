package com.appsisben.backend.modules.callcenter.application;
import com.appsisben.backend.modules.callcenter.domain.CallCenterGestionLlamada;
import com.appsisben.backend.modules.callcenter.domain.CallCenterMotivoNoContacto;
import com.appsisben.backend.modules.callcenter.domain.CallCenterMotivoNoDisposicion;
import com.appsisben.backend.modules.callcenter.domain.CallCenterRegistro;
import com.appsisben.backend.modules.callcenter.domain.CallCenterResultadoLlamada;
import com.appsisben.backend.modules.callcenter.domain.CallCenterVisita;
import com.appsisben.backend.modules.callcenter.dto.*;
import com.appsisben.backend.modules.callcenter.repository.*;
import com.appsisben.backend.modules.catalogs.domain.Encuestador;
import com.appsisben.backend.modules.catalogs.repository.EncuestadorRepository;
import com.appsisben.backend.modules.users.domain.User;
import com.appsisben.backend.modules.users.repository.UserRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Servicio de flujo formal para el módulo Call Center.
 *
 * <p>Este servicio complementa el servicio principal de Call Center y separa
 * la trazabilidad operativa en dos procesos: gestiones de llamada y visitas
 * de encuestadores. El caso maestro sigue siendo {@link CallCenterRegistro}.</p>
 */
@Service
@RequiredArgsConstructor
public class CallCenterWorkflowService {

    private static final String ESTADO_ASIGNADO_ENCUESTADOR = "ASIGNADO_ENCUESTADOR";
    private static final String ESTADO_VISITA_PROGRAMADA = "VISITA_PROGRAMADA";
    private static final String ESTADO_VISITA_NO_ATENDIDA = "VISITA_NO_ATENDIDA";
    private static final String ESTADO_REPROGRAMADO = "REPROGRAMADO";
    private static final String ESTADO_CANCELADO = "CANCELADO";
    private static final String ESTADO_CERRADO = "CERRADO";
    private static final String MOTIVO_CIERRE_ENCUESTA_REALIZADA = "Encuesta realizada por encuestador";

    private final CallCenterRegistroRepository callCenterRegistroRepository;
    private final CallCenterGestionLlamadaRepository gestionLlamadaRepository;
    private final CallCenterVisitaRepository visitaRepository;
    private final CallCenterResultadoLlamadaRepository resultadoLlamadaRepository;
    private final CallCenterMotivoNoContactoRepository motivoNoContactoRepository;
    private final CallCenterMotivoNoDisposicionRepository motivoNoDisposicionRepository;
    private final EncuestadorRepository encuestadorRepository;
    private final UserRepository userRepository;

    /**
     * Lista los resultados de llamada activos.
     *
     * @return catálogo de resultados de llamada.
     */
    @Transactional(readOnly = true)
    public List<CallCenterResultadoLlamadaResponse> findResultadosLlamada() {
        return resultadoLlamadaRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(this::toResultadoResponse)
                .toList();
    }

    /**
     * Lista las gestiones de llamada de un caso maestro.
     *
     * @param callCenterRegistroId identificador del caso.
     * @return gestiones de llamada del caso.
     */
    @Transactional(readOnly = true)
    public List<CallCenterGestionLlamadaResponse> findLlamadasByCaso(Long callCenterRegistroId) {
        CallCenterRegistro registro = findRegistro(callCenterRegistroId);
        validateCanViewCase(registro, currentUser());

        return gestionLlamadaRepository
                .findByCallCenterRegistroIdAndActivoTrueOrderByIntentoNumeroAscIdAsc(callCenterRegistroId)
                .stream()
                .map(this::toGestionResponse)
                .toList();
    }

    /**
     * Registra una nueva gestión de llamada en un caso maestro.
     *
     * @param callCenterRegistroId identificador del caso.
     * @param request datos de la gestión.
     * @return gestión creada.
     */
    @Transactional
    public CallCenterGestionLlamadaResponse registrarLlamada(
            Long callCenterRegistroId,
            CallCenterGestionLlamadaRequest request
    ) {
        CallCenterRegistro registro = findRegistro(callCenterRegistroId);
        User user = currentUser();

        validateCanManageCallCenterCase(registro, user);

        String resultadoCodigo = normalizeRequired(request.resultadoLlamada(), "El resultado de la llamada es obligatorio");

        CallCenterResultadoLlamada resultado = resultadoLlamadaRepository
                .findFirstByCodigoIgnoreCaseAndActivoTrue(resultadoCodigo)
                .orElseThrow(() -> new BusinessException("El resultado de llamada seleccionado no existe o está inactivo"));

        Boolean llamadaConectada = Boolean.TRUE.equals(request.llamadaConectada());
        CallCenterMotivoNoContacto motivoNoContacto = findMotivoNoContacto(request.motivoNoContactoId());
        CallCenterMotivoNoDisposicion motivoNoDisposicion = findMotivoNoDisposicion(request.motivoNoDisposicionId());

        validateLlamadaRequest(resultadoCodigo, llamadaConectada, motivoNoContacto, motivoNoDisposicion);

        long intentosPrevios = gestionLlamadaRepository.countByCallCenterRegistroId(callCenterRegistroId);

        CallCenterGestionLlamada entity = new CallCenterGestionLlamada();
        entity.setCallCenterRegistro(registro);
        entity.setFuncionarioCallcenter(user);
        entity.setFechaLlamada(request.fechaLlamada() != null ? request.fechaLlamada() : LocalDate.now());
        entity.setHoraLlamada(request.horaLlamada() != null ? request.horaLlamada() : LocalTime.now().withNano(0));
        entity.setIntentoNumero((int) intentosPrevios + 1);
        entity.setLlamadaConectada(llamadaConectada);
        entity.setResultadoLlamada(resultado.getCodigo());
        entity.setMotivoNoContacto(motivoNoContacto);
        entity.setMotivoNoDisposicion(motivoNoDisposicion);
        entity.setFechaReprogramacionLlamada(request.fechaReprogramacionLlamada());
        entity.setHoraReprogramacionLlamada(request.horaReprogramacionLlamada());
        entity.setObservacion(trimToNull(request.observacion()));
        entity.setCreadoPor(user);
        entity.setActualizadoPor(user);

        CallCenterGestionLlamada saved = gestionLlamadaRepository.save(entity);

        applyLlamadaToRegistro(registro, saved, resultado);
        callCenterRegistroRepository.save(registro);

        return toGestionResponse(saved);
    }

    /**
     * Lista las visitas registradas para un caso maestro.
     *
     * @param callCenterRegistroId identificador del caso.
     * @return visitas del caso.
     */
    @Transactional(readOnly = true)
    public List<CallCenterVisitaResponse> findVisitasByCaso(Long callCenterRegistroId) {
        CallCenterRegistro registro = findRegistro(callCenterRegistroId);
        validateCanViewCase(registro, currentUser());

        return visitaRepository
                .findByCallCenterRegistroIdAndActivoTrueOrderByFechaAsignacionDescIdDesc(callCenterRegistroId)
                .stream()
                .map(this::toVisitaResponse)
                .toList();
    }

    /**
     * Asigna un caso maestro a un encuestador para visita en campo.
     *
     * @param callCenterRegistroId identificador del caso.
     * @param request datos de asignación.
     * @return visita creada.
     */
    @Transactional
    public CallCenterVisitaResponse asignarVisita(
            Long callCenterRegistroId,
            CallCenterVisitaAsignacionRequest request
    ) {
        CallCenterRegistro registro = findRegistro(callCenterRegistroId);
        User user = currentUser();

        validateCanManageCallCenterCase(registro, user);

        Encuestador encuestador = encuestadorRepository.findById(request.encuestadorId())
                .orElseThrow(() -> new ResourceNotFoundException("Encuestador no encontrado"));

        CallCenterVisita visita = new CallCenterVisita();
        visita.setCallCenterRegistro(registro);
        visita.setEncuestador(encuestador);
        visita.setUsuarioAsigna(user);
        visita.setFechaProgramada(request.fechaProgramada());
        visita.setHoraProgramada(request.horaProgramada());
        visita.setEstadoVisita(request.fechaProgramada() != null ? "PROGRAMADA" : "PENDIENTE");
        visita.setObservacionEncuestador(trimToNull(request.observacion()));
        visita.setCreadoPor(user);
        visita.setActualizadoPor(user);

        CallCenterVisita saved = visitaRepository.save(visita);

        registro.setEncuestadorAsignado(encuestador);
        registro.setEncuestadorProgramado(encuestador);
        registro.setFechaEncuestaProgramada(request.fechaProgramada());
        registro.setEstadoVisita(saved.getEstadoVisita());
        registro.setEstadoCaso(request.fechaProgramada() != null ? ESTADO_VISITA_PROGRAMADA : ESTADO_ASIGNADO_ENCUESTADOR);
        registro.setActualizadoPor(user);

        callCenterRegistroRepository.save(registro);

        return toVisitaResponse(saved);
    }

    /**
     * Lista las visitas asignadas al usuario autenticado cuando es encuestador.
     * Para perfiles administrativos retorna todas las visitas activas.
     *
     * <p>La consulta aplica filtros dinámicos sobre la visita y sobre el caso
     * maestro asociado. Esto permite que la pantalla de Mis asignaciones filtre
     * desde base de datos y no solamente sobre los registros cargados en la página.</p>
     *
     * @param filter filtros de búsqueda.
     * @param pageable configuración de paginación.
     * @return página de visitas.
     */
    @Transactional(readOnly = true)
    public PageResponse<CallCenterVisitaResponse> misVisitas(
            CallCenterVisitaFilterRequest filter,
            Pageable pageable
    ) {
        User user = currentUser();

        Specification<CallCenterVisita> specification = CallCenterVisitaSpecification
                .activeOnly()
                .and(CallCenterVisitaSpecification.byFilter(filter));

        if (currentUserHasRole("FUNCIONARIO_ENCUESTADOR")) {
            Encuestador encuestador = currentEncuestador(user);

            specification = specification.and(
                    CallCenterVisitaSpecification.byEncuestador(encuestador.getId())
            );
        }

        Page<CallCenterVisita> page = visitaRepository.findAll(specification, pageable);

        return PageResponse.from(
                page,
                page.getContent().stream().map(this::toVisitaResponse).toList()
        );
    }

    /**
     * Actualiza el resultado operativo de una visita de encuestador.
     *
     * <p>Si la visita queda realizada o se marca que la encuesta fue realizada,
     * también se cierra el caso maestro en {@link CallCenterRegistro}. Esto evita
     * que el o se marca que la encuesta fue realizada,
     * también se cierra el caso maestro en {@link CallCenterRegistro}. Esto evita
     * que el ciudadano siga apareciendo como caso abierto o vuelva a ser asignado.</p>
     *
     * <p>No permite actualizar resultados de visita cuando el caso ya está
     * cerrado o cancelado.</p>
     *
     * @param visitaId identificador de la visita.
     * @param request datos del resultado.
     * @return visita actualizada.
     */
    @Transactional
    public CallCenterVisitaResponse actualizarResultadoVisita(
            Long visitaId,
            CallCenterVisitaResultadoRequest request
    ) {
        CallCenterVisita visita = visitaRepository.findById(visitaId)
                .orElseThrow(() -> new ResourceNotFoundException("Visita de Call Center no encontrada"));

        User user = currentUser();
        validateCanUpdateVisit(visita, user);

        CallCenterRegistro registro = visita.getCallCenterRegistro();
        validateCaseIsOpen(registro, "No se puede actualizar el resultado de visita de un caso cerrado o cancelado");

        String estadoVisita = normalizeRequired(request.estadoVisita(), "El estado de la visita es obligatorio");
        validateEstadoVisita(estadoVisita);

        visita.setEstadoVisita(estadoVisita);
        visita.setFechaVisitaReal(request.fechaVisitaReal() != null ? request.fechaVisitaReal() : LocalDate.now());
        visita.setHoraVisitaReal(request.horaVisitaReal() != null ? request.horaVisitaReal() : LocalTime.now().withNano(0));
        visita.setEncuestaRealizada(request.encuestaRealizada());
        visita.setMotivoNoEncuesta(trimToNull(request.motivoNoEncuesta()));
        visita.setFechaReprogramacion(request.fechaReprogramacion());
        visita.setObservacionEncuestador(trimToNull(request.observacionEncuestador()));
        visita.setActualizadoPor(user);

        CallCenterVisita saved = visitaRepository.save(visita);

        registro.setEstadoVisita(saved.getEstadoVisita());
        registro.setFechaVisitaReal(saved.getFechaVisitaReal());
        registro.setHoraVisitaReal(saved.getHoraVisitaReal());
        registro.setEncuestaRealizada(saved.getEncuestaRealizada());
        registro.setMotivoNoEncuesta(saved.getMotivoNoEncuesta());
        registro.setFechaReprogramacion(saved.getFechaReprogramacion());
        registro.setObservacionEncuestador(saved.getObservacionEncuestador());
        registro.setEstadoCaso(resolveEstadoCasoFromVisita(saved));
        registro.setActualizadoPor(user);

        if (shouldCloseCaseFromVisita(saved)) {
            registro.setEstadoCaso(ESTADO_CERRADO);
            registro.setFechaCierre(LocalDateTime.now());
            registro.setMotivoCierre(MOTIVO_CIERRE_ENCUESTA_REALIZADA);
            registro.setUsuarioCierre(user);
        }

        callCenterRegistroRepository.save(registro);

        return toVisitaResponse(saved);
    }

    /**
     * Aplica los datos principales de la llamada al caso maestro para mantener
     * compatibilidad con pantallas y reportes existentes.
     *
     * @param registro caso maestro.
     * @param gestion gestión registrada.
     * @param resultado catálogo de resultado aplicado.
     */
    private void applyLlamadaToRegistro(
            CallCenterRegistro registro,
            CallCenterGestionLlamada gestion,
            CallCenterResultadoLlamada resultado
    ) {
        registro.setFechaLlamada(gestion.getFechaLlamada());
        registro.setHoraLlamada(gestion.getHoraLlamada());
        registro.setLlamadaConectada(gestion.getLlamadaConectada());
        registro.setMotivoNoContacto(gestion.getMotivoNoContacto());
        registro.setMotivoNoDisposicion(gestion.getMotivoNoDisposicion());
        registro.setObservacion(gestion.getObservacion());
        registro.setEstadoCaso(resolveEstadoCasoFromResultado(resultado, gestion.getLlamadaConectada()));
        registro.setActualizadoPor(gestion.getActualizadoPor());
    }

    /**
     * Resuelve el estado del caso maestro con base en el resultado de la llamada.
     *
     * @param resultado catálogo de resultado.
     * @param llamadaConectada indica si la llamada fue contestada.
     * @return estado del caso maestro.
     */
    private String resolveEstadoCasoFromResultado(
            CallCenterResultadoLlamada resultado,
            Boolean llamadaConectada
    ) {
        String sugerido = normalize(resultado.getEstadoCasoSugerido());

        if (!isBlank(sugerido)) {
            return sugerido;
        }

        return Boolean.TRUE.equals(llamadaConectada) ? "EN_GESTION_LLAMADA" : "NO_CONTACTADO";
    }

    /**
     * Resuelve el estado del caso maestro con base en el resultado de visita.
     *
     * <p>Cuando la visita queda realizada o la encuesta fue marcada como realizada,
     * el caso maestro debe cerrarse para evitar que siga apareciendo como caso abierto
     * o vuelva a ser asignado.</p>
     *
     * @param visita visita actualizada.
     * @return estado del caso maestro.
     */
    private String resolveEstadoCasoFromVisita(CallCenterVisita visita) {
        String estado = normalize(visita.getEstadoVisita());

        if (shouldCloseCaseFromVisita(visita)) {
            return ESTADO_CERRADO;
        }

        if ("NO_ATENDIDA".equals(estado)) {
            return ESTADO_VISITA_NO_ATENDIDA;
        }

        if ("REPROGRAMADA".equals(estado)) {
            return ESTADO_REPROGRAMADO;
        }

        if ("CANCELADA".equals(estado)) {
            return ESTADO_CANCELADO;
        }

        if ("PROGRAMADA".equals(estado)) {
            return ESTADO_VISITA_PROGRAMADA;
        }

        return ESTADO_ASIGNADO_ENCUESTADOR;
    }

    /**
     * Determina si el resultado de una visita debe cerrar el caso maestro.
     *
     * @param visita visita evaluada.
     * @return true si el caso debe cerrarse.
     */
    private boolean shouldCloseCaseFromVisita(CallCenterVisita visita) {
        String estado = normalize(visita.getEstadoVisita());

        return "REALIZADA".equals(estado) || Boolean.TRUE.equals(visita.getEncuestaRealizada());
    }

    /**
     * Valida coherencia mínima entre resultado, conexión y motivos.
     *
     * @param resultadoCodigo código de resultado.
     * @param llamadaConectada indica si la llamada fue contestada.
     * @param motivoNoContacto motivo de no contacto.
     * @param motivoNoDisposicion motivo de no disposición.
     */
    private void validateLlamadaRequest(
            String resultadoCodigo,
            Boolean llamadaConectada,
            CallCenterMotivoNoContacto motivoNoContacto,
            CallCenterMotivoNoDisposicion motivoNoDisposicion
    ) {
        if (!Boolean.TRUE.equals(llamadaConectada) && motivoNoContacto == null) {
            throw new BusinessException("Debe seleccionar el motivo de no contacto");
        }

        if ("CONTACTADO_NO_ACEPTA_VISITA".equals(resultadoCodigo) && motivoNoDisposicion == null) {
            throw new BusinessException("Debe seleccionar el motivo de no disposición");
        }
    }

    /**
     * Valida que el estado de visita sea permitido por el flujo.
     *
     * @param estado estado recibido.
     */
    private void validateEstadoVisita(String estado) {
        List<String> allowed = List.of(
                "PENDIENTE",
                "PROGRAMADA",
                "REALIZADA",
                "NO_ATENDIDA",
                "REPROGRAMADA",
                "CANCELADA"
        );

        if (!allowed.contains(estado)) {
            throw new BusinessException("Estado de visita no permitido: " + estado);
        }
    }

    /**
     * Valida permisos de visualización sobre un caso.
     *
     * @param registro caso maestro.
     * @param user usuario autenticado.
     */
    private void validateCanViewCase(CallCenterRegistro registro, User user) {
        if (currentUserHasAnyRole("ADMIN", "SUPERVISOR", "COORDINADOR_CALLCENTER")) {
            return;
        }

        if (currentUserHasRole("FUNCIONARIO_CALLCENTER")) {
            validateAssignedToFuncionarioCallCenter(registro, user);
            return;
        }

        if (currentUserHasRole("FUNCIONARIO_ENCUESTADOR")) {
            Encuestador encuestador = currentEncuestador(user);
            boolean assigned = Objects.equals(
                    registro.getEncuestadorAsignado() != null ? registro.getEncuestadorAsignado().getId() : null,
                    encuestador.getId()
            ) || Objects.equals(
                    registro.getEncuestadorProgramado() != null ? registro.getEncuestadorProgramado().getId() : null,
                    encuestador.getId()
            );

            if (!assigned) {
                throw new BusinessException("El caso no está asignado al encuestador autenticado");
            }

            return;
        }

        throw new BusinessException("No tiene permisos para consultar este caso");
    }

    /**
     * Valida permisos para gestionar llamadas y asignaciones sobre un caso.
     *
     * <p>Además de validar el rol, bloquea cualquier operación sobre casos
     * cerrados o cancelados.</p>
     *
     * @param registro caso maestro.
     * @param user usuario autenticado.
     */
    private void validateCanManageCallCenterCase(CallCenterRegistro registro, User user) {
        validateCaseIsOpen(registro, "No se puede gestionar un caso cerrado o cancelado");

        if (currentUserHasAnyRole("ADMIN", "SUPERVISOR", "COORDINADOR_CALLCENTER")) {
            return;
        }

        if (currentUserHasRole("FUNCIONARIO_CALLCENTER")) {
            validateAssignedToFuncionarioCallCenter(registro, user);
            return;
        }

        throw new BusinessException("No tiene permisos para gestionar este caso de Call Center");
    }

    /**
     * Valida que un funcionario Call Center solo gestione registros asignados a él.
     *
     * @param registro caso maestro.
     * @param user usuario autenticado.
     */
    private void validateAssignedToFuncionarioCallCenter(CallCenterRegistro registro, User user) {
        Long assignedId = registro.getFuncionarioCallcenterAsignado() != null
                ? registro.getFuncionarioCallcenterAsignado().getId()
                : null;

        if (!Objects.equals(assignedId, user.getId())) {
            throw new BusinessException("El caso no está asignado al funcionario Call Center autenticado");
        }
    }

    /**
     * Valida que el usuario pueda actualizar una visita.
     *
     * @param visita visita a actualizar.
     * @param user usuario autenticado.
     */
    private void validateCanUpdateVisit(CallCenterVisita visita, User user) {
        if (currentUserHasAnyRole("ADMIN", "SUPERVISOR")) {
            return;
        }

        if (currentUserHasRole("FUNCIONARIO_ENCUESTADOR")) {
            String documentoUsuario = normalize(user.getDocumento());
            String documentoEncuestador = normalize(visita.getEncuestador().getDocumento());

            if (!Objects.equals(documentoUsuario, documentoEncuestador)) {
                throw new BusinessException("La visita no está asignada al encuestador autenticado");
            }

            return;
        }

        throw new BusinessException("No tiene permisos para actualizar el resultado de la visita");
    }

    /**
     * Valida que un caso maestro esté abierto para permitir operaciones.
     *
     * @param registro caso maestro.
     * @param message mensaje de error.
     */
    private void validateCaseIsOpen(CallCenterRegistro registro, String message) {
        if (registro == null) {
            throw new BusinessException("La visita no tiene un caso maestro asociado");
        }

        if (isClosedOrCancelled(registro.getEstadoCaso())) {
            throw new BusinessException(message);
        }
    }

    /**
     * Indica si un estado formal corresponde a un cierre operativo.
     *
     * @param estadoCaso estado formal del caso.
     * @return true si el caso está cerrado o cancelado.
     */
    private boolean isClosedOrCancelled(String estadoCaso) {
        String estado = normalize(estadoCaso);

        return ESTADO_CERRADO.equals(estado) || ESTADO_CANCELADO.equals(estado);
    }

    /**
     * Obtiene el usuario autenticado desde Spring Security.
     *
     * @return usuario autenticado.
     */
    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    /**
     * Obtiene el encuestador asociado al usuario autenticado usando el documento.
     *
     * @param user usuario autenticado.
     * @return encuestador asociado.
     */
    private Encuestador currentEncuestador(User user) {
        String documento = normalize(user.getDocumento());

        return encuestadorRepository.findAll()
                .stream()
                .filter(item -> Boolean.TRUE.equals(item.getActivo()))
                .filter(item -> Objects.equals(normalize(item.getDocumento()), documento))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "El usuario autenticado no tiene un encuestador activo asociado por documento"
                ));
    }

    /**
     * Determina si el usuario autenticado tiene al menos uno de los roles indicados.
     *
     * @param roles roles a validar.
     * @return true si tiene alguno de los roles.
     */
    private boolean currentUserHasAnyRole(String... roles) {
        for (String role : roles) {
            if (currentUserHasRole(role)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determina si el usuario autenticado tiene un rol específico.
     *
     * @param roleCode código del rol.
     * @return true si el usuario tiene el rol.
     */
    private boolean currentUserHasRole(String roleCode) {
        String expectedRole = normalize(roleCode);

        boolean byAuthority = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(authority -> Objects.equals(normalize(authority.getAuthority()), expectedRole)
                        || Objects.equals(normalize(authority.getAuthority()), "ROLE_" + expectedRole));

        if (byAuthority) {
            return true;
        }

        User user = currentUser();

        return user.getRole() != null && Objects.equals(normalize(user.getRole().getCodigo()), expectedRole);
    }

    /**
     * Busca el caso maestro de Call Center.
     *
     * @param id identificador del caso.
     * @return caso encontrado.
     */
    private CallCenterRegistro findRegistro(Long id) {
        return callCenterRegistroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de Call Center no encontrado"));
    }

    /**
     * Busca un motivo de no contacto, si fue enviado.
     *
     * @param id identificador del motivo.
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
     * Busca un motivo de no disposición, si fue enviado.
     *
     * @param id identificador del motivo.
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
     * Convierte entidad de resultado de llamada a DTO.
     *
     * @param entity entidad origen.
     * @return respuesta DTO.
     */
    private CallCenterResultadoLlamadaResponse toResultadoResponse(CallCenterResultadoLlamada entity) {
        return new CallCenterResultadoLlamadaResponse(
                entity.getId(),
                entity.getCodigo(),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.getEstadoCasoSugerido(),
                entity.getActivo()
        );
    }

    /**
     * Convierte una gestión de llamada a DTO.
     *
     * @param entity entidad origen.
     * @return respuesta DTO.
     */
    private CallCenterGestionLlamadaResponse toGestionResponse(CallCenterGestionLlamada entity) {
        User funcionario = entity.getFuncionarioCallcenter();

        return new CallCenterGestionLlamadaResponse(
                entity.getId(),
                entity.getCallCenterRegistro() != null ? entity.getCallCenterRegistro().getId() : null,
                funcionario != null ? funcionario.getId() : null,
                funcionario != null ? funcionario.getUsername() : null,
                buildUserFullName(funcionario),
                entity.getFechaLlamada(),
                entity.getHoraLlamada(),
                entity.getIntentoNumero(),
                entity.getLlamadaConectada(),
                entity.getResultadoLlamada(),
                entity.getMotivoNoContacto() != null ? entity.getMotivoNoContacto().getId() : null,
                entity.getMotivoNoContacto() != null ? entity.getMotivoNoContacto().getNombre() : null,
                entity.getMotivoNoDisposicion() != null ? entity.getMotivoNoDisposicion().getId() : null,
                entity.getMotivoNoDisposicion() != null ? entity.getMotivoNoDisposicion().getNombre() : null,
                entity.getFechaReprogramacionLlamada(),
                entity.getHoraReprogramacionLlamada(),
                entity.getObservacion(),
                entity.getActivo(),
                entity.getCreadoEn()
        );
    }

    /**
     * Convierte una visita a DTO.
     *
     * <p>Además de la información propia de la visita, incluye datos principales
     * del caso maestro para que la pantalla del encuestador pueda mostrar
     * información contextual y bloquear acciones cuando el caso esté cerrado
     * o cancelado.</p>
     *
     * @param entity entidad origen.
     * @return respuesta DTO.
     */
    private CallCenterVisitaResponse toVisitaResponse(CallCenterVisita entity) {
        User usuarioAsigna = entity.getUsuarioAsigna();
        CallCenterRegistro registro = entity.getCallCenterRegistro();

        return new CallCenterVisitaResponse(
                entity.getId(),
                registro != null ? registro.getId() : null,
                entity.getEncuestador() != null ? entity.getEncuestador().getId() : null,
                entity.getEncuestador() != null ? entity.getEncuestador().getNombre() : null,
                usuarioAsigna != null ? usuarioAsigna.getId() : null,
                usuarioAsigna != null ? usuarioAsigna.getUsername() : null,
                entity.getFechaAsignacion(),
                entity.getFechaProgramada(),
                entity.getHoraProgramada(),
                entity.getEstadoVisita(),
                entity.getFechaVisitaReal(),
                entity.getHoraVisitaReal(),
                entity.getEncuestaRealizada(),
                entity.getMotivoNoEncuesta(),
                entity.getFechaReprogramacion(),
                entity.getObservacionEncuestador(),
                entity.getActivo(),
                entity.getCreadoEn(),

                registro != null ? registro.getCedulaSolicitante() : null,
                registro != null ? registro.getNombreCompleto() : null,
                registro != null ? registro.getTelefono() : null,
                registro != null ? registro.getDireccionTexto() : null,
                registro != null && registro.getBarrio() != null
                        ? registro.getBarrio().getNombre()
                        : null,
                registro != null && registro.getBarrio() != null && registro.getBarrio().getComuna() != null
                        ? registro.getBarrio().getComuna().getNombre()
                        : null,
                registro != null ? registro.getTipoSolicitudCallcenter() : null,
                registro != null ? registro.getEstadoCaso() : null
        );
    }
    /**
     * Construye el nombre completo del usuario.
     *
     * @param user usuario origen.
     * @return nombre completo o username.
     */
    private String buildUserFullName(User user) {
        if (user == null) {
            return null;
        }

        String fullName = (String.valueOf(user.getNombres()) + " " + String.valueOf(user.getApellidos()))
                .replace("null", "")
                .trim();

        return !fullName.isBlank() ? fullName : user.getUsername();
    }

    /**
     * Normaliza un texto a mayúsculas sin espacios extremos.
     *
     * @param value valor origen.
     * @return valor normalizado.
     */
    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Normaliza un texto obligatorio.
     *
     * @param value valor origen.
     * @param message mensaje de validación.
     * @return texto normalizado.
     */
    private String normalizeRequired(String value, String message) {
        String normalized = normalize(value);

        if (isBlank(normalized)) {
            throw new BusinessException(message);
        }

        return normalized;
    }

    /**
     * Convierte texto vacío en null.
     *
     * @param value valor origen.
     * @return texto limpio o null.
     */
    private String trimToNull(String value) {
        if (isBlank(value)) {
            return null;
        }

        return value.trim();
    }

    /**
     * Indica si un texto está vacío.
     *
     * @param value valor a evaluar.
     * @return true si está vacío.
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}