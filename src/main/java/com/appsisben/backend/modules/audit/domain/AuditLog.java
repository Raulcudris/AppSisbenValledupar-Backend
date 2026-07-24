package com.appsisben.backend.modules.audit.domain;
import com.appsisben.backend.modules.users.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "auditoria")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private User usuario;

    @Column(name = "tabla_afectada", nullable = false, length = 100)
    private String tablaAfectada;

    @Column(name = "registro_id")
    private Long registroId;

    @Column(name = "accion", nullable = false, length = 50)
    private String accion;

    @Column(name = "fecha_accion", nullable = false)
    private LocalDateTime fechaAccion;

    @Column(name = "ip_origen", length = 80)
    private String ipOrigen;

    @Column(name = "datos_anteriores", columnDefinition = "json")
    private String datosAnteriores;

    @Column(name = "datos_nuevos", columnDefinition = "json")
    private String datosNuevos;
}
