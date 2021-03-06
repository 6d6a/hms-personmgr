package ru.majordomo.hms.personmgr.service;

import net.javacrumbs.shedlock.core.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.event.account.AccountPrepareChargesEvent;
import ru.majordomo.hms.personmgr.manager.BatchJobManager;
import ru.majordomo.hms.personmgr.manager.ChargeRequestManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequest;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequestItem;
import ru.majordomo.hms.personmgr.model.service.AccountService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ChargePreparer {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ApplicationEventPublisher publisher;
    private final ChargeRequestManager chargeRequestManager;
    private final PersonalAccountManager accountManager;
    private final AccountServiceHelper accountServiceHelper;
    private final BatchJobManager batchJobManager;

    public ChargePreparer(
            ApplicationEventPublisher publisher,
            ChargeRequestManager chargeRequestManager,
            PersonalAccountManager accountManager,
            AccountServiceHelper accountServiceHelper,
            BatchJobManager batchJobManager
    ) {
        this.publisher = publisher;
        this.chargeRequestManager = chargeRequestManager;
        this.accountManager = accountManager;
        this.accountServiceHelper = accountServiceHelper;
        this.batchJobManager = batchJobManager;
    }

    //Выполняем списания в 01:00:00 каждый день
    @SchedulerLock(name="prepareCharges")
    public void prepareCharges(LocalDate chargeDate, String batchJobId) {
        logger.info("Started PrepareCharges for " + chargeDate);

        List<PersonalAccount> personalAccounts = accountManager.findByActiveIncludeId(true);

        final AtomicInteger preparedChargesCount = new AtomicInteger(0);

        if (personalAccounts != null && !personalAccounts.isEmpty()) {
            batchJobManager.setCount(batchJobId, personalAccounts.size());
            logger.info("PrepareCharges found " + personalAccounts.size() + " active accounts");

            try {
                personalAccounts = personalAccounts
                        .stream()
                        .filter(personalAccount -> !accountServiceHelper.getDailyServicesToCharge(personalAccount, chargeDate).isEmpty())
                        .peek(personalAccount -> {
                            batchJobManager.incrementNeedToProcess(batchJobId);
                            preparedChargesCount.incrementAndGet();
                        })
                        .collect(Collectors.toList());

                batchJobManager.setStateToProcessing(batchJobId);

                personalAccounts
                        .forEach(account -> publisher.publishEvent(new AccountPrepareChargesEvent(account.getId(), chargeDate, batchJobId)));
            } catch (Exception e) {
                logger.error("Catching exception in publish events AccountPrepareChargesEvent");
                e.printStackTrace();
            }

            logger.info("PrepareCharges created " + preparedChargesCount + " records");
        } else {
            logger.error("Active accounts not found in daily charges.");
        }

        batchJobManager.setStateToFinishedIfNeeded(batchJobId);

        logger.info("Ended PrepareCharges for " + chargeDate);
    }

    public ChargeRequest prepareCharge(String accountId, LocalDate chargeDate) {
        return prepareCharge(accountId,  chargeDate, false);
    }

    public ChargeRequest prepareCharge(String accountId, LocalDate chargeDate, boolean chargeFromInactive) {
        PersonalAccount account = accountManager.findOneByIdIncludeIdAndActive(accountId);

        //Не списываем с неактивных аккаунтов если это не требуется указанием параметра chargeFromInactive
        if (!chargeFromInactive && !account.isActive()) { return null; }

        Map<AccountService, BigDecimal> accountServices = accountServiceHelper.getDailyServicesToCharge(account, chargeDate);

        //Если списывать нечего
        if (accountServices.isEmpty()) { return null; }

        ChargeRequest chargeRequest = new ChargeRequest();
        chargeRequest.setPersonalAccountId(accountId);
        chargeRequest.setChargeDate(chargeDate);
        accountServices.forEach((key, value) -> {
            ChargeRequestItem chargeRequestItem = new ChargeRequest();
            chargeRequestItem.setAccountServiceId(key.getId());
            chargeRequestItem.setChargeDate(chargeDate);
            chargeRequestItem.setAmount(accountServiceHelper.getDailyCostForService(key, value, chargeDate));

            chargeRequest.addChargeRequest(chargeRequestItem);
        });

        return chargeRequestManager.insert(chargeRequest);
    }
}
