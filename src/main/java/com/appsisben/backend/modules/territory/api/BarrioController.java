package com.appsisben.backend.modules.territory.api;

import com.appsisben.backend.modules.territory.application.BarrioService;
import com.appsisben.backend.modules.territory.dto.BarrioRequest;
import com.appsisben.backend.modules.territory.dto.BarrioResponse;
import com.appsisben.backend.security.AppRolePreAuthorize;
import com.appsisben.backend.shared.api.ApiResponse;
import com.appsisben.backend.shared.api.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/territory/barrios")
public class BarrioController {

    private final BarrioService barrioService;

    @PreAuthorize(AppRolePreAuthorize.TERRITORY_READ)
    @GetMapping
    public ApiResponse<PageResponse<BarrioResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
        return ApiResponse.ok(barrioService.findAll(pageable));
    }

    @PreAuthorize(AppRolePreAuthorize.TERRITORY_READ)
    @GetMapping("/{id}")
    public ApiResponse<BarrioResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(barrioService.findById(id));
    }

    @PreAuthorize(AppRolePreAuthorize.TERRITORY_WRITE)
    @PostMapping
    public ApiResponse<BarrioResponse> create(@Valid @RequestBody BarrioRequest request) {
        return ApiResponse.ok("Barrio creado correctamente", barrioService.create(request));
    }

    @PreAuthorize(AppRolePreAuthorize.TERRITORY_WRITE)
    @PutMapping("/{id}")
    public ApiResponse<BarrioResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BarrioRequest request
    ) {
        return ApiResponse.ok("Barrio actualizado correctamente", barrioService.update(id, request));
    }

    @PreAuthorize(AppRolePreAuthorize.TERRITORY_WRITE)
    @PatchMapping("/{id}/activate")
    public ApiResponse<BarrioResponse> activate(@PathVariable Long id) {
        return ApiResponse.ok("Barrio activado correctamente", barrioService.setActive(id, true));
    }

    @PreAuthorize(AppRolePreAuthorize.TERRITORY_WRITE)
    @PatchMapping("/{id}/deactivate")
    public ApiResponse<BarrioResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.ok("Barrio inactivado correctamente", barrioService.setActive(id, false));
    }
}
