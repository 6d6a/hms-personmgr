package ru.majordomo.hms.personmgr.service.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.ContactInfo;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.account.PersonalInfo;
import ru.majordomo.hms.personmgr.repository.AccountOwnerRepository;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.rc.user.resources.Person;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class AccountOwnerDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(AccountOwnerDBImportService.class);

    private AccountOwnerRepository accountOwnerRepository;
    private PersonalAccountManager accountManager;
    private RcUserFeignClient rcUserFeignClient;

    @Autowired
    public AccountOwnerDBImportService(
            AccountOwnerRepository accountOwnerRepository,
            PersonalAccountManager accountManager,
            RcUserFeignClient rcUserFeignClient
    ) {
        this.accountOwnerRepository = accountOwnerRepository;
        this.accountManager = accountManager;
        this.rcUserFeignClient = rcUserFeignClient;
    }

    public boolean importToMongo() {
//        accountOwnerRepository.deleteAll();

        try (Stream<PersonalAccount> personalAccountStream = accountManager.findAllStream()) {
            personalAccountStream.forEach(this::pullOwner);
        }
        return true;
    }

    private void pullOwner(PersonalAccount account) {
        if (account.getOwnerPersonId() == null) {
            return;
        } else {
            AccountOwner accountOwner = accountOwnerRepository.findOneByPersonalAccountId(account.getId());
            if (accountOwner != null) {
                accountOwnerRepository.delete(accountOwner);
            }
        }

        AccountOwner accountOwner;
        Person person;

        try {
            person = rcUserFeignClient.getPerson(account.getId(), account.getOwnerPersonId());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("rcUserFeignClient.getPerson got Exception for " + account.getId() + " " + account.getOwnerPersonId() + " exception: " + e.getMessage());
            return;
        }

        if (person != null) {
            accountOwner = AccountOwnerFromPerson(person);
        } else {
            logger.error("rcUserFeignClient.getPerson person == null");
            return;
        }

        try {
            accountOwnerRepository.insert(accountOwner);
        } catch (Exception e) {
            logger.error("[pullOwner][Exception] person: " + person + " accountOwner: " + accountOwner + " exc: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private AccountOwner AccountOwnerFromPerson(Person person) {
        AccountOwner accountOwner = new AccountOwner();
        accountOwner.setName(person.getName());
        accountOwner.setContactInfo(contactInfoFromPerson(person));
        accountOwner.setPersonalAccountId(person.getAccountId());
        accountOwner.setPersonalInfo(personalInfoFromPerson(person));

        if (person.getLegalEntity() != null) {
            accountOwner.setType(AccountOwner.Type.COMPANY);
        } else {
            accountOwner.setType(AccountOwner.Type.INDIVIDUAL);
        }

        return accountOwner;
    }

    private PersonalInfo personalInfoFromPerson(Person person) {
        PersonalInfo personalInfo = new PersonalInfo();

        if (person.getLegalEntity() != null) {
            personalInfo.setInn(person.getLegalEntity().getInn());
            personalInfo.setOgrn(person.getLegalEntity().getOgrn());
            personalInfo.setKpp(person.getLegalEntity().getKpp());
            personalInfo.setOkpo(person.getLegalEntity().getOkpo());
            personalInfo.setOkvedCodes(person.getLegalEntity().getOkvedCodes());
            personalInfo.setAddress(person.getLegalEntity().getAddress());
        }

        if (person.getLegalEntity() == null && person.getPassport() != null) {
            personalInfo.setNumber(person.getPassport().getNumber());
            personalInfo.setIssuedDate(person.getPassport().getIssuedDate());
            personalInfo.setIssuedOrg(person.getPassport().getIssuedOrg());
            personalInfo.setAddress(person.getPassport().getAddress());
        }

        return personalInfo;
    }

    private ContactInfo contactInfoFromPerson(Person person) {
        ContactInfo contactInfo = new ContactInfo();
        contactInfo.setEmailAddresses(person.getEmailAddresses());
        contactInfo.setPhoneNumbers(person.getPhoneNumbers());
        contactInfo.setPostalAddress(person.getPostalAddressAsString());

        if (person.getLegalEntity() != null) {
            contactInfo.setBankAccount(person.getLegalEntity().getBankAccount());
            contactInfo.setBankName(person.getLegalEntity().getBankName());
            contactInfo.setCorrespondentAccount(person.getLegalEntity().getCorrespondentAccount());
            contactInfo.setBik(person.getLegalEntity().getBik());
        }

        return contactInfo;
    }
}
