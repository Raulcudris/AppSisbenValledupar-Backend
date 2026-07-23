package com.appsisben.backend.modules.catalogos.application;

import com.appsisben.backend.modules.catalogos.domain.Barrio;
import com.appsisben.backend.modules.catalogos.domain.Comuna;
import com.appsisben.backend.modules.catalogos.dto.BarrioRequest;
import com.appsisben.backend.modules.catalogos.dto.BarrioResponse;
import com.appsisben.backend.modules.catalogos.dto.ComunaResponse;
import com.appsisben.backend.modules.catalogos.repository.BarrioRepository;
import com.appsisben.backend.modules.catalogos.repository.ComunaRepository;
import lombok.RequiredArgsConstructor;
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
    public List<BarrioResponse> searchBarrios(String q, Long comunaId, Boolean activo) {
        return barrioRepository.search(normalizeSearch(q), comunaId, activo)
                .stream()
                .map(BarrioResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BarrioResponse getBarrio(Long id) {
        return BarrioResponse.from(findBarrio(id));
    }

    @Transactional
    public BarrioResponse createBarrio(BarrioRequest request) {
        String nombre = normalizeNombre(request.nombre());
        Comuna comuna = findComunaActiva(request.comunaId());

        validateDuplicate(nombre, comuna.getId(), null);

        Barrio barrio = new Barrio();
        barrio.setNombre(nombre);
        barrio.setComuna(comuna);
        barrio.setActivo(request.activo() == null || request.activo());

        return BarrioResponse.from(barrioRepository.save(barrio));
    }

    @Transactional
    public BarrioResponse updateBarrio(Long id, BarrioRequest request) {
        Barrio barrio = findBarrio(id);

        String nombre = normalizeNombre(request.nombre());
        Comuna comuna = findComunaActiva(request.comunaId());

        validateDuplicate(nombre, comuna.getId(), id);

        barrio.setNombre(nombre);
        barrio.setComuna(comuna);

        if (request.activo() != null) {
            barrio.setActivo(request.activo());
        }

        return BarrioResponse.from(barrioRepository.save(barrio));
    }

    @Transactional
    public BarrioResponse inactivateBarrio(Long id) {
        Barrio barrio = findBarrio(id);
        barrio.setActivo(false);

        return BarrioResponse.from(barrioRepository.save(barrio));
    }

    @Transactional
    public BarrioResponse reactivateBarrio(Long id) {
        Barrio barrio = findBarrio(id);
        barrio.setActivo(true);

        return BarrioResponse.from(barrioRepository.save(barrio));
    }

    @Transactional(readOnly = true)
    public List<ComunaResponse> searchComunas(String q, Boolean activo) {
        return comunaRepository.search(normalizeSearch(q), activo)
                .stream()
                .map(ComunaResponse::from)
                .toList();
    }

    private Barrio findBarrio(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id del barrio es obligatorio.");
        }

        return barrioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró el barrio solicitado."
                ));
    }

    private Comuna findComunaActiva(Long comunaId) {
        if (comunaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La comuna es obligatoria.");
        }

        Comuna comuna = comunaRepository.findById(comunaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró la comuna seleccionada."
                ));

        if (!Boolean.TRUE.equals(comuna.getActivo())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La comuna seleccionada se encuentra inactiva."
            );
        }

        return comuna;
    }

    private void validateDuplicate(String nombre, Long comunaId, Long excludeId) {
        boolean exists = barrioRepository.existsDuplicatedNameInComuna(nombre, comunaId, excludeId);

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un barrio con ese nombre en la comuna seleccionada."
            );
        }
    }

    private String normalizeNombre(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del barrio es obligatorio.");
        }

        if (value.trim().length() > 150) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre del barrio no puede superar 150 caracteres."
            );
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeSearch(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
