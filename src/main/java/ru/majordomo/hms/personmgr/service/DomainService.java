package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.event.mailManager.SendSmsEvent;
import ru.majordomo.hms.personmgr.exception.LowBalanceException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.rc.user.resources.Domain;

import static ru.majordomo.hms.personmgr.common.Constants.AUTO_RENEW_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Utils.formatBigDecimalWithCurrency;

@Service
public class DomainService {
    private final static Logger logger = LoggerFactory.getLogger(DomainService.class);

    private static TemporalAdjuster THIRTY_DAYS_AFTER = TemporalAdjusters.ofDateAdjuster(date -> date.plusDays(30));
    private static TemporalAdjuster SIXTY_DAYS_AFTER = TemporalAdjusters.ofDateAdjuster(date -> date.plusDays(60));
    private static TemporalAdjuster TWENTY_NINE_DAYS_BEFORE = TemporalAdjusters.ofDateAdjuster(date -> date.minusDays(29));
    private static TemporalAdjuster THREE_DAYS_BEFORE = TemporalAdjusters.ofDateAdjuster(date -> date.minusDays(3));
    private static TemporalAdjuster FOURTEEN_DAYS_AFTER = TemporalAdjusters.ofDateAdjuster(date -> date.plusDays(14));

    private final RcUserFeignClient rcUserFeignClient;
    private final AccountHelper accountHelper;
    private final DomainTldService domainTldService;
    private final BusinessActionBuilder businessActionBuilder;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public DomainService(
            RcUserFeignClient rcUserFeignClient,
            AccountHelper accountHelper,
            DomainTldService domainTldService,
            BusinessActionBuilder businessActionBuilder,
            ApplicationEventPublisher publisher
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.accountHelper = accountHelper;
        this.domainTldService = domainTldService;
        this.businessActionBuilder = businessActionBuilder;
        this.publisher = publisher;
    }

    public void processExpiringDomainsByAccount(PersonalAccount account) {
        //В итоге нам нужно получить домены которые заканчиваются через 30 дней или между 14 "до" и 3 днями "после" окончания

        //Ищем paidTill равный +30 дням от текущей даты
        LocalDate paidTillStart = LocalDate.now().with(THIRTY_DAYS_AFTER);
        LocalDate paidTillEnd = LocalDate.now().with(THIRTY_DAYS_AFTER);

        logger.debug("Trying to find all expiring domains from paidTillStart: "
                + paidTillStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + " to paidTillEnd: " + paidTillEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        List<Domain> domains = rcUserFeignClient.getExpiringDomainsByAccount(
                account.getId(),
                paidTillStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                paidTillEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        //Ищем paidTill начиная с 3 дней до текущей даты
        paidTillStart = LocalDate.now().with(THREE_DAYS_BEFORE);
        //И закакнчивая 14 днями после текущей даты
        paidTillEnd = LocalDate.now().with(FOURTEEN_DAYS_AFTER);

        logger.debug("Trying to find expiring domains from paidTillStart: "
                + paidTillStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + " to paidTillEnd: " + paidTillEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        domains.addAll(rcUserFeignClient.getExpiringDomainsByAccount(
                account.getId(),
                paidTillStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                paidTillEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        ));

        if (domains.isEmpty()) {
            logger.debug("Not found expiring domains for accountId: " + account.getId());
        }

        domains.forEach(domain -> logger.debug("We found expiring domain: " + domain));

        if (!domains.isEmpty() && account.hasNotification(MailManagerMessageType.EMAIL_DOMAIN_DELEGATION_ENDING)) {
            BigDecimal balance = accountHelper.getBalance(account);

            String domainsForMail = "";
            for (Domain domain : domains) {
                String autoRenew = domain.getAutoRenew() ? "включено" : "выключено";
                domainsForMail += String.format(
                        "%-20s - %s - %-10s<br>",
                        domain.getName(),
                        domain.getRegSpec().getPaidTill().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        autoRenew
                );
            }

            //Отправим письмо
            String email = accountHelper.getEmail(account);

            SimpleServiceMessage message = new SimpleServiceMessage();

            message.setAccountId(account.getId());
            message.setParams(new HashMap<>());
            message.addParam("email", email);
            //TODO Убрать точку в шаблоне после баланса, так как получается "руб.."
            message.addParam("api_name", "MajordomoVHDomainsExpires");
            message.addParam("priority", 10);

            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("client_id", message.getAccountId());
            parameters.put("acc_id", account.getName());
            parameters.put("domains", domainsForMail);
            parameters.put("balance", formatBigDecimalWithCurrency(balance));
            parameters.put("from", "noreply@majordomo.ru");

            message.addParam("parametrs", parameters);

            publisher.publishEvent(new SendMailEvent(message));
        }
    }

    public void processDomainsAutoRenewByAccount(PersonalAccount account) {
        //Ищем paidTill начиная с текущей даты
        LocalDate paidTillStart = LocalDate.now();
        //И закакнчивая 30 днями после текущей даты
        LocalDate paidTillEnd = LocalDate.now().with(THIRTY_DAYS_AFTER);

        logger.debug("Trying to find domains for AutoRenew from paidTillStart: "
                + paidTillStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + " to paidTillEnd: " + paidTillEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        List<Domain> domains = rcUserFeignClient.getExpiringDomainsByAccount(
                account.getId(),
                paidTillStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                paidTillEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        if (domains.isEmpty()) {
            logger.debug("Not found expiring domains for AutoRenew for accountId: " + account.getId());
        }

        domains.forEach(domain -> logger.debug("We found domain for AutoRenew: " + domain));

        for (Domain domain : domains) {
            BigDecimal balance = accountHelper.getBalance(account);

            DomainTld domainTld = domainTldService.findDomainTldByDomainNameAndRegistrator(domain.getName(), domain.getRegSpec().getRegistrar());

            //TODO Стоимость может быть со скидкой
            //например для ac_99521 ru, рф по 150
            //TODO Домен может быть премиальным и стоимость продления нужно проверить на reg-rpc
            //domains_get_premium_pricing($domain, 'renew');
            try {
                accountHelper.checkBalance(account, domainTld.getRenewService());
            } catch (LowBalanceException e) {
                //Если денег не хватает
                //Запишем попытку в историю клиента
                Map<String, String> params = new HashMap<>();
                params.put(HISTORY_MESSAGE_KEY, "Автоматическое продление " + domain.getName() + " невозможно, на счету " + balance + " руб.");
                params.put(OPERATOR_KEY, "service");

                publisher.publishEvent(new AccountHistoryEvent(account.getId(), params));

                //Отправим письмо
                String email = accountHelper.getEmail(account);

                SimpleServiceMessage message = new SimpleServiceMessage();

                message.setAccountId(account.getId());
                message.setParams(new HashMap<>());
                message.addParam("email", email);
                message.addParam("api_name", "MajordomoVHNomoneyProlong");
                message.addParam("priority", 10);

                HashMap<String, String> parameters = new HashMap<>();
                parameters.put("client_id", message.getAccountId());
                parameters.put("acc_id", account.getName());
                parameters.put("domen", domain.getName());
                parameters.put("from", "noreply@majordomo.ru");

                message.addParam("parametrs", parameters);

                publisher.publishEvent(new SendMailEvent(message));

                String smsPhone = account.getSmsPhoneNumber();

                //Если подключено СМС-уведомление, то также отправим его
                if (account.hasNotification(MailManagerMessageType.SMS_NO_MONEY_TO_AUTORENEW_DOMAIN)
                        && smsPhone != null
                        && !smsPhone.equals("")) {
                    message = new SimpleServiceMessage();

                    message.setAccountId(account.getId());
                    message.setParams(new HashMap<>());
                    message.addParam("phone", smsPhone);
                    message.addParam("api_name", "MajordomoNoMoneyToAutoRenewDomain");
                    message.addParam("priority", 10);

                    parameters = new HashMap<>();
                    parameters.put("client_id", message.getAccountId());
                    parameters.put("domain", domain.getName());

                    message.addParam("parametrs", parameters);

                    publisher.publishEvent(new SendSmsEvent(message));
                }
            }

            SimpleServiceMessage blockResult = accountHelper.block(account, domainTld.getRenewService());

            String documentNumber = (String) blockResult.getParam("documentNumber");

            SimpleServiceMessage domainRenewMessage = new SimpleServiceMessage();

            domainRenewMessage.setAccountId(account.getId());
            domainRenewMessage.addParam(RESOURCE_ID_KEY, domain.getId());
            domainRenewMessage.addParam("renew", true);
            domainRenewMessage.addParam(AUTO_RENEW_KEY, true);
            domainRenewMessage.addParam("documentNumber", documentNumber);

            businessActionBuilder.build(BusinessActionType.DOMAIN_UPDATE_RC, domainRenewMessage);
        }
    }
}
