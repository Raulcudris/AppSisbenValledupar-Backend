package com.appsisben.backend.modules.catalogs.application;

import com.appsisben.backend.modules.catalogs.domain.Encuestador;
import com.appsisben.backend.modules.catalogs.dto.EncuestadorRequest;
import com.appsisben.backend.modules.catalogs.dto.EncuestadorResponse;
import com.appsisben.backend.modules.catalogs.repository.EncuestadorRepository;
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
public class EncuestadorService {

    private final EncuestadorRepository repository;

    @Transactional(readOnly = true)
    public PageResponse<EncuestadorResponse> findAll(Pageable pageable) {
        Page<Encuestador> page = repository.findAll(pageable);
        List<EncuestadorResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public EncuestadorResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public EncuestadorResponse create(EncuestadorRequest request) {
        Encuestador entity = new Encuestador();
        apply(entity, request);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public EncuestadorResponse update(Long id, EncuestadorRequest request) {
        Encuestador entity = findEntity(id);
        apply(entity, request);
        return toResponse(entity);
    }

    @Transactional
    public EncuestadorResponse setActive(Long id, boolean active) {
        Encuestador entity = findEntity(id);
        entity.setActivo(active);
        return toResponse(entity);
    }

    private Encuestador findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encuestador no encontrado"));
    }

    private void apply(Encuestador entity, EncuestadorRequest request) {
        entity.setNombre(request.nombre().trim().toUpperCase());
        entity.setDocumento(request.documento());
        entity.setTelefono(request.telefono());
        entity.setActivo(request.activo() != null ? request.activo() : Boolean.TRUE);
    }

    private EncuestadorResponse toResponse(Encuestador entity) {
        return new EncuestadorResponse(entity.getId(), entity.getNombre(), entity.getDocumento(), entity.getTelefono(), entity.getActivo());
    }
}
