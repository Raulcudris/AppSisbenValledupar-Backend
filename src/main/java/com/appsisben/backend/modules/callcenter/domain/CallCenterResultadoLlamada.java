package com.appsisben.backend.modules.callcenter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidad de catálogo para los resultados posibles de una gestión de llamada
 * del módulo Call Center.
 *
 * <p>Esta entidad permite parametrizar el resultado operativo de una llamada
 * y sugerir el estado que debe tomar el caso maestro en {@link CallCenterRegistro}.
 * Ejemplos: NO_CONTESTA, CONTACTADO_ACEPTA_VISITA, CONTACTADO_NO_ACEPTA_VISITA.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "callcenter_resultado_llamada")
public class CallCenterResultadoLlamada {

    /**
     * Identificador primario autoincremental del resultado de llamada.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código técnico único del resultado de llamada.
     */
    @Column(name = "codigo", nullable = false, unique = true, length = 60)
    private String codigo;

    /**
     * Nombre visible del resultado de llamada.
     */
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    /**
     * Descripción funcional del resultado de llamada.
     */
    @Column(name = "descripcion", length = 500)
    private String descripcion;

    /**
     * Estado sugerido para el caso maestro después de registrar este resultado.
     */
    @Column(name = "estado_caso_sugerido", length = 60)
    private String estadoCasoSugerido;

    /**
     * Indica si el resultado se encuentra activo para selección.
     */
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /**
     * Fecha y hora de creación del registro.
     */
    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    /**
     * Fecha y hora de última actualización del registro.
     */
    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;
}
