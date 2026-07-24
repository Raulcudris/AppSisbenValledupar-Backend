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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BarrioService {

    private final BarrioRepository barrioRepository;
    private final ComunaRepository comunaRepository;

    @Transactional(readOnly = true)
    public PageResponse<BarrioResponse> findAll(Pageable pageable, String q, Long comunaId, Boolean activo) {
        Page<Barrio> page = barrioRepository.search(normalizeSearch(q), comunaId, activo, pageable);
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
        applyRequest(barrio, request, true);
        return toResponse(barrioRepository.save(barrio));
    }

    @Transactional
    public BarrioResponse update(Long id, BarrioRequest request) {
        Barrio barrio = findEntity(id);
        applyRequest(barrio, request, false);
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

    private void applyRequest(Barrio barrio, BarrioRequest request, boolean creating) {
        String nombre = normalizeNombre(request.nombre());

        Comuna comuna = comunaRepository.findById(request.comunaId())
                .orElseThrow(() -> new ResourceNotFoundException("Comuna no encontrada"));

        if (!Boolean.TRUE.equals(comuna.getActivo())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La comuna seleccionada se encuentra inactiva."
            );
        }

        Long excludeId = creating ? null : barrio.getId();
        boolean duplicated = barrioRepository.existsDuplicatedNameInComuna(nombre, comuna.getId(), excludeId);

        if (duplicated) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un barrio con ese nombre en la comuna seleccionada."
            );
        }

        barrio.setComuna(comuna);
        barrio.setNombre(nombre);

        if (creating) {
            barrio.setActivo(request.activo() != null ? request.activo() : Boolean.TRUE);
        } else if (request.activo() != null) {
            barrio.setActivo(request.activo());
        }
    }

    private String normalizeNombre(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del barrio es obligatorio.");
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeSearch(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
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
