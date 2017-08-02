package ru.majordomo.hms.personmgr.manager.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;

import ru.majordomo.hms.personmgr.exception.ParameterWithRoleSecurityException;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.ContactInfo;
import ru.majordomo.hms.personmgr.model.account.PersonalInfo;
import ru.majordomo.hms.personmgr.repository.AccountOwnerRepository;

@Component
public class AccountOwnerManagerImpl implements AccountOwnerManager {
    private final AccountOwnerRepository repository;
    private final MongoOperations mongoOperations;

    @Autowired
    public AccountOwnerManagerImpl(
            AccountOwnerRepository repository,
            MongoOperations mongoOperations
    ) {
        this.repository = repository;
        this.mongoOperations = mongoOperations;
    }

    @Override
    public boolean exists(String id) {
        return repository.exists(id);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public void delete(String id) {
        repository.delete(id);
    }

    @Override
    public void delete(AccountOwner accountOwner) {
        repository.delete(accountOwner);
    }

    @Override
    public void delete(Iterable<AccountOwner> accountOwners) {
        repository.delete(accountOwners);
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }

    @Override
    public AccountOwner save(AccountOwner accountOwner) {
        return repository.save(accountOwner);
    }

    @Override
    public List<AccountOwner> save(Iterable<AccountOwner> accountOwners) {
        return repository.save(accountOwners);
    }

    @Override
    public AccountOwner insert(AccountOwner accountOwner) {
        return repository.insert(accountOwner);
    }

    @Override
    public List<AccountOwner> insert(Iterable<AccountOwner> accountOwners) {
        return repository.insert(accountOwners);
    }

    @Override
    public AccountOwner findOne(String id) {
        checkById(id);
        return repository.findOne(id);
    }

    @Override
    public List<AccountOwner> findAll() {
        return repository.findAll();
    }

    @Override
    public List<AccountOwner> findByPersonalAccountId(String personalAccountId) {
        return repository.findByPersonalAccountId(personalAccountId);
    }

    @Override
    public Page<AccountOwner> findByPersonalAccountId(String personalAccountId, Pageable pageable) {
        return repository.findByPersonalAccountId(personalAccountId, pageable);
    }

    @Override
    public AccountOwner findOneByPersonalAccountId(String personalAccountId) {
        return repository.findOneByPersonalAccountId(personalAccountId);
    }

    @Override
    public void deleteByPersonalAccountId(String personalAccountId) {
        repository.deleteOneByPersonalAccountId(personalAccountId);
    }

    @Override
    public List<AccountOwner> findAllByTypeIn(List<AccountOwner.Type> types) {
        return repository.findAllByTypeIn(types);
    }

    @Override
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
                    !currentPersonalInfo.getNumber().equals("") &&
                    !currentPersonalInfo.getNumber().equals(personalInfo.getNumber())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'серия и номера паспорта' запрещено");
            }

            if (currentPersonalInfo.getIssuedDate() != null &&
                    !currentPersonalInfo.getIssuedDate().equals(personalInfo.getIssuedDate())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'дата выдачи паспорта' запрещено");
            }

            if (currentPersonalInfo.getIssuedOrg() != null &&
                    !currentPersonalInfo.getIssuedOrg().equals("") &&
                    !currentPersonalInfo.getIssuedOrg().equals(personalInfo.getIssuedOrg())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'паспорт выдан' запрещено");
            }

            if (currentPersonalInfo.getAddress() != null &&
                    !currentPersonalInfo.getAddress().equals("") &&
                    !currentPersonalInfo.getAddress().equals(personalInfo.getAddress())
                    ) {
                throw new ParameterWithRoleSecurityException("Изменение поля '" + addressLabel + "' запрещено");
            }

            if (currentPersonalInfo.getInn() != null &&
                    !currentPersonalInfo.getInn().equals("") &&
                    !currentPersonalInfo.getInn().equals(personalInfo.getInn())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'ИНН' запрещено");
            }

            if (currentPersonalInfo.getKpp() != null &&
                    !currentPersonalInfo.getKpp().equals("") &&
                    !currentPersonalInfo.getKpp().equals(personalInfo.getKpp())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'КПП' запрещено");
            }

            if (currentPersonalInfo.getOgrn() != null &&
                    !currentPersonalInfo.getOgrn().equals("") &&
                    !currentPersonalInfo.getOgrn().equals(personalInfo.getOgrn())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'ОГРН' запрещено");
            }

            if (currentPersonalInfo.getOkpo() != null &&
                    !currentPersonalInfo.getOkpo().equals("") &&
                    !currentPersonalInfo.getOkpo().equals(personalInfo.getOkpo())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'ОКПО' запрещено");
            }

