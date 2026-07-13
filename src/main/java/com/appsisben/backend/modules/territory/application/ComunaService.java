package com.appsisben.backend.modules.territory.application;

import com.appsisben.backend.modules.territory.domain.Comuna;
import com.appsisben.backend.modules.territory.dto.ComunaRequest;
import com.appsisben.backend.modules.territory.dto.ComunaResponse;
import com.appsisben.backend.modules.territory.repository.ComunaRepository;
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
public class ComunaService {

    private final ComunaRepository comunaRepository;

    @Transactional(readOnly = true)
    public PageResponse<ComunaResponse> findAll(Pageable pageable) {
        Page<Comuna> page = comunaRepository.findAll(pageable);
        List<ComunaResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public ComunaResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public ComunaResponse create(ComunaRequest request) {
        Comuna comuna = new Comuna();
        applyRequest(comuna, request);
        return toResponse(comunaRepository.save(comuna));
    }

    @Transactional
    public ComunaResponse update(Long id, ComunaRequest request) {
        Comuna comuna = findEntity(id);
        applyRequest(comuna, request);
        return toResponse(comuna);
    }

    @Transactional
    public ComunaResponse setActive(Long id, boolean active) {
        Comuna comuna = findEntity(id);
        comuna.setActivo(active);
        return toResponse(comuna);
    }

    private Comuna findEntity(Long id) {
        return comunaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comuna no encontrada"));
    }

    private void applyRequest(Comuna comuna, ComunaRequest request) {
        comuna.setCodigo(request.codigo().trim().toUpperCase());
        comuna.setNombre(request.nombre().trim().toUpperCase());
        comuna.setActivo(request.activo() != null ? request.activo() : Boolean.TRUE);
    }

    private ComunaResponse toResponse(Comuna comuna) {
        return new ComunaResponse(
                comuna.getId(),
                comuna.getCodigo(),
                comuna.getNombre(),
                comuna.getActivo()
        );
    }
}
