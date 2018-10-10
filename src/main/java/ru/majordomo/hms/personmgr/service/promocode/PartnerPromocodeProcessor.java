package ru.majordomo.hms.personmgr.service.promocode;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.dto.partners.RegisterStat;
import ru.majordomo.hms.personmgr.dto.partners.RegisterStatRequest;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.PartnersFeignClient;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class PartnerPromocodeProcessor {

    private final PartnersFeignClient partnersFeignClient;
    private final AccountHistoryManager history;

    @Autowired
    public PartnerPromocodeProcessor(
            PartnersFeignClient partnersFeignClient,
            AccountHistoryManager history
    ) {
        this.partnersFeignClient = partnersFeignClient;
        this.history = history;
    }

    public Result process(PersonalAccount account, String code) {
        RegisterStat registerStat = null;
        try {
            RegisterStatRequest registerStatRequest = new RegisterStatRequest();
            registerStatRequest.setCode(code);
            registerStatRequest.setAccountName(account.getName());

            registerStat = partnersFeignClient.registerByAccountIdAndCode(account.getId(), registerStatRequest);
        } catch (FeignException e) {
            if (e.status() == 404) {
                return Result.error("Партнёрский промокод " + code + " не найден");
            } else if (e.status() == 400) {
                return Result.gotException(translateError(e.getMessage()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.gotException("Не удалось обработать промокод");
        }

        if (registerStat == null) {
            return Result.error("Не удалось обработать промокод");
        }

        history.save(account, "Промокод " + code + " обработан как партнёрский");

        return Result.success();
    }

    private String translateError(String content) {
        log.info("try to translate error response from partners: " + content);

        content = content == null ? "" : content.replaceAll(".*content:", "");

        String result;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> response = objectMapper.readValue(content, new TypeReference<Map<String, Object>>(){});
            result = response.get("message").toString();
        } catch (Exception e) {
            log.error("Cant convert content to map: " + content);
            result = "Возникла ошибка";
        }

        return result;
    }
}
