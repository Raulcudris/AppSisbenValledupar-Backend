package com.appsisben.backend.modules.catalogos.api;

import com.appsisben.backend.modules.catalogos.application.BarrioService;
import com.appsisben.backend.modules.catalogos.dto.ComunaResponse;
import com.appsisben.backend.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/catalogos/comunas")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR','ROLE_ADMIN','ROLE_SUPERVISOR')")
public class ComunaController {

    private final BarrioService service;

    @GetMapping
    public ApiResponse<List<ComunaResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activo
    ) {
        return ApiResponse.ok(service.searchComunas(q, activo));
    }
}
