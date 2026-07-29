package com.appsisben.backend.modules.callcenter.api;
import com.appsisben.backend.modules.callcenter.application.CallCenterService;
import com.appsisben.backend.modules.callcenter.dto.*;
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

    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_ASSIGN_FUNCIONARIO)
    @GetMapping("/pendientes-asignar-funcionario")
    public ApiResponse<PageResponse<CallCenterResponse>> pendientesAsignarFuncionario(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("fechaEncuestaProgramada"),
                        Sort.Order.asc("fechaLlamada")
                )
        );

        return ApiResponse.ok(service.pendientesAsignarFuncionario(pageable));
    }

    /**
     * Consulta los casos asignados al funcionario Call Center autenticado.
     *
     * <p>Permite aplicar filtros reales desde base de datos para la pantalla
     * de Mis registros Call Center.</p>
     *
     * @param filter filtros de búsqueda.
     * @param page número de página.
     * @param size tamaño de página.
     * @return página de registros asignados.
     */
    @PreAuthorize(
            "hasAnyAuthority(" +
                    "'ADMIN', 'ROLE_ADMIN', " +
                    "'SUPERVISOR', 'ROLE_SUPERVISOR', " +
                    "'AnyAuthority(" +
                    "'ADMIN', 'ROLE_ADMIN', " +
                    "'SUPERVISOR', 'ROLE_SUPERVISOR', " +
                    "'COORDINADOR_CALLCENTER', 'ROLE_COORDINADOR_CALLCENTER', " +
                    "'FUNCIONARIO_CALLCENTER', 'ROLE_FUNCIONARIO_CALLCENTER'" +
                    ") or hasAnyRole('ADMIN', 'SUPERVISOR', 'COORDINADOR_CALLCENTER', 'FUNCIONARIO_CALLCENTER')"
    )
    @GetMapping("/mis-registros-callcenter")
    public ApiResponse<PageResponse<CallCenterResponse>> misRegistrosCallcenter(
            @ModelAttribute CallCenterFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("fechaEncuestaProgramada"),
                        Sort.Order.asc("fechaLlamada")
                )
        );

        return ApiResponse.ok(service.misRegistrosCallcenter(filter, pageable));
    }

    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_ASSIGN_FUNCIONARIO)
    @GetMapping("/catalogs/funcionarios-callcenter")
    public ApiResponse<List<CallCenterUserOptionResponse>> funcionariosCallcenter() {
        return ApiResponse.ok(service.findFuncionariosCallcenter());
    }

    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_ASSIGN_FUNCIONARIO)
    @PatchMapping("/asignar-funcionario-callcenter")
    public ApiResponse<List<CallCenterResponse>> asignarFuncionarioCallcenter(
            @Valid @RequestBody CallCenterAsignarFuncionarioRequest request
    ) {
        return ApiResponse.ok(
                "Registros asignados al funcionario Call Center correctamente",
                service.asignarFuncionarioCallcenter(request)
        );
    }

    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_ASSIGN_ENCUESTADOR)
    @PatchMapping("/asignar-encuestador")
    public ApiResponse<List<CallCenterResponse>> asignarEncuestador(
            @Valid @RequestBody CallCenterAsignarEncuestadorRequest request
    ) {
        return ApiResponse.ok(
                "Registros asignados al encuestador correctamente",
                service.asignarEncuestador(request)
        );
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