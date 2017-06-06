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
            throw new ParameterWithRoleSecurityException("Изменение поля 'тип' запрещено");
        }

        if (currentAccountOwner.getName() != null &&
                accountOwner.getName() != null &&
                !currentAccountOwner.getName().equals(accountOwner.getName())
                ) {
            throw new ParameterWithRoleSecurityException("Изменение поля 'имя/наименование' запрещено");
        }

        if (currentAccountOwner.getPersonalInfo() != null &&
                accountOwner.getPersonalInfo() != null
                ) {
            if (currentAccountOwner.getPersonalInfo().getNumber() != null &&
                    accountOwner.getPersonalInfo().getNumber() != null &&
                    !currentAccountOwner.getPersonalInfo().getNumber().equals(accountOwner.getPersonalInfo().getNumber())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'серия и номера паспорта' запрещено");
            }

            if (currentAccountOwner.getPersonalInfo().getIssuedDate() != null &&
                    accountOwner.getPersonalInfo().getIssuedDate() != null &&
                    !currentAccountOwner.getPersonalInfo().getIssuedDate().equals(accountOwner.getPersonalInfo().getIssuedDate())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'дата выдачи паспорта' запрещено");
            }

            if (currentAccountOwner.getPersonalInfo().getIssuedOrg() != null &&
                    accountOwner.getPersonalInfo().getIssuedOrg() != null &&
                    !currentAccountOwner.getPersonalInfo().getIssuedOrg().equals(accountOwner.getPersonalInfo().getIssuedOrg())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'дата выдачи паспорта' запрещено");
            }

            if (currentAccountOwner.getPersonalInfo().getAddress() != null &&
                    accountOwner.getPersonalInfo().getAddress() != null &&
                    !currentAccountOwner.getPersonalInfo().getAddress().equals(accountOwner.getPersonalInfo().getAddress())
                    ) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'адрес регистрации' запрещено");
            }

            if (currentAccountOwner.getPersonalInfo().getInn() != null &&
                    accountOwner.getPersonalInfo().getInn() != null &&
                    !currentAccountOwner.getPersonalInfo().getInn().equals(accountOwner.getPersonalInfo().getInn())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'ИНН' запрещено");
            }

            if (currentAccountOwner.getPersonalInfo().getKpp() != null &&
                    accountOwner.getPersonalInfo().getKpp() != null &&
                    !currentAccountOwner.getPersonalInfo().getKpp().equals(accountOwner.getPersonalInfo().getKpp())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'КПП' запрещено");
            }

            if (currentAccountOwner.getPersonalInfo().getOgrn() != null &&
                    accountOwner.getPersonalInfo().getOgrn() != null &&
                    !currentAccountOwner.getPersonalInfo().getOgrn().equals(accountOwner.getPersonalInfo().getOgrn())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'ОГРН' запрещено");
            }

            if (currentAccountOwner.getPersonalInfo().getOkpo() != null &&
                    accountOwner.getPersonalInfo().getOkpo() != null &&
                    !currentAccountOwner.getPersonalInfo().getOkpo().equals(accountOwner.getPersonalInfo().getOkpo())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'ОКПО' запрещено");
            }

            if (currentAccountOwner.getPersonalInfo().getOkvedCodes() != null &&
                    accountOwner.getPersonalInfo().getOkvedCodes() != null &&
                    !currentAccountOwner.getPersonalInfo().getOkvedCodes().equals(accountOwner.getPersonalInfo().getOkvedCodes())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'ОКВЭД' запрещено");
            }

            if (currentAccountOwner.getPersonalInfo().getAddress() != null &&
                    accountOwner.getPersonalInfo().getAddress() != null &&
                    !currentAccountOwner.getPersonalInfo().getAddress().equals(accountOwner.getPersonalInfo().getAddress())
                    ) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'юридический адрес' запрещено");
            }
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

        if (currentAccountOwner.getPersonalInfo() == null &&
                accountOwner.getPersonalInfo() != null
                ) {
            currentAccountOwner.setPersonalInfo(accountOwner.getPersonalInfo());
        }

        setAllowedFields(currentAccountOwner, accountOwner);
    }

    public void setFields(AccountOwner currentAccountOwner, AccountOwner accountOwner) {
        currentAccountOwner.setType(accountOwner.getType());
        currentAccountOwner.setName(accountOwner.getName());
        currentAccountOwner.setPersonalInfo(accountOwner.getPersonalInfo());

        setAllowedFields(currentAccountOwner, accountOwner);
    }

    private void setAllowedFields(AccountOwner currentAccountOwner, AccountOwner accountOwner) {
        currentAccountOwner.setContactInfo(accountOwner.getContactInfo());
    }
}
