package com.appsisben.backend.modules.callcenter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Solicitud para registrar una gestión de llamada dentro de un caso
 * de Call Center.
 *
 * <p>Cada gestión conserva los datos propios del intento, como fecha,
 * hora, resultado, motivos y observación.</p>
 *
 * <p>Los datos de confirmación se utilizan para actualizar la última
 * información conocida del ciudadano dentro del caso maestro.</p>
 *
 * @param fechaLlamada fecha de la llamada. Si llega nula, el servicio
 *                     utiliza la fecha actual.
 * @param horaLlamada hora de la llamada. Si llega nula, el servicio
 *                    utiliza la hora actual.
 * @param llamadaConectada indica si la llamada fue contestada.
 * @param resultadoLlamada código técnico del resultado de la llamada.
 * @param motivoNoContactoId motivo de no contacto, cuando aplique.
 * @param motivoNoDisposicionId motivo de no disposición, cuando aplique.
 * @param fechaReprogramacionLlamada fecha propuesta para realizar
 *                                   un nuevo intento de llamada.
 * @param horaReprogramacionLlamada hora propuesta para realizar
 *                                  un nuevo intento de llamada.
 * @param solicitoNuevaEncuesta indica si el ciudadano confirmó que
 *                              realizó la solicitud de nueva encuesta.
 * @param direccionTexto última dirección confirmada durante la llamada.
 * @param fechaAplicacionInformada fecha de aplicación informada
 *                                 al ciudadano.
 * @param disposicionRecibirEncuesta indica si el ciudadano confirmó
 *                                    disposición para recibir la encuesta.
 * @param explicoInformanteCalificado indica si el funcionario explicó
 *                                     el concepto de informante calificado
 *                                     y los soportes requeridos.
 * @param observacion observación operativa del intento de llamada.
 */
public record CallCenterGestionLlamadaRequest(

        LocalDate fechaLlamada,

        LocalTime horaLlamada,

        @NotNull(
                message = "Debe indicar si la llamada fue conectada"
        )
        Boolean llamadaConectada,

        @NotBlank(
                message = "El resultado de la llamada es obligatorio"
        )
        String resultadoLlamada,

        Long motivoNoContactoId,

        Long motivoNoDisposicionId,

        LocalDate fechaReprogramacionLlamada,

        LocalTime horaReprogramacionLlamada,

        Boolean solicitoNuevaEncuesta,

        String direccionTexto,

        LocalDate fechaAplicacionInformada,

        Boolean disposicionRecibirEncuesta,

        Boolean explicoInformanteCalificado,

        String observacion

) {
}