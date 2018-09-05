package ru.majordomo.hms.personmgr.dto.request;

import lombok.Data;
import ru.majordomo.hms.rc.user.resources.validation.ValidEmail;

import javax.validation.constraints.NotNull;

@Data
public class EmailInviteRequest implements InviteRequest {
    @NotNull(message = "Не указан email")
    @ValidEmail
    private String email;
}
