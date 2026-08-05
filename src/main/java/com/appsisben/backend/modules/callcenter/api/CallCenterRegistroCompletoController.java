package com.appsisben.backend.modules.callcenter.api;

import com.appsisben.backend.modules.callcenter.application.CallCenterRegistroCompletoService;
import com.appsisben.backend.modules.callcenter.dto.CallCenterRegistroCompletoRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterRegistroCompletoResponse;
import com.appsisben.backend.security.AppRolePreAuthorize;
import com.appsisben.backend.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
     * Crea transaccionalmente el caso, la primera llamada
     * y la visita programada.
     */
    @PreAuthorize(
            AppRolePreAuthorize
                    .CALLCENTER_SELF_REGISTER
    )
    @PostMapping("/registro-completo")
    public ApiResponse<CallCenterRegistroCompletoResponse>
    crear(
            @Valid
            @RequestBody
            CallCenterRegistroCompletoRequest request
    ) {
        return ApiResponse.ok(
                "Caso, llamada y visita "
                        + "registrados correctamente",
                service.crear(
                        request
                )
        );
    }

    /**
     * Consulta el agregado completo para edición.
     */
    @PreAuthorize(
            AppRolePreAuthorize
                    .CALLCENTER_SELF_REGISTER
    )
    @GetMapping("/registro-completo/{id}")
    public ApiResponse<CallCenterRegistroCompletoResponse>
    consultar(
            @PathVariable
            Long id
    ) {
        return ApiResponse.ok(
                service.consultar(
                        id
                )
        );
    }

    /**
     * Actualiza transaccionalmente el caso, su última llamada
     * y su visita activa.
     */
    @PreAuthorize(
            AppRolePreAuthorize
                    .CALLCENTER_SELF_REGISTER
    )
    @PutMapping("/registro-completo/{id}")
    public ApiResponse<CallCenterRegistroCompletoResponse>
    actualizar(
            @PathVariable
            Long id,

            @Valid
            @RequestBody
            CallCenterRegistroCompletoRequest request
    ) {
        return ApiResponse.ok(
                "Caso, llamada y visita "
                        + "actualizados correctamente",
                service.actualizar(
                        id,
                        request
                )
        );
    }
}