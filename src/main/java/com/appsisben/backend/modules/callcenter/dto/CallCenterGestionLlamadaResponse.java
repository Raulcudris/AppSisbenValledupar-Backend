package com.appsisben.backend.modules.callcenter.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Respuesta con la información de una gestión de llamada registrada
 * en el módulo Call Center.
 *
 * @param id identificador de la gestión.
 * @param callCenterRegistroId identificador del caso maestro.
 * @param funcionarioCallcenterId identificador del funcionario Call Center.
 * @param funcionarioCallcenterUsername nombre de usuario del funcionario.
 * @param funcionarioCallcenterNombre nombre completo del funcionario.
 * @param fechaLlamada fecha de la llamada.
 * @param horaLlamada hora de la llamada.
 * @param intentoNumero número de intento dentro del caso.
 * @param llamadaConectada indica si la llamada fue contestada.
 * @param resultadoLlamada código del resultado.
 * @param motivoNoContactoId identificador del motivo de no contacto.
 * @param motivoNoContactoNombre nombre del motivo de no contacto.
 * @param motivoNoDisposicionId identificador del motivo de no disposición.
 * @param motivoNoDisposicionNombre nombre del motivo de no disposición.
 * @param fechaReprogramacionLlamada fecha de reprogramación de llamada.
 * @param horaReprogramacionLlamada hora de reprogramación de llamada.
 * @param observacion observación registrada.
 * @param activo indica si la gestión está activa.
 * @param creadoEn fecha de creación.
 */
public record CallCenterGestionLlamadaResponse(
        Long id,
        Long callCenterRegistroId,
        Long funcionarioCallcenterId,
        String funcionarioCallcenterUsername,
        String funcionarioCallcenterNombre,
        LocalDate fechaLlamada,
        LocalTime horaLlamada,
        Integer intentoNumero,
        Boolean llamadaConectada,
        String resultadoLlamada,
        Long motivoNoContactoId,
        String motivoNoContactoNombre,
        Long motivoNoDisposicionId,
        String motivoNoDisposicionNombre,
        LocalDate fechaReprogramacionLlamada,
        LocalTime horaReprogramacionLlamada,
        String observacion,
        Boolean activo,
        LocalDateTime creadoEn
) {}
