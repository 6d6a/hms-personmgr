package ru.majordomo.hms.personmgr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PromocodeManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.model.promocode.UnknownPromocode;
import ru.majordomo.hms.personmgr.repository.UnknownPromocodeRepository;
import ru.majordomo.hms.personmgr.service.promocode.PartnerPromocodeProcessor;
import ru.majordomo.hms.personmgr.service.promocode.PromocodeProcessorFactory;

@Slf4j
@Service
public class PromocodeService {
    private final UnknownPromocodeRepository unknownPromocodeRepository;
    private final AccountHistoryManager history;
    private final PromocodeManager promocodeManager;
    private final PromocodeProcessorFactory promocodeProcessorFactory;
    private final PartnerPromocodeProcessor partnerPromocodeProcessor;

    @Autowired
    public PromocodeService(
            UnknownPromocodeRepository unknownPromocodeRepository,
            AccountHistoryManager history,
            PromocodeManager promocodeManager,
            PromocodeProcessorFactory promocodeProcessorFactory,
            PartnerPromocodeProcessor partnerPromocodeProcessor
    ) {
        this.unknownPromocodeRepository = unknownPromocodeRepository;
        this.history = history;
        this.promocodeManager = promocodeManager;
        this.promocodeProcessorFactory = promocodeProcessorFactory;
        this.partnerPromocodeProcessor = partnerPromocodeProcessor;
    }

    public void processPromocode(PersonalAccount account, String code) {
        log.debug("We got promocode '" + code + "'. Try to process it");
        code = code.trim();

        Result partnerResult = partnerPromocodeProcessor.process(account, code);

        if (partnerResult.isSuccess() || partnerResult.isGotException()) {
            log.info("account id " + account.getId() + "promocode " + code + " was process as partner"
                + (partnerResult.isGotException() ? " with exception" : "")
            );
        } else {
            processPmPromocode(account, code);
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
