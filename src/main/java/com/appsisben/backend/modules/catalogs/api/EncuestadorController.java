package com.appsisben.backend.modules.catalogs.api;

import com.appsisben.backend.modules.catalogs.application.EncuestadorService;
import com.appsisben.backend.modules.catalogs.dto.EncuestadorRequest;
import com.appsisben.backend.modules.catalogs.dto.EncuestadorResponse;
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
@RequestMapping("/api/catalogs/encuestadores")
public class EncuestadorController {

    private final EncuestadorService service;

    @PreAuthorize(AppRolePreAuthorize.CATALOG_READ)
    @GetMapping
    public ApiResponse<PageResponse<EncuestadorResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
        return ApiResponse.ok(service.findAll(pageable));
    }

    @PreAuthorize(AppRolePreAuthorize.CATALOG_READ)
    @GetMapping("/{id}")
    public ApiResponse<EncuestadorResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PreAuthorize(AppRolePreAuthorize.CATALOG_WRITE)
    @PostMapping
    public ApiResponse<EncuestadorResponse> create(@Valid @RequestBody EncuestadorRequest request) {
        return ApiResponse.ok("Encuestador creado correctamente", service.create(request));
    }

    @PreAuthorize(AppRolePreAuthorize.CATALOG_WRITE)
    @PutMapping("/{id}")
    public ApiResponse<EncuestadorResponse> update(@PathVariable Long id, @Valid @RequestBody EncuestadorRequest request) {
        return ApiResponse.ok("Encuestador actualizado correctamente", service.update(id, request));
    }

    @PreAuthorize(AppRolePreAuthorize.CATALOG_WRITE)
    @PatchMapping("/{id}/activate")
    public ApiResponse<EncuestadorResponse> activate(@PathVariable Long id) {
        return ApiResponse.ok("Encuestador activado correctamente", service.setActive(id, true));
    }

    @PreAuthorize(AppRolePreAuthorize.CATALOG_WRITE)
    @PatchMapping("/{id}/deactivate")
    public ApiResponse<EncuestadorResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.ok("Encuestador inactivado correctamente", service.setActive(id, false));
    }
}
