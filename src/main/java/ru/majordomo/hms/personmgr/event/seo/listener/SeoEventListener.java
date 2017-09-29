package ru.majordomo.hms.personmgr.event.seo.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.event.seo.SeoOrderedEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.AccountHelper;

import static ru.majordomo.hms.personmgr.common.Constants.DOMAIN_NAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_NAME_KEY;

@Component
public class SeoEventListener {
    private final static Logger logger = LoggerFactory.getLogger(SeoEventListener.class);

    private final AccountHelper accountHelper;
    private final PersonalAccountManager personalAccountManager;
    private final ApplicationEventPublisher publisher;
    private final String proEmail;

    @Autowired
    public SeoEventListener(
            AccountHelper accountHelper,
            PersonalAccountManager personalAccountManager,
            ApplicationEventPublisher publisher,
            @Value("${mail_manager.pro_email}") String proEmail
    ) {
        this.accountHelper = accountHelper;
        this.personalAccountManager = personalAccountManager;
        this.publisher = publisher;
        this.proEmail = proEmail;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onSeoOrdered(SeoOrderedEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());
        Map<String, ?> params = event.getParams();

        logger.debug("We got SeoOrderedEvent");

        String domainName = (String) params.get(DOMAIN_NAME_KEY);
        String serviceName = (String) params.get(SERVICE_NAME_KEY);

        //Письмо в  СЕО
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam("email", proEmail);
        message.addParam("api_name", "MajordomoServiceMessage");
        message.addParam("priority", 10);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", message.getAccountId());

        String clientEmails = accountHelper.getEmail(account);

        parameters.put("body", "1. Аккаунт: " + account.getName() + "<br>" +
                "2. E-mail: " + clientEmails + "<br>" +
                "3. Имя сайта: " + domainName + "<br><br>" +
                "Услуга " + serviceName + " оплачена из ПУ.");
        parameters.put("subject", "Услуга " + serviceName + " оплачена");

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }
}
