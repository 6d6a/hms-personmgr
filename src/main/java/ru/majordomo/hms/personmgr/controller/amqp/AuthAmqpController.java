package ru.majordomo.hms.personmgr.controller.amqp;

import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.dto.NewAuth;
import ru.majordomo.hms.personmgr.event.account.NewAuthNotifyEvent;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

import java.util.HashMap;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.AUTH_IP_NEW;

@Service
@AllArgsConstructor
public class AuthAmqpController extends CommonAmqpController {

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + AUTH_IP_NEW)
    public void add(@Payload NewAuth newAuth) {

        PersonalAccount account = accountManager.findByAccountId(newAuth.getName());

        Map<String, String> params = new HashMap<>();
        params.put("acc_id", newAuth.getName());
        params.put("ip_add", newAuth.getIp());

        publisher.publishEvent(new NewAuthNotifyEvent(account, params));
    }
}
