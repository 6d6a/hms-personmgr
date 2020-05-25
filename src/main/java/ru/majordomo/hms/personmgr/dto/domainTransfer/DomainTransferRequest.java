package ru.majordomo.hms.personmgr.dto.domainTransfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DomainTransferRequest {
    @NotBlank(message = "Укажите доменное имя")
    private final String domainName;

    @NotBlank(message = "Укажите код трансфера")
    @Size(min = 6, max = 32, message = "Длина кода должна быть от 6 до 32 символов")
    @Pattern(regexp = "^((?!##)[\\x20-\\x3B\\x3D\\x3F-\\x7E])*$", message = "Неверный формат кода")
    private final String authInfo;

    @NotBlank(message = "Укажите персону")
    private final String personId;
}
