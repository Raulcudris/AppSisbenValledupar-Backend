package com.appsisben.backend.modules.callcenter.repository;

import com.appsisben.backend.modules.callcenter.domain.CallCenterMotivoNoDisposicion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CallCenterMotivoNoDisposicionRepository extends JpaRepository<CallCenterMotivoNoDisposicion, Long> {

    List<CallCenterMotivoNoDisposicion> findByActivoTrueOrderByNombreAsc();
}
