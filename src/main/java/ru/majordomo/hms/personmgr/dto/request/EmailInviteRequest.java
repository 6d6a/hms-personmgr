package ru.majordomo.hms.personmgr.dto.request;

import lombok.Data;
import javax.validation.constraints.NotEmpty;
import ru.majordomo.hms.rc.user.resources.validation.ValidEmail;

import javax.validation.Valid;
import java.util.List;

@Data
public class EmailInviteRequest implements InviteRequest {
    @Valid
    @NotEmpty(message = "Не указан ни один почтовый ящик")
    private List<@ValidEmail String> emails;
}
