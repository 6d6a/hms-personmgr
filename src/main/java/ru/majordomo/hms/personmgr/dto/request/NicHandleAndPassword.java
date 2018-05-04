package ru.majordomo.hms.personmgr.dto.request;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

@Data
public class NicHandleAndPassword {

    @NotBlank(message = "Не указан Nic-Handle")
    private String nicHandle;

    @NotBlank(message = "Не указан пароль")
    private String password;
}
