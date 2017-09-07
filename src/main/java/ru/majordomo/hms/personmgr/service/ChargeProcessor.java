package ru.majordomo.hms.personmgr.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.ChargeResult;
import ru.majordomo.hms.personmgr.manager.ChargeRequestManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequest;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequestItem;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;

@Service
public class ChargeProcessor {
    private final ChargeRequestManager chargeRequestManager;
    private final PersonalAccountManager accountManager;
    private final AccountServiceRepository accountServiceRepository;
    private final AccountServiceHelper accountServiceHelper;
    private final AccountHelper accountHelper;
    private final AccountNotificationHelper accountNotificationHelper;
    private final AccountStatHelper accountStatHelper;
    private final Charger charger;

    public ChargeProcessor(
            ChargeRequestManager chargeRequestManager,
            PersonalAccountManager accountManager,
            AccountServiceRepository accountServiceRepository,
            AccountServiceHelper accountServiceHelper,
            AccountHelper accountHelper,
            AccountNotificationHelper accountNotificationHelper,
            AccountStatHelper accountStatHelper,
            Charger charger
    ) {
        this.chargeRequestManager = chargeRequestManager;
        this.accountManager = accountManager;
        this.accountServiceRepository = accountServiceRepository;
        this.accountServiceHelper = accountServiceHelper;
        this.accountHelper = accountHelper;
        this.accountNotificationHelper = accountNotificationHelper;
        this.accountStatHelper = accountStatHelper;
        this.charger = charger;
    }

    public void process() {
        LocalDate chargeDate = LocalDate.now();
        process(chargeDate);
    }

    public void process(LocalDate chargeDate) {
        List<ChargeRequest> chargeRequests = chargeRequestManager.getForProcess(chargeDate, 500);

        for (ChargeRequest chargeRequest : chargeRequests) {
            processChargeRequest(chargeRequest);
        }
    }

    public ChargeResult processChargeRequest(ChargeRequest chargeRequest) {
        PersonalAccount account = accountManager.findOne(chargeRequest.getPersonalAccountId());

        BigDecimal dailyCost = BigDecimal.ZERO;
        for(ChargeRequestItem chargeRequestItem : chargeRequest.getChargeRequests()) {
            AccountService accountService =  accountServiceRepository.findOne(chargeRequestItem.getAccountServiceId());

            ChargeResult chargeResult = charger.makeCharge(accountService, chargeRequest.getChargeDate());
            if (chargeResult.isSuccess()) {
                dailyCost = dailyCost.add(accountServiceHelper.getDailyCostForService(accountService, chargeRequest.getChargeDate()));
                chargeRequestItem.setStatus(ChargeRequestItem.Status.CHARGED);
            } else if (!chargeResult.isSuccess() && !chargeResult.isGotException()) {
                switch (accountServiceHelper.getPaymentServiceType(accountService)) {
                    case "PLAN":
                        disableAndNotifyAccountByReasonNotEnoughMoney(account);
                        return chargeResult;
                    case "ADDITIONAL_SERVICE":
                    default:
                        accountHelper.disableAdditionalService(accountService);
                }
                chargeRequestItem.setStatus(ChargeRequestItem.Status.SKIPPED);
            } else {
                chargeRequestItem.setStatus(ChargeRequestItem.Status.ERROR);
            }
        }
        if (dailyCost.compareTo(BigDecimal.ZERO) > 0) {
            if (accountHelper.getBalance(account).compareTo(BigDecimal.ZERO) < 0)
                accountHelper.setCreditActivationDateIfNotSet(account);
            // Если были списания, то отправить уведомления
            accountNotificationHelper.sendNotificationsRemainingDays(account, dailyCost);
        }

        if (chargeRequest.getChargeRequests().stream().anyMatch(chargeRequestItem -> chargeRequestItem.getStatus() == ChargeRequestItem.Status.ERROR)) {
            chargeRequest.setStatus(ChargeRequestItem.Status.ERROR);
        } else {
            chargeRequest.setStatus(ChargeRequestItem.Status.CHARGED);
        }

        chargeRequestManager.save(chargeRequest);

        return ChargeResult.success();
    }
    /*
     *  Выключает аккаунт
     *  пишет в статистику причину о нехватке средств
     *  отправляет письмо
     */
    private void disableAndNotifyAccountByReasonNotEnoughMoney(PersonalAccount account) {
        accountHelper.disableAccount(account);
        switch (account.getAccountType()) {
            case VIRTUAL_HOSTING:
            default:
                accountStatHelper.add(account, AccountStatType.VIRTUAL_HOSTING_ACC_OFF_NOT_ENOUGH_MONEY);
        }
        accountNotificationHelper.sendMailForDeactivatedAccount(account);
    }
}
