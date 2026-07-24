package com.appsisben.backend.modules.callcenter.application;

import com.appsisben.backend.modules.audit.application.AuditService;
import com.appsisben.backend.modules.audit.domain.AuditAction;
import com.appsisben.backend.modules.callcenter.domain.CallCenterMotivoNoContacto;
import com.appsisben.backend.modules.callcenter.domain.CallCenterMotivoNoDisposicion;
import com.appsisben.backend.modules.callcenter.domain.CallCenterRegistro;
import com.appsisben.backend.modules.callcenter.dto.CallCenterCatalogResponse;
import com.appsisben.backend.modules.callcenter.dto.CallCenterFilterRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterResponse;
import com.appsisben.backend.modules.callcenter.dto.CallCenterSummaryResponse;
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
import com.appsisben.backend.shared.api.PageResponse;
import com.appsisben.backend.shared.exception.BusinessException;
import com.appsisben.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
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
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<CallCenterResponse> findAll(Pageable pageable) {
        Page<CallCenterRegistro> page = repository.findAll(
                CallCenterRegistroSpecification.activeOnly(),
                pageable
        );

        List<CallCenterResponse> content = page.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public PageResponse<CallCenterResponse> search(CallCenterFilterRequest filter, Pageable pageable) {
        Page<CallCenterRegistro> page = repository.findAll(
                CallCenterRegistroSpecification.byFilter(filter),
                pageable
        );

        List<CallCenterResponse> content = page.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public CallCenterResponse findById(Long id) {
        return toResponse(findEntity(id));
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
    public CallCenterSummaryResponse summary() {
        List<CallCenterRegistro> all = repository.findAll();

        long total = all.size();
        long conectadas = all.stream()
                .filter(item -> Boolean.TRUE.equals(item.getLlamadaConectada()))
                .count();
        long noConectadas = all.stream()
                .filter(item -> Boolean.FALSE.equals(item.getLlamadaConectada()))
                .count();
        long activos = all.stream()
                .filter(item -> Boolean.TRUE.equals(item.getActivo()))
                .count();
        long inactivos = all.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getActivo()))
                .count();

        return new CallCenterSummaryResponse(
                total,
                conectadas,
                noConectadas,
                activos,
                inactivos
        );
    }

    @Transactional
    public CallCenterResponse create(CallCenterRequest request) {
        CallCenterRegistro entity = new CallCenterRegistro();
        User user = currentUser();

        entity.setFuncionario(user);
        entity.setCreadoPor(user);
        entity.setActivo(true);

        apply(entity, request, user);

        CallCenterRegistro saved = repository.save(entity);

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
    public CallCenterResponse update(Long id, CallCenterRequest request) {
        CallCenterRegistro entity = findEntity(id);
        Map<String, Object> before = snapshot(entity);

        apply(entity, request, currentUser());

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
    public CallCenterResponse activate(Long id) {
        CallCenterRegistro entity = findEntity(id);
        Map<String, Object> before = snapshot(entity);

        entity.setActivo(true);
        entity.setActualizadoPor(currentUser());

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
    public CallCenterResponse deactivate(Long id) {
        CallCenterRegistro entity = findEntity(id);
        Map<String, Object> before = snapshot(entity);

        entity.setActivo(false);
        entity.setActualizadoPor(currentUser());

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
    public void delete(Long id) {
        deactivate(id);
    }

    private void apply(CallCenterRegistro entity, CallCenterRequest request, User user) {
        validateRequest(request);

        entity.setMarcaTemporal(request.marcaTemporal());
        entity.setFechaLlamada(request.fechaLlamada());
        entity.setHoraLlamada(request.horaLlamada());
        entity.setCedulaSolicitante(clean(request.cedulaSolicitante()));
        entity.setNombreCompleto(upper(request.nombreCompleto()));
        entity.setTelefono(clean(request.telefono()));
        entity.setLlamadaConectada(request.llamadaConectada());
        entity.setObservacion(clean(request.observacion()));
        entity.setActualizadoPor(user);

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

    private void validateRequest(CallCenterRequest request) {
        if (Boolean.FALSE.equals(request.llamadaConectada())) {
            boolean hasMotivo = request.motivoNoContactoId() != null
                    || hasText(request.motivoNoContactoTexto());

            if (!hasMotivo) {
                throw new BusinessException("Debe registrar el motivo por el cual no se logró conectar la llamada");
            }
        }

        if (Boolean.TRUE.equals(request.llamadaConectada())
                && Boolean.FALSE.equals(request.disposicionRecibirEncuesta())) {
            boolean hasMotivo = request.motivoNoDisposicionId() != null
                    || hasText(request.motivoNoDisposicionTexto());

            if (!hasMotivo) {
                throw new BusinessException("Debe registrar el motivo por el cual no se confirmó la disposición");
            }
        }
    }

    private CallCenterRegistro findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro Call Center no encontrado"));
    }

    private CallCenterMotivoNoContacto findMotivoNoContacto(Long id) {
        if (id == null) {
            return null;
        }

        return motivoNoContactoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Motivo de no contacto no encontrado"));
    }

    private CallCenterMotivoNoDisposicion findMotivoNoDisposicion(Long id) {
        if (id == null) {
            return null;
        }

        return motivoNoDisposicionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Motivo de no disposición no encontrado"));
    }

    private Encuestador findEncuestador(Long id) {
        if (id == null) {
            return null;
        }

        return encuestadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encuestador no encontrado"));
    }

    private Barrio findBarrio(Long id) {
        if (id == null) {
            return null;
        }

        return barrioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Barrio no encontrado"));
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            throw new BusinessException("No hay usuario autenticado");
        }

        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    private CallCenterResponse toResponse(CallCenterRegistro entity) {
        return new CallCenterResponse(
                entity.getId(),
                entity.getMarcaTemporal(),
                entity.getFechaLlamada(),
                entity.getHoraLlamada(),

                entity.getFuncionario() != null ? entity.getFuncionario().getId() : null,
                entity.getFuncionario() != null ? entity.getFuncionario().getUsername() : null,

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
        data.put("funcionarioId", entity.getFuncionario() != null ? entity.getFuncionario().getId() : null);
        data.put("funcionarioUsername", entity.getFuncionario() != null ? entity.getFuncionario().getUsername() : null);
        data.put("cedulaSolicitante", entity.getCedulaSolicitante());
        data.put("nombreCompleto", entity.getNombreCompleto());
        data.put("telefono", entity.getTelefono());
        data.put("llamadaConectada", entity.getLlamadaConectada());
        data.put("motivoNoContactoId", entity.getMotivoNoContacto() != null ? entity.getMotivoNoContacto().getId() : null);
        data.put("motivoNoContactoNombre", entity.getMotivoNoContacto() != null ? entity.getMotivoNoContacto().getNombre() : null);
        data.put("motivoNoContactoTexto", entity.getMotivoNoContactoTexto());
        data.put("encuestadorProgramadoId", entity.getEncuestadorProgramado() != null ? entity.getEncuestadorProgramado().getId() : null);
        data.put("encuestadorProgramadoNombre", entity.getEncuestadorProgramado() != null ? entity.getEncuestadorProgramado().getNombre() : null);
        data.put("fechaEncuestaProgramada", entity.getFechaEncuestaProgramada());
        data.put("solicitoNuevaEncuesta", entity.getSolicitoNuevaEncuesta());
        data.put("direccionTexto", entity.getDireccionTexto());
        data.put("barrioId", entity.getBarrio() != null ? entity.getBarrio().getId() : null);
        data.put("barrioNombre", entity.getBarrio() != null ? entity.getBarrio().getNombre() : null);
        data.put("comunaNombre", entity.getBarrio() != null && entity.getBarrio().getComuna() != null
                ? entity.getBarrio().getComuna().getNombre()
                : null);
        data.put("fechaAplicacionInformada", entity.getFechaAplicacionInformada());
        data.put("disposicionRecibirEncuesta", entity.getDisposicionRecibirEncuesta());
        data.put("motivoNoDisposicionId", entity.getMotivoNoDisposicion() != null ? entity.getMotivoNoDisposicion().getId() : null);
        data.put("motivoNoDisposicionNombre", entity.getMotivoNoDisposicion() != null ? entity.getMotivoNoDisposicion().getNombre() : null);
        data.put("motivoNoDisposicionTexto", entity.getMotivoNoDisposicionTexto());
        data.put("encuestadorAsignadoId", entity.getEncuestadorAsignado() != null ? entity.getEncuestadorAsignado().getId() : null);
        data.put("encuestadorAsignadoNombre", entity.getEncuestadorAsignado() != null ? entity.getEncuestadorAsignado().getNombre() : null);
        data.put("explicoInformanteCalificado", entity.getExplicoInformanteCalificado());
        data.put("observacion", entity.getObservacion());
        data.put("activo", entity.getActivo());

        return data;
    }

    private String clean(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String upper(String value) {
        return hasText(value) ? value.trim().replaceAll("\\s+", " ").toUpperCase() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }
}
