package com.appsisben.backend.modules.callcenter.dto;

import java.time.LocalDate;

public record CallCenterFilterRequest(
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Long funcionarioId,
        String cedulaSolicitante,
        String nombreCompleto,
        String telefono,
        Boolean llamadaConectada,
        Long motivoNoContactoId,
        Long encuestadorProgramadoId,
        Long encuestadorAsignadoId,
        LocalDate fechaEncuestaInicio,
        LocalDate fechaEncuestaFin,
        Boolean solicitoNuevaEncuesta,
        Long barrioId,
        Long comunaId,
        Long funcionarioCallcenterAsignadoId,
        Boolean disposicionRecibirEncuesta,
        Boolean explicoInformanteCalificado,
        Boolean activo,
        String q,
        String tipoRegistro,
        String origenRegistro,
        Long ventanillaRegistroId,
        Boolean verificado,
        String estadoVisita,
        Boolean encuestaRealizada
) {
}
