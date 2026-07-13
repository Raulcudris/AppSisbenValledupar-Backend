package com.appsisben.backend.modules.dmc.api;

import com.appsisben.backend.modules.dmc.application.DmcService;
import com.appsisben.backend.modules.dmc.dto.DmcFilterRequest;
import com.appsisben.backend.modules.dmc.dto.DmcRequest;
import com.appsisben.backend.modules.dmc.dto.DmcResponse;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dmc")
public class DmcController {

    private final DmcService service;

    @PreAuthorize(AppRolePreAuthorize.DMC_READ)
    @GetMapping
    public ApiResponse<PageResponse<DmcResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
        return ApiResponse.ok(service.findAll(pageable));
    }

    @PreAuthorize(AppRolePreAuthorize.DMC_READ)
    @GetMapping("/search")
    public ApiResponse<PageResponse<DmcResponse>> search(
            @ModelAttribute DmcFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
        return ApiResponse.ok(service.search(filter, pageable));
    }

    @PreAuthorize(AppRolePreAuthorize.DMC_READ)
    @GetMapping("/{id}")
    public ApiResponse<DmcResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PreAuthorize(AppRolePreAuthorize.DMC_WRITE)
    @PostMapping
    public ApiResponse<DmcResponse> create(@Valid @RequestBody DmcRequest request) {
        return ApiResponse.ok("Registro DMC creado correctamente", service.create(request));
    }

    @PreAuthorize(AppRolePreAuthorize.DMC_WRITE)
    @PutMapping("/{id}")
    public ApiResponse<DmcResponse> update(@PathVariable Long id, @Valid @RequestBody DmcRequest request) {
        return ApiResponse.ok("Registro DMC actualizado correctamente", service.update(id, request));
    }
}
