package com.appsisben.backend.modules.ventanilla.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record VentanillaFilterRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaInicio,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaFin,

        String numeroVentanilla,
        String cedulaUsuario,
        String nombreUsuario,
        Long funcionarioId,
        Long categoriaId,
        Long solicitudId,
        Long estadoSolicitudId,
        Long barrioId,
        Long comunaId,
        Boolean extranjero,

        /**
         * Solo aplica para ADMIN.
         * Si viene true, permite consultar activos e inactivos.
         */
        Boolean incluirInactivos,

        /**
         * Solo aplica para ADMIN cuando incluirInactivos = true.
         * true = activos, false = inactivos, null = todos.
         */
        Boolean activo
) {
}