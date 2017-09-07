package ru.majordomo.hms.personmgr.service.scheduler;

import net.javacrumbs.shedlock.core.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.service.FinFeignClient;
import ru.majordomo.hms.personmgr.service.RecurrentProcessorService;

@Component
public class ChargesScheduler {
    private final static Logger logger = LoggerFactory.getLogger(ChargesScheduler.class);

    private final PersonalAccountManager accountManager;
    private final FinFeignClient finFeignClient;
    private final RecurrentProcessorService recurrentProcessorService;

    @Autowired
    public ChargesScheduler(
            PersonalAccountManager accountManager,
            FinFeignClient finFeignClient,
            RecurrentProcessorService recurrentProcessorService
    ) {
        this.accountManager = accountManager;
        this.finFeignClient = finFeignClient;
        this.recurrentProcessorService = recurrentProcessorService;
    }


    //Выполняем реккуренты в 00:10:00 каждый день
    @SchedulerLock(name="processRecurrents")
    public void processRecurrents() {
        logger.info("Started processRecurrents");
        try {
            List<String> accountIds = finFeignClient.getRecurrentAccounts();
            accountIds.forEach(item -> recurrentProcessorService.processRecurrent(accountManager.findOne(item)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("Ended processRecurrents");
    }
}
