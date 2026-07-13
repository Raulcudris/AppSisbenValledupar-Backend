package com.appsisben.backend.modules.users.repository;

import com.appsisben.backend.modules.users.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Override
    @EntityGraph(attributePaths = "role")
    Page<User> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "role")
    Optional<User> findById(Long id);

    @EntityGraph(attributePaths = "role")
    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    boolean existsByDocumentoIgnoreCase(String documento);

    boolean existsByDocumentoIgnoreCaseAndIdNot(String documento, Long id);
}