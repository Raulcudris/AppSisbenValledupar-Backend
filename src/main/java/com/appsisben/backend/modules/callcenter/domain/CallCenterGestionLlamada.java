package com.appsisben.backend.modules.callcenter.domain;

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
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entidad que representa una gestión o intento de llamada realizado dentro
 * de un caso de Call Center.
 *
 * <p>Un caso maestro puede tener múltiples gestiones de llamada. Esta separación
 * permite conservar la trazabilidad completa: fecha, hora, funcionario, resultado,
 * motivo y observación de cada intento.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "callcenter_gestion_llamada")
public class CallCenterGestionLlamada {

    /**
     * Identificador primario autoincremental de la gestión de llamada.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Caso maestro de Call Center al que pertenece la gestión.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "callcenter_registro_id", nullable = false)
    private CallCenterRegistro callCenterRegistro;

    /**
     * Funcionario Call Center que realizó la llamada.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_callcenter_id", nullable = false)
    private User funcionarioCallcenter;

    /**
     * Fecha en la que se realizó la llamada.
     */
    @Column(name = "fecha_llamada", nullable = false)
    private LocalDate fechaLlamada;

    /**
     * Hora en la que se realizó la llamada.
     */
    @Column(name = "hora_llamada")
    private LocalTime horaLlamada;

    /**
     * Número consecutivo del intento dentro del caso.
     */
    @Column(name = "intento_numero", nullable = false)
    private Integer intentoNumero = 1;

    /**
     * Indica si la llamada fue contestada por el ciudadano.
     */
    @Column(name = "llamada_conectada", nullable = false)
    private Boolean llamadaConectada = false;

    /**
     * Código técnico del resultado de la llamada.
     */
    @Column(name = "resultado_llamada", nullable = false, length = 60)
    private String resultadoLlamada;

    /**
     * Motivo de no contacto cuando la llamada no fue contestada o no fue efectiva.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motivo_no_contacto_id")
    private CallCenterMotivoNoContacto motivoNoContacto;

    /**
     * Motivo de no disposición cuando el ciudadano no acepta continuar con visita.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motivo_no_disposicion_id")
    private CallCenterMotivoNoDisposicion motivoNoDisposicion;

    /**
     * Fecha sugerida para volver a llamar al ciudadano.
     */
    @Column(name = "fecha_reprogramacion_llamada")
    private LocalDate fechaReprogramacionLlamada;

    /**
     * Hora sugerida para volver a llamar al ciudadano.
     */
    @Column(name = "hora_reprogramacion_llamada")
    private LocalTime horaReprogramacionLlamada;

    /**
     * Observación registrada por el funcionario Call Center.
     */
    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    /**
     * Indica si la gestión se encuentra activa.
     */
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /**
     * Usuario que creó la gestión.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_id")
    private User creadoPor;

    /**
     * Usuario que actualizó la gestión por última vez.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por_id")
    private User actualizadoPor;

    /**
     * Fecha y hora de creación del registro.
     */
    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    /**
     * Fecha y hora de última actualización del registro.
     */
    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;
}
