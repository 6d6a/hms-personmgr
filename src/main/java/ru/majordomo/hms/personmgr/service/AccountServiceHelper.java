package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;

@Service
public class AccountServiceHelper {
    private final AccountServiceRepository accountServiceRepository;

    @Autowired
    public AccountServiceHelper(AccountServiceRepository accountServiceRepository) {
        this.accountServiceRepository = accountServiceRepository;
    }

    /**
     * Удаляем старую услугу
     *
     * @param account   Аккаунт
     * @param oldServiceId id текущей услуги
     */
    public void deleteAccountService(PersonalAccount account, String oldServiceId) {
        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), oldServiceId);

        if (accountServices != null && !accountServices.isEmpty()) {
            accountServiceRepository.delete(accountServices);
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
            deleteAccountService(account, oldServiceId);

            addAccountService(account, newServiceId);
        }
    }
}
