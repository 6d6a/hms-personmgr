package ru.majordomo.hms.personmgr.service.promocodeAction;

import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;

public interface PromocodeActionProcessor {
    Result process(PersonalAccount account, PromocodeAction action, String code);

    boolean isAllowed(PersonalAccount account, PromocodeAction action);
}
