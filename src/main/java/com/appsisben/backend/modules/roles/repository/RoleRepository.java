package com.appsisben.backend.modules.roles.repository;

import com.appsisben.backend.modules.roles.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCodigoIgnoreCase(String codigo);
}