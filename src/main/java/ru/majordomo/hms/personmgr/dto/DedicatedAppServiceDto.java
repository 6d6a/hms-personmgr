package ru.majordomo.hms.personmgr.dto;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.DedicatedAppService;
import ru.majordomo.hms.rc.staff.resources.Service;

import javax.annotation.Nullable;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class DedicatedAppServiceDto {

    private String personalAccountId;

    private String accountServiceId;

    private String id;

    @Nullable
    private AccountService accountService;

    private String templateId;

    @Nullable
    private LocalDate createdDate;

    private boolean active;

    @Nullable
    private Service service = null;

    public DedicatedAppServiceDto(DedicatedAppService dedicatedAppService) {
        accountServiceId = dedicatedAppService.getAccountServiceId();
        accountService = dedicatedAppService.getAccountService();
        id = dedicatedAppService.getId();
        templateId = dedicatedAppService.getTemplateId();
        createdDate = dedicatedAppService.getCreatedDate();
        active = dedicatedAppService.isActive();
        personalAccountId = dedicatedAppService.getPersonalAccountId();
    }
}
