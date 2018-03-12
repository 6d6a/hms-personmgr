package ru.majordomo.hms.personmgr.service.scheduler;

import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.dto.revisium.CheckResponse;
import ru.majordomo.hms.personmgr.dto.revisium.ResultStatus;
import ru.majordomo.hms.personmgr.event.revisium.ProcessRevisiumRequestEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequest;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequestService;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.RevisiumRequestRepository;
import ru.majordomo.hms.personmgr.repository.RevisiumRequestServiceRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.AccountServiceHelper;
import ru.majordomo.hms.personmgr.service.FinFeignClient;
import ru.majordomo.hms.personmgr.service.RecurrentProcessorService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class RevisiumRequestScheduler {
    private final static Logger logger = LoggerFactory.getLogger(RevisiumRequestScheduler.class);

    private final RevisiumRequestRepository revisiumRequestRepository;
    private final RevisiumRequestServiceRepository revisiumRequestServiceRepository;
    private final AccountServiceHelper accountServiceHelper;
    private final PersonalAccountManager accountManager;

    @Autowired
    public RevisiumRequestScheduler(
            RevisiumRequestRepository revisiumRequestRepository,
            RevisiumRequestServiceRepository revisiumRequestServiceRepository,
            AccountServiceHelper accountServiceHelper,
            PersonalAccountManager accountManager
    ) {
        this.revisiumRequestRepository = revisiumRequestRepository;
        this.revisiumRequestServiceRepository = revisiumRequestServiceRepository;
        this.accountServiceHelper = accountServiceHelper;
        this.accountManager = accountManager;
    }

    //2 раза в день
    @SchedulerLock(name="processRequests")
    public void processRequests() {
        logger.info("Started processRecurrents");
        try {

            List<RevisiumRequestService> revisiumRequestServices = revisiumRequestServiceRepository.findAll();
            revisiumRequestServices.forEach(item -> {
                if (!item.getExpireDate().isBefore(LocalDate.now()) && item.getAccountService().isEnabled()) {
                    accountServiceHelper.revisiumCheckRequest(accountManager.findOne(item.getPersonalAccountId()), item);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("Ended processRecurrents");
    }
}
