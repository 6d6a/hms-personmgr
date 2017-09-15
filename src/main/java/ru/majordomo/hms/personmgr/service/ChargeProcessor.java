package ru.majordomo.hms.personmgr.service;

import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.ChargeResult;
import ru.majordomo.hms.personmgr.event.account.AccountSendNotificationsRemainingDaysEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessChargeEvent;
import ru.majordomo.hms.personmgr.manager.BatchJobManager;
import ru.majordomo.hms.personmgr.manager.ChargeRequestManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequest;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequestItem;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;

@Service
public class ChargeProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ChargeRequestManager chargeRequestManager;
    private final PersonalAccountManager accountManager;
    private final AccountServiceRepository accountServiceRepository;
    private final AccountServiceHelper accountServiceHelper;
    private final AccountHelper accountHelper;
    private final AccountNotificationHelper accountNotificationHelper;
    private final AccountStatHelper accountStatHelper;
    private final Charger charger;
    private final ApplicationEventPublisher publisher;
    private final BatchJobManager batchJobManager;

    public ChargeProcessor(
            ChargeRequestManager chargeRequestManager,
            PersonalAccountManager accountManager,
            AccountServiceRepository accountServiceRepository,
            AccountServiceHelper accountServiceHelper,
            AccountHelper accountHelper,
            AccountNotificationHelper accountNotificationHelper,
            AccountStatHelper accountStatHelper,
            Charger charger,
            ApplicationEventPublisher publisher,
            BatchJobManager batchJobManager
    ) {
        this.chargeRequestManager = chargeRequestManager;
        this.accountManager = accountManager;
        this.accountServiceRepository = accountServiceRepository;
        this.accountServiceHelper = accountServiceHelper;
        this.accountHelper = accountHelper;
        this.accountNotificationHelper = accountNotificationHelper;
        this.accountStatHelper = accountStatHelper;
        this.charger = charger;
        this.publisher = publisher;
        this.batchJobManager = batchJobManager;
    }

    @SchedulerLock(name="processCharges")
    public void processCharges(LocalDate chargeDate, String batchJobId) {
        logger.info("Started processCharges emitting events for " + chargeDate);

        int needToProcess = chargeRequestManager.countNeedToProcessChargeRequests(chargeDate);
        batchJobManager.setCount(batchJobId, needToProcess);
        batchJobManager.setNeedToProcess(batchJobId, needToProcess);

        List<ChargeRequest> chargeRequests = chargeRequestManager.getNeedToProcessChargeRequests(chargeDate);

        logger.info("processCharges found " + chargeRequests.size() + " ChargeRequests ");

        batchJobManager.setProcessingState(batchJobId);

        chargeRequests.forEach(chargeRequest -> publisher.publishEvent(new ProcessChargeEvent(chargeRequest.getId(), batchJobId)));

        logger.info("Ended processCharges emitting events for " + chargeDate);
    }

    @SchedulerLock(name="processErrorCharges")
    public void processErrorCharges(LocalDate chargeDate, String batchJobId) {
        logger.info("Started processErrorCharges emitting events for " + chargeDate);

        int needToProcess = chargeRequestManager.countChargeRequestsWithErrors(chargeDate);
        batchJobManager.setCount(batchJobId, needToProcess);
        batchJobManager.setNeedToProcess(batchJobId, needToProcess);

        List<ChargeRequest> chargeRequests = chargeRequestManager.getChargeRequestsWithErrors(chargeDate);

        logger.info("processErrorCharges found " + chargeRequests.size() + " ChargeRequests with errors");

        batchJobManager.setProcessingState(batchJobId);

        chargeRequests.forEach(chargeRequest -> publisher.publishEvent(new ProcessChargeEvent(chargeRequest.getId(), batchJobId)));

        logger.info("Ended processErrorCharges emitting events for " + chargeDate);
    }

    public ChargeResult processChargeRequest(ChargeRequest chargeRequest) {
        PersonalAccount account = accountManager.findOne(chargeRequest.getPersonalAccountId());

        BigDecimal dailyCost = BigDecimal.ZERO;
        for(ChargeRequestItem chargeRequestItem : chargeRequest.getChargeRequests()
                .stream()
                .filter(chargeRequestItem ->
                        chargeRequestItem.getStatus() == ChargeRequestItem.Status.NEW ||
                        chargeRequestItem.getStatus() == ChargeRequestItem.Status.ERROR
                )
                .collect(Collectors.toSet())) {
            AccountService accountService =  accountServiceRepository.findOne(chargeRequestItem.getAccountServiceId());

            ChargeResult chargeResult = charger.makeCharge(accountService, chargeRequest.getChargeDate());
            if (chargeResult.isSuccess()) {
                dailyCost = dailyCost.add(accountServiceHelper.getDailyCostForService(accountService, chargeRequest.getChargeDate()));
                chargeRequestItem.setStatus(ChargeRequestItem.Status.CHARGED);
            } else if (!chargeResult.isSuccess() && !chargeResult.isGotException()) {
                switch (accountServiceHelper.getPaymentServiceType(accountService)) {
                    case "PLAN":
                        try {
                            disableAndNotifyAccountByReasonNotEnoughMoney(account);
                        } catch (Exception e) {
                            e.printStackTrace();
                            chargeRequest.setStatus(ChargeRequestItem.Status.ERROR);
                        }
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
            try {
                if (accountHelper.getBalance(account).compareTo(BigDecimal.ZERO) < 0) {
                    accountHelper.setCreditActivationDateIfNotSet(account);
                }
            } catch (Exception e) {
                e.printStackTrace();
                chargeRequest.setStatus(ChargeRequestItem.Status.ERROR);
            }
            // Если были списания, то отправить уведомления
            HashMap<String, Object> params = new HashMap<>();
            params.put("daylyCost", dailyCost);
            publisher.publishEvent(new AccountSendNotificationsRemainingDaysEvent(account, params));
        }

        if (chargeRequest.getChargeRequests().stream().anyMatch(chargeRequestItem -> chargeRequestItem.getStatus() == ChargeRequestItem.Status.ERROR)) {
            chargeRequest.setStatus(ChargeRequestItem.Status.ERROR);
        } else {
            chargeRequest.setStatus(ChargeRequestItem.Status.CHARGED);
        }

        chargeRequestManager.save(chargeRequest);

        return ChargeResult.success();
    }

    /**
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
        accountNotificationHelper.sendMailForDeactivatedAccount(account, LocalDate.now());
    }
}
