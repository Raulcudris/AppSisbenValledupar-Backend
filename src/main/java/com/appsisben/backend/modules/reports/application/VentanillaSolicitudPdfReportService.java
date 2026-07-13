package com.appsisben.backend.modules.reports.application;

import com.appsisben.backend.modules.reports.dto.VentanillaSolicitudDailyCount;
import com.appsisben.backend.modules.ventanilla.repository.VentanillaRegistroRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VentanillaSolicitudPdfReportService {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");

    private static final DateTimeFormatter DAILY_LABEL_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter MONTHLY_LABEL_FORMAT =
            DateTimeFormatter.ofPattern("MM/yyyy");

    private static final DateTimeFormatter MONTHLY_KEY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM");

    private static final int MAX_DAILY_DAYS = 31;
    private static final int MAX_RANGE_DAYS = 1825;

    private final VentanillaRegistroRepository ventanillaRepository;

    @Transactional(readOnly = true)
    public byte[] generate(LocalDate fechaInicio, LocalDate fechaFin) {
        validateDates(fechaInicio, fechaFin);

        long days = ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;
        boolean monthlyGrouping = days > MAX_DAILY_DAYS;

        List<ReportColumn> columns = monthlyGrouping
                ? buildMonthlyColumns(fechaInicio, fechaFin)
                : buildDailyColumns(fechaInicio, fechaFin);

        List<VentanillaSolicitudDailyCount> data = ventanillaRepository.countSolicitudesByFecha(
                fechaInicio,
                fechaFin
        );

        List<ReportRow> rows = buildRows(data, columns, monthlyGrouping);
        long totalPeriodo = rows.stream().mapToLong(ReportRow::total).sum();

        return buildPdf(
                fechaInicio,
                fechaFin,
                columns,
                rows,
                totalPeriodo,
                monthlyGrouping
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
                    "El reporte PDF permite máximo 5 años por generación"
            );
        }
    }

    private List<ReportColumn> buildDailyColumns(LocalDate fechaInicio, LocalDate fechaFin) {
        List<ReportColumn> columns = new ArrayList<>();
        LocalDate current = fechaInicio;

        while (!current.isAfter(fechaFin)) {
            String key = current.format(DateTimeFormatter.ISO_DATE);
            String label = current.format(DAILY_LABEL_FORMAT);

            columns.add(new ReportColumn(key, label));
            current = current.plusDays(1);
        }

        return columns;
    }

    private List<ReportColumn> buildMonthlyColumns(LocalDate fechaInicio, LocalDate fechaFin) {
        List<ReportColumn> columns = new ArrayList<>();

        YearMonth current = YearMonth.from(fechaInicio);
        YearMonth end = YearMonth.from(fechaFin);

        while (!current.isAfter(end)) {
            String key = current.format(MONTHLY_KEY_FORMAT);
            String label = current.format(MONTHLY_LABEL_FORMAT);

            columns.add(new ReportColumn(key, label));
            current = current.plusMonths(1);
        }

        return columns;
    }

    private List<ReportRow> buildRows(
            List<VentanillaSolicitudDailyCount> data,
            List<ReportColumn> columns,
            boolean monthlyGrouping
    ) {
        Map<String, Map<String, Long>> grouped = new LinkedHashMap<>();

        for (VentanillaSolicitudDailyCount item : data) {
            String columnKey = monthlyGrouping
                    ? YearMonth.from(item.fecha()).format(MONTHLY_KEY_FORMAT)
                    : item.fecha().format(DateTimeFormatter.ISO_DATE);

            grouped
                    .computeIfAbsent(item.solicitudNombre(), key -> new LinkedHashMap<>())
                    .merge(columnKey, item.cantidad(), Long::sum);
        }

        List<ReportRow> rows = new ArrayList<>();

        grouped.forEach((solicitud, values) -> {
            Map<String, Long> countsByColumn = new LinkedHashMap<>();

            for (ReportColumn column : columns) {
                countsByColumn.put(column.key(), values.getOrDefault(column.key(), 0L));
            }

            long total = countsByColumn.values()
                    .stream()
                    .mapToLong(Long::longValue)
                    .sum();

            rows.add(new ReportRow(solicitud, countsByColumn, total));
        });

        rows.sort(
                Comparator.comparingLong(ReportRow::total)
                        .reversed()
                        .thenComparing(ReportRow::solicitud)
        );

        return rows;
    }

    private byte[] buildPdf(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            List<ReportColumn> columns,
            List<ReportRow> rows,
            long totalPeriodo,
            boolean monthlyGrouping
    ) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Rectangle pageSize = columns.size() <= 2 ? PageSize.A4 : PageSize.A4.rotate();
            Document document = new Document(pageSize, 24, 24, 24, 24);

            PdfWriter.getInstance(document, outputStream);
            document.open();

            addHeader(document, fechaInicio, fechaFin, monthlyGrouping);
            addTable(document, columns, rows, totalPeriodo);
            addFooter(document, fechaInicio, fechaFin);

            document.close();

            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible generar el reporte PDF"
            );
        }
    }

    private void addHeader(
            Document document,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            boolean monthlyGrouping
    ) throws Exception {
        String subtitle = "Informe de solicitudes - Ventanilla | "
                + buildPeriodTitle(fechaInicio, fechaFin);

        Font smallFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);
        Paragraph subtitleParagraph = new Paragraph(subtitle, smallFont);
        subtitleParagraph.setAlignment(Element.ALIGN_RIGHT);
        document.add(subtitleParagraph);

        Font titleFont = new Font(Font.HELVETICA, 17, Font.BOLD, Color.BLACK);
        Paragraph title = new Paragraph("Totales por tipo de solicitud - Ventanilla", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(4);
        title.setSpacingAfter(10);
        document.add(title);

        Font dateFont = new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK);

        Paragraph startDate = new Paragraph(
                "Fecha Inicio: " + fechaInicio.format(DAILY_LABEL_FORMAT),
                dateFont
        );
        startDate.setAlignment(Element.ALIGN_CENTER);
        document.add(startDate);

        Paragraph endDate = new Paragraph(
                "Fecha Fin: " + fechaFin.format(DAILY_LABEL_FORMAT),
                dateFont
        );
        endDate.setAlignment(Element.ALIGN_CENTER);
        document.add(endDate);

        Paragraph grouping = new Paragraph(
                "Agrupación: " + (monthlyGrouping ? "Mensual" : "Diaria"),
                new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY)
        );
        grouping.setAlignment(Element.ALIGN_CENTER);
        grouping.setSpacingAfter(8);
        document.add(grouping);
    }

    private void addTable(
            Document document,
            List<ReportColumn> columns,
            List<ReportRow> rows,
            long totalPeriodo
    ) throws Exception {
        int tableColumns = columns.size() + 3;

        PdfPTable table = new PdfPTable(tableColumns);
        table.setWidthPercentage(100);
        table.setWidths(buildColumnWidths(columns.size()));

        Color headerColor = new Color(218, 231, 242);
        Color borderColor = new Color(150, 150, 150);

        table.addCell(headerCell(".", headerColor, borderColor, 7));

        PdfPCell fechasHeader = headerCell("Fechas", headerColor, borderColor, 7);
        fechasHeader.setColspan(columns.size());
        table.addCell(fechasHeader);

        table.addCell(headerCell("", headerColor, borderColor, 7));
        table.addCell(headerCell("", headerColor, borderColor, 7));

        table.addCell(headerCell("Solicitudes", headerColor, borderColor, 7));

        for (ReportColumn column : columns) {
            table.addCell(headerCell(column.label(), headerColor, borderColor, 7));
        }

        table.addCell(headerCell("Total general", headerColor, borderColor, 7));
        table.addCell(headerCell("%", headerColor, borderColor, 7));

        if (rows.isEmpty()) {
            PdfPCell emptyCell = bodyCell(
                    "No hay registros para el periodo seleccionado",
                    borderColor,
                    Element.ALIGN_CENTER,
                    7
            );
            emptyCell.setColspan(tableColumns);
            table.addCell(emptyCell);
        } else {
            for (ReportRow row : rows) {
                table.addCell(bodyCell(row.solicitud(), borderColor, Element.ALIGN_LEFT, 7));

                for (ReportColumn column : columns) {
                    long value = row.countsByColumn().getOrDefault(column.key(), 0L);

                    table.addCell(bodyCell(
                            value == 0 ? "" : String.valueOf(value),
                            borderColor,
                            Element.ALIGN_CENTER,
                            7
                    ));
                }

                table.addCell(bodyCell(String.valueOf(row.total()), borderColor, Element.ALIGN_CENTER, 7));
                table.addCell(bodyCell(formatPercent(row.total(), totalPeriodo), borderColor, Element.ALIGN_CENTER, 7));
            }
        }

        document.add(table);
    }

    private void addFooter(
            Document document,
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) throws Exception {
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingBefore(18);
        document.add(spacer);

        Font footerFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);

        Paragraph footer = new Paragraph(
                "Fuente: Reporte Estadístico Ventanilla, periodo "
                        + fechaInicio.format(DAILY_LABEL_FORMAT)
                        + " a "
                        + fechaFin.format(DAILY_LABEL_FORMAT)
                        + ".",
                footerFont
        );

        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private float[] buildColumnWidths(int dateColumns) {
        float[] widths = new float[dateColumns + 3];

        widths[0] = 5.2f;

        for (int i = 1; i <= dateColumns; i++) {
            widths[i] = 1.05f;
        }

        widths[dateColumns + 1] = 1.35f;
        widths[dateColumns + 2] = 0.9f;

        return widths;
    }

    private PdfPCell headerCell(
            String value,
            Color backgroundColor,
            Color borderColor,
            int fontSize
    ) {
        Font font = new Font(Font.HELVETICA, fontSize, Font.BOLD, Color.BLACK);
        PdfPCell cell = new PdfPCell(new Phrase(value, font));

        cell.setBackgroundColor(backgroundColor);
        cell.setBorderColor(borderColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);

        return cell;
    }

    private PdfPCell bodyCell(
            String value,
            Color borderColor,
            int align,
            int fontSize
    ) {
        Font font = new Font(Font.HELVETICA, fontSize, Font.NORMAL, Color.BLACK);
        PdfPCell cell = new PdfPCell(new Phrase(value, font));

        cell.setBorderColor(borderColor);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);

        return cell;
    }

    private String formatPercent(long value, long total) {
        if (total <= 0) {
            return "0,0 %";
        }

        BigDecimal percentage = BigDecimal.valueOf(value)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);

        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(LOCALE_ES_CO);
        DecimalFormat formatter = new DecimalFormat("#,##0.0", symbols);

        return formatter.format(percentage) + " %";
    }

    private String buildPeriodTitle(LocalDate fechaInicio, LocalDate fechaFin) {
        YearMonth startMonth = YearMonth.from(fechaInicio);
        YearMonth endMonth = YearMonth.from(fechaFin);

        if (startMonth.equals(endMonth)) {
            return capitalize(
                    fechaFin.getMonth().getDisplayName(TextStyle.FULL, LOCALE_ES_CO)
            ) + " " + fechaFin.getYear();
        }

        return capitalize(
                fechaInicio.getMonth().getDisplayName(TextStyle.FULL, LOCALE_ES_CO)
        ) + " " + fechaInicio.getYear()
                + " a "
                + capitalize(
                fechaFin.getMonth().getDisplayName(TextStyle.FULL, LOCALE_ES_CO)
        ) + " " + fechaFin.getYear();
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.substring(0, 1).toUpperCase(LOCALE_ES_CO)
                + value.substring(1).toLowerCase(LOCALE_ES_CO);
    }

    private record ReportColumn(
            String key,
            String label
    ) {
    }

    private record ReportRow(
            String solicitud,
            Map<String, Long> countsByColumn,
            long total
    ) {
    }
}