package com.appsisben.backend.modules.callcenter.application;

import com.appsisben.backend.modules.callcenter.domain.CallCenterRegistro;
import com.appsisben.backend.modules.callcenter.domain.CallCenterVisita;
import com.appsisben.backend.modules.callcenter.dto.CallCenterAgendaVisitaResponse;
import com.appsisben.backend.modules.callcenter.repository.CallCenterVisitaRepository;
import com.appsisben.backend.modules.catalogs.domain.Encuestador;
import com.appsisben.backend.modules.catalogs.repository.EncuestadorRepository;
import com.appsisben.backend.shared.api.PageResponse;
import com.appsisben.backend.shared.exception.BusinessException;
import com.appsisben.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Servicio de consulta administrativa de la agenda
 * diaria de los encuestadores.
 */
@Service
@RequiredArgsConstructor
public class CallCenterAgendaVisitasService {

    private final CallCenterVisitaRepository visitaRepository;

    private final EncuestadorRepository encuestadorRepository;

    /**
     * Consulta las personas que un encuestador tiene
     * pendientes por visitar en una fecha determinada.
     *
     * @param encuestadorId identificador del encuestador.
     * @param fecha fecha de la agenda.
     * @param pageable configuración de paginación.
     * @return página de personas programadas.
     */
    @Transactional(readOnly = true)
    public PageResponse<CallCenterAgendaVisitaResponse>
    consultar(
            Long encuestadorId,
            LocalDate fecha,
            Pageable pageable
    ) {
        if (encuestadorId == null) {
            throw new BusinessException(
                    "Debe seleccionar un encuestador"
            );
        }

        if (fecha == null) {
            throw new BusinessException(
                    "Debe seleccionar la fecha de la visita"
            );
        }

        Encuestador encuestador =
                encuestadorRepository
                        .findById(encuestadorId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Encuestador no encontrado"
                                        )
                        );

        if (
                !Boolean.TRUE.equals(
                        encuestador.getActivo()
                )
        ) {
            throw new BusinessException(
                    "El encuestador seleccionado está inactivo"
            );
        }

        Page<CallCenterVisita> page =
                visitaRepository
                        .findAgendaByEncuestadorAndFecha(
                                encuestadorId,
                                fecha,
                                pageable
                        );

        return PageResponse.from(
                page,
                page.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    /**
     * Convierte una visita en la respuesta reducida
     * utilizada por la agenda administrativa.
     */
    private CallCenterAgendaVisitaResponse
    toResponse(
            CallCenterVisita visita
    ) {
        CallCenterRegistro registro =
                visita.getCallCenterRegistro();

        Encuestador encuestador =
                visita.getEncuestador();

        LocalDate fechaAgenda =
                "REPROGRAMADA".equalsIgnoreCase(
                        visita.getEstadoVisita()
                )
                        && visita.getFechaReprogramacion() != null
                        ? visita.getFechaReprogramacion()
                        : visita.getFechaProgramada();

        String barrioNombre =
                registro != null
                        && registro.getBarrio() != null
                        ? registro.getBarrio().getNombre()
                        : null;

        return new CallCenterAgendaVisitaResponse(
                visita.getId(),

                encuestador != null
                        ? encuestador.getId()
                        : null,

                encuestador != null
                        ? encuestador.getNombre()
                        : null,

                fechaAgenda,

                visita.getHoraProgramada(),

                registro != null
                        ? registro.getCedulaSolicitante()
                        : null,

                registro != null
                        ? registro.getNombreCompleto()
                        : null,

                registro != null
                        ? registro.getDireccionTexto()
                        : null,

                barrioNombre,

                registro != null
                        ? registro.getTelefono()
                        : null
        );
    }
}