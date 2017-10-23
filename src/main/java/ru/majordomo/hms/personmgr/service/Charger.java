package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import ru.majordomo.hms.personmgr.common.ChargeResult;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.ChargeException;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;

@Service
public class Charger {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AccountServiceHelper accountServiceHelper;
    private final PersonalAccountManager accountManager;
    private final AccountHelper accountHelper;
    private final AccountServiceRepository accountServiceRepository;

    public Charger(
            AccountServiceHelper accountServiceHelper,
            PersonalAccountManager accountManager,
            AccountHelper accountHelper,
            AccountServiceRepository accountServiceRepository
    ) {
        this.accountServiceHelper = accountServiceHelper;
        this.accountManager = accountManager;
        this.accountHelper = accountHelper;
        this.accountServiceRepository = accountServiceRepository;
    }

    /*
            результат списания за услугу, полученный от finansier
            Значение по-умолчанию true, на случай если
            списания не было, значит успешно и выключать услугу не надо
        */
    ChargeResult makeCharge(AccountService accountService, LocalDate chargeDate) {
        BigDecimal cost = accountServiceHelper.getDailyCostForService(accountService, chargeDate);

        if (cost.compareTo(BigDecimal.ZERO) <= 0) { return ChargeResult.success(); }

        LocalDateTime chargeDateTime = LocalDateTime.of(chargeDate, LocalTime.of(0,0,0,0));
        PersonalAccount account = accountManager.findOne(accountService.getPersonalAccountId());
        Boolean forceCharge = accountHelper.hasActiveCredit(account);

        SimpleServiceMessage response;

        try {
            logger.info("Send charge request in fin for PersonalAccount: " + account.getAccountId()
                    + " name: " + account.getName()
                    + " for date: " + chargeDate.format(DateTimeFormatter.ISO_DATE)
                    + " cost: " + cost
            );
            response = accountHelper.charge(account, accountService.getPaymentService(), cost, forceCharge, false, chargeDateTime);
        } catch (ChargeException e) {
            logger.info("Error. accountHelper.charge returned ChargeException for service: " + accountService.toString());
            return ChargeResult.error();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in Charger.makeCharge " + e.getMessage());
            return ChargeResult.errorWithException();
        }

        if (response != null && response.getParam("success") != null && ((boolean) response.getParam("success"))) {
            if (accountService.getLastBilled().isBefore(chargeDateTime)) {
                accountService.setLastBilled(chargeDateTime);
            }
            accountServiceRepository.save(accountService);
            logger.info("Success. Charge Processor returned true fo service: " + accountService.toString());
            return ChargeResult.success();
        } else {
            logger.info("Error. Charge Processor returned false for service: " + accountService.toString());
        }

        return ChargeResult.error();
    }
}
