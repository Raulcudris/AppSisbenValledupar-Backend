package com.appsisben.backend.modules.callcenter.dto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record CallCenterAsignarEncuestadorRequest(
        @NotNull(message = "Debe seleccionar el encuestador")
        Long encuestadorId,

        LocalDate fechaEncuestaProgramada,

        @NotEmpty(message = "Debe seleccionar al menos un registro")
        List<Long> registroIds
) {
}
