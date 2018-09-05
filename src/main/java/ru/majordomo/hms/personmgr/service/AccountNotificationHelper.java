package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountNotificationRemainingDaysWasSentEvent;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.event.mailManager.SendSmsEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.notification.Notification;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.NotificationRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.rc.user.resources.Domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.PhoneNumberManager.phoneValid;
import static ru.majordomo.hms.personmgr.common.Utils.formatBigDecimalWithCurrency;

@Service
public class AccountNotificationHelper {

    private static final String EMAIL = "EMAIL";
    private static final String SMS = "SMS";

    private final static Logger logger = LoggerFactory.getLogger(AccountNotificationHelper.class);

    private final ApplicationEventPublisher publisher;
    private final PlanRepository planRepository;
    private final AccountHelper accountHelper;
    private final AccountServiceHelper accountServiceHelper;
    private final NotificationRepository notificationRepository;
    private final PersonalAccountManager accountManager;
    private final String finEmail;
    private final String inviteEmailApiName;

    @Autowired
    public AccountNotificationHelper(
            ApplicationEventPublisher publisher,
            PlanRepository planRepository,
            AccountHelper accountHelper,
            AccountServiceHelper accountServiceHelper,
            NotificationRepository notificationRepository,
            PersonalAccountManager accountManager,
            @Value("${mail_manager.department.fin}") String finEmail,
            @Value("${invites.client_api_name}") String inviteEmailApiName
    ) {
        this.publisher = publisher;
        this.planRepository = planRepository;
        this.accountHelper = accountHelper;
        this.accountServiceHelper = accountServiceHelper;
        this.notificationRepository = notificationRepository;
        this.accountManager = accountManager;
        this.finEmail = finEmail;
        this.inviteEmailApiName = inviteEmailApiName;
    }

    public String getDomainForEmail(PersonalAccount account) {

        List<Domain> domains = accountHelper.getDomains(account);
        if (domains != null && !domains.isEmpty()) {
            return domains.stream().map(Domain::getName).collect(Collectors.joining("<br>"));
        }
        return "";
    }

    public String getBalanceForEmail(PersonalAccount account) {
        return formatBigDecimalForEmail(accountHelper.getBalance(account));
    }

    public String formatBigDecimalForEmail(BigDecimal number) {
        return number.setScale(2, BigDecimal.ROUND_DOWN).toString();
    }

    public void sendNotification(SimpleServiceMessage message) {

        PersonalAccount account = accountManager.findOne(message.getAccountId());

        if (account == null) {
            throw new ParameterValidationException(this.getClass().getSimpleName() + " Аккаунт с id " + message.getAccountId() + " не найден");
        }

        Map<String, Object> params = message.getParams();
        if (params == null || params.isEmpty()) {
            throw  new ParameterValidationException(this.getClass().getSimpleName() + " Отсутствуют необходимые параметры params " + message);
        }

        Map<String, String> paramsForMailManager;

        if (params.containsKey(PARAMETRS_KEY) && params.get(PARAMETRS_KEY) != null) {
            paramsForMailManager = (Map<String, String>) params.get(PARAMETRS_KEY);
        } else {
            paramsForMailManager = new HashMap<>();
        }

        paramsForMailManager.put(CLIENT_ID_KEY, account.getAccountId());
        paramsForMailManager.put(ACC_ID_KEY, account.getName());

        int priority = 5;
        if (params.containsKey(PRIORITY_KEY)) { priority = (Integer) params.get(PRIORITY_KEY); }

        String apiName = (String) params.get(API_NAME_KEY);

        switch ((String) params.get(TYPE_KEY)){
            case EMAIL:
                this.sendMail(
                        account,
                        apiName,
                        priority,
                        paramsForMailManager
                );
                break;
            case SMS:
                this.sendSms(
                        account,
                        apiName,
                        priority,
                        paramsForMailManager
                );
                break;
            default:
                throw  new ParameterValidationException("Not implemented NotificationType " + params.get(TYPE_KEY));
        }
    }

    /*
     * отправим письмо на все ящики аккаунта
     * по умолчанию приоритет 5
     */

    public void sendMail(PersonalAccount account, String apiName, Map<String, String> parameters) {
        this.sendMail(account, apiName, 5, parameters);
    }

    public void sendMailWithAttachement(
            PersonalAccount account,
            String email,
            String apiName,
            int priority,
            Map<String, String> parameters,
            Map<String, String> attachment
    ){
        SimpleServiceMessage message = new SimpleServiceMessage();

        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam(EMAIL_KEY, email);
        message.addParam(API_NAME_KEY, apiName);
        message.addParam(PRIORITY_KEY, priority);
        if (parameters != null) {
            message.addParam(PARAMETRS_KEY, parameters);
        }
        message.addParam("attachment", attachment);
        publisher.publishEvent(new SendMailEvent(message));
    }

