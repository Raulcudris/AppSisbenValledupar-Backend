package com.appsisben.backend.modules.roles.application;

import com.appsisben.backend.modules.roles.domain.Role;
import com.appsisben.backend.modules.roles.dto.RoleResponse;
import com.appsisben.backend.modules.roles.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<RoleResponse> findAll() {
        return roleRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getCodigo(),
                role.getNombre(),
                role.getDescripcion(),
                role.getActivo()
        );
    }
}
