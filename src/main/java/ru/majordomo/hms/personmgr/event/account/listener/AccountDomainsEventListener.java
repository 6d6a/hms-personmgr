package ru.majordomo.hms.personmgr.event.account.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.event.account.AccountProcessDomainsAutoRenewEvent;
import ru.majordomo.hms.personmgr.event.account.AccountProcessExpiringDomainsEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.DomainService;

@Component
public class AccountDomainsEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountDomainsEventListener.class);

    private final DomainService domainService;
    private final PersonalAccountManager personalAccountManager;

    @Autowired
    public AccountDomainsEventListener(
            DomainService domainService,
            PersonalAccountManager personalAccountManager
    ) {
        this.domainService = domainService;
        this.personalAccountManager = personalAccountManager;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountProcessExpiringDomainsEvent(AccountProcessExpiringDomainsEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

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
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        logger.debug("We got AccountProcessDomainsAutoRenewEvent");

        try {
            domainService.processDomainsAutoRenewByAccount(account);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.event.account.listener.AccountDomainsEventListener.onAccountProcessDomainsAutoRenewEvent " + e.getMessage());
        }
    }
}
