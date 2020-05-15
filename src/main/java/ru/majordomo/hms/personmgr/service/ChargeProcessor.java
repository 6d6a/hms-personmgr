package ru.majordomo.hms.personmgr.service;

import net.javacrumbs.shedlock.core.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.ChargeResult;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.event.account.*;
import ru.majordomo.hms.personmgr.manager.BatchJobManager;
import ru.majordomo.hms.personmgr.manager.ChargeRequestManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequest;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequestItem;
import ru.majordomo.hms.personmgr.model.charge.Status;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.DiscountedService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;

import static ru.majordomo.hms.personmgr.common.Constants.ADVANCED_BACKUP_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.Constants.ANTI_SPAM_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.Constants.LONG_LIFE_RESOURCE_ARCHIVE_SERVICE_ID;

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
            DiscountServiceHelper discountServiceHelper
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
        boolean hasActiveCreditAfterCharges = false;
        boolean additionalServicesDisabled = false;
        List<AccountService> accountServices = new ArrayList<>();

        Set<ChargeRequestItem> itemSet = chargeRequest.getChargeRequests().stream()
                .filter(item -> item.getStatus() == Status.NEW || item.getStatus() == Status.ERROR)
                .collect(Collectors.toSet());

        for(ChargeRequestItem chargeRequestItem : itemSet) {

            AccountService accountService =  accountServiceRepository.findById(chargeRequestItem.getAccountServiceId())
                    .orElse(null);

            //TODO такое происходит если была смена тарифа или покупка абонемента
            //При смене тарифа надо пересоздать ChargeRequest или заменить сервис тарифа на новый
            if (accountService == null) {
                chargeRequestItem.setStatus(Status.SKIPPED);
                chargeRequestItem.setMessage("Услуга не найдена на аккаунте");
                continue;
            }

            //TODO перенести эти 100%-е скидки к остальным
            DiscountedService discountedService = discountServiceHelper.getDiscountedService(account.getDiscounts(), accountService);

            BigDecimal costToCharge = chargeRequestItem.getAmount();
            if (discountedService != null) {
                costToCharge = discountedService.getCost();
            }

            if (accountService.getPaymentService().getPaymentType() == ServicePaymentType.ONE_TIME || costToCharge.compareTo(BigDecimal.ZERO) <= 0) {
                chargeRequestItem.setStatus(Status.SKIPPED);
            } else {
                ChargeResult chargeResult = charger.makeCharge(accountService, chargeRequest.getChargeDate(), costToCharge);
                if (chargeResult.isSuccess()) {
                    dailyCost = dailyCost.add(costToCharge);
                    chargeRequestItem.setStatus(Status.CHARGED);
                    accountServices.add(accountService);
                } else if (!chargeResult.isSuccess() && !chargeResult.isGotException()) {
                    switch (accountServiceHelper.getPaymentServiceType(accountService)) {
                        case "PLAN":
                            try {
                                processNotEnoughMoneyPersonalAccount(account);
                            } catch (Exception e) {
                                logger.error(e.getMessage());
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
                            additionalServicesDisabled = true;
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
                    hasActiveCreditAfterCharges = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                chargeRequest.setStatus(Status.ERROR);
                chargeRequest.setMessage(e.getMessage());
                chargeRequest.setException(e.getClass().getName());
            }
            // Если были списания, то отправить уведомления
            notifyAccountRemainingDays(account, dailyCost, hasActiveCreditAfterCharges, accountServices, additionalServicesDisabled);
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
     *  Если тариф архивный и дешевле 245 рублей в месяц, то тариф меняется на безлимитный
     *  Выключает аккаунт
     *  пишет в статистику причину о нехватке средств
     *  отправляет письмо
     */
    private void processNotEnoughMoneyPersonalAccount(PersonalAccount account) {
        if (accountHelper.getPlan(account).isArchival()) {
            logger.info("changeArchivalPlanToActive(PersonalAccount(id='" + account.getId() + "'))");
            accountHelper.changeArchivalPlanToActive(account);
        }

        accountHelper.disableAccount(account);
        accountStatHelper.notMoney(account);

        boolean hasExpiredCreditBeforeCharges = account.getCreditActivationDate() != null
                && account.getCreditActivationDate().isBefore(
                LocalDateTime
                        .now()
                        .minus(Period.parse(account.getCreditPeriod()))
        );

        if (hasExpiredCreditBeforeCharges) {
            //у аккаунта просроченный кредит
            publisher.publishEvent(new AccountDeactivatedWithExpiredCreditSendMailEvent(account.getId()));
        } else {
            publisher.publishEvent(new AccountDeactivatedSendMailEvent(account.getId()));
        }
    }

    private void notifyAccountRemainingDays(
            PersonalAccount account,
            BigDecimal dailyCost,
            boolean hasActiveCreditAfterCharges,
            List<AccountService> accountServices,
            boolean additionalServicesDisabled
    ) {
        // Уведомление о заканчивающихся средствах отправляются только активным аккаунтам или тем, у кого есть списания
        if (!account.isActive() || dailyCost.compareTo(BigDecimal.ZERO) == 0) { return;}

        boolean hasAnyCreditBeforeCharges = account.getCreditActivationDate() != null;
        boolean hasExpiredCreditBeforeCharges = hasAnyCreditBeforeCharges
                && account.getCreditActivationDate().isBefore(
                LocalDateTime
                        .now()
                        .minus(Period.parse(account.getCreditPeriod()))
        );
        BigDecimal balance = accountHelper.getBalance(account);
        int remainingDays = (balance.divide(dailyCost, 0, BigDecimal.ROUND_DOWN)).intValue();
        int remainingCreditDays = accountNotificationHelper.getRemainingDaysCreditPeriod(account);
        boolean hasActiveAbonement = accountHelper.hasActiveAbonement(account.getId());

        if (hasActiveAbonement) {
            List<Integer> creditNotifyDays = Arrays.asList(7, 5, 3, 1);

            if (hasActiveCreditAfterCharges) {
                //на данный момент кредит включен
                if (!hasAnyCreditBeforeCharges) {
                    //кредит на доп.услуги был только что включен
                    publisher.publishEvent(new AccountCreditJustActivatedWithHostingAbonementSendMailEvent(account.getId()));
                } else if (creditNotifyDays.contains(remainingCreditDays)) {
                    //если кредит подходит к концу
                    publisher.publishEvent(new AccountCreditExpiringWithHostingAbonementSendMailEvent(account.getId()));
                }
            } else {
                if (additionalServicesDisabled) {
                    if (hasExpiredCreditBeforeCharges) {
                        //если кредит просрочен и отключены доп.услуги
                        publisher.publishEvent(new AccountCreditExpiredWithHostingAbonementSendMailEvent(account.getId()));
                    } else if (accountServices.stream()
                            .anyMatch(accountService -> !accountService.getPaymentService().getOldId().equals(ANTI_SPAM_SERVICE_ID) &&
                                    !accountService.getPaymentService().getOldId().equals(LONG_LIFE_RESOURCE_ARCHIVE_SERVICE_ID) &&
                                    !accountService.getPaymentService().getOldId().equals(ADVANCED_BACKUP_SERVICE_ID))) {
                        //если были отключены какие-то услуги кроме антиспама, расширенных бекпов и вечных архивов
                        publisher.publishEvent(new AccountServicesDisabledWithHostingAbonementSendMailEvent(account.getId()));
                    }
                } else if (accountServices.stream()
                        .anyMatch(accountService -> !accountService.getPaymentService().getOldId().equals(ANTI_SPAM_SERVICE_ID) &&
                                !accountService.getPaymentService().getOldId().equals(LONG_LIFE_RESOURCE_ARCHIVE_SERVICE_ID) &&
                                !accountService.getPaymentService().getOldId().equals(ADVANCED_BACKUP_SERVICE_ID))) {
                    //если заканчиваются какие-то доп.услуги кроме антиспама, расширенных бекпов и вечных архивов
                    List<Integer> days = Arrays.asList(5, 3, 2, 1);

                    if (days.contains(remainingDays)) {
                        publisher.publishEvent(new AccountServicesExpiringWithHostingAbonementSendMailEvent(account.getId(), remainingDays));
                    }
                }
            }
        } else {
            if (hasActiveCreditAfterCharges) {
                //на данный момент кредит включен
                if (!hasAnyCreditBeforeCharges) {
                    //кредит на все услуги был только что включен
                    publisher.publishEvent(new AccountCreditJustActivatedSendMailEvent(account.getId()));
                } else {
                    List<Integer> creditNotifyDays = Arrays.asList(7, 5, 3, 1);

                    if (creditNotifyDays.contains(remainingCreditDays)) {
                        //если кредит подходит к концу
                        publisher.publishEvent(new AccountCreditExpiringSendMailEvent(account.getId()));
                    }
                }
            } else {
                List<Integer> days = Arrays.asList(7, 5, 3, 2, 1);

                if (days.contains(remainingDays)) {
                    //если заканчиваются средства на все услуги
                    publisher.publishEvent(new AccountServicesExpiringSendMailEvent(account.getId(), remainingDays));
                }
            }
        }

        //Отправим смс тем, у кого подключена услуга
        if (Arrays.asList(5, 1).contains(remainingDays) &&
                accountNotificationHelper.isSubscribedToSmsType(
                        account,
                        MailManagerMessageType.SMS_REMAINING_DAYS)
        ) {
            publisher.publishEvent(new AccountSendSmsNotificationRemainingDaysEvent(account.getId(), remainingDays));
        }
        if (Arrays.asList(5, 1).contains(remainingDays) &&
                accountNotificationHelper.isSubscribedToTelegramType(
                        account,
                        MailManagerMessageType.TELEGRAM_REMAINING_DAYS)
        ) {
            publisher.publishEvent(new AccountSendTelegramNotificationRemainingDaysEvent(account.getId(), remainingDays));
        }
    }
}
