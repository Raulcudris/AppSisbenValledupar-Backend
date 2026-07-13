package com.appsisben.backend.modules.directory.domain;

import com.appsisben.backend.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "directorio_contacto")
public class DirectoryContact extends BaseEntity {

    @Column(name = "nombre", nullable = false, length = 180)
    private String nombre;

    @Column(name = "telefono", nullable = false, length = 40)
    private String telefono;

    @Column(name = "perfil", nullable = false, length = 120)
    private String perfil;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "entidad", length = 180)
    private String entidad;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
