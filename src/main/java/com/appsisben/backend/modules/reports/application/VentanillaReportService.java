package com.appsisben.backend.modules.reports.application;

import com.appsisben.backend.modules.reports.dto.ReportDateRangeRequest;
import com.appsisben.backend.modules.reports.dto.ReportGroupResponse;
import com.appsisben.backend.modules.reports.dto.VentanillaReportSummaryResponse;
import com.appsisben.backend.modules.ventanilla.repository.VentanillaRegistroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    private LocalDate fechaInicio(ReportDateRangeRequest request) {
        return request != null ? request.fechaInicio() : null;
    }

    private LocalDate fechaFin(ReportDateRangeRequest request) {
        return request != null ? request.fechaFin() : null;
    }

    private Long safe(Long value) {
        return value != null ? value : 0L;
    }
}
