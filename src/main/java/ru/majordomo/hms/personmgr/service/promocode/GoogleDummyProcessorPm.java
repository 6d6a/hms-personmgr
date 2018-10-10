package ru.majordomo.hms.personmgr.service.promocode;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;

@Slf4j
public class GoogleDummyProcessorPm implements PmPromocodeProcessor {
    @Override
    public Result process(PersonalAccount account, Promocode promocode) {
        log.info("Пользователь " + account.getId() + " использовал промокод Google " + promocode.getCode() + " в контрольной панели");
        return Result.error("Не удалось обработать промокод");
    }
}
