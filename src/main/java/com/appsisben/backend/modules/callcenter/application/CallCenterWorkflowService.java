package com.appsisben.backend.modules.callcenter.application;

import com.appsisben.backend.modules.audit.application.AuditService;
import com.appsisben.backend.modules.audit.domain.AuditAction;
import com.appsisben.backend.modules.callcenter.domain.CallCenterGestionLlamada;
import com.appsisben.backend.modules.callcenter.domain.CallCenterMotivoNoContacto;
import com.appsisben.backend.modules.callcenter.domain.CallCenterMotivoNoDisposicion;
import com.appsisben.backend.modules.callcenter.domain.CallCenterRegistro;
import com.appsisben.backend.modules.callcenter.domain.CallCenterResultadoLlamada;
import com.appsisben.backend.modules.callcenter.domain.CallCenterVisita;
import com.appsisben.backend.modules.callcenter.dto.CallCenterGestionLlamadaRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterGestionLlamadaResponse;
import com.appsisben.backend.modules.callcenter.dto.CallCenterResultadoLlamadaResponse;
import com.appsisben.backend.modules.callcenter.dto.CallCenterVisitaAsignacionRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterVisitaFilterRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterVisitaResponse;
import com.appsisben.backend.modules.callcenter.dto.CallCenterVisitaResultadoRequest;
import com.appsisben.backend.modules.callcenter.repository.CallCenterGestionLlamadaRepository;
import com.appsisben.backend.modules.callcenter.repository.CallCenterMotivoNoContactoRepository;
import com.appsisben.backend.modules.callcenter.repository.CallCenterMotivoNoDisposicionRepository;
import com.appsisben.backend.modules.callcenter.repository.CallCenterRegistroRepository;
import com.appsisben.backend.modules.callcenter.repository.CallCenterResultadoLlamadaRepository;
import com.appsisben.backend.modules.callcenter.repository.CallCenterVisitaRepository;
import com.appsisben.backend.modules.callcenter.repository.CallCenterVisitaSpecification;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CallCenterWorkflowService {

    private static final String TABLE_REGISTRO =
            "callcenter_registro";

    private static final String TABLE_GESTION_LLAMADA =
            "callcenter_gestion_llamada";

    private static final String TABLE_VISITA =
            "callcenter_visita";

    private static final String MOTIVO_CIERRE_ENCUESTA_REALIZADA =
            "Encuesta realizada por encuestador";

    private final CallCenterRegistroRepository
            callCenterRegistroRepository;

    private final CallCenterGestionLlamadaRepository
            gestionLlamadaRepository;

    private final CallCenterVisitaRepository
            visitaRepository;

    private final CallCenterResultadoLlamadaRepository
            resultadoLlamadaRepository;

    private final CallCenterMotivoNoContactoRepository
            motivoNoContactoRepository;

    private final CallCenterMotivoNoDisposicionRepository
            motivoNoDisposicionRepository;

    private final EncuestadorRepository
            encuestadorRepository;

    private final UserRepository userRepository;

    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<CallCenterResultadoLlamadaResponse>
    findResultadosLlamada() {

        return resultadoLlamadaRepository
                .findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(this::toResultadoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CallCenterGestionLlamadaResponse>
    findLlamadasByCaso(
            Long callCenterRegistroId
    ) {

        CallCenterRegistro registro =
                findRegistro(callCenterRegistroId);

        validateCanViewCase(
                registro,
                currentUser()
        );

        return gestionLlamadaRepository
                .findByCallCenterRegistroIdAndActivoTrueOrderByIntentoNumeroAscIdAsc(
                        callCenterRegistroId
                )
                .stream()
                .map(this::toGestionResponse)
                .toList();
    }

    @Transactional
    public CallCenterGestionLlamadaResponse registrarLlamada(
            Long callCenterRegistroId,
            CallCenterGestionLlamadaRequest request
    ) {

        CallCenterRegistro registro =
                findRegistro(callCenterRegistroId);

        User user = currentUser();

        validateCanManageCallCenterCase(
                registro,
                user
        );

        CallCenterStatePolicy.validateCanRegisterCall(
                registro.getEstadoCaso()
        );

        String resultadoCodigo =
                normalizeRequired(
                        request.resultadoLlamada(),
                        "El resultado de la llamada es obligatorio"
                );

        CallCenterResultadoLlamada resultado =
                resultadoLlamadaRepository
                        .findFirstByCodigoIgnoreCaseAndActivoTrue(
                                resultadoCodigo
                        )
                        .orElseThrow(
                                () -> new BusinessException(
                                        "El resultado de llamada "
                                                + "seleccionado no existe "
                                                + "o está inactivo"
                                )
                        );

        Boolean llamadaConectada =
                Boolean.TRUE.equals(
                        request.llamadaConectada()
                );

        CallCenterMotivoNoContacto motivoNoContacto =
                findActiveMotivoNoContacto(
                        request.motivoNoContactoId()
                );

        CallCenterMotivoNoDisposicion motivoNoDisposicion =
                findActiveMotivoNoDisposicion(
                        request.motivoNoDisposicionId()
                );

        validateLlamadaRequest(
                request,
                resultadoCodigo,
                llamadaConectada,
                motivoNoContacto,
                motivoNoDisposicion
        );

        String targetState =
                resolveEstadoCasoFromResultado(
                        resultado,
                        llamadaConectada
                );

        targetState =
                CallCenterStatePolicy
                        .validateCallTransition(
                                registro.getEstadoCaso(),
                                targetState
                        );

        Map<String, Object> beforeRegistro =
                snapshotRegistro(registro);

        long intentosPrevios =
                gestionLlamadaRepository
                        .countByCallCenterRegistroId(
                                callCenterRegistroId
                        );

        CallCenterGestionLlamada entity =
                new CallCenterGestionLlamada();

        entity.setCallCenterRegistro(registro);

        entity.setFuncionarioCallcenter(user);

        entity.setFechaLlamada(
                request.fechaLlamada() != null
                        ? request.fechaLlamada()
                        : LocalDate.now()
        );

        entity.setHoraLlamada(
                request.horaLlamada() != null
                        ? request.horaLlamada()
                        : LocalTime.now().withNano(0)
        );

        entity.setIntentoNumero(
                (int) intentosPrevios + 1
        );

        entity.setLlamadaConectada(
                llamadaConectada
        );

        entity.setResultadoLlamada(
                resultado.getCodigo()
        );

        entity.setMotivoNoContacto(
                motivoNoContacto
        );

        entity.setMotivoNoDisposicion(
                motivoNoDisposicion
        );

        entity.setFechaReprogramacionLlamada(
                request.fechaReprogramacionLlamada()
        );

        entity.setHoraReprogramacionLlamada(
                request.horaReprogramacionLlamada()
        );

        entity.setObservacion(
                trimToNull(request.observacion())
        );

        entity.setCreadoPor(user);

        entity.setActualizadoPor(user);

        CallCenterGestionLlamada saved =
                gestionLlamadaRepository.save(entity);

        applyLlamadaToRegistro(
                registro,
                saved,
                request,
                targetState
        );

        applyFinalStateMetadata(
                registro,
                targetState,
                user,
                buildCallFinalReason(resultado)
        );

        callCenterRegistroRepository.save(registro);

        auditService.safeLogWithUser(
                user,
                AuditAction.CREATE,
                TABLE_GESTION_LLAMADA,
                saved.getId(),
                null,
                snapshotGestion(saved)
        );

        auditService.safeLogWithUser(
                user,
                AuditAction.UPDATE,
                TABLE_REGISTRO,
                registro.getId(),
                beforeRegistro,
                snapshotRegistro(registro)
        );

        return toGestionResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CallCenterVisitaResponse>
    findVisitasByCaso(
            Long callCenterRegistroId
    ) {

        CallCenterRegistro registro =
                findRegistro(callCenterRegistroId);

        validateCanViewCase(
                registro,
                currentUser()
        );

        return visitaRepository
                .findByCallCenterRegistroIdAndActivoTrueOrderByFechaAsignacionDescIdDesc(
                        callCenterRegistroId
                )
                .stream()
                .map(this::toVisitaResponse)
                .toList();
    }

    @Transactional
    public CallCenterVisitaResponse asignarVisita(
            Long callCenterRegistroId,
            CallCenterVisitaAsignacionRequest request
    ) {

        CallCenterRegistro registro =
                findRegistro(callCenterRegistroId);

        User user = currentUser();

        validateCanManageCallCenterCase(
                registro,
                user
        );

        boolean programmed =
                request.fechaProgramada() != null;

        String targetState =
                CallCenterStatePolicy
                        .validateEncuestadorAssignment(
                                registro.getEstadoCaso(),
                                programmed
                        );

        Encuestador encuestador =
                findActiveEncuestador(
                        request.encuestadorId()
                );

        Map<String, Object> beforeRegistro =
                snapshotRegistro(registro);

        CallCenterVisita visita =
                new CallCenterVisita();

        visita.setCallCenterRegistro(registro);

        visita.setEncuestador(encuestador);

        visita.setUsuarioAsigna(user);

        visita.setFechaProgramada(
                request.fechaProgramada()
        );

        visita.setHoraProgramada(
                request.horaProgramada()
        );

        visita.setEstadoVisita(
                programmed
                        ? "PROGRAMADA"
                        : "PENDIENTE"
        );

        visita.setObservacionEncuestador(
                trimToNull(request.observacion())
        );

        visita.setCreadoPor(user);

        visita.setActualizadoPor(user);

        CallCenterVisita saved =
                visitaRepository.save(visita);

        registro.setEncuestadorAsignado(encuestador);

        registro.setEncuestadorProgramado(encuestador);

        registro.setFechaEncuestaProgramada(
                request.fechaProgramada()
        );

        registro.setEstadoVisita(
                saved.getEstadoVisita()
        );

        registro.setEstadoCaso(targetState);

        registro.setActualizadoPor(user);

        callCenterRegistroRepository.save(registro);

        auditService.safeLogWithUser(
                user,
                AuditAction.CREATE,
                TABLE_VISITA,
                saved.getId(),
                null,
                snapshotVisita(saved)
        );

        auditService.safeLogWithUser(
                user,
                AuditAction.UPDATE,
                TABLE_REGISTRO,
                registro.getId(),
                beforeRegistro,
                snapshotRegistro(registro)
        );

        return toVisitaResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<CallCenterVisitaResponse>
    misVisitas(
            CallCenterVisitaFilterRequest filter,
            Pageable pageable
    ) {

        User user = currentUser();

        Specification<CallCenterVisita> specification =
                CallCenterVisitaSpecification
                        .activeOnly()
                        .and(
                                CallCenterVisitaSpecification
                                        .byFilter(filter)
                        );

        if (
                currentUserHasRole(
                        "FUNCIONARIO_ENCUESTADOR"
                )
        ) {

            Encuestador encuestador =
                    currentEncuestador(user);

            specification =
                    specification.and(
                            CallCenterVisitaSpecification
                                    .byEncuestador(
                                            encuestador.getId()
                                    )
                    );
        }

        Page<CallCenterVisita> page =
                visitaRepository.findAll(
                        specification,
                        pageable
                );

        return PageResponse.from(
                page,
                page.getContent()
                        .stream()
                        .map(this::toVisitaResponse)
                        .toList()
        );
    }

    @Transactional
    public CallCenterVisitaResponse actualizarResultadoVisita(
            Long visitaId,
            CallCenterVisitaResultadoRequest request
    ) {

        CallCenterVisita visita =
                visitaRepository
                        .findById(visitaId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Visita de Call Center no encontrada"
                                )
                        );

        User user = currentUser();

        validateCanUpdateVisit(
                visita,
                user
        );

        CallCenterRegistro registro =
                visita.getCallCenterRegistro();

        if (registro == null) {
            throw new BusinessException(
                    "La visita no tiene un caso maestro asociado"
            );
        }

        CallCenterStatePolicy.validateCanUpdateVisit(
                registro.getEstadoCaso()
        );

        String estadoVisita =
                normalizeRequired(
                        request.estadoVisita(),
                        "El estado de la visita es obligatorio"
                );

        validateEstadoVisita(estadoVisita);

        String targetState =
                CallCenterStatePolicy.resolveStateFromVisit(
                        estadoVisita,
                        request.encuestaRealizada()
                );

        targetState =
                CallCenterStatePolicy
                        .validateVisitTransition(
                                registro.getEstadoCaso(),
                                targetState
                        );

        Map<String, Object> beforeVisita =
                snapshotVisita(visita);

        Map<String, Object> beforeRegistro =
                snapshotRegistro(registro);

        visita.setEstadoVisita(estadoVisita);

        visita.setFechaVisitaReal(
                request.fechaVisitaReal() != null
                        ? request.fechaVisitaReal()
                        : LocalDate.now()
        );

        visita.setHoraVisitaReal(
                request.horaVisitaReal() != null
                        ? request.horaVisitaReal()
                        : LocalTime.now().withNano(0)
        );

        visita.setEncuestaRealizada(
                request.encuestaRealizada()
        );

        visita.setMotivoNoEncuesta(
                trimToNull(
                        request.motivoNoEncuesta()
                )
        );

        visita.setFechaReprogramacion(
                request.fechaReprogramacion()
        );

        visita.setObservacionEncuestador(
                trimToNull(
                        request.observacionEncuestador()
                )
        );

        visita.setActualizadoPor(user);

        CallCenterVisita saved =
                visitaRepository.save(visita);

        registro.setEstadoVisita(
                saved.getEstadoVisita()
        );

        registro.setFechaVisitaReal(
                saved.getFechaVisitaReal()
        );

        registro.setHoraVisitaReal(
                saved.getHoraVisitaReal()
        );

        registro.setEncuestaRealizada(
                saved.getEncuestaRealizada()
        );

        registro.setMotivoNoEncuesta(
                saved.getMotivoNoEncuesta()
        );

        registro.setFechaReprogramacion(
                saved.getFechaReprogramacion()
        );

        registro.setObservacionEncuestador(
                saved.getObservacionEncuestador()
        );

        registro.setEstadoCaso(targetState);

        registro.setActualizadoPor(user);

        applyFinalStateMetadata(
                registro,
                targetState,
                user,
                buildVisitFinalReason(
                        targetState,
                        saved
                )
        );

        callCenterRegistroRepository.save(registro);

        auditService.safeLogWithUser(
                user,
                AuditAction.UPDATE,
                TABLE_VISITA,
                saved.getId(),
                beforeVisita,
                snapshotVisita(saved)
        );

        auditService.safeLogWithUser(
                user,
                AuditAction.UPDATE,
                TABLE_REGISTRO,
                registro.getId(),
                beforeRegistro,
                snapshotRegistro(registro)
        );

        return toVisitaResponse(saved);
    }

    /**
     * Aplica en el caso maestro la información correspondiente
     * al último intento de llamada.
     *
     * <p>Cuando el intento no se conecta, se actualiza el motivo
     * de no contacto sin eliminar los datos previamente confirmados.</p>
     *
     * <p>Cuando el intento se conecta, se actualizan solamente los
     * valores enviados dentro de la solicitud.</p>
     */
    private void applyLlamadaToRegistro(
            CallCenterRegistro registro,
            CallCenterGestionLlamada gestion,
            CallCenterGestionLlamadaRequest request,
            String targetState
    ) {

        registro.setFechaLlamada(
                gestion.getFechaLlamada()
        );

        registro.setHoraLlamada(
                gestion.getHoraLlamada()
        );

        registro.setLlamadaConectada(
                gestion.getLlamadaConectada()
        );

        if (
                !Boolean.TRUE.equals(
                        gestion.getLlamadaConectada()
                )
        ) {

            /*
             * Una llamada no conectada conserva los datos que
             * pudieron confirmarse en intentos anteriores.
             */
            registro.setMotivoNoContacto(
                    gestion.getMotivoNoContacto()
            );

        } else {

            /*
             * Una llamada conectada deja sin efecto el motivo
             * de no contacto del intento anterior.
             */
            registro.setMotivoNoContacto(null);

            registro.setMotivoNoDisposicion(
                    gestion.getMotivoNoDisposicion()
            );

            applyConfirmedCallData(
                    registro,
                    request
            );
        }

        registro.setObservacion(
                gestion.getObservacion()
        );

        registro.setEstadoCaso(targetState);

        registro.setActualizadoPor(
                gestion.getActualizadoPor()
        );
    }

    /**
     * Actualiza en el caso maestro la última información
     * confirmada durante una llamada conectada.
     *
     * <p>Los valores nulos o vacíos no reemplazan información
     * previamente almacenada.</p>
     */
    private void applyConfirmedCallData(
            CallCenterRegistro registro,
            CallCenterGestionLlamadaRequest request
    ) {

        if (
                request.solicitoNuevaEncuesta()
                        != null
        ) {

            registro.setSolicitoNuevaEncuesta(
                    request.solicitoNuevaEncuesta()
            );
        }

        String direccionConfirmada =
                trimToNull(
                        request.direccionTexto()
                );

        if (direccionConfirmada != null) {

            registro.setDireccionTexto(
                    limitLength(
                            direccionConfirmada,
                            500
                    )
            );
        }

        if (
                request.fechaAplicacionInformada()
                        != null
        ) {

            registro.setFechaAplicacionInformada(
                    request.fechaAplicacionInformada()
            );
        }

        if (
                request.disposicionRecibirEncuesta()
                        != null
        ) {

            registro.setDisposicionRecibirEncuesta(
                    request.disposicionRecibirEncuesta()
            );
        }

        if (
                request.explicoInformanteCalificado()
                        != null
        ) {

            registro.setExplicoInformanteCalificado(
                    request.explicoInformanteCalificado()
            );
        }
    }

    private String resolveEstadoCasoFromResultado(
            CallCenterResultadoLlamada resultado,
            Boolean llamadaConectada
    ) {

        String suggested =
                normalize(
                        resultado.getEstadoCasoSugerido()
                );

        if (!isBlank(suggested)) {

            return CallCenterStatePolicy
                    .requireKnownTargetState(
                            suggested
                    );
        }

        return Boolean.TRUE.equals(llamadaConectada)
                ? CallCenterStatePolicy.EN_GESTION_LLAMADA
                : CallCenterStatePolicy.NO_CONTACTADO;
    }

    private void validateLlamadaRequest(
            CallCenterGestionLlamadaRequest request,
            String resultadoCodigo,
            Boolean llamadaConectada,
            CallCenterMotivoNoContacto motivoNoContacto,
            CallCenterMotivoNoDisposicion motivoNoDisposicion
    ) {

        if (
                !Boolean.TRUE.equals(llamadaConectada)
                        && motivoNoContacto == null
        ) {

            throw new BusinessException(
                    "Debe seleccionar el motivo de no contacto"
            );
        }

        boolean ciudadanoSinDisposicion =
                Boolean.TRUE.equals(llamadaConectada)
                        && Boolean.FALSE.equals(
                        request.disposicionRecibirEncuesta()
                );

        boolean resultadoSinDisposicion =
                "CONTACTADO_NO_ACEPTA_VISITA"
                        .equals(resultadoCodigo);

        if (
                (
                        ciudadanoSinDisposicion
                                || resultadoSinDisposicion
                )
                        && motivoNoDisposicion == null
        ) {

            throw new BusinessException(
                    "Debe seleccionar el motivo de no disposición"
            );
        }
    }

    private void validateEstadoVisita(
            String estado
    ) {

        List<String> allowed =
                List.of(
                        "PENDIENTE",
                        "PROGRAMADA",
                        "REALIZADA",
                        "NO_ATENDIDA",
                        "REPROGRAMADA",
                        "CANCELADA"
                );

        if (!allowed.contains(estado)) {

            throw new BusinessException(
                    "Estado de visita no permitido: "
                            + estado
            );
        }
    }

    private void validateCanViewCase(
            CallCenterRegistro registro,
            User user
    ) {

        if (
                currentUserHasAnyRole(
                        "ADMIN",
                        "SUPERVISOR",
                        "COORDINADOR_CALLCENTER"
                )
        ) {
            return;
        }

        if (
                currentUserHasRole(
                        "FUNCIONARIO_CALLCENTER"
                )
        ) {

            validateAssignedToFuncionarioCallCenter(
                    registro,
                    user
            );

            return;
        }

        if (
                currentUserHasRole(
                        "FUNCIONARIO_ENCUESTADOR"
                )
        ) {

            Encuestador encuestador =
                    currentEncuestador(user);

            boolean assigned =
                    Objects.equals(
                            registro.getEncuestadorAsignado()
                                    != null
                                    ? registro
                                    .getEncuestadorAsignado()
                                    .getId()
                                    : null,
                            encuestador.getId()
                    )
                            || Objects.equals(
                            registro.getEncuestadorProgramado()
                                    != null
                                    ? registro
                                    .getEncuestadorProgramado()
                                    .getId()
                                    : null,
                            encuestador.getId()
                    );

            if (!assigned) {

                throw new BusinessException(
                        "El caso no está asignado al "
                                + "encuestador autenticado"
                );
            }

            return;
        }

        throw new BusinessException(
                "No tiene permisos para consultar este caso"
        );
    }

    private void validateCanManageCallCenterCase(
            CallCenterRegistro registro,
            User user
    ) {

        if (
                CallCenterStatePolicy.isFinalState(
                        registro.getEstadoCaso()
                )
        ) {

            throw new BusinessException(
                    "No se puede gestionar un caso cerrado o cancelado"
            );
        }

        if (
                currentUserHasAnyRole(
                        "ADMIN",
                        "SUPERVISOR",
                        "COORDINADOR_CALLCENTER"
                )
        ) {
            return;
        }

        if (
                currentUserHasRole(
                        "FUNCIONARIO_CALLCENTER"
                )
        ) {

            validateAssignedToFuncionarioCallCenter(
                    registro,
                    user
            );

            return;
        }

        throw new BusinessException(
                "No tiene permisos para gestionar "
                        + "este caso de Call Center"
        );
    }

    private void validateAssignedToFuncionarioCallCenter(
            CallCenterRegistro registro,
            User user
    ) {

        Long assignedId =
                registro.getFuncionarioCallcenterAsignado()
                        != null
                        ? registro
                        .getFuncionarioCallcenterAsignado()
                        .getId()
                        : null;

        if (
                !Objects.equals(
                        assignedId,
                        user.getId()
                )
        ) {

            throw new BusinessException(
                    "El caso no está asignado al funcionario "
                            + "Call Center autenticado"
            );
        }
    }

    private void validateCanUpdateVisit(
            CallCenterVisita visita,
            User user
    ) {

        if (
                currentUserHasAnyRole(
                        "ADMIN",
                        "SUPERVISOR"
                )
        ) {
            return;
        }

        if (
                currentUserHasRole(
                        "FUNCIONARIO_ENCUESTADOR"
                )
        ) {

            String documentoUsuario =
                    normalize(
                            user.getDocumento()
                    );

            String documentoEncuestador =
                    normalize(
                            visita.getEncuestador()
                                    .getDocumento()
                    );

            if (
                    !Objects.equals(
                            documentoUsuario,
                            documentoEncuestador
                    )
            ) {

                throw new BusinessException(
                        "La visita no está asignada al "
                                + "encuestador autenticado"
                );
            }

            return;
        }

        throw new BusinessException(
                "No tiene permisos para actualizar "
                        + "el resultado de la visita"
        );
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
                isBlank(username)
                        || "anonymousUser".equals(username)
        ) {

            throw new BusinessException(
                    "No hay usuario autenticado"
            );
        }

        return userRepository
                .findByUsernameIgnoreCase(username)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Usuario autenticado no encontrado"
                        )
                );
    }

    private Encuestador currentEncuestador(
            User user
    ) {

        String documento =
                normalize(
                        user.getDocumento()
                );

        if (isBlank(documento)) {

            throw new BusinessException(
                    "El usuario autenticado no tiene "
                            + "documento registrado"
            );
        }

        Encuestador encuestador =
                encuestadorRepository
                        .findFirstByDocumento(
                                user.getDocumento()
                        )
                        .orElseThrow(
                                () -> new BusinessException(
                                        "El usuario autenticado no tiene "
                                                + "un encuestador asociado "
                                                + "por documento"
                                )
                        );

        if (
                !Boolean.TRUE.equals(
                        encuestador.getActivo()
                )
        ) {

            throw new BusinessException(
                    "El encuestador asociado al usuario "
                            + "autenticado está inactivo"
            );
        }

        return encuestador;
    }

    private boolean currentUserHasAnyRole(
            String... roles
    ) {

        for (String role : roles) {

            if (currentUserHasRole(role)) {
                return true;
            }
        }

        return false;
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

        String expectedRole =
                normalize(roleCode);

        boolean byAuthority =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getAuthorities()
                        .stream()
                        .anyMatch(
                                authority ->
                                        Objects.equals(
                                                normalize(
                                                        authority
                                                                .getAuthority()
                                                ),
                                                expectedRole
                                        )
                                                || Objects.equals(
                                                normalize(
                                                        authority
                                                                .getAuthority()
                                                ),
                                                "ROLE_" + expectedRole
                                        )
                        );

        if (byAuthority) {
            return true;
        }

        User user = currentUser();

        return user.getRole() != null
                && Objects.equals(
                normalize(
                        user.getRole()
                                .getCodigo()
                ),
                expectedRole
        );
    }

    private CallCenterRegistro findRegistro(
            Long id
    ) {

        return callCenterRegistroRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Registro de Call Center no encontrado"
                        )
                );
    }

    private CallCenterMotivoNoContacto
    findActiveMotivoNoContacto(
            Long id
    ) {

        if (id == null) {
            return null;
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
    findActiveMotivoNoDisposicion(
            Long id
    ) {

        if (id == null) {
            return null;
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

    private Encuestador findActiveEncuestador(
            Long id
    ) {

        Encuestador encuestador =
                encuestadorRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Encuestador no encontrado"
                                )
                        );

        if (
                !Boolean.TRUE.equals(
                        encuestador.getActivo()
                )
        ) {

            throw new BusinessException(
                    "El encuestador seleccionado está inactivo"
            );
        }

        return encuestador;
    }

    private void applyFinalStateMetadata(
            CallCenterRegistro registro,
            String targetState,
            User user,
            String reason
    ) {

        if (
                !CallCenterStatePolicy
                        .isFinalState(targetState)
        ) {
            return;
        }

        registro.setFechaCierre(
                LocalDateTime.now()
        );

        registro.setUsuarioCierre(user);

        String finalReason =
                trimToNull(reason);

        registro.setMotivoCierre(
                finalReason != null
                        ? limitLength(
                        finalReason,
                        500
                )
                        : "Finalización del caso Call Center"
        );
    }

    private String buildCallFinalReason(
            CallCenterResultadoLlamada resultado
    ) {

        String nombre =
                resultado != null
                        ? trimToNull(
                        resultado.getNombre()
                )
                        : null;

        String codigo =
                resultado != null
                        ? trimToNull(
                        resultado.getCodigo()
                )
                        : null;

        if (nombre != null) {
            return "Resultado de llamada: " + nombre;
        }

        if (codigo != null) {
            return "Resultado de llamada: " + codigo;
        }

        return "Finalización por resultado de llamada";
    }

    private String buildVisitFinalReason(
            String targetState,
            CallCenterVisita visita
    ) {

        if (
                CallCenterStatePolicy
                        .isClosedState(targetState)
        ) {

            return MOTIVO_CIERRE_ENCUESTA_REALIZADA;
        }

        String motivo =
                visita != null
                        ? trimToNull(
                        visita.getMotivoNoEncuesta()
                )
                        : null;

        return motivo != null
                ? "Visita cancelada: " + motivo
                : "Visita cancelada";
    }

    private Map<String, Object> snapshotRegistro(
            CallCenterRegistro entity
    ) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "id",
                entity.getId()
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
                "fechaLlamada",
                entity.getFechaLlamada()
        );

        data.put(
                "horaLlamada",
                entity.getHoraLlamada()
        );

        data.put(
                "llamadaConectada",
                entity.getLlamadaConectada()
        );

        data.put(
                "solicitoNuevaEncuesta",
                entity.getSolicitoNuevaEncuesta()
        );

        data.put(
                "direccionTexto",
                entity.getDireccionTexto()
        );

        data.put(
                "fechaAplicacionInformada",
                entity.getFechaAplicacionInformada()
        );

        data.put(
                "disposicionRecibirEncuesta",
                entity.getDisposicionRecibirEncuesta()
        );

        data.put(
                "explicoInformanteCalificado",
                entity.getExplicoInformanteCalificado()
        );

        data.put(
                "motivoNoContactoId",
                entity.getMotivoNoContacto() != null
                        ? entity.getMotivoNoContacto()
                        .getId()
                        : null
        );

        data.put(
                "motivoNoDisposicionId",
                entity.getMotivoNoDisposicion() != null
                        ? entity.getMotivoNoDisposicion()
                        .getId()
                        : null
        );

        data.put(
                "encuestadorAsignadoId",
                entity.getEncuestadorAsignado() != null
                        ? entity.getEncuestadorAsignado()
                        .getId()
                        : null
        );

        data.put(
                "encuestadorProgramadoId",
                entity.getEncuestadorProgramado() != null
                        ? entity.getEncuestadorProgramado()
                        .getId()
                        : null
        );

        data.put(
                "fechaEncuestaProgramada",
                entity.getFechaEncuestaProgramada()
        );

        data.put(
                "encuestaRealizada",
                entity.getEncuestaRealizada()
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
                entity.getUsuarioCierre() != null
                        ? entity.getUsuarioCierre()
                        .getId()
                        : null
        );

        return data;
    }

    private Map<String, Object> snapshotGestion(
            CallCenterGestionLlamada entity
    ) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "id",
                entity.getId()
        );

        data.put(
                "callCenterRegistroId",
                entity.getCallCenterRegistro() != null
                        ? entity.getCallCenterRegistro()
                        .getId()
                        : null
        );

        data.put(
                "funcionarioCallcenterId",
                entity.getFuncionarioCallcenter() != null
                        ? entity.getFuncionarioCallcenter()
                        .getId()
                        : null
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
                "intentoNumero",
                entity.getIntentoNumero()
        );

        data.put(
                "llamadaConectada",
                entity.getLlamadaConectada()
        );

        data.put(
                "resultadoLlamada",
                entity.getResultadoLlamada()
        );

        data.put(
                "motivoNoContactoId",
                entity.getMotivoNoContacto() != null
                        ? entity.getMotivoNoContacto()
                        .getId()
                        : null
        );

        data.put(
                "motivoNoDisposicionId",
                entity.getMotivoNoDisposicion() != null
                        ? entity.getMotivoNoDisposicion()
                        .getId()
                        : null
        );

        data.put(
                "fechaReprogramacionLlamada",
                entity.getFechaReprogramacionLlamada()
        );

        data.put(
                "horaReprogramacionLlamada",
                entity.getHoraReprogramacionLlamada()
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

    private Map<String, Object> snapshotVisita(
            CallCenterVisita entity
    ) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "id",
                entity.getId()
        );

        data.put(
                "callCenterRegistroId",
                entity.getCallCenterRegistro() != null
                        ? entity.getCallCenterRegistro()
                        .getId()
                        : null
        );

        data.put(
                "encuestadorId",
                entity.getEncuestador() != null
                        ? entity.getEncuestador()
                        .getId()
                        : null
        );

        data.put(
                "usuarioAsignaId",
                entity.getUsuarioAsigna() != null
                        ? entity.getUsuarioAsigna()
                        .getId()
                        : null
        );

        data.put(
                "fechaAsignacion",
                entity.getFechaAsignacion()
        );

        data.put(
                "fechaProgramada",
                entity.getFechaProgramada()
        );

        data.put(
                "horaProgramada",
                entity.getHoraProgramada()
        );

        data.put(
                "estadoVisita",
                entity.getEstadoVisita()
        );

        data.put(
                "fechaVisitaReal",
                entity.getFechaVisitaReal()
        );

        data.put(
                "horaVisitaReal",
                entity.getHoraVisitaReal()
        );

        data.put(
                "encuestaRealizada",
                entity.getEncuestaRealizada()
        );

        data.put(
                "motivoNoEncuesta",
                entity.getMotivoNoEncuesta()
        );

        data.put(
                "fechaReprogramacion",
                entity.getFechaReprogramacion()
        );

        data.put(
                "observacionEncuestador",
                entity.getObservacionEncuestador()
        );

        data.put(
                "activo",
                entity.getActivo()
        );

        return data;
    }

    private CallCenterResultadoLlamadaResponse
    toResultadoResponse(
            CallCenterResultadoLlamada entity
    ) {

        return new CallCenterResultadoLlamadaResponse(
                entity.getId(),
                entity.getCodigo(),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.getEstadoCasoSugerido(),
                entity.getActivo()
        );
    }

    private CallCenterGestionLlamadaResponse
    toGestionResponse(
            CallCenterGestionLlamada entity
    ) {

        User funcionario =
                entity.getFuncionarioCallcenter();

        return new CallCenterGestionLlamadaResponse(
                entity.getId(),

                entity.getCallCenterRegistro() != null
                        ? entity.getCallCenterRegistro()
                        .getId()
                        : null,

                funcionario != null
                        ? funcionario.getId()
                        : null,

                funcionario != null
                        ? funcionario.getUsername()
                        : null,

                buildUserFullName(funcionario),

                entity.getFechaLlamada(),

                entity.getHoraLlamada(),

                entity.getIntentoNumero(),

                entity.getLlamadaConectada(),

                entity.getResultadoLlamada(),

                entity.getMotivoNoContacto() != null
                        ? entity.getMotivoNoContacto()
                        .getId()
                        : null,

                entity.getMotivoNoContacto() != null
                        ? entity.getMotivoNoContacto()
                        .getNombre()
                        : null,

                entity.getMotivoNoDisposicion() != null
                        ? entity.getMotivoNoDisposicion()
                        .getId()
                        : null,

                entity.getMotivoNoDisposicion() != null
                        ? entity.getMotivoNoDisposicion()
                        .getNombre()
                        : null,

                entity.getFechaReprogramacionLlamada(),

                entity.getHoraReprogramacionLlamada(),

                entity.getObservacion(),

                entity.getActivo(),

                entity.getCreadoEn()
        );
    }

    private CallCenterVisitaResponse toVisitaResponse(
            CallCenterVisita entity
    ) {

        User usuarioAsigna =
                entity.getUsuarioAsigna();

        CallCenterRegistro registro =
                entity.getCallCenterRegistro();

        return new CallCenterVisitaResponse(
                entity.getId(),

                registro != null
                        ? registro.getId()
                        : null,

                entity.getEncuestador() != null
                        ? entity.getEncuestador()
                        .getId()
                        : null,

                entity.getEncuestador() != null
                        ? entity.getEncuestador()
                        .getNombre()
                        : null,

                usuarioAsigna != null
                        ? usuarioAsigna.getId()
                        : null,

                usuarioAsigna != null
                        ? usuarioAsigna.getUsername()
                        : null,

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

                registro != null
                        ? registro.getCedulaSolicitante()
                        : null,

                registro != null
                        ? registro.getNombreCompleto()
                        : null,

                registro != null
                        ? registro.getTelefono()
                        : null,

                registro != null
                        ? registro.getDireccionTexto()
                        : null,

                registro != null
                        && registro.getBarrio() != null
                        ? registro.getBarrio()
                        .getNombre()
                        : null,

                registro != null
                        && registro.getBarrio() != null
                        && registro.getBarrio()
                        .getComuna() != null
                        ? registro.getBarrio()
                        .getComuna()
                        .getNombre()
                        : null,

                registro != null
                        ? registro.getTipoSolicitudCallcenter()
                        : null,

                registro != null
                        ? registro.getEstadoCaso()
                        : null
        );
    }

    private String buildUserFullName(
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

        String fullName =
                (nombres + " " + apellidos)
                        .trim();

        return !fullName.isBlank()
                ? fullName
                : user.getUsername();
    }

    private String normalize(
            String value
    ) {

        return value == null
                ? null
                : value
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private String normalizeRequired(
            String value,
            String message
    ) {

        String normalized =
                normalize(value);

        if (isBlank(normalized)) {

            throw new BusinessException(
                    message
            );
        }

        return normalized;
    }

    private String trimToNull(
            String value
    ) {

        if (isBlank(value)) {
            return null;
        }

        return value.trim();
    }

    private String limitLength(
            String value,
            int maxLength
    ) {

        if (value == null) {
            return null;
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(
                0,
                maxLength
        );
    }

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value
                .trim()
                .isEmpty();
    }
}