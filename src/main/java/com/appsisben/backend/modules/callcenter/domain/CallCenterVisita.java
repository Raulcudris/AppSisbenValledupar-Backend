package com.appsisben.backend.modules.callcenter.domain;

import com.appsisben.backend.modules.catalogs.domain.Encuestador;
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
 * Entidad que representa la asignación y resultado de una visita realizada
 * por un encuestador dentro de un caso de Call Center.
 *
 * <p>Esta entidad evita perder trazabilidad del trabajo de campo, ya que
 * conserva quién fue asignado, cuándo fue programada la visita y cuál fue
 * el resultado reportado por el encuestador.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "callcenter_visita")
public class CallCenterVisita {

    /**
     * Identificador primario autoincremental de la visita.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Caso maestro de Call Center al que pertenece la visita.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "callcenter_registro_id", nullable = false)
    private CallCenterRegistro callCenterRegistro;

    /**
     * Encuestador asignado para realizar la visita en campo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encuestador_id", nullable = false)
    private Encuestador encuestador;

    /**
     * Usuario que asignó la visita al encuestador.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_asigna_id")
    private User usuarioAsigna;

    /**
     * Fecha y hora en la que se asignó la visita.
     */
    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion = LocalDateTime.now();

    /**
     * Fecha programada para la visita.
     */
    @Column(name = "fecha_programada")
    private LocalDate fechaProgramada;

    /**
     * Hora programada para la visita.
     */
    @Column(name = "hora_programada")
    private LocalTime horaProgramada;

    /**
     * Estado operativo de la visita.
     */
    @Column(name = "estado_visita", nullable = false, length = 40)
    private String estadoVisita = "PENDIENTE";

    /**
     * Fecha real en la que se realizó o intentó realizar la visita.
     */
    @Column(name = "fecha_visita_real")
    private LocalDate fechaVisitaReal;

    /**
     * Hora real en la que se realizó o intentó realizar la visita.
     */
    @Column(name = "hora_visita_real")
    private LocalTime horaVisitaReal;

    /**
     * Indica si la encuesta fue realizada durante la visita.
     */
    @Column(name = "encuesta_realizada")
    private Boolean encuestaRealizada;

    /**
     * Motivo por el cual no fue posible realizar la encuesta.
     */
    @Column(name = "motivo_no_encuesta", length = 500)
    private String motivoNoEncuesta;

    /**
     * Fecha de reprogramación cuando la visita no pudo finalizarse.
     */
    @Column(name = "fecha_reprogramacion")
    private LocalDate fechaReprogramacion;

    /**
     * Observación registrada por el encuestador.
     */
    @Column(name = "observacion_encuestador", columnDefinition = "TEXT")
    private String observacionEncuestador;

    /**
     * Indica si la visita se encuentra activa.
     */
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /**
     * Usuario que creó la visita.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_id")
    private User creadoPor;

    /**
     * Usuario que actualizó la visita por última vez.
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
