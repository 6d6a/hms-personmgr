package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;

import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountSendNotificationsRemainingDaysEvent;
import ru.majordomo.hms.personmgr.exception.ChargeException;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;

@Service
public class PaymentChargesProcessorService {
    private final static Logger logger = LoggerFactory.getLogger(PaymentChargesProcessorService.class);

    private final PersonalAccountManager accountManager;
    private final AccountServiceRepository accountServiceRepository;
    private final AccountHelper accountHelper;
    private final ApplicationEventPublisher publisher;
    private final AccountStatHelper accountStatHelper;
    private final AccountNotificationHelper accountNotificationHelper;
    private final AccountServiceHelper accountServiceHelper;

    @Autowired
    public PaymentChargesProcessorService(
            PersonalAccountManager accountManager,
            AccountServiceRepository accountServiceRepository,
            AccountHelper accountHelper,
            ApplicationEventPublisher publisher,
            AccountStatHelper accountStatHelper,
            AccountServiceHelper accountServiceHelper,
            AccountNotificationHelper accountNotificationHelper
    ) {
        this.accountManager = accountManager;
        this.accountServiceRepository = accountServiceRepository;
        this.accountHelper = accountHelper;
        this.publisher = publisher;
        this.accountStatHelper = accountStatHelper;
        this.accountNotificationHelper = accountNotificationHelper;
        this.accountServiceHelper = accountServiceHelper;
    }

    public void processCharge(String paymentAccountName) {
        PersonalAccount account = accountManager.findByName(paymentAccountName);
        this.processingDailyServices(account);
    }

    public Boolean processingDailyServices(PersonalAccount account) {
        Boolean success = this.chargeDailyServicesAndDisableItIfChargeFail(account);
        /*
         *  TODO нужно добавить afterProcessing для попытки включения выключенных по причине нехватки средств услуг
         */
        return success;
    }

    public Boolean chargeDailyServicesAndDisableItIfChargeFail(PersonalAccount account) {
        //Не списываем с неактивных аккаунтов
        if (!account.isActive()) { return false; }

        LocalDateTime chargeDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        List<AccountService> accountServices = accountServiceHelper.getDaylyServicesToCharge(account, chargeDate);
        //Если списывать нечего
        if (accountServices.isEmpty()) { return true; }

        //сортируем в порядке убывания paymentService.chargePriority
        //в начало попадет сервис с тарифом
        accountServices.sort(AccountService.ChargePriorityComparator);

        BigDecimal daylyCost = BigDecimal.ZERO;
        for(AccountService accountService: accountServices) {
            Boolean chargeResult = makeCharge(accountService);
            if (chargeResult) {
                daylyCost = daylyCost.add(accountService.getCost());
            } else {
                switch (accountServiceHelper.getPaymentServiceType(accountService)) {
                    case "PLAN":
                        disableAndNotifyAccountByReasonNotEnoughMoney(account);
                        return false;
                    case "ADDITIONAL_SERVICE":
                    default:
                        accountHelper.disableAdditionalService(accountService);
                }
            }
        }
        if (daylyCost.compareTo(BigDecimal.ZERO) > 0) {
            if (accountHelper.getBalance(account).compareTo(BigDecimal.ZERO) < 0)
                accountHelper.setCreditActivationDateIfNotSet(account);
            // Если были списания, то отправить уведомления
            HashMap<String, Object> params = new HashMap<>();
            params.put("daylyCost", daylyCost);
            publisher.publishEvent(new AccountSendNotificationsRemainingDaysEvent(account, params));
        }
        return true;
    }

    /*
        результат списания за услугу, полученный от finansier
        Значение по-умолчанию true, на случай если
        списания не было, значит успешно и выключать услугу не надо
    */
    private boolean makeCharge(AccountService accountService) {
        Integer daysInCurrentMonth = LocalDateTime.now().toLocalDate().lengthOfMonth();
        BigDecimal cost = BigDecimal.ZERO;
        switch (accountService.getPaymentService().getPaymentType()) {
            case MONTH:
                cost = accountService.getCost().divide(BigDecimal.valueOf(daysInCurrentMonth), 4, BigDecimal.ROUND_HALF_UP);
                break;
            case DAY:
                cost = accountService.getCost();
                break;
        }

        if (cost.compareTo(BigDecimal.ZERO) == 0) { return true; }

        LocalDateTime chargeDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        PersonalAccount account = accountManager.findOne(accountService.getPersonalAccountId());
        Boolean forceCharge = accountHelper.hasActiveCredit(account);
        boolean success = false;

        if (cost.compareTo(BigDecimal.ZERO) > 0) {
            SimpleServiceMessage response = null;

            try {
                logger.info("Send charge request in fin for PersonalAccount: " + account.getAccountId()
                        + " name: " + account.getName()
                        + " for date: " + chargeDate.format(DateTimeFormatter.ISO_DATE_TIME)
                        + " cost: " + cost
                );
                response = accountHelper.charge(account, accountService.getPaymentService(), cost, forceCharge);
            } catch (ChargeException e) {
                logger.debug("Error. Charge Processor returned ChargeException for service: " + accountService.toString());
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Exception in ru.majordomo.hms.personmgr.service.PaymentChargesProcessorService.makeCharge " + e.getMessage());
                return false;
            }

            if (response != null && response.getParam("success") != null && ((boolean) response.getParam("success"))) {
                accountService.setLastBilled(chargeDate);
                accountServiceRepository.save(accountService);
                success = true;
                logger.debug("Success. Charge Processor returned true fo service: " + accountService.toString());
            } else {
                logger.debug("Error. Charge Processor returned false for service: " + accountService.toString());
            }
        }
        return success;
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
