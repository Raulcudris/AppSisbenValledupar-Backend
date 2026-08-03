package com.appsisben.backend.modules.callcenter.application;

import com.appsisben.backend.modules.callcenter.dto.CallCenterGestionLlamadaRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterGestionLlamadaResponse;
import com.appsisben.backend.modules.callcenter.dto.CallCenterRegistroCompletoRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterRegistroCompletoResponse;
import com.appsisben.backend.modules.callcenter.dto.CallCenterRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterResponse;
import com.appsisben.backend.modules.callcenter.dto.CallCenterVisitaAsignacionRequest;
import com.appsisben.backend.modules.callcenter.dto.CallCenterVisitaResponse;
import com.appsisben.backend.modules.callcenter.repository.CallCenterRegistroRepository;
import com.appsisben.backend.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Set;

/**
 * Orquesta el registro completo de un caso por parte
 * del usuario autenticado autorizado.
 *
 * <p>Todos los pasos participan en una única transacción:</p>
 *
 * <ol>
 *     <li>Crea y asigna el caso maestro.</li>
 *     <li>Registra la primera llamada o intento.</li>
 *     <li>Asigna y programa la visita.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class CallCenterRegistroCompletoService {

    private static final String TIPO_REGISTRO =
            "LLAMADA";

    private static final String TIPO_NUEVA_ENCUESTA =
            "NUEVA_ENCUESTA";

    private static final Set<String> TIPOS_SOLICITUD =
            Set.of(
                    "NUEVA_ENCUESTA",
                    "INCLUSION",
                    "VERIFICACION",
                    "OTRO"
            );

    private final CallCenterService
            callCenterService;

    private final CallCenterWorkflowService
            callCenterWorkflowService;

    private final CallCenterRegistroRepository
            callCenterRegistroRepository;

    /**
     * Registra el caso, la llamada y la visita dentro
     * de una única transacción.
     *
     * @param request datos completos del registro.
     * @return caso, llamada y visita creados.
     */
    @Transactional
    public CallCenterRegistroCompletoResponse crear(
            CallCenterRegistroCompletoRequest request
    ) {
        validateRequest(request);

        String tipoSolicitud =
                normalizeRequired(
                        request
                                .registro()
                                .tipoSolicitudCallcenter(),
                        "El tipo de solicitud es obligatorio"
                );

        validateTipoSolicitud(
                tipoSolicitud
        );

        String cedula =
                cleanRequired(
                        request
                                .registro()
                                .cedulaSolicitante(),
                        "La cédula del solicitante es obligatoria"
                );

        validateNoPendingSurvey(
                tipoSolicitud,
                request
                        .registro()
                        .ventanillaRegistroId(),
                cedula
        );

        CallCenterRequest createRequest =
                buildCreateRequest(
                        request,
                        tipoSolicitud,
                        cedula
                );

        /*
         * Este método asigna el caso al usuario autenticado,
         * tanto para ADMIN como para FUNCIONARIO_CALLCENTER.
         */
        CallCenterResponse created =
                callCenterService
                        .createSelfRegistered(
                                createRequest
                        );

        validateCreatedRecord(
                created
        );

        Long registroId =
                created.id();

        CallCenterGestionLlamadaResponse llamada =
                callCenterWorkflowService
                        .registrarLlamada(
                                registroId,
                                request.llamada()
                        );

        /*
         * El formulario completo siempre programa una visita.
         * Un resultado que cierre o cancele el caso no puede
         * coexistir con la programación obligatoria.
         */
        CallCenterResponse afterCall =
                callCenterService.findById(
                        registroId
                );

        if (
                CallCenterStatePolicy
                        .isFinalState(
                                afterCall.estadoCaso()
                        )
        ) {
            throw new BusinessException(
                    "El resultado de llamada seleccionado "
                            + "cerró o canceló el caso y no permite "
                            + "programar la visita"
            );
        }

        CallCenterVisitaResponse visita =
                callCenterWorkflowService
                        .asignarVisita(
                                registroId,
                                request.visita()
                        );

        CallCenterResponse finalRegistro =
                callCenterService.findById(
                        registroId
                );

        return new CallCenterRegistroCompletoResponse(
                finalRegistro,
                llamada,
                visita
        );
    }

    /**
     * Construye el request saneado del caso maestro.
     *
     * Los datos operativos de llamada y visita se guardan
     * mediante sus servicios formales.
     */
    private CallCenterRequest buildCreateRequest(
            CallCenterRegistroCompletoRequest completeRequest,
            String tipoSolicitud,
            String cedula
    ) {
        CallCenterRequest registro =
                completeRequest.registro();

        CallCenterGestionLlamadaRequest llamada =
                completeRequest.llamada();

        LocalDate fechaCaso =
                llamada.fechaLlamada() != null
                        ? llamada.fechaLlamada()
                        : registro.fechaLlamada();

        LocalTime horaCaso =
                llamada.horaLlamada();

        boolean nuevaEncuesta =
                TIPO_NUEVA_ENCUESTA.equals(
                        tipoSolicitud
                );

        return new CallCenterRequest(
                LocalDateTime.now(),
                fechaCaso,
                horaCaso,
                TIPO_REGISTRO,
                registro.origenRegistro(),
                registro.ventanillaRegistroId(),
                cedula,
                cleanRequired(
                        registro.nombreCompleto(),
                        "El nombre completo es obligatorio"
                ),
                clean(registro.telefono()),

                /*
                 * Llamada: se registra posteriormente mediante
                 * CallCenterWorkflowService.registrarLlamada.
                 */
                null,
                null,
                null,

                /*
                 * Encuestador y programación: se registran
                 * posteriormente mediante asignarVisita.
                 */
                null,
                null,

                nuevaEncuesta,

                cleanRequired(
                        registro.direccionTexto(),
                        "La dirección es obligatoria"
                ),

                registro.barrioId(),

                /*
                 * Información confirmada durante la llamada.
                 */
                null,
                null,
                null,
                null,
                null,
                null,
                null,

                /*
                 * El backend determina el estado formal.
                 */
                null,

                tipoSolicitud,

                clean(registro.observacion()),

                true
        );
    }

    private void validateRequest(
            CallCenterRegistroCompletoRequest request
    ) {
        if (request == null) {
            throw new BusinessException(
                    "La solicitud de registro completo "
                            + "es obligatoria"
            );
        }

        if (request.registro() == null) {
            throw new BusinessException(
                    "Los datos del caso son obligatorios"
            );
        }

        if (request.llamada() == null) {
            throw new BusinessException(
                    "Los datos de la llamada son obligatorios"
            );
        }

        if (request.visita() == null) {
            throw new BusinessException(
                    "Los datos de la visita son obligatorios"
            );
        }

        if (request.registro().fechaLlamada() == null) {
            throw new BusinessException(
                    "La fecha del caso es obligatoria"
            );
        }

        validateVisita(
                request.visita()
        );
    }

    private void validateVisita(
            CallCenterVisitaAsignacionRequest visita
    ) {
        if (visita.encuestadorId() == null) {
            throw new BusinessException(
                    "Debe seleccionar el encuestador"
            );
        }

        if (visita.fechaProgramada() == null) {
            throw new BusinessException(
                    "La fecha programada de la visita "
                            + "es obligatoria"
            );
        }

        if (visita.horaProgramada() == null) {
            throw new BusinessException(
                    "La hora programada de la visita "
                            + "es obligatoria"
            );
        }
    }

    private void validateTipoSolicitud(
            String tipoSolicitud
    ) {
        if (
                !TIPOS_SOLICITUD.contains(
                        tipoSolicitud
                )
        ) {
            throw new BusinessException(
                    "Tipo de solicitud Call Center no válido"
            );
        }
    }

    /**
     * Aplica la regla existente de duplicidad para
     * solicitudes de nueva encuesta.
     */
    private void validateNoPendingSurvey(
            String tipoSolicitud,
            Long ventanillaRegistroId,
            String cedula
    ) {
        if (
                !TIPO_NUEVA_ENCUESTA.equals(
                        tipoSolicitud
                )
        ) {
            return;
        }

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

    /**
     * Verifica que el caso haya quedado asignado al usuario
     * autenticado, sea ADMIN o FUNCIONARIO_CALLCENTER.
     */
    private void validateCreatedRecord(
            CallCenterResponse created
    ) {
        if (
                created == null
                        || created.id() == null
        ) {
            throw new BusinessException(
                    "No fue posible obtener el identificador "
                            + "del caso creado"
            );
        }

        if (
                created
                        .funcionarioCallcenterAsignadoId()
                        == null
        ) {
            throw new BusinessException(
                    "El caso no quedó asignado al usuario "
                            + "autenticado"
            );
        }

        if (
                !CallCenterStatePolicy
                        .ASIGNADO_CALLCENTER
                        .equalsIgnoreCase(
                                created.estadoCaso()
                        )
        ) {
            throw new BusinessException(
                    "El caso no quedó en estado "
                            + "ASIGNADO_CALLCENTER"
            );
        }
    }

    private String normalizeRequired(
            String value,
            String message
    ) {
        String normalized =
                value == null
                        ? null
                        : value
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (
                normalized == null
                        || normalized.isBlank()
        ) {
            throw new BusinessException(
                    message
            );
        }

        return normalized;
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