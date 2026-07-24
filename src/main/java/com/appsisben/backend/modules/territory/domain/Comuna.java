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

    @Column(name = "estrato", columnDefinition = "TINYINT UNSIGNED")
    private Byte estrato;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
