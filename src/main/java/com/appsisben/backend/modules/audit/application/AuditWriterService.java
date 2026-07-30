package com.appsisben.backend.modules.audit.application;

import com.appsisben.backend.modules.audit.domain.AuditLog;
import com.appsisben.backend.modules.audit.repository.AuditLogRepository;
import com.appsisben.backend.modules.users.domain.User;
import com.appsisben.backend.modules.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditWriterService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(AuditEvent event) {
        AuditLog auditLog = new AuditLog();

        auditLog.setUsuario(findUser(event.usuarioId()));
        auditLog.setTablaAfectada(event.tablaAfectada());
        auditLog.setRegistroId(event.registroId());
        auditLog.setAccion(event.accion());
        auditLog.setFechaAccion(event.fechaAccion());
        auditLog.setIpOrigen(event.ipOrigen());
        auditLog.setDatosAnteriores(event.datosAnteriores());
        auditLog.setDatosNuevos(event.datosNuevos());

        auditLogRepository.save(auditLog);
    }

    private User findUser(Long usuarioId) {
        if (usuarioId == null) {
            return null;
        }

        /*
         * La auditoría debe poder guardarse incluso si el usuario
         * fue eliminado o dejó de estar disponible.
         *
         * La columna usuario_id permite null según el esquema actual.
         */
        return userRepository.findById(usuarioId)
                .orElse(null);
    }
}