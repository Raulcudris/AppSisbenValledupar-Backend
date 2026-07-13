package com.appsisben.backend.modules.users.application;

import com.appsisben.backend.modules.roles.domain.Role;
import com.appsisben.backend.modules.roles.repository.RoleRepository;
import com.appsisben.backend.modules.users.domain.User;
import com.appsisben.backend.modules.users.dto.*;
import com.appsisben.backend.modules.users.repository.UserRepository;
import com.appsisben.backend.shared.api.PageResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int USERNAME_MAX_LENGTH = 80;
    private static final int DOCUMENTO_MAX_LENGTH = 30;
    private static final int NOMBRES_MAX_LENGTH = 120;
    private static final int APELLIDOS_MAX_LENGTH = 120;
    private static final int EMAIL_MAX_LENGTH = 150;
    private static final int TELEFONO_MAX_LENGTH = 40;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> findAll(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);

        List<UserResponse> content = page.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public List<RoleOptionResponse> findRoles() {
        return roleRepository.findAll(Sort.by("nombre").ascending())
                .stream()
                .map(role -> new RoleOptionResponse(
                        role.getId(),
                        role.getCodigo(),
                        role.getNombre()
                ))
                .toList();
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        validateCreateRequest(request);

        Role role = findRoleByCodigo(request.rolCodigo());

        User user = new User();
        user.setUsername(clean(request.username()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDocumento(cleanNullable(request.documento()));
        user.setNombres(clean(request.nombres()));
        user.setApellidos(cleanNullable(request.apellidos()));
        user.setEmail(cleanNullable(request.email()));
        user.setTelefono(cleanNullable(request.telefono()));
        user.setActivo(request.activo() == null || request.activo());
        user.setRole(role);

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return toResponse(savedUser);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Error de integridad creando usuario: {}", ex.getMessage());

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No fue posible crear el usuario. Verifica que el usuario o documento no estén repetidos."
            );
        } catch (DataAccessException ex) {
            log.error("Error de base de datos creando usuario", ex);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No fue posible crear el usuario. Verifica que los datos sean válidos."
            );
        }
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = findById(id);

        validateUpdateRequest(id, request);

        Role role = findRoleByCodigo(request.rolCodigo());

        user.setUsername(clean(request.username()));
        user.setDocumento(cleanNullable(request.documento()));
        user.setNombres(clean(request.nombres()));
        user.setApellidos(cleanNullable(request.apellidos()));
        user.setEmail(cleanNullable(request.email()));
        user.setTelefono(cleanNullable(request.telefono()));
        user.setActivo(request.activo() == null || request.activo());
        user.setRole(role);

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return toResponse(savedUser);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Error de integridad actualizando usuario con id {}: {}", id, ex.getMessage());

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No fue posible actualizar el usuario. Verifica que el usuario o documento no estén repetidos."
            );
        } catch (DataAccessException ex) {
            log.error("Error de base de datos actualizando usuario con id {}", id, ex);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No fue posible actualizar el usuario. Verifica que los datos sean válidos."
            );
        }
    }

    @Transactional
    public UserResponse changeStatus(Long id, boolean active) {
        User user = findById(id);
        user.setActivo(active);

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return toResponse(savedUser);
        } catch (DataAccessException ex) {
            log.error("Error actualizando estado del usuario con id {}", id, ex);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No fue posible actualizar el estado del usuario."
            );
        }
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        User user = findById(id);

        validatePasswordConfirmation(
                request.newPassword(),
                request.confirmPassword()
        );

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));

        try {
            userRepository.saveAndFlush(user);
        } catch (DataAccessException ex) {
            log.error("Error restableciendo contraseña del usuario con id {}", id, ex);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No fue posible restablecer la contraseña del usuario."
            );
        }
    }

    @Transactional
    public void changeOwnPassword(ChangePasswordRequest request) {
        User user = getAuthenticatedUser();

        if (!hasText(request.currentPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña actual es obligatoria"
            );
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña actual no es correcta"
            );
        }

        validatePasswordConfirmation(
                request.newPassword(),
                request.confirmPassword()
        );

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La nueva contraseña debe ser diferente a la contraseña actual"
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.saveAndFlush(user);
    }

    @Transactional
    public void changePasswordFromLogin(PublicChangePasswordRequest request) {
        if (!hasText(request.username())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El usuario es obligatorio"
            );
        }

        User user = userRepository.findByUsernameIgnoreCase(request.username().trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Usuario o contraseña actual incorrectos"
                ));

        if (Boolean.FALSE.equals(user.getActivo())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El usuario se encuentra inactivo"
            );
        }

        if (!hasText(request.currentPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña actual es obligatoria"
            );
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Usuario o contraseña actual incorrectos"
            );
        }

        validatePasswordConfirmation(
                request.newPassword(),
                request.confirmPassword()
        );

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La nueva contraseña debe ser diferente a la contraseña actual"
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.saveAndFlush(user);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "No se encontró el usuario autenticado"
            );
        }

        String username = authentication.getName();

        if (!hasText(username) || "anonymousUser".equalsIgnoreCase(username)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "No se encontró el nombre del usuario autenticado"
            );
        }

        return userRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "El usuario autenticado no existe"
                ));
    }

    private User findById(Long id) {
        if (id == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador del usuario es obligatorio"
            );
        }

        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                ));
    }

    private Role findRoleByCodigo(String rolCodigo) {
        if (!hasText(rolCodigo)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El rol es obligatorio"
            );
        }

        return roleRepository.findByCodigoIgnoreCase(rolCodigo.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El rol seleccionado no existe"
                ));
    }

    private void validateCreateRequest(UserCreateRequest request) {
        if (!hasText(request.username())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario es obligatorio");
        }

        if (!hasText(request.nombres())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los nombres son obligatorios");
        }

        if (!hasText(request.rolCodigo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol es obligatorio");
        }

        validateTextLength(request.username(), USERNAME_MAX_LENGTH, "El usuario no puede superar 80 caracteres");
        validateTextLength(request.documento(), DOCUMENTO_MAX_LENGTH, "El documento no puede superar 30 caracteres");
        validateTextLength(request.nombres(), NOMBRES_MAX_LENGTH, "Los nombres no pueden superar 120 caracteres");
        validateTextLength(request.apellidos(), APELLIDOS_MAX_LENGTH, "Los apellidos no pueden superar 120 caracteres");
        validateTextLength(request.email(), EMAIL_MAX_LENGTH, "El correo no puede superar 150 caracteres");
        validateTextLength(request.telefono(), TELEFONO_MAX_LENGTH, "El teléfono no puede superar 40 caracteres");

        if (userRepository.existsByUsernameIgnoreCase(request.username().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de usuario ya existe");
        }

        if (hasText(request.documento())
                && userRepository.existsByDocumentoIgnoreCase(request.documento().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El documento ya está registrado");
        }

        validatePasswordConfirmation(request.password(), request.confirmPassword());
    }

    private void validateUpdateRequest(Long id, UserUpdateRequest request) {
        if (id == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador del usuario es obligatorio"
            );
        }

        if (!hasText(request.username())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El usuario es obligatorio"
            );
        }

        if (!hasText(request.nombres())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los nombres son obligatorios"
            );
        }

        if (!hasText(request.rolCodigo())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El rol es obligatorio"
            );
        }

        validateTextLength(request.username(), USERNAME_MAX_LENGTH, "El usuario no puede superar 80 caracteres");
        validateTextLength(request.documento(), DOCUMENTO_MAX_LENGTH, "El documento no puede superar 30 caracteres");
        validateTextLength(request.nombres(), NOMBRES_MAX_LENGTH, "Los nombres no pueden superar 120 caracteres");
        validateTextLength(request.apellidos(), APELLIDOS_MAX_LENGTH, "Los apellidos no pueden superar 120 caracteres");
        validateTextLength(request.email(), EMAIL_MAX_LENGTH, "El correo no puede superar 150 caracteres");
        validateTextLength(request.telefono(), TELEFONO_MAX_LENGTH, "El teléfono no puede superar 40 caracteres");

        String username = request.username().trim();

        if (userRepository.existsByUsernameIgnoreCaseAndIdNot(username, id)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre de usuario ya existe"
            );
        }

        if (hasText(request.documento())) {
            String documento = request.documento().trim();

            if (userRepository.existsByDocumentoIgnoreCaseAndIdNot(documento, id)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El documento ya está registrado"
                );
            }
        }
    }

    private void validatePasswordConfirmation(String password, String confirmPassword) {
        if (!hasText(password)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria");
        }

        if (password.length() < PASSWORD_MIN_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña debe tener mínimo " + PASSWORD_MIN_LENGTH + " caracteres"
            );
        }

        if (!password.equals(confirmPassword)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña y la confirmación no coinciden"
            );
        }
    }

    private void validateTextLength(String value, int maxLength, String message) {
        if (hasText(value) && value.trim().length() > maxLength) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    message
            );
        }
    }

    private UserResponse toResponse(User user) {
        Role role = user.getRole();

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDocumento(),
                user.getNombres(),
                user.getApellidos(),
                user.getEmail(),
                user.getTelefono(),
                user.getActivo(),
                role != null ? role.getCodigo() : null,
                role != null ? role.getNombre() : null
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String cleanNullable(String value) {
        if (!hasText(value)) {
            return null;
        }

        return value.trim();
    }
}