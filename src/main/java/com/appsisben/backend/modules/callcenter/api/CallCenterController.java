package com.appsisben.backend.modules.callcenter.api;

import com.appsisben.backend.modules.callcenter.application.CallCenterService;
import com.appsisben.backend.modules.callcenter.dto.CallCenterCatalogResponse;
import com.appsisben.backend.modules.callcenter.dto.CallCenterFilterRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterResponse;
import com.appsisben.backend.modules.callcenter.dto.CallCenterSummaryResponse;
import com.appsisben.backend.modules.callcenter.dto.CallCenterVisitaRequest;
import com.appsisben.backend.security.AppRolePreAuthorize;
import com.appsisben.backend.shared.api.ApiResponse;
import com.appsisben.backend.shared.api.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/callcenter")
public class CallCenterController {

    private final CallCenterService service;

    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_READ)
    @GetMapping
    public ApiResponse<PageResponse<CallCenterResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fechaLlamada").descending());
        return ApiResponse.ok(service.findAll(pageable));
    }

    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_READ)
    @GetMapping("/search")
    public ApiResponse<PageResponse<CallCenterResponse>> search(
            @ModelAttribute CallCenterFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fechaLlamada").descending());
        return ApiResponse.ok(service.search(filter, pageable));
    }

    /*
     * IMPORTANTE:
     * Este endpoint debe estar antes de @GetMapping("/{id}")
     * para evitar que Spring intente interpretar "mis-asignaciones" como un Long.
     */
    @PreAuthorize(
            "hasAnyAuthority(" +
                    "'ADMIN', 'ROLE_ADMIN', " +
                    "'SUPERVISOR', 'ROLE_SUPERVISOR', " +
                    "'FUNCIONARIO_CALLCENTER', 'ROLE_FUNCIONARIO_CALLCENTER', " +
                    "'FUNCIONARIO_ENCUESTADOR', 'ROLE_FUNCIONARIO_ENCUESTADOR'" +
                    ") or hasAnyRole('ADMIN', 'SUPERVISOR', 'FUNCIONARIO_CALLCENTER', 'FUNCIONARIO_ENCUESTADOR')"
    )
    @GetMapping("/mis-asignaciones")
    public ApiResponse<PageResponse<CallCenterResponse>> misAsignaciones(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("fechaAplicacionInformada"),
                        Sort.Order.asc("fechaEncuestaProgramada"),
                        Sort.Order.asc("fechaLlamada")
                )
        );

        return ApiResponse.ok(service.misAsignaciones(pageable));
    }

    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_READ)
    @GetMapping("/catalogs/motivos-no-contacto")
    public ApiResponse<List<CallCenterCatalogResponse>> motivosNoContacto() {
        return ApiResponse.ok(service.findMotivosNoContacto());
    }

    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_READ)
    @GetMapping("/catalogs/motivos-no-disposicion")
    public ApiResponse<List<CallCenterCatalogResponse>> motivosNoDisposicion() {
        return ApiResponse.ok(service.findMotivosNoDisposicion());
    }

    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_READ)
    @GetMapping("/summary")
    public ApiResponse<CallCenterSummaryResponse> summary() {
        return ApiResponse.ok(service.summary());
    }

    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_READ)
    @GetMapping("/{id}")
    public ApiResponse<CallCenterResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_WRITE)
    @PostMapping
    public ApiResponse<CallCenterResponse> create(@Valid @RequestBody CallCenterRequest request) {
        return ApiResponse.ok("Registro Call Center creado correctamente", service.create(request));
    }

    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_WRITE)
    @PutMapping("/{id}")
    public ApiResponse<CallCenterResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CallCenterRequest request
    ) {
        return ApiResponse.ok("Registro Call Center actualizado correctamente", service.update(id, request));
    }

    /*
     * Actualiza únicamente la gestión del encuestador:
     * estado de visita, fecha/hora real, encuesta realizada, motivo, reprogramación,
     * observación del encuestador y verificación.
     */
    @PreAuthorize(
            "hasAnyAuthority(" +
                    "'ADMIN', 'ROLE_ADMIN', " +
                    "'SUPERVISOR', 'ROLE_SUPERVISOR', " +
                    "'FUNCIONARIO_CALLCENTER', 'ROLE_FUNCIONARIO_CALLCENTER', " +
                    "'FUNCIONARIO_ENCUESTADOR', 'ROLE_FUNCIONARIO_ENCUESTADOR'" +
                    ") or hasAnyRole('ADMIN', 'SUPERVISOR', 'FUNCIONARIO_CALLCENTER', 'FUNCIONARIO_ENCUESTADOR')"
    )
    @PatchMapping("/{id}/visita")
    public ApiResponse<CallCenterResponse> updateVisita(
            @PathVariable Long id,
            @Valid @RequestBody CallCenterVisitaRequest request
    ) {
        return ApiResponse.ok("Resultado de visita actualizado correctamente", service.updateVisita(id, request));
    }

    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_WRITE)
    @PatchMapping("/{id}/activate")
    public ApiResponse<CallCenterResponse> activate(@PathVariable Long id) {
        return ApiResponse.ok("Registro Call Center activado correctamente", service.activate(id));
    }

    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_WRITE)
    @PatchMapping("/{id}/deactivate")
    public ApiResponse<CallCenterResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.ok("Registro Call Center inactivado correctamente", service.deactivate(id));
    }

    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_WRITE)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);

        return ApiResponse.ok("Registro Call Center retirado correctamente", null);
    }
}