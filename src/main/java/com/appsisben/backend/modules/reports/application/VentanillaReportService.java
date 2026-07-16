package com.appsisben.backend.modules.reports.application;

import com.appsisben.backend.modules.reports.dto.*;
import com.appsisben.backend.modules.ventanilla.repository.VentanillaRegistroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VentanillaReportService {

    private final VentanillaRegistroRepository repository;

    @Transactional(readOnly = true)
    public VentanillaReportSummaryResponse summary(ReportDateRangeRequest request) {
        LocalDate fechaInicio = fechaInicio(request);
        LocalDate fechaFin = fechaFin(request);

        return new VentanillaReportSummaryResponse(
                safe(repository.countInDateRange(fechaInicio, fechaFin)),
                safe(repository.countByEstadoCodigo("PENDIENTE", fechaInicio, fechaFin)),
                safe(repository.countByEstadoCodigo("REALIZADA", fechaInicio, fechaFin)),
                safe(repository.countByEstadoCodigo("APROBADA", fechaInicio, fechaFin)),
                safe(repository.countByEstadoCodigo("RECHAZADA", fechaInicio, fechaFin)),
                safe(repository.countByEstadoCodigo("CANCELADA", fechaInicio, fechaFin)),
                safe(repository.countByEstadoCodigo("REVISAR", fechaInicio, fechaFin)),
                safe(repository.countByExtranjero(Boolean.TRUE, fechaInicio, fechaFin)),
                safe(repository.countByExtranjero(Boolean.FALSE, fechaInicio, fechaFin))
        );
    }

    @Transactional(readOnly = true)
    public List<ReportGroupResponse> byStatus(ReportDateRangeRequest request) {
        return repository.countByEstado(fechaInicio(request), fechaFin(request));
    }

    @Transactional(readOnly = true)
    public List<ReportGroupResponse> byRequestType(ReportDateRangeRequest request) {
        return repository.countBySolicitud(fechaInicio(request), fechaFin(request));
    }

    @Transactional(readOnly = true)
    public List<ReportGroupResponse> byCategory(ReportDateRangeRequest request) {
        return repository.countByCategoria(fechaInicio(request), fechaFin(request));
    }

    @Transactional(readOnly = true)
    public List<ReportGroupResponse> byUser(ReportDateRangeRequest request) {
        return repository.countByFuncionario(fechaInicio(request), fechaFin(request));
    }

    @Transactional(readOnly = true)
    public List<ReportGroupResponse> byNeighborhood(ReportDateRangeRequest request) {
        return repository.countByBarrio(fechaInicio(request), fechaFin(request));
    }

    @Transactional(readOnly = true)
    public List<ReportGroupResponse> byComuna(ReportDateRangeRequest request) {
        return repository.countByComuna(fechaInicio(request), fechaFin(request));
    }

    @Transactional(readOnly = true)
    public List<VentanillaDailyTrendResponse> solicitudesTrend(ReportDateRangeRequest request) {
        return repository.countDailyTrend(fechaInicio(request), fechaFin(request));
    }

    @Transactional(readOnly = true)
    public List<VentanillaFuncionarioPerformanceResponse> funcionariosPerformance(
            ReportDateRangeRequest request
    ) {
        LocalDate fechaInicio = fechaInicio(request);
        LocalDate fechaFin = fechaFin(request);

        List<ReportGroupResponse> funcionarios = repository.countByFuncionario(fechaInicio, fechaFin);

        long totalGeneral = funcionarios
                .stream()
                .map(ReportGroupResponse::total)
                .map(this::safe)
                .reduce(0L, Long::sum);

        long totalDias = totalDias(fechaInicio, fechaFin);

        return funcionarios
                .stream()
                .map(item -> {
                    Long total = safe(item.total());

                    return new VentanillaFuncionarioPerformanceResponse(
                            item.id(),
                            item.nombre(),
                            total,
                            porcentaje(total, totalGeneral),
                            promedioDiario(total, totalDias)
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VentanillaFuncionarioTrendResponse> funcionariosTrend(
            ReportDateRangeRequest request
    ) {
        return repository.countFuncionarioTrend(fechaInicio(request), fechaFin(request));
    }

    private LocalDate fechaInicio(ReportDateRangeRequest request) {
        return request != null ? request.fechaInicio() : null;
    }

    private LocalDate fechaFin(ReportDateRangeRequest request) {
        return request != null ? request.fechaFin() : null;
    }

    private Long safe(Long value) {
        return value != null ? value : 0L;
    }

    private Double porcentaje(Long total, long totalGeneral) {
        if (totalGeneral <= 0) {
            return 0.0;
        }

        return round((safe(total) * 100.0) / totalGeneral);
    }

    private Double promedioDiario(Long total, long totalDias) {
        if (totalDias <= 0) {
            return 0.0;
        }

        return round(safe(total) / (double) totalDias);
    }

    private long totalDias(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            return 1L;
        }

        return Math.max(1L, ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1L);
    }

    private Double round(Double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @Transactional(readOnly = true)
    public List<VentanillaFrequentCitizenResponse> frequentCitizens(
            ReportDateRangeRequest request,
            Integer limit
    ) {
        int safeLimit = limit == null ? 50 : Math.max(1, Math.min(limit, 200));

        return repository.findFrequentCitizens(
                fechaInicio(request),
                fechaFin(request),
                PageRequest.of(0, safeLimit)
        );
    }
}