package ru.majordomo.hms.personmgr.service;

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

import ru.majordomo.hms.personmgr.common.ImportConstants;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.notification.AccountNotification;
import ru.majordomo.hms.personmgr.model.notification.Notification;
import ru.majordomo.hms.personmgr.repository.AccountNotificationsRepository;
import ru.majordomo.hms.personmgr.repository.NotificationRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class AccountNotificationDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(AccountNotificationDBImportService.class);

    private AccountNotificationsRepository accountNotificationsRepository;
    private PersonalAccountRepository personalAccountRepository;
    private NamedParameterJdbcTemplate jdbcTemplate;
    private List<AccountNotification> accountNotificationList = new ArrayList<>();

    @Autowired
    public AccountNotificationDBImportService(NamedParameterJdbcTemplate jdbcTemplate, AccountNotificationsRepository accountNotificationsRepository, PersonalAccountRepository personalAccountRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.accountNotificationsRepository = accountNotificationsRepository;
        this.personalAccountRepository = personalAccountRepository;
    }

    public void pull() {
        accountNotificationsRepository.deleteAll();

        List<PersonalAccount> personalAccounts = personalAccountRepository.findAll();

        for (PersonalAccount personalAccount : personalAccounts) {
            this.pull(personalAccount.getName());
        }
    }

    private void pull(String accountName) {
        PersonalAccount personalAccount = personalAccountRepository.findByName(accountName);
        logger.info("Start pull for " + accountName);

        if (personalAccount != null) {
            logger.info("Start pull found account " + accountName);

            AccountNotification accountNotification = new AccountNotification();

            accountNotification.setAccountId(personalAccount.getAccountId());

            accountNotification.addNotification(MailManagerMessageType.EMAIL_NEWS);
            accountNotification.addNotification(MailManagerMessageType.EMAIL_REMAINING_DAYS);
            accountNotification.addNotification(MailManagerMessageType.EMAIL_DOMAIN_DELEGATION_ENDING);

            String query = "SELECT ahs_account_id, ahs_sms_id FROM account_has_sms WHERE ahs_account_id = :ahs_account_id AND ahs_sms_id IN (24, 26, 28, 29, 42, 44, 77)";

            SqlParameterSource namedParametersE = new MapSqlParameterSource("ahs_account_id", personalAccount.getAccountId());

            jdbcTemplate.query(query, namedParametersE, (rs, rowNum) -> {
                accountNotification.addNotification(ImportConstants.getNotifications().get(rs.getInt("ahs_sms_id")));

                return accountNotification;
            });

            query = "SELECT client_id, type FROM email_notify WHERE client_id = :client_id AND type = 'news-off'";

            namedParametersE = new MapSqlParameterSource("client_id", personalAccount.getClientId());

            jdbcTemplate.query(query, namedParametersE, (rs, rowNum) -> {
                accountNotification.removeNotification(MailManagerMessageType.EMAIL_NEWS);

                return accountNotification;
            });

            query = "SELECT client_id, type FROM email_notify WHERE client_id = :client_id AND type = 'domain-delegate'";

            namedParametersE = new MapSqlParameterSource("client_id", personalAccount.getClientId());

            jdbcTemplate.query(query, namedParametersE, (rs, rowNum) -> {
                accountNotification.removeNotification(MailManagerMessageType.EMAIL_DOMAIN_DELEGATION_ENDING);

                return accountNotification;
            });

            query = "SELECT client_id, type FROM email_notify WHERE client_id = :client_id AND type = 'autobill'";

            namedParametersE = new MapSqlParameterSource("client_id", personalAccount.getClientId());

            jdbcTemplate.query(query, namedParametersE, (rs, rowNum) -> {
                accountNotification.removeNotification(MailManagerMessageType.EMAIL_BILLING_DOCUMENTS);

                return accountNotification;
            });

            query = "SELECT acc_id, notify FROM Money WHERE acc_id = :acc_id";

            namedParametersE = new MapSqlParameterSource("acc_id", personalAccount.getAccountId());

            jdbcTemplate.query(query, namedParametersE, (rs, rowNum) -> {
                accountNotification.addNotification(MailManagerMessageType.EMAIL_REMAINING_DAYS);

                return accountNotification;
            });

            accountNotificationList.add(accountNotification);
        }
    }

    public boolean importToMongo() {
        accountNotificationsRepository.deleteAll();
        pull();
        pushToMongo();
        return true;
    }

    public boolean importToMongo(String accountName) {
        accountNotificationsRepository.deleteAll();
        pull(accountName);
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
        try {
            accountNotificationsRepository.save(accountNotificationList);
//            for (AccountNotification accountNotificationElement : accountNotificationList) {
//                AccountNotification findedAccount = accountNotificationsRepository.findByAccountId(accountNotificationElement.getAccountId());
//                if (findedAccount != null) {
//                    findedAccount.addNotifications(accountNotificationElement.getNotifications());
//                    accountNotificationsRepository.save(findedAccount);
//                } else {
//                    accountNotificationsRepository.save(accountNotificationElement);
//                }
//            }
        } catch (ConstraintViolationException e) {
            logger.info(e.getMessage() + " with errors: " + StreamSupport.stream(e.getConstraintViolations().spliterator(), false).map(ConstraintViolation::getMessage).collect(Collectors.joining()));
        }
    }
}
