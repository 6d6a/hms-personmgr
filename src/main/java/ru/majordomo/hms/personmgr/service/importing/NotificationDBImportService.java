package ru.majordomo.hms.personmgr.service.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.model.notification.Notification;
import ru.majordomo.hms.personmgr.repository.NotificationRepository;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class NotificationDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(NotificationDBImportService.class);

    private NotificationRepository notificationRepository;
    private List<Notification> notifications = new ArrayList<>();

    @Autowired
    public NotificationDBImportService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void pull() {
        notificationRepository.deleteAll();

        //СМС-ки
        notifications.add(new Notification(MailManagerMessageType.SMS_DOMAIN_DELEGATION_ENDING, "Уведомление об окончании делегирования домена за 7 дней (ежедневно)", "MajordomoDomainsDelegationEnding"));
        notifications.add(new Notification(MailManagerMessageType.SMS_NEW_PAYMENT, "Уведомление о пополнении баланса аккаунта", "MajordomoNewPayment"));
        notifications.add(new Notification(MailManagerMessageType.SMS_NO_MONEY_TO_AUTORENEW_DOMAIN, "Уведомление о недостаточном количестве средств для автоматического продления домена (14 - 5 дней)", "MajordomoNoMoneyToAutoRenewDomain"));
        notifications.add(new Notification(MailManagerMessageType.SMS_REMAINING_DAYS, "Уведомление об окончании средств на услуги хостинга за 5 дней (ежедневно)", "MajordomoRemainingDays"));

        //Почтовые
        notifications.add(new Notification(MailManagerMessageType.EMAIL_CHANGE_ACCOUNT_PASSWORD, "Смена пароля к контрольной панели", "MajordomoVHPassChAccount"));
        notifications.add(new Notification(MailManagerMessageType.EMAIL_CHANGE_FTP_PASSWORD, "Смена пароля к основному FTP-входу", ""));
        notifications.add(new Notification(MailManagerMessageType.EMAIL_LOGIN_TO_CONTROL_PANEL, "Вход в контрольную панель аккаунта", "MajordomoVHEnterToCp"));
        notifications.add(new Notification(MailManagerMessageType.EMAIL_DOMAIN_DELEGATION_ENDING, "Уведомления об окончании делегирования доменов", "MajordomoVHDomainsExpires"));
        notifications.add(new Notification(MailManagerMessageType.EMAIL_REMAINING_DAYS, "Уведомление об окончании средств на контактный email", ""));
        notifications.add(new Notification(MailManagerMessageType.EMAIL_NEWS, "Информационная рассылка «Новости Majordomo» на все почтовые ящики аккаунта", ""));
        notifications.add(new Notification(MailManagerMessageType.EMAIL_BILLING_DOCUMENTS, "Ежемесячная автоматическая выписка документов", ""));
    }

    public boolean importToMongo() {
        pull();
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
        try {
            notificationRepository.save(notifications);
        } catch (ConstraintViolationException e) {
            logger.debug(e.getMessage() + " with errors: " + StreamSupport.stream(e.getConstraintViolations().spliterator(), false).map(ConstraintViolation::getMessage).collect(Collectors.joining()));
        }
    }
}
