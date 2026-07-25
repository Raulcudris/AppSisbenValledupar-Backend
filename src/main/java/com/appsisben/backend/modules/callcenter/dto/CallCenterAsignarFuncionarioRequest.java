package com.appsisben.backend.modules.callcenter.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CallCenterAsignarFuncionarioRequest(
        @NotNull(message = "Debe seleccionar el funcionario Call Center")
        Long funcionarioCallcenterId,

        @NotEmpty(message = "Debe seleccionar al menos un registro")
        List<Long> registroIds
) {
}