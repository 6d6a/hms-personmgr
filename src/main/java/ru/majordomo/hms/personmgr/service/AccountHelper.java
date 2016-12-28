package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.ChargeException;
import ru.majordomo.hms.personmgr.exception.LowBalanceException;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.rc.user.resources.Person;

@Service
public class AccountHelper {
    private final RcUserFeignClient rcUserFeignClient;
    private final FinFeignClient finFeignClient;

    @Autowired
    public AccountHelper(
            RcUserFeignClient rcUserFeignClient,
            FinFeignClient finFeignClient
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.finFeignClient = finFeignClient;
    }

    public String getEmail(PersonalAccount account) {
        String clientEmails = "";

        Person person = null;
        if (account.getOwnerPersonId() != null) {
            person = rcUserFeignClient.getPerson(account.getId(), account.getOwnerPersonId());
        }

        if (person != null) {
            clientEmails = String.join(", ", person.getEmailAddresses());
        }

        return clientEmails;
    }

    /**
     * Получим баланс
     *
     * @param account Аккаунт
     */
    public BigDecimal getBalance(PersonalAccount account) {
        Map<String, Object> balance = finFeignClient.getBalance(account.getId());

        if (balance == null) {
            throw new ResourceNotFoundException("Account balance not found.");
        }

        return BigDecimal.valueOf((double) balance.get("available"));
    }

    /**
     * Проверим не отрицательный ли баланс
     *
     * @param account Аккаунт
     */
    public void checkBalance(PersonalAccount account) {
        BigDecimal available = getBalance(account);

        if (available.compareTo(BigDecimal.ZERO) < 0) {
            throw new LowBalanceException("Account balance is lower than zero. balance is: "
                    + available.toPlainString());
        }
    }

    /**
     * Проверим не отрицательный ли баланс
     *
     * @param account Аккаунт
     */
    public void checkBalance(PersonalAccount account, PaymentService service) {
        BigDecimal available = getBalance(account);

        if (available.compareTo(service.getCost()) < 0) {
            throw new LowBalanceException("Account balance is too low for specified service. " +
                    "Current balance is: " + available.toPlainString() + " service cost is: " + service.getCost());
        }
    }

    //TODO на самом деле сюда ещё должна быть возможность передать discountedService
    public SimpleServiceMessage charge(PersonalAccount account, PaymentService service) {
        Map<String, Object> paymentOperation = new HashMap<>();
        paymentOperation.put("serviceId", service.getId());
        paymentOperation.put("amount", service.getCost());

        SimpleServiceMessage response = finFeignClient.charge(account.getId(), paymentOperation);

        if (response.getParam("success") == null || !((boolean) response.getParam("success"))) {
            throw new ChargeException("Account balance is too low for specified service. " +
                    " Service cost is: " + service.getCost());
        }

        return response;
    }
    //TODO на самом деле сюда ещё должна быть возможность передать discountedService
    public SimpleServiceMessage block(PersonalAccount account, PaymentService service) {
        Map<String, Object> paymentOperation = new HashMap<>();
        paymentOperation.put("serviceId", service.getId());
        paymentOperation.put("amount", service.getCost());

        SimpleServiceMessage response = finFeignClient.block(account.getId(), paymentOperation);

        if (response.getParam("success") == null || !((boolean) response.getParam("success"))) {
            throw new ChargeException("Account balance is too low for specified service. " +
                    " Service cost is: " + service.getCost());
        }

        return response;
    }

}
