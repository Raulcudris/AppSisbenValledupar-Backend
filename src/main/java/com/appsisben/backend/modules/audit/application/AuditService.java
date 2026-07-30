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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Logger log =
            LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(
            AuditFilterRequest filter,
            Pageable pageable
    ) {
        Page<AuditLog> page = auditLogRepository.findAll(
                AuditLogSpecification.byFilter(filter),
                pageable
        );

        List<AuditLogResponse> content = page.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public AuditLogResponse findById(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Registro de auditoría no encontrado"
                        )
                );

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
            log(
                    action,
                    tablaAfectada,
                    registroId,
                    datosAnteriores,
                    datosNuevos
            );
        } catch (Exception exception) {
            /*
             * La publicación de la auditoría no debe bloquear
             * la operación principal.
             */
            log.warn(
                    "No fue posible publicar evento de auditoría. "
                            + "accion={}, tabla={}, registroId={}",
                    action,
                    tablaAfectada,
                    registroId,
                    exception
            );
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
            logWithUser(
                    user,
                    action,
                    tablaAfectada,
                    registroId,
                    datosAnteriores,
                    datosNuevos
            );
        } catch (Exception exception) {
            /*
             * La publicación de la auditoría no debe bloquear
             * la operación principal.
             */
            log.warn(
                    "No fue posible publicar evento de auditoría con usuario. "
                            + "accion={}, tabla={}, registroId={}",
                    action,
                    tablaAfectada,
                    registroId,
                    exception
            );
        }
    }

    public void log(
            AuditAction action,
            String tablaAfectada,
            Long registroId,
            Object datosAnteriores,
            Object datosNuevos
    ) {
        User user = currentUser();

        publishEvent(
                user.getId(),
                action,
                tablaAfectada,
                registroId,
                datosAnteriores,
                datosNuevos
        );
    }

    public void logWithUser(
            User user,
            AuditAction action,
            String tablaAfectada,
            Long registroId,
            Object datosAnteriores,
            Object datosNuevos
    ) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(
                    "El usuario de la auditoría es obligatorio"
            );
        }

        publishEvent(
                user.getId(),
                action,
                tablaAfectada,
                registroId,
                datosAnteriores,
                datosNuevos
        );
    }

    private void publishEvent(
            Long usuarioId,
            AuditAction action,
            String tablaAfectada,
            Long registroId,
            Object datosAnteriores,
            Object datosNuevos
    ) {
        validateAuditData(
                action,
                tablaAfectada
        );

        /*
         * Los objetos se convierten a JSON antes de terminar
         * la transacción principal.
         *
         * Esto evita intentar serializar entidades o proxies JPA
         * después de que la sesión haya sido cerrada.
         */
        String previousDataJson = toJson(datosAnteriores);
        String newDataJson = toJson(datosNuevos);

        AuditEvent event = new AuditEvent(
                usuarioId,
                action.name(),
                tablaAfectada.trim(),
                registroId,
                LocalDateTime.now(),
                resolveIpAddress(),
                previousDataJson,
                newDataJson
        );

        eventPublisher.publishEvent(event);
    }

    private void validateAuditData(
            AuditAction action,
            String tablaAfectada
    ) {
        if (action == null) {
            throw new BusinessException(
                    "La acción de auditoría es obligatoria"
            );
        }

        if (tablaAfectada == null || tablaAfectada.isBlank()) {
            throw new BusinessException(
                    "La tabla afectada es obligatoria para registrar auditoría"
            );
        }
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (
                authentication == null
                        || authentication.getName() == null
                        || authentication.getName().isBlank()
                        || "anonymousUser".equals(authentication.getName())
        ) {
            throw new BusinessException(
                    "No hay usuario autenticado para registrar auditoría"
            );
        }

        return userRepository
                .findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Usuario autenticado no encontrado"
                        )
                );
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            try {
                return objectMapper.writeValueAsString(
                        Map.of(
                                "value",
                                String.valueOf(value)
                        )
                );
            } catch (JsonProcessingException fallbackException) {
                log.warn(
                        "No fue posible convertir los datos de auditoría a JSON",
                        fallbackException
                );

                return null;
            }
        }
    }

    private String resolveIpAddress() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes)
                        RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();

        String forwardedFor =
                request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return limitLength(
                    forwardedFor.split(",")[0].trim(),
                    45
            );
        }

        String realIp =
                request.getHeader("X-Real-IP");

        if (realIp != null && !realIp.isBlank()) {
            return limitLength(
                    realIp.trim(),
                    45
            );
        }

        return limitLength(
                request.getRemoteAddr(),
                45
        );
    }

    private String limitLength(
            String value,
            int maxLength
    ) {
        if (value == null) {
            return null;
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getUsuario() != null
                        ? auditLog.getUsuario().getId()
                        : null,
                auditLog.getUsuario() != null
                        ? auditLog.getUsuario().getUsername()
                        : null,
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