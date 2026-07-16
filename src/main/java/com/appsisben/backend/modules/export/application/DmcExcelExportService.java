package com.appsisben.backend.modules.export.application;

import com.appsisben.backend.modules.dmc.domain.DmcRegistro;
import com.appsisben.backend.modules.dmc.dto.DmcFilterRequest;
import com.appsisben.backend.modules.dmc.repository.DmcRegistroRepository;
import com.appsisben.backend.modules.dmc.repository.DmcSpecification;
import com.appsisben.backend.modules.export.dto.ExportDmcPreviewResponse;
import com.appsisben.backend.shared.export.ExcelExportException;
import com.appsisben.backend.shared.export.ExcelWorkbookUtil;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DmcExcelExportService {

    private final DmcRegistroRepository repository;

    @Transactional(readOnly = true)
    public List<ExportDmcPreviewResponse> preview(DmcFilterRequest filter, Integer limit) {
        int safeLimit = limit == null ? 200 : Math.max(1, Math.min(limit, 500));

        return repository.findAll(
                        DmcSpecification.byFilter(filter),
                        PageRequest.of(
                                0,
                                safeLimit,
                                Sort.by(Sort.Direction.DESC, "fecha")
                                        .and(Sort.by(Sort.Direction.DESC, "id"))
                        )
                )
                .getContent()
                .stream()
                .map(this::toPreviewResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] export(DmcFilterRequest filter) {
        List<DmcRegistro> registros = repository.findAll(
                DmcSpecification.byFilter(filter),
                Sort.by(Sort.Direction.DESC, "fecha").and(Sort.by(Sort.Direction.DESC, "id"))
        );

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("DMC");
            CellStyle titleStyle = ExcelWorkbookUtil.titleStyle(workbook);
            CellStyle headerStyle = ExcelWorkbookUtil.headerStyle(workbook);
            CellStyle dateStyle = ExcelWorkbookUtil.dateStyle(workbook);

            List<String> headers = List.of(
                    "ID",
                    "Fecha",
                    "Funcionario",
                    "Tipo DMC código",
                    "Tipo DMC nombre",
                    "Encuestador",
                    "Cantidad",
                    "Barrio",
                    "Comuna",
                    "Observación"
            );

            ExcelWorkbookUtil.createTitle(sheet, "Exportación registros DMC", headers.size(), titleStyle);
            ExcelWorkbookUtil.createHeader(sheet, 2, headers, headerStyle);

            int rowIndex = 3;
            for (DmcRegistro registro : registros) {
                Row row = sheet.createRow(rowIndex++);
                int column = 0;

                ExcelWorkbookUtil.setCell(row, column++, registro.getId(), dateStyle);
                ExcelWorkbookUtil.setCell(row, column++, registro.getFecha(), dateStyle);
                ExcelWorkbookUtil.setCell(row, column++, registro.getFuncionario() != null ? registro.getFuncionario().getUsername() : "", dateStyle);
                ExcelWorkbookUtil.setCell(row, column++, registro.getTipoDmc() != null ? registro.getTipoDmc().getCodigo() : "", dateStyle);
                ExcelWorkbookUtil.setCell(row, column++, registro.getTipoDmc() != null ? registro.getTipoDmc().getNombre() : "", dateStyle);
                ExcelWorkbookUtil.setCell(row, column++, registro.getEncuestador() != null ? registro.getEncuestador().getNombre() : "", dateStyle);
                ExcelWorkbookUtil.setCell(row, column++, registro.getCantidad(), dateStyle);
                ExcelWorkbookUtil.setCell(row, column++, registro.getBarrio() != null ? registro.getBarrio().getNombre() : "", dateStyle);
                ExcelWorkbookUtil.setCell(row, column++, registro.getBarrio() != null && registro.getBarrio().getComuna() != null
                        ? registro.getBarrio().getComuna().getNombre()
                        : "", dateStyle);
                ExcelWorkbookUtil.setCell(row, column, registro.getObservacion(), dateStyle);
            }

            ExcelWorkbookUtil.autoSize(sheet, headers.size());
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new ExcelExportException("No fue posible exportar los registros DMC", ex);
        }
    }

    private ExportDmcPreviewResponse toPreviewResponse(DmcRegistro registro) {
        return new ExportDmcPreviewResponse(
                registro.getId(),
                registro.getFecha(),
                registro.getFuncionario() != null ? registro.getFuncionario().getUsername() : "",
                registro.getTipoDmc() != null ? registro.getTipoDmc().getCodigo() : "",
                registro.getTipoDmc() != null ? registro.getTipoDmc().getNombre() : "",
                registro.getEncuestador() != null ? registro.getEncuestador().getNombre() : "",
                registro.getCantidad(),
                registro.getBarrio() != null ? registro.getBarrio().getNombre() : "",
                registro.getBarrio() != null && registro.getBarrio().getComuna() != null
                        ? registro.getBarrio().getComuna().getNombre()
                        : "",
                registro.getObservacion()
        );
    }
}