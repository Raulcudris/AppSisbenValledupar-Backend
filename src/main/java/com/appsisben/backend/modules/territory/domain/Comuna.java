package com.appsisben.backend.modules.territory.domain;

import com.appsisben.backend.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "comuna")
public class Comuna extends BaseEntity {

    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "nombre", nullable = false, unique = true, length = 120)
    private String nombre;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
