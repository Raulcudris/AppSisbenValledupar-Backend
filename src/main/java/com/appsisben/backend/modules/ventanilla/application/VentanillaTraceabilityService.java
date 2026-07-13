package com.appsisben.backend.modules.ventanilla.application;

import com.appsisben.backend.modules.ventanilla.domain.VentanillaRegistro;
import com.appsisben.backend.modules.ventanilla.dto.VentanillaCiudadanoSolicitudSummaryResponse;
import com.appsisben.backend.modules.ventanilla.dto.VentanillaCiudadanoTraceabilityResponse;
import com.appsisben.backend.modules.ventanilla.dto.VentanillaCiudadanoVisitResponse;
import com.appsisben.backend.modules.ventanilla.repository.VentanillaRegistroRepository;
import com.appsisben.backend.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VentanillaTraceabilityService {

    private static final long FREQUENT_VISIT_WINDOW_DAYS = 30;
    private static final long FREQUENT_VISIT_THRESHOLD = 3;

    private final VentanillaRegistroRepository repository;

    @Transactional(readOnly = true)
    public VentanillaCiudadanoTraceabilityResponse findByCedula(String cedulaUsuario) {
        if (!hasText(cedulaUsuario)) {
            throw new BusinessException("La cédula del ciudadano es obligatoria");
        }

        String cedula = cedulaUsuario.trim();

        List<VentanillaRegistro> visits = repository.findActiveTraceabilityByCedula(cedula);

        if (visits.isEmpty()) {
            return new VentanillaCiudadanoTraceabilityResponse(
                    cedula,
                    null,
                    null,
                    null,
                    null,
                    null,
                    0L,
                    0L,
                    false,
                    List.of(),
                    List.of()
            );
        }

        LocalDate today = LocalDate.now();

        LocalDate primeraVisita = visits.stream()
                .map(VentanillaRegistro::getFecha)
                .min(LocalDate::compareTo)
                .orElse(null);

        LocalDate ultimaVisita = visits.stream()
                .map(VentanillaRegistro::getFecha)
                .max(LocalDate::compareTo)
                .orElse(null);

        Long diasDesdeUltimaVisita = ultimaVisita == null
                ? null
                : Math.max(0L, ChronoUnit.DAYS.between(ultimaVisita, today));

        LocalDate windowStart = today.minusDays(FREQUENT_VISIT_WINDOW_DAYS - 1);

        long visitasUltimos30Dias = visits.stream()
                .filter(visit -> visit.getFecha() != null)
                .filter(visit -> !visit.getFecha().isBefore(windowStart))
                .filter(visit -> !visit.getFecha().isAfter(today))
                .count();

        boolean ciudadanoFrecuente = visitasUltimos30Dias >= FREQUENT_VISIT_THRESHOLD;

        VentanillaRegistro latestVisit = visits.get(0);

        List<VentanillaCiudadanoSolicitudSummaryResponse> solicitudes = buildSolicitudSummary(visits);

        List<VentanillaCiudadanoVisitResponse> historial = visits.stream()
                .map(this::toVisitResponse)
                .toList();

        return new VentanillaCiudadanoTraceabilityResponse(
                cedula,
                latestVisit.getNombreUsuario(),
                latestVisit.getTelefono(),
                primeraVisita,
                ultimaVisita,
                diasDesdeUltimaVisita,
                (long) visits.size(),
                visitasUltimos30Dias,
                ciudadanoFrecuente,
                solicitudes,
                historial
        );
    }

    private List<VentanillaCiudadanoSolicitudSummaryResponse> buildSolicitudSummary(
            List<VentanillaRegistro> visits
    ) {
        Map<Long, SolicitudAccumulator> grouped = new LinkedHashMap<>();

        for (VentanillaRegistro visit : visits) {
            if (visit.getSolicitud() == null) {
                continue;
            }

            Long solicitudId = visit.getSolicitud().getId();
            String solicitudNombre = visit.getSolicitud().getNombre();

            grouped.computeIfAbsent(
                    solicitudId,
                    id -> new SolicitudAccumulator(solicitudId, solicitudNombre)
            ).addVisit(visit.getFecha());
        }

        return grouped.values()
                .stream()
                .map(accumulator -> new VentanillaCiudadanoSolicitudSummaryResponse(
                        accumulator.solicitudId,
                        accumulator.solicitudNombre,
                        accumulator.cantidad,
                        accumulator.ultimaFecha
                ))
                .sorted(
                        Comparator.comparingLong(VentanillaCiudadanoSolicitudSummaryResponse::cantidad)
                                .reversed()
                                .thenComparing(
                                        VentanillaCiudadanoSolicitudSummaryResponse::ultimaFecha,
                                        Comparator.nullsLast(Comparator.reverseOrder())
                                )
                )
                .toList();
    }

    private VentanillaCiudadanoVisitResponse toVisitResponse(VentanillaRegistro entity) {
        return new VentanillaCiudadanoVisitResponse(
                entity.getId(),
                entity.getFecha(),
                entity.getNumeroVentanilla(),
                entity.getSolicitud() != null ? entity.getSolicitud().getId() : null,
                entity.getSolicitud() != null ? entity.getSolicitud().getNombre() : null,
                entity.getEstadoSolicitud() != null ? entity.getEstadoSolicitud().getId() : null,
                entity.getEstadoSolicitud() != null ? entity.getEstadoSolicitud().getNombre() : null,
                entity.getCategoria() != null ? entity.getCategoria().getId() : null,
                entity.getCategoria() != null ? entity.getCategoria().getNombre() : null,
                entity.getBarrio() != null ? entity.getBarrio().getNombre() : null,
                entity.getBarrio() != null && entity.getBarrio().getComuna() != null
                        ? entity.getBarrio().getComuna().getNombre()
                        : null,
                entity.getFuncionario() != null ? entity.getFuncionario().getId() : null,
                entity.getFuncionario() != null ? entity.getFuncionario().getUsername() : null,
                entity.getObservacion()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static class SolicitudAccumulator {

        private final Long solicitudId;
        private final String solicitudNombre;
        private long cantidad;
        private LocalDate ultimaFecha;

        private SolicitudAccumulator(Long solicitudId, String solicitudNombre) {
            this.solicitudId = solicitudId;
            this.solicitudNombre = solicitudNombre;
        }

        private void addVisit(LocalDate fecha) {
            cantidad++;

            if (fecha == null) {
                return;
            }

            if (ultimaFecha == null || fecha.isAfter(ultimaFecha)) {
                ultimaFecha = fecha;
            }
        }
    }
}