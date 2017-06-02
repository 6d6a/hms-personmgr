package ru.majordomo.hms.personmgr.service;

import org.springframework.stereotype.Service;


import ru.majordomo.hms.personmgr.exception.ParameterWithRoleSecurityException;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;

@Service
public class AccountOwnerHelper {
    public void checkNotEmptyFields(AccountOwner currentAccountOwner, AccountOwner accountOwner) {
        if (currentAccountOwner.getType() != null &&
                accountOwner.getType() != null &&
                currentAccountOwner.getType() != accountOwner.getType()
                ) {
            throw new ParameterWithRoleSecurityException("Изменение 'типа' владельца аккаунта запрещено");
        }

        if (currentAccountOwner.getName() != null &&
                accountOwner.getName() != null &&
                currentAccountOwner.getName().equals(accountOwner.getName())
                ) {
            throw new ParameterWithRoleSecurityException("Изменение 'имени/наименования' владельца аккаунта запрещено");
        }

        if (currentAccountOwner.getPassport() != null &&
                accountOwner.getPassport() != null &&
                currentAccountOwner.getPassport().equals(accountOwner.getPassport())
                ) {
            throw new ParameterWithRoleSecurityException("Изменение 'паспортных данных' владельца аккаунта запрещено");
        }

        if (currentAccountOwner.getLegalEntity() != null &&
                accountOwner.getLegalEntity() != null &&
                currentAccountOwner.getLegalEntity().equals(accountOwner.getLegalEntity())
                ) {
            throw new ParameterWithRoleSecurityException("Изменение 'реквизитов' владельца аккаунта запрещено");
        }
    }

    public void setEmptyAndAllowedToEditFields(AccountOwner currentAccountOwner, AccountOwner accountOwner) {
        if (currentAccountOwner.getType() == null &&
                accountOwner.getType() != null
                ) {
            currentAccountOwner.setType(accountOwner.getType());
        }

        if (currentAccountOwner.getName() == null &&
                accountOwner.getName() != null
                ) {
            currentAccountOwner.setName(accountOwner.getName());
        }

        if (currentAccountOwner.getPassport() == null &&
                accountOwner.getPassport() != null
                ) {
            currentAccountOwner.setPassport(accountOwner.getPassport());
        }

        if (currentAccountOwner.getLegalEntity() == null &&
                accountOwner.getLegalEntity() != null
                ) {
            currentAccountOwner.setLegalEntity(accountOwner.getLegalEntity());
        }

        setAllowedFields(currentAccountOwner, accountOwner);
    }

    public void setFields(AccountOwner currentAccountOwner, AccountOwner accountOwner) {
        currentAccountOwner.setType(accountOwner.getType());
        currentAccountOwner.setName(accountOwner.getName());
        currentAccountOwner.setPassport(accountOwner.getPassport());
        currentAccountOwner.setLegalEntity(accountOwner.getLegalEntity());

        setAllowedFields(currentAccountOwner, accountOwner);
    }

    private void setAllowedFields(AccountOwner currentAccountOwner, AccountOwner accountOwner) {
        currentAccountOwner.setPostalAddress(accountOwner.getPostalAddress());
        currentAccountOwner.setPhoneNumbers(accountOwner.getPhoneNumbers());
        currentAccountOwner.setEmailAddresses(accountOwner.getEmailAddresses());
    }
}
