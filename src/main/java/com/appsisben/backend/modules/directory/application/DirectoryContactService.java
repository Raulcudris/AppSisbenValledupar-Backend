package com.appsisben.backend.modules.directory.application;
import com.appsisben.backend.modules.directory.domain.DirectoryContact;
import com.appsisben.backend.modules.directory.dto.DirectoryContactRequest;
import com.appsisben.backend.modules.directory.dto.DirectoryContactResponse;
import com.appsisben.backend.modules.directory.repository.DirectoryContactRepository;
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
public class DirectoryContactService {

    private final DirectoryContactRepository repository;

    @Transactional(readOnly = true)
    public PageResponse<DirectoryContactResponse> findAll(Pageable pageable) {
        Page<DirectoryContact> page = repository.findAll(pageable);
        List<DirectoryContactResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public DirectoryContactResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public DirectoryContactResponse create(DirectoryContactRequest request) {
        DirectoryContact entity = new DirectoryContact();
        apply(entity, request);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public DirectoryContactResponse update(Long id, DirectoryContactRequest request) {
        DirectoryContact entity = findEntity(id);
        apply(entity, request);
        return toResponse(entity);
    }

    @Transactional
    public DirectoryContactResponse setActive(Long id, boolean active) {
        DirectoryContact entity = findEntity(id);
        entity.setActivo(active);
        return toResponse(entity);
    }

    private DirectoryContact findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contacto de directorio no encontrado"));
    }

    private void apply(DirectoryContact entity, DirectoryContactRequest request) {
        entity.setNombre(request.nombre().trim().toUpperCase());
        entity.setTelefono(request.telefono().trim());
        entity.setPerfil(request.perfil().trim().toUpperCase());
        entity.setEmail(request.email());
        entity.setEntidad(request.entidad());
        entity.setActivo(request.activo() != null ? request.activo() : Boolean.TRUE);
    }

    private DirectoryContactResponse toResponse(DirectoryContact entity) {
        return new DirectoryContactResponse(
                entity.getId(),
                entity.getNombre(),
                entity.getTelefono(),
                entity.getPerfil(),
                entity.getEmail(),
                entity.getEntidad(),
                entity.getActivo()
        );
    }
}
