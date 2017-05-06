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

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountNotifyRemainingDaysEvent;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.rc.user.resources.*;

@Service
public class PaymentChargesProcessorService {
    private final static Logger logger = LoggerFactory.getLogger(PaymentChargesProcessorService.class);

    private final PersonalAccountRepository personalAccountRepository;
    private final AccountServiceRepository accountServiceRepository;
    private final AccountHelper accountHelper;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public PaymentChargesProcessorService(
            PersonalAccountRepository personalAccountRepository,
            AccountServiceRepository accountServiceRepository,
            AccountHelper accountHelper,
            ApplicationEventPublisher publisher
    ) {
        this.personalAccountRepository = personalAccountRepository;
        this.accountServiceRepository = accountServiceRepository;
        this.accountHelper = accountHelper;
        this.publisher = publisher;
    }

    public void processCharge(String paymentAccountName) {
        PersonalAccount account = personalAccountRepository.findByName(paymentAccountName);
        this.processCharge(account);
    }

    public void processCharge(PersonalAccount account) {

        if (account.isActive()) {

            LocalDateTime chargeDate = LocalDateTime.now().minusDays(1).withHour(23).withMinute(59).withSecond(59);

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
                        || accountService.getLastBilled().isBefore(chargeDate.minusDays(1))
                        || accountService.getLastBilled().isEqual(chargeDate.minusDays(1))))
                {
                    BigDecimal cost;
                    switch (accountService.getPaymentService().getPaymentType()) {
                        case MONTH:
                            cost = accountService.getCost().divide(BigDecimal.valueOf(daysInCurrentMonth), 4, BigDecimal.ROUND_HALF_UP);
                            this.makeCharge(account, accountService, cost, chargeDate);
                            dailyCost = dailyCost.add(cost);
                            break;
                        case DAY:
                            cost = accountService.getCost();
                            this.makeCharge(account, accountService, cost, chargeDate);
                            dailyCost = dailyCost.add(cost);
                            break;
                    }
                }
            }

            //После списаний баланс отрицательный
            if ((balance.subtract(dailyCost).compareTo(BigDecimal.ZERO)) < 0) {
                if (account.isCredit()) {
                    //Если у аккаунта подключен кредит

                    LocalDateTime creditActivationDate = account.getCreditActivationDate();

                    //Проверяем что дата активации выставлена
                    if (creditActivationDate == null) {
                        // Далее дата активация выставляется в null, только при платеже, который вывел аккаунт из минуса
                        account.setCreditActivationDate(LocalDateTime.now());
                        personalAccountRepository.save(account);
                    } else {
                        // Проверяем сколько он уже пользуется
                        if ( creditActivationDate.isBefore(LocalDateTime.now().minus(Period.parse(account.getCreditPeriod()))) ) {
                            // Выклчаем аккаунт, если срок кредита истёк
                            accountHelper.switchAccountResources(account, false);
                            this.sendDisableAccMail(account);
                        }
                    }

                } else {
                    accountHelper.switchAccountResources(account, false);
                    this.sendDisableAccMail(account);
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

    private void sendDisableAccMail(PersonalAccount account) {
        //Отправим письмо
        String email = accountHelper.getEmail(account);

        SimpleServiceMessage message = new SimpleServiceMessage();

        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam("email", email);
        message.addParam("api_name", "MajordomoVHMoneyDeactivateacc");
        message.addParam("priority", 1);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", message.getAccountId());
        parameters.put("acc_id", account.getName());

        List<Domain> domains = accountHelper.getDomains(account);
        List<String> domainNames = new ArrayList<>();
        for (Domain domain: domains) {
            domainNames.add(domain.getName());
        }

        parameters.put("domains", String.join("<br>", domainNames));

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }

    private void makeCharge(PersonalAccount paymentAccount, AccountService accountService, BigDecimal cost, LocalDateTime chargeDate) {
        if (cost.compareTo(BigDecimal.ZERO) == 1) {
            SimpleServiceMessage response = null;

            try {
                response = accountHelper.charge(paymentAccount, accountService.getPaymentService(), cost, true);
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
