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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ComunaService {

    private final ComunaRepository comunaRepository;

    @Transactional(readOnly = true)
    public PageResponse<ComunaResponse> findAll(Pageable pageable, String q, Boolean activo) {
        Page<Comuna> page = comunaRepository.search(normalizeSearch(q), activo, pageable);
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

        comuna.setCodigo(generateNextCodigo());
        applyRequest(comuna, request, true);

        return toResponse(comunaRepository.save(comuna));
    }

    @Transactional
    public ComunaResponse update(Long id, ComunaRequest request) {
        Comuna comuna = findEntity(id);

        /*
         * El código no se modifica en actualización.
         * Esto conserva la secuencia C001, C002, C003...
         * y mantiene la trazabilidad de registros asociados.
         */
        applyRequest(comuna, request, false);

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

    private void applyRequest(Comuna comuna, ComunaRequest request, boolean creating) {
        String nombre = normalizeNombre(request.nombre());
        Long excludeId = creating ? null : comuna.getId();

        if (comunaRepository.existsDuplicatedNombre(nombre, excludeId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una comuna con ese nombre."
            );
        }

        comuna.setNombre(nombre);
        comuna.setEstrato(validateEstrato(request.estrato()));
        comuna.setDescripcion(normalizeDescripcion(request.descripcion()));

        if (creating) {
            comuna.setActivo(request.activo() != null ? request.activo() : Boolean.TRUE);
        } else if (request.activo() != null) {
            comuna.setActivo(request.activo());
        }
    }

    private String generateNextCodigo() {
        Long maxCodigoNumber = comunaRepository.findMaxCodigoNumber();

        long nextNumber = (maxCodigoNumber == null ? 0L : maxCodigoNumber) + 1L;

        return "C" + String.format("%03d", nextNumber);
    }

    private Byte validateEstrato(Integer value) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estrato de la comuna es obligatorio.");
        }

        if (value < 1 || value > 6) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El estrato de la comuna debe estar entre 1 y 6."
            );
        }

        return value.byteValue();
    }

    private String normalizeNombre(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de la comuna es obligatorio.");
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeDescripcion(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeSearch(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private ComunaResponse toResponse(Comuna comuna) {
        return new ComunaResponse(
                comuna.getId(),
                comuna.getCodigo(),
                comuna.getNombre(),
                comuna.getEstrato() != null ? comuna.getEstrato().intValue() : null,
                comuna.getDescripcion(),
                comuna.getActivo()
        );
    }
}
