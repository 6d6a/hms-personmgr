package ru.majordomo.hms.personmgr.service.promocode;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;

@Slf4j
public class AlwaysErrorPromocodeProcessor implements PromocodeProcessor {
    @Override
    public Result process(PersonalAccount account, Promocode promocode) {
        return Result.error("Не удалось обработать промокод");
    }
}
