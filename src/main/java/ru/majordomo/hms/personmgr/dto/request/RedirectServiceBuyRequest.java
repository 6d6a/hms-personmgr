package ru.majordomo.hms.personmgr.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class RedirectServiceBuyRequest {
    @NotBlank(message = "Не указан id домена, для которого подключается редирект")
    private String domainId;
}
