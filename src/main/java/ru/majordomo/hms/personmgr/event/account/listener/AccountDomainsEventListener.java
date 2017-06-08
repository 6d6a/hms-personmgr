package ru.majordomo.hms.personmgr.event.account.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.event.account.AccountDomainAutoRenewCompletedEvent;
import ru.majordomo.hms.personmgr.event.account.AccountProcessDomainsAutoRenewEvent;
import ru.majordomo.hms.personmgr.event.account.AccountProcessExpiringDomainsEvent;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.DomainService;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.rc.user.resources.Domain;

import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_ID_KEY;

@Component
public class AccountDomainsEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountDomainsEventListener.class);

    private final DomainService domainService;
    private final ApplicationEventPublisher publisher;
    private final RcUserFeignClient rcUserFeignClient;

    @Autowired
    public AccountDomainsEventListener(
            DomainService domainService,
            ApplicationEventPublisher publisher,
            RcUserFeignClient rcUserFeignClient
    ) {
        this.domainService = domainService;
        this.publisher = publisher;
        this.rcUserFeignClient = rcUserFeignClient;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountProcessExpiringDomainsEvent(AccountProcessExpiringDomainsEvent event) {
        PersonalAccount account = event.getSource();

        logger.debug("We got AccountProcessExpiringDomainsEvent");

        try {
            domainService.processExpiringDomainsByAccount(account);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.event.account.listener.AccountDomainsEventListener.onAccountProcessExpiringDomainsEvent " + e.getMessage());
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountProcessDomainsAutoRenewEvent(AccountProcessDomainsAutoRenewEvent event) {
        PersonalAccount account = event.getSource();

        logger.debug("We got AccountProcessDomainsAutoRenewEvent");

        try {
            domainService.processDomainsAutoRenewByAccount(account);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.event.account.listener.AccountDomainsEventListener.onAccountProcessDomainsAutoRenewEvent " + e.getMessage());
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountDomainAutoRenewCompletedEvent(AccountDomainAutoRenewCompletedEvent event) {
        PersonalAccount account = event.getSource();

        Map<String, ?> params = event.getParams();

        logger.debug("We got AccountDomainAutoRenewCompletedEvent");

        Domain domain = rcUserFeignClient.getDomain(account.getId(), (String) params.get(RESOURCE_ID_KEY));

        //Запишем попытку в историю клиента
        Map<String, String> historyParams = new HashMap<>();
        historyParams.put(HISTORY_MESSAGE_KEY, "Произведено автоматическое продление " + domain.getName());
        historyParams.put(OPERATOR_KEY, "service");

        publisher.publishEvent(new AccountHistoryEvent(account.getId(), historyParams));
    }
}
