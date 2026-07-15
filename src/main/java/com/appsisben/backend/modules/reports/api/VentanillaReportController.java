package com.appsisben.backend.modules.reports.api;

import com.appsisben.backend.modules.reports.application.VentanillaReportService;
import com.appsisben.backend.modules.reports.application.VentanillaSolicitudPdfReportService;
import com.appsisben.backend.modules.reports.application.VentanillaSolicitudPreviewService;
import com.appsisben.backend.modules.reports.dto.ReportDateRangeRequest;
import com.appsisben.backend.modules.reports.dto.ReportGroupResponse;
import com.appsisben.backend.modules.reports.dto.VentanillaDailyTrendResponse;
import com.appsisben.backend.modules.reports.dto.VentanillaFuncionarioPerformanceResponse;
import com.appsisben.backend.modules.reports.dto.VentanillaFuncionarioTrendResponse;
import com.appsisben.backend.modules.reports.dto.VentanillaReportSummaryResponse;
import com.appsisben.backend.modules.reports.dto.VentanillaSolicitudPreviewResponse;
import com.appsisben.backend.security.AppRolePreAuthorize;
import com.appsisben.backend.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping({
        "/api/reportes",
        "/api/reports"
})
public class VentanillaReportController {

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final VentanillaSolicitudPdfReportService pdfService;
    private final VentanillaSolicitudPreviewService previewService;
    private final VentanillaReportService reportService;

    @PreAuthorize(AppRolePreAuthorize.REPORT_READ)
    @GetMapping("/ventanilla/summary")
    public ApiResponse<VentanillaReportSummaryResponse> summary(
            @ModelAttribute ReportDateRangeRequest request
    ) {
        return ApiResponse.ok(reportService.summary(request));
    }

    @PreAuthorize(AppRolePreAuthorize.REPORT_READ)
    @GetMapping("/ventanilla/by-status")
    public ApiResponse<List<ReportGroupResponse>> byStatus(
            @ModelAttribute ReportDateRangeRequest request
    ) {
        return ApiResponse.ok(reportService.byStatus(request));
    }

    @PreAuthorize(AppRolePreAuthorize.REPORT_READ)
    @GetMapping("/ventanilla/by-request-type")
    public ApiResponse<List<ReportGroupResponse>> byRequestType(
            @ModelAttribute ReportDateRangeRequest request
    ) {
        return ApiResponse.ok(reportService.byRequestType(request));
    }

    @PreAuthorize(AppRolePreAuthorize.REPORT_READ)
    @GetMapping("/ventanilla/by-category")
    public ApiResponse<List<ReportGroupResponse>> byCategory(
            @ModelAttribute ReportDateRangeRequest request
    ) {
        return ApiResponse.ok(reportService.byCategory(request));
    }

    @PreAuthorize(AppRolePreAuthorize.REPORT_READ)
    @GetMapping("/ventanilla/by-user")
    public ApiResponse<List<ReportGroupResponse>> byUser(
            @ModelAttribute ReportDateRangeRequest request
    ) {
        return ApiResponse.ok(reportService.byUser(request));
    }

    @PreAuthorize(AppRolePreAuthorize.REPORT_READ)
    @GetMapping("/ventanilla/by-neighborhood")
    public ApiResponse<List<ReportGroupResponse>> byNeighborhood(
            @ModelAttribute ReportDateRangeRequest request
    ) {
        return ApiResponse.ok(reportService.byNeighborhood(request));
    }

    @PreAuthorize(AppRolePreAuthorize.REPORT_READ)
    @GetMapping("/ventanilla/by-comuna")
    public ApiResponse<List<ReportGroupResponse>> byComuna(
            @ModelAttribute ReportDateRangeRequest request
    ) {
        return ApiResponse.ok(reportService.byComuna(request));
    }

    @PreAuthorize(AppRolePreAuthorize.REPORT_READ)
    @GetMapping("/ventanilla/solicitudes/preview")
    public ApiResponse<VentanillaSolicitudPreviewResponse> previewVentanillaSolicitudes(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin
    ) {
        return ApiResponse.ok(previewService.preview(fechaInicio, fechaFin));
    }

    @PreAuthorize(AppRolePreAuthorize.REPORT_READ)
    @GetMapping("/ventanilla/solicitudes/pdf")
    public ResponseEntity<byte[]> generateVentanillaSolicitudesPdf(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin
    ) {
        byte[] pdf = pdfService.generate(fechaInicio, fechaFin);

        String filename = "Informe-solicitudes-"
                + fechaInicio.format(FILE_DATE_FORMAT)
                + "-a-"
                + fechaFin.format(FILE_DATE_FORMAT)
                + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename)
                                .build()
                                .toString()
                )
                .body(pdf);
    }

    @PreAuthorize(AppRolePreAuthorize.REPORT_READ)
    @GetMapping("/ventanilla/solicitudes/tendencia")
    public ApiResponse<List<VentanillaDailyTrendResponse>> getVentanillaSolicitudesTrend(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin
    ) {
        return ApiResponse.ok(
                reportService.solicitudesTrend(new ReportDateRangeRequest(fechaInicio, fechaFin))
        );
    }

    @PreAuthorize(AppRolePreAuthorize.REPORT_READ)
    @GetMapping("/ventanilla/funcionarios/desempeno")
    public ApiResponse<List<VentanillaFuncionarioPerformanceResponse>> getFuncionariosPerformance(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin
    ) {
        return ApiResponse.ok(
                reportService.funcionariosPerformance(new ReportDateRangeRequest(fechaInicio, fechaFin))
        );
    }

    @PreAuthorize(AppRolePreAuthorize.REPORT_READ)
    @GetMapping("/ventanilla/funcionarios/tendencia")
    public ApiResponse<List<VentanillaFuncionarioTrendResponse>> getFuncionariosTrend(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin
    ) {
        return ApiResponse.ok(
                reportService.funcionariosTrend(new ReportDateRangeRequest(fechaInicio, fechaFin))
        );
    }
}
