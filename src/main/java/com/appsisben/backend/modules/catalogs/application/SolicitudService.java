package com.appsisben.backend.modules.catalogs.application;

import com.appsisben.backend.modules.catalogs.domain.Solicitud;
import com.appsisben.backend.modules.catalogs.dto.SimpleCatalogRequest;
import com.appsisben.backend.modules.catalogs.dto.SimpleCatalogResponse;
import com.appsisben.backend.modules.catalogs.repository.SolicitudRepository;
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
public class SolicitudService {

    private final SolicitudRepository repository;

    @Transactional(readOnly = true)
    public PageResponse<SimpleCatalogResponse> findAll(Pageable pageable) {
        Page<Solicitud> page = repository.findAll(pageable);
        List<SimpleCatalogResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public SimpleCatalogResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public SimpleCatalogResponse create(SimpleCatalogRequest request) {
        Solicitud entity = new Solicitud();
        apply(entity, request);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public SimpleCatalogResponse update(Long id, SimpleCatalogRequest request) {
        Solicitud entity = findEntity(id);
        apply(entity, request);
        return toResponse(entity);
    }

    @Transactional
    public SimpleCatalogResponse setActive(Long id, boolean active) {
        Solicitud entity = findEntity(id);
        entity.setActivo(active);
        return toResponse(entity);
    }

    private Solicitud findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));
    }

    private void apply(Solicitud entity, SimpleCatalogRequest request) {
        entity.setNombre(request.nombre().trim().toUpperCase());
        entity.setDescripcion(request.descripcion());
        entity.setActivo(request.activo() != null ? request.activo() : Boolean.TRUE);
    }

    private SimpleCatalogResponse toResponse(Solicitud entity) {
        return new SimpleCatalogResponse(entity.getId(), entity.getNombre(), entity.getDescripcion(), entity.getActivo());
    }
}
