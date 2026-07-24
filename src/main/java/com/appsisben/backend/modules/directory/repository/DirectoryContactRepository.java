package com.appsisben.backend.modules.directory.repository;
import com.appsisben.backend.modules.directory.domain.DirectoryContact;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DirectoryContactRepository extends JpaRepository<DirectoryContact, Long> {
}
