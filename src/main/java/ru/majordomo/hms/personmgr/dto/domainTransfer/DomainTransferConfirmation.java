package ru.majordomo.hms.personmgr.dto.domainTransfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DomainTransferConfirmation {
    @NotBlank(message = "Введите код подтверждения")
    @Pattern(regexp = "^[0-9]+$", message = "Разрешены только цифры")
    private final String verificationCode;

    public DomainTransferConfirmation(@JsonProperty("verificationCode") String verificationCode) {
        this.verificationCode = verificationCode;
    }
}
