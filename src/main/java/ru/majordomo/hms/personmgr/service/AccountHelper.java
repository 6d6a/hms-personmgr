package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.ChargeException;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.LowBalanceException;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.present.AccountPresent;
import ru.majordomo.hms.personmgr.model.present.Present;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountPresentRepository;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.Domain;

import static ru.majordomo.hms.personmgr.common.Constants.PASSWORD_KEY;

@Service
public class AccountHelper {
    private final RcUserFeignClient rcUserFeignClient;
    private final FinFeignClient finFeignClient;
    private final SiFeignClient siFeignClient;
    private final AccountPresentRepository accountPresentRepository;

    @Autowired
    public AccountHelper(
            RcUserFeignClient rcUserFeignClient,
            FinFeignClient finFeignClient,
            SiFeignClient siFeignClient,
            AccountPresentRepository accountPresentRepository
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.finFeignClient = finFeignClient;
        this.siFeignClient = siFeignClient;
        this.accountPresentRepository = accountPresentRepository;
    }

    public String getEmail(PersonalAccount account) {
        String clientEmails = "";

        Person person = null;
        if (account.getOwnerPersonId() != null) {
            try {
                person = rcUserFeignClient.getPerson(account.getId(), account.getOwnerPersonId());
            } catch (Exception e) {
                e.printStackTrace();
            }
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
        Map<String, Object> balance = null;

        try {
            balance = finFeignClient.getBalance(account.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (balance == null) {
            throw new ResourceNotFoundException("Account balance not found.");
        }

        BigDecimal available;

        try {
            if (balance.get("available") instanceof Integer) {
                available = BigDecimal.valueOf((Integer) balance.get("available"));
            } else if (balance.get("available") instanceof Double) {
                available = BigDecimal.valueOf((Double) balance.get("available"));
            } else {
                available = (BigDecimal) balance.get("available");
            }
        } catch (Exception e) {
            e.printStackTrace();
            available = BigDecimal.ZERO;
        }

        return available;
    }

    /**
     * Получаем домены
     *
     * @param account Аккаунт
     */
    public List<Domain> getDomains(PersonalAccount account) {
        List<Domain> domains = null;

        try {
            domains = rcUserFeignClient.getDomains(account.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return domains;
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
     * Проверим хватает ли баланса на услугу
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

    /**
     * Проверим хватает ли баланса на один день услуги
     *
     * @param account Аккаунт
     */
    public void checkBalance(PersonalAccount account, PaymentService service, boolean forOneDay) {
        if (!forOneDay) {
            checkBalance(account, service);
        } else {
            BigDecimal dayCost = getDayCostByService(service);

            BigDecimal available = getBalance(account);

            if (available.compareTo(dayCost) < 0) {
                throw new LowBalanceException("Account balance is too low for specified service. " +
                        "Current balance is: " + available.toPlainString() + " service oneDayCost is: " + dayCost);
            }
        }
    }

    public BigDecimal getDayCostByService(PaymentService service, LocalDateTime chargeDate) {
        Integer daysInCurrentMonth = chargeDate.toLocalDate().lengthOfMonth();

        return service.getCost().divide(BigDecimal.valueOf(daysInCurrentMonth), 4, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal getDayCostByService(PaymentService service) {
        LocalDateTime chargeDate = LocalDateTime.now();

        return getDayCostByService(service, chargeDate);
    }

    //TODO на самом деле сюда ещё должна быть возможность передать discountedService
    public SimpleServiceMessage charge(PersonalAccount account, PaymentService service) {
        BigDecimal amount = service.getCost();

        return charge(account, service, amount);
    }

    //TODO на самом деле сюда ещё должна быть возможность передать discountedService
    public SimpleServiceMessage charge(PersonalAccount account, PaymentService service, BigDecimal amount) {
        Map<String, Object> paymentOperation = new HashMap<>();
        paymentOperation.put("serviceId", service.getId());
        paymentOperation.put("amount", amount);

        SimpleServiceMessage response = null;

        try {
            response = finFeignClient.charge(account.getId(), paymentOperation);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (response != null && (response.getParam("success") == null || !((boolean) response.getParam("success")))) {
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

        SimpleServiceMessage response = null;
        try {
            response = finFeignClient.block(account.getId(), paymentOperation);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (response != null && (response.getParam("success") == null || !((boolean) response.getParam("success")))) {
            throw new ChargeException("Account balance is too low for specified service. " +
                    " Service cost is: " + service.getCost());
        }

        return response;
    }

    public SimpleServiceMessage changePassword(PersonalAccount account, String newPassword) {
        Map<String, String> params = new HashMap<>();
        params.put(PASSWORD_KEY, newPassword);

        SimpleServiceMessage response = null;

        try {
            response = siFeignClient.changePassword(account.getId(), params);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (response != null && (response.getParam("success") == null || !((boolean) response.getParam("success")))) {
            throw new InternalApiException("Account password not changed. ");
        }

        return response;
    }

    public void givePresent(PersonalAccount account, Present present) {
        Long currentCount = accountPresentRepository.countByPersonalAccountIdAndPresentId(account.getId(), present.getId());
        if ((currentCount < present.getLimitPerAccount()) || present.getLimitPerAccount() == -1) {
            AccountPresent accountPresent = new AccountPresent();
            accountPresent.setPersonalAccountId(account.getId());
            accountPresent.setPresentId(present.getId());
            accountPresent.setPresent(present);
            accountPresent.setCreated(LocalDateTime.now());

            Map<String, Boolean> actionsWithStatus = new HashMap<>();
            for (String actionId : present.getActionIds()) {
                actionsWithStatus.put(actionId, true);
            }
            accountPresent.setActionsWithStatus(actionsWithStatus);

            accountPresentRepository.save(accountPresent);
        }
    }
}
