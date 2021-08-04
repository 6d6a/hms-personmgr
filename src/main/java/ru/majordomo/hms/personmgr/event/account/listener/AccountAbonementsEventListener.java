package ru.majordomo.hms.personmgr.event.account.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.event.account.*;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.account.AccountStat;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.AccountStatRepository;
import ru.majordomo.hms.personmgr.service.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import static ru.majordomo.hms.personmgr.common.Utils.formatBigDecimalWithCurrency;

@Component
public class AccountAbonementsEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountAbonementsEventListener.class);

    private final AbonementService abonementService;
    private final ServiceAbonementService serviceAbonementService;
    private final AccountHelper accountHelper;
    private final AccountServiceHelper accountServiceHelper;
    private final AccountStatRepository accountStatRepository;
    private final ApplicationEventPublisher publisher;
    private final PlanManager planManager;
    private final AccountNotificationHelper accountNotificationHelper;
    private final PersonalAccountManager personalAccountManager;

    @Autowired
    public AccountAbonementsEventListener(
            AccountHelper accountHelper,
            AccountStatRepository accountStatRepository,
            AbonementService abonementService,
            ApplicationEventPublisher publisher,
            PlanManager planManager,
            AccountNotificationHelper accountNotificationHelper,
            PersonalAccountManager personalAccountManager,
            ServiceAbonementService serviceAbonementService,
            AccountServiceHelper accountServiceHelper
    ) {
        this.abonementService = abonementService;
        this.accountHelper = accountHelper;
        this.accountStatRepository = accountStatRepository;
        this.publisher = publisher;
        this.planManager = planManager;
        this.accountNotificationHelper = accountNotificationHelper;
        this.personalAccountManager = personalAccountManager;
        this.serviceAbonementService = serviceAbonementService;
        this.accountServiceHelper = accountServiceHelper;
    }

    @EventListener
    @Async("cronThreadPoolTaskExecutor")
    public void onAccountProcessExpiringAbonementsEvent(AccountProcessExpiringAbonementsEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        logger.debug("We got AccountProcessExpiringAbonementsEvent");

        try {
            abonementService.processExpiringAbonementsByAccount(account);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.event.account.listener.AccountAbonementsEventListener.onAccountProcessExpiringAbonementsEvent " + e.getMessage());
        }
    }

    @EventListener
    @Async("cronThreadPoolTaskExecutor")
    public void onAccountProcessExpiringServiceAbonementsEvent(AccountProcessExpiringServiceAbonementsEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        logger.debug("We got AccountProcessExpiringServiceAbonementsEvent");

        try {
            serviceAbonementService.processExpiringAbonementsByAccount(account);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.event.account.listener.AccountAbonementsEventListener.onAccountProcessExpiringServiceAbonementsEvent " + e.getMessage());
        }
    }

    @EventListener
    @Async("cronThreadPoolTaskExecutor")
    public void onAccountProcessAbonementsAutoRenewEvent(AccountProcessAbonementsAutoRenewEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        logger.debug("We got AccountProcessAbonementsAutoRenewEvent");

        try {
            abonementService.processAbonementsAutoRenewByAccount(account);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.event.account.listener.AccountAbonementsEventListener.onAccountProcessExpiringAbonementsEvent " + e.getMessage());
        }
    }

    @EventListener
    @Async("cronThreadPoolTaskExecutor")
    public void onAccountProcessServiceAbonementsAutoRenewEvent(AccountProcessServiceAbonementsAutoRenewEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        logger.debug("We got AccountProcessAbonementsAutoRenewEvent");

        try {
            serviceAbonementService.processAbonementsAutoRenewByAccount(account);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.event.account.listener.AccountAbonementsEventListener.onAccountProcessExpiringAbonementsEvent " + e.getMessage());
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountProccessNotifyExpiredAbonementEvent(AccountProcessNotifyExpiredAbonementsEvent event){
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        if (account.isFreeze()) {
            return;
        }

        logger.debug("We got AccountProcessNotifyExpiredAbonementsEvent");

        List<AccountStat> accountStats = accountStatRepository.findByPersonalAccountIdAndTypeAndCreatedAfterOrderByCreatedDesc(
                account.getId(),
                AccountStatType.VIRTUAL_HOSTING_ABONEMENT_DELETE,
                LocalDateTime.now().minusDays(21)
        );

        if (accountStats.isEmpty()) { return; }

        int[] daysAgo = {1, 3, 5, 10, 15, 20};
        boolean needToSendMail = false;
        LocalDate now = LocalDate.now();
        LocalDate abonementExpiredDate = accountStats.get(0).getCreated().toLocalDate();

        for (int dayAgo : daysAgo) {
            //Срок действия абонемента закончился - через 1, 3, 5, 10, 15, 20 дней после окончания
            if (abonementExpiredDate.isEqual(now.minusDays(dayAgo))) {

                Plan plan = planManager.findOne(account.getPlanId());
                if (!plan.isAbonementOnly()) {

                    //не отправляется, если хватает на 1 месяц хостинга по выбранному тарифу после окончания абонемента
                    //берем текущий баланс и сравниваем его с
                    //стоимостью тарифа за месяц, деленной на 30 дней и умноженная на количество оставшихся дней с окончания абонемента
                    Calendar cal = Calendar.getInstance();
                    int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                    BigDecimal balance = accountHelper.getBalance(account);

                    BigDecimal cost = accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getService());

                    needToSendMail = balance.compareTo(
                            (cost.divide(new BigDecimal(daysInMonth), BigDecimal.ROUND_FLOOR)
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

        BigDecimal costAbonement = accountHelper.getCostAbonement(account).setScale(2, BigDecimal.ROUND_DOWN);
        BigDecimal balance = accountHelper.getBalance(account).setScale(2, BigDecimal.ROUND_DOWN);
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("acc_id", account.getName());
        parameters.put("domains", accountNotificationHelper.getDomainForEmail(account));
        parameters.put("balance", formatBigDecimalWithCurrency(balance));
        parameters.put("cost_per_year", formatBigDecimalWithCurrency(costAbonement));
        accountNotificationHelper.sendMail(account,"MajordomoHmsAbonementEnd", parameters);
    }
}
