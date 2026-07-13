package com.appsisben.backend.modules.auth.api;
import com.appsisben.backend.modules.auth.application.AuthService;
import com.appsisben.backend.modules.auth.dto.AuthUserResponse;
import com.appsisben.backend.modules.auth.dto.ChangePasswordRequest;
import com.appsisben.backend.modules.auth.dto.LoginRequest;
import com.appsisben.backend.modules.auth.dto.LoginResponse;
import com.appsisben.backend.security.AppRolePreAuthorize;
import com.appsisben.backend.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Autenticación correcta", authService.login(request));
    }

    @PreAuthorize(AppRolePreAuthorize.AUTHENTICATED)
    @GetMapping("/me")
    public ApiResponse<AuthUserResponse> me() {
        return ApiResponse.ok(authService.me());
    }

    @PreAuthorize(AppRolePreAuthorize.AUTHENTICATED)
    @PutMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.ok("Contraseña actualizada correctamente", null);
    }
}
