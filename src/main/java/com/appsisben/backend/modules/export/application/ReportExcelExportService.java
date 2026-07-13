package com.appsisben.backend.modules.export.application;

import com.appsisben.backend.modules.reports.application.DmcReportService;
import com.appsisben.backend.modules.reports.application.VentanillaReportService;
import com.appsisben.backend.modules.reports.dto.DmcReportSummaryResponse;
import com.appsisben.backend.modules.reports.dto.ReportDateRangeRequest;
import com.appsisben.backend.modules.reports.dto.ReportGroupResponse;
import com.appsisben.backend.modules.reports.dto.VentanillaReportSummaryResponse;
import com.appsisben.backend.shared.export.ExcelExportException;
import com.appsisben.backend.shared.export.ExcelWorkbookUtil;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportExcelExportService {

    private final VentanillaReportService ventanillaReportService;
    private final DmcReportService dmcReportService;

    @Transactional(readOnly = true)
    public byte[] exportVentanillaReport(ReportDateRangeRequest request) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            CellStyle titleStyle = ExcelWorkbookUtil.titleStyle(workbook);
            CellStyle headerStyle = ExcelWorkbookUtil.headerStyle(workbook);
            CellStyle dateStyle = ExcelWorkbookUtil.dateStyle(workbook);

            createVentanillaSummarySheet(
                    workbook,
                    ventanillaReportService.summary(request),
                    titleStyle,
                    headerStyle,
                    dateStyle
            );

            createGroupSheet(workbook, "Por estado", "Reporte ventanilla por estado",
                    ventanillaReportService.byStatus(request), titleStyle, headerStyle, dateStyle);
            createGroupSheet(workbook, "Por solicitud", "Reporte ventanilla por tipo de solicitud",
                    ventanillaReportService.byRequestType(request), titleStyle, headerStyle, dateStyle);
            createGroupSheet(workbook, "Por categoria", "Reporte ventanilla por categoría",
                    ventanillaReportService.byCategory(request), titleStyle, headerStyle, dateStyle);
            createGroupSheet(workbook, "Por funcionario", "Reporte ventanilla por funcionario",
                    ventanillaReportService.byUser(request), titleStyle, headerStyle, dateStyle);
            createGroupSheet(workbook, "Por barrio", "Reporte ventanilla por barrio",
                    ventanillaReportService.byNeighborhood(request), titleStyle, headerStyle, dateStyle);
            createGroupSheet(workbook, "Por comuna", "Reporte ventanilla por comuna",
                    ventanillaReportService.byComuna(request), titleStyle, headerStyle, dateStyle);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new ExcelExportException("No fue posible exportar el reporte de ventanilla", ex);
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportDmcReport(ReportDateRangeRequest request) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            CellStyle titleStyle = ExcelWorkbookUtil.titleStyle(workbook);
            CellStyle headerStyle = ExcelWorkbookUtil.headerStyle(workbook);
            CellStyle dateStyle = ExcelWorkbookUtil.dateStyle(workbook);

            createDmcSummarySheet(
                    workbook,
                    dmcReportService.summary(request),
                    titleStyle,
                    headerStyle,
                    dateStyle
            );

            createGroupSheet(workbook, "Por tipo", "Reporte DMC por tipo",
                    dmcReportService.byType(request), titleStyle, headerStyle, dateStyle);
            createGroupSheet(workbook, "Por encuestador", "Reporte DMC por encuestador",
                    dmcReportService.bySurveyor(request), titleStyle, headerStyle, dateStyle);
            createGroupSheet(workbook, "Por funcionario", "Reporte DMC por funcionario",
                    dmcReportService.byUser(request), titleStyle, headerStyle, dateStyle);
            createGroupSheet(workbook, "Por barrio", "Reporte DMC por barrio",
                    dmcReportService.byNeighborhood(request), titleStyle, headerStyle, dateStyle);
            createGroupSheet(workbook, "Por comuna", "Reporte DMC por comuna",
                    dmcReportService.byComuna(request), titleStyle, headerStyle, dateStyle);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new ExcelExportException("No fue posible exportar el reporte DMC", ex);
        }
    }

    private void createVentanillaSummarySheet(
            Workbook workbook,
            VentanillaReportSummaryResponse summary,
            CellStyle titleStyle,
            CellStyle headerStyle,
            CellStyle dateStyle
    ) {
        Sheet sheet = workbook.createSheet("Resumen");
        ExcelWorkbookUtil.createTitle(sheet, "Resumen reporte ventanilla", 2, titleStyle);
        ExcelWorkbookUtil.createHeader(sheet, 2, List.of("Indicador", "Total"), headerStyle);

        int rowIndex = 3;
        rowIndex = addMetric(sheet, rowIndex, "Total registros", summary.totalRegistros(), dateStyle);
        rowIndex = addMetric(sheet, rowIndex, "Pendientes", summary.pendientes(), dateStyle);
        rowIndex = addMetric(sheet, rowIndex, "Realizadas", summary.realizadas(), dateStyle);
        rowIndex = addMetric(sheet, rowIndex, "Aprobadas", summary.aprobadas(), dateStyle);
        rowIndex = addMetric(sheet, rowIndex, "Rechazadas", summary.rechazadas(), dateStyle);
        rowIndex = addMetric(sheet, rowIndex, "Canceladas", summary.canceladas(), dateStyle);
        rowIndex = addMetric(sheet, rowIndex, "Revisar", summary.revisar(), dateStyle);
        rowIndex = addMetric(sheet, rowIndex, "Extranjeros", summary.extranjeros(), dateStyle);
        addMetric(sheet, rowIndex, "Nacionales", summary.nacionales(), dateStyle);

        ExcelWorkbookUtil.autoSize(sheet, 2);
    }

    private void createDmcSummarySheet(
            Workbook workbook,
            DmcReportSummaryResponse summary,
            CellStyle titleStyle,
            CellStyle headerStyle,
            CellStyle dateStyle
    ) {
        Sheet sheet = workbook.createSheet("Resumen");
        ExcelWorkbookUtil.createTitle(sheet, "Resumen reporte DMC", 2, titleStyle);
        ExcelWorkbookUtil.createHeader(sheet, 2, List.of("Indicador", "Total"), headerStyle);

        int rowIndex = 3;
        rowIndex = addMetric(sheet, rowIndex, "Total registros", summary.totalRegistros(), dateStyle);
        rowIndex = addMetric(sheet, rowIndex, "Total cantidad", summary.totalCantidad(), dateStyle);
        rowIndex = addMetric(sheet, rowIndex, "Total cargadas", summary.totalCargadas(), dateStyle);
        rowIndex = addMetric(sheet, rowIndex, "Total descargadas", summary.totalDescargadas(), dateStyle);
        addMetric(sheet, rowIndex, "Total rechazadas", summary.totalRechazadas(), dateStyle);

        ExcelWorkbookUtil.autoSize(sheet, 2);
    }

    private int addMetric(Sheet sheet, int rowIndex, String name, Long total, CellStyle dateStyle) {
        Row row = sheet.createRow(rowIndex);
        ExcelWorkbookUtil.setCell(row, 0, name, dateStyle);
        ExcelWorkbookUtil.setCell(row, 1, total, dateStyle);
        return rowIndex + 1;
    }

    private void createGroupSheet(
            Workbook workbook,
            String sheetName,
            String title,
            List<ReportGroupResponse> rows,
            CellStyle titleStyle,
            CellStyle headerStyle,
            CellStyle dateStyle
    ) {
        Sheet sheet = workbook.createSheet(sheetName);
        ExcelWorkbookUtil.createTitle(sheet, title, 4, titleStyle);
        ExcelWorkbookUtil.createHeader(sheet, 2, List.of("ID", "Código", "Nombre", "Total"), headerStyle);

        int rowIndex = 3;
        for (ReportGroupResponse item : rows) {
            Row row = sheet.createRow(rowIndex++);
            ExcelWorkbookUtil.setCell(row, 0, item.id(), dateStyle);
            ExcelWorkbookUtil.setCell(row, 1, item.codigo(), dateStyle);
            ExcelWorkbookUtil.setCell(row, 2, item.nombre(), dateStyle);
            ExcelWorkbookUtil.setCell(row, 3, item.total(), dateStyle);
        }

        ExcelWorkbookUtil.autoSize(sheet, 4);
    }
}
