package ru.majordomo.hms.personmgr.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.TelegramMessageData;
import ru.majordomo.hms.personmgr.dto.push.LowBalancePush;
import ru.majordomo.hms.personmgr.dto.push.Push;
import ru.majordomo.hms.personmgr.dto.fin.PaymentLinkRequest;
import ru.majordomo.hms.personmgr.event.account.AccountNotificationRemainingDaysWasSentEvent;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.event.mailManager.SendSmsEvent;
import ru.majordomo.hms.personmgr.exception.BaseException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.notification.Notification;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.telegram.AccountTelegram;
import ru.majordomo.hms.personmgr.repository.AccountTelegramRepository;
import ru.majordomo.hms.personmgr.repository.NotificationRepository;
import ru.majordomo.hms.rc.user.resources.Domain;

import javax.annotation.ParametersAreNonnullByDefault;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.PhoneNumberManager.phoneValid;
import static ru.majordomo.hms.personmgr.common.Utils.formatBigDecimalWithCurrency;

@Service
@RequiredArgsConstructor
public class AccountNotificationHelper {

    private static final String EMAIL = "EMAIL";
    private static final String SMS = "SMS";

    private final static Logger logger = LoggerFactory.getLogger(AccountNotificationHelper.class);

    @Value("${mail_manager.department.fin}")
    private final String finEmail;

    @Value("${invites.client_api_name}")
    private final String inviteEmailApiName;

    @Value("${mail_manager.dev_email}")
    private final String devEmail;

    @Value("${mail_manager.service_message_api_name}")
    private final String serviceMessageApiName;

    @Value("${spring.profiles.active}")
    private final String activeProfiles;

    @Value("${spring.application.name}")
    private final String applicationName;

    private final ApplicationEventPublisher publisher;
    private final PlanManager planManager;
    private final AccountHelper accountHelper;
    private final AccountServiceHelper accountServiceHelper;
    private final NotificationRepository notificationRepository;
    private final PersonalAccountManager accountManager;
    private final PaymentLinkHelper paymentLinkHelper;
    private final PushService pushService;
    private final TelegramRestClient telegramRestClient;
    private final AccountTelegramRepository accountTelegramRepository;


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

        if (params.containsKey(SEND_ONLY_TO_ACTIVE_KEY) && (boolean) params.get(SEND_ONLY_TO_ACTIVE_KEY) && (!account.isActive() || account.getDeleted() != null)) {
            return;
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

    public void sendMailIfAbonementNotBought(PersonalAccount account, Plan currentPlan) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("acc_id", account.getName());
        parameters.put("plan_name", currentPlan.getName());

        sendMail(account, "HmsMjBuyAbonement", parameters);
    }

    public void sendMailForDeactivatedAccount(PersonalAccount account, LocalDate dateFinish) {
        Plan plan = planManager.findOne(account.getPlanId());
        Map<String, String> parameters = new HashMap<>();

        String paymentLink = paymentLinkHelper.generatePaymentLinkForMail(
                account,
                new PaymentLinkRequest(accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService()))
        ).getPaymentLink();

        parameters.put("acc_id", account.getName());
        parameters.put("date_finish", dateFinish.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        parameters.put("balance", this.getBalanceForEmail(account));
        parameters.put("cost_per_month", formatBigDecimalWithCurrency(
                accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService())
        ));//У нас есть тарифы abonementOnly, возможно, стоит как-то по другому писать в письме для них цену
        parameters.put("cost_abonement", formatBigDecimalWithCurrency(accountServiceHelper.getServiceCostDependingOnDiscount(
                    account.getId(), plan.getDefaultP1YAbonementService()
        )));
        parameters.put("domains", this.getDomainForEmail(account));
        parameters.put("payment_link", paymentLink);

        this.sendMail(account, "MajordomoHmsMoneyEnd", parameters);

        push(
                new LowBalancePush(account.getId(),
                        account.getName() + " выключен",
                        "Закончились средства на балансе. Аккаунт " + account.getName() + " блокирован.",
                        plan.isAbonementOnly() ? accountServiceHelper.getServiceCostDependingOnDiscount(
                    account.getId(), plan.getDefaultP1YAbonementService()
                        ) : accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService())
                )
        );
    }

    public void sendMailDeactivatedWithExpiredCredit(PersonalAccount account) {
        Plan plan = planManager.findOne(account.getPlanId());

        BigDecimal balance = accountHelper.getBalance(account);

        String paymentLink = paymentLinkHelper.generatePaymentLinkForMail(
                account,
                new PaymentLinkRequest(accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService()))
        ).getPaymentLink();

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("domains", getDomainForEmail(account));
        parameters.put("balance", formatBigDecimalWithCurrency(balance));
        parameters.put("date_finish", "");
        parameters.put("cost_per_month", formatBigDecimalWithCurrency(
                accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService()))
        );//У нас есть тарифы abonementOnly, возможно, стоит как-то по другому писать в письме для них цену
        parameters.put("cost_abonement", formatBigDecimalWithCurrency(accountServiceHelper.getServiceCostDependingOnDiscount(
                    account.getId(), plan.getDefaultP1YAbonementService()
        )));
        parameters.put("from", "noreply@majordomo.ru");
        parameters.put("payment_link", paymentLink);

        sendMail(account, "MajordomoHmsServicesCreditMoneyEnd", 1, parameters);

        push(
                new LowBalancePush(account.getId(),
                        account.getName() + " выключен",
                        "Закончились кредитные средства на услуги хостинга на аккаунте " + account.getName(),
                        plan.isAbonementOnly() ? accountServiceHelper.getServiceCostDependingOnDiscount(
                    account.getId(), plan.getDefaultP1YAbonementService()
                        ) :
                                accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService())
                )
        );
    }

    public void sendMailCreditJustActivatedWithHostingAbonement(PersonalAccount account) {
        Plan plan = planManager.findOne(account.getPlanId());

        BigDecimal balance = accountHelper.getBalance(account);

        String paymentLink = paymentLinkHelper.generatePaymentLinkForMail(
                account,
                new PaymentLinkRequest(accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService()))
        ).getPaymentLink();

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("domains", getDomainForEmail(account));
        parameters.put("balance", formatBigDecimalWithCurrency(balance));
        parameters.put("from", "noreply@majordomo.ru");
        parameters.put("payment_link", paymentLink);

        String apiName = "MajordomoHmsAddServicesHostingVCredit";

        sendMail(account, apiName, 1, parameters);

        push(
                new LowBalancePush(account.getId(),
                        account.getName() + " включена услуга \"Хостинг в кредит\"",
                        "Закончились средства на балансе. Аккаунт " + account.getName() + " блокирован.",
                        plan.isAbonementOnly() ? accountServiceHelper.getServiceCostDependingOnDiscount(
                    account.getId(), plan.getDefaultP1YAbonementService()
                        ) : accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService())
                )
        );

        publisher.publishEvent(
                new AccountNotificationRemainingDaysWasSentEvent(
                        account.getId(),
                        apiName
                )
        );
    }

    public void sendMailCreditJustActivated(PersonalAccount account) {
        Plan plan = planManager.findOne(account.getPlanId());

        BigDecimal balance = accountHelper.getBalance(account);

        String paymentLink = paymentLinkHelper.generatePaymentLinkForMail(
                account,
                new PaymentLinkRequest(accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService()))
        ).getPaymentLink();

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("domains", getDomainForEmail(account));
        parameters.put("balance", formatBigDecimalWithCurrency(balance));
        parameters.put("from", "noreply@majordomo.ru");
        parameters.put("payment_link", paymentLink);

        String apiName = "MajordomoHmsAllServicesHostingVCredit";

        sendMail(account, apiName, 1, parameters);

        push(
                new LowBalancePush(account.getId(),
                        account.getName() + " включена услуга \"Хостинг в кредит\"",
                        "Средства на услуги хостинга на Вашем аккаунте " + account.getName() + " закончились. Включена услуга \"Хостинг в кредит\" на 14 дней",
                        plan.isAbonementOnly() ? accountServiceHelper.getServiceCostDependingOnDiscount(
                    account.getId(), plan.getDefaultP1YAbonementService()
                        ) : accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService())
                )
        );

        publisher.publishEvent(
                new AccountNotificationRemainingDaysWasSentEvent(
                        account.getId(),
                        apiName
                )
        );
    }

    public void sendMailCreditExpiringWithHostingAbonement(PersonalAccount account) {
        Plan plan = planManager.findOne(account.getPlanId());

        BigDecimal balance = accountHelper.getBalance(account);

        String paymentLink = paymentLinkHelper.generatePaymentLinkForMail(
                account,
                new PaymentLinkRequest(accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService()))
        ).getPaymentLink();

        String dateFinish = "через " + Utils.pluralizeDays(getRemainingDaysCreditPeriod(account));

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("domains", getDomainForEmail(account));
        parameters.put("balance", formatBigDecimalWithCurrency(balance));
        parameters.put("date_finish", dateFinish);
        parameters.put("from", "noreply@majordomo.ru");
        parameters.put("payment_link", paymentLink);

        String apiName = "MajordomoHmsServicesCreditMoneySoonEndAbonement";

        sendMail(account, apiName, 1, parameters);

        push(
                new LowBalancePush(account.getId(),
                        account.getName() + " Заканчиваются кредит. средства на доп.услуги",
                        "Кредитные средства на оплату доп.услуг хостинга (SMS-информирование, увеличение квоты и т. д.) " +
                                "на аккаунте " + account.getName() + " заканчиваются " + dateFinish,
                        plan.isAbonementOnly() ? accountServiceHelper.getServiceCostDependingOnDiscount(
                    account.getId(), plan.getDefaultP1YAbonementService()
                        ) : accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService())
                )
        );

        publisher.publishEvent(
                new AccountNotificationRemainingDaysWasSentEvent(
                        account.getId(),
                        apiName
                )
        );
    }

    public void sendMailCreditExpiring(PersonalAccount account) {
        BigDecimal balance = accountHelper.getBalance(account);

        Plan plan = planManager.findOne(account.getPlanId());

        String paymentLink = paymentLinkHelper.generatePaymentLinkForMail(
                account,
                new PaymentLinkRequest(accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService()))
        ).getPaymentLink();

        String dateFinish = "через " + Utils.pluralizeDays(getRemainingDaysCreditPeriod(account));

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("domains", getDomainForEmail(account));
        parameters.put("balance", formatBigDecimalWithCurrency(balance));
        parameters.put("date_finish", dateFinish);
        parameters.put("cost_per_month", formatBigDecimalWithCurrency(
                accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService()))
        );//У нас есть тарифы abonementOnly, возможно, стоит как-то по другому писать в письме для них цену
        parameters.put("cost_abonement", formatBigDecimalWithCurrency(accountServiceHelper.getServiceCostDependingOnDiscount(
                    account.getId(), plan.getDefaultP1YAbonementService()
        )));
        parameters.put("from", "noreply@majordomo.ru");
        parameters.put("payment_link", paymentLink);

        String apiName = "MajordomoHmsCreditMoneySoonEnd";

        sendMail(account, apiName, 1, parameters);

        push(
                new LowBalancePush(account.getId(),
                        account.getName() + " Заканчивается кредит. Аккаунт будет отключен.",
                        "Кредит на услуги хостинга на аккаунте " + account.getName() + " заканчивается " + dateFinish +
                                " Для продолжения работы пополните баланс аккаунта или купите абонемент со скидкой",
                        plan.isAbonementOnly() ? accountServiceHelper.getServiceCostDependingOnDiscount(
                    account.getId(), plan.getDefaultP1YAbonementService()
                        ) : accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService())
                )
        );

        publisher.publishEvent(
                new AccountNotificationRemainingDaysWasSentEvent(
                        account.getId(),
                        apiName
                )
        );
    }

    public void sendMailCreditExpiredWithHostingAbonement(PersonalAccount account) {
        Plan plan = planManager.findOne(account.getPlanId());

        BigDecimal balance = accountHelper.getBalance(account);

        String paymentLink = paymentLinkHelper.generatePaymentLinkForMail(
                account,
                new PaymentLinkRequest(accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService()))
        ).getPaymentLink();

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("domains", getDomainForEmail(account));
        parameters.put("balance", formatBigDecimalWithCurrency(balance));
        parameters.put("date_finish", "");
        parameters.put("from", "noreply@majordomo.ru");
        parameters.put("payment_link", paymentLink);

        String apiName = "MajordomoHmsAddServicesCreditMoneyEndAbonement";

        sendMail(account, apiName, 1, parameters);

        push(
                new LowBalancePush(account.getId(),
                        account.getName() + " Закончился кредит. Дополнительные услуги отключены.",
                        "Закончились кредитные средства на доп.услуги хостинга на аккаунте " + account.getName()
                            + " Для возобновления работы доп.услуг пополните баланс аккаунта.",
                        plan.isAbonementOnly() ? accountServiceHelper.getServiceCostDependingOnDiscount(
                    account.getId(), plan.getDefaultP1YAbonementService()
                        ) : accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService())
                )
        );

        publisher.publishEvent(
                new AccountNotificationRemainingDaysWasSentEvent(
                        account.getId(),
                        apiName
                )
        );
    }

    public void sendMailServicesDisabledWithHostingAbonement(PersonalAccount account) {
        Plan plan = planManager.findOne(account.getPlanId());

        BigDecimal balance = accountHelper.getBalance(account);

        String paymentLink = paymentLinkHelper.generatePaymentLinkForMail(
                account,
                new PaymentLinkRequest(accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService()))
        ).getPaymentLink();

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("domains", getDomainForEmail(account));
        parameters.put("balance", formatBigDecimalWithCurrency(balance));
        parameters.put("date_finish", "");
        parameters.put("from", "noreply@majordomo.ru");
        parameters.put("payment_link", paymentLink);

        String apiName = "MajordomoHmsAddServicesMoneyEndAbonement";

        sendMail(account, apiName, 1, parameters);

        push(
                new LowBalancePush(account.getId(),
                        account.getName() + " Закончились средства. Дополнительные услуги отключены.",
                        "Закончились средства на доп.услуги хостинга на аккаунте " + account.getName()
                                + " Для возобновления работы доп.услуг пополните баланс аккаунта.",
                        plan.isAbonementOnly() ? accountServiceHelper.getServiceCostDependingOnDiscount(
                    account.getId(), plan.getDefaultP1YAbonementService()
                        ) : accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService())
                )
        );

        publisher.publishEvent(
                new AccountNotificationRemainingDaysWasSentEvent(
                        account.getId(),
                        apiName
                )
        );
    }

    public void sendMailServicesExpiringWithHostingAbonement(PersonalAccount account, int remainingDays) {
        Plan plan = planManager.findOne(account.getPlanId());

        BigDecimal balance = accountHelper.getBalance(account);

        String paymentLink = paymentLinkHelper.generatePaymentLinkForMail(
                account,
                new PaymentLinkRequest(accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService()))
        ).getPaymentLink();

        String dateFinish = "через " + Utils.pluralizeDays(remainingDays);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("domains", getDomainForEmail(account));
        parameters.put("balance", formatBigDecimalWithCurrency(balance));
        parameters.put("date_finish", dateFinish);
        parameters.put("from", "noreply@majordomo.ru");
        parameters.put("payment_link", paymentLink);

        String apiName = "MajordomoHmsAddServicesMoneySoonEndAbonement";

        sendMail(account, apiName, 1, parameters);

        push(
                new LowBalancePush(account.getId(),
                        account.getName() + " Заканчиваются средства на дополнительные услуги",
                        "Заканчиваются средства на доп.услуги хостинга на аккаунте " + account.getName() + " " + dateFinish
                                + " Для сохранения работы доп.услуг пополните баланс аккаунта.",
                        plan.isAbonementOnly() ? accountServiceHelper.getServiceCostDependingOnDiscount(
                    account.getId(), plan.getDefaultP1YAbonementService()
                        ) : accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService())
                )
        );

        publisher.publishEvent(
                new AccountNotificationRemainingDaysWasSentEvent(
                        account.getId(),
                        apiName
                )
        );
    }

    public void sendMailServicesExpiring(PersonalAccount account, int remainingDays) {
        BigDecimal balance = accountHelper.getBalance(account);

        Plan plan = planManager.findOne(account.getPlanId());

        String paymentLink = paymentLinkHelper.generatePaymentLinkForMail(
                account,
                new PaymentLinkRequest(accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService()))
        ).getPaymentLink();

        String dateFinish = "через " + Utils.pluralizeDays(remainingDays);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("domains", getDomainForEmail(account));
        parameters.put("balance", formatBigDecimalWithCurrency(balance));
        parameters.put("date_finish", dateFinish);
        parameters.put("cost_per_month", formatBigDecimalWithCurrency(
                accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService()))
        );//У нас есть тарифы abonementOnly, возможно, стоит как-то по другому писать в письме для них цену
        parameters.put("cost_abonement", formatBigDecimalWithCurrency(accountServiceHelper.getServiceCostDependingOnDiscount(
                    account.getId(), plan.getDefaultP1YAbonementService()
        )));
        parameters.put("from", "noreply@majordomo.ru");
        parameters.put("payment_link", paymentLink);

        String apiName = "MajordomoHmsMoneySoonEnd";

        sendMail(account, apiName, 1, parameters);

        push(
                new LowBalancePush(account.getId(),
                        account.getName() + " Заканчиваются средства. Пополните баланс.",
                        "Средства на оплату услуг хостинга на аккаунте " + account.getName() + " заканчиваются " + dateFinish
                                + " Для продолжения работы пополните баланс аккаунта.",
                        plan.isAbonementOnly() ? accountServiceHelper.getServiceCostDependingOnDiscount(
                    account.getId(), plan.getDefaultP1YAbonementService()
                        ) : accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService())
                )
        );

        publisher.publishEvent(
                new AccountNotificationRemainingDaysWasSentEvent(
                        account.getId(),
                        apiName
                )
        );
    }

    public void sendArchivalAbonementExpiring(
            PersonalAccount account, BigDecimal balance, BigDecimal abonementCost, LocalDateTime expired
    ) {
        String dateFinish = expired.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        emailBuilder()
                .account(account)
                .apiName("MajordomoVHExpiringArchivalAbonement")
                .from("noreply@majordomo.ru")
                .priority(1)
                .param("client_id", account.getAccountId())
                .param("acc_id", account.getName())
                .param("domains", getDomainForEmail(account))
                .param("balance", formatBigDecimalWithCurrency(balance))
                .param("cost", formatBigDecimalWithCurrency(abonementCost))
                .param("date_finish", dateFinish)
                .param("from", "noreply@majordomo.ru")
                .send();

        push(
                new Push(account.getId(), account.getName() + " истекает архивный абонемент",
                        "Архивный абонемент " + account.getName() + " заканчивается " + dateFinish
                                + ". Мы автоматически переведем аккаунт на наиболее подходящий тариф из действующих."
                )
        );
    }

    public void sendHostingAbonementNoMoneyToProlong(
            PersonalAccount account, BigDecimal balance, BigDecimal abonementCost, LocalDateTime expired
    ) {
        logger.debug("Account balance is too low to buy new abonement. Balance: {} abonementCost: {}", balance, abonementCost);

        String dateFinish = expired.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        String paymentLink = paymentLinkHelper.generatePaymentLinkForMail(
                account, new PaymentLinkRequest(abonementCost)
        ).getPaymentLink();

        emailBuilder()
                .account(account)
                .apiName("MajordomoVHAbNoMoneyProlong")
                .from("noreply@majordomo.ru")
                .priority(1)
                .param("client_id", account.getAccountId())
                .param("acc_id", account.getName())
                .param("domains", getDomainForEmail(account))
                .param("balance", formatBigDecimalWithCurrency(balance))
                .param("cost", formatBigDecimalWithCurrency(abonementCost))
                .param("date_finish", dateFinish)
                .param("from", "noreply@majordomo.ru")
                .param("payment_link", paymentLink)
                .send();

        push(
                new LowBalancePush(account.getId(), account.getName() + " Абонемент истекает " + dateFinish,
                        format("Срок действия абонемента на аккаунте %s заканчивается %s. " +
                                        "Для продолжения работы продлите абонемент со скидкой за %s",
                                account.getName(), dateFinish, Utils.formatBigDecimalWithCurrency(abonementCost)
                        ), abonementCost
                )
        );
    }

    public void sendSmsVhAbonementExpiring(PersonalAccount account, Long daysToExpired) {
        HashMap<String, String> paramsForSms = new HashMap<>();
        paramsForSms.put("acc_id", account.getName());
        paramsForSms.put("client_id", account.getAccountId());
        paramsForSms.put("remaining_days", Utils.pluralizeDays((daysToExpired).intValue()));
        sendSms(account, "HmsMajordomoAbonementExpiring", 5, paramsForSms);
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

    public boolean isSubscribedToTelegramType(PersonalAccount account, MailManagerMessageType messageType) {
        return account.hasNotification(messageType) && getTelegramChanId(account) != 0;
    }

    /**
     * Получения идентификатора telegram чата или проверка на доступность телеграм
     * @param account
     * @return 0 если телеграм не настроен или id чата
     */
    private long getTelegramChanId(PersonalAccount account) {
        AccountTelegram accountTelegram = accountTelegramRepository.findByPersonalAccountId(account.getId()).orElse(null);
        if (accountTelegram == null || !accountTelegram.getActive()) {
            return 0;
        }
        return NumberUtils.toLong(accountTelegram.getChatId());
    }

    /**
     * Отправить сообщение в telegram если возможно. Если не настроено ничего не делать.
     * @param account
     * @param apiName
     * @param parameters
     */
    public void sendTelegram(PersonalAccount account, String apiName, Map<String, String> parameters) {
        long chatId = getTelegramChanId(account);
        if (chatId == 0) {
            return;
        }
        try {
            telegramRestClient.callSendTelegramMessage(new TelegramMessageData(chatId, null, apiName, parameters));
        } catch (BaseException | FeignException ex) {
            logger.error("We got exception when send telegram message", ex);
        } catch (Exception ex) {
            logger.error("We got unknown exception when send telegram message", ex);
        }
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
            remainingDays = Utils.differenceInDays(maxCreditActivationDate, creditActivationDate.toLocalDate());
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

    public void checkSmsAllowness(PersonalAccount account) {
        boolean smsNotificationsEmpty = !hasActiveSmsNotifications(account);

        boolean phoneInvalid = account.getSmsPhoneNumber() == null || !phoneValid(account.getSmsPhoneNumber());
        if (smsNotificationsEmpty || phoneInvalid) {
            String message;
            if (smsNotificationsEmpty && phoneInvalid) {
                message = "Выберите хотя бы один вид уведомлений и укажите корректный номер телефона.";
            } else if (smsNotificationsEmpty) {
                message = "Выберите хотя бы один вид уведомлений.";
            } else {
                message = "Укажите корректный номер телефона.";
            }
            throw new ParameterValidationException(message);
        }
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

    public EmailBuilder emailBuilder() {
        return new EmailBuilder(accountHelper::getEmails, publisher);
    }

    public static class EmailBuilder {
        private final Function<PersonalAccount, List<String>> emailProvider;
        private final ApplicationEventPublisher publisher;

        private SimpleServiceMessage message = new SimpleServiceMessage();
        private Map<String, String> parameters = new HashMap<>();
        private PersonalAccount account;

        EmailBuilder(
                Function<PersonalAccount, List<String>> emailProvider,
                ApplicationEventPublisher publisher
        ) {
            this.emailProvider = emailProvider;
            this.publisher = publisher;
        }

        public EmailBuilder from(String from) {
            parameters.put("from", from);
            return this;
        }

        public EmailBuilder account(PersonalAccount account) {
            this.account = account;
            message.setAccountId(account.getId());
            return this;
        }

        public EmailBuilder emails(String... emails) {
            return this.emails(Arrays.asList(emails));
        }

        public EmailBuilder emails(List<String> emails) {
            message.addParam(EMAIL_KEY, String.join(",", emails));
            return this;
        }

        public EmailBuilder apiName(String apiName) {
            message.addParam(API_NAME_KEY, apiName);
            return this;
        }

        public EmailBuilder priority(int priority) {
            message.addParam(PRIORITY_KEY, priority);
            return this;
        }

        public EmailBuilder param(String key, String value) {
            parameters.put(key, value);
            return this;
        }

        public EmailBuilder params(Map<String, String> params) {
            parameters.putAll(params);
            return this;
        }

        public EmailBuilder attachment(Map<String, String> attachment) {
            message.addParam("attachment", attachment);
            return this;
        }

        public void send() {
            if (message.getParam(EMAIL_KEY) == null) {
                emails(emailProvider.apply(account));
            }
            message.addParam(PARAMETRS_KEY, parameters);

            publisher.publishEvent(new SendMailEvent(message));
        }
    }

    public EmailAttachmentBuilder attachment() {
        return new EmailAttachmentBuilder();
    }

    public static class EmailAttachmentBuilder {
        private Map<String, String> attachment = new HashMap<>();

        private EmailAttachmentBuilder() {}

        public EmailAttachmentBuilder body(String body) {
            attachment.put("body", body);
            return this;
        }

        public EmailAttachmentBuilder mimeType(String mimeType) {
            attachment.put("mime_type", mimeType);
            return this;
        }

        public EmailAttachmentBuilder fileName(String fileName) {
            attachment.put("filename", fileName);
            return this;
        }

        public Map<String, String> build() {
            return attachment;
        }
    }

    public void push(Push push) {
        pushService.send(push);
    }

    /**
     * @param subject
     * @param bodyHtml тело сообщение отформатированное в html (только часть, заголовки, footer и header добавит mail-manager)
     */
    @ParametersAreNonnullByDefault
    public void sendDevEmail(String subject, String bodyHtml) {
        SimpleServiceMessage message = new SimpleServiceMessage();

        message.setParams(new HashMap<>());
        message.addParam("email", devEmail);
        message.addParam("api_name", serviceMessageApiName);
        message.addParam("priority", 10);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("body", bodyHtml);
        parameters.put("subject", String.format("[%s][%s] %s", applicationName, activeProfiles, subject));

        message.addParam("parametrs", parameters);
        publisher.publishEvent(new SendMailEvent(message));
    }
}
