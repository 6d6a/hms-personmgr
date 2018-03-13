package ru.majordomo.hms.personmgr.service.scheduler;

import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.dto.revisium.GetStatResponse;
import ru.majordomo.hms.personmgr.dto.revisium.ResultStatus;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequestService;
import ru.majordomo.hms.personmgr.repository.RevisiumRequestRepository;
import ru.majordomo.hms.personmgr.repository.RevisiumRequestServiceRepository;
import ru.majordomo.hms.personmgr.service.AccountServiceHelper;
import ru.majordomo.hms.personmgr.service.Revisium.RevisiumApiClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class RevisiumRequestScheduler {
    private final static Logger logger = LoggerFactory.getLogger(RevisiumRequestScheduler.class);

    private final RevisiumRequestServiceRepository revisiumRequestServiceRepository;
    private final AccountServiceHelper accountServiceHelper;
    private final PersonalAccountManager accountManager;
    private final RevisiumApiClient revisiumApiClient;

    @Autowired
    public RevisiumRequestScheduler(
            RevisiumRequestServiceRepository revisiumRequestServiceRepository,
            AccountServiceHelper accountServiceHelper,
            PersonalAccountManager accountManager,
            RevisiumApiClient revisiumApiClient
    ) {
        this.revisiumRequestServiceRepository = revisiumRequestServiceRepository;
        this.accountServiceHelper = accountServiceHelper;
        this.accountManager = accountManager;
        this.revisiumApiClient = revisiumApiClient;
    }

    //2 раза в день
    @SchedulerLock(name="processRequests")
    public void processRequests() {
        logger.info("Started processRecurrents");

            List<RevisiumRequestService> revisiumRequestServices = revisiumRequestServiceRepository.findAll();

            for (RevisiumRequestService item: revisiumRequestServices) {

                try {

                    GetStatResponse getStatResponse = revisiumApiClient.getStat();

                    switch (ResultStatus.valueOf(getStatResponse.getStatus().toUpperCase())) {
                        case COMPLETE:
                            if (getStatResponse.getQueued() >= (getStatResponse.getQueueLength() - 3)) {
                                try {
                                    //Проверка сайта происходит в течении примерно минуты => ждём около 100 секунд, пока очередь очистится
                                    Thread.sleep(100000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            break;
                        default:
                            //Ошибка при запросе статистики?
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            break;
                    }

                    if (!item.getExpireDate().isBefore(LocalDate.now()) && item.getAccountService().isEnabled()) {
                        accountServiceHelper.revisiumCheckRequest(accountManager.findOne(item.getPersonalAccountId()), item);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        logger.info("Ended processRecurrents");
    }
}
