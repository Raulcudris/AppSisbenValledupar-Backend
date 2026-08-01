package com.appsisben.backend.modules.callcenter.application;

import com.appsisben.backend.modules.callcenter.dto.CallCenterAsignarFuncionarioRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterResponse;
import com.appsisben.backend.modules.callcenter.dto.CallCenterUltimaHoraRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterUltimaHoraResponse;
import com.appsisben.backend.modules.callcenter.dto.CallCenterVisitaAsignacionRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterVisitaResponse;
import com.appsisben.backend.modules.callcenter.repository.CallCenterRegistroRepository;
import com.appsisben.backend.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de orquestación para las operaciones administrativas
 * de una jornada de encuestas.
 */
@Service
@RequiredArgsConstructor
public class CallCenterJornadaService {

    private static final String TIPO_REGISTRO =
            "BASE_ENCUESTADOR";

    private static final String TIPO_SOLICITUD =
            "NUEVA_ENCUESTA";

    private static final String ORIGEN_MANUAL =
            "MANUAL";

    private static final String ORIGEN_VENTANILLA =
            "VENTANILLA";

    private final CallCenterRegistroRepository
            callCenterRegistroRepository;

    private final CallCenterService
            callCenterService;

    private final CallCenterWorkflowService
            callCenterWorkflowService;

    /**
     * Incorpora un ciudadano de última hora.
     *
     * <p>La operación se ejecuta dentro de una sola transacción:</p>
     *
     * <ol>
     *     <li>Valida que no exista una encuesta activa pendiente.</li>
     *     <li>Crea el caso maestro.</li>
     *     <li>Asigna el funcionario Call Center.</li>
     *     <li>Crea y programa la visita del encuestador.</li>
     * </ol>
     *
     * @param request información del ciudadano y de la asignación.
     * @return caso y visita creados.
     */
    @Transactional
    public CallCenterUltimaHoraResponse crearUltimaHora(
            CallCenterUltimaHoraRequest request
    ) {
        validateRequiredRequest(request);

        String cedula =
                cleanRequired(
                        request.cedulaSolicitante(),
                        "La cédula del solicitante es obligatoria"
                );

        validateNoPendingSurvey(
                request.ventanillaRegistroId(),
                cedula
        );

        CallCenterRequest createRequest =
                buildCreateRequest(
                        request,
                        cedula
                );

        CallCenterResponse created =
                callCenterService.create(
                        createRequest
                );

        if (
                created == null
                        || created.id() == null
        ) {
            throw new BusinessException(
                    "No fue posible obtener el identificador "
                            + "del caso Call Center creado"
            );
        }

        Long registroId =
                created.id();

        assignFuncionario(
                registroId,
                request.funcionarioCallcenterId()
        );

        CallCenterVisitaResponse visita =
                assignVisita(
                        registroId,
                        request
                );

        CallCenterResponse finalRegistro =
                callCenterService.findById(
                        registroId
                );

        return new CallCenterUltimaHoraResponse(
                finalRegistro,
                visita
        );
    }

    private void validateRequiredRequest(
            CallCenterUltimaHoraRequest request
    ) {
        if (request == null) {
            throw new BusinessException(
                    "La solicitud de ciudadano de última hora "
                            + "es obligatoria"
            );
        }

        if (request.fechaCaso() == null) {
            throw new BusinessException(
                    "La fecha del caso es obligatoria"
            );
        }

        cleanRequired(
                request.nombreCompleto(),
                "El nombre completo es obligatorio"
        );

        cleanRequired(
                request.direccionTexto(),
                "La dirección es obligatoria"
        );

        if (
                request.funcionarioCallcenterId()
                        == null
        ) {
            throw new BusinessException(
                    "Debe seleccionar el funcionario Call Center"
            );
        }

        if (request.encuestadorId() == null) {
            throw new BusinessException(
                    "Debe seleccionar el encuestador"
            );
        }

        if (request.fechaProgramada() == null) {
            throw new BusinessException(
                    "La fecha programada de la visita "
                            + "es obligatoria"
            );
        }
    }

    private void validateNoPendingSurvey(
            Long ventanillaRegistroId,
            String cedula
    ) {
        boolean exists =
                callCenterRegistroRepository
                        .existsNuevaEncuestaActivaNoRealizada(
                                ventanillaRegistroId,
                                cedula
                        );

        if (exists) {
            throw new BusinessException(
                    "El ciudadano ya tiene una nueva encuesta "
                            + "activa y pendiente de realización"
            );
        }
    }

    private CallCenterRequest buildCreateRequest(
            CallCenterUltimaHoraRequest request,
            String cedula
    ) {
        String origenRegistro =
                request.ventanillaRegistroId()
                        != null
                        ? ORIGEN_VENTANILLA
                        : ORIGEN_MANUAL;

        return new CallCenterRequest(
                LocalDateTime.now(),
                request.fechaCaso(),
                null,
                TIPO_REGISTRO,
                origenRegistro,
                request.ventanillaRegistroId(),
                cedula,
                cleanRequired(
                        request.nombreCompleto(),
                        "El nombre completo es obligatorio"
                ),
                clean(request.telefono()),
                null,
                null,
                null,
                null,
                null,
                true,
                cleanRequired(
                        request.direccionTexto(),
                        "La dirección es obligatoria"
                ),
                request.barrioId(),
                null,
                null,
                null,
                null,
                null,
                true,
                false,
                null,
                TIPO_SOLICITUD,
                clean(request.observacion()),
                true
        );
    }

    private void assignFuncionario(
            Long registroId,
            Long funcionarioCallcenterId
    ) {
        CallCenterAsignarFuncionarioRequest
                assignmentRequest =
                new CallCenterAsignarFuncionarioRequest(
                        funcionarioCallcenterId,
                        List.of(registroId)
                );

        callCenterService
                .asignarFuncionarioCallcenter(
                        assignmentRequest
                );
    }

    private CallCenterVisitaResponse assignVisita(
            Long registroId,
            CallCenterUltimaHoraRequest request
    ) {
        CallCenterVisitaAsignacionRequest
                visitaRequest =
                new CallCenterVisitaAsignacionRequest(
                        request.encuestadorId(),
                        request.fechaProgramada(),
                        request.horaProgramada(),
                        clean(request.observacion())
                );

        return callCenterWorkflowService
                .asignarVisita(
                        registroId,
                        visitaRequest
                );
    }

    private String cleanRequired(
            String value,
            String message
    ) {
        String cleaned =
                clean(value);

        if (cleaned == null) {
            throw new BusinessException(
                    message
            );
        }

        return cleaned;
    }

    private String clean(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String cleaned =
                value.trim();

        return cleaned.isEmpty()
                ? null
                : cleaned;
    }
}