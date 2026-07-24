package com.appsisben.backend.modules.callcenter.domain;

import com.appsisben.backend.modules.catalogs.domain.Encuestador;
import com.appsisben.backend.modules.territory.domain.Barrio;
import com.appsisben.backend.modules.users.domain.User;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private User funcionario;

    @Column(name = "cedula_solicitante", nullable = false, length = 30)
    private String cedulaSolicitante;

    @Column(name = "nombre_completo", nullable = false, length = 250)
    private String nombreCompleto;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "llamada_conectada", nullable = false)
    private Boolean llamadaConectada = false;

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

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_id")
    private User creadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por_id")
    private User actualizadoPor;
}
