package ru.majordomo.hms.personmgr.service.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.Address;
import ru.majordomo.hms.personmgr.model.account.LegalEntity;
import ru.majordomo.hms.personmgr.model.account.Passport;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
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
        accountOwnerRepository.deleteAll();

        try (Stream<PersonalAccount> personalAccountStream = accountManager.findAllStream()) {
            personalAccountStream.forEach(this::pullOwner);
        }
        return true;
    }

    private void pullOwner(PersonalAccount account) {
        if (account.getOwnerPersonId() == null) {
            return;
        }

        AccountOwner accountOwner;
        Person person;

        try {
            person = rcUserFeignClient.getPerson(account.getId(), account.getOwnerPersonId());
        } catch (Exception e) {
            logger.error("rcUserFeignClient.getPerson got Exception for " + account.getId() + " " + account.getOwnerPersonId());
            return;
        }

        if (person != null) {
            accountOwner = AccountOwnerFromPerson(person);
        } else {
            logger.error("rcUserFeignClient.getPerson person == null");
            return;
        }

        accountOwnerRepository.insert(accountOwner);
    }


    private AccountOwner AccountOwnerFromPerson(Person person) {
        AccountOwner accountOwner = new AccountOwner();
        accountOwner.setName(person.getName());
        accountOwner.setEmailAddresses(person.getEmailAddresses());
        accountOwner.setPhoneNumbers(person.getPhoneNumbers());

        Address address = AddressFromString(person.getPostalAddressAsString());
        address.setCountry(person.getCountry());

        accountOwner.setPostalAddress(address);

        accountOwner.setPersonalAccountId(person.getAccountId());
        accountOwner.setLegalEntity(LegalEntityFromRcLegalEntity(person.getLegalEntity()));
        accountOwner.setPassport(PassportFromRcPassport(person.getPassport()));

        if (person.getLegalEntity() != null) {
            accountOwner.setType(AccountOwner.Type.COMPANY);
        } else {
            accountOwner.setType(AccountOwner.Type.INDIVIDUAL);
        }

        return accountOwner;
    }

    private LegalEntity LegalEntityFromRcLegalEntity(ru.majordomo.hms.rc.user.resources.LegalEntity rcLegalEntity) {
        if (rcLegalEntity == null) {
            return null;
        }

        LegalEntity legalEntity = new LegalEntity();
        legalEntity.setInn(rcLegalEntity.getInn());
        legalEntity.setOgrn(rcLegalEntity.getOgrn());
        legalEntity.setKpp(rcLegalEntity.getKpp());
        legalEntity.setOkpo(rcLegalEntity.getOkpo());
        legalEntity.setOkvedCodes(rcLegalEntity.getOkvedCodes());
        legalEntity.setBankAccount(rcLegalEntity.getBankAccount());
        legalEntity.setBankName(rcLegalEntity.getBankName());
        legalEntity.setCorrespondentAccount(rcLegalEntity.getCorrespondentAccount());
        legalEntity.setBik(rcLegalEntity.getBik());
        legalEntity.setAddress(AddressFromString(rcLegalEntity.getAddress()));

        return legalEntity;
    }


    private Passport PassportFromRcPassport(ru.majordomo.hms.rc.user.resources.Passport rcPassport) {
        if (rcPassport == null) {
            return null;
        }

        Passport passport = new Passport();
        passport.setNumber(rcPassport.getNumber());
        passport.setIssuedDate(rcPassport.getIssuedDate());
        passport.setIssuedOrg(rcPassport.getIssuedOrg());
        passport.setAddress(AddressFromString(rcPassport.getAddress()));

        return passport;
    }

    private Address AddressFromString(String address) {
        Address newAddress = new Address();

        if (address != null && !address.isEmpty()) {

            newAddress.setZip(findPostalIndexInAddressString(address));
            if (newAddress.getZip() != null) {
                address = address.replaceAll(newAddress.getZip() + "\\s?,?\\s?", "");
            }

            String[] addressParts = address.split(",");
            if (addressParts.length < 2) {
                addressParts = address.split(" ");
            }

            if (addressParts.length >= 2) {
                StringBuilder streetBuilder = new StringBuilder();

                newAddress.setCity(addressParts[0].trim());

                for (int i = 1; i < addressParts.length; i++) {
                    streetBuilder.append(addressParts[i].trim());
                    streetBuilder.append(", ");
                }

                newAddress.setStreet(streetBuilder.toString().trim());
                if (!newAddress.getStreet().isEmpty() && newAddress.getStreet().charAt(newAddress.getStreet().length() - 1) == ',') {
                    newAddress.setStreet(newAddress.getStreet().substring(0, newAddress.getStreet().length() - 1));
                }
            } else {
                newAddress.setStreet(address);
            }
        }

        return newAddress;
    }

    private String findPostalIndexInAddressString(String address) {
        String postalIndexPattern = "\\d{4,}";
        Pattern pattern = Pattern.compile(postalIndexPattern);
        Matcher matcher = pattern.matcher(address);

        return matcher.find() ? matcher.group() : null;
    }
}
