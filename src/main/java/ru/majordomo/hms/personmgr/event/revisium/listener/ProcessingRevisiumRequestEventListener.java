package ru.majordomo.hms.personmgr.event.revisium.listener;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.dto.revisium.GetResultResponse;
import ru.majordomo.hms.personmgr.dto.revisium.MonitoringFlag;
import ru.majordomo.hms.personmgr.dto.revisium.ResultStatus;
import ru.majordomo.hms.personmgr.event.revisium.ProcessBulkRevisiumRequestEvent;
import ru.majordomo.hms.personmgr.event.revisium.ProcessRevisiumRequestEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequest;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequestService;
import ru.majordomo.hms.personmgr.repository.RevisiumRequestRepository;
import ru.majordomo.hms.personmgr.repository.RevisiumRequestServiceRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.Revisium.ReviScanApiClient;
import ru.majordomo.hms.personmgr.service.scheduler.RevisiumRequestScheduler;

import java.util.HashMap;
import java.util.List;

@Component
public class ProcessingRevisiumRequestEventListener {
    private final static Logger logger = LoggerFactory.getLogger(ProcessingRevisiumRequestEventListener.class);

    private final ReviScanApiClient reviScanApiClient;
    private final ApplicationEventPublisher publisher;
    private final AccountHelper accountHelper;
    private final PersonalAccountManager personalAccountManager;
    private final RevisiumRequestRepository revisiumRequestRepository;
    private final RevisiumRequestServiceRepository revisiumRequestServiceRepository;
    private final RevisiumRequestScheduler scheduler;

    @Autowired
    public ProcessingRevisiumRequestEventListener(
            ReviScanApiClient reviScanApiClient,
            ApplicationEventPublisher publisher,
            AccountHelper accountHelper,
            PersonalAccountManager personalAccountManager,
            RevisiumRequestRepository revisiumRequestRepository,
            RevisiumRequestServiceRepository revisiumRequestServiceRepository,
            RevisiumRequestScheduler scheduler
    ) {
        this.reviScanApiClient = reviScanApiClient;
        this.publisher = publisher;
        this.accountHelper = accountHelper;
        this.personalAccountManager = personalAccountManager;
        this.revisiumRequestRepository = revisiumRequestRepository;
        this.revisiumRequestServiceRepository = revisiumRequestServiceRepository;
        this.scheduler = scheduler;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onProcessingRevisiumRequestEventAction(ProcessRevisiumRequestEvent event) {

        logger.debug("We got ProcessingRevisiumRequestEvent");

        try {
            //Проверка сайта происходит в течении примерно минуты
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        RevisiumRequest revisiumRequest = event.getSource();

        try {

            GetResultResponse getResultResponse = reviScanApiClient.getResult(revisiumRequest.getRequestId());

            switch (ResultStatus.valueOf(getResultResponse.getStatus().toUpperCase())) {
                case COMPLETE:
                    revisiumRequest.setSuccessGetResult(true);
                    revisiumRequest.setResult(new Gson().toJson(getResultResponse));
                    revisiumRequestRepository.save(revisiumRequest);
                    break;
                case INCOMPLETE:
                    publisher.publishEvent(new ProcessRevisiumRequestEvent(revisiumRequest));
                    break;
                case CANCELED:
                case FAILED:
                default:
                    revisiumRequest.setSuccessGetResult(false);
                    revisiumRequestRepository.save(revisiumRequest);
                    PersonalAccount account = personalAccountManager.findOne(revisiumRequest.getPersonalAccountId());
                    RevisiumRequestService revisiumRequestService = revisiumRequestServiceRepository.findOne(revisiumRequest.getRevisiumRequestServiceId());
                    accountHelper.saveHistoryForOperatorService(
                            account,
                            "Результат проверки (get_result) сайта '" + revisiumRequestService.getSiteUrl() + "' в Ревизиум содержит ошибку. " +
                                    "Текст ошибки: '" + getResultResponse.getErrorMessage() + "'"
                    );
                    break;
            }

            List<RevisiumRequest> revisiumRequests = revisiumRequestRepository.findByPersonalAccountIdAndRevisiumRequestServiceIdOrderByCreatedDesc(
                    revisiumRequest.getPersonalAccountId(), revisiumRequest.getRevisiumRequestServiceId()
            );

            if (revisiumRequests.size() > 1) {

                GetResultResponse last = new Gson().fromJson(revisiumRequests.get(0).getResult(), GetResultResponse.class);
                GetResultResponse previous = new Gson().fromJson(revisiumRequests.get(1).getResult(), GetResultResponse.class);

                Boolean newAlertFound = false;

                if (!last.getResult().equals(previous.getResult())) {
                    newAlertFound = isNewAlertIncoming(
                            last.getMonitoring().getHtmlMalware(), previous.getMonitoring().getHtmlMalware());
                    newAlertFound = isNewAlertIncoming(
                            last.getMonitoring().getFilesMalware(), previous.getMonitoring().getFilesMalware()) ? true : newAlertFound;
                    newAlertFound = isNewAlertIncoming(
                            last.getMonitoring().getBlacklistedUrls(), previous.getMonitoring().getBlacklistedUrls()) ? true : newAlertFound;
                    newAlertFound = isNewAlertIncoming(
                            last.getMonitoring().getRedirects(), previous.getMonitoring().getRedirects()) ? true : newAlertFound;
                    newAlertFound = isNewAlertIncoming(
                            last.getMonitoring().getBlacklisted(), previous.getMonitoring().getBlacklisted()) ? true : newAlertFound;
                    newAlertFound = isNewAlertIncoming(
                            last.getMonitoring().getSuspiciousUrls(), previous.getMonitoring().getSuspiciousUrls()) ? true : newAlertFound;
                    newAlertFound = isNewAlertIncoming(
                            last.getMonitoring().getExternalResources(), previous.getMonitoring().getExternalResources()) ? true : newAlertFound;
                    newAlertFound = isNewAlertIncoming(
                            last.getMonitoring().getExternalLinks(), previous.getMonitoring().getExternalLinks()) ? true : newAlertFound;
                    newAlertFound = isNewAlertIncoming(
                            last.getMonitoring().getIssues(), previous.getMonitoring().getIssues()) ? true : newAlertFound;
                    newAlertFound = isNewAlertIncoming(
                            last.getMonitoring().getIp(), previous.getMonitoring().getIp()) ? true : newAlertFound;
                    newAlertFound = isNewAlertIncoming(
                            last.getMonitoring().getDns(), previous.getMonitoring().getDns()) ? true : newAlertFound;
                    newAlertFound = isNewAlertIncoming(
                            last.getMonitoring().getDnsExpiration(), previous.getMonitoring().getDnsExpiration()) ? true : newAlertFound;
                    newAlertFound = isNewAlertIncoming(
                            last.getMonitoring().getCms(), previous.getMonitoring().getCms()) ? true : newAlertFound;
                    newAlertFound = isNewAlertIncoming(
                            last.getMonitoring().getJsErrors(), previous.getMonitoring().getJsErrors()) ? true : newAlertFound;
                }

                if (newAlertFound) {
                    //TODO уведомить пользователя
                }

            }

        } catch (Exception e) {
            logger.error("Непредвиденная ошибка при проверке сайта в Ревизиум. revisiumRequest ID: " + revisiumRequest.getId());
            e.printStackTrace();
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessBulkRevisiumRequestEvent event) {
        logger.debug("We got ProcessBulkRevisiumRequestEvent");

        scheduler.processRequests();
    }

    private Boolean isNewAlertIncoming(HashMap<MonitoringFlag, Integer> last, HashMap<MonitoringFlag, Integer> previous) {
        return last != null && (previous == null || last.get(MonitoringFlag.ALERT) > previous.get(MonitoringFlag.ALERT));
    }
}
