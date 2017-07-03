package ru.majordomo.hms.personmgr.event.account.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountSendEmailWithExpiredAbonementEvent;
import ru.majordomo.hms.personmgr.event.account.AccountProcessAbonementsAutoRenewEvent;
import ru.majordomo.hms.personmgr.event.account.AccountProcessExpiringAbonementsEvent;
import ru.majordomo.hms.personmgr.event.account.AccountProcessNotifyExpiredAbonementsEvent;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.manager.AccountAbonementManager;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.account.AccountStat;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.AccountStatRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.AbonementService;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.rc.user.resources.Domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AccountAbonementsEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountAbonementsEventListener.class);

    private final AbonementService abonementService;
    private final AccountHelper accountHelper;
    private final AccountStatRepository accountStatRepository;
    private final ApplicationEventPublisher publisher;
    private final AccountAbonementManager accountAbonementManager;
    private final PlanRepository planRepository;

    @Autowired
    public AccountAbonementsEventListener(
            AccountHelper accountHelper,
            AccountStatRepository accountStatRepository,
            AbonementService abonementService,
            ApplicationEventPublisher publisher,
            AccountAbonementManager accountAbonementManager,
            PlanRepository planRepository
    ) {
        this.abonementService = abonementService;
        this.accountHelper = accountHelper;
        this.accountStatRepository = accountStatRepository;
        this.publisher = publisher;
        this.accountAbonementManager = accountAbonementManager;
        this.planRepository = planRepository;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountProcessExpiringAbonementsEvent(AccountProcessExpiringAbonementsEvent event) {
        PersonalAccount account = event.getSource();

        logger.debug("We got AccountProcessExpiringAbonementsEvent");

        try {
            abonementService.processExpiringAbonementsByAccount(account);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.event.account.listener.AccountAbonementsEventListener.onAccountProcessExpiringAbonementsEvent " + e.getMessage());
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountProcessAbonementsAutoRenewEvent(AccountProcessAbonementsAutoRenewEvent event) {
        PersonalAccount account = event.getSource();

        logger.debug("We got AccountProcessAbonementsAutoRenewEvent");

        try {
            abonementService.processAbonementsAutoRenewByAccount(account);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.event.account.listener.AccountAbonementsEventListener.onAccountProcessExpiringAbonementsEvent " + e.getMessage());
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountProccessNotifyExpiredAbonementEvent(AccountProcessNotifyExpiredAbonementsEvent event){
        PersonalAccount account = event.getSource();

        logger.debug("We got AccountProcessNotifyExpiredAbonementsEvent");

        //Не отправляем письма при активном абонементе
        if (!accountAbonementManager.findByPersonalAccountIdAndExpiredAfter(account.getAccountId(), LocalDateTime.now()).isEmpty()) {return;}

        LocalDate now = LocalDate.now();

        int[] daysAgo = {1, 3, 5, 10, 15, 20};


        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        logger.debug("Trying to find all expired abonements for the last month on the date: "
                + now.format(formatterDate)
        );

        List<AccountStat> accountStats = accountStatRepository.findByPersonalAccountIdAndTypeAndCreatedAfterOrderByCreatedDesc(
                account.getId(),
                AccountStatType.VIRTUAL_HOSTING_ABONEMENT_DELETE,
                LocalDateTime.now().minusDays(21)
        );

        if (accountStats.isEmpty()) {
            logger.debug("Not found expired abonements for accountId: " + account.getId() +
                    "for the last month on date " + now.format(formatterDate));
            return;
        }

        boolean needToSendMail = false;
        LocalDate abonementExpiredDate = accountStats.get(0).getCreated().toLocalDate();
        for (int dayAgo : daysAgo) {
            //Срок действия абонемента закончился - через 1, 3, 5, 10, 15, 20 дней после окончания
            if (abonementExpiredDate.isEqual(now.minusDays(dayAgo))) {

                Plan plan = planRepository.findOne(account.getPlanId());
                if (!plan.isAbonementOnly()) {

                    //не отправляется, если хватает на 1 месяц хостинга по выбранному тарифу после окончания абонемента
                    //берем текущий баланс и сравниваем его с
                    //стоимостью тарифа за месяц, деленной на 30 дней и умноженная на количество оставшихся дней с окончания абонемента
                    Calendar cal = Calendar.getInstance();
                    int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                    BigDecimal balance = accountHelper.getBalance(account);
                    needToSendMail = balance.compareTo(
                            (plan.getService().getCost()
                                    .divide(new BigDecimal(daysInMonth), BigDecimal.ROUND_FLOOR)
                                    .multiply(new BigDecimal(daysInMonth - dayAgo)))) < 0;
                }

                //для только абонементных тарифов и неактивных аккаунтов отправляем письмо
                if (plan.isAbonementOnly() && !account.isActive()) {
                    needToSendMail = true;
                }
                if (needToSendMail) {
                    publisher.publishEvent(new AccountSendEmailWithExpiredAbonementEvent(account));
                    //Отправляем только одно письмо
                    break;
                }
            }
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountSendEmailWithExpiredAbonementEvent(AccountSendEmailWithExpiredAbonementEvent event) {
        PersonalAccount account = event.getSource();

        logger.debug("We got AccountSendEmailWithExpiredAbonementEvent");

        BigDecimal balance = accountHelper.getBalance(account).setScale(2, BigDecimal.ROUND_DOWN);
        Plan plan = planRepository.findOne(account.getPlanId());

        String emails = accountHelper.getEmail(account);
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setParams(new HashMap<>());
        message.addParam("email", emails);
        message.addParam("api_name", "MajordomoHmsAbonementEnd");
        message.addParam("priority", 10);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("acc_id", account.getName());

        String domainsForEmail = "";
        List<Domain> domains = accountHelper.getDomains(account);
        if (!(domains.isEmpty())) {
            domainsForEmail = domains.stream().map(Domain::getName).collect(Collectors.joining("<br>"));
        }
        parameters.put("domains", domainsForEmail);

        parameters.put("balance", balance.toString() + " рублей");

        //так как активного абонемента уже нет или он мог быть бесплатным тестовым, получаем стоимость абонемента через активный план
        List<Abonement> abonements = plan.getAbonements()
                .stream().filter(
                        abonement -> abonement.getPeriod().equals("P1Y")
                ).collect(Collectors.toList());
        BigDecimal costAbonement = abonements.get(0).getService().getCost().setScale(2, BigDecimal.ROUND_DOWN);
        parameters.put("cost_per_year", costAbonement.toString());

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }
}
