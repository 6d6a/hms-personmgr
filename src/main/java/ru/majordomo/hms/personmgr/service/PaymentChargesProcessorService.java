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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountDeactivatedSendMailEvent;
import ru.majordomo.hms.personmgr.event.account.AccountNotifyRemainingDaysEvent;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.account.AccountStat;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.AccountStatRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.personmgr.service.AccountStatHelper;

@Service
public class PaymentChargesProcessorService {
    private final static Logger logger = LoggerFactory.getLogger(PaymentChargesProcessorService.class);

    private final PersonalAccountManager accountManager;
    private final AccountServiceRepository accountServiceRepository;
    private final AccountStatRepository accountStatRepository;
    private final AccountHelper accountHelper;
    private final ApplicationEventPublisher publisher;
    private final PlanRepository planRepository;
    private final AccountStatHelper accountStatHelper;
    private final AccountNotificationHelper accountNotificationHelper;

    @Autowired
    public PaymentChargesProcessorService(
            PersonalAccountManager accountManager,
            AccountServiceRepository accountServiceRepository,
            AccountStatRepository accountStatRepository,
            AccountHelper accountHelper,
            ApplicationEventPublisher publisher,
            PlanRepository planRepository,
            AccountStatHelper accountStatHelper,
            AccountNotificationHelper accountNotificationHelper
    ) {
        this.accountManager = accountManager;
        this.accountServiceRepository = accountServiceRepository;
        this.accountHelper = accountHelper;
        this.accountStatRepository = accountStatRepository;
        this.publisher = publisher;
        this.planRepository = planRepository;
        this.accountStatHelper = accountStatHelper;
        this.accountNotificationHelper = accountNotificationHelper;
    }

    public void processCharge(String paymentAccountName) {
        PersonalAccount account = accountManager.findByName(paymentAccountName);
        this.processCharge(account);
    }

    public void processCharge(PersonalAccount account) {

        if (account.isActive()) {

            LocalDateTime chargeDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

            logger.debug("processing charges for PersonalAccount: " + account.getAccountId()
                    + " name: " + account.getName()
                    + " for date: " + chargeDate.format(DateTimeFormatter.ISO_DATE_TIME));

            logger.debug("processing monthly charge for PersonalAccount: " + account.getAccountId()
                    + " name: " + account.getName()
                    + " for date: " + chargeDate.format(DateTimeFormatter.ISO_DATE_TIME));

            List<AccountService> accountServices = account.getServices();

            logger.debug("accountServices : " + accountServices);

            Integer daysInCurrentMonth = chargeDate.toLocalDate().lengthOfMonth();
            BigDecimal balance = accountHelper.getBalance(account);
            BigDecimal dailyCost = BigDecimal.ZERO;

            for (AccountService accountService : accountServices) {
                if (accountService.getPaymentService() == null) {
                    logger.error("accountService.getPaymentService() == null");
    //                throw new ChargeException("accountService.getPaymentService() == null");
                }

                if (accountService.isEnabled()
                        && accountService.getPaymentService() != null
                        && (accountService.getLastBilled() == null
                        || accountService.getLastBilled().isBefore(chargeDate)
                        || accountService.getLastBilled().isEqual(chargeDate)))
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
                                // Выключаем аккаунт, если срок кредита истёк
                                accountHelper.switchAccountResources(account, false);
                                accountStatHelper.add(account, AccountStatType.VIRTUAL_HOSTING_ACC_OFF_NOT_ENOUGH_MONEY);
                                accountNotificationHelper.sendMailForDeactivatedAccount(account);

                            } else {
                                forceCharge = true;
                            }
                        }
                    }

                    switch (accountService.getPaymentService().getPaymentType()) {
                        case MONTH:
                            cost = accountService.getCost().divide(BigDecimal.valueOf(daysInCurrentMonth), 4, BigDecimal.ROUND_HALF_UP);
                            this.makeCharge(account, accountService, cost, chargeDate, forceCharge);
                            dailyCost = dailyCost.add(cost);
                            break;
                        case DAY:
                            cost = accountService.getCost();
                            this.makeCharge(account, accountService, cost, chargeDate, forceCharge);
                            dailyCost = dailyCost.add(cost);
                            break;
                    }
                }
            }

            //Если изначального баланса не хватило для списания
            if ((balance.subtract(dailyCost).compareTo(BigDecimal.ZERO)) < 0) {
                if (!account.isCredit()) {
                    accountHelper.switchAccountResources(account, false);
                    accountStatHelper.add(account, AccountStatType.VIRTUAL_HOSTING_ACC_OFF_NOT_ENOUGH_MONEY);
                    accountNotificationHelper.sendMailForDeactivatedAccount(account);
                } else if (account.getCreditActivationDate() == null) {
                    accountManager.setCreditActivationDate(account.getId(), LocalDateTime.now());
                }
            }


            if (dailyCost.compareTo(BigDecimal.ZERO) > 0) {
                Integer remainingDays = (balance.divide(dailyCost, 0, BigDecimal.ROUND_DOWN)).intValue() - 1;

                if (account.getNotifyDays() > 0 &&
                        remainingDays > 0 &&
                        remainingDays <= account.getNotifyDays() &&
                        account.isActive() &&
                        account.hasNotification(MailManagerMessageType.EMAIL_REMAINING_DAYS)
                        ) {
                    //Уведомление об окончании средств
                    Map<String, Integer> params = new HashMap<>();
                    params.put("remainingDays", remainingDays);

                    publisher.publishEvent(new AccountNotifyRemainingDaysEvent(account, params));
                }
            }

        }
    }

    private void makeCharge(PersonalAccount paymentAccount, AccountService accountService, BigDecimal cost, LocalDateTime chargeDate, Boolean forceCharge) {
        if (cost.compareTo(BigDecimal.ZERO) == 1) {
            SimpleServiceMessage response = null;

            try {
                response = accountHelper.charge(paymentAccount, accountService.getPaymentService(), cost, forceCharge);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Exception in ru.majordomo.hms.personmgr.service.PaymentChargesProcessorService.makeCharge " + e.getMessage());
            }

            if (response != null && response.getParam("success") != null && !((boolean) response.getParam("success"))) {
                logger.debug("Error. Charge Processor returned false fo service: " + accountService.toString());
            } else {
                accountService.setLastBilled(chargeDate);
                accountServiceRepository.save(accountService);
                logger.debug("Success. Charge Processor returned true fo service: " + accountService.toString());
            }
        }
    }
}
