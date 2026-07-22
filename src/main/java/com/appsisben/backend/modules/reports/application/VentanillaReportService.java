package com.appsisben.backend.modules.reports.application;

import com.appsisben.backend.modules.dmc.repository.DmcRegistroRepository;
import com.appsisben.backend.modules.reports.dto.*;
import com.appsisben.backend.modules.ventanilla.repository.VentanillaRegistroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VentanillaReportService {

    private final VentanillaRegistroRepository repository;
    private final DmcRegistroRepository dmcRepository;

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

    @Transactional(readOnly = true)
    public List<VentanillaEmployeeProductivityResponse> employeeProductivity(
            ReportDateRangeRequest request,
            String grouping
    ) {
        LocalDate fechaInicio = fechaInicio(request);
        LocalDate fechaFin = fechaFin(request);
        ProductivityGrouping productivityGrouping = parseProductivityGrouping(grouping);

        List<VentanillaEmployeeDailyCount> dailyRows =
                repository.countEmployeeDailyProductivity(fechaInicio, fechaFin);

        if (dailyRows.isEmpty()) {
            return List.of();
        }

        Map<ProductivityKey, Long> totalsByEmployeeAndPeriod = new HashMap<>();
        Map<String, Long> totalsByPeriod = new HashMap<>();

        for (VentanillaEmployeeDailyCount row : dailyRows) {
            PeriodInfo periodInfo = buildPeriodInfo(
                    row.fecha(),
                    productivityGrouping,
                    fechaInicio,
                    fechaFin
            );

            ProductivityKey key = new ProductivityKey(
                    periodInfo.periodo(),
                    periodInfo.fechaInicio(),
                    periodInfo.fechaFin(),
                    row.funcionarioId(),
                    row.funcionarioUsername()
            );

            Long total = safe(row.total());

            totalsByEmployeeAndPeriod.merge(key, total, Long::sum);
            totalsByPeriod.merge(periodInfo.periodo(), total, Long::sum);
        }

        List<VentanillaEmployeeProductivityResponse> response = new ArrayList<>();

        totalsByEmployeeAndPeriod.forEach((key, total) -> {
            long totalPeriodo = totalsByPeriod.getOrDefault(key.periodo(), 0L);

            response.add(new VentanillaEmployeeProductivityResponse(
                    key.periodo(),
                    key.fechaInicioPeriodo(),
                    key.fechaFinPeriodo(),
                    key.funcionarioId(),
                    key.funcionarioUsername(),
                    total,
                    porcentaje(total, totalPeriodo),
                    promedioDiario(total, totalDias(key.fechaInicioPeriodo(), key.fechaFinPeriodo()))
            ));
        });

        response.sort(
                Comparator
                        .comparing(VentanillaEmployeeProductivityResponse::fechaInicioPeriodo)
                        .thenComparing(
                                Comparator
                                        .comparing(VentanillaEmployeeProductivityResponse::totalAtenciones)
                                        .reversed()
                        )
                        .thenComparing(
                                VentanillaEmployeeProductivityResponse::funcionarioUsername,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                        )
        );

        return response;
    }

    @Transactional(readOnly = true)
    public List<VentanillaEmployeeDetailedPerformanceResponse> employeeDetailedPerformance(
            ReportDateRangeRequest request
    ) {
        LocalDate fechaInicio = fechaInicio(request);
        LocalDate fechaFin = fechaFin(request);

        List<VentanillaEmployeeDetailedPerformanceRow> employeeRows =
                repository.countEmployeeDetailedPerformance(fechaInicio, fechaFin);

        if (employeeRows.isEmpty()) {
            return List.of();
        }

        List<VentanillaEmployeeDailyDetailResponse> dailyRows =
                repository.countEmployeeDetailedPerformanceDaily(fechaInicio, fechaFin);

        Map<Long, List<VentanillaEmployeeDailyDetailResponse>> dailyByEmployeeId = new HashMap<>();
        Map<String, List<VentanillaEmployeeDailyDetailResponse>> dailyByEmployeeName = new HashMap<>();

        for (VentanillaEmployeeDailyDetailResponse row : dailyRows) {
            if (row.funcionarioId() != null) {
                dailyByEmployeeId
                        .computeIfAbsent(row.funcionarioId(), key -> new ArrayList<>())
                        .add(row);
            } else {
                dailyByEmployeeName
                        .computeIfAbsent(normalizeEmployeeName(row.funcionarioUsername()), key -> new ArrayList<>())
                        .add(row);
            }
        }

        long totalGeneral = employeeRows
                .stream()
                .map(VentanillaEmployeeDetailedPerformanceRow::totalAtenciones)
                .map(this::safe)
                .reduce(0L, Long::sum);

        long totalDias = totalDiasHabiles(fechaInicio, fechaFin);

        return employeeRows
                .stream()
                .map(row -> {
                    Long totalAtenciones = safe(row.totalAtenciones());

                    List<VentanillaEmployeeDailyDetailResponse> employeeDailyRows =
                            row.funcionarioId() != null
                                    ? dailyByEmployeeId.getOrDefault(row.funcionarioId(), List.of())
                                    : dailyByEmployeeName.getOrDefault(
                                    normalizeEmployeeName(row.funcionarioUsername()),
                                    List.of()
                            );

                    return new VentanillaEmployeeDetailedPerformanceResponse(
                            row.funcionarioId(),
                            row.funcionarioUsername(),
                            totalAtenciones,
                            porcentaje(totalAtenciones, totalGeneral),
                            promedioDiario(totalAtenciones, totalDias),
                            safe(row.pendientes()),
                            safe(row.realizadas()),
                            safe(row.aprobadas()),
                            safe(row.rechazadas()),
                            safe(row.canceladas()),
                            safe(row.revisar()),
                            safe(row.nacionales()),
                            safe(row.extranjeros()),
                            employeeDailyRows
                                    .stream()
                                    .sorted(
                                            Comparator
                                                    .comparing(VentanillaEmployeeDailyDetailResponse::fecha)
                                                    .thenComparing(
                                                            VentanillaEmployeeDailyDetailResponse::funcionarioUsername,
                                                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                                                    )
                                    )
                                    .toList()
                    );
                })
                .sorted(
                        Comparator
                                .comparing(VentanillaEmployeeDetailedPerformanceResponse::totalAtenciones)
                                .reversed()
                                .thenComparing(
                                        VentanillaEmployeeDetailedPerformanceResponse::funcionarioUsername,
                                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                                )
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DmcEncuestadorPerformanceResponse> dmcEncuestadoresDesempeno(
            ReportDateRangeRequest request
    ) {
        return dmcRepository.countDmcEncuestadorPerformance(fechaInicio(request), fechaFin(request))
                .stream()
                .map(row -> {
                    Long cargadas = safe(row.cargadas());
                    Long efectivas = safe(row.efectivas());
                    Long noEfectivas = Math.max(0L, cargadas - efectivas);
                    Double cumplimiento = porcentaje(efectivas, cargadas);
                    Long total = cargadas + efectivas;

                    return new DmcEncuestadorPerformanceResponse(
                            row.encuestadorId(),
                            row.encuestadorNombre(),
                            cargadas,
                            efectivas,
                            noEfectivas,
                            cumplimiento,
                            resolveDmcPerformanceLabel(cumplimiento),
                            total
                    );
                })
                .sorted(
                        Comparator
                                .comparing(DmcEncuestadorPerformanceResponse::total)
                                .reversed()
                                .thenComparing(
                                        DmcEncuestadorPerformanceResponse::encuestadorNombre,
                                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                                )
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DmcComunaTotalResponse> dmcComunasTotales(
            ReportDateRangeRequest request
    ) {
        return dmcRepository.countDmcComunaTotals(fechaInicio(request), fechaFin(request))
                .stream()
                .map(row -> {
                    Long cargadas = safe(row.cargadas());
                    Long descargadas = safe(row.descargadas());

                    return new DmcComunaTotalResponse(
                            row.comunaId(),
                            row.comunaCodigo(),
                            row.comunaNombre(),
                            cargadas,
                            descargadas,
                            cargadas + descargadas
                    );
                })
                .sorted(
                        Comparator
                                .comparing(DmcComunaTotalResponse::total)
                                .reversed()
                                .thenComparing(
                                        DmcComunaTotalResponse::comunaNombre,
                                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                                )
                )
                .toList();
    }

    private String resolveDmcPerformanceLabel(Double cumplimiento) {
        if (cumplimiento == null) {
            return "Bajo";
        }

        if (cumplimiento >= 90.0) {
            return "Alto";
        }

        if (cumplimiento >= 70.0) {
            return "Medio";
        }

        return "Bajo";
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

    private ProductivityGrouping parseProductivityGrouping(String grouping) {
        if ("MENSUAL".equalsIgnoreCase(grouping)) {
            return ProductivityGrouping.MENSUAL;
        }

        return ProductivityGrouping.SEMANAL;
    }

    private PeriodInfo buildPeriodInfo(
            LocalDate fecha,
            ProductivityGrouping grouping,
            LocalDate reportStart,
            LocalDate reportEnd
    ) {
        if (grouping == ProductivityGrouping.MENSUAL) {
            LocalDate start = fecha.withDayOfMonth(1);
            LocalDate end = fecha.withDayOfMonth(fecha.lengthOfMonth());

            LocalDate clippedStart = clipStart(start, reportStart);
            LocalDate clippedEnd = clipEnd(end, reportEnd);

            String label = "Mes "
                    + String.format("%02d", fecha.getMonthValue())
                    + "-"
                    + fecha.getYear();

            return new PeriodInfo(label, clippedStart, clippedEnd);
        }

        LocalDate start = fecha.with(DayOfWeek.MONDAY);
        LocalDate end = fecha.with(DayOfWeek.SUNDAY);

        LocalDate clippedStart = clipStart(start, reportStart);
        LocalDate clippedEnd = clipEnd(end, reportEnd);

        int week = fecha.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int weekYear = fecha.get(IsoFields.WEEK_BASED_YEAR);

        String label = "Semana "
                + String.format("%02d", week)
                + "-"
                + weekYear;

        return new PeriodInfo(label, clippedStart, clippedEnd);
    }

    private LocalDate clipStart(LocalDate periodStart, LocalDate reportStart) {
        if (reportStart == null) {
            return periodStart;
        }

        return periodStart.isBefore(reportStart) ? reportStart : periodStart;
    }

    private LocalDate clipEnd(LocalDate periodEnd, LocalDate reportEnd) {
        if (reportEnd == null) {
            return periodEnd;
        }

        return periodEnd.isAfter(reportEnd) ? reportEnd : periodEnd;
    }

    private String normalizeEmployeeName(String value) {
        return value == null ? "Sin funcionario" : value.trim().toLowerCase();
    }

    private long totalDiasHabiles(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            return 1L;
        }

        long total = 0L;
        LocalDate current = fechaInicio;

        while (!current.isAfter(fechaFin)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();

            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                total++;
            }

            current = current.plusDays(1);
        }

        return Math.max(1L, total);
    }

    private enum ProductivityGrouping {
        SEMANAL,
        MENSUAL
    }

    private record PeriodInfo(
            String periodo,
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
    }

    private record ProductivityKey(
            String periodo,
            LocalDate fechaInicioPeriodo,
            LocalDate fechaFinPeriodo,
            Long funcionarioId,
            String funcionarioUsername
    ) {
    }
}