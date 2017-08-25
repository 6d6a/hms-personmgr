package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountCheckQuotaEvent;
import ru.majordomo.hms.personmgr.event.account.AccountNotifyRemainingDaysEvent;
import ru.majordomo.hms.personmgr.exception.ChargeException;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;

import static ru.majordomo.hms.personmgr.common.Constants.ADDITIONAL_QUOTA_100_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.Constants.ANTI_SPAM_SERVICE_ID;

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
        this.processCharge(account);
    }

    public Boolean processCharge(PersonalAccount account) {

        Boolean success = true;

        if (account.isActive()) {

            Boolean hasActiveAbonement = accountHelper.hasActiveAbonement(account);
            LocalDateTime chargeDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

            logger.info("processing charges for PersonalAccount: " + account.getAccountId()
                    + " name: " + account.getName()
                    + " for date: " + chargeDate.format(DateTimeFormatter.ISO_DATE_TIME));

            List<AccountService> accountServices = account.getServices();

            logger.debug("accountServices : " + accountServices);

            Integer daysInCurrentMonth = chargeDate.toLocalDate().lengthOfMonth();
            BigDecimal balance = accountHelper.getBalance(account);
            BigDecimal dailyCost = BigDecimal.ZERO;

            for (AccountService accountService : accountServices) {
                PaymentService paymentService = accountService.getPaymentService();
                if (paymentService == null) {
                    logger.error("accountService.getPaymentService() == null");
    //                throw new ChargeException("accountService.getPaymentService() == null");
                }

                if (accountService.isEnabled()
                        && paymentService != null
                        && (accountService.getLastBilled() == null
                        || accountService.getLastBilled().isBefore(chargeDate)))
                {
                    BigDecimal cost;
                    Boolean forceCharge = false;

                    if (account.isCredit()) {
                        //Если у аккаунта подключен кредит
                        LocalDateTime creditActivationDate = account.getCreditActivationDate();

                        //Проверяем что дата активации выставлена
                        if (creditActivationDate == null) {
                            // Далее дата активация выставляется в null, только при платеже, который вывел аккаунт из минуса
                            forceCharge = true;
                        } else {
                            // Проверяем сколько он уже пользуется
                            if ( creditActivationDate.isBefore(LocalDateTime.now().minus(Period.parse(account.getCreditPeriod()))) ) {
                                success = false;
                                if (!hasActiveAbonement) {
                                    // Если нет абонемента и срок кредита истёк, выключаем аккаунт
                                    disableAccountSaveStatSendSms(account);
                                }
                            } else {
                                forceCharge = true;
                            }
                        }
                    }

                    //Значение по-умолчанию true, на случай если
                    //списания не было, значит успешно и выключать услугу не надо
                    boolean chargeResult = true;
                    switch (accountService.getPaymentService().getPaymentType()) {
                        case MONTH:
                            cost = accountService.getCost().divide(BigDecimal.valueOf(daysInCurrentMonth), 4, BigDecimal.ROUND_HALF_UP);
                            chargeResult = this.makeCharge(account, accountService, cost, chargeDate, forceCharge);
                            dailyCost = dailyCost.add(cost);
                            break;
                        case DAY:
                            cost = accountService.getCost();
                            chargeResult = this.makeCharge(account, accountService, cost, chargeDate, forceCharge);
                            dailyCost = dailyCost.add(cost);
                            break;
                    }

                    //Если есть активный абонемент и списание неудачное, нужно выключить услугу и послать апдейты на ресурсы
                    if (hasActiveAbonement && !chargeResult) {
                        disableAdditionalService(account, accountService);
                    }
                }
            }

            if (dailyCost.compareTo(BigDecimal.ZERO) > 0) {

                //Если изначального баланса не хватило для списания
                if ((balance.subtract(dailyCost).compareTo(BigDecimal.ZERO)) < 0) {
                    if (!account.isCredit()) {
                        success = false;
                        if (!hasActiveAbonement) {
                            //выключаем аккаунт без абонемента
                            disableAccountSaveStatSendSms(account);
                        }
                    } else if (account.getCreditActivationDate() == null) {
                        accountManager.setCreditActivationDate(account.getId(), LocalDateTime.now());
                    }
                }

                //Уведомления
                Integer remainingDays = (balance.divide(dailyCost, 0, BigDecimal.ROUND_DOWN)).intValue() - 1;
                if (remainingDays > 0 && account.isActive()) {
                    //Отправляем техническое уведомление на почту об окончании средств за 7, 5, 3, 2, 1 дней
                    if (Arrays.asList(7, 5, 3, 2, 1).contains(remainingDays)) {
                        //Уведомление об окончании средств
                        Map<String, Integer> params = new HashMap<>();
                        params.put("remainingDays", remainingDays);

                        publisher.publishEvent(new AccountNotifyRemainingDaysEvent(account, params));
                    }
                    //Отправим смс тем, у кого подключена услуга
                    if (Arrays.asList(5, 3, 1).contains(remainingDays)) {
                        if (accountNotificationHelper.hasActiveSmsNotificationsAndMessageType(account, MailManagerMessageType.SMS_REMAINING_DAYS)) {
                            HashMap<String, String> parameters = new HashMap<>();
                            parameters.put("remaining_days", Utils.pluralizef("остался %d день", "осталось %d дня", "осталось %d дней", remainingDays));
                            parameters.put("client_id", account.getAccountId());
                            accountNotificationHelper.sendSms(account, "MajordomoRemainingDays", 10, parameters);
                        }
                    }
                }

            }


        }

        return success;
    }

    private boolean makeCharge(PersonalAccount paymentAccount, AccountService accountService, BigDecimal cost, LocalDateTime chargeDate, Boolean forceCharge) {
        boolean success = false;
        if (cost.compareTo(BigDecimal.ZERO) == 1) {
            SimpleServiceMessage response = null;

            try {
                logger.info("Send charge request in fin for PersonalAccount: " + paymentAccount.getAccountId()
                        + " name: " + paymentAccount.getName()
                        + " for date: " + chargeDate.format(DateTimeFormatter.ISO_DATE_TIME)
                        + " cost: " + cost
                );
                response = accountHelper.charge(paymentAccount, accountService.getPaymentService(), cost, forceCharge);
            } catch (ChargeException e) {
                logger.debug("Error. Charge Processor returned ChargeException for service: " + accountService.toString());
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Exception in ru.majordomo.hms.personmgr.service.PaymentChargesProcessorService.makeCharge " + e.getMessage());
                return false;
            }

            if (response != null && response.getParam("success") != null && !((boolean) response.getParam("success"))) {
                logger.debug("Error. Charge Processor returned false for service: " + accountService.toString());
            } else {
                accountService.setLastBilled(chargeDate);
                accountServiceRepository.save(accountService);
                success = true;
                logger.debug("Success. Charge Processor returned true fo service: " + accountService.toString());
            }
        }
        return success;
    }

    private void disableAccountSaveStatSendSms(PersonalAccount account) {
        accountHelper.disableAccount(account);
        accountStatHelper.add(account, AccountStatType.VIRTUAL_HOSTING_ACC_OFF_NOT_ENOUGH_MONEY);
        accountNotificationHelper.sendMailForDeactivatedAccount(account);
    }

    private void disableAdditionalService(PersonalAccount account, AccountService accountService) {
        accountServiceHelper.disableAccountService(account, accountService.getServiceId());
        String paymentServiceOldId = accountService.getPaymentService().getOldId();
        if (paymentServiceOldId.equals(ADDITIONAL_QUOTA_100_SERVICE_ID)) {
            account.setAddQuotaIfOverquoted(false);
            publisher.publishEvent(new AccountCheckQuotaEvent(account));
//                        } else if (paymentServiceOldId.equals(ANTI_SPAM_SERVICE_ID)) {
//                            TODO надо что-нибудь отправлять в rc-user чтобы отключить защиту у ящиков
//                            В rc-user никакого параметра для этого нет, нужно добавить
//                        } else if (paymentService.getId().equals(smsPaymentService.getId())) {
//                            Для SMS достаточно выключать сервис
//                        TODO надо сделать выключение для остальных дополнительных услуг, типа доп ftp
        }
        account.setAddQuotaIfOverquoted(false);
        publisher.publishEvent(new AccountCheckQuotaEvent(account));
        accountHelper.saveHistoryForOperatorService(account, "Услуга " + accountService.getPaymentService().getName() + " отключена в связи с нехваткой средств.");
    }
}
