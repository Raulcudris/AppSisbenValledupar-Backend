package com.appsisben.backend.modules.export.application;

import com.appsisben.backend.modules.ventanilla.domain.VentanillaRegistro;
import com.appsisben.backend.modules.ventanilla.dto.VentanillaFilterRequest;
import com.appsisben.backend.modules.ventanilla.repository.VentanillaRegistroRepository;
import com.appsisben.backend.modules.ventanilla.repository.VentanillaSpecification;
import com.appsisben.backend.shared.export.ExcelExportException;
import com.appsisben.backend.shared.export.ExcelWorkbookUtil;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VentanillaExcelExportService {

    private final VentanillaRegistroRepository repository;

    @Transactional(readOnly = true)
    public byte[] export(VentanillaFilterRequest filter) {
        boolean allowInactiveRecords = isAdmin()
                && filter != null
                && Boolean.TRUE.equals(filter.incluirInactivos());

        List<VentanillaRegistro> registros = repository.findAll(
                VentanillaSpecification.byFilter(filter, allowInactiveRecords),
                Sort.by(Sort.Direction.DESC, "fecha").and(Sort.by(Sort.Direction.DESC, "id"))
        );

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Ventanilla");
            CellStyle titleStyle = ExcelWorkbookUtil.titleStyle(workbook);
            CellStyle headerStyle = ExcelWorkbookUtil.headerStyle(workbook);
            CellStyle dateStyle = ExcelWorkbookUtil.dateStyle(workbook);

            List<String> headers = List.of(
                    "ID",
                    "Fecha",
                    "Número ventanilla",
                    "Funcionario",
                    "Cédula usuario",
                    "Nombre usuario",
                    "Teléfono",
                    "Categoría",
                    "Dirección",
                    "Barrio",
                    "Comuna",
                    "Extranjero",
                    "Solicitud",
                    "Estado solicitud",
                    "Estado registro",
                    "Observación"
            );

            ExcelWorkbookUtil.createTitle(sheet, "Exportación registros de ventanilla", headers.size(), titleStyle);
            ExcelWorkbookUtil.createHeader(sheet, 2, headers, headerStyle);

            int rowIndex = 3;

            for (VentanillaRegistro registro : registros) {
                Row row = sheet.createRow(rowIndex++);
                int column = 0;

                ExcelWorkbookUtil.setCell(row, column++, registro.getId(), dateStyle);
                ExcelWorkbookUtil.setCell(row, column++, registro.getFecha(), dateStyle);
                ExcelWorkbookUtil.setCell(row, column++, registro.getNumeroVentanilla(), dateStyle);
                ExcelWorkbookUtil.setCell(
                        row,
                        column++,
                        registro.getFuncionario() != null ? registro.getFuncionario().getUsername() : "",
                        dateStyle
                );
                ExcelWorkbookUtil.setCell(row, column++, registro.getCedulaUsuario(), dateStyle);
                ExcelWorkbookUtil.setCell(row, column++, registro.getNombreUsuario(), dateStyle);
                ExcelWorkbookUtil.setCell(row, column++, registro.getTelefono(), dateStyle);
                ExcelWorkbookUtil.setCell(
                        row,
                        column++,
                        registro.getCategoria() != null ? registro.getCategoria().getNombre() : "",
                        dateStyle
                );
                ExcelWorkbookUtil.setCell(row, column++, registro.getDireccion(), dateStyle);
                ExcelWorkbookUtil.setCell(
                        row,
                        column++,
                        registro.getBarrio() != null ? registro.getBarrio().getNombre() : "",
                        dateStyle
                );
                ExcelWorkbookUtil.setCell(
                        row,
                        column++,
                        registro.getBarrio() != null && registro.getBarrio().getComuna() != null
                                ? registro.getBarrio().getComuna().getNombre()
                                : "",
                        dateStyle
                );
                ExcelWorkbookUtil.setCell(row, column++, registro.getExtranjero(), dateStyle);
                ExcelWorkbookUtil.setCell(
                        row,
                        column++,
                        registro.getSolicitud() != null ? registro.getSolicitud().getNombre() : "",
                        dateStyle
                );
                ExcelWorkbookUtil.setCell(
                        row,
                        column++,
                        registro.getEstadoSolicitud() != null ? registro.getEstadoSolicitud().getNombre() : "",
                        dateStyle
                );
                ExcelWorkbookUtil.setCell(
                        row,
                        column++,
                        Boolean.TRUE.equals(registro.getActivo()) ? "ACTIVO" : "INACTIVO",
                        dateStyle
                );
                ExcelWorkbookUtil.setCell(row, column, registro.getObservacion(), dateStyle);
            }

            ExcelWorkbookUtil.autoSize(sheet, headers.size());
            workbook.write(outputStream);

            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new ExcelExportException("No fue posible exportar los registros de ventanilla", ex);
        }
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ADMIN".equals(authority) || "ROLE_ADMIN".equals(authority));
    }
}