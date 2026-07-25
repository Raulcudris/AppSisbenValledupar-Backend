package com.appsisben.backend.modules.callcenter.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record CallCenterResponse(
        Long id,
        LocalDateTime marcaTemporal,
        LocalDate fechaLlamada,
        LocalTime horaLlamada,
        String tipoRegistro,
        String origenRegistro,
        Long ventanillaRegistroId,
        String ventanillaNumeroVentanilla,
        LocalDate ventanillaFecha,

        Long funcionarioId,
        String funcionarioUsername,

        String cedulaSolicitante,
        String nombreCompleto,
        String telefono,

        Boolean llamadaConectada,

        Long motivoNoContactoId,
        String motivoNoContactoCodigo,
        String motivoNoContactoNombre,
        String motivoNoContactoTexto,

        Long encuestadorProgramadoId,
        String encuestadorProgramadoNombre,
        LocalDate fechaEncuestaProgramada,

        Boolean solicitoNuevaEncuesta,

        String direccionTexto,
        Long barrioId,
        String barrioNombre,
        Long comunaId,
        String comunaNombre,

        LocalDate fechaAplicacionInformada,
        Boolean disposicionRecibirEncuesta,

        Long motivoNoDisposicionId,
        String motivoNoDisposicionCodigo,
        String motivoNoDisposicionNombre,
        String motivoNoDisposicionTexto,

        Long encuestadorAsignadoId,
        String encuestadorAsignadoNombre,

        Boolean explicoInformanteCalificado,
        Boolean verificado,
        String estadoVisita,
        LocalDate fechaVisitaReal,
        LocalTime horaVisitaReal,
        Boolean encuestaRealizada,
        String motivoNoEncuesta,
        LocalDate fechaReprogramacion,
        String observacionEncuestador,

        String observacion,
        Boolean activo
) {
}
