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

        if (currentAccountOwner.getPassport() != null &&
                accountOwner.getPassport() != null
                ) {
            if (currentAccountOwner.getPassport().getNumber() != null &&
                    accountOwner.getPassport().getNumber() != null &&
                    !currentAccountOwner.getPassport().getNumber().equals(accountOwner.getPassport().getNumber())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'серия и номера паспорта' запрещено");
            }

            if (currentAccountOwner.getPassport().getIssuedDate() != null &&
                    accountOwner.getPassport().getIssuedDate() != null &&
                    !currentAccountOwner.getPassport().getIssuedDate().equals(accountOwner.getPassport().getIssuedDate())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'дата выдачи паспорта' запрещено");
            }

            if (currentAccountOwner.getPassport().getIssuedOrg() != null &&
                    accountOwner.getPassport().getIssuedOrg() != null &&
                    !currentAccountOwner.getPassport().getIssuedOrg().equals(accountOwner.getPassport().getIssuedOrg())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'дата выдачи паспорта' запрещено");
            }

            if (currentAccountOwner.getPassport().getAddress() != null &&
                    accountOwner.getPassport().getAddress() != null
                    ) {
                if (currentAccountOwner.getPassport().getAddress().getStreet() != null &&
                        accountOwner.getPassport().getAddress().getStreet() != null &&
                        !currentAccountOwner.getPassport().getAddress().getStreet().equals(accountOwner.getPassport().getAddress().getStreet())) {
                    throw new ParameterWithRoleSecurityException("Изменение поля 'улица адреса регистрации' запрещено");
                }

                if (currentAccountOwner.getPassport().getAddress().getZip() != null &&
                        accountOwner.getPassport().getAddress().getZip() != null &&
                        !currentAccountOwner.getPassport().getAddress().getZip().equals(accountOwner.getPassport().getAddress().getZip())) {
                    throw new ParameterWithRoleSecurityException("Изменение поля 'индекс адреса регистрации' запрещено");
                }

                if (currentAccountOwner.getPassport().getAddress().getCity() != null &&
                        accountOwner.getPassport().getAddress().getCity() != null &&
                        !currentAccountOwner.getPassport().getAddress().getCity().equals(accountOwner.getPassport().getAddress().getCity())) {
                    throw new ParameterWithRoleSecurityException("Изменение поля 'город адреса регистрации' запрещено");
                }

                if (currentAccountOwner.getPassport().getAddress().getCountry() != null &&
                        accountOwner.getPassport().getAddress().getCountry() != null &&
                        !currentAccountOwner.getPassport().getAddress().getCountry().equals(accountOwner.getPassport().getAddress().getCountry())) {
                    throw new ParameterWithRoleSecurityException("Изменение поля 'страна адреса регистрации' запрещено");
                }
            }
        }

        if (currentAccountOwner.getLegalEntity() != null &&
                accountOwner.getLegalEntity() != null &&
                !currentAccountOwner.getLegalEntity().equals(accountOwner.getLegalEntity())
                ) {
            if (currentAccountOwner.getLegalEntity().getInn() != null &&
                    accountOwner.getLegalEntity().getInn() != null &&
                    !currentAccountOwner.getLegalEntity().getInn().equals(accountOwner.getLegalEntity().getInn())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'ИНН' запрещено");
            }

            if (currentAccountOwner.getLegalEntity().getKpp() != null &&
                    accountOwner.getLegalEntity().getKpp() != null &&
                    !currentAccountOwner.getLegalEntity().getKpp().equals(accountOwner.getLegalEntity().getKpp())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'КПП' запрещено");
            }

            if (currentAccountOwner.getLegalEntity().getOgrn() != null &&
                    accountOwner.getLegalEntity().getOgrn() != null &&
                    !currentAccountOwner.getLegalEntity().getOgrn().equals(accountOwner.getLegalEntity().getOgrn())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'ОГРН' запрещено");
            }

            if (currentAccountOwner.getLegalEntity().getOkpo() != null &&
                    accountOwner.getLegalEntity().getOkpo() != null &&
                    !currentAccountOwner.getLegalEntity().getOkpo().equals(accountOwner.getLegalEntity().getOkpo())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'ОКПО' запрещено");
            }

            if (currentAccountOwner.getLegalEntity().getOkvedCodes() != null &&
                    accountOwner.getLegalEntity().getOkvedCodes() != null &&
                    !currentAccountOwner.getLegalEntity().getOkvedCodes().equals(accountOwner.getLegalEntity().getOkvedCodes())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'ОКВЭД' запрещено");
            }

            if (currentAccountOwner.getLegalEntity().getAddress() != null &&
                    accountOwner.getLegalEntity().getAddress() != null
                    ) {
                if (currentAccountOwner.getLegalEntity().getAddress().getStreet() != null &&
                        accountOwner.getLegalEntity().getAddress().getStreet() != null &&
                        !currentAccountOwner.getLegalEntity().getAddress().getStreet().equals(accountOwner.getLegalEntity().getAddress().getStreet())) {
                    throw new ParameterWithRoleSecurityException("Изменение поля 'улица юридического адреса' запрещено");
                }

                if (currentAccountOwner.getLegalEntity().getAddress().getZip() != null &&
                        accountOwner.getLegalEntity().getAddress().getZip() != null &&
                        !currentAccountOwner.getLegalEntity().getAddress().getZip().equals(accountOwner.getLegalEntity().getAddress().getZip())) {
                    throw new ParameterWithRoleSecurityException("Изменение поля 'индекс юридического адреса' запрещено");
                }

                if (currentAccountOwner.getLegalEntity().getAddress().getCity() != null &&
                        accountOwner.getLegalEntity().getAddress().getCity() != null &&
                        !currentAccountOwner.getLegalEntity().getAddress().getCity().equals(accountOwner.getLegalEntity().getAddress().getCity())) {
                    throw new ParameterWithRoleSecurityException("Изменение поля 'город юридического адреса' запрещено");
                }

                if (currentAccountOwner.getLegalEntity().getAddress().getCountry() != null &&
                        accountOwner.getLegalEntity().getAddress().getCountry() != null &&
                        !currentAccountOwner.getLegalEntity().getAddress().getCountry().equals(accountOwner.getLegalEntity().getAddress().getCountry())) {
                    throw new ParameterWithRoleSecurityException("Изменение поля 'страна юридического адреса' запрещено");
                }
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
