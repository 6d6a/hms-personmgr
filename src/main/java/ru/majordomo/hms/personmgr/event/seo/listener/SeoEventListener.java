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
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.rc.user.resources.WebSite;

import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_NAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_ID_KEY;

@Component
public class SeoEventListener {
    private final static Logger logger = LoggerFactory.getLogger(SeoEventListener.class);

    private final AccountHelper accountHelper;
    private final ApplicationEventPublisher publisher;
    private final RcUserFeignClient rcUserFeignClient;
    private final String proEmail;

    @Autowired
    public SeoEventListener(
            AccountHelper accountHelper,
            ApplicationEventPublisher publisher,
            RcUserFeignClient rcUserFeignClient,
            @Value("${mail_manager.pro_email}") String proEmail
    ) {
        this.accountHelper = accountHelper;
        this.publisher = publisher;
        this.rcUserFeignClient = rcUserFeignClient;
        this.proEmail = proEmail;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onSeoOrdered(SeoOrderedEvent event) {
        System.out.println("Execute method asynchronously. "
                + Thread.currentThread().getName());

        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        logger.debug("We got SeoOrderedEvent");

        WebSite webSite = null;

        String webSiteId = (String) params.get(RESOURCE_ID_KEY);
        String serviceName = (String) params.get(SERVICE_NAME_KEY);

        try {
            webSite = rcUserFeignClient.getWebSite(account.getId(), webSiteId);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("ru.majordomo.hms.personmgr.event.seo.listener.SeoEventListener.onSeoOrdered SeoOrderedEvent getWebSite Exception");
        }

        if (webSite == null) {
            logger.error("ru.majordomo.hms.personmgr.event.seo.listener.SeoEventListener.onSeoOrdered WebSite with id " + webSiteId + " not found");

            return;
        }

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
        String webSiteName = webSite.getName();

        parameters.put("body", "1. Аккаунт: " + account.getName() + "<br>" +
                "2. E-mail: " + clientEmails + "<br>" +
                "3. Имя сайта: " + webSiteName + "<br><br>" +
                "Услуга " + serviceName + " оплачена из ПУ.");
        parameters.put("subject", "Услуга " + serviceName + " оплачена");

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }
}
