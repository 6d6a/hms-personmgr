package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;


/**
 * PaymentChargesProcessorService
 */
@Service
public class PaymentChargesProcessorService {
    private final static Logger logger = LoggerFactory.getLogger(PaymentChargesProcessorService.class);

    @Autowired
    private PersonalAccountRepository personalAccountRepository;

    @Autowired
    private FinFeignClient finFeignClient;

    @Autowired
    private AccountServiceRepository accountServiceRepository;

    //Выполняем списания в 01:00:00 каждый день
    @Scheduled(cron = "0 0 1 * * *")
    public void processCharges() {
        try (Stream<PersonalAccount> personalAccountStream = personalAccountRepository.findAllStream()) {
            personalAccountStream.forEach(
                    this::processCharge
            );
        }
    }

    public void processCharges(String paymentAccountName) {
        PersonalAccount account = personalAccountRepository.findByName(paymentAccountName);
        this.processCharge(account);
    }

    private void processCharge(PersonalAccount paymentAccount) {
        LocalDateTime chargeDate = LocalDateTime.now().minusDays(1).withHour(23).withMinute(59).withSecond(59);
        logger.debug("processing charges for PersonalAccount: " + paymentAccount.getAccountId()
                + " name: " + paymentAccount.getName()
                + " for date: " + chargeDate.format(DateTimeFormatter.ISO_DATE_TIME));

        logger.debug("processing monthly charge for PersonalAccount: " + paymentAccount.getAccountId()
                + " name: " + paymentAccount.getName()
                + " for date: " + chargeDate.format(DateTimeFormatter.ISO_DATE_TIME));
        List<AccountService> accountServices = paymentAccount.getServices();
        accountServices.stream().filter((accountService) -> accountService.getPaymentService().getPaymentType() == ServicePaymentType.MONTH
                && (accountService.getLastBilled() == null
                || accountService.getLastBilled().isBefore(chargeDate.minusMonths(1))
                || accountService.getLastBilled().isEqual(chargeDate.minusMonths(1)))
        ).collect(Collectors.toList()).forEach(
                (accountService -> {
                    BigDecimal cost = accountService.getCost();
                    this.makeCharge(paymentAccount, accountService, cost, chargeDate);
                })
        );

        logger.debug("processing daily charge for PersonalAccount: " + paymentAccount.getAccountId()
                + " name: " + paymentAccount.getName());
        accountServices.stream().filter((accountService) -> accountService.getPaymentService().getPaymentType() == ServicePaymentType.DAY
                && (accountService.getLastBilled() == null
                || accountService.getLastBilled().isBefore(chargeDate.minusDays(1))
                || accountService.getLastBilled().isEqual(chargeDate.minusDays(1)))
        ).collect(Collectors.toList()).forEach(
                (accountService -> {
                    BigDecimal cost = accountService.getCost();
                    this.makeCharge(paymentAccount, accountService, cost, chargeDate);
                })
        );
    }

    private void makeCharge(PersonalAccount paymentAccount, AccountService accountService, BigDecimal cost, LocalDateTime chargeDate) {
        if (cost.compareTo(BigDecimal.ZERO) == 1) {
            Map<String, Object> paymentOperation = new HashMap<>();
            paymentOperation.put("serviceId", accountService.getServiceId());
            paymentOperation.put("amount", cost);

            Map<String, Object> response = null;

            try {
                response = finFeignClient.charge(paymentAccount.getId(), paymentOperation);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            if (response != null && response.get("success") != null && !((boolean) response.get("success"))) {
                logger.debug("Error. Charge Processor returned false fo service: " + accountService.toString());
            } else {
                accountService.setLastBilled(chargeDate);
                accountServiceRepository.save(accountService);
                logger.debug("Success. Charge Processor returned true fo service: " + accountService.toString());
            }
        }
    }
}
