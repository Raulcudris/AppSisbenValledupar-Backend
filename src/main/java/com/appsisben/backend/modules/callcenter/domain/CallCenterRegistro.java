package com.appsisben.backend.modules.callcenter.domain;

import com.appsisben.backend.modules.catalogs.domain.Encuestador;
import com.appsisben.backend.modules.territory.domain.Barrio;
import com.appsisben.backend.modules.users.domain.User;
import com.appsisben.backend.modules.ventanilla.domain.VentanillaRegistro;
import com.appsisben.backend.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "callcenter_registro")
public class CallCenterRegistro extends BaseEntity {

    @Column(name = "marca_temporal")
    private LocalDateTime marcaTemporal;

    @Column(name = "fecha_llamada", nullable = false)
    private LocalDate fechaLlamada;

    @Column(name = "hora_llamada")
    private LocalTime horaLlamada;

    @Column(name = "tipo_registro", nullable = false, length = 40)
    private String tipoRegistro = "LLAMADA";

    @Column(name = "origen_registro", nullable = false, length = 40)
    private String origenRegistro = "MANUAL";

    /**
     * Tipo de solicitud que origina el caso de Call Center.
     *
     * Ejemplos funcionales:
     * NUEVA_ENCUESTA, INCLUSION, VERIFICACION.
     */
    @Column(name = "tipo_solicitud_callcenter", length = 60)
    private String tipoSolicitudCallcenter;

    /**
     * Estado central del caso maestro de Call Center.
     *
     * Este campo permite conocer en qué punto del flujo se encuentra el caso:
     * pendiente de enrutamiento, asignado, en llamada, pendiente de visita,
     * visita realizada, cerrado o cancelado.
     */
    @Column(name = "estado_caso", nullable = false, length = 60)
    private String estadoCaso = "PENDIENTE_ENRUTAMIENTO";

    /**
     * Fecha y hora de cierre del caso, cuando aplique.
     */
    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    /**
     * Motivo o descripción del cierre del caso.
     */
    @Column(name = "motivo_cierre", length = 500)
    private String motivoCierre;

    /**
     * Usuario que realizó el cierre del caso.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_cierre_id")
    private User usuarioCierre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ventanilla_registro_id")
    private VentanillaRegistro ventanillaRegistro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private User funcionario;

    @Column(name = "cedula_solicitante", nullable = false, length = 30)
    private String cedulaSolicitante;

    @Column(name = "nombre_completo", nullable = false, length = 250)
    private String nombreCompleto;

    @Column(name = "telefono", length = 30)
    private String telefono;

    /**
     * Indica si la llamada fue conectada.
     *
     * Puede ser null cuando el caso apenas se crea desde Ventanilla
     * o manualmente y todavía no existe una gestión telefónica registrada.
     */
    @Column(name = "llamada_conectada")
    private Boolean llamadaConectada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motivo_no_contacto_id")
    private CallCenterMotivoNoContacto motivoNoContacto;

    @Column(name = "motivo_no_contacto_texto", length = 500)
    private String motivoNoContactoTexto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encuestador_programado_id")
    private Encuestador encuestadorProgramado;

    @Column(name = "fecha_encuesta_programada")
    private LocalDate fechaEncuestaProgramada;

    @Column(name = "solicito_nueva_encuesta")
    private Boolean solicitoNuevaEncuesta;

    @Column(name = "direccion_texto", length = 500)
    private String direccionTexto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barrio_id")
    private Barrio barrio;

    @Column(name = "fecha_aplicacion_informada")
    private LocalDate fechaAplicacionInformada;

    @Column(name = "disposicion_recibir_encuesta")
    private Boolean disposicionRecibirEncuesta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motivo_no_disposicion_id")
    private CallCenterMotivoNoDisposicion motivoNoDisposicion;

    @Column(name = "motivo_no_disposicion_texto", length = 500)
    private String motivoNoDisposicionTexto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encuestador_asignado_id")
    private Encuestador encuestadorAsignado;

    @Column(name = "explico_informante_calificado")
    private Boolean explicoInformanteCalificado;

    @Column(name = "verificado")
    private Boolean verificado;

    @Column(name = "estado_visita", nullable = false, length = 40)
    private String estadoVisita = "PENDIENTE";

    @Column(name = "fecha_visita_real")
    private LocalDate fechaVisitaReal;

    @Column(name = "hora_visita_real")
    private LocalTime horaVisitaReal;

    @Column(name = "encuesta_realizada")
    private Boolean encuestaRealizada;

    @Column(name = "motivo_no_encuesta", length = 500)
    private String motivoNoEncuesta;

    @Column(name = "fecha_reprogramacion")
    private LocalDate fechaReprogramacion;

    @Column(name = "observacion_encuestador", columnDefinition = "TEXT")
    private String observacionEncuestador;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_callcenter_asignado_id")
    private User funcionarioCallcenterAsignado;

    @Column(name = "fecha_asignacion_callcenter")
    private LocalDateTime fechaAsignacionCallcenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_asigna_callcenter_id")
    private User usuarioAsignaCallcenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_id")
    private User creadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por_id")
    private User actualizadoPor;
}
