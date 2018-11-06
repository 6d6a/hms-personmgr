package ru.majordomo.hms.personmgr.service.promocode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.YaPromoterFeignClient;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class YandexPromocodeProcessor {
    private YaPromoterFeignClient client;

    @Autowired
    public YandexPromocodeProcessor(YaPromoterFeignClient client) {
        this.client = client;
    }

    public Result process(PersonalAccount account, String code, String clickId) {
        try {
            Map<String, String> request = new HashMap<>();
            request.put("clickId", clickId);
            request.put("code", code);
            client.registerEvent(account.getId(), request);
            return Result.success();
        } catch (Exception e) {
            log.error("with process code {} and clickId {} for accountId {} catch {} with message {}",
                    code, clickId, account.getId(), e.getClass().getName(), e.getMessage());
            return Result.error("Не удалось обработать промокод");
        }
    }
}
