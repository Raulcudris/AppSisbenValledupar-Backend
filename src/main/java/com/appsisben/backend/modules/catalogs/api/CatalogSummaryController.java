package com.appsisben.backend.modules.catalogs.api;
import com.appsisben.backend.modules.catalogs.repository.CategoriaRepository;
import com.appsisben.backend.modules.catalogs.repository.EncuestadorRepository;
import com.appsisben.backend.modules.catalogs.repository.EstadoSolicitudRepository;
import com.appsisben.backend.modules.catalogs.repository.SolicitudRepository;
import com.appsisben.backend.modules.catalogs.repository.TipoDmcRepository;
import com.appsisben.backend.security.AppRolePreAuthorize;
import com.appsisben.backend.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/catalogs")
public class CatalogSummaryController {

    private final CategoriaRepository categoriaRepository;
    private final SolicitudRepository solicitudRepository;
    private final EstadoSolicitudRepository estadoSolicitudRepository;
    private final EncuestadorRepository encuestadorRepository;
    private final TipoDmcRepository tipoDmcRepository;

    @PreAuthorize(AppRolePreAuthorize.CATALOG_READ)
    @GetMapping("/summary")
    public ApiResponse<Map<String, Long>> summary() {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("categorias", categoriaRepository.count());
        result.put("solicitudes", solicitudRepository.count());
        result.put("estadosSolicitud", estadoSolicitudRepository.count());
        result.put("encuestadores", encuestadorRepository.count());
        result.put("tiposDmc", tipoDmcRepository.count());
        return ApiResponse.ok(result);
    }
}
