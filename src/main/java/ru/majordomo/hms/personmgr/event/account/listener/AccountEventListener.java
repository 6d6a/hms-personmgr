package ru.majordomo.hms.personmgr.event.account.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.*;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.rc.user.resources.Domain;

import static ru.majordomo.hms.personmgr.common.Constants.PASSWORD_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_NAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.TECHNICAL_SUPPORT_EMAIL;

@Component
public class AccountEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountEventListener.class);

    private final AccountHelper accountHelper;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public AccountEventListener(
            AccountHelper accountHelper,
            ApplicationEventPublisher publisher) {
        this.accountHelper = accountHelper;
        this.publisher = publisher;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountCreated(AccountCreatedEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        logger.debug("We got AccountCreatedEvent");

        String emails = accountHelper.getEmail(account);

        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setParams(new HashMap<>());
        message.addParam("email", emails);
        message.addParam("api_name", "MajordomoVHClientCreatedConfirmation");
        message.addParam("priority", 10);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put(PASSWORD_KEY, (String) params.get(PASSWORD_KEY));
        parameters.put("ftp_ip", "FTP_IP");
        parameters.put("ftp_login", "FTP_LOGIN");
        parameters.put("ftp_password", "FTP_PASSWORD");

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountQuotaAdded(AccountQuotaAddedEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        logger.debug("We got AccountQuotaAddedEvent");

        String email = accountHelper.getEmail(account);
        String planName = (String) params.get(SERVICE_NAME_KEY);

        SimpleServiceMessage message = new SimpleServiceMessage();

        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam("email", email);
        message.addParam("api_name", "MajordomoVHQuotaAdd");
        message.addParam("priority", 10);


        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", message.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("tariff", planName);

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountQuotaDiscard(AccountQuotaDiscardEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        logger.debug("We got AccountQuotaDiscardEvent");

        String email = accountHelper.getEmail(account);
        String planName = (String) params.get(SERVICE_NAME_KEY);

        SimpleServiceMessage message = new SimpleServiceMessage();

        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam("email", email);
        message.addParam("api_name", "MajordomoVHQuotaDiscard");
        message.addParam("priority", 10);


        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", message.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("tariff", planName);
        List<Domain> domains = accountHelper.getDomains(account);
        List<String> domainNames = new ArrayList<>();
        for (Domain domain: domains) {
            domainNames.add(domain.getName());
        }
        parameters.put("domains", String.join("<br>", domainNames));

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountNotifySupportOnChangePlan(AccountNotifySupportOnChangePlanEvent event) {
        PersonalAccount account = event.getSource();

        logger.debug("We got AccountNotifySupportOnChangePlanEvent");

        SimpleServiceMessage message = new SimpleServiceMessage();

        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam("email", TECHNICAL_SUPPORT_EMAIL);
        message.addParam("api_name", "MajordomoCorporatePlanChange");
        message.addParam("priority", 1);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", message.getAccountId());;

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountNotifyRemainingDays(AccountNotifyRemainingDaysEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        logger.debug("We got AccountNotifyRemainingDaysEvent");

        String email = accountHelper.getEmail(account);
        Integer remainingDays = (Integer) params.get("remainingDays");

        SimpleServiceMessage message = new SimpleServiceMessage();

        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam("email", email);
        message.addParam("api_name", "MajordomoVHMoneyLowLevel");
        message.addParam("priority", 10);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("acc_id", account.getName());
        parameters.put("days", pluralizef("остался %d день", "осталось %d дня", "осталось %d дней", remainingDays));
        List<Domain> domains = accountHelper.getDomains(account);
        List<String> domainNames = new ArrayList<>();
        for (Domain domain : domains) {
            domainNames.add(domain.getName());
        }
        parameters.put("domains", "<br>" + String.join("<br>", domainNames));

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }

    private String pluralizef(String form1, String form2, String form3, Integer number)
    {
        List<String> messages = new LinkedList<>();
        messages.add(form1);
        messages.add(form2);
        messages.add(form3);

        Integer index = number % 10 == 1 && number % 100 != 11 ? 0 : (number % 10 >= 2 && number % 10 <= 4 && (number % 100 < 10 || number % 100 >= 20) ? 1 : 2);

        return String.format(messages.get(index), number);
    }
}
