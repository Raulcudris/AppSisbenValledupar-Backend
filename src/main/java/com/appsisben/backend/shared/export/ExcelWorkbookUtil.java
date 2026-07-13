package com.appsisben.backend.shared.export;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;

import java.time.LocalDate;
import java.util.List;

public final class ExcelWorkbookUtil {

    private ExcelWorkbookUtil() {
    }

    public static CellStyle titleStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    public static CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    public static CellStyle dateStyle(Workbook workbook) {
        CreationHelper creationHelper = workbook.getCreationHelper();

        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd"));
        return style;
    }

    public static void createTitle(Sheet sheet, String title, int columns, CellStyle titleStyle) {
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(titleStyle);
        row.setHeightInPoints(24);

        if (columns > 1) {
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, columns - 1));
        }
    }

    public static void createHeader(Sheet sheet, int rowIndex, List<String> headers, CellStyle headerStyle) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(28);

        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
        }
    }

    public static void setCell(Row row, int columnIndex, Object value, CellStyle dateStyle) {
        Cell cell = row.createCell(columnIndex);

        if (value == null) {
            cell.setCellValue("");
            return;
        }

        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            return;
        }

        if (value instanceof Boolean bool) {
            cell.setCellValue(Boolean.TRUE.equals(bool) ? "SI" : "NO");
            return;
        }

        if (value instanceof LocalDate date) {
            cell.setCellValue(java.sql.Date.valueOf(date));
            cell.setCellStyle(dateStyle);
            return;
        }

        cell.setCellValue(String.valueOf(value));
    }

    public static void autoSize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    public static String safe(String value) {
        return value != null ? value : "";
    }
}
