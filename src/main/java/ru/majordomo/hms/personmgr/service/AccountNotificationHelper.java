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
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.event.mailManager.SendSmsEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
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

    @Autowired
    public AccountNotificationHelper(
            ApplicationEventPublisher publisher,
            PlanRepository planRepository,
            AccountHelper accountHelper,
            AccountServiceHelper accountServiceHelper,
            NotificationRepository notificationRepository,
            PersonalAccountManager accountManager,
            @Value("${mail_manager.department.fin}") String finEmail
    ) {
        this.publisher = publisher;
        this.planRepository = planRepository;
        this.accountHelper = accountHelper;
        this.accountServiceHelper = accountServiceHelper;
        this.notificationRepository = notificationRepository;
        this.accountManager = accountManager;
        this.finEmail = finEmail;
    }

    public String getCostAbonementForEmail(Plan plan) {
        return accountHelper.getCostAbonement(plan).setScale(2, BigDecimal.ROUND_DOWN).toString();
    }

    public String getDomainForEmail(PersonalAccount account) {

        List<Domain> domains = accountHelper.getDomains(account);
        if (domains != null && !domains.isEmpty()) {
            return domains.stream().map(Domain::getName).collect(Collectors.joining("<br>"));
        }
        return "";
    }

    public String getDomainForEmailWithPrefixString(PersonalAccount account) {

        List<Domain> domains = accountHelper.getDomains(account);
        if (domains != null && !domains.isEmpty()) {
            String prefix = "";
            if (domains.size() == 1) {
                prefix = "На аккаунте размещен домен: ";
            } else {
                prefix = "На аккаунте размещены домены:<br>";
            }
            return prefix + domains.stream().map(Domain::getName).collect(Collectors.joining("<br>"));
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
        BigDecimal costPerMonth = plan.getService().getCost().setScale(2, BigDecimal.ROUND_DOWN);
        Map<String, String> parameters = new HashMap<>();

        parameters.put("acc_id", account.getName());
        parameters.put("date_finish", dateFinish.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        parameters.put("balance", this.getBalanceForEmail(account));
        parameters.put("cost_per_month", costPerMonth.toString());
        parameters.put("cost_abonement", this.getCostAbonementForEmail(plan));
        parameters.put("domains", this.getDomainForEmail(account));
        this.sendMail(account, "MajordomoHmsMoneyEnd", parameters);
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

    public void sendSms(PersonalAccount account, String apiName, int priority) {
        sendSms(account, apiName, priority, null);
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
