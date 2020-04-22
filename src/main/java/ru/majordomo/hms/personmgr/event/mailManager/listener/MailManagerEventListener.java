package ru.majordomo.hms.personmgr.event.mailManager.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

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
    @Async("mailThreadPoolTaskExecutor")
    public void onSendMail(SendMailEvent event) {
        SimpleServiceMessage message = event.getSource();

        logger.debug("We got SendMailEvent");

        try {
            mailManager.send(message, MailManager.UrlKey.SEND_EMAIL);
        } catch (HttpClientErrorException e) {
            logger.error("mail not sent: " + message + " exception: " + e.getMessage()+ " " + e.getResponseBodyAsString());
            e.printStackTrace();
        } catch (Exception e) {
            logger.error("mail not sent: " + message + " exception: " + e.getMessage());
            e.printStackTrace();
        }

        logger.debug("mail sent: " + message);
    }

    @EventListener
    @Async("mailThreadPoolTaskExecutor")
    public void onSendSms(SendSmsEvent event) {
        SimpleServiceMessage message = event.getSource();

        try {
            mailManager.send(message, MailManager.UrlKey.SEND_SMS);
        } catch (HttpClientErrorException e) {
            logger.error("sms not sent: " + message + " exception: " + e.getMessage()+ " " + e.getResponseBodyAsString());
            e.printStackTrace();
        } catch (Exception e) {
            logger.error("sms not sent: " + message + " exception: " + e.getMessage());
            e.printStackTrace();
        }

        logger.debug("sms sent: " + message);
    }
}
