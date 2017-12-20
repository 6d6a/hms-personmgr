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

import ru.majordomo.hms.personmgr.event.webSite.WebSiteCreatedEvent;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.AccountNotificationHelper;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.rc.user.resources.WebSite;

import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_ID_KEY;

@Component
public class WebSiteEventListener {
    private final static Logger logger = LoggerFactory.getLogger(WebSiteEventListener.class);

    private final AccountHelper accountHelper;
    private final RcUserFeignClient rcUserFeignClient;
    private final AccountNotificationHelper accountNotificationHelper;

    @Autowired
    public WebSiteEventListener(
            AccountHelper accountHelper,
            ApplicationEventPublisher publisher,
            RcUserFeignClient rcUserFeignClient,
            AccountNotificationHelper accountNotificationHelper
    ) {
        this.accountHelper = accountHelper;
        this.rcUserFeignClient = rcUserFeignClient;
        this.accountNotificationHelper = accountNotificationHelper;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onWebSiteCreated(WebSiteCreatedEvent event) {
        //Нет такого шаблона, попытки отправки засирают лог
//        PersonalAccount account = event.getSource();
//        Map<String, ?> params = event.getParams();
//
//        WebSite webSite = null;
//
//        String webSiteId = (String) params.get(RESOURCE_ID_KEY);
//
//        try {
//            webSite = rcUserFeignClient.getWebSite(account.getId(), webSiteId);
//        } catch (Exception e) {
//            e.printStackTrace();
//            logger.error("ru.majordomo.hms.personmgr.event.webSite.listener.WebSiteEventListener.onWebSiteCreated SeoOrderedEvent getWebSite Exception");
//        }
//
//        if (webSite == null) {
//            logger.error("ru.majordomo.hms.personmgr.event.webSite.listener.WebSiteEventListener.onWebSiteCreated WebSite with id " + webSiteId + " not found");
//
//            return;
//        }
//
//        HashMap<String, String> parameters = new HashMap<>();
//        parameters.put("client_id", account.getAccountId());
//        parameters.put("website_name", webSite.getName());
//
//        accountNotificationHelper.sendMail(account, "MajordomoVHWebSiteCreated", 10, parameters);
    }
}
