package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
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

    @Autowired
    public PaymentChargesProcessorService(
            PersonalAccountRepository personalAccountRepository,
            AccountServiceRepository accountServiceRepository,
            AccountHelper accountHelper
    ) {
        this.personalAccountRepository = personalAccountRepository;
        this.accountServiceRepository = accountServiceRepository;
        this.accountHelper = accountHelper;
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

        accountServices.stream().filter(
                (accountService) -> {
                    logger.debug("accountService.getPaymentService() : " + accountService.getPaymentService());

                    if (accountService.getPaymentService() == null) {
                        logger.debug("accountService.getPaymentService() == null");
//                        throw new ChargeException("accountService.getPaymentService() == null");
                    }

                    return accountService.getPaymentService() != null && accountService.getPaymentService().getPaymentType() == ServicePaymentType.MONTH
                            && (accountService.getLastBilled() == null
                            || accountService.getLastBilled().isBefore(chargeDate.minusMonths(1))
                            || accountService.getLastBilled().isEqual(chargeDate.minusMonths(1)));
                }
        ).collect(Collectors.toList()).forEach(
                (accountService -> {
                    BigDecimal cost = accountService.getCost();
                    this.makeCharge(account, accountService, cost, chargeDate);
                })
        );

        logger.debug("processing daily charge for PersonalAccount: " + account.getAccountId()
                + " name: " + account.getName());
        accountServices.stream().filter((accountService) -> accountService.getPaymentService().getPaymentType() == ServicePaymentType.DAY
                && (accountService.getLastBilled() == null
                || accountService.getLastBilled().isBefore(chargeDate.minusDays(1))
                || accountService.getLastBilled().isEqual(chargeDate.minusDays(1)))
        ).collect(Collectors.toList()).forEach(
                (accountService -> {
                    BigDecimal cost = accountService.getCost();
                    this.makeCharge(account, accountService, cost, chargeDate);
                })
        );
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
