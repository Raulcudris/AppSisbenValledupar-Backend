package com.appsisben.backend.modules.dmc.domain;

import com.appsisben.backend.modules.catalogs.domain.Encuestador;
import com.appsisben.backend.modules.catalogs.domain.TipoDmc;
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
@Table(name = "dmc_registro")
public class DmcRegistro extends BaseEntity {

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private User funcionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_dmc_id", nullable = false)
    private TipoDmc tipoDmc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encuestador_id", nullable = false)
    private Encuestador encuestador;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barrio_id", nullable = false)
    private Barrio barrio;
}
