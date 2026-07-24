package com.appsisben.backend.modules.catalogs.api;
import com.appsisben.backend.modules.catalogs.application.EstadoSolicitudService;
import com.appsisben.backend.modules.catalogs.dto.CodeCatalogRequest;
import com.appsisben.backend.modules.catalogs.dto.CodeCatalogResponse;
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
@RequestMapping("/api/catalogs/estados-solicitud")
public class EstadoSolicitudController {

    private final EstadoSolicitudService service;

    @PreAuthorize(AppRolePreAuthorize.CATALOG_READ)
    @GetMapping
    public ApiResponse<PageResponse<CodeCatalogResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
        return ApiResponse.ok(service.findAll(pageable));
    }

    @PreAuthorize(AppRolePreAuthorize.CATALOG_READ)
    @GetMapping("/{id}")
    public ApiResponse<CodeCatalogResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PreAuthorize(AppRolePreAuthorize.CATALOG_WRITE)
    @PostMapping
    public ApiResponse<CodeCatalogResponse> create(@Valid @RequestBody CodeCatalogRequest request) {
        return ApiResponse.ok("Estado creado correctamente", service.create(request));
    }

    @PreAuthorize(AppRolePreAuthorize.CATALOG_WRITE)
    @PutMapping("/{id}")
    public ApiResponse<CodeCatalogResponse> update(@PathVariable Long id, @Valid @RequestBody CodeCatalogRequest request) {
        return ApiResponse.ok("Estado actualizado correctamente", service.update(id, request));
    }

    @PreAuthorize(AppRolePreAuthorize.CATALOG_WRITE)
    @PatchMapping("/{id}/activate")
    public ApiResponse<CodeCatalogResponse> activate(@PathVariable Long id) {
        return ApiResponse.ok("Estado activado correctamente", service.setActive(id, true));
    }

    @PreAuthorize(AppRolePreAuthorize.CATALOG_WRITE)
    @PatchMapping("/{id}/deactivate")
    public ApiResponse<CodeCatalogResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.ok("Estado inactivado correctamente", service.setActive(id, false));
    }
}
