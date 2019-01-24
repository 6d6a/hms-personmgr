package ru.majordomo.hms.personmgr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.PromocodeManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.model.promocode.UnknownPromocode;
import ru.majordomo.hms.personmgr.repository.UnknownPromocodeRepository;
import ru.majordomo.hms.personmgr.service.promocode.PartnerPromocodeProcessor;
import ru.majordomo.hms.personmgr.service.promocode.PromocodeProcessorFactory;
import ru.majordomo.hms.personmgr.service.promocode.YandexPromocodeProcessor;

import java.util.Arrays;
import java.util.Map;

@Slf4j
@Service
public class PromocodeService {
    private final UnknownPromocodeRepository unknownPromocodeRepository;
    private final AccountHistoryManager history;
    private final PromocodeManager promocodeManager;
    private final PromocodeProcessorFactory promocodeProcessorFactory;
    private final PartnerPromocodeProcessor partnerPromocodeProcessor;
    private final YandexPromocodeProcessor yandexPromocodeProcessor;
    private final PersonalAccountManager accountManager;

    @Autowired
    public PromocodeService(
            UnknownPromocodeRepository unknownPromocodeRepository,
            AccountHistoryManager history,
            PromocodeManager promocodeManager,
            PromocodeProcessorFactory promocodeProcessorFactory,
            PartnerPromocodeProcessor partnerPromocodeProcessor,
            YandexPromocodeProcessor yandexPromocodeProcessor,
            PersonalAccountManager accountManager
    ) {
        this.unknownPromocodeRepository = unknownPromocodeRepository;
        this.history = history;
        this.promocodeManager = promocodeManager;
        this.promocodeProcessorFactory = promocodeProcessorFactory;
        this.partnerPromocodeProcessor = partnerPromocodeProcessor;
        this.yandexPromocodeProcessor = yandexPromocodeProcessor;
        this.accountManager = accountManager;
    }

    public void processRegistration(ProcessingBusinessOperation operation) {
        try {
            String code = operation.getParam("promocode") != null
                    ? operation.getParam("promocode").toString().trim()
                    : "";

            if (code.isEmpty()) {
                return;
            }

            log.debug("We got promocode '" + code + "'. Try to process it");

            PersonalAccount account = accountManager.findOne(operation.getPersonalAccountId());

            Map<String, Object> params = operation.getParams();

            //Сначала нужно обработать коды яндекса
            if (params.get("clickId") != null && !params.get("clickId").toString().isEmpty()) {
                Result yandexResult = yandexPromocodeProcessor.process(account, code, params.get("clickId").toString());
                if (yandexResult.isSuccess()) {
                    history.save(account, "Промокод " + code + " успешно обработан как промокод яндекса");
                    return;
                }
            }

            Result partnerResult = partnerPromocodeProcessor.process(account, code);

            if (partnerResult.isSuccess()) {
                log.info("account id " + account.getId() + "promocode " + code + " was process as partner");
            } else {
                processPmPromocode(account, code);
            }
        } catch (Exception e) {
            log.error(
                    "Can't process registration promocode e: {} message: {}, operation: {}",
                    e.getClass(), e.getMessage(), operation.toString()
            );
        }
    }

    public Result processPmPromocode(PersonalAccount account, String code) {
        try {
            code = code.trim();

            Promocode promocode = promocodeManager.findByCodeAndActive(code, true);

            if (promocode == null) {
                if (!code.equals("")) {
                    addUnknownPromocode(account, code);
                }
                return Result.error("Промокод не найден");
            }

            return promocodeProcessorFactory.getProcessor(promocode.getType()).process(account, promocode);

        } catch (Exception e) {
            log.error("Обработка промокода для аккаунта " + account.getId() + " и кода " + code
                    + " завершена с ошибкой " + e.getClass().getName() + " message: " + e.getMessage()
                    + " stackTrace: " + Arrays.asList(e.getStackTrace()).toString()
            );
            history.save(account, "Обработка промокода '" + code + "' завершена с ошибкой");
            return Result.error("Обработка промокода '" + code + "' завершена с ошибкой");
        }
    }

    private void addUnknownPromocode(PersonalAccount account, String code) {
        UnknownPromocode unknownPromocode = new UnknownPromocode(account, code);

        unknownPromocodeRepository.save(unknownPromocode);

        history.save(account, "Клиент использовал неизвестный промокод " + code);
    }
}
