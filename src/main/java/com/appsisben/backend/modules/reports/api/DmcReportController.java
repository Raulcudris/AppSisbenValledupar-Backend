package com.appsisben.backend.modules.reports.api;

import com.appsisben.backend.modules.reports.application.DmcReportService;
import com.appsisben.backend.modules.reports.dto.DmcReportSummaryResponse;
import com.appsisben.backend.modules.reports.dto.ReportDateRangeRequest;
import com.appsisben.backend.modules.reports.dto.ReportGroupResponse;
import com.appsisben.backend.security.AppRolePreAuthorize;
import com.appsisben.backend.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports/dmc")
@PreAuthorize(AppRolePreAuthorize.REPORT_READ)
public class DmcReportController {

    private final DmcReportService service;

    @GetMapping("/summary")
    public ApiResponse<DmcReportSummaryResponse> summary(@ModelAttribute ReportDateRangeRequest request) {
        return ApiResponse.ok(service.summary(request));
    }

    @GetMapping("/by-type")
    public ApiResponse<List<ReportGroupResponse>> byType(@ModelAttribute ReportDateRangeRequest request) {
        return ApiResponse.ok(service.byType(request));
    }

    @GetMapping("/by-surveyor")
    public ApiResponse<List<ReportGroupResponse>> bySurveyor(@ModelAttribute ReportDateRangeRequest request) {
        return ApiResponse.ok(service.bySurveyor(request));
    }

    @GetMapping("/by-user")
    public ApiResponse<List<ReportGroupResponse>> byUser(@ModelAttribute ReportDateRangeRequest request) {
        return ApiResponse.ok(service.byUser(request));
    }

    @GetMapping("/by-neighborhood")
    public ApiResponse<List<ReportGroupResponse>> byNeighborhood(@ModelAttribute ReportDateRangeRequest request) {
        return ApiResponse.ok(service.byNeighborhood(request));
    }

    @GetMapping("/by-comuna")
    public ApiResponse<List<ReportGroupResponse>> byComuna(@ModelAttribute ReportDateRangeRequest request) {
        return ApiResponse.ok(service.byComuna(request));
    }
}
