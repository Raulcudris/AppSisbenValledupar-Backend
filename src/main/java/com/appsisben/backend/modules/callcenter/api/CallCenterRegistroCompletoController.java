package com.appsisben.backend.modules.callcenter.api;

import com.appsisben.backend.modules.callcenter.application.CallCenterRegistroCompletoService;
import com.appsisben.backend.modules.callcenter.dto.CallCenterRegistroCompletoRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterRegistroCompletoResponse;
import com.appsisben.backend.security.AppRolePreAuthorize;
import com.appsisben.backend.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador del registro completo realizado por el
 * usuario autenticado autorizado.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/callcenter")
public class CallCenterRegistroCompletoController {

    private final CallCenterRegistroCompletoService
            service;

    /**
     * Crea transaccionalmente:
     *
     * 1. El caso maestro.
     * 2. La primera gestión telefónica.
     * 3. La visita programada.
     *
     * El responsable se obtiene desde el usuario autenticado.
     *
     * @param request datos completos del registro.
     * @return caso, llamada y visita creados.
     */
    @PreAuthorize(
            AppRolePreAuthorize
                    .CALLCENTER_SELF_REGISTER
    )
    @PostMapping("/registro-completo")
    public ApiResponse<
            CallCenterRegistroCompletoResponse
            > crear(
            @Valid
            @RequestBody
            CallCenterRegistroCompletoRequest request
    ) {
        return ApiResponse.ok(
                "Caso, llamada y visita "
                        + "registrados correctamente",
                service.crear(request)
        );
    }
}