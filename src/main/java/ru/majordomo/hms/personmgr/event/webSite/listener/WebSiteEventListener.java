package ru.majordomo.hms.personmgr.event.webSite.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.event.webSite.WebSiteCreatedEvent;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.rc.user.resources.WebSite;

import static ru.majordomo.hms.personmgr.common.Constants.WEB_SITE_ID_KEY;

@Component
public class WebSiteEventListener {
    private final static Logger logger = LoggerFactory.getLogger(WebSiteEventListener.class);

    private final AccountHelper accountHelper;
    private final ApplicationEventPublisher publisher;
    private final RcUserFeignClient rcUserFeignClient;

    @Autowired
    public WebSiteEventListener(
            AccountHelper accountHelper,
            ApplicationEventPublisher publisher,
            RcUserFeignClient rcUserFeignClient
    ) {
        this.accountHelper = accountHelper;
        this.publisher = publisher;
        this.rcUserFeignClient = rcUserFeignClient;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onWebSiteCreated(WebSiteCreatedEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        String emails = accountHelper.getEmail(account);

        WebSite webSite = null;

        String webSiteId = (String) params.get(WEB_SITE_ID_KEY);

        try {
            webSite = rcUserFeignClient.getWebSite(account.getId(), webSiteId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (webSite == null) {
            logger.debug("WebSite with id " + webSiteId + " not found");

            return;
        }

        String webSiteName = webSite.getName();

        SimpleServiceMessage message = new SimpleServiceMessage();

        message.setParams(new HashMap<>());
        message.addParam("email", emails);
        message.addParam("api_name", "MajordomoVHWebSiteCreated");
        message.addParam("priority", 10);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("website_name", webSiteName);

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }
}
