package com.appsisben.backend.modules.export.api;

import com.appsisben.backend.modules.audit.application.AuditService;
import com.appsisben.backend.modules.audit.domain.AuditAction;
import com.appsisben.backend.modules.dmc.dto.DmcFilterRequest;
import com.appsisben.backend.modules.export.application.DmcExcelExportService;
import com.appsisben.backend.modules.export.application.ReportExcelExportService;
import com.appsisben.backend.modules.export.application.VentanillaExcelExportService;
import com.appsisben.backend.modules.export.dto.ExportDmcPreviewResponse;
import com.appsisben.backend.modules.export.dto.ExportVentanillaPreviewResponse;
import com.appsisben.backend.modules.reports.dto.ReportDateRangeRequest;
import com.appsisben.backend.modules.ventanilla.dto.VentanillaFilterRequest;
import com.appsisben.backend.security.AppRolePreAuthorize;
import com.appsisben.backend.shared.api.ApiResponse;
import com.appsisben.backend.shared.export.ExcelResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/export")
@PreAuthorize(AppRolePreAuthorize.EXPORT)
public class ExcelExportController {

    private final VentanillaExcelExportService ventanillaExcelExportService;
    private final DmcExcelExportService dmcExcelExportService;
    private final ReportExcelExportService reportExcelExportService;
    private final AuditService auditService;

    @GetMapping("/preview/ventanilla")
    public ApiResponse<List<ExportVentanillaPreviewResponse>> previewVentanilla(
            @ModelAttribute VentanillaFilterRequest filter,
            @RequestParam(defaultValue = "200") Integer limit
    ) {
        return ApiResponse.ok(ventanillaExcelExportService.preview(filter, limit));
    }

    @GetMapping("/preview/dmc")
    public ApiResponse<List<ExportDmcPreviewResponse>> previewDmc(
            @ModelAttribute DmcFilterRequest filter,
            @RequestParam(defaultValue = "200") Integer limit
    ) {
        return ApiResponse.ok(dmcExcelExportService.preview(filter, limit));
    }

    @GetMapping("/ventanilla")
    public ResponseEntity<ByteArrayResource> exportVentanilla(@ModelAttribute VentanillaFilterRequest filter) {
        byte[] content = ventanillaExcelExportService.export(filter);

        auditService.safeLog(
                AuditAction.EXPORT,
                "ventanilla_registro",
                null,
                null,
                exportSnapshot("export-ventanilla", filter)
        );

        return ExcelResponseUtil.asAttachment(content, filename("ventanilla-registros"));
    }

    @GetMapping("/dmc")
    public ResponseEntity<ByteArrayResource> exportDmc(@ModelAttribute DmcFilterRequest filter) {
        byte[] content = dmcExcelExportService.export(filter);

        auditService.safeLog(
                AuditAction.EXPORT,
                "dmc_registro",
                null,
                null,
                exportSnapshot("export-dmc", filter)
        );

        return ExcelResponseUtil.asAttachment(content, filename("dmc-registros"));
    }

    @GetMapping("/reports/ventanilla")
    public ResponseEntity<ByteArrayResource> exportVentanillaReport(@ModelAttribute ReportDateRangeRequest request) {
        byte[] content = reportExcelExportService.exportVentanillaReport(request);

        auditService.safeLog(
                AuditAction.EXPORT,
                "reporte_ventanilla",
                null,
                null,
                exportSnapshot("export-reporte-ventanilla", request)
        );

        return ExcelResponseUtil.asAttachment(content, filename("reporte-ventanilla"));
    }

    @GetMapping("/reports/dmc")
    public ResponseEntity<ByteArrayResource> exportDmcReport(@ModelAttribute ReportDateRangeRequest request) {
        byte[] content = reportExcelExportService.exportDmcReport(request);

        auditService.safeLog(
                AuditAction.EXPORT,
                "reporte_dmc",
                null,
                null,
                exportSnapshot("export-reporte-dmc", request)
        );

        return ExcelResponseUtil.asAttachment(content, filename("reporte-dmc"));
    }

    private String filename(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        return prefix + "-" + timestamp + ".xlsx";
    }

    private Map<String, Object> exportSnapshot(String tipo, Object filtros) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tipo", tipo);
        data.put("filtros", filtros);
        data.put("fecha", LocalDateTime.now());
        return data;
    }
}