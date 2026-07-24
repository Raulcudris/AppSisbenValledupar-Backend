package com.appsisben.backend.modules.territory.api;
import com.appsisben.backend.modules.territory.application.ComunaService;
import com.appsisben.backend.modules.territory.dto.ComunaRequest;
import com.appsisben.backend.modules.territory.dto.ComunaResponse;
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
@RequestMapping("/api/territory/comunas")
public class ComunaController {

    private final ComunaService comunaService;

    @PreAuthorize(AppRolePreAuthorize.TERRITORY_READ)
    @GetMapping
    public ApiResponse<PageResponse<ComunaResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activo
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by("nombre").ascending()
        );

        return ApiResponse.ok(comunaService.findAll(pageable, q, activo));
    }

    @PreAuthorize(AppRolePreAuthorize.TERRITORY_READ)
    @GetMapping("/{id}")
    public ApiResponse<ComunaResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(comunaService.findById(id));
    }

    @PreAuthorize(AppRolePreAuthorize.TERRITORY_WRITE)
    @PostMapping
    public ApiResponse<ComunaResponse> create(@Valid @RequestBody ComunaRequest request) {
        return ApiResponse.ok("Comuna creada correctamente", comunaService.create(request));
    }

    @PreAuthorize(AppRolePreAuthorize.TERRITORY_WRITE)
    @PutMapping("/{id}")
    public ApiResponse<ComunaResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ComunaRequest request
    ) {
        return ApiResponse.ok("Comuna actualizada correctamente", comunaService.update(id, request));
    }

    @PreAuthorize(AppRolePreAuthorize.TERRITORY_WRITE)
    @PatchMapping("/{id}/activate")
    public ApiResponse<ComunaResponse> activate(@PathVariable Long id) {
        return ApiResponse.ok("Comuna activada correctamente", comunaService.setActive(id, true));
    }

    @PreAuthorize(AppRolePreAuthorize.TERRITORY_WRITE)
    @PatchMapping("/{id}/deactivate")
    public ApiResponse<ComunaResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.ok("Comuna inactivada correctamente", comunaService.setActive(id, false));
    }
}
