package com.appsisben.backend.modules.audit.api;

import com.appsisben.backend.modules.audit.application.AuditService;
import com.appsisben.backend.modules.audit.dto.AuditFilterRequest;
import com.appsisben.backend.modules.audit.dto.AuditLogResponse;
import com.appsisben.backend.security.AppRolePreAuthorize;
import com.appsisben.backend.shared.api.ApiResponse;
import com.appsisben.backend.shared.api.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/audit")
@PreAuthorize(AppRolePreAuthorize.AUDIT_READ)
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ApiResponse<PageResponse<AuditLogResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fechaAccion").descending());
        return ApiResponse.ok(auditService.search(null, pageable));
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<AuditLogResponse>> search(
            @ModelAttribute AuditFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fechaAccion").descending());
        return ApiResponse.ok(auditService.search(filter, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<AuditLogResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(auditService.findById(id));
    }
}