    public void sendMail(PersonalAccount account, String apiName, int priority, Map<String, String> parameters) {

        String email = accountHelper.getEmail(account);
        SimpleServiceMessage message = new SimpleServiceMessage();

        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam(EMAIL_KEY, email);
        message.addParam(API_NAME_KEY, apiName);
        message.addParam(PRIORITY_KEY, priority);
        if (parameters != null) {
            message.addParam(PARAMETRS_KEY, parameters);
        }

        publisher.publishEvent(new SendMailEvent(message));
    }

    public void sendMailForDeactivatedAccount(PersonalAccount account, LocalDate dateFinish) {
        Plan plan = planRepository.findOne(account.getPlanId());
        Map<String, String> parameters = new HashMap<>();

        parameters.put("acc_id", account.getName());
        parameters.put("date_finish", dateFinish.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        parameters.put("balance", this.getBalanceForEmail(account));
        parameters.put("cost_per_month", formatBigDecimalWithCurrency(plan.getService().getCost()));//У нас есть тарифы abonementOnly, возможно, стоит как-то по другому писать в письме для них цену
        parameters.put("cost_abonement", formatBigDecimalWithCurrency(plan.getDefaultP1YAbonementCost()));
        parameters.put("domains", this.getDomainForEmail(account));
        this.sendMail(account, "MajordomoHmsMoneyEnd", parameters);
    }

    public void sendMailDeactivatedWithExpiredCredit(PersonalAccount account) {
        Plan plan = planRepository.findOne(account.getPlanId());

        BigDecimal balance = accountHelper.getBalance(account);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("domains", getDomainForEmail(account));
        parameters.put("balance", formatBigDecimalWithCurrency(balance));
        parameters.put("date_finish", "");
        parameters.put("cost_per_month", formatBigDecimalWithCurrency(plan.getService().getCost()));//У нас есть тарифы abonementOnly, возможно, стоит как-то по другому писать в письме для них цену
        parameters.put("cost_abonement", formatBigDecimalWithCurrency(plan.getDefaultP1YAbonementCost()));
        parameters.put("from", "noreply@majordomo.ru");

        sendMail(account, "MajordomoHmsServicesCreditMoneyEnd", 1, parameters);
    }

    public void sendMailCreditJustActivatedWithHostingAbonement(PersonalAccount account) {
        BigDecimal balance = accountHelper.getBalance(account);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("domains", getDomainForEmail(account));
        parameters.put("balance", formatBigDecimalWithCurrency(balance));
        parameters.put("from", "noreply@majordomo.ru");

        String apiName = "MajordomoHmsAddServicesHostingVCredit";

        sendMail(account, apiName, 1, parameters);

        publisher.publishEvent(
                new AccountNotificationRemainingDaysWasSentEvent(
                        account.getId(),
                        apiName
                )
        );
    }

    public void sendMailCreditJustActivated(PersonalAccount account) {
        BigDecimal balance = accountHelper.getBalance(account);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("domains", getDomainForEmail(account));
        parameters.put("balance", formatBigDecimalWithCurrency(balance));
        parameters.put("from", "noreply@majordomo.ru");

        String apiName = "MajordomoHmsAllServicesHostingVCredit";

        sendMail(account, apiName, 1, parameters);

        publisher.publishEvent(
                new AccountNotificationRemainingDaysWasSentEvent(
                        account.getId(),
                        apiName
                )
        );
    }

    public void sendMailCreditExpiringWithHostingAbonement(PersonalAccount account) {
        BigDecimal balance = accountHelper.getBalance(account);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("domains", getDomainForEmail(account));
        parameters.put("balance", formatBigDecimalWithCurrency(balance));
        parameters.put("date_finish", "через " + Utils.pluralizef("%d день", "%d дня", "%d дней", getRemainingDaysCreditPeriod(account)));
        parameters.put("from", "noreply@majordomo.ru");

        String apiName = "MajordomoHmsServicesCreditMoneySoonEndAbonement";

        sendMail(account, apiName, 1, parameters);

        publisher.publishEvent(
                new AccountNotificationRemainingDaysWasSentEvent(
                        account.getId(),
                        apiName
                )
        );
    }

    public void sendMailCreditExpiring(PersonalAccount account) {
        BigDecimal balance = accountHelper.getBalance(account);

        Plan plan = planRepository.findOne(account.getPlanId());

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("domains", getDomainForEmail(account));
        parameters.put("balance", formatBigDecimalWithCurrency(balance));
        parameters.put("date_finish", "через " + Utils.pluralizef("%d день", "%d дня", "%d дней", getRemainingDaysCreditPeriod(account)));
        parameters.put("cost_per_month", formatBigDecimalWithCurrency(plan.getService().getCost()));//У нас есть тарифы abonementOnly, возможно, стоит как-то по другому писать в письме для них цену
        parameters.put("cost_abonement", formatBigDecimalWithCurrency(plan.getDefaultP1YAbonementCost()));
        parameters.put("from", "noreply@majordomo.ru");

        String apiName = "MajordomoHmsCreditMoneySoonEnd";

        sendMail(account, apiName, 1, parameters);

        publisher.publishEvent(
                new AccountNotificationRemainingDaysWasSentEvent(
                        account.getId(),
                        apiName
                )
        );
    }

    public void sendMailCreditExpiredWithHostingAbonement(PersonalAccount account) {
        BigDecimal balance = accountHelper.getBalance(account);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("domains", getDomainForEmail(account));
        parameters.put("balance", formatBigDecimalWithCurrency(balance));
        parameters.put("date_finish", "");
        parameters.put("from", "noreply@majordomo.ru");

        String apiName = "MajordomoHmsAddServicesCreditMoneyEndAbonement";

        sendMail(account, apiName, 1, parameters);

        publisher.publishEvent(
                new AccountNotificationRemainingDaysWasSentEvent(
                        account.getId(),
                        apiName
                )
        );
    }

    public void sendMailServicesDisabledWithHostingAbonement(PersonalAccount account) {
        BigDecimal balance = accountHelper.getBalance(account);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("domains", getDomainForEmail(account));
        parameters.put("balance", formatBigDecimalWithCurrency(balance));
        parameters.put("date_finish", "");
        parameters.put("from", "noreply@majordomo.ru");

        String apiName = "MajordomoHmsAddServicesMoneyEndAbonement";

        sendMail(account, apiName, 1, parameters);

        publisher.publishEvent(
                new AccountNotificationRemainingDaysWasSentEvent(
                        account.getId(),
                        apiName
                )
        );
    }

    public void sendMailServicesExpiringWithHostingAbonement(PersonalAccount account, int remainingDays) {
        BigDecimal balance = accountHelper.getBalance(account);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("domains", getDomainForEmail(account));
        parameters.put("balance", formatBigDecimalWithCurrency(balance));
        parameters.put("date_finish", "через " + Utils.pluralizef("%d день", "%d дня", "%d дней", remainingDays));
        parameters.put("from", "noreply@majordomo.ru");

        String apiName = "MajordomoHmsAddServicesMoneySoonEndAbonement";

        sendMail(account, apiName, 1, parameters);

        publisher.publishEvent(
                new AccountNotificationRemainingDaysWasSentEvent(
                        account.getId(),
                        apiName
                )
        );
    }

    public void sendMailServicesExpiring(PersonalAccount account, int remainingDays) {
        BigDecimal balance = accountHelper.getBalance(account);

        Plan plan = planRepository.findOne(account.getPlanId());

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("domains", getDomainForEmail(account));
        parameters.put("balance", formatBigDecimalWithCurrency(balance));
        parameters.put("date_finish", "через " + Utils.pluralizef("%d день", "%d дня", "%d дней", remainingDays));
        parameters.put("cost_per_month", formatBigDecimalWithCurrency(plan.getService().getCost()));//У нас есть тарифы abonementOnly, возможно, стоит как-то по другому писать в письме для них цену
        parameters.put("cost_abonement", formatBigDecimalWithCurrency(plan.getDefaultP1YAbonementCost()));
        parameters.put("from", "noreply@majordomo.ru");

        String apiName = "MajordomoHmsMoneySoonEnd";

        sendMail(account, apiName, 1, parameters);

        publisher.publishEvent(
                new AccountNotificationRemainingDaysWasSentEvent(
                        account.getId(),
                        apiName
                )
        );
    }

    public void sendInviteMail(PersonalAccount account, String emailForInvite, String code) {
        AccountOwner owner = accountHelper.getOwnerByPersonalAccountId(account.getId());

        Map<String, String> parameters = new HashMap<>();
        parameters.put("owner_email", owner.getContactInfo().getEmailAddresses().get(0));
        parameters.put("from", "noreply@majordomo.ru");
        parameters.put("code", code);

        sendInternalEmail(emailForInvite, inviteEmailApiName, account.getId(), 10, parameters);
    }

    public void sendInfoMail(PersonalAccount account, String apiName) {
        Map<String, String> parameters = new HashMap<>();

        parameters.put(CLIENT_ID_KEY, account.getAccountId());
        this.sendMail(account, apiName, 1, parameters);
    }

    public boolean isSubscribedToSmsType(PersonalAccount account, MailManagerMessageType messageType) {
        return (
                account.hasNotification(messageType)
                && notificationRepository.findByTypeAndActive(messageType, true) != null
                && accountServiceHelper.hasSmsNotifications(account)
        );
    }

    public void sendSms(PersonalAccount account, String apiName, int priority, Map<String, String> parameters) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        String smsPhone = account.getSmsPhoneNumber();
        if (smsPhone == null || smsPhone.equals("")) {
            logger.error("AccountSmsPhoneNumber is missing, accountName [" + account.getName() + "], parameters: " + (parameters != null ? parameters : ""));
            return;
        }

        //Для mailmanager нужно преобразовать телефон в вид 7XXXXXXXXXX

        if (!phoneValid(smsPhone)) {
            logger.error("settings.SMS_PHONE_NUMBER [" + smsPhone + "] not valid, accountId : " + account.getId());
            return;
        }

        String smsPhoneForMailManager = smsPhone.replaceAll("(\\+7|^8)", "7").replaceAll("[^\\d]", "");

        //Отправляем только по РФ
        Pattern p = Pattern.compile("^7[\\d]{10}$");
        Matcher m = p.matcher(smsPhoneForMailManager);
        if (!m.matches()) {
            logger.error("smsPhoneForMailManager [" + smsPhoneForMailManager + "] not valid for mail-manager, must be [7xxxxxxxxxx] , accountId : " + account.getId());
            return;
        }

        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam("phone", smsPhoneForMailManager);
        message.addParam(API_NAME_KEY, apiName);
        message.addParam(PRIORITY_KEY, priority);
        if (parameters != null && !parameters.isEmpty()) {
            message.addParam(PARAMETRS_KEY, parameters);
        } else {
            parameters = new HashMap<>();
            parameters.put(CLIENT_ID_KEY, account.getId());
        }
        publisher.publishEvent(new SendSmsEvent(message));
    }

