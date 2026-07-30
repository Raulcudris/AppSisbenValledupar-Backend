package com.appsisben.backend.modules.catalogs.application;

import com.appsisben.backend.modules.catalogs.domain.TipoDmc;
import com.appsisben.backend.modules.catalogs.dto.CodeCatalogRequest;
import com.appsisben.backend.modules.catalogs.dto.CodeCatalogResponse;
import com.appsisben.backend.modules.catalogs.repository.TipoDmcRepository;
import com.appsisben.backend.shared.api.PageResponse;
import com.appsisben.backend.shared.exception.BusinessException;
import com.appsisben.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TipoDmcService {

    private static final Set<String> ALLOWED_CODES =
            Set.of("CARGADAS", "DESCARGADAS");

    private final TipoDmcRepository repository;

    @Transactional(readOnly = true)
    public PageResponse<CodeCatalogResponse> findAll(Pageable pageable) {
        Page<TipoDmc> page = repository.findAll(pageable);

        List<CodeCatalogResponse> content = page.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public CodeCatalogResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public CodeCatalogResponse create(CodeCatalogRequest request) {
        String codigo = normalizeCode(request.codigo());

        validateAllowedCode(codigo);

        if (repository.existsByCodigoIgnoreCase(codigo)) {
            throw new BusinessException(
                    "Ya existe un tipo DMC con el código " + codigo
            );
        }

        TipoDmc entity = new TipoDmc();
        entity.setCodigo(codigo);

        applyEditableFields(entity, request, true);

        return toResponse(repository.save(entity));
    }

    @Transactional
    public CodeCatalogResponse update(
            Long id,
            CodeCatalogRequest request
    ) {
        TipoDmc entity = findEntity(id);
        String codigo = normalizeCode(request.codigo());

        validateAllowedCode(codigo);
        validateCodeCannotChange(entity, codigo);

        applyEditableFields(entity, request, false);

        /*
         * No es necesario llamar repository.save(entity).
         * La entidad está administrada por JPA dentro de la transacción.
         */
        return toResponse(entity);
    }

    @Transactional
    public CodeCatalogResponse setActive(
            Long id,
            boolean active
    ) {
        TipoDmc entity = findEntity(id);

        /*
         * Regla recomendada:
         * CARGADAS y DESCARGADAS son tipos técnicos obligatorios.
         */
        if (!active) {
            throw new BusinessException(
                    "Los tipos DMC CARGADAS y DESCARGADAS no pueden inactivarse"
            );
        }

        entity.setActivo(true);

        return toResponse(entity);
    }

    private TipoDmc findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Tipo DMC no encontrado"
                        )
                );
    }

    private void applyEditableFields(
            TipoDmc entity,
            CodeCatalogRequest request,
            boolean creating
    ) {
        entity.setNombre(request.nombre().trim());
        entity.setDescripcion(normalizeOptionalText(request.descripcion()));

        if (creating) {
            entity.setActivo(
                    request.activo() != null
                            ? request.activo()
                            : Boolean.TRUE
            );
            return;
        }

        /*
         * Durante una actualización, activo null significa:
         * conservar el estado actual.
         */
        if (request.activo() != null) {
            entity.setActivo(request.activo());
        }
    }

    private void validateAllowedCode(String codigo) {
        if (!ALLOWED_CODES.contains(codigo)) {
            throw new BusinessException(
                    "Los únicos tipos DMC permitidos son CARGADAS y DESCARGADAS"
            );
        }
    }

    private void validateCodeCannotChange(
            TipoDmc entity,
            String requestedCode
    ) {
        if (
                entity.getCodigo() != null
                        && !entity.getCodigo()
                        .equalsIgnoreCase(requestedCode)
        ) {
            throw new BusinessException(
                    "El código de un tipo DMC existente no puede modificarse"
            );
        }
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(
                    "El código del tipo DMC es obligatorio"
            );
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private CodeCatalogResponse toResponse(TipoDmc entity) {
        return new CodeCatalogResponse(
                entity.getId(),
                entity.getCodigo(),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.getActivo()
        );
    }
}