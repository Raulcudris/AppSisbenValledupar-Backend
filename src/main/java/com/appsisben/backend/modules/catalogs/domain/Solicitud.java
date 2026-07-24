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
@Table(name = "solicitud")
public class Solicitud extends BaseEntity {

@Column(name = "nombre", nullable = false, length = 180)
private String nombre;

@Column(name = "descripcion")
private String descripcion;

@Column(name = "activo", nullable = false)
private Boolean activo = true;

}
