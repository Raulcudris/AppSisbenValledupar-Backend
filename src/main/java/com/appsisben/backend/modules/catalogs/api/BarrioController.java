package com.appsisben.backend.modules.catalogs.api;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/catalogos/barrios")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPERVISOR','ROLE_ADMIN','ROLE_SUPERVISOR')")
public class BarrioController {

    private final com.appsisben.backend.modules.catalogos.application.BarrioService service;

    @GetMapping
    public ApiResponse<List<com.appsisben.backend.modules.catalogos.dto.BarrioResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long comunaId,
            @RequestParam(required = false) Boolean activo
    ) {
        return ApiResponse.ok(service.searchBarrios(q, comunaId, activo));
    }

    @GetMapping("/{id}")
    public ApiResponse<BarrioResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(service.getBarrio(id));
    }

    @PostMapping
    public ApiResponse<BarrioResponse> create(@RequestBody BarrioRequest request) {
        return ApiResponse.ok(service.createBarrio(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<BarrioResponse> update(
            @PathVariable Long id,
            @RequestBody BarrioRequest request
    ) {
        return ApiResponse.ok(service.updateBarrio(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<BarrioResponse> inactivate(@PathVariable Long id) {
        return ApiResponse.ok(service.inactivateBarrio(id));
    }

    @PatchMapping("/{id}/reactivar")
    public ApiResponse<BarrioResponse> reactivate(@PathVariable Long id) {
        return ApiResponse.ok(service.reactivateBarrio(id));
    }
}

