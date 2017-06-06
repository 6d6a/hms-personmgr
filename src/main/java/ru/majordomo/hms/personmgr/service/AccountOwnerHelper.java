package ru.majordomo.hms.personmgr.service;

import org.springframework.stereotype.Service;


import ru.majordomo.hms.personmgr.exception.ParameterWithRoleSecurityException;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.PersonalInfo;

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

        String addressLabel = currentAccountOwner.getType().equals(AccountOwner.Type.INDIVIDUAL) ? "адрес регистрации" : "юридический адрес";

        PersonalInfo currentPersonalInfo = currentAccountOwner.getPersonalInfo();
        PersonalInfo personalInfo = accountOwner.getPersonalInfo();

        if (currentPersonalInfo != null &&
                personalInfo != null
                ) {
            if (currentPersonalInfo.getNumber() != null &&
                    personalInfo.getNumber() != null &&
                    !currentPersonalInfo.getNumber().equals(personalInfo.getNumber())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'серия и номера паспорта' запрещено");
            }

            if (currentPersonalInfo.getIssuedDate() != null &&
                    personalInfo.getIssuedDate() != null &&
                    !currentPersonalInfo.getIssuedDate().equals(personalInfo.getIssuedDate())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'дата выдачи паспорта' запрещено");
            }

            if (currentPersonalInfo.getIssuedOrg() != null &&
                    personalInfo.getIssuedOrg() != null &&
                    !currentPersonalInfo.getIssuedOrg().equals(personalInfo.getIssuedOrg())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'дата выдачи паспорта' запрещено");
            }

            if (currentPersonalInfo.getAddress() != null &&
                    personalInfo.getAddress() != null &&
                    !currentPersonalInfo.getAddress().equals(personalInfo.getAddress())
                    ) {
                throw new ParameterWithRoleSecurityException("Изменение поля '" + addressLabel + "' запрещено");
            }

            if (currentPersonalInfo.getInn() != null &&
                    personalInfo.getInn() != null &&
                    !currentPersonalInfo.getInn().equals(personalInfo.getInn())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'ИНН' запрещено");
            }

            if (currentPersonalInfo.getKpp() != null &&
                    personalInfo.getKpp() != null &&
                    !currentPersonalInfo.getKpp().equals(personalInfo.getKpp())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'КПП' запрещено");
            }

            if (currentPersonalInfo.getOgrn() != null &&
                    personalInfo.getOgrn() != null &&
                    !currentPersonalInfo.getOgrn().equals(personalInfo.getOgrn())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'ОГРН' запрещено");
            }

            if (currentPersonalInfo.getOkpo() != null &&
                    personalInfo.getOkpo() != null &&
                    !currentPersonalInfo.getOkpo().equals(personalInfo.getOkpo())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'ОКПО' запрещено");
            }

            if (currentPersonalInfo.getOkvedCodes() != null &&
                    personalInfo.getOkvedCodes() != null &&
                    !currentPersonalInfo.getOkvedCodes().equals(personalInfo.getOkvedCodes())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'ОКВЭД' запрещено");
            }
        }
    }

    public void setEmptyAndAllowedToEditFields(AccountOwner currentAccountOwner, AccountOwner accountOwner) {
        if (currentAccountOwner.getType() == null && accountOwner.getType() != null) {
            currentAccountOwner.setType(accountOwner.getType());
        }

        if (currentAccountOwner.getName() == null && accountOwner.getName() != null) {
            currentAccountOwner.setName(accountOwner.getName());
        }

        PersonalInfo currentPersonalInfo = currentAccountOwner.getPersonalInfo();
        PersonalInfo personalInfo = accountOwner.getPersonalInfo();

        if (currentPersonalInfo == null && personalInfo != null) {
            currentPersonalInfo = personalInfo;
        } else if (currentPersonalInfo != null && personalInfo != null) {
            if (currentPersonalInfo.getNumber() == null &&
                    personalInfo.getNumber() != null) {
                currentPersonalInfo.setNumber(personalInfo.getNumber());
            }

            if (currentPersonalInfo.getIssuedDate() == null &&
                    personalInfo.getIssuedDate() != null) {
                currentPersonalInfo.setIssuedDate(personalInfo.getIssuedDate());
            }

            if (currentPersonalInfo.getIssuedOrg() == null &&
                    personalInfo.getIssuedOrg() != null) {
                currentPersonalInfo.setIssuedOrg(personalInfo.getIssuedOrg());
            }

            if (currentPersonalInfo.getAddress() == null &&
                    personalInfo.getAddress() != null) {
                currentPersonalInfo.setAddress(personalInfo.getAddress());
            }

            if (currentPersonalInfo.getInn() == null &&
                    personalInfo.getInn() != null) {
                currentPersonalInfo.setInn(personalInfo.getInn());
            }

            if (currentPersonalInfo.getKpp() == null &&
                    personalInfo.getKpp() != null) {
                currentPersonalInfo.setKpp(personalInfo.getKpp());
            }

            if (currentPersonalInfo.getOgrn() == null &&
                    personalInfo.getOgrn() != null) {
                currentPersonalInfo.setOgrn(personalInfo.getOgrn());
            }

            if (currentPersonalInfo.getOkpo() == null &&
                    personalInfo.getOkpo() != null) {
                currentPersonalInfo.setOkpo(personalInfo.getOkpo());
            }

            if (currentPersonalInfo.getOkvedCodes() == null &&
                    personalInfo.getOkvedCodes() != null) {
                currentPersonalInfo.setOkvedCodes(personalInfo.getOkvedCodes());
            }
        }

        currentAccountOwner.setPersonalInfo(currentPersonalInfo);

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
