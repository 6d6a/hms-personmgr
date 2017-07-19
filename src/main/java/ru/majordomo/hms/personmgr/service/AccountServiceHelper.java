package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

import static ru.majordomo.hms.personmgr.common.Constants.SMS_NOTIFICATIONS_29_RUB_SERVICE_ID;

@Service
public class AccountServiceHelper {
    private final AccountServiceRepository accountServiceRepository;
    private final PlanRepository planRepository;
    private final PaymentServiceRepository serviceRepository;

    @Autowired
    public AccountServiceHelper(
            AccountServiceRepository accountServiceRepository,
            PlanRepository planRepository,
            PaymentServiceRepository serviceRepository
    ) {
        this.accountServiceRepository = accountServiceRepository;
        this.planRepository = planRepository;
        this.serviceRepository = serviceRepository;
    }

    /**
     * Удаляем старую услугу
     *
     * @param account   Аккаунт
     * @param serviceId id услуги ServiceId
     */
    public void deleteAccountServiceByServiceId(PersonalAccount account, String serviceId) {
        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), serviceId);

        if (accountServices != null && !accountServices.isEmpty()) {
            accountServiceRepository.delete(accountServices);
        }
    }

    /**
     * Удаляем старую услугу
     *
     * @param account   Аккаунт
     * @param accountServiceId id услуги AccountService
     */
    public void deleteAccountServiceById(PersonalAccount account, String accountServiceId) {
        AccountService accountService = accountServiceRepository.findByPersonalAccountIdAndId(account.getId(), accountServiceId);

        if (accountService != null) {
            accountServiceRepository.delete(accountService);
        }
    }

    /**
     * Добавляем новую услугу
     *
     * @param account   Аккаунт
     * @param newServiceId id новой услуги
     */
    public void addAccountService(PersonalAccount account, String newServiceId) {
        AccountService service = new AccountService();
        service.setPersonalAccountId(account.getId());
        service.setServiceId(newServiceId);

        accountServiceRepository.save(service);
    }

    /**
     * Добавляем новую услугу
     *
     * @param account   Аккаунт
     * @param newServiceId id новой услуги
     * @param quantity кол-во услуг
     */
    public void addAccountService(PersonalAccount account, String newServiceId, int quantity) {
        AccountService service = new AccountService();
        service.setPersonalAccountId(account.getId());
        service.setServiceId(newServiceId);
        service.setQuantity(quantity);

        accountServiceRepository.save(service);
    }

    /**
     * Оюновляем услугу
     *
     * @param account   Аккаунт
     * @param serviceId id услуги
     * @param quantity кол-во услуг
     */
    public void updateAccountService(PersonalAccount account, String serviceId, int quantity) {
        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), serviceId);

        if (accountServices != null && !accountServices.isEmpty()) {
            AccountService accountService = accountServices.get(0);

            accountService.setQuantity(quantity);

            accountServiceRepository.save(accountService);
        } else {
            addAccountService(account, serviceId, quantity);
        }
    }

    /**
     * Заменяем старую услугу на новую
     *
     * @param account   Аккаунт
     * @param oldServiceId id текущей услуги
     * @param newServiceId id новой услуги
     */
    public void replaceAccountService(PersonalAccount account, String oldServiceId, String newServiceId) {
        if (!oldServiceId.equals(newServiceId)) {
            deleteAccountServiceByServiceId(account, oldServiceId);

            addAccountService(account, newServiceId);
        }
    }

    /**
     * Проверяем есть ли услуга на аккаунте
     *
     * @param account   Аккаунт
     * @param serviceId id услуги
     */
    public boolean accountHasService(PersonalAccount account, String serviceId) {
        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), serviceId);

        return accountServices != null && !accountServices.isEmpty();
    }

    /**
     * Получить определенные услуги на аккаунте
     *
     * @param account   Аккаунт
     * @param serviceId id услуги
     */
    public List<AccountService> getAccountServices(PersonalAccount account, String serviceId) {
        return accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), serviceId);
    }

    /**
     * Включаем услугу
     *
     * @param account   Аккаунт
     * @param serviceId id услуги
     */
    public void enableAccountService(PersonalAccount account, String serviceId) {
        setEnabledAccountService(account, serviceId, true);
    }

    /**
     * Выключаем услугу
     *
     * @param account   Аккаунт
     * @param serviceId id услуги
     */
    public void disableAccountService(PersonalAccount account, String serviceId) {
        setEnabledAccountService(account, serviceId, false);
    }

    /**
     * Меняем статус услуги
     *
     * @param account   Аккаунт
     * @param serviceId id услуги
     */
    public void setEnabledAccountService(PersonalAccount account, String serviceId, boolean enabled) {
        List<AccountService> accountServices = getAccountServices(account, serviceId);

        accountServices.forEach(accountService -> accountService.setEnabled(enabled));

        accountServiceRepository.save(accountServices);
    }

    /**
     * Меняем статус услуги
     *
     * @param account   Аккаунт
     * @param serviceId id услуги
     */
    public void setLastBilledAccountService(PersonalAccount account, String serviceId) {
        List<AccountService> accountServices = getAccountServices(account, serviceId);

        accountServices.forEach(accountService -> accountService.setLastBilled(LocalDateTime.now()));

        accountServiceRepository.save(accountServices);
    }

    //получить PaymentService для услуги SMS-уведомлений
    public PaymentService getSmsPaymentServiceByPlanId(String planId) {
        Plan plan = planRepository.findOne(planId);

        if (plan == null) {
            throw new ParameterValidationException("Plan with id " + planId + " not found");
        }

        String smsServiceId = plan.getSmsServiceId();

        PaymentService paymentService;

        if (smsServiceId == null) {
            smsServiceId = SMS_NOTIFICATIONS_29_RUB_SERVICE_ID;
            paymentService = serviceRepository.findByOldId(smsServiceId);
        } else {
            paymentService = serviceRepository.findOne(smsServiceId);
        }

        if (paymentService == null) {
            throw new ParameterValidationException("paymentService with id " + smsServiceId + " not found");
        }

        return paymentService;
    }
}
