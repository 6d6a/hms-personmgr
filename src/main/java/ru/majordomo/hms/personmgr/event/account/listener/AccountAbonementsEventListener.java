package ru.majordomo.hms.personmgr.event.account.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountProcessAbonementsAutoRenewEvent;
import ru.majordomo.hms.personmgr.event.account.AccountProcessExpiringAbonementsEvent;
import ru.majordomo.hms.personmgr.event.account.AccountProcessNotifyExpiredAbonementsEvent;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.manager.AccountAbonementManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.AbonementService;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.rc.user.resources.Domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Constants.PASSWORD_KEY;

@Component
public class AccountAbonementsEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountAbonementsEventListener.class);

    private final AbonementService abonementService;
    private final AccountHelper accountHelper;
    private final ApplicationEventPublisher publisher;
    private final AccountAbonementManager accountAbonementManager;

    @Autowired
    public AccountAbonementsEventListener(
            AccountHelper accountHelper,
            AbonementService abonementService,
            ApplicationEventPublisher publisher,
            AccountAbonementManager accountAbonementManager
    ) {
        this.abonementService = abonementService;
        this.accountHelper = accountHelper;
        this.publisher = publisher;
        this.accountAbonementManager = accountAbonementManager;

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
        //получаем абонементы которые закончились 1, 3, 5, 10, 15, 20 дней назад и отправляем уведомления
        for (int day: days) {

            logger.debug("Trying to find all expired abonements on the date expireEnd: "
                    + expireEnd.minusDays(day).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            );
            List<AccountAbonement> accountAbonements = accountAbonementManager.findByExpiredBetween(expireEnd.minusDays(day), expireEnd.minusDays(day - 1));

            if (accountAbonements.isEmpty()) {
                logger.debug("Not found expired abonements for accountId: " + account.getId());
            }

            accountAbonements.forEach(accountAbonement -> {
                //условие рассылки писем
                //Срок действия абонемента закончился. - через 1, 3, 5, 10, 15, 20 дней после окончания
                //не отправляется, если абонемент продлился автоматически
                //не отправляется, если хватает на 1 месяц хостинга по выбранному тарифу
                BigDecimal balance = accountHelper.getBalance(account).setScale(2, BigDecimal.ROUND_DOWN);
                if (!(balance < planCost)) {
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

                    //BigDecimal balance = accountHelper.getBalance(account).setScale(2, BigDecimal.ROUND_DOWN);
                    parameters.put("balance", balance.toString() + " рублей");

                    BigDecimal cost = accountAbonement.getAbonement().getService().getCost().setScale(2, BigDecimal.ROUND_DOWN);
                    parameters.put("cost_per_year", cost.toString() + " рублей");

                    message.addParam("parametrs", parameters);

                    publisher.publishEvent(new SendMailEvent(message));

                }
            });
        }
    }
}
