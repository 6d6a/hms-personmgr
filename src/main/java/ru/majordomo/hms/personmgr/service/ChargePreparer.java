package ru.majordomo.hms.personmgr.service;

import net.javacrumbs.shedlock.core.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.event.account.AccountPrepareChargesEvent;
import ru.majordomo.hms.personmgr.manager.ChargeRequestManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequest;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequestItem;
import ru.majordomo.hms.personmgr.model.service.AccountService;

import java.time.LocalDate;
import java.util.List;

@Service
public class ChargePreparer {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ApplicationEventPublisher publisher;
    private final ChargeRequestManager chargeRequestManager;
    private final PersonalAccountManager accountManager;
    private final AccountServiceHelper accountServiceHelper;

    public ChargePreparer(
            ApplicationEventPublisher publisher,
            ChargeRequestManager chargeRequestManager,
            PersonalAccountManager accountManager,
            AccountServiceHelper accountServiceHelper
    ) {
        this.publisher = publisher;
        this.chargeRequestManager = chargeRequestManager;
        this.accountManager = accountManager;
        this.accountServiceHelper = accountServiceHelper;
    }

    //Выполняем списания в 01:00:00 каждый день
    @SchedulerLock(name="prepareCharges")
    public void prepareCharges(LocalDate chargeDate) {
        logger.info("Started PrepareCharges for " + chargeDate);
        List<PersonalAccount> personalAccounts = accountManager.findByActiveIncludeId(true);
        if (personalAccounts != null && !personalAccounts.isEmpty()) {
            logger.info("PrepareCharges found " + personalAccounts.size() + " active accounts");

            try {
                personalAccounts.forEach(account -> publisher.publishEvent(new AccountPrepareChargesEvent(account.getId(), chargeDate)));
            } catch (Exception e) {
                logger.error("Catching exception in publish events AccountPrepareChargesEvent");
                e.printStackTrace();
            }
        } else {
            logger.error("Active accounts not found in daily charges.");
        }
        logger.info("Ended PrepareCharges for " + chargeDate);
    }

    public ChargeRequest prepareCharge(String accountId) {
        return prepareCharge(accountId,  LocalDate.now());
    }

    public ChargeRequest prepareCharge(String accountId, LocalDate chargeDate) {
        PersonalAccount account = accountManager.findOne(accountId);

        //Не списываем с неактивных аккаунтов
        if (!account.isActive()) { return null; }

        List<AccountService> accountServices = accountServiceHelper.getDailyServicesToCharge(account, chargeDate);

        //Если списывать нечего
        if (accountServices.isEmpty()) { return null; }

        ChargeRequest chargeRequest = new ChargeRequest();
        chargeRequest.setPersonalAccountId(accountId);
        chargeRequest.setChargeDate(chargeDate);

        for(AccountService accountService: accountServices) {
            ChargeRequestItem chargeRequestItem = new ChargeRequest();
            chargeRequestItem.setAccountServiceId(accountService.getId());
            chargeRequestItem.setChargeDate(chargeDate);
            chargeRequestItem.setAmount(accountServiceHelper.getDailyCostForService(accountService, chargeDate));

            chargeRequest.addChargeRequest(chargeRequestItem);
        }

        return chargeRequestManager.insert(chargeRequest);
    }
}
