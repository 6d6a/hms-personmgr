package ru.majordomo.hms.personmgr.service.scheduler;

import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.event.account.AccountProcessChargesEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.FinFeignClient;
import ru.majordomo.hms.personmgr.service.RecurrentProcessorService;

@Component
public class ChargesScheduler {
    private final static Logger logger = LoggerFactory.getLogger(ChargesScheduler.class);

    private final PersonalAccountManager accountManager;
    private final ApplicationEventPublisher publisher;
    private final FinFeignClient finFeignClient;
    private final RecurrentProcessorService recurrentProcessorService;

    @Autowired
    public ChargesScheduler(
            PersonalAccountManager accountManager,
            ApplicationEventPublisher publisher,
            FinFeignClient finFeignClient,
            RecurrentProcessorService recurrentProcessorService
    ) {
        this.accountManager = accountManager;
        this.publisher = publisher;
        this.finFeignClient = finFeignClient;
        this.recurrentProcessorService = recurrentProcessorService;
    }


    //Выполняем реккуренты в 00:10:00 каждый день
    @Scheduled(cron = "0 10 0 * * *")
    @SchedulerLock(name="processRecurrents")
    public void processRecurrents() {
        logger.debug("Started processRecurrents");
        try {
            List<String> accountIds = finFeignClient.getRecurrentAccounts();
            accountIds.forEach(item -> recurrentProcessorService.processRecurrent(accountManager.findOne(item)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.debug("Ended processRecurrents");
    }

    //Выполняем списания в 01:00:00 каждый день
    @Scheduled(cron = "0 15 1 * * *")
    @SchedulerLock(name="processCharges")
    public void processCharges() {
        logger.debug("Started processCharges");
        List<PersonalAccount> personalAccounts = accountManager.findByActive(true);
        if (personalAccounts != null) {
            try {

                personalAccounts.forEach(account -> publisher.publishEvent(new AccountProcessChargesEvent(account)));
            } catch (Exception e) {
                logger.error("Catching exception in publish events AccountProcessChargesEvent");
                e.printStackTrace();
            }
        } else {
            logger.error("Active accounts not found in daily charges.");
        }
        logger.debug("Ended processCharges");
    }
}
