package ru.majordomo.hms.personmgr.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;

import javax.validation.constraints.Pattern;

@Data
public class CodeApplyRequest {
    @NotBlank(message = "Необходимо указать код")
    @Pattern(regexp = "^\\s*$", message = "Необходимо указать код")
    private String code;
}