    /*
     *  возвращает количество оставшихся дней кредитного периода
     *  если кредита нет или кредитный период истёк, то вернет 0
     *  если кредит еще не начался, то вернет весь срок кредита
     */
    public Integer getRemainingDaysCreditPeriod(PersonalAccount account) {
        //Если кредита нет или он закончился, то 0 дней
        Integer remainingDays = 0;
        if (!accountHelper.hasActiveCredit(account)) {
            return 0;
        } else {
            LocalDateTime creditActivationDate = account.getCreditActivationDate();
            if (creditActivationDate == null) { creditActivationDate = LocalDateTime.now(); }
            LocalDate maxCreditActivationDate = LocalDateTime.now().minus(Period.parse(account.getCreditPeriod())).toLocalDate();
            remainingDays = Utils.getDifferentInDaysBetweenDates(maxCreditActivationDate, creditActivationDate.toLocalDate());
        }
        return remainingDays;
    }

    public List<MailManagerMessageType> getActiveMailManagerMessageTypes() {
        return notificationRepository.findByActive(true).stream()
                .map(Notification::getType).collect(Collectors.toList());
    }

    public boolean hasActiveSmsNotifications(PersonalAccount account) {
        List<MailManagerMessageType> activeNotificationTypes = this.getActiveMailManagerMessageTypes();

        return account.getNotifications().stream()
                .anyMatch(mailManagerMessageType -> mailManagerMessageType.name().startsWith("SMS_")
                        && activeNotificationTypes.contains(mailManagerMessageType));
    }

    public void sendEmailToFinDepartment(String apiName, String personalAccountId, Map<String, String> parameters){
        this.sendInternalEmail(finEmail, apiName, personalAccountId, 10, parameters);
    }

    public void sendInternalEmail(String departmentEmail, String apiName, String personalAccountId, int priority, Map<String, String> parameters){
        SimpleServiceMessage message = new SimpleServiceMessage();

        message.setAccountId(personalAccountId);
        message.setParams(new HashMap<>());
        message.addParam(EMAIL_KEY, departmentEmail);
        message.addParam(API_NAME_KEY, apiName);
        message.addParam(PRIORITY_KEY, priority);

        if (parameters == null || parameters.isEmpty()) {
            parameters = new HashMap<>();
            parameters.put(CLIENT_ID_KEY, message.getAccountId());
        }

        message.addParam(PARAMETRS_KEY, parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }
}
