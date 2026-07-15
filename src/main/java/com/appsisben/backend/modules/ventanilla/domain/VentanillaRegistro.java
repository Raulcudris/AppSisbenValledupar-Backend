package com.appsisben.backend.modules.ventanilla.domain;

import com.appsisben.backend.modules.catalogs.domain.Categoria;
import com.appsisben.backend.modules.catalogs.domain.EstadoSolicitud;
import com.appsisben.backend.modules.catalogs.domain.Solicitud;
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

@Getter
@Setter
@Entity
@Table(name = "ventanilla_registro")
public class VentanillaRegistro extends BaseEntity {

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "numero_ventanilla", nullable = false, length = 80)
    private String numeroVentanilla;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private User funcionario;

    @Column(name = "cedula_usuario", nullable = false, length = 30)
    private String cedulaUsuario;

    @Column(name = "nombre_usuario", nullable = false, length = 250)
    private String nombreUsuario;

    @Column(name = "telefono", length = 40)
    private String telefono;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "direccion", length = 350)
    private String direccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barrio_id", nullable = false)
    private Barrio barrio;

    @Column(name = "extranjero", nullable = false)
    private Boolean extranjero = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_solicitud_id", nullable = false)
    private EstadoSolicitud estadoSolicitud;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "motivo_repeticion", length = 500)
    private String motivoRepeticion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "editado_por_id")
    private User editadoPor;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}