package com.appsisben.backend.modules.reports.application;

import com.appsisben.backend.modules.dmc.repository.DmcRegistroRepository;
import com.appsisben.backend.modules.reports.dto.DmcReportSummaryResponse;
import com.appsisben.backend.modules.reports.dto.ReportDateRangeRequest;
import com.appsisben.backend.modules.reports.dto.ReportGroupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DmcReportService {

    private final DmcRegistroRepository repository;

    @Transactional(readOnly = true)
    public DmcReportSummaryResponse summary(ReportDateRangeRequest request) {
        LocalDate fechaInicio = fechaInicio(request);
        LocalDate fechaFin = fechaFin(request);

        return new DmcReportSummaryResponse(
                safe(repository.countInDateRange(fechaInicio, fechaFin)),
                safe(repository.sumCantidadInDateRange(fechaInicio, fechaFin)),
                safe(repository.sumCantidadByTipoCodigo("CARGADAS", fechaInicio, fechaFin)),
                safe(repository.sumCantidadByTipoCodigo("DESCARGADAS", fechaInicio, fechaFin)),
                safe(repository.sumCantidadByTipoCodigo("RECHAZADAS", fechaInicio, fechaFin))
        );
    }

    @Transactional(readOnly = true)
    public List<ReportGroupResponse> byType(ReportDateRangeRequest request) {
        return repository.sumCantidadByTipoDmc(fechaInicio(request), fechaFin(request));
    }

    @Transactional(readOnly = true)
    public List<ReportGroupResponse> bySurveyor(ReportDateRangeRequest request) {
        return repository.sumCantidadByEncuestador(fechaInicio(request), fechaFin(request));
    }

    @Transactional(readOnly = true)
    public List<ReportGroupResponse> byUser(ReportDateRangeRequest request) {
        return repository.sumCantidadByFuncionario(fechaInicio(request), fechaFin(request));
    }

    @Transactional(readOnly = true)
    public List<ReportGroupResponse> byNeighborhood(ReportDateRangeRequest request) {
        return repository.sumCantidadByBarrio(fechaInicio(request), fechaFin(request));
    }

    @Transactional(readOnly = true)
    public List<ReportGroupResponse> byComuna(ReportDateRangeRequest request) {
        return repository.sumCantidadByComuna(fechaInicio(request), fechaFin(request));
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
