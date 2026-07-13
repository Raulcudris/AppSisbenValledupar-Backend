package com.appsisben.backend.modules.reports.api;

import com.appsisben.backend.modules.reports.application.VentanillaSolicitudPdfReportService;
import com.appsisben.backend.modules.reports.application.VentanillaSolicitudPreviewService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reportes")
public class VentanillaReportController {

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final VentanillaSolicitudPdfReportService pdfService;
    private final VentanillaSolicitudPreviewService previewService;

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
}