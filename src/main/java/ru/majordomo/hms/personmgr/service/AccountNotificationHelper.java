package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.rc.user.resources.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountNotificationHelper {

    private final static Logger logger = LoggerFactory.getLogger(AccountNotificationHelper.class);

    private final ApplicationEventPublisher publisher;
    private final PlanRepository planRepository;
    private final AccountHelper accountHelper;
    private final AccountOwnerManager accountOwnerManager;

    @Autowired
    public AccountNotificationHelper(
            ApplicationEventPublisher publisher,
            PlanRepository planRepository,
            AccountOwnerManager accountOwnerManager,
            AccountHelper accountHelper
    ) {
        this.publisher = publisher;
        this.planRepository = planRepository;
        this.accountHelper = accountHelper;
        this.accountOwnerManager = accountOwnerManager;
    }

    public String getCostAbonementForEmail(Plan plan) {return accountHelper.getCostAbonement(plan).setScale(2, BigDecimal.ROUND_DOWN).toString();}

    public String getDomainForEmail(PersonalAccount account) {

        List<Domain> domains = accountHelper.getDomains(account);
        if (!(domains.isEmpty())) {
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
}
