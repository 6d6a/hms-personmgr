package ru.majordomo.hms.personmgr.service.promocode;

import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;

public interface PromocodeProcessor {
    Result process(PersonalAccount account, Promocode promocode);
}
