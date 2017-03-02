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
import java.util.Map;

import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountNotifyRemainingDaysEvent;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;

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
                logger.debug("accountService.getPaymentService() == null");
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

    private void makeCharge(PersonalAccount paymentAccount, AccountService accountService, BigDecimal cost, LocalDateTime chargeDate) {
        if (cost.compareTo(BigDecimal.ZERO) == 1) {
            SimpleServiceMessage response = null;

            try {
                response = accountHelper.charge(paymentAccount, accountService.getPaymentService(), cost);
            } catch (Exception e) {
                e.printStackTrace();
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
