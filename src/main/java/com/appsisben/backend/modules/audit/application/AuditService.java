package com.appsisben.backend.modules.audit.application;
import com.appsisben.backend.modules.audit.domain.AuditAction;
import com.appsisben.backend.modules.audit.domain.AuditLog;
import com.appsisben.backend.modules.audit.dto.AuditFilterRequest;
import com.appsisben.backend.modules.audit.dto.AuditLogResponse;
import com.appsisben.backend.modules.audit.repository.AuditLogRepository;
import com.appsisben.backend.modules.audit.repository.AuditLogSpecification;
import com.appsisben.backend.modules.users.domain.User;
import com.appsisben.backend.modules.users.repository.UserRepository;
import com.appsisben.backend.shared.api.PageResponse;
import com.appsisben.backend.shared.exception.BusinessException;
import com.appsisben.backend.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(AuditFilterRequest filter, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findAll(AuditLogSpecification.byFilter(filter), pageable);
        List<AuditLogResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public AuditLogResponse findById(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de auditoría no encontrado"));
        return toResponse(auditLog);
    }

    public void safeLog(
            AuditAction action,
            String tablaAfectada,
            Long registroId,
            Object datosAnteriores,
            Object datosNuevos
    ) {
        try {
            log(action, tablaAfectada, registroId, datosAnteriores, datosNuevos);
        } catch (Exception ignored) {
            // La auditoría no debe bloquear la operación principal.
        }
    }

    public void safeLogWithUser(
            User user,
            AuditAction action,
            String tablaAfectada,
            Long registroId,
            Object datosAnteriores,
            Object datosNuevos
    ) {
        try {
            logWithUser(user, action, tablaAfectada, registroId, datosAnteriores, datosNuevos);
        } catch (Exception ignored) {
            // La auditoría no debe bloquear la operación principal.
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
            AuditAction action,
            String tablaAfectada,
            Long registroId,
            Object datosAnteriores,
            Object datosNuevos
    ) {
        User user = currentUser();
        save(user, action, tablaAfectada, registroId, datosAnteriores, datosNuevos);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logWithUser(
            User user,
            AuditAction action,
            String tablaAfectada,
            Long registroId,
            Object datosAnteriores,
            Object datosNuevos
    ) {
        save(user, action, tablaAfectada, registroId, datosAnteriores, datosNuevos);
    }

    private void save(
            User user,
            AuditAction action,
            String tablaAfectada,
            Long registroId,
            Object datosAnteriores,
            Object datosNuevos
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUsuario(user);
        auditLog.setTablaAfectada(tablaAfectada);
        auditLog.setRegistroId(registroId);
        auditLog.setAccion(action.name());
        auditLog.setFechaAccion(LocalDateTime.now());
        auditLog.setIpOrigen(resolveIpAddress());
        auditLog.setDatosAnteriores(toJson(datosAnteriores));
        auditLog.setDatosNuevos(toJson(datosNuevos));

        auditLogRepository.save(auditLog);
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException("No hay usuario autenticado para registrar auditoría");
        }

        return userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            try {
                return objectMapper.writeValueAsString(Map.of("value", String.valueOf(value)));
            } catch (JsonProcessingException ignored) {
                return null;
            }
        }
    }

    private String resolveIpAddress() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");

        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getUsuario() != null ? auditLog.getUsuario().getId() : null,
                auditLog.getUsuario() != null ? auditLog.getUsuario().getUsername() : null,
                auditLog.getTablaAfectada(),
                auditLog.getRegistroId(),
                auditLog.getAccion(),
                auditLog.getFechaAccion(),
                auditLog.getIpOrigen(),
                auditLog.getDatosAnteriores(),
                auditLog.getDatosNuevos()
        );
    }
}
