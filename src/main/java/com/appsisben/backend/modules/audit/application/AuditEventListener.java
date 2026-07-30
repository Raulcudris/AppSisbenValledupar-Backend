package com.appsisben.backend.modules.audit.application;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditWriterService auditWriterService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handleAuditEvent(AuditEvent event) {
        try {
            auditWriterService.write(event);
        } catch (Exception exception) {
            /*
             * Un fallo de auditoría no debe afectar una operación
             * de negocio que ya fue confirmada.
             */
            log.error(
                    "No fue posible guardar auditoría. "
                            + "accion={}, tabla={}, registroId={}",
                    event.accion(),
                    event.tablaAfectada(),
                    event.registroId(),
                    exception
            );
        }
    }
}