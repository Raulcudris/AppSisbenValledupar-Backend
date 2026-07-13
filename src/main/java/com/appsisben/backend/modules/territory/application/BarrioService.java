package com.appsisben.backend.modules.territory.application;

import com.appsisben.backend.modules.territory.domain.Barrio;
import com.appsisben.backend.modules.territory.domain.Comuna;
import com.appsisben.backend.modules.territory.dto.BarrioRequest;
import com.appsisben.backend.modules.territory.dto.BarrioResponse;
import com.appsisben.backend.modules.territory.repository.BarrioRepository;
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
public class BarrioService {

    private final BarrioRepository barrioRepository;
    private final ComunaRepository comunaRepository;

    @Transactional(readOnly = true)
    public PageResponse<BarrioResponse> findAll(Pageable pageable) {
        Page<Barrio> page = barrioRepository.findAll(pageable);
        List<BarrioResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public BarrioResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public BarrioResponse create(BarrioRequest request) {
        Barrio barrio = new Barrio();
        applyRequest(barrio, request);
        return toResponse(barrioRepository.save(barrio));
    }

    @Transactional
    public BarrioResponse update(Long id, BarrioRequest request) {
        Barrio barrio = findEntity(id);
        applyRequest(barrio, request);
        return toResponse(barrio);
    }

    @Transactional
    public BarrioResponse setActive(Long id, boolean active) {
        Barrio barrio = findEntity(id);
        barrio.setActivo(active);
        return toResponse(barrio);
    }

    private Barrio findEntity(Long id) {
        return barrioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Barrio no encontrado"));
    }

    private void applyRequest(Barrio barrio, BarrioRequest request) {
        Comuna comuna = comunaRepository.findById(request.comunaId())
                .orElseThrow(() -> new ResourceNotFoundException("Comuna no encontrada"));

        barrio.setComuna(comuna);
        barrio.setNombre(request.nombre().trim().toUpperCase());
        barrio.setActivo(request.activo() != null ? request.activo() : Boolean.TRUE);
    }

    private BarrioResponse toResponse(Barrio barrio) {
        return new BarrioResponse(
                barrio.getId(),
                barrio.getComuna().getId(),
                barrio.getComuna().getNombre(),
                barrio.getNombre(),
                barrio.getActivo()
        );
    }
}