            if (currentPersonalInfo.getOkvedCodes() != null &&
                    !currentPersonalInfo.getOkvedCodes().equals("") &&
                    !currentPersonalInfo.getOkvedCodes().equals(personalInfo.getOkvedCodes())) {
                throw new ParameterWithRoleSecurityException("Изменение поля 'ОКВЭД' запрещено");
            }
        }
    }

    @Override
    public void setEmptyAndAllowedToEditFields(AccountOwner currentAccountOwner, AccountOwner accountOwner) {
        if (currentAccountOwner.getType() == null && accountOwner.getType() != null) {
            currentAccountOwner.setType(accountOwner.getType());
        }

        if ((currentAccountOwner.getName() == null || currentAccountOwner.getName().equals(""))
                && (accountOwner.getName() != null && accountOwner.getName().equals("")))
        {
            currentAccountOwner.setName(accountOwner.getName());
        }

        PersonalInfo currentPersonalInfo = currentAccountOwner.getPersonalInfo();
        PersonalInfo personalInfo = accountOwner.getPersonalInfo();

        if (currentPersonalInfo == null && personalInfo != null) {
            currentPersonalInfo = personalInfo;
        } else if (currentPersonalInfo != null && personalInfo != null) {
            if ((currentPersonalInfo.getNumber() == null || currentPersonalInfo.getNumber().equals("")) &&
                    (personalInfo.getNumber() != null && !personalInfo.getNumber().equals(""))) {
                currentPersonalInfo.setNumber(personalInfo.getNumber());
            }

            if (currentPersonalInfo.getIssuedDate() == null &&
                    personalInfo.getIssuedDate() != null) {
                currentPersonalInfo.setIssuedDate(personalInfo.getIssuedDate());
            }

            if ((currentPersonalInfo.getIssuedOrg() == null || currentPersonalInfo.getIssuedOrg().equals("")) &&
                    (personalInfo.getIssuedOrg() != null && !personalInfo.getIssuedOrg().equals(""))) {
                currentPersonalInfo.setIssuedOrg(personalInfo.getIssuedOrg());
            }

            if ((currentPersonalInfo.getAddress() == null || currentPersonalInfo.getAddress().equals("")) &&
                    (personalInfo.getAddress() != null && !personalInfo.getAddress().equals(""))) {
                currentPersonalInfo.setAddress(personalInfo.getAddress());
            }

            if ((currentPersonalInfo.getInn() == null || currentPersonalInfo.getInn().equals("")) &&
                    (personalInfo.getInn() != null && !personalInfo.getInn().equals(""))) {
                currentPersonalInfo.setInn(personalInfo.getInn());
            }

            if ((currentPersonalInfo.getKpp() == null || currentPersonalInfo.getKpp().equals("")) &&
                    (personalInfo.getKpp() != null && !personalInfo.getKpp().equals(""))) {
                currentPersonalInfo.setKpp(personalInfo.getKpp());
            }

            if ((currentPersonalInfo.getOgrn() == null || currentPersonalInfo.getOgrn().equals("")) &&
                    (personalInfo.getOgrn() != null && !personalInfo.getOgrn().equals(""))) {
                currentPersonalInfo.setOgrn(personalInfo.getOgrn());
            }

            if ((currentPersonalInfo.getOkpo() == null || currentPersonalInfo.getOkpo().equals("")) &&
                    (personalInfo.getOkpo() != null && !personalInfo.getOkpo().equals(""))) {
                currentPersonalInfo.setOkpo(personalInfo.getOkpo());
            }

            if ((currentPersonalInfo.getOkvedCodes() == null || currentPersonalInfo.getOkvedCodes().equals("")) &&
                    (personalInfo.getOkvedCodes() != null && !personalInfo.getOkvedCodes().equals(""))) {
                currentPersonalInfo.setOkvedCodes(personalInfo.getOkvedCodes());
            }
        }

        currentAccountOwner.setPersonalInfo(currentPersonalInfo);

        setAllowedFields(currentAccountOwner, accountOwner);
    }

    @Override
    public void setFields(AccountOwner currentAccountOwner, AccountOwner accountOwner) {
        currentAccountOwner.setType(accountOwner.getType());
        currentAccountOwner.setName(accountOwner.getName());
        currentAccountOwner.setPersonalInfo(accountOwner.getPersonalInfo());

        setAllowedFieldsAdmin(currentAccountOwner, accountOwner);
    }

    private void setAllowedFields(AccountOwner currentAccountOwner, AccountOwner accountOwner) {
        if (!currentAccountOwner.equalEmailAdressess(accountOwner)) {
            ContactInfo contactInfo = new ContactInfo(accountOwner.getContactInfo());
            contactInfo.setEmailAddresses(currentAccountOwner.getContactInfo().getEmailAddresses());
            currentAccountOwner.setContactInfo(contactInfo);
        } else {
            currentAccountOwner.setContactInfo(accountOwner.getContactInfo());
        }
    }

    private void setAllowedFieldsAdmin(AccountOwner currentAccountOwner, AccountOwner accountOwner) {
        currentAccountOwner.setContactInfo(accountOwner.getContactInfo());
    }

    private void checkById(String id) {
        if (!exists(id)) {
            throw new ResourceNotFoundException("AccountAbonement с id: " + id + " не найден");
        }
    }
}
