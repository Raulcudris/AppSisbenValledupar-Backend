package com.appsisben.backend.modules.ventanilla.api;
import com.appsisben.backend.modules.ventanilla.application.VentanillaService;
import com.appsisben.backend.modules.ventanilla.dto.*;
import com.appsisben.backend.security.AppRolePreAuthorize;
import com.appsisben.backend.shared.api.ApiResponse;
import com.appsisben.backend.shared.api.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ventanilla")
public class VentanillaController {

    private final VentanillaService service;

    @PreAuthorize(AppRolePreAuthorize.VENTANILLA_READ)
    @GetMapping
    public ApiResponse<PageResponse<VentanillaResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
        return ApiResponse.ok(service.findAll(pageable));
    }

    @PreAuthorize(AppRolePreAuthorize.VENTANILLA_READ)
    @GetMapping("/search")
    public ApiResponse<PageResponse<VentanillaResponse>> search(
            @ModelAttribute VentanillaFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
        return ApiResponse.ok(service.search(filter, pageable));
    }

    @PreAuthorize(AppRolePreAuthorize.VENTANILLA_READ)
    @GetMapping("/historial/usuario/export/pdf")
    public ResponseEntity<byte[]> exportUserHistoryPdf(
            @RequestParam String cedulaUsuario
    ) {
        byte[] file = service.exportUserHistoryPdf(cedulaUsuario);
        String filename = "historial-usuario-" + cedulaUsuario.trim() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }
    @PreAuthorize(AppRolePreAuthorize.VENTANILLA_READ)
    @GetMapping("/historial/usuarios")
    public ApiResponse<PageResponse<VentanillaUserHistorySummaryResponse>> findUserHistorySummaries(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.ok(service.findUserHistorySummaries(search, pageable));
    }

    @PreAuthorize(AppRolePreAuthorize.VENTANILLA_READ)
    @GetMapping("/historial/usuario")
    public ApiResponse<VentanillaUserHistoryResponse> findUserHistory(
            @RequestParam String cedulaUsuario
    ) {
        return ApiResponse.ok(service.findUserHistory(cedulaUsuario));
    }

    @PreAuthorize(AppRolePreAuthorize.VENTANILLA_READ)
    @GetMapping("/historial/usuario/export")
    public ResponseEntity<byte[]> exportUserHistory(
            @RequestParam String cedulaUsuario
    ) {
        byte[] file = service.exportUserHistory(cedulaUsuario);
        String filename = "historial-usuario-" + cedulaUsuario.trim() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(file);
    }
    @PreAuthorize(AppRolePreAuthorize.VENTANILLA_WRITE)
    @GetMapping("/validacion-previa")
    public ApiResponse<VentanillaDailyValidationResponse> validateBeforeSave(
            @RequestParam(required = false) Long currentId,
            @RequestParam LocalDate fecha,
            @RequestParam String cedulaUsuario,
            @RequestParam Long solicitudId
    ) {
        return ApiResponse.ok(service.validateBeforeSave(currentId, fecha, cedulaUsuario, solicitudId));
    }

    @PreAuthorize(AppRolePreAuthorize.VENTANILLA_READ)
    @GetMapping("/historial/ciudadano")
    public ApiResponse<VentanillaCitizenHistoryResponse> findCitizenHistory(
            @RequestParam String cedulaUsuario
    ) {
        return ApiResponse.ok(service.findCitizenHistory(cedulaUsuario));
    }

    @PreAuthorize(AppRolePreAuthorize.VENTANILLA_READ)
    @GetMapping("/{id}")
    public ApiResponse<VentanillaResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PreAuthorize(AppRolePreAuthorize.VENTANILLA_WRITE)
    @PostMapping
    public ApiResponse<VentanillaResponse> create(@Valid @RequestBody VentanillaRequest request) {
        return ApiResponse.ok("Registro de ventanilla creado correctamente", service.create(request));
    }

    @PreAuthorize(AppRolePreAuthorize.VENTANILLA_WRITE)
    @PutMapping("/{id}")
    public ApiResponse<VentanillaResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody VentanillaRequest request
    ) {
        return ApiResponse.ok("Registro de ventanilla actualizado correctamente", service.update(id, request));
    }

    @PreAuthorize(AppRolePreAuthorize.VENTANILLA_WRITE)
    @PatchMapping("/{id}/inactivar")
    public ApiResponse<Void> inactivate(@PathVariable Long id) {
        service.inactivate(id);

        return ApiResponse.ok("Registro de ventanilla retirado correctamente", null);
    }

    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('ROLE_ADMIN')")
    @PatchMapping("/{id}/activar")
    public ApiResponse<Void> activate(@PathVariable Long id) {
        service.activate(id);

        return ApiResponse.ok("Registro de ventanilla activado correctamente", null);
    }

    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('ROLE_ADMIN')")
    @PatchMapping("/{id}/estado")
    public ApiResponse<Void> changeActiveStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request
    ) {
        service.changeActiveStatus(id, request.get("activo"));

        return ApiResponse.ok("Estado del registro actualizado correctamente", null);
    }

    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);

        return ApiResponse.ok("Registro de ventanilla eliminado definitivamente", null);
    }
}