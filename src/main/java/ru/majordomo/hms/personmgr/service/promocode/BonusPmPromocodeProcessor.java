package ru.majordomo.hms.personmgr.service.promocode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.repository.AccountPromocodeRepository;
import ru.majordomo.hms.personmgr.service.promocodeAction.PromocodeActionProcessorFactory;

import java.time.LocalDate;
import java.util.List;

import static ru.majordomo.hms.personmgr.common.Constants.TOCHKA_BANK_PROMOCODE_TAG_ID;

@Slf4j
@Service
public class BonusPmPromocodeProcessor implements PmPromocodeProcessor {
    private final AccountPromocodeRepository accountPromocodeRepository;
    private final AccountHistoryManager history;
    private final PromocodeActionProcessorFactory promocodeActionProcessorFactory;
    private final PersonalAccountManager accountManager;

    @Autowired
    public BonusPmPromocodeProcessor(
            AccountPromocodeRepository accountPromocodeRepository,
            AccountHistoryManager history,
            PromocodeActionProcessorFactory promocodeActionProcessorFactory,
            PersonalAccountManager accountManager) {
        this.accountPromocodeRepository = accountPromocodeRepository;
        this.history = history;
        this.promocodeActionProcessorFactory = promocodeActionProcessorFactory;
        this.accountManager = accountManager;
    }

    @Override
    public Result process(PersonalAccount account, Promocode promocode) {
        if (promocode.getExpired() != null && promocode.getExpired().isBefore(LocalDate.now())) {
            return Result.error("Срок действия промокода истёк");
        }

        boolean codeAlreadyExists = accountPromocodeRepository.existsByPromocodeId(promocode.getId());
        if (codeAlreadyExists) {
            history.save(
                    account,
                    "Клиент пытается использовать уже использованный бонусный промокод " + promocode.getCode()
            );
            return Result.error("Код уже использован");
        }

        List<PromocodeAction> promocodeActions = promocode.getActions();

        boolean isAllowed = promocodeActions.stream().allMatch(action ->
                        promocodeActionProcessorFactory
                                .getProcessor(action.getActionType())
                                .isAllowed(account, action)
        );

        if (!isAllowed) { return Result.error("Использование промокода недоступно"); }

        AccountPromocode accountPromocode = addAccountPromocode(account, promocode);

        if (promocode.getTagId() != null && promocode.getTagId().equals(TOCHKA_BANK_PROMOCODE_TAG_ID)) {
            accountManager.setHideGoogleAdWords(account.getId(), true);
        }

        log.debug("Processing promocode actions for account: " + account.getName() + " for code: " + accountPromocode.getCode());

        for (PromocodeAction action: promocodeActions) {
            Result result = promocodeActionProcessorFactory
                    .getProcessor(action.getActionType())
                    .process(account, action, accountPromocode.getCode());

            if (!result.isSuccess()) {
                return result;
            }
        }

        return Result.success();
    }

    private AccountPromocode addAccountPromocode(PersonalAccount account, Promocode promocode) {
        AccountPromocode accountPromocode = new AccountPromocode();
        accountPromocode.setOwnedByAccount(true);
        accountPromocode.setPersonalAccountId(account.getId());
        accountPromocode.setOwnerPersonalAccountId(account.getId());
        accountPromocode.setPromocodeId(promocode.getId());
        accountPromocode.setPromocode(promocode);

        history.save(account, "Клиент использовал бонусный промокод '" + promocode.getCode() + "'");

        return accountPromocodeRepository.save(accountPromocode);
    }
}
