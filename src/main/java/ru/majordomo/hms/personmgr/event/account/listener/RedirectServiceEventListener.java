package ru.majordomo.hms.personmgr.event.account.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.RedirectWasDisabledEvent;
import ru.majordomo.hms.personmgr.event.account.RedirectWasProlongEvent;
import ru.majordomo.hms.personmgr.service.BusinessHelper;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.rc.user.resources.Redirect;

import java.util.List;

@Component
public class RedirectServiceEventListener {

    private RcUserFeignClient rcUserFeignClient;
    private BusinessHelper businessHelper;

    @Autowired
    public RedirectServiceEventListener(
            RcUserFeignClient rcUserFeignClient,
            BusinessHelper businessHelper
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.businessHelper = businessHelper;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void enableRedirects(RedirectWasProlongEvent event) {
        String accountId = event.getSource();
        String domainName = event.getDomainName();
        switchRedirectService(true, accountId, domainName);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void disableRedirects(RedirectWasDisabledEvent event) {
        String accountId = event.getSource();
        String domainName = event.getDomainName();
        switchRedirectService(false, accountId, domainName);
    }

    private void switchRedirectService(boolean state, String accountId, String domainName) {
        List<Redirect> redirects = rcUserFeignClient.getRedirects(accountId);
        redirects.stream()
                .filter(redirect -> domainName.equals(redirect.getDomain().getName()))
                .forEach(redirect -> {
                    SimpleServiceMessage message = new SimpleServiceMessage();
                    message.setAccountId(accountId);
                    message.addParam("resourceId", redirect.getId());
                    message.addParam("switchedOn", false);
                    businessHelper.buildActionAndOperation(BusinessOperationType.REDIRECT_UPDATE, BusinessActionType.REDIRECT_UPDATE_RC, message);
                });
    }
}
