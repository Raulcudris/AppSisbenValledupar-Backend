package com.appsisben.backend.modules.callcenter.api;

import com.appsisben.backend.modules.callcenter.application.CallCenterAgendaVisitasService;
import com.appsisben.backend.modules.callcenter.dto.CallCenterAgendaVisitaResponse;
import com.appsisben.backend.security.AppRolePreAuthorize;
import com.appsisben.backend.shared.api.ApiResponse;
import com.appsisben.backend.shared.api.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Consulta administrativa de agendas de encuestadores.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/callcenter/visitas/agenda")
public class CallCenterAgendaVisitasController {

    private final CallCenterAgendaVisitasService service;

    /**
     * Consulta las personas programadas para visitar
     * por encuestador y fecha.
     *
     * @param encuestadorId identificador del encuestador.
     * @param fecha fecha de la agenda.
     * @param page número de página.
     * @param size tamaño de página.
     * @return página con la agenda del encuestador.
     */
    @PreAuthorize(
            AppRolePreAuthorize.CALLCENTER_AGENDA_VISITAS_READ
    )
    @GetMapping
    public ApiResponse<PageResponse<CallCenterAgendaVisitaResponse>>
    consultar(
            @RequestParam
            Long encuestadorId,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fecha,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.asc("horaProgramada"),
                                Sort.Order.asc("id")
                        )
                );

        return ApiResponse.ok(
                service.consultar(
                        encuestadorId,
                        fecha,
                        pageable
                )
        );
    }
}