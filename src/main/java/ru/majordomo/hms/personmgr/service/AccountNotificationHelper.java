package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountNotifyRemainingDaysEvent;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.event.mailManager.SendSmsEvent;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.rc.user.resources.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ru.majordomo.hms.personmgr.common.PhoneNumberManager.phoneValid;

@Service
public class AccountNotificationHelper {

    private final static Logger logger = LoggerFactory.getLogger(AccountNotificationHelper.class);

    private final ApplicationEventPublisher publisher;
    private final PlanRepository planRepository;
    private final AccountHelper accountHelper;
    private final AccountServiceHelper accountServiceHelper;

    @Autowired
    public AccountNotificationHelper(
            ApplicationEventPublisher publisher,
            PlanRepository planRepository,
            AccountHelper accountHelper,
            AccountServiceHelper accountServiceHelper
    ) {
        this.publisher = publisher;
        this.planRepository = planRepository;
        this.accountHelper = accountHelper;
        this.accountServiceHelper = accountServiceHelper;
    }

    public String getCostAbonementForEmail(Plan plan) {return accountHelper.getCostAbonement(plan).setScale(2, BigDecimal.ROUND_DOWN).toString();}

    public String getDomainForEmail(PersonalAccount account) {

        List<Domain> domains = accountHelper.getDomains(account);
        if (domains != null && !domains.isEmpty()) {
            return domains.stream().map(Domain::getName).collect(Collectors.joining("<br>"));
        }
        return "";
    }

    public String getBalanceForEmail(PersonalAccount account) {return accountHelper.getBalance(account).setScale(2, BigDecimal.ROUND_DOWN).toString();}

    /*
     * отправим письмо на все ящики аккаунта
     * по умолчанию приоритет 5
     */

    public void sendMail(PersonalAccount account, String apiName, HashMap<String, String> parameters) {
        this.sendMail(account, apiName, 5, parameters);
    }

    public void sendMail(PersonalAccount account, String apiName, int priority, HashMap<String, String> parameters) {

        String email = accountHelper.getEmail(account);
        SimpleServiceMessage message = new SimpleServiceMessage();

        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam("email", email);
        message.addParam("api_name", apiName);
        message.addParam("priority", priority);
        if (parameters != null) {
            message.addParam("parametrs", parameters);
        }

        publisher.publishEvent(new SendMailEvent(message));
    }

    public void sendMailForDeactivatedAccount(PersonalAccount account) {
        this.sendMailForDeactivatedAccount(account, LocalDateTime.now());
    }

    public void sendMailForDeactivatedAccount(PersonalAccount account, LocalDateTime dateFinish) {
        Plan plan = planRepository.findOne(account.getPlanId());
        BigDecimal costPerMonth = plan.getService().getCost().setScale(2, BigDecimal.ROUND_DOWN);
        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("acc_id", account.getName());
        parameters.put("date_finish", dateFinish.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        parameters.put("balance", this.getBalanceForEmail(account));
        parameters.put("cost_per_month", costPerMonth.toString());
        parameters.put("cost_abonement", this.getCostAbonementForEmail(plan));
        parameters.put("domains", this.getDomainForEmail(account));
        this.sendMail(account, "MajordomoHmsMoneyEnd", parameters);
    }

    public void sendInfoMail(PersonalAccount account, String apiName) {
        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("client_id", account.getAccountId());
        this.sendMail(account, apiName, 1, parameters);
    }

    public boolean hasActiveSmsNotificationsAndMessageType(PersonalAccount account, MailManagerMessageType messageType) {
        return (account.hasNotification(messageType) && accountServiceHelper.hasSmsNotifications(account));
    }

    public void sendSms(PersonalAccount account, String apiName, int priority) {
        sendSms(account, apiName, priority, null);
    }

    public void sendSms(PersonalAccount account, String apiName, int priority, HashMap<String, String> parameters) {
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
        message.addParam("api_name", apiName);
        message.addParam("priority", priority);
        if (parameters != null && !parameters.isEmpty()) {
            message.addParam("parametrs", parameters);
        } else {
            parameters = new HashMap<>();
            parameters.put("client_id", account.getId());
        }
        publisher.publishEvent(new SendSmsEvent(message));
    }

    public void sendNotificationsRemainingDays(PersonalAccount account, BigDecimal dailyCost) {
        BigDecimal balance = accountHelper.getBalance(account);
        Integer remainingDays = (balance.divide(dailyCost, 0, BigDecimal.ROUND_DOWN)).intValue();
        if (remainingDays > 0 && account.isActive()) {
            //Отправляем техническое уведомление на почту об окончании средств за 7, 5, 3, 2, 1 дней
            if (Arrays.asList(7, 5, 3, 2, 1).contains(remainingDays)) {
                Map<String, Object> params = new HashMap<>();
                params.put("remainingDays", remainingDays);
                publisher.publishEvent(new AccountNotifyRemainingDaysEvent(account, params));
            }
            //Отправим смс тем, у кого подключена услуга
            if (Arrays.asList(5, 3, 1).contains(remainingDays)) {
                if (this.hasActiveSmsNotificationsAndMessageType(account, MailManagerMessageType.SMS_REMAINING_DAYS)) {
                    HashMap<String, String> parameters = new HashMap<>();
                    parameters.put("remaining_days", Utils.pluralizef("остался %d день", "осталось %d дня", "осталось %d дней", remainingDays));
                    parameters.put("client_id", account.getAccountId());
                    this.sendSms(account, "MajordomoRemainingDays", 10, parameters);
                }
            }
        }
    }
}
