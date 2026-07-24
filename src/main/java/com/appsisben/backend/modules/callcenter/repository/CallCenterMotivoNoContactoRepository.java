package com.appsisben.backend.modules.callcenter.repository;

import com.appsisben.backend.modules.callcenter.domain.CallCenterMotivoNoContacto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CallCenterMotivoNoContactoRepository extends JpaRepository<CallCenterMotivoNoContacto, Long> {

    List<CallCenterMotivoNoContacto> findByActivoTrueOrderByNombreAsc();
}
