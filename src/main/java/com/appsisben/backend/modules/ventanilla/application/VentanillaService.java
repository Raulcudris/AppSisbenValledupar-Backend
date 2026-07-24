package com.appsisben.backend.modules.ventanilla.application;

import com.appsisben.backend.modules.audit.application.AuditService;
import com.appsisben.backend.modules.audit.domain.AuditAction;
import com.appsisben.backend.modules.catalogs.domain.Categoria;
import com.appsisben.backend.modules.catalogs.domain.EstadoSolicitud;
import com.appsisben.backend.modules.catalogs.domain.Solicitud;
import com.appsisben.backend.modules.catalogs.repository.CategoriaRepository;
import com.appsisben.backend.modules.catalogs.repository.EstadoSolicitudRepository;
import com.appsisben.backend.modules.catalogs.repository.SolicitudRepository;
import com.appsisben.backend.modules.territory.domain.Barrio;
import com.appsisben.backend.modules.territory.repository.BarrioRepository;
import com.appsisben.backend.modules.users.domain.User;
import com.appsisben.backend.modules.users.repository.UserRepository;
import com.appsisben.backend.modules.ventanilla.domain.VentanillaRegistro;
import com.appsisben.backend.modules.ventanilla.dto.VentanillaCitizenHistoryResponse;
import com.appsisben.backend.modules.ventanilla.dto.VentanillaDailyRequestItemResponse;
import com.appsisben.backend.modules.ventanilla.dto.VentanillaDailyValidationResponse;
import com.appsisben.backend.modules.ventanilla.dto.VentanillaFilterRequest;
import com.appsisben.backend.modules.ventanilla.dto.VentanillaRequest;
import com.appsisben.backend.modules.ventanilla.dto.VentanillaResponse;
import com.appsisben.backend.modules.ventanilla.dto.VentanillaTraceabilityBadgeResponse;
import com.appsisben.backend.modules.ventanilla.dto.VentanillaUserHistoryItemResponse;
import com.appsisben.backend.modules.ventanilla.dto.VentanillaUserHistoryResponse;
import com.appsisben.backend.modules.ventanilla.dto.VentanillaUserHistorySummaryResponse;
import com.appsisben.backend.modules.ventanilla.repository.VentanillaRegistroRepository;
import com.appsisben.backend.modules.ventanilla.repository.VentanillaSpecification;
import com.appsisben.backend.shared.api.PageResponse;
import com.appsisben.backend.shared.exception.BusinessException;
import com.appsisben.backend.shared.exception.ResourceNotFoundException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VentanillaService {

    private static final String TABLE_NAME = "ventanilla_registro";
    private static final Logger log = LoggerFactory.getLogger(VentanillaService.class);
    private static final DateTimeFormatter PDF_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final VentanillaRegistroRepository repository;
    private final UserRepository userRepository;
    private final CategoriaRepository categoriaRepository;
    private final SolicitudRepository solicitudRepository;
    private final EstadoSolicitudRepository estadoSolicitudRepository;
    private final BarrioRepository barrioRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<VentanillaResponse> findAll(Pageable pageable) {
        Page<VentanillaRegistro> page = isAdmin()
                ? repository.findAll(pageable)
                : repository.findAll(VentanillaSpecification.activeOnly(), pageable);

        List<VentanillaResponse> content = page.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public PageResponse<VentanillaResponse> search(VentanillaFilterRequest filter, Pageable pageable) {
        boolean allowInactiveRecords = isAdmin()
                && filter != null
                && Boolean.TRUE.equals(filter.incluirInactivos());

        Page<VentanillaRegistro> page = repository.findAll(
                VentanillaSpecification.byFilter(filter, allowInactiveRecords),
                pageable
        );

        List<VentanillaResponse> content = page.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public VentanillaResponse findById(Long id) {
        VentanillaRegistro entity = isAdmin() ? findEntity(id) : findActiveEntity(id);

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public PageResponse<VentanillaUserHistorySummaryResponse> findUserHistorySummaries(
            String search,
            Pageable pageable
    ) {
        String normalizedSearch = isBlank(search) ? null : search.trim();

        Page<VentanillaUserHistorySummaryResponse> page = repository.findUserHistorySummaries(
                normalizedSearch,
                pageable
        );

        return PageResponse.from(page, page.getContent());
    }

    @Transactional(readOnly = true)
    public VentanillaUserHistoryResponse findUserHistory(String cedulaUsuario) {
        if (isBlank(cedulaUsuario)) {
            throw new BusinessException("La cédula del ciudadano es obligatoria");
        }

        String cedula = cedulaUsuario.trim();

        List<VentanillaRegistro> records = repository.findTraceabilityByCedula(cedula);

        if (records.isEmpty()) {
            return new VentanillaUserHistoryResponse(
                    cedula,
                    null,
                    null,
                    0L,
                    0L,
                    null,
                    null,
                    List.of()
            );
        }

        List<VentanillaRegistro> orderedRecords = records
                .stream()
                .sorted(
                        Comparator.comparing(VentanillaRegistro::getFecha)
                                .reversed()
                                .thenComparing(VentanillaRegistro::getId, Comparator.reverseOrder())
                )
                .toList();

        VentanillaRegistro latest = orderedRecords.get(0);

        Long totalVisitas = orderedRecords
                .stream()
                .map(VentanillaRegistro::getFecha)
                .distinct()
                .count();

        Long totalSolicitudes = (long) orderedRecords.size();

        LocalDate primeraVisita = orderedRecords
                .stream()
                .map(VentanillaRegistro::getFecha)
                .min(LocalDate::compareTo)
                .orElse(null);

        LocalDate ultimaVisita = orderedRecords
                .stream()
                .map(VentanillaRegistro::getFecha)
                .max(LocalDate::compareTo)
                .orElse(null);

        List<VentanillaUserHistoryItemResponse> solicitudes = orderedRecords
                .stream()
                .map(this::toUserHistoryItem)
                .toList();

        return new VentanillaUserHistoryResponse(
                cedula,
                latest.getNombreUsuario(),
                latest.getTelefono(),
                totalVisitas,
                totalSolicitudes,
                primeraVisita,
                ultimaVisita,
                solicitudes
        );
    }

    @Transactional(readOnly = true)
    public byte[] exportUserHistory(String cedulaUsuario) {
        VentanillaUserHistoryResponse history = findUserHistory(cedulaUsuario);

        StringBuilder csv = new StringBuilder();

        csv.append('\uFEFF');
        csv.append("Fecha,N° Ventanilla,Cédula,Nombre usuario,Teléfono,Solicitud,Categoría,Estado solicitud,Barrio,Comuna,Funcionario,Extranjero,Estado registro,Motivo repetición,Observación\n");

        for (VentanillaUserHistoryItemResponse item : history.solicitudes()) {
            csv.append(csvValue(item.fecha()))
                    .append(',')
                    .append(csvValue(item.numeroVentanilla()))
                    .append(',')
                    .append(csvValue(item.cedulaUsuario()))
                    .append(',')
                    .append(csvValue(item.nombreUsuario()))
                    .append(',')
                    .append(csvValue(item.telefono()))
                    .append(',')
                    .append(csvValue(item.solicitudNombre()))
                    .append(',')
                    .append(csvValue(item.categoriaNombre()))
                    .append(',')
                    .append(csvValue(item.estadoSolicitudNombre()))
                    .append(',')
                    .append(csvValue(item.barrioNombre()))
                    .append(',')
                    .append(csvValue(item.comunaNombre()))
                    .append(',')
                    .append(csvValue(item.funcionarioUsername()))
                    .append(',')
                    .append(csvValue(Boolean.TRUE.equals(item.extranjero()) ? "Sí" : "No"))
                    .append(',')
                    .append(csvValue(Boolean.TRUE.equals(item.activo()) ? "Activo" : "Inactivo"))
                    .append(',')
                    .append(csvValue(item.motivoRepeticion()))
                    .append(',')
                    .append(csvValue(item.observacion()))
                    .append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportUserHistoryPdf(String cedulaUsuario) {
        VentanillaUserHistoryResponse history = findUserHistory(cedulaUsuario);

        Document document = null;

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            document = new Document(PageSize.A4.rotate(), 28, 28, 26, 28);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            addUserHistoryPdfHeader(document, history);
            addUserHistoryPdfSummary(document, history);
            addUserHistoryPdfTable(document, history);
            addUserHistoryPdfFooter(document);

            document.close();

            return outputStream.toByteArray();
        } catch (Exception ex) {
            log.error("Error generando PDF de historial de usuario. Cedula={}", cedulaUsuario, ex);

            if (document != null && document.isOpen()) {
                document.close();
            }

            throw new BusinessException(
                    "No fue posible generar el PDF del historial del usuario. Detalle técnico: "
                            + ex.getMessage()
            );
        }
    }

    @Transactional(readOnly = true)
    public VentanillaDailyValidationResponse validateBeforeSave(
            Long currentId,
            LocalDate fecha,
            String cedulaUsuario,
            Long solicitudId
    ) {
        if (fecha == null) {
            throw new BusinessException("La fecha es obligatoria");
        }

        if (isBlank(cedulaUsuario)) {
            throw new BusinessException("La cédula del ciudadano es obligatoria");
        }

        if (solicitudId == null) {
            throw new BusinessException("La solicitud es obligatoria");
        }

        String cedula = cedulaUsuario.trim();

        List<VentanillaRegistro> dailyRequests = currentId == null
                ? repository.findDailyRequestsByCedula(fecha, cedula)
                : repository.findDailyRequestsByCedulaAndIdNot(fecha, cedula, currentId);

        List<VentanillaDailyRequestItemResponse> items = dailyRequests
                .stream()
                .map(this::toDailyRequestItem)
                .toList();

        boolean duplicated = dailyRequests
                .stream()
                .anyMatch(item -> item.getSolicitud() != null
                        && item.getSolicitud().getId() != null
                        && item.getSolicitud().getId().equals(solicitudId));

        if (duplicated) {
            return new VentanillaDailyValidationResponse(
                    "SOLICITUD_DUPLICADA_MISMA_FECHA",
                    "Solicitud repetida el mismo día",
                    "Este ciudadano ya tiene registrada esta misma solicitud para la fecha seleccionada. Para guardarla nuevamente debes registrar el motivo.",
                    true,
                    true,
                    (long) dailyRequests.size(),
                    items
            );
        }

        if (!dailyRequests.isEmpty()) {
            return new VentanillaDailyValidationResponse(
                    "SOLICITUD_DIFERENTE_MISMA_FECHA",
                    "El ciudadano ya tiene solicitudes registradas hoy",
                    "Este ciudadano ya tiene solicitudes registradas para la fecha seleccionada. Para guardar una nueva solicitud debes registrar el motivo.",
                    true,
                    true,
                    (long) dailyRequests.size(),
                    items
            );
        }

        return new VentanillaDailyValidationResponse(
                "PRIMERA_SOLICITUD",
                "Primera solicitud del día",
                "El ciudadano no tiene solicitudes registradas para la fecha seleccionada. Puede continuar con el registro.",
                true,
                false,
                0L,
                List.of()
        );
    }

    @Transactional(readOnly = true)
    public VentanillaCitizenHistoryResponse findCitizenHistory(String cedulaUsuario) {
        if (isBlank(cedulaUsuario)) {
            throw new BusinessException("La cédula del ciudadano es obligatoria");
        }

        String cedula = cedulaUsuario.trim();

        List<VentanillaRegistro> records = repository.findTraceabilityByCedula(cedula);

        if (records.isEmpty()) {
            return new VentanillaCitizenHistoryResponse(
                    cedula,
                    null,
                    null,
                    0L,
                    0L,
                    null,
                    List.of()
            );
        }

        VentanillaRegistro latest = records.get(0);

        Long totalVisitas = records
                .stream()
                .map(VentanillaRegistro::getFecha)
                .distinct()
                .count();

        List<VentanillaDailyRequestItemResponse> solicitudes = records
                .stream()
                .map(this::toDailyRequestItem)
                .toList();

        return new VentanillaCitizenHistoryResponse(
                cedula,
                latest.getNombreUsuario(),
                latest.getTelefono(),
                totalVisitas,
                (long) records.size(),
                latest.getFecha(),
                solicitudes
        );
    }

    @Transactional
    public VentanillaResponse create(VentanillaRequest request) {
        validateDailySolicitudLimit(null, request);

        VentanillaRegistro entity = new VentanillaRegistro();

        entity.setFuncionario(currentUser());
        entity.setActivo(true);

        apply(entity, request);

        VentanillaRegistro saved = repository.save(entity);

        auditService.safeLog(
                AuditAction.CREATE,
                TABLE_NAME,
                saved.getId(),
                null,
                snapshot(saved)
        );

        return toResponse(saved);
    }

    @Transactional
    public VentanillaResponse update(Long id, VentanillaRequest request) {
        VentanillaRegistro entity = isAdmin() ? findEntity(id) : findActiveEntity(id);
        Map<String, Object> before = snapshot(entity);

        validateDailySolicitudLimit(id, request);

        entity.setEditadoPor(currentUser());
        apply(entity, request);

        auditService.safeLog(
                AuditAction.UPDATE,
                TABLE_NAME,
                entity.getId(),
                before,
                snapshot(entity)
        );

        return toResponse(entity);
    }

    @Transactional
    public void inactivate(Long id) {
        VentanillaRegistro entity = findActiveEntity(id);
        Map<String, Object> before = snapshot(entity);

        entity.setActivo(false);
        entity.setEditadoPor(currentUser());

        auditService.safeLog(
                AuditAction.UPDATE,
                TABLE_NAME,
                entity.getId(),
                before,
                snapshot(entity)
        );
    }

    @Transactional
    public void activate(Long id) {
        ensureAdmin();

        VentanillaRegistro entity = findEntity(id);
        Map<String, Object> before = snapshot(entity);

        entity.setActivo(true);
        entity.setEditadoPor(currentUser());

        auditService.safeLog(
                AuditAction.UPDATE,
                TABLE_NAME,
                entity.getId(),
                before,
                snapshot(entity)
        );
    }

    @Transactional
    public void changeActiveStatus(Long id, Boolean activo) {
        ensureAdmin();

        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        VentanillaRegistro entity = findEntity(id);
        Map<String, Object> before = snapshot(entity);

        entity.setActivo(activo);
        entity.setEditadoPor(currentUser());

        auditService.safeLog(
                AuditAction.UPDATE,
                TABLE_NAME,
                entity.getId(),
                before,
                snapshot(entity)
        );
    }

    @Transactional
    public void delete(Long id) {
        ensureAdmin();

        VentanillaRegistro entity = findEntity(id);
        Map<String, Object> before = snapshot(entity);

        entity.setActivo(false);
        entity.setEditadoPor(currentUser());

        auditService.safeLog(
                AuditAction.UPDATE,
                TABLE_NAME,
                entity.getId(),
                before,
                snapshot(entity)
        );
    }

    private VentanillaRegistro findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de ventanilla no encontrado"));
    }

    private VentanillaRegistro findActiveEntity(Long id) {
        VentanillaRegistro entity = findEntity(id);

        if (!Boolean.TRUE.equals(entity.getActivo())) {
            throw new ResourceNotFoundException("Registro de ventanilla no encontrado");
        }

        return entity;
    }

    private void validateDailySolicitudLimit(Long currentId, VentanillaRequest request) {
        if (request == null) {
            throw new BusinessException("Los datos de la solicitud son obligatorios");
        }

        if (request.fecha() == null) {
            throw new BusinessException("La fecha es obligatoria");
        }

        if (isBlank(request.cedulaUsuario())) {
            throw new BusinessException("La cédula del usuario es obligatoria");
        }

        if (request.solicitudId() == null) {
            throw new BusinessException("La solicitud es obligatoria");
        }

        String cedulaUsuario = request.cedulaUsuario().trim();

        List<VentanillaRegistro> dailyRequests = currentId == null
                ? repository.findDailyRequestsByCedula(request.fecha(), cedulaUsuario)
                : repository.findDailyRequestsByCedulaAndIdNot(request.fecha(), cedulaUsuario, currentId);

        if (!dailyRequests.isEmpty() && isBlank(request.motivoRepeticion())) {
            throw new BusinessException(
                    "Este ciudadano ya tiene solicitudes registradas para la fecha seleccionada. Para guardarla nuevamente debes registrar el motivo."
            );
        }
    }

    private void apply(VentanillaRegistro entity, VentanillaRequest request) {
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

        Solicitud solicitud = solicitudRepository.findById(request.solicitudId())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));

        EstadoSolicitud estado = estadoSolicitudRepository.findById(request.estadoSolicitudId())
                .orElseThrow(() -> new ResourceNotFoundException("Estado de solicitud no encontrado"));

        Barrio barrio = barrioRepository.findById(request.barrioId())
                .orElseThrow(() -> new ResourceNotFoundException("Barrio no encontrado"));

        entity.setFecha(request.fecha());
        entity.setNumeroVentanilla(request.numeroVentanilla().trim());
        entity.setCedulaUsuario(request.cedulaUsuario().trim());
        entity.setNombreUsuario(request.nombreUsuario().trim().toUpperCase());
        entity.setTelefono(normalizeOptionalText(request.telefono()));
        entity.setCategoria(categoria);
        entity.setDireccion(normalizeOptionalText(request.direccion()));
        entity.setBarrio(barrio);
        entity.setExtranjero(request.extranjero() != null ? request.extranjero() : Boolean.FALSE);
        entity.setSolicitud(solicitud);
        entity.setEstadoSolicitud(estado);
        entity.setObservacion(normalizeOptionalText(request.observacion()));
        entity.setMotivoRepeticion(normalizeOptionalText(request.motivoRepeticion()));
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (isBlank(username) || "anonymousUser".equals(username)) {
            throw new BusinessException("No hay usuario autenticado");
        }

        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ADMIN".equals(authority) || "ROLE_ADMIN".equals(authority));
    }

    private void ensureAdmin() {
        if (!isAdmin()) {
            throw new BusinessException("Solo un administrador puede realizar esta acción");
        }
    }

    private VentanillaResponse toResponse(VentanillaRegistro entity) {
        return new VentanillaResponse(
                entity.getId(),
                entity.getFecha(),
                entity.getNumeroVentanilla(),
                entity.getFuncionario() != null ? entity.getFuncionario().getId() : null,
                entity.getFuncionario() != null ? entity.getFuncionario().getUsername() : null,
                entity.getCedulaUsuario(),
                entity.getNombreUsuario(),
                entity.getTelefono(),
                entity.getCategoria() != null ? entity.getCategoria().getId() : null,
                entity.getCategoria() != null ? entity.getCategoria().getNombre() : null,
                entity.getDireccion(),
                entity.getBarrio() != null ? entity.getBarrio().getId() : null,
                entity.getBarrio() != null ? entity.getBarrio().getNombre() : null,
                entity.getBarrio() != null && entity.getBarrio().getComuna() != null
                        ? entity.getBarrio().getComuna().getNombre()
                        : null,
                entity.getExtranjero(),
                entity.getSolicitud() != null ? entity.getSolicitud().getId() : null,
                entity.getSolicitud() != null ? entity.getSolicitud().getNombre() : null,
                entity.getEstadoSolicitud() != null ? entity.getEstadoSolicitud().getId() : null,
                entity.getEstadoSolicitud() != null ? entity.getEstadoSolicitud().getNombre() : null,
                entity.getObservacion(),
                entity.getMotivoRepeticion(),
                entity.getActivo(),
                buildTraceabilityBadge(entity)
        );
    }

    private VentanillaDailyRequestItemResponse toDailyRequestItem(VentanillaRegistro entity) {
        return new VentanillaDailyRequestItemResponse(
                entity.getId(),
                entity.getFecha(),
                entity.getNumeroVentanilla(),
                entity.getFuncionario() != null ? entity.getFuncionario().getId() : null,
                entity.getFuncionario() != null ? entity.getFuncionario().getUsername() : null,
                entity.getCedulaUsuario(),
                entity.getNombreUsuario(),
                entity.getTelefono(),
                entity.getCategoria() != null ? entity.getCategoria().getId() : null,
                entity.getCategoria() != null ? entity.getCategoria().getNombre() : null,
                entity.getDireccion(),
                entity.getBarrio() != null ? entity.getBarrio().getId() : null,
                entity.getBarrio() != null ? entity.getBarrio().getNombre() : null,
                entity.getBarrio() != null && entity.getBarrio().getComuna() != null
                        ? entity.getBarrio().getComuna().getNombre()
                        : null,
                entity.getExtranjero(),
                entity.getSolicitud() != null ? entity.getSolicitud().getId() : null,
                entity.getSolicitud() != null ? entity.getSolicitud().getNombre() : null,
                entity.getEstadoSolicitud() != null ? entity.getEstadoSolicitud().getId() : null,
                entity.getEstadoSolicitud() != null ? entity.getEstadoSolicitud().getNombre() : null,
                entity.getObservacion(),
                entity.getMotivoRepeticion(),
                entity.getActivo()
        );
    }

    private VentanillaUserHistoryItemResponse toUserHistoryItem(VentanillaRegistro entity) {
        return new VentanillaUserHistoryItemResponse(
                entity.getId(),
                entity.getFecha(),
                entity.getNumeroVentanilla(),
                entity.getFuncionario() != null ? entity.getFuncionario().getId() : null,
                entity.getFuncionario() != null ? entity.getFuncionario().getUsername() : null,
                entity.getCedulaUsuario(),
                entity.getNombreUsuario(),
                entity.getTelefono(),
                entity.getCategoria() != null ? entity.getCategoria().getId() : null,
                entity.getCategoria() != null ? entity.getCategoria().getNombre() : null,
                entity.getDireccion(),
                entity.getBarrio() != null ? entity.getBarrio().getId() : null,
                entity.getBarrio() != null ? entity.getBarrio().getNombre() : null,
                entity.getBarrio() != null && entity.getBarrio().getComuna() != null
                        ? entity.getBarrio().getComuna().getNombre()
                        : null,
                entity.getExtranjero(),
                entity.getSolicitud() != null ? entity.getSolicitud().getId() : null,
                entity.getSolicitud() != null ? entity.getSolicitud().getNombre() : null,
                entity.getEstadoSolicitud() != null ? entity.getEstadoSolicitud().getId() : null,
                entity.getEstadoSolicitud() != null ? entity.getEstadoSolicitud().getNombre() : null,
                entity.getObservacion(),
                entity.getMotivoRepeticion(),
                entity.getActivo()
        );
    }

    private String csvValue(Object value) {
        if (value == null) {
            return "\"\"";
        }

        String text = String.valueOf(value)
                .replace("\"", "\"\"")
                .replace("\r", " ")
                .replace("\n", " ");

        return "\"" + text + "\"";
    }

    private VentanillaTraceabilityBadgeResponse buildTraceabilityBadge(VentanillaRegistro entity) {
        if (entity == null
                || entity.getId() == null
                || entity.getFecha() == null
                || isBlank(entity.getCedulaUsuario())) {
            return new VentanillaTraceabilityBadgeResponse(
                    "NO_DISPONIBLE",
                    "Sin trazabilidad",
                    "default",
                    "No hay información suficiente para calcular la trazabilidad.",
                    0L,
                    0L,
                    null,
                    null,
                    false
            );
        }

        String cedulaUsuario = entity.getCedulaUsuario().trim();

        Long totalVisitas = safeLong(repository.countVisitsByCedula(cedulaUsuario));

        LocalDate fechaInicioVentana = entity.getFecha().minusDays(29);
        LocalDate fechaFinVentana = entity.getFecha();

        Long visitasUltimos30Dias = safeLong(
                repository.countVisitsByCedulaBetween(
                        cedulaUsuario,
                        fechaInicioVentana,
                        fechaFinVentana
                )
        );

        List<VentanillaRegistro> previousVisits = repository.findPreviousVisitsByCedula(
                cedulaUsuario,
                entity.getFecha(),
                entity.getId(),
                PageRequest.of(0, 1)
        );

        LocalDate ultimaVisitaAnterior = previousVisits.isEmpty()
                ? null
                : previousVisits.get(0).getFecha();

        Long diasDesdeUltimaVisitaAnterior = ultimaVisitaAnterior == null
                ? null
                : Math.max(0L, ChronoUnit.DAYS.between(ultimaVisitaAnterior, entity.getFecha()));

        boolean ciudadanoFrecuente = visitasUltimos30Dias >= 3;

        String nivel;
        String etiqueta;
        String color;
        String descripcion;

        if (ultimaVisitaAnterior == null) {
            nivel = "PRIMERA_VISITA";
            etiqueta = "Primera visita";
            color = "success";
            descripcion = "No registra atenciones anteriores en Ventanilla.";
        } else if (ciudadanoFrecuente) {
            nivel = "FRECUENTE";
            etiqueta = "Frecuente";
            color = "warning";
            descripcion = "Registra "
                    + visitasUltimos30Dias
                    + " visitas en los últimos 30 días. Última atención anterior: "
                    + formatDaysText(diasDesdeUltimaVisitaAnterior)
                    + ".";
        } else if (diasDesdeUltimaVisitaAnterior != null && diasDesdeUltimaVisitaAnterior == 0) {
            nivel = "MISMO_DIA";
            etiqueta = "Vino hoy";
            color = "info";
            descripcion = "Ya tenía otra atención registrada el mismo día.";
        } else {
            nivel = "RECURRENTE";
            etiqueta = "Recurrente";
            color = "primary";
            descripcion = "Última atención anterior: "
                    + formatDaysText(diasDesdeUltimaVisitaAnterior)
                    + ". Total visitas registradas: "
                    + totalVisitas
                    + ".";
        }

        return new VentanillaTraceabilityBadgeResponse(
                nivel,
                etiqueta,
                color,
                descripcion,
                totalVisitas,
                visitasUltimos30Dias,
                ultimaVisitaAnterior,
                diasDesdeUltimaVisitaAnterior,
                ciudadanoFrecuente
        );
    }

    private Long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String formatDaysText(Long days) {
        if (days == null) {
            return "sin dato";
        }

        if (days == 0) {
            return "hoy";
        }

        if (days == 1) {
            return "hace 1 día";
        }

        return "hace " + days + " días";
    }

    private Map<String, Object> snapshot(VentanillaRegistro entity) {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("id", entity.getId());
        data.put("fecha", entity.getFecha());
        data.put("numeroVentanilla", entity.getNumeroVentanilla());
        data.put("funcionarioId", entity.getFuncionario() != null ? entity.getFuncionario().getId() : null);
        data.put("funcionarioUsername", entity.getFuncionario() != null ? entity.getFuncionario().getUsername() : null);
        data.put("cedulaUsuario", entity.getCedulaUsuario());
        data.put("nombreUsuario", entity.getNombreUsuario());
        data.put("telefono", entity.getTelefono());
        data.put("categoriaId", entity.getCategoria() != null ? entity.getCategoria().getId() : null);
        data.put("categoriaNombre", entity.getCategoria() != null ? entity.getCategoria().getNombre() : null);
        data.put("direccion", entity.getDireccion());
        data.put("barrioId", entity.getBarrio() != null ? entity.getBarrio().getId() : null);
        data.put("barrioNombre", entity.getBarrio() != null ? entity.getBarrio().getNombre() : null);
        data.put("extranjero", entity.getExtranjero());
        data.put("solicitudId", entity.getSolicitud() != null ? entity.getSolicitud().getId() : null);
        data.put("solicitudNombre", entity.getSolicitud() != null ? entity.getSolicitud().getNombre() : null);
        data.put("estadoSolicitudId", entity.getEstadoSolicitud() != null ? entity.getEstadoSolicitud().getId() : null);
        data.put("estadoSolicitudNombre", entity.getEstadoSolicitud() != null ? entity.getEstadoSolicitud().getNombre() : null);
        data.put("observacion", entity.getObservacion());
        data.put("motivoRepeticion", entity.getMotivoRepeticion());
        data.put("activo", entity.getActivo());

        return data;
    }

    private void addUserHistoryPdfHeader(
            Document document,
            VentanillaUserHistoryResponse history
    ) throws DocumentException {
        Color primaryColor = new Color(11, 42, 69);
        Color accentColor = new Color(24, 129, 148);

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, primaryColor);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(90, 90, 90));
        Font badgeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

        PdfPTable headerTable = new PdfPTable(new float[]{75, 25});
        headerTable.setWidthPercentage(100);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPaddingBottom(8);

        Paragraph title = new Paragraph("Historial de usuario - Ventanilla", titleFont);
        title.setSpacingAfter(4);

        Paragraph subtitle = new Paragraph(
                "Reporte individual de visitas y solicitudes registradas en el módulo de Ventanilla.",
                subtitleFont
        );

        titleCell.addElement(title);
        titleCell.addElement(subtitle);

        PdfPCell badgeCell = new PdfPCell(new Phrase("AppSisben\nValledupar", badgeFont));
        badgeCell.setBorder(Rectangle.NO_BORDER);
        badgeCell.setBackgroundColor(accentColor);
        badgeCell.setPadding(10);
        badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        badgeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        headerTable.addCell(titleCell);
        headerTable.addCell(badgeCell);

        document.add(headerTable);

        PdfPTable userTable = new PdfPTable(new float[]{20, 30, 18, 32});
        userTable.setWidthPercentage(100);
        userTable.setSpacingBefore(8);
        userTable.setSpacingAfter(10);

        addUserInfoCell(userTable, "Cédula", history.cedulaUsuario());
        addUserInfoCell(userTable, "Nombre", safeText(history.nombreUsuario()));
        addUserInfoCell(userTable, "Teléfono", safeText(history.telefono()));
        addUserInfoCell(userTable, "Fecha de generación", formatPdfDate(LocalDate.now()));

        document.add(userTable);
    }

    private void addUserHistoryPdfSummary(
            Document document,
            VentanillaUserHistoryResponse history
    ) throws DocumentException {
        PdfPTable summaryTable = new PdfPTable(4);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingAfter(12);

        addSummaryCell(summaryTable, "Total visitas", String.valueOf(history.totalVisitas()));
        addSummaryCell(summaryTable, "Total solicitudes", String.valueOf(history.totalSolicitudes()));
        addSummaryCell(summaryTable, "Primera visita", formatPdfDate(history.primeraVisita()));
        addSummaryCell(summaryTable, "Última visita", formatPdfDate(history.ultimaVisita()));

        document.add(summaryTable);
    }

    private void addUserHistoryPdfTable(
            Document document,
            VentanillaUserHistoryResponse history
    ) throws DocumentException {
        Font sectionFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                12,
                new Color(11, 42, 69)
        );

        Paragraph sectionTitle = new Paragraph("Detalle de solicitudes registradas", sectionFont);
        sectionTitle.setSpacingBefore(6);
        sectionTitle.setSpacingAfter(8);
        document.add(sectionTitle);

        if (history.solicitudes() == null || history.solicitudes().isEmpty()) {
            Font emptyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(90, 90, 90));
            Paragraph empty = new Paragraph(
                    "No se encontraron solicitudes registradas para este ciudadano.",
                    emptyFont
            );

            document.add(empty);
            return;
        }

        PdfPTable table = new PdfPTable(new float[]{8, 8, 15, 12, 11, 14, 11, 6, 9, 17, 20});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        addHeaderCell(table, "Fecha");
        addHeaderCell(table, "Vent.");
        addHeaderCell(table, "Solicitud");
        addHeaderCell(table, "Categoría");
        addHeaderCell(table, "Estado");
        addHeaderCell(table, "Barrio / Comuna");
        addHeaderCell(table, "Funcionario");
        addHeaderCell(table, "Ext.");
        addHeaderCell(table, "Registro");
        addHeaderCell(table, "Motivo repetición");
        addHeaderCell(table, "Observación");

        for (VentanillaUserHistoryItemResponse item : history.solicitudes()) {
            addBodyCell(table, formatPdfDate(item.fecha()));
            addBodyCell(table, safeText(item.numeroVentanilla()));
            addBodyCell(table, safeText(item.solicitudNombre()));
            addBodyCell(table, safeText(item.categoriaNombre()));
            addBodyCell(table, safeText(item.estadoSolicitudNombre()));
            addBodyCell(table, safeText(item.barrioNombre()) + " / " + safeText(item.comunaNombre()));
            addBodyCell(table, safeText(item.funcionarioUsername()));
            addBodyCell(table, Boolean.TRUE.equals(item.extranjero()) ? "Sí" : "No");
            addBodyCell(table, Boolean.TRUE.equals(item.activo()) ? "Activo" : "Inactivo");
            addBodyCell(table, safeText(item.motivoRepeticion()));
            addBodyCell(table, safeText(item.observacion()));
        }

        document.add(table);
    }

    private void addUserHistoryPdfFooter(Document document) throws DocumentException {
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(110, 110, 110));

        Paragraph footer = new Paragraph(
                "Documento generado automáticamente por AppSisben. La información corresponde a registros activos e inactivos del módulo de Ventanilla.",
                footerFont
        );

        footer.setSpacingBefore(12);
        footer.setAlignment(Element.ALIGN_CENTER);

        document.add(footer);
    }

    private void addUserInfoCell(
            PdfPTable table,
            String label,
            String value
    ) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(90, 90, 90));
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(25, 25, 25));

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(new Color(244, 249, 252));
        cell.setPadding(8);

        Paragraph labelParagraph = new Paragraph(label, labelFont);
        labelParagraph.setSpacingAfter(2);

        Paragraph valueParagraph = new Paragraph(safeText(value), valueFont);

        cell.addElement(labelParagraph);
        cell.addElement(valueParagraph);

        table.addCell(cell);
    }

    private void addSummaryCell(
            PdfPTable table,
            String label,
            String value
    ) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(90, 90, 90));
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(11, 42, 69));

        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(new Color(226, 232, 240));
        cell.setBackgroundColor(Color.WHITE);
        cell.setPadding(9);

        Paragraph labelParagraph = new Paragraph(label, labelFont);
        labelParagraph.setAlignment(Element.ALIGN_CENTER);
        labelParagraph.setSpacingAfter(3);

        Paragraph valueParagraph = new Paragraph(safeText(value), valueFont);
        valueParagraph.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(labelParagraph);
        cell.addElement(valueParagraph);

        table.addCell(cell);
    }

    private void addHeaderCell(
            PdfPTable table,
            String text
    ) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);

        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(11, 42, 69));
        cell.setBorderColor(new Color(11, 42, 69));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        table.addCell(cell);
    }

    private void addBodyCell(
            PdfPTable table,
            String text
    ) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(45, 45, 45));

        PdfPCell cell = new PdfPCell(new Phrase(safeText(text), font));
        cell.setBorderColor(new Color(226, 232, 240));
        cell.setPadding(5);
        cell.setVerticalAlignment(Element.ALIGN_TOP);

        table.addCell(cell);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeOptionalText(String value) {
        if (isBlank(value)) {
            return null;
        }

        return value.trim();
    }

    private String safeText(Object value) {
        if (value == null) {
            return "-";
        }

        String text = String.valueOf(value)
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();

        if (text.isEmpty()) {
            return "-";
        }

        if (text.length() > 180) {
            return text.substring(0, 177) + "...";
        }

        return text;
    }

    private String formatPdfDate(LocalDate date) {
        if (date == null) {
            return "-";
        }

        return date.format(PDF_DATE_FORMATTER);
    }
}