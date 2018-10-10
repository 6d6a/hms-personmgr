package ru.majordomo.hms.personmgr.service.promocodeAction;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.dto.fin.PaymentRequest;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.repository.AccountPromocodeRepository;
import ru.majordomo.hms.personmgr.service.FinFeignClient;

import java.util.List;

@Service
@Slf4j
public class BalanceFillActionProcessor implements PromocodeActionProcessor {

    private final FinFeignClient finFeignClient;
    private final AccountHistoryManager history;
    private final AccountPromocodeRepository accountPromocodeRepository;

    @Autowired
    public BalanceFillActionProcessor(
            FinFeignClient finFeignClient,
            AccountHistoryManager history,
            AccountPromocodeRepository accountPromocodeRepository
    ) {
        this.finFeignClient = finFeignClient;
        this.history = history;
        this.accountPromocodeRepository = accountPromocodeRepository;
    }

    @Override
    public Result process(PersonalAccount account, PromocodeAction action, String code) {
        log.debug("Processing codeAction: " + action.toString());

        try {
            finFeignClient.addPayment(
                    new PaymentRequest(account.getName())
                            .withAmount(Utils.getBigDecimalFromUnexpectedInput(action.getProperties().get("amount")))
                            .withBonusType()
                            .withMessage("Использован промокод " + code)
                            .withDisableAsync(true)
            );
            history.save(account,"Начислено " + action.getProperties().get("amount") + " бонусов при использовании кода " + code);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[BalanceFillActionProcessor]" + e.getClass().getName() + ": " + e.getMessage());
            return Result.gotException(e.getMessage());
        }
    }

    @Override
    public boolean isAllowed(PersonalAccount account, PromocodeAction action) {
        List<AccountPromocode> accountPromocodes = accountPromocodeRepository.findByPersonalAccountId(account.getId());

        return accountPromocodes
                .stream()
                .flatMap(ap -> ap.getPromocode().getActions().stream())
                .noneMatch(usedAction -> usedAction.getActionType().equals(action.getActionType()));
    }
}
