package com.appsisben.backend.modules.territory.api;

import com.appsisben.backend.modules.territory.repository.BarrioRepository;
import com.appsisben.backend.modules.territory.repository.ComunaRepository;
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
@RequestMapping("/api/territory")
public class TerritoryController {

    private final ComunaRepository comunaRepository;
    private final BarrioRepository barrioRepository;

    @PreAuthorize(AppRolePreAuthorize.TERRITORY_READ)
    @GetMapping("/summary")
    public ApiResponse<Map<String, Long>> summary() {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("comunas", comunaRepository.count());
        result.put("barrios", barrioRepository.count());
        return ApiResponse.ok(result);
    }
}
