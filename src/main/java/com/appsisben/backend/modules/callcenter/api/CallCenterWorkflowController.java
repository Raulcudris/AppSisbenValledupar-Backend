package com.appsisben.backend.modules.callcenter.api;
import com.appsisben.backend.modules.callcenter.application.CallCenterWorkflowService;
import com.appsisben.backend.modules.callcenter.dto.*;
import com.appsisben.backend.security.AppRolePreAuthorize;
import com.appsisben.backend.shared.api.ApiResponse;
import com.appsisben.backend.shared.api.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.List;

/**
 * Controlador REST para el flujo formal de Call Center.
 *
 * <p>Expone endpoints complementarios para registrar intentos de llamada,
 * asignar visitas a encuestadores y actualizar resultados de visitas.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/callcenter")
public class CallCenterWorkflowController {


    private final CallCenterWorkflowService service;

    /**
     * Consulta catálogo de resultados de llamada.
     *
     * @return resultados activos de llamada.
     */
    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_READ)
    @GetMapping("/catalogs/resultados-llamada")
    public ApiResponse<List<CallCenterResultadoLlamadaResponse>> findResultadosLlamada() {
        return ApiResponse.ok(
                service.findResultadosLlamada()
        );
    }

    /**
     * Lista las gestiones de llamada de un caso.
     *
     * @param id identificador del caso maestro.
     * @return gestiones de llamada del caso.
     */
    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_READ)
    @GetMapping("/{id}/llamadas")
    public ApiResponse<List<CallCenterGestionLlamadaResponse>> findLlamadasByCaso(
            @PathVariable Long id
    ) {
        return ApiResponse.ok(
                service.findLlamadasByCaso(id)
        );
    }

    /**
     * Registra una nueva gestión de llamada sobre un caso.
     *
     * @param id identificador del caso maestro.
     * @param request datos de la llamada.
     * @return gestión creada.
     */
    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_WRITE)
    @PostMapping("/{id}/llamadas")
    public ApiResponse<CallCenterGestionLlamadaResponse> registrarLlamada(
            @PathVariable Long id,
            @Valid @RequestBody CallCenterGestionLlamadaRequest request
    ) {
        return ApiResponse.ok(
                "Gestión de llamada registrada correctamente",
                service.registrarLlamada(id, request)
        );
    }

    /**
     * Lista las visitas de un caso.
     *
     * @param id identificador del caso maestro.
     * @return visitas del caso.
     */
    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_READ)
    @GetMapping("/{id}/visitas")
    public ApiResponse<List<CallCenterVisitaResponse>> findVisitasByCaso(
            @PathVariable Long id
    ) {
        return ApiResponse.ok(
                service.findVisitasByCaso(id)
        );
    }

    /**
     * Asigna una visita a un encuestador.
     *
     * @param id identificador del caso maestro.
     * @param request datos de asignación.
     * @return visita creada.
     */
    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_ASSIGN_ENCUESTADOR)
    @PostMapping("/{id}/visitas")
    public ApiResponse<CallCenterVisitaResponse> asignarVisita(
            @PathVariable Long id,
            @Valid @RequestBody CallCenterVisitaAsignacionRequest request
    ) {
        return ApiResponse.ok(
                "Visita asignada correctamente",
                service.asignarVisita(id, request)
        );
    }

    /**
     * Consulta las visitas asignadas al encuestador autenticado.
     *
     * <p>Permite filtrar por texto general, estado de visita, estado formal
     * del caso, condición funcional y rango de fecha programada.</p>
     *
     * @param page número de página.
     * @param size tamaño de página.
     * @param q texto de búsqueda.
     * @param estadoVisita estado operativo de la visita.
     * @param estadoCaso estado formal del caso.
     * @param condicion condición funcional del filtro.
     * @param fechaDesde fecha programada inicial.
     * @param fechaHasta fecha programada final.
     * @return página de visitas.
     */
    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_READ)
    @GetMapping("/visitas/mis-asignaciones")
    public ApiResponse<PageResponse<CallCenterVisitaResponse>> misVisitas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estadoVisita,
            @RequestParam(required = false) String estadoCaso,
            @RequestParam(required = false) String condicion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("fechaProgramada"),
                        Sort.Order.asc("id")
                )
        );

        CallCenterVisitaFilterRequest filter = new CallCenterVisitaFilterRequest(
                q,
                estadoVisita,
                estadoCaso,
                condicion,
                fechaDesde,
                fechaHasta
        );

        return ApiResponse.ok(service.misVisitas(filter, pageable));
    }

    /**
     * Actualiza el resultado de una visita.
     *
     * @param visitaId identificador de la visita.
     * @param request datos del resultado.
     * @return visita actualizada.
     */
    @PreAuthorize(AppRolePreAuthorize.CALLCENTER_VISIT_UPDATE)
    @PatchMapping("/visitas/{visitaId}/resultado")
    public ApiResponse<CallCenterVisitaResponse> actualizarResultadoVisita(
            @PathVariable Long visitaId,
            @Valid @RequestBody CallCenterVisitaResultadoRequest request
    ) {
        return ApiResponse.ok(
                "Resultado de visita actualizado correctamente",
                service.actualizarResultadoVisita(
                        visitaId,
                        request
                )
        );
    }
}
