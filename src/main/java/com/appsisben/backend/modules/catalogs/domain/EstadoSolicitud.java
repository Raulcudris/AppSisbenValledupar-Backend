package com.appsisben.backend.modules.catalogs.domain;
import com.appsisben.backend.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "estado_solicitud")
public class EstadoSolicitud extends BaseEntity {

@Column(name = "codigo", nullable = false, unique = true, length = 50)
private String codigo;

@Column(name = "nombre", nullable = false, length = 100)
private String nombre;

@Column(name = "descripcion")
private String descripcion;

@Column(name = "activo", nullable = false)
private Boolean activo = true;

}
