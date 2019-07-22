package ru.majordomo.hms.personmgr.service.Revisium;

import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.dto.revisium.CheckResponse;
import ru.majordomo.hms.personmgr.dto.revisium.ResultStatus;
import ru.majordomo.hms.personmgr.event.revisium.ProcessRevisiumRequestEvent;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequest;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequestService;
import ru.majordomo.hms.personmgr.repository.RevisiumRequestRepository;

import java.time.LocalDateTime;

@AllArgsConstructor
@Service
public class RevisiumRequestProcessor {
    private final RevisiumRequestRepository repository;
    private final RevisiumApiClient client;
    private final ApplicationEventPublisher publisher;
    private final AccountHistoryManager history;

    public RevisiumRequest process(PersonalAccount account, RevisiumRequestService revisiumRequestService) {
        //Запрос в Ревизиум и обработка
        RevisiumRequest revisiumRequest = new RevisiumRequest();
        revisiumRequest.setRevisiumRequestServiceId(revisiumRequestService.getId());
        revisiumRequest.setPersonalAccountId(revisiumRequestService.getPersonalAccountId());
        revisiumRequest.setCreated(LocalDateTime.now());
        repository.save(revisiumRequest);

        try {

            CheckResponse checkResponse = client.check(revisiumRequestService.getSiteUrl());

            switch (ResultStatus.valueOf(checkResponse.getStatus().toUpperCase())) {
                case COMPLETE:
                case INCOMPLETE:
                    revisiumRequest.setRequestId(checkResponse.getRequestId());
                    revisiumRequest.setSuccessCheck(true);
                    revisiumRequest = repository.save(revisiumRequest);
                    publisher.publishEvent(new ProcessRevisiumRequestEvent(revisiumRequest));
                    break;
                case CANCELED:
                case FAILED:
                default:
                    revisiumRequest.setSuccessCheck(false);
                    repository.save(revisiumRequest);
                    history.save(
                            account,
                            "Ошибка при запросе проверки (check) сайта '" + revisiumRequestService.getSiteUrl() + "' в Ревизиум. Текст ошибки: '" + checkResponse.getErrorMessage() + "'",
                            "service");
                    break;
            }

        } catch (Exception e) {
            history.save(
                    account,
                    "Ошибка при запросе проверки (check) сайта: '" + revisiumRequestService.getSiteUrl() + "' в Ревизиум.",
                    "service");
            e.printStackTrace();
        }

        return revisiumRequest;
    }
}
