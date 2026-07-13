package com.appsisben.backend.modules.dmc.application;

import com.appsisben.backend.modules.audit.application.AuditService;
import com.appsisben.backend.modules.audit.domain.AuditAction;
import com.appsisben.backend.modules.catalogs.domain.Encuestador;
import com.appsisben.backend.modules.catalogs.domain.TipoDmc;
import com.appsisben.backend.modules.catalogs.repository.EncuestadorRepository;
import com.appsisben.backend.modules.catalogs.repository.TipoDmcRepository;
import com.appsisben.backend.modules.dmc.domain.DmcRegistro;
import com.appsisben.backend.modules.dmc.dto.DmcFilterRequest;
import com.appsisben.backend.modules.dmc.dto.DmcRequest;
import com.appsisben.backend.modules.dmc.dto.DmcResponse;
import com.appsisben.backend.modules.dmc.repository.DmcRegistroRepository;
import com.appsisben.backend.modules.dmc.repository.DmcSpecification;
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
public class DmcService {

    private final DmcRegistroRepository repository;
    private final UserRepository userRepository;
    private final TipoDmcRepository tipoDmcRepository;
    private final EncuestadorRepository encuestadorRepository;
    private final BarrioRepository barrioRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<DmcResponse> findAll(Pageable pageable) {
        Page<DmcRegistro> page = repository.findAll(pageable);
        List<DmcResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public PageResponse<DmcResponse> search(DmcFilterRequest filter, Pageable pageable) {
        Page<DmcRegistro> page = repository.findAll(DmcSpecification.byFilter(filter), pageable);
        List<DmcResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public DmcResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public DmcResponse create(DmcRequest request) {
        DmcRegistro entity = new DmcRegistro();
        entity.setFuncionario(currentUser());
        apply(entity, request);

        DmcRegistro saved = repository.save(entity);

        auditService.safeLog(
                AuditAction.CREATE,
                "dmc_registro",
                saved.getId(),
                null,
                snapshot(saved)
        );

        return toResponse(saved);
    }

    @Transactional
    public DmcResponse update(Long id, DmcRequest request) {
        DmcRegistro entity = findEntity(id);
        Map<String, Object> before = snapshot(entity);

        apply(entity, request);

        auditService.safeLog(
                AuditAction.UPDATE,
                "dmc_registro",
                entity.getId(),
                before,
                snapshot(entity)
        );

        return toResponse(entity);
    }

    private DmcRegistro findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro DMC no encontrado"));
    }

    private void apply(DmcRegistro entity, DmcRequest request) {
        TipoDmc tipoDmc = tipoDmcRepository.findById(request.tipoDmcId())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo DMC no encontrado"));

        Encuestador encuestador = encuestadorRepository.findById(request.encuestadorId())
                .orElseThrow(() -> new ResourceNotFoundException("Encuestador no encontrado"));

        Barrio barrio = barrioRepository.findById(request.barrioId())
                .orElseThrow(() -> new ResourceNotFoundException("Barrio no encontrado"));

        entity.setFecha(request.fecha());
        entity.setTipoDmc(tipoDmc);
        entity.setEncuestador(encuestador);
        entity.setCantidad(request.cantidad());
        entity.setObservacion(request.observacion());
        entity.setBarrio(barrio);
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            throw new BusinessException("No hay usuario autenticado");
        }

        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    private DmcResponse toResponse(DmcRegistro entity) {
        return new DmcResponse(
                entity.getId(),
                entity.getFecha(),
                entity.getFuncionario().getId(),
                entity.getFuncionario().getUsername(),
                entity.getTipoDmc().getId(),
                entity.getTipoDmc().getCodigo(),
                entity.getTipoDmc().getNombre(),
                entity.getEncuestador().getId(),
                entity.getEncuestador().getNombre(),
                entity.getCantidad(),
                entity.getObservacion(),
                entity.getBarrio().getId(),
                entity.getBarrio().getNombre(),
                entity.getBarrio().getComuna().getNombre()
        );
    }

    private Map<String, Object> snapshot(DmcRegistro entity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", entity.getId());
        data.put("fecha", entity.getFecha());
        data.put("funcionarioId", entity.getFuncionario() != null ? entity.getFuncionario().getId() : null);
        data.put("funcionarioUsername", entity.getFuncionario() != null ? entity.getFuncionario().getUsername() : null);
        data.put("tipoDmcId", entity.getTipoDmc() != null ? entity.getTipoDmc().getId() : null);
        data.put("tipoDmcCodigo", entity.getTipoDmc() != null ? entity.getTipoDmc().getCodigo() : null);
        data.put("tipoDmcNombre", entity.getTipoDmc() != null ? entity.getTipoDmc().getNombre() : null);
        data.put("encuestadorId", entity.getEncuestador() != null ? entity.getEncuestador().getId() : null);
        data.put("encuestadorNombre", entity.getEncuestador() != null ? entity.getEncuestador().getNombre() : null);
        data.put("cantidad", entity.getCantidad());
        data.put("barrioId", entity.getBarrio() != null ? entity.getBarrio().getId() : null);
        data.put("barrioNombre", entity.getBarrio() != null ? entity.getBarrio().getNombre() : null);
        data.put("observacion", entity.getObservacion());
        return data;
    }
}
