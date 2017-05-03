package ru.majordomo.hms.personmgr.event.mailManager.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.event.mailManager.SendSmsEvent;
import ru.majordomo.hms.personmgr.service.MailManager;

@Component
public class MailManagerEventListener {
    private final static Logger logger = LoggerFactory.getLogger(MailManagerEventListener.class);

    private final MailManager mailManager;

    @Autowired
    public MailManagerEventListener(MailManager mailManager) {
        this.mailManager = mailManager;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onSendMail(SendMailEvent event) {
        SimpleServiceMessage message = event.getSource();

        logger.debug("We got SendMailEvent");

        mailManager.sendEmail(message);
        logger.debug("mail sent: " + message);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onSendSms(SendSmsEvent event) {
        SimpleServiceMessage message = event.getSource();

        mailManager.sendSms(message);
        logger.debug("sms sent: " + message);
    }
}
