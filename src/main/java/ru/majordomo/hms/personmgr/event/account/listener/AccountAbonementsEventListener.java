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

        //текущая дата в полночь
        LocalDateTime expireEnd = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

        int[] days = {1, 3, 5, 10, 15, 20};
        //для каждого абонемента, который закончился 1, 3, 5, 10, 15, 20 дней назад, отправляем уведомления
        //for (int day: days) {

            /*logger.debug("Trying to find all expired abonements on the date expireEnd: "
                    + expireEnd.minusDays(day).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            );*/
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        List<AccountStat> accountStats = accountStatRepository.findByPersonalAccountIdAndTypeAndCreatedAfter(
                account.getId(),
                AccountStatType.VIRTUAL_HOSTING_ABONEMENT_DELETE,
                LocalDateTime.now().minusMonths(1)
        );/*.stream().filter((accStat)-> (
                    LocalDateTime.parse(accStat.getData().get("expireEnd"), formatter).isBefore(expireEnd.minusDays(day - 1)) &&
                    LocalDateTime.parse(accStat.getData().get("expireEnd"), formatter).isAfter(expireEnd.minusDays(day))
            )).collect(Collectors.toList());*/

        for (AccountStat accountStat: accountStats) {
            for (int day: days) {
                if (LocalDateTime.parse(accountStat.getData().get("expireEnd"), formatter).isBefore(expireEnd.minusDays(day - 1)) &&
                        LocalDateTime.parse(accountStat.getData().get("expireEnd"), formatter).isAfter(expireEnd.minusDays(day))) {

                    /*if (accountStats.isEmpty()) {
                        logger.debug("Not found expired abonements for accountId: " + account.getId());
                    }*/

                    BigDecimal balance = accountHelper.getBalance(account).setScale(2, BigDecimal.ROUND_DOWN);
                    Plan plan = planRepository.findOne(account.getPlanId());

                    //условие рассылки писем
                    //Срок действия абонемента закончился. - через 1, 3, 5, 10, 15, 20 дней после окончания
                    //не отправляется, если абонемент продлился автоматически
                    //не отправляется, если хватает на 1 месяц хостинга по выбранному тарифу

                    //берем текущий баланс и сравниваем его с
                    //стоимостью тарифа за месяц, деленной на 31 день и умноженная на количество оставшихся дней с окончания абонемента
                    if ((balance.compareTo((plan.getService().getCost().divide(new BigDecimal(31)).multiply(new BigDecimal(31 - day)))) != 1)
                            && accountAbonementManager.findByPersonalAccountIdAndExpiredAfter(account.getAccountId(), expireEnd).isEmpty()
                            ) //&& !(accountStats.isEmpty()))
                    {
                        String emails = accountHelper.getEmail(account);
                        SimpleServiceMessage message = new SimpleServiceMessage();
                        message.setParams(new HashMap<>());
                        message.addParam("email", emails);
                        message.addParam("api_name", "MajordomoHmsAbonementEnd");
                        //уточнить приоритет
                        message.addParam("priority", 10);

                        HashMap<String, String> parameters = new HashMap<>();
                        parameters.put("acc_id", account.getName());

                        List<Domain> domains = accountHelper.getDomains(account);
                        List<String> domainNames = new ArrayList<>();
                        for (Domain domain : domains) {
                            domainNames.add(domain.getName());
                        }

                        parameters.put("domains", "<br>" + String.join("<br>", domainNames));
                        parameters.put("balance", balance.toString() + " рублей");

                        //так как активного абонемента уже нет, получаем стоимость абонемента через активный план
                        List<Abonement> abonements = plan.getAbonements().stream().filter(abonement -> abonement.getPeriod().equals("P1Y")).collect(Collectors.toList());
                        BigDecimal costAbonement = abonements.get(0).getService().getCost().setScale(2, BigDecimal.ROUND_DOWN);
                        parameters.put("cost_per_year", costAbonement.toString());

                        message.addParam("parametrs", parameters);

                        publisher.publishEvent(new SendMailEvent(message));

                    }
                    //отправляем письмо только один раз при нахождении недавно закончившегося абонемента
                    break;
                }
            }
        }
    }
}
