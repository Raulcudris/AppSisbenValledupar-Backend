package com.appsisben.backend.modules.users.api;

import com.appsisben.backend.modules.users.application.UserService;
import com.appsisben.backend.modules.users.dto.*;
import com.appsisben.backend.security.AppRolePreAuthorize;
import com.appsisben.backend.shared.api.ApiResponse;
import com.appsisben.backend.shared.api.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PreAuthorize(AppRolePreAuthorize.ADMIN)
    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
        return ApiResponse.ok(userService.findAll(pageable));
    }

    @PreAuthorize(AppRolePreAuthorize.ADMIN)
    @GetMapping("/roles")
    public ApiResponse<List<RoleOptionResponse>> findRoles() {
        return ApiResponse.ok(userService.findRoles());
    }

    @PreAuthorize(AppRolePreAuthorize.ADMIN)
    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok("Usuario creado correctamente", userService.create(request));
    }

    @PreAuthorize(AppRolePreAuthorize.ADMIN)
    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ApiResponse.ok("Usuario actualizado correctamente", userService.update(id, request));
    }

    @PreAuthorize(AppRolePreAuthorize.ADMIN)
    @PatchMapping("/{id}/activar")
    public ApiResponse<UserResponse> activate(@PathVariable Long id) {
        return ApiResponse.ok("Usuario activado correctamente", userService.changeStatus(id, true));
    }

    @PreAuthorize(AppRolePreAuthorize.ADMIN)
    @PatchMapping("/{id}/inactivar")
    public ApiResponse<UserResponse> inactivate(@PathVariable Long id) {
        return ApiResponse.ok("Usuario inactivado correctamente", userService.changeStatus(id, false));
    }

    @PreAuthorize(AppRolePreAuthorize.ADMIN)
    @PatchMapping("/{id}/reset-password")
    public ApiResponse<String> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        userService.resetPassword(id, request);
        return ApiResponse.ok("Contraseña restablecida correctamente", "OK");
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/change-password")
    public ApiResponse<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changeOwnPassword(request);
        return ApiResponse.ok("Contraseña actualizada correctamente", "OK");
    }

    @PostMapping("/public/change-password")
    public ApiResponse<String> changePasswordFromLogin(
            @Valid @RequestBody PublicChangePasswordRequest request
    ) {
        userService.changePasswordFromLogin(request);
        return ApiResponse.ok("Contraseña actualizada correctamente", "OK");
    }
}