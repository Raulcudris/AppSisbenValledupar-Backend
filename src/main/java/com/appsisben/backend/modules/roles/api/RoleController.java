package com.appsisben.backend.modules.roles.api;

import com.appsisben.backend.modules.roles.application.RoleService;
import com.appsisben.backend.modules.roles.dto.RoleResponse;
import com.appsisben.backend.security.AppRolePreAuthorize;
import com.appsisben.backend.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    @PreAuthorize(AppRolePreAuthorize.ADMIN)
    @GetMapping
    public ApiResponse<List<RoleResponse>> findAll() {
        return ApiResponse.ok(roleService.findAll());
    }
}
