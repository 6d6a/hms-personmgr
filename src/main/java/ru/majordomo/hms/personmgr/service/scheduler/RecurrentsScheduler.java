package ru.majordomo.hms.personmgr.service.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

import ru.majordomo.hms.personmgr.event.account.AccountRecurrentEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.feign.FinFeignClient;
import ru.majordomo.hms.personmgr.service.RecurrentProcessorService;

@Component
@AllArgsConstructor
@Slf4j
public class RecurrentsScheduler {

    private final FinFeignClient finFeignClient;
    private final ApplicationEventPublisher publisher;

    //Выполняем реккуренты в 00:10:00 каждый день
    @SchedulerLock(name="processRecurrents")
    public void processRecurrents() {
        log.info("Started processRecurrents");
        try {
            List<String> accountIds = finFeignClient.getRecurrentAccounts();
            accountIds.forEach(item -> publisher.publishEvent(new AccountRecurrentEvent(item)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("Ended processRecurrents");
    }
}
