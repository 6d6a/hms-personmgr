package ru.majordomo.hms.personmgr.dto.request;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

@Data
public class Credentials {

    @NotBlank(message = "Не указан логин")
    private String login;

    @NotBlank(message = "Не указан пароль")
    private String password;
}
