package com.appsisben.backend.modules.reports.application;

import com.appsisben.backend.modules.reports.dto.VentanillaSolicitudDailyCount;
import com.appsisben.backend.modules.reports.dto.VentanillaSolicitudPreviewResponse;
import com.appsisben.backend.modules.reports.dto.VentanillaSolicitudPreviewRowResponse;
import com.appsisben.backend.modules.ventanilla.repository.VentanillaRegistroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VentanillaSolicitudPreviewService {

    private static final DateTimeFormatter DAILY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTHLY_FORMAT = DateTimeFormatter.ofPattern("MM/yyyy");

    private static final int MAX_DAILY_DAYS = 31;
    private static final int MAX_RANGE_DAYS = 1825;

    private final VentanillaRegistroRepository ventanillaRepository;

    @Transactional(readOnly = true)
    public VentanillaSolicitudPreviewResponse preview(LocalDate fechaInicio, LocalDate fechaFin) {
        validateDates(fechaInicio, fechaFin);

        long days = ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;
        boolean monthly = days > MAX_DAILY_DAYS;

        List<String> columns = monthly
                ? buildMonthColumns(fechaInicio, fechaFin)
                : buildDailyColumns(fechaInicio, fechaFin);

        List<VentanillaSolicitudDailyCount> data = ventanillaRepository.countSolicitudesByFecha(
                fechaInicio,
                fechaFin
        );

        List<VentanillaSolicitudPreviewRowResponse> rows = monthly
                ? buildMonthlyRows(data, columns)
                : buildDailyRows(data, columns);

        long totalGeneral = rows.stream()
                .mapToLong(VentanillaSolicitudPreviewRowResponse::totalGeneral)
                .sum();

        List<VentanillaSolicitudPreviewRowResponse> rowsWithPercentage = rows.stream()
                .map(row -> new VentanillaSolicitudPreviewRowResponse(
                        row.solicitud(),
                        row.cantidadesPorFecha(),
                        row.totalGeneral(),
                        calculatePercentage(row.totalGeneral(), totalGeneral)
                ))
                .sorted(
                        Comparator.comparingLong(VentanillaSolicitudPreviewRowResponse::totalGeneral)
                                .reversed()
                                .thenComparing(VentanillaSolicitudPreviewRowResponse::solicitud)
                )
                .toList();

        return new VentanillaSolicitudPreviewResponse(
                fechaInicio,
                fechaFin,
                monthly ? "MENSUAL" : "DIARIA",
                columns,
                totalGeneral,
                rowsWithPercentage
        );
    }

    private void validateDates(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha inicio es obligatoria"
            );
        }

        if (fechaFin == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha fin es obligatoria"
            );
        }

        if (fechaFin.isBefore(fechaInicio)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha fin no puede ser menor que la fecha inicio"
            );
        }

        long days = ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;

        if (days > MAX_RANGE_DAYS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La previsualización permite máximo 5 años por consulta"
            );
        }
    }

    private List<String> buildDailyColumns(LocalDate fechaInicio, LocalDate fechaFin) {
        List<String> columns = new ArrayList<>();
        LocalDate current = fechaInicio;

        while (!current.isAfter(fechaFin)) {
            columns.add(current.format(DAILY_FORMAT));
            current = current.plusDays(1);
        }

        return columns;
    }

    private List<String> buildMonthColumns(LocalDate fechaInicio, LocalDate fechaFin) {
        List<String> columns = new ArrayList<>();

        YearMonth current = YearMonth.from(fechaInicio);
        YearMonth end = YearMonth.from(fechaFin);

        while (!current.isAfter(end)) {
            columns.add(current.format(MONTHLY_FORMAT));
            current = current.plusMonths(1);
        }

        return columns;
    }

    private List<VentanillaSolicitudPreviewRowResponse> buildDailyRows(
            List<VentanillaSolicitudDailyCount> data,
            List<String> columns
    ) {
        Map<String, Map<String, Long>> grouped = new LinkedHashMap<>();

        for (VentanillaSolicitudDailyCount item : data) {
            String column = item.fecha().format(DAILY_FORMAT);

            grouped
                    .computeIfAbsent(item.solicitudNombre(), key -> new LinkedHashMap<>())
                    .merge(column, item.cantidad(), Long::sum);
        }

        return buildRowsFromGroupedData(grouped, columns);
    }

    private List<VentanillaSolicitudPreviewRowResponse> buildMonthlyRows(
            List<VentanillaSolicitudDailyCount> data,
            List<String> columns
    ) {
        Map<String, Map<String, Long>> grouped = new LinkedHashMap<>();

        for (VentanillaSolicitudDailyCount item : data) {
            String column = YearMonth.from(item.fecha()).format(MONTHLY_FORMAT);

            grouped
                    .computeIfAbsent(item.solicitudNombre(), key -> new LinkedHashMap<>())
                    .merge(column, item.cantidad(), Long::sum);
        }

        return buildRowsFromGroupedData(grouped, columns);
    }

    private List<VentanillaSolicitudPreviewRowResponse> buildRowsFromGroupedData(
            Map<String, Map<String, Long>> grouped,
            List<String> columns
    ) {
        List<VentanillaSolicitudPreviewRowResponse> rows = new ArrayList<>();

        grouped.forEach((solicitud, values) -> {
            Map<String, Long> cantidadesPorFecha = new LinkedHashMap<>();

            for (String column : columns) {
                cantidadesPorFecha.put(column, values.getOrDefault(column, 0L));
            }

            long total = cantidadesPorFecha.values()
                    .stream()
                    .mapToLong(Long::longValue)
                    .sum();

            rows.add(new VentanillaSolicitudPreviewRowResponse(
                    solicitud,
                    cantidadesPorFecha,
                    total,
                    BigDecimal.ZERO
            ));
        });

        return rows;
    }

    private BigDecimal calculatePercentage(long value, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(value)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
}