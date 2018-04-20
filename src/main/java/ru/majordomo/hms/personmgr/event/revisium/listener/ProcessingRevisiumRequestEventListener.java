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
import ru.majordomo.hms.personmgr.dto.revisium.Monitoring;
import ru.majordomo.hms.personmgr.dto.revisium.MonitoringFlag;
import ru.majordomo.hms.personmgr.dto.revisium.ResultStatus;
import ru.majordomo.hms.personmgr.event.revisium.ProcessBulkRevisiumRequestEvent;
import ru.majordomo.hms.personmgr.event.revisium.ProcessRevisiumRequestEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.RevisiumAccountNotice;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequest;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequestService;
import ru.majordomo.hms.personmgr.repository.AccountNoticeRepository;
import ru.majordomo.hms.personmgr.repository.RevisiumRequestRepository;
import ru.majordomo.hms.personmgr.repository.RevisiumRequestServiceRepository;
import ru.majordomo.hms.personmgr.service.AccountHistoryService;
import ru.majordomo.hms.personmgr.service.AccountNotificationHelper;
import ru.majordomo.hms.personmgr.service.Revisium.RevisiumApiClient;
import ru.majordomo.hms.personmgr.service.scheduler.RevisiumRequestScheduler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Component
public class ProcessingRevisiumRequestEventListener {
    private final static Logger logger = LoggerFactory.getLogger(ProcessingRevisiumRequestEventListener.class);

    private final RevisiumApiClient revisiumApiClient;
    private final ApplicationEventPublisher publisher;
    private final PersonalAccountManager personalAccountManager;
    private final RevisiumRequestRepository revisiumRequestRepository;
    private final RevisiumRequestServiceRepository revisiumRequestServiceRepository;
    private final RevisiumRequestScheduler scheduler;
    private final AccountNoticeRepository accountNoticeRepository;
    private final AccountNotificationHelper accountNotificationHelper;
    private final AccountHistoryService history;

    @Autowired
    public ProcessingRevisiumRequestEventListener(
            RevisiumApiClient revisiumApiClient,
            ApplicationEventPublisher publisher,
            PersonalAccountManager personalAccountManager,
            RevisiumRequestRepository revisiumRequestRepository,
            RevisiumRequestServiceRepository revisiumRequestServiceRepository,
            RevisiumRequestScheduler scheduler,
            AccountNoticeRepository accountNoticeRepository,
            AccountNotificationHelper accountNotificationHelper,
            AccountHistoryService history
    ) {
        this.revisiumApiClient = revisiumApiClient;
        this.publisher = publisher;
        this.personalAccountManager = personalAccountManager;
        this.revisiumRequestRepository = revisiumRequestRepository;
        this.revisiumRequestServiceRepository = revisiumRequestServiceRepository;
        this.scheduler = scheduler;
        this.accountNoticeRepository = accountNoticeRepository;
        this.accountNotificationHelper = accountNotificationHelper;
        this.history = history;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onProcessingRevisiumRequestEventAction(ProcessRevisiumRequestEvent event) {

        logger.debug("We got ProcessingRevisiumRequestEvent");

        RevisiumRequest revisiumRequest = event.getSource();

        try {

            GetResultResponse getResultResponse = revisiumApiClient.getResult(revisiumRequest.getRequestId());

            Boolean isResultCompleted = false;

            switch (ResultStatus.valueOf(getResultResponse.getStatus().toUpperCase())) {
                case COMPLETE:
                    revisiumRequest.setSuccessGetResult(true);
                    revisiumRequest.setResult(new Gson().toJson(getResultResponse));
                    revisiumRequestRepository.save(revisiumRequest);
                    isResultCompleted = true;
                    break;
                case INCOMPLETE:
                    try {
                        //Проверка сайта происходит в течении примерно минуты
                        Thread.sleep(100000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    publisher.publishEvent(new ProcessRevisiumRequestEvent(revisiumRequest));
                    break;
                case CANCELED:
                case FAILED:
                default:
                    revisiumRequest.setSuccessGetResult(false);
                    revisiumRequestRepository.save(revisiumRequest);
                    PersonalAccount account = personalAccountManager.findOne(revisiumRequest.getPersonalAccountId());
                    RevisiumRequestService revisiumRequestService = revisiumRequestServiceRepository.findOne(revisiumRequest.getRevisiumRequestServiceId());
                    history.saveForOperatorService(
                            account,
                            "Результат проверки (get_result) сайта '" + revisiumRequestService.getSiteUrl() + "' в Ревизиум содержит ошибку. " +
                                    "Текст ошибки: '" + getResultResponse.getErrorMessage() + "'"
                    );
                    break;
            }

            if (isResultCompleted) {
                try {
                    Boolean newAlertFound = false;

                    List<RevisiumRequest> revisiumRequests = revisiumRequestRepository.findByPersonalAccountIdAndRevisiumRequestServiceIdOrderByCreatedDesc(
                            revisiumRequest.getPersonalAccountId(), revisiumRequest.getRevisiumRequestServiceId()
                    );

                    if (revisiumRequests.size() > 1) {

                        GetResultResponse last = new Gson().fromJson(revisiumRequests.get(0).getResult(), GetResultResponse.class);
                        GetResultResponse previous = new Gson().fromJson(revisiumRequests.get(1).getResult(), GetResultResponse.class);

                        if (last != null && previous != null) {
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
                            //newAlertFound = isNewAlertIncoming(
                            //        last.getMonitoring().getIp(), previous.getMonitoring().getIp()) ? true : newAlertFound;
                            //newAlertFound = isNewAlertIncoming(
                            //        last.getMonitoring().getDns(), previous.getMonitoring().getDns()) ? true : newAlertFound;
                            //newAlertFound = isNewAlertIncoming(
                            //        last.getMonitoring().getDnsExpiration(), previous.getMonitoring().getDnsExpiration()) ? true : newAlertFound;
                            //newAlertFound = isNewAlertIncoming(
                            //        last.getMonitoring().getCms(), previous.getMonitoring().getCms()) ? true : newAlertFound;
                            //newAlertFound = isNewAlertIncoming(
                            //        last.getMonitoring().getJsErrors(), previous.getMonitoring().getJsErrors()) ? true : newAlertFound;
                        }

                    } else if (revisiumRequests.size() == 1) {
                        GetResultResponse last = new Gson().fromJson(revisiumRequests.get(0).getResult(), GetResultResponse.class);

                        if (last != null && last.getMonitoring() != null) {
                            Monitoring monitoring = last.getMonitoring();
                            newAlertFound = !monitoring.getHtmlMalware().isEmpty() && monitoring.getHtmlMalware().get(MonitoringFlag.ALERT) > 0;
                            newAlertFound = !monitoring.getFilesMalware().isEmpty() && monitoring.getFilesMalware().get(MonitoringFlag.ALERT) > 0 ? true : newAlertFound;
                            newAlertFound = !monitoring.getBlacklistedUrls().isEmpty() && monitoring.getBlacklistedUrls().get(MonitoringFlag.ALERT) > 0 ? true : newAlertFound;
                            newAlertFound = !monitoring.getRedirects().isEmpty() && monitoring.getRedirects().get(MonitoringFlag.ALERT) > 0 ? true : newAlertFound;
                            newAlertFound = !monitoring.getBlacklisted().isEmpty() && monitoring.getBlacklisted().get(MonitoringFlag.ALERT) > 0 ? true : newAlertFound;
                            newAlertFound = !monitoring.getSuspiciousUrls().isEmpty() && monitoring.getSuspiciousUrls().get(MonitoringFlag.ALERT) > 0 ? true : newAlertFound;
                            newAlertFound = !monitoring.getExternalResources().isEmpty() && monitoring.getExternalResources().get(MonitoringFlag.ALERT) > 0 ? true : newAlertFound;
                            newAlertFound = !monitoring.getExternalLinks().isEmpty() && monitoring.getExternalLinks().get(MonitoringFlag.ALERT) > 0 ? true : newAlertFound;
                            newAlertFound = !monitoring.getIssues().isEmpty() && monitoring.getIssues().get(MonitoringFlag.ALERT) > 0 ? true : newAlertFound;
                            //newAlertFound = (monitoring.getIp().get(MonitoringFlag.ALERT) > 0) ? true : newAlertFound;
                            //newAlertFound = (monitoring.getDns().get(MonitoringFlag.ALERT) > 0) ? true : newAlertFound;
                            //newAlertFound = (monitoring.getDnsExpiration().get(MonitoringFlag.ALERT) > 0) ? true : newAlertFound;
                            //newAlertFound = (monitoring.getCms().get(MonitoringFlag.ALERT) > 0) ? true : newAlertFound;
                            //newAlertFound = (monitoring.getJsErrors().get(MonitoringFlag.ALERT) > 0) ? true : newAlertFound;
                        }
                    }

                    if (newAlertFound) {
                        RevisiumAccountNotice notification = new RevisiumAccountNotice();
                        notification.setPersonalAccountId(revisiumRequest.getPersonalAccountId());
                        notification.setCreated(LocalDateTime.now());
                        notification.setViewed(false);
                        notification.setRevisiumRequestServiceId(revisiumRequest.getRevisiumRequestServiceId());
                        notification.setRevisiumRequestId(revisiumRequest.getId());

                        accountNoticeRepository.save(notification);

                        PersonalAccount account = personalAccountManager.findOne(revisiumRequest.getPersonalAccountId());
                        RevisiumRequestService revisiumRequestService = revisiumRequestServiceRepository.findOne(revisiumRequest.getRevisiumRequestServiceId());
                        HashMap<String, String> parameters = new HashMap<>();
                        parameters.put("client_id", account.getAccountId());
                        parameters.put("site_url", revisiumRequestService.getSiteUrl());

                        accountNotificationHelper.sendMail(account, "HmsMajordomoProverkaScanera", 10, parameters);
                    }

                } catch (Exception e) {
                    logger.error("Непредвиденная ошибка при сравнении результатов из Ревизиума. revisiumRequest ID: " + revisiumRequest.getId());
                    e.printStackTrace();
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
        return !last.isEmpty() && (previous.isEmpty() || last.get(MonitoringFlag.ALERT) > previous.get(MonitoringFlag.ALERT));
    }
}
