package ru.majordomo.hms.personmgr.service;

import org.apache.http.util.EncodingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import ru.majordomo.hms.personmgr.common.ImportConstants;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.model.AccountHistory;
import ru.majordomo.hms.personmgr.model.notification.AccountNotifications;
import ru.majordomo.hms.personmgr.model.notification.Notification;
import ru.majordomo.hms.personmgr.repository.AccountHistoryRepository;
import ru.majordomo.hms.personmgr.repository.AccountNotificationsRepository;
import ru.majordomo.hms.personmgr.repository.NotificationRepository;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class NotificationDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(NotificationDBImportService.class);

    private NotificationRepository notificationRepository;
    private AccountNotificationsRepository accountNotificationsRepository;
    private NamedParameterJdbcTemplate jdbcTemplate;
    private List<Notification> notifications = new ArrayList<>();
    private List<AccountNotifications> accountNotificationsList = new ArrayList<>();

    @Autowired
    public NotificationDBImportService(NamedParameterJdbcTemplate jdbcTemplate, NotificationRepository notificationRepository, AccountNotificationsRepository accountNotificationsRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.notificationRepository = notificationRepository;
        this.accountNotificationsRepository = accountNotificationsRepository;
    }

    public void pull() {
        notificationRepository.deleteAll();

        notifications.add(new Notification(MailManagerMessageType.SMS_DOMAIN_DELEGATION_ENDING, "Уведомление об окончании делегирования домена за 7 дней (ежедневно)", "MajordomoDomainsDelegationEnding"));
        notifications.add(new Notification(MailManagerMessageType.SMS_NEW_PAYMENT, "Уведомление о пополнении баланса аккаунта", "MajordomoNewPayment"));
        notifications.add(new Notification(MailManagerMessageType.SMS_NO_MONEY_TO_AUTORENEW_DOMAIN, "Уведомление о недостаточном количестве средств для автоматического продления домена (14 - 5 дней)", "MajordomoNoMoneyToAutoRenewDomain"));
        notifications.add(new Notification(MailManagerMessageType.SMS_REMAINING_DAYS, "Уведомление об окончании средств на услуги хостинга за 5 дней (ежедневно)", "MajordomoRemainingDays"));

        accountNotificationsRepository.deleteAll();

        String query = "SELECT ahs_account_id, ahs_sms_id FROM account_has_sms WHERE ahs_sms_id IN (24, 26, 28, 29)";


        accountNotificationsList.addAll(jdbcTemplate.query(query, (rs, rowNum) -> {
            AccountNotifications accountNotifications = new AccountNotifications();

            for (AccountNotifications accountNotification : accountNotificationsList) {
                if (accountNotification.getAccountId().equals(rs.getString("ahs_account_id"))) {
                    accountNotifications = accountNotification;
                }
            }

            accountNotifications.setAccountId(rs.getString("ahs_account_id"));

            accountNotifications.addNotification(ImportConstants.getSmsNotifications().get(rs.getInt("ahs_sms_id")));

            return accountNotifications;
        }));
    }

    public boolean importToMongo() {
        pull();
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
        try {
            notificationRepository.save(notifications);

            for (AccountNotifications accountNotification : accountNotificationsList) {
                AccountNotifications findedAccount = accountNotificationsRepository.findByAccountId(accountNotification.getAccountId());
                if (findedAccount != null) {
                    findedAccount.addNotifications(accountNotification.getNotifications());
                    accountNotificationsRepository.save(findedAccount);
                } else {
                    accountNotificationsRepository.save(accountNotification);
                }
            }

        } catch (ConstraintViolationException e) {
            logger.info(e.getMessage() + " with errors: " + StreamSupport.stream(e.getConstraintViolations().spliterator(), false).map(ConstraintViolation::getMessage).collect(Collectors.joining()));
        }
    }
}
