package ru.majordomo.hms.personmgr.service.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import ru.majordomo.hms.personmgr.common.Constants;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class AccountNotificationDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(AccountNotificationDBImportService.class);

    private PersonalAccountRepository personalAccountRepository;
    private NamedParameterJdbcTemplate jdbcTemplate;
    private List<PersonalAccount> personalAccountList = new ArrayList<>();

    @Autowired
    public AccountNotificationDBImportService(NamedParameterJdbcTemplate jdbcTemplate, PersonalAccountRepository personalAccountRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.personalAccountRepository = personalAccountRepository;
    }

    public void pull() {
        List<PersonalAccount> personalAccounts = personalAccountRepository.findAll();

        for (PersonalAccount personalAccount : personalAccounts) {
            this.pull(personalAccount.getName());
        }
    }

    private void pull(String accountName) {
        PersonalAccount personalAccount = personalAccountRepository.findByName(accountName);
        logger.debug("Start pull for " + accountName);

        if (personalAccount != null) {
            logger.debug("Start pull found account " + accountName);

            personalAccount.addNotification(MailManagerMessageType.EMAIL_NEWS);
            personalAccount.addNotification(MailManagerMessageType.EMAIL_REMAINING_DAYS);
            personalAccount.addNotification(MailManagerMessageType.EMAIL_DOMAIN_DELEGATION_ENDING);

            String query = "SELECT ahs_account_id, ahs_sms_id FROM account_has_sms WHERE ahs_account_id = :ahs_account_id AND ahs_sms_id IN (24, 26, 28, 29, 42, 44, 77)";

            SqlParameterSource namedParametersE = new MapSqlParameterSource("ahs_account_id", personalAccount.getAccountId());

            jdbcTemplate.query(query, namedParametersE, (rs, rowNum) -> {
                personalAccount.addNotification(Constants.MANAGER_MESSAGE_TYPE_MAP.get(rs.getInt("ahs_sms_id")));

                return personalAccount;
            });

            query = "SELECT client_id, type FROM email_notify WHERE client_id = :client_id AND type = 'news-off'";

            namedParametersE = new MapSqlParameterSource("client_id", personalAccount.getClientId());

            jdbcTemplate.query(query, namedParametersE, (rs, rowNum) -> {
                personalAccount.removeNotification(MailManagerMessageType.EMAIL_NEWS);

                return personalAccount;
            });

            query = "SELECT client_id, type FROM email_notify WHERE client_id = :client_id AND type = 'domain-delegate'";

            namedParametersE = new MapSqlParameterSource("client_id", personalAccount.getClientId());

            jdbcTemplate.query(query, namedParametersE, (rs, rowNum) -> {
                personalAccount.removeNotification(MailManagerMessageType.EMAIL_DOMAIN_DELEGATION_ENDING);

                return personalAccount;
            });

            query = "SELECT client_id, type FROM email_notify WHERE client_id = :client_id AND type = 'autobill'";

            namedParametersE = new MapSqlParameterSource("client_id", personalAccount.getClientId());

            jdbcTemplate.query(query, namedParametersE, (rs, rowNum) -> {
                personalAccount.removeNotification(MailManagerMessageType.EMAIL_BILLING_DOCUMENTS);

                return personalAccount;
            });

            query = "SELECT acc_id, notify FROM Money WHERE acc_id = :acc_id";

            namedParametersE = new MapSqlParameterSource("acc_id", personalAccount.getAccountId());

            jdbcTemplate.query(query, namedParametersE, (rs, rowNum) -> {
                personalAccount.addNotification(MailManagerMessageType.EMAIL_REMAINING_DAYS);

                return personalAccount;
            });

            personalAccountList.add(personalAccount);
        }
    }

    public boolean importToMongo() {
        pull();
        pushToMongo();
        return true;
    }

    public boolean importToMongo(String accountName) {
        pull(accountName);
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
        try {
            personalAccountRepository.save(personalAccountList);
        } catch (ConstraintViolationException e) {
            logger.debug(e.getMessage() + " with errors: " + StreamSupport.stream(e.getConstraintViolations().spliterator(), false).map(ConstraintViolation::getMessage).collect(Collectors.joining()));
        }
    }
}
