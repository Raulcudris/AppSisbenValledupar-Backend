package com.appsisben.backend.modules.reports.api;

import com.appsisben.backend.modules.catalogs.repository.CategoriaRepository;
import com.appsisben.backend.modules.catalogs.repository.EncuestadorRepository;
import com.appsisben.backend.modules.catalogs.repository.EstadoSolicitudRepository;
import com.appsisben.backend.modules.catalogs.repository.SolicitudRepository;
import com.appsisben.backend.modules.catalogs.repository.TipoDmcRepository;
import com.appsisben.backend.modules.directory.repository.DirectoryContactRepository;
import com.appsisben.backend.modules.dmc.repository.DmcRegistroRepository;
import com.appsisben.backend.modules.roles.repository.RoleRepository;
import com.appsisben.backend.modules.territory.repository.BarrioRepository;
import com.appsisben.backend.modules.territory.repository.ComunaRepository;
import com.appsisben.backend.modules.users.repository.UserRepository;
import com.appsisben.backend.modules.ventanilla.repository.VentanillaRegistroRepository;
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
@RequestMapping("/api/reports/migration")
public class MigrationValidationController {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ComunaRepository comunaRepository;
    private final BarrioRepository barrioRepository;
    private final CategoriaRepository categoriaRepository;
    private final SolicitudRepository solicitudRepository;
    private final EstadoSolicitudRepository estadoSolicitudRepository;
    private final EncuestadorRepository encuestadorRepository;
    private final TipoDmcRepository tipoDmcRepository;
    private final DirectoryContactRepository directoryContactRepository;
    private final VentanillaRegistroRepository ventanillaRegistroRepository;
    private final DmcRegistroRepository dmcRegistroRepository;

    @PreAuthorize(AppRolePreAuthorize.ADMIN_OR_SUPERVISOR)
    @GetMapping("/counts")
    public ApiResponse<Map<String, Long>> counts() {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("roles", roleRepository.count());
        result.put("usuarios", userRepository.count());
        result.put("comunas", comunaRepository.count());
        result.put("barrios", barrioRepository.count());
        result.put("categorias", categoriaRepository.count());
        result.put("solicitudes", solicitudRepository.count());
        result.put("estadosSolicitud", estadoSolicitudRepository.count());
        result.put("encuestadores", encuestadorRepository.count());
        result.put("tiposDmc", tipoDmcRepository.count());
        result.put("directorio", directoryContactRepository.count());
        result.put("ventanilla", ventanillaRegistroRepository.count());
        result.put("dmc", dmcRegistroRepository.count());
        return ApiResponse.ok(result);
    }
}
