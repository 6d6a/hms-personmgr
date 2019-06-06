package ru.majordomo.hms.personmgr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.push.Push;

@Service
@Slf4j
public class PushService {
    private final AmqpSender sender;

    @Autowired
    public PushService(AmqpSender sender) {
        this.sender = sender;
    }

    public void send(Push push) {
        SimpleServiceMessage message = push.toMessage();

        try {
            sender.send("message.send", "notifier", message);
        } catch (Exception e) {
            log.error("push not sent: {} e.class: {} e.message: {}", message, e.getClass(), e.getMessage());
            e.printStackTrace();
        }
        log.debug("push sent: " + message);
    }
}
