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
import ru.majordomo.hms.personmgr.event.account.SendEmailWithExpiredAbonementEvent;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

        //границы поиска за каждый из дней - полночь
        LocalDateTime midnightToday = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

        int[] daysAgo = {1, 3, 5, 10, 15, 20};


        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        logger.debug("Trying to find all expired abonements for the last month on the date: "
                + midnightToday.format(formatterDate)
        );

        List<AccountStat> accountStats = accountStatRepository.findByPersonalAccountIdAndTypeAndCreatedAfterOrderByCreatedDesc(
                account.getId(),
                AccountStatType.VIRTUAL_HOSTING_ABONEMENT_DELETE,
                LocalDateTime.now().minusMonths(1)
        );

        if (accountStats.isEmpty()) {
            logger.debug("Not found expired abonements for accountId: " + account.getId() +
                    "for the last month on date " + midnightToday.format(formatterDate));
            return;}

        //Не отправляем письма при активном абонементе
        if (accountAbonementManager.findByPersonalAccountIdAndExpiredAfter(account.getAccountId(), LocalDateTime.now()).isEmpty()) {

            LocalDateTime abonementExpiredDateTime = LocalDateTime.parse(accountStats.get(0).getData().get("expireEnd"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            for (int dayAgo : daysAgo) {
                //Срок действия абонемента закончился. - через 1, 3, 5, 10, 15, 20 дней после окончания
                if (abonementExpiredDateTime.isBefore(midnightToday.minusDays(dayAgo - 1)) &&
                        abonementExpiredDateTime.isAfter(midnightToday.minusDays(dayAgo)))
                {
                    BigDecimal balance = accountHelper.getBalance(account).setScale(2, BigDecimal.ROUND_DOWN);
                    Plan plan = planRepository.findOne(account.getPlanId());
                    //не отправляется, если хватает на 1 месяц хостинга по выбранному тарифу после окончания абонемента
                    //берем текущий баланс и сравниваем его с
                    //стоимостью тарифа за месяц, деленной на 30 дней и умноженная на количество оставшихся дней с окончания абонемента
                    boolean balanceEnoughForOneMonth = balance.compareTo(
                            (plan.getService().getCost().
                                            divide(new BigDecimal(30), BigDecimal.ROUND_FLOOR).
                                            multiply(new BigDecimal(30 - dayAgo)))) != 1;

                    if (balanceEnoughForOneMonth) {
                        publisher.publishEvent(new SendEmailWithExpiredAbonementEvent(account));
                        //Отправляем только одно письмо
                        break;
                    }
                }
            }
        }
    }
    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onSendEmailWithExpiredAbonementEvent(SendEmailWithExpiredAbonementEvent event) {
        PersonalAccount account = event.getSource();

        logger.debug("We got AccountNotifyExpiredAbonementEvent");

        BigDecimal balance = accountHelper.getBalance(account).setScale(2, BigDecimal.ROUND_DOWN);
        Plan plan = planRepository.findOne(account.getPlanId());

        String emails = accountHelper.getEmail(account);
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setParams(new HashMap<>());
        message.addParam("email", emails);
        message.addParam("api_name", "MajordomoHmsAbonementEnd");
        message.addParam("priority", 5);

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
