package com.appsisben.backend.modules.ventanilla.api;

import com.appsisben.backend.modules.ventanilla.application.VentanillaTraceabilityService;
import com.appsisben.backend.modules.ventanilla.dto.VentanillaCiudadanoTraceabilityResponse;
import com.appsisben.backend.security.AppRolePreAuthorize;
import com.appsisben.backend.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ventanilla/trazabilidad")
public class VentanillaTraceabilityController {

    private final VentanillaTraceabilityService service;

    @PreAuthorize(AppRolePreAuthorize.VENTANILLA_READ)
    @GetMapping("/ciudadano")
    public ApiResponse<VentanillaCiudadanoTraceabilityResponse> findByCedula(
            @RequestParam String cedulaUsuario
    ) {
        return ApiResponse.ok(service.findByCedula(cedulaUsuario));
    }
}