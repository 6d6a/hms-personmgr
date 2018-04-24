package ru.majordomo.hms.personmgr.service;

import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.ChargeResult;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.event.account.AccountSendMailNotificationRemainingDaysEvent;
import ru.majordomo.hms.personmgr.event.account.AccountSendSmsNotificationRemainingDaysEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessChargeEvent;
import ru.majordomo.hms.personmgr.exception.NotEnoughMoneyException;
import ru.majordomo.hms.personmgr.manager.BatchJobManager;
import ru.majordomo.hms.personmgr.manager.ChargeRequestManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequest;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequestItem;
import ru.majordomo.hms.personmgr.model.charge.Status;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.AccountServiceExpiration;
import ru.majordomo.hms.personmgr.model.service.DiscountedService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountServiceExpirationRepository;
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
    private final DiscountServiceHelper discountServiceHelper;
    private final AccountServiceExpirationRepository accountServiceExpirationRepository;

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
            BatchJobManager batchJobManager,
            DiscountServiceHelper discountServiceHelper,
            AccountServiceExpirationRepository accountServiceExpirationRepository
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
        this.discountServiceHelper = discountServiceHelper;
        this.accountServiceExpirationRepository = accountServiceExpirationRepository;
    }

    @SchedulerLock(name="processCharges")
    public void processCharges(LocalDate chargeDate, String batchJobId) {
        logger.info("Started processCharges emitting events for " + chargeDate);

        int needToProcess = chargeRequestManager.countNeedToProcessChargeRequests(chargeDate);
        batchJobManager.setCount(batchJobId, needToProcess);
        batchJobManager.setNeedToProcess(batchJobId, needToProcess);

        List<ChargeRequest> chargeRequests = chargeRequestManager.pullNeedToProcessChargeRequests(chargeDate);

        logger.info("processCharges found " + chargeRequests.size() + " ChargeRequests ");

        batchJobManager.setStateToProcessing(batchJobId);

        chargeRequests.forEach(chargeRequest -> publisher.publishEvent(new ProcessChargeEvent(chargeRequest.getId(), batchJobId)));

        batchJobManager.setStateToFinishedIfNeeded(batchJobId);

        logger.info("Ended processCharges emitting events for " + chargeDate);
    }

    @SchedulerLock(name="processErrorCharges")
    public void processErrorCharges(LocalDate chargeDate, String batchJobId) {
        logger.info("Started processErrorCharges emitting events for " + chargeDate);

        int needToProcess = chargeRequestManager.countChargeRequestsWithErrors(chargeDate);
        batchJobManager.setCount(batchJobId, needToProcess);
        batchJobManager.setNeedToProcess(batchJobId, needToProcess);

        List<ChargeRequest> chargeRequests = chargeRequestManager.pullChargeRequestsWithErrors(chargeDate);

        logger.info("processErrorCharges found " + chargeRequests.size() + " ChargeRequests with errors");

        batchJobManager.setStateToProcessing(batchJobId);

        chargeRequests.forEach(chargeRequest -> publisher.publishEvent(new ProcessChargeEvent(chargeRequest.getId(), batchJobId)));

        batchJobManager.setStateToFinishedIfNeeded(batchJobId);

        logger.info("Ended processErrorCharges emitting events for " + chargeDate);
    }

    public ChargeResult processChargeRequest(ChargeRequest chargeRequest) {
        PersonalAccount account = accountManager.findOne(chargeRequest.getPersonalAccountId());

        BigDecimal dailyCost = BigDecimal.ZERO;
        for(ChargeRequestItem chargeRequestItem : chargeRequest.getChargeRequests()
                .stream()
                .filter(chargeRequestItem ->
                        chargeRequestItem.getStatus() == Status.NEW ||
                        chargeRequestItem.getStatus() == Status.ERROR
                )
                .collect(Collectors.toSet())) {
            AccountService accountService =  accountServiceRepository.findOne(chargeRequestItem.getAccountServiceId());

            DiscountedService discountedService = discountServiceHelper.getDiscountedService(account.getDiscounts(), accountService);

            if (discountedService != null) {
                accountService = discountedService;
            }

            if (accountService.getPaymentService().getPaymentType() == ServicePaymentType.ONE_TIME) {
                chargeRequestItem.setStatus(Status.SKIPPED);
            } else {
                ChargeResult chargeResult = charger.makeCharge(accountService, chargeRequest.getChargeDate());
                if (chargeResult.isSuccess()) {
                    dailyCost = dailyCost.add(accountServiceHelper.getDailyCostForService(accountService, chargeRequest.getChargeDate()));
                    chargeRequestItem.setStatus(Status.CHARGED);
                } else if (!chargeResult.isSuccess() && !chargeResult.isGotException()) {
                    switch (accountServiceHelper.getPaymentServiceType(accountService)) {
                        case "PLAN":
                            try {
                                disableAndNotifyAccountByReasonNotEnoughMoney(account);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            chargeRequestItem.setStatus(Status.SKIPPED);
                            chargeRequestItem.setMessage(chargeResult.getMessage());
                            chargeRequest.setStatus(Status.SKIPPED);
                            chargeRequestItem.setMessage(chargeResult.getMessage());

                            chargeRequestManager.save(chargeRequest);

                            return chargeResult;
                        case "ADDITIONAL_SERVICE":
                        default:
                            accountHelper.disableAdditionalService(accountService);
                    }
                    chargeRequestItem.setStatus(Status.SKIPPED);
                    chargeRequestItem.setMessage(chargeResult.getMessage());
                } else {
                    chargeRequestItem.setStatus(Status.ERROR);
                    chargeRequestItem.setMessage(chargeResult.getMessage());
                    chargeRequestItem.setException(chargeResult.getException());
                }
            }
        }

        if (dailyCost.compareTo(BigDecimal.ZERO) > 0) {
            try {
                if (accountHelper.getBalance(account).compareTo(BigDecimal.ZERO) < 0) {
                    accountHelper.setCreditActivationDateIfNotSet(account);
                }
            } catch (Exception e) {
                e.printStackTrace();
                chargeRequest.setStatus(Status.ERROR);
                chargeRequest.setMessage(e.getMessage());
                chargeRequest.setException(e.getClass().getName());
            }
            // Если были списания, то отправить уведомления
            notifyAccountRemainingDays(account, dailyCost);
        }

        if (chargeRequest.getChargeRequests().stream().anyMatch(chargeRequestItem -> chargeRequestItem.getStatus() == Status.ERROR)) {
            chargeRequest.setStatus(Status.ERROR);
        } else {
            chargeRequest.setStatus(Status.FINISHED);
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
                accountStatHelper.add(account.getId(), AccountStatType.VIRTUAL_HOSTING_ACC_OFF_NOT_ENOUGH_MONEY);
        }
        accountNotificationHelper.sendMailForDeactivatedAccount(account, LocalDate.now());
    }

    private void notifyAccountRemainingDays(PersonalAccount account, BigDecimal dailyCost) {
        // Уведомление о заканчивающихся средствах отправляются только активным аккаунтам или тем, у кого есть списания
        if (!account.isActive() || dailyCost.compareTo(BigDecimal.ZERO) == 0) { return;}

        BigDecimal balance = accountHelper.getBalance(account);
        int remainingDays = (balance.divide(dailyCost, 0, BigDecimal.ROUND_DOWN)).intValue();
        int remainingCreditDays = accountNotificationHelper.getRemainingDaysCreditPeriod(account);
        boolean hasActiveAbonement = accountHelper.hasActiveAbonement(account.getId());
        boolean hasActiveCredit = accountHelper.hasActiveCredit(account);
        boolean balanceIsPositive = balance.compareTo(BigDecimal.ZERO) > 0;

        List<Integer> days = Arrays.asList(7, 5, 3, 2, 1);

        if (days.contains(remainingDays) || days.contains(remainingCreditDays)) {
            publisher.publishEvent(
                    new AccountSendMailNotificationRemainingDaysEvent(
                            account.getId(),
                            remainingDays,
                            remainingCreditDays,
                            hasActiveAbonement,
                            hasActiveCredit,
                            balanceIsPositive,
                            balance
                    )
            );
        }

        //        Отправим смс тем, у кого подключена услуга
        if (Arrays.asList(5, 1).contains(remainingDays) &&
                accountNotificationHelper.isSubscribedToSmsType(
                        account,
                        MailManagerMessageType.SMS_REMAINING_DAYS)) {
            publisher.publishEvent(new AccountSendSmsNotificationRemainingDaysEvent(account.getId(), remainingDays));
        }
    }
}
