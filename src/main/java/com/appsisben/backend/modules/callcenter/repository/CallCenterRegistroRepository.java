package com.appsisben.backend.modules.callcenter.repository;

import com.appsisben.backend.modules.callcenter.domain.CallCenterRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CallCenterRegistroRepository extends JpaRepository<CallCenterRegistro, Long>,
        JpaSpecificationExecutor<CallCenterRegistro> {
}
