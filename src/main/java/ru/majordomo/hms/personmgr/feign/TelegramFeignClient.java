package ru.majordomo.hms.personmgr.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.majordomo.hms.personmgr.config.FeignConfig;
import ru.majordomo.hms.personmgr.dto.TelegramMessageData;

@FeignClient(name = "telegram", configuration = FeignConfig.class)
public interface TelegramFeignClient {
    @PostMapping(value = "/send", consumes = "application/json")
    void sendMessage(@RequestBody TelegramMessageData telegramMessageData);

    @GetMapping(value = "/botname", consumes = "application/json")
    String getBotName();
}
