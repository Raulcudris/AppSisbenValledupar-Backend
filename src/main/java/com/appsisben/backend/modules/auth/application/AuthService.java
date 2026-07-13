package com.appsisben.backend.modules.auth.application;

import com.appsisben.backend.modules.audit.application.AuditService;
import com.appsisben.backend.modules.audit.domain.AuditAction;
import com.appsisben.backend.modules.auth.dto.AuthUserResponse;
import com.appsisben.backend.modules.auth.dto.ChangePasswordRequest;
import com.appsisben.backend.modules.auth.dto.LoginRequest;
import com.appsisben.backend.modules.auth.dto.LoginResponse;
import com.appsisben.backend.modules.users.domain.User;
import com.appsisben.backend.modules.users.repository.UserRepository;
import com.appsisben.backend.security.CustomUserDetails;
import com.appsisben.backend.security.JwtService;
import com.appsisben.backend.shared.exception.BusinessException;
import com.appsisben.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        user.setUltimoAcceso(LocalDateTime.now());

        auditService.safeLogWithUser(
                user,
                AuditAction.LOGIN,
                "usuario",
                user.getId(),
                null,
                loginSnapshot(user)
        );

        String token = jwtService.generateToken(userDetails);
        return new LoginResponse(token, "Bearer", toAuthUserResponse(user));
    }

    @Transactional(readOnly = true)
    public AuthUserResponse me() {
        User user = getCurrentUser();
        return toAuthUserResponse(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("La contraseña actual no es correcta");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));

        auditService.safeLogWithUser(
                user,
                AuditAction.CHANGE_PASSWORD,
                "usuario",
                user.getId(),
                null,
                Map.of("username", user.getUsername(), "accion", "Cambio de contraseña")
        );
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException("No hay usuario autenticado");
        }

        return userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    private AuthUserResponse toAuthUserResponse(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getUsername(),
                user.getNombres(),
                user.getApellidos(),
                user.getRole().getCodigo(),
                user.getRole().getNombre()
        );
    }

    private Map<String, Object> loginSnapshot(User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("rol", user.getRole() != null ? user.getRole().getCodigo() : null);
        data.put("ultimoAcceso", user.getUltimoAcceso());
        return data;
    }
}
