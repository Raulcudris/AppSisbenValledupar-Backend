package com.appsisben.backend.modules.catalogs.application;
import com.appsisben.backend.modules.catalogs.domain.EstadoSolicitud;
import com.appsisben.backend.modules.catalogs.dto.CodeCatalogRequest;
import com.appsisben.backend.modules.catalogs.dto.CodeCatalogResponse;
import com.appsisben.backend.modules.catalogs.repository.EstadoSolicitudRepository;
import com.appsisben.backend.shared.api.PageResponse;
import com.appsisben.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EstadoSolicitudService {

    private final EstadoSolicitudRepository repository;

    @Transactional(readOnly = true)
    public PageResponse<CodeCatalogResponse> findAll(Pageable pageable) {
        Page<EstadoSolicitud> page = repository.findAll(pageable);
        List<CodeCatalogResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public CodeCatalogResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public CodeCatalogResponse create(CodeCatalogRequest request) {
        EstadoSolicitud entity = new EstadoSolicitud();
        apply(entity, request);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public CodeCatalogResponse update(Long id, CodeCatalogRequest request) {
        EstadoSolicitud entity = findEntity(id);
        apply(entity, request);
        return toResponse(entity);
    }

    @Transactional
    public CodeCatalogResponse setActive(Long id, boolean active) {
        EstadoSolicitud entity = findEntity(id);
        entity.setActivo(active);
        return toResponse(entity);
    }

    private EstadoSolicitud findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estado de solicitud no encontrado"));
    }

    private void apply(EstadoSolicitud entity, CodeCatalogRequest request) {
        entity.setCodigo(request.codigo().trim().toUpperCase());
        entity.setNombre(request.nombre().trim());
        entity.setDescripcion(request.descripcion());
        entity.setActivo(request.activo() != null ? request.activo() : Boolean.TRUE);
    }

    private CodeCatalogResponse toResponse(EstadoSolicitud entity) {
        return new CodeCatalogResponse(entity.getId(), entity.getCodigo(), entity.getNombre(), entity.getDescripcion(), entity.getActivo());
    }
}
