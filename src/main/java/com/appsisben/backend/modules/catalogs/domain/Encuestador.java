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
@Table(name = "encuestador")
public class Encuestador extends BaseEntity {

@Column(name = "nombre", nullable = false, length = 180)
private String nombre;

@Column(name = "documento", length = 30)
private String documento;

@Column(name = "telefono", length = 40)
private String telefono;

@Column(name = "activo", nullable = false)
private Boolean activo = true;

}
