package ru.majordomo.hms.personmgr.importing;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.majordomo.hms.personmgr.common.PhoneNumberManager;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.ContactInfo;
import ru.majordomo.hms.personmgr.model.account.PersonalInfo;

@Service
public class AccountOwnerDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(AccountOwnerDBImportService.class);

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final AccountOwnerManager accountOwnerManager;
    private final EmailValidator emailValidator = EmailValidator.getInstance(true, true); //allowLocal, allowTLD

    @Autowired
    public AccountOwnerDBImportService(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            AccountOwnerManager accountOwnerManager
    ) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.accountOwnerManager = accountOwnerManager;
    }

    public void pull() {
        String query = "SELECT a.id, " +
                "c.Client_ID, c.name as client_name, c.phone, c.phone2, c.email, c.email2, c.email3, " +
                "c.passport, '' as passport_number, '' as passport_org, '' as passport_date, " +
                "c.HB as birthdate, c.address as legal_address, c.address_post, c.inn, c.kpp, c.ogrn, c.okpo, c.okved, " +
                "c.bank_rekv, c.bank_name, c.bill as bank_account, c.coor_bill as bank_account_loro, c.bik as bank_bik, c.face, c.company_name " +
                "FROM client c " +
                "JOIN account a USING(client_id) " +
                "ORDER BY a.id ASC";

        namedParameterJdbcTemplate.query(query, this::rowMap);
    }

    public void pull(String accountId) {
        logger.debug("[start] Searching for AccountOwner for acc " + accountId);

        String query = "SELECT a.id, " +
                "c.Client_ID, c.name as client_name, c.phone, c.phone2, c.email, c.email2, c.email3, " +
                "c.passport, '' as passport_number, '' as passport_org, '' as passport_date, " +
                "c.HB as birthdate, c.address as legal_address, c.address_post, c.inn, c.kpp, c.ogrn, c.okpo, c.okved, " +
                "c.bank_rekv, c.bank_name, c.bill as bank_account, c.coor_bill as bank_account_loro, c.bik as bank_bik, c.face, c.company_name " +
                "FROM client c " +
                "JOIN account a USING(client_id) " +
                "WHERE a.id = :accountId " +
                "GROUP BY a.id ";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("accountId", accountId);

        namedParameterJdbcTemplate.query(query,
                namedParameters1,
                this::rowMap
        );

        logger.debug("[finish] Searching for AccountOwner for acc " + accountId);
    }

    private AccountOwner rowMap(ResultSet rs, int rowNum) throws SQLException {
        logger.debug("Found Person for id: " + rs.getString("id") +
                " name: " + rs.getString("client_name"));

        AccountOwner accountOwner = new AccountOwner();
        accountOwner.setName(!rs.getString("client_name").equals("") ?
                rs.getString("client_name") :
                "person_" + rs.getString("id") + "_" + rs.getString("Client_ID")
        );
        accountOwner.setContactInfo(contactInfoFromResultSet(rs));
        accountOwner.setPersonalAccountId(rs.getString("id"));
        accountOwner.setPersonalInfo(personalInfoFromResultSet(rs));

        String personType = rs.getString("face");

        if (personType.equals("ph")) {
            accountOwner.setType(AccountOwner.Type.INDIVIDUAL);
        } else if (personType.equals("ju")) {
            accountOwner.setType(AccountOwner.Type.COMPANY);
        }

        try {
            accountOwnerManager.insert(accountOwner);
        } catch (Exception e) {
            logger.error("[pullOwner][Exception] accountOwner: " + accountOwner + " exc: " + e.getMessage());
            e.printStackTrace();
        }

        return accountOwner;
    }

    public void clean() {
        accountOwnerManager.deleteAll();
    }

    public void clean(String accountId) {
        accountOwnerManager.deleteByPersonalAccountId(accountId);
    }

    public boolean importToMongo() {
        clean();
        pull();
        return true;
    }

    public boolean importToMongo(String accountId) {
        clean(accountId);
        pull(accountId);
        return true;
    }

    private PersonalInfo personalInfoFromResultSet(ResultSet rs) throws SQLException {
        PersonalInfo personalInfo = new PersonalInfo();

        String personType = rs.getString("face");

        if (personType.equals("ph")) {
            String passportFromDatabase = rs.getString("passport");

            if (passportFromDatabase != null && !passportFromDatabase.equals("")) {
                logger.debug("passportFromDatabase: " + passportFromDatabase);

                passportFromDatabase = passportFromDatabase.replaceAll("\r\n", " ");

                // Очистка паспорта от HTML-спецсимволов
                passportFromDatabase = passportFromDatabase.replaceAll("(?u)&#([0-9]+|[a-z]+);", "");

                // Номер паспорта
                Pattern p = Pattern.compile("(?u)([A-Z0-9]+[A-Z0-9\\s]*).*");
                Matcher m = p.matcher(passportFromDatabase);

                if (m.matches()) {
                    personalInfo.setNumber(m.group(1).replaceAll("(?u)\\s+", ""));
                    passportFromDatabase = passportFromDatabase.replaceAll(m.group(1), "");
                }

                // Дата выдачи паспорта
                p = Pattern.compile("(?u).*(\\d{2}\\.\\d{2}\\.\\d{4}).*");
                m = p.matcher(passportFromDatabase);

                if (m.matches()) {
                    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    LocalDate issuedDate;
                    try {
                        issuedDate = LocalDate.parse(m.group(1), dateTimeFormatter);
                        personalInfo.setIssuedDate(issuedDate);
                    } catch (DateTimeParseException e) {
                        logger.error("can not parse issuedDate from passportFromDatabase: " + passportFromDatabase +
                                " for name: " + rs.getString("client_name"));
                    }
                    passportFromDatabase = passportFromDatabase.replaceAll(m.group(1), "");
                }

                // Очиска информации о паспорте от лишних слов и знаков
                if (personalInfo.getIssuedDate() != null || personalInfo.getNumber() != null) {
                    passportFromDatabase = passportFromDatabase.replaceAll("(?uim)(выдан|дата|место|номер|выдачи|паспорта|паспорт|серия)", "");
                    passportFromDatabase = passportFromDatabase.replaceAll("(?um)\\s{2,}", "");
                    passportFromDatabase = passportFromDatabase.replaceAll("^[ ,.]", "");
                    passportFromDatabase = passportFromDatabase.replaceAll("[ ,.]$", "");
                }

                personalInfo.setIssuedOrg(passportFromDatabase);
            }

            String postalAddress = rs.getString("address_post");
            if (postalAddress != null && !postalAddress.equals("")) {
                personalInfo.setAddress(postalAddress);
            }
        } else if (personType.equals("ju")) {
            personalInfo.setInn(rs.getString("inn"));
            personalInfo.setKpp(rs.getString("kpp"));
            personalInfo.setOgrn(rs.getString("ogrn"));
            personalInfo.setAddress(rs.getString("legal_address"));
        }

        return personalInfo;
    }

    private ContactInfo contactInfoFromResultSet(ResultSet rs) throws SQLException {
        ContactInfo contactInfo = new ContactInfo();

        String phone = rs.getString("phone");
        if (phone != null && !phone.equals("") && PhoneNumberManager.phoneValid(phone)) {
            contactInfo.addPhoneNumber(phone);
        }

        phone = rs.getString("phone2");
        if (phone != null && !phone.equals("") && PhoneNumberManager.phoneValid(phone)) {
            contactInfo.addPhoneNumber(phone);
        }

        String email = rs.getString("email");
        if (email != null && !email.equals("") && emailValidator.isValid(email)) {
            contactInfo.addEmailAddress(email);
        }

        email = rs.getString("email2");
        if (email != null && !email.equals("") && emailValidator.isValid(email)) {
            contactInfo.addEmailAddress(email);
        }

        email = rs.getString("email3");
        if (email != null && !email.equals("") && emailValidator.isValid(email)) {
            contactInfo.addEmailAddress(email);
        }

        String postalAddress = rs.getString("address_post");
        if (postalAddress != null && !postalAddress.equals("")) {
            contactInfo.setPostalAddress(postalAddress);
        }

        String personType = rs.getString("face");

        if (personType.equals("ju")) {
            String bankRekv = rs.getString("bank_rekv");
            String bankName = rs.getString("bank_name");
            String bankAccount = rs.getString("bank_account");
            String bankCorrespondentAccount = rs.getString("bank_account_loro");
            String bankBik = rs.getString("bank_bik");

            if (bankRekv != null && !bankRekv.equals("") && (bankName == null || bankName.equals(""))) {
                bankRekv = StringEscapeUtils.unescapeHtml(bankRekv);
                bankRekv = bankRekv.replace("\\", "");

                // Выборка расчетного счета
                Pattern p = Pattern.compile("(?uim)(4[0-9]{19})");
                Matcher m = p.matcher(bankRekv);

                if (m.matches()) {
                    contactInfo.setBankAccount(m.group(1));
                }

                // Выборка кор. счета
                p = Pattern.compile("(?uim)(3[0-9]{19})");
                m = p.matcher(bankRekv);

                if (m.matches()) {
                    contactInfo.setCorrespondentAccount(m.group(1));
                }

                // Выборка БИК
                p = Pattern.compile("(?uim)(0[0-9]{8})");
                m = p.matcher(bankRekv);

                if (m.matches()) {
                    contactInfo.setBik(m.group(1));
                }

                // Выборка названия банка
                String[] splitedBankRekv = bankRekv.split("(?uim)\r?\n", 2);
                contactInfo.setBankName(splitedBankRekv[0]);
            } else {
                if (bankName != null) {
                    bankName = bankName.replace("\\", "");
                }
                contactInfo.setBankName(bankName);
                contactInfo.setBankAccount(bankAccount);
                contactInfo.setCorrespondentAccount(bankCorrespondentAccount);
                contactInfo.setBik(bankBik);
            }
        }

        return contactInfo;
    }
}
