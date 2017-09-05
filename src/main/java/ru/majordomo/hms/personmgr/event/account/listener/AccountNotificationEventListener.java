package ru.majordomo.hms.personmgr.event.account.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.event.account.*;
import ru.majordomo.hms.personmgr.model.account.AccountStat;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.repository.AccountStatRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.*;
import ru.majordomo.hms.rc.user.resources.Domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_NAME_KEY;

@Component
public class AccountNotificationEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountEventListener.class);

    private final AccountHelper accountHelper;
    private final AccountStatRepository accountStatRepository;
    private final PlanRepository planRepository;
    private final AccountNotificationHelper accountNotificationHelper;
    private final BizMailFeignClient bizMailFeignClient;

    @Autowired
    public AccountNotificationEventListener(
            AccountHelper accountHelper,
            AccountStatRepository accountStatRepository,
            PlanRepository planRepository,
            AccountNotificationHelper accountNotificationHelper,
            BizMailFeignClient bizMailFeignClient
    ) {
        this.accountHelper = accountHelper;
        this.accountStatRepository = accountStatRepository;
        this.planRepository = planRepository;
        this.accountNotificationHelper = accountNotificationHelper;
        this.bizMailFeignClient = bizMailFeignClient;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void sendNotificationsRemainingDaysEvent(AccountSendNotificationsRemainingDaysEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, Object> params = event.getParams();
        BigDecimal dailyCost = (BigDecimal) params.get("daylyCost");

        // Уведомление о заканчивающихся средствах отправляются только активным аккаунтам или тем, у кого есть списания
        if (!account.isActive() || dailyCost.compareTo(BigDecimal.ZERO) == 0) { return;}

        BigDecimal balance = accountHelper.getBalance(account);
        Integer remainingDays = (balance.divide(dailyCost, 0, BigDecimal.ROUND_DOWN)).intValue();
        Integer remainingCreditDays = accountNotificationHelper.getRemainingDaysCreditPeriod(account);
        List<Integer> days = Arrays.asList(7, 5, 3, 2, 1);
        if (days.contains(remainingDays) || days.contains(remainingCreditDays)) {
            Boolean hasActiveAbonement = accountHelper.hasActiveAbonement(account);
            Boolean hasActiveCredit = accountHelper.hasActiveCredit(account);
            String remainingDaysInString = Utils.pluralizef("%d день", "%d дня", "%d дней", remainingDays);
            String remainingCreditDaysInString = Utils.pluralizef("%d день", "%d дня", "%d дней", remainingCreditDays);
            Boolean balanceIsPositive = balance.compareTo(BigDecimal.ZERO) > 0;

            String whatHappened = String.format("%s период хостинга на аккаунте %s истекает через %s.<br>",
                    balanceIsPositive ? "Оплаченный" : "Кредитный",
                    account.getName(),
                    hasActiveCredit && !balanceIsPositive ? remainingCreditDaysInString : remainingDaysInString
            );

            String actionAfter = String.format("После истечения %s%s дополнительные платные услуги будут %s.<br>",
                    balanceIsPositive ? "оплаченного периода" : "кредитного периода",
                    hasActiveAbonement ? "" : " аккаунт и",
                    hasActiveCredit && balanceIsPositive ? "работать в течение 14-дневного кредитного периода" : "отключены"
            );

            HashMap<String, String> paramsForEmail = new HashMap<>();
            paramsForEmail.put("acc_id", account.getName());
            paramsForEmail.put("what_happened", whatHappened);
            paramsForEmail.put("action_after", actionAfter);
            paramsForEmail.put("domains", accountNotificationHelper.getDomainForEmailWithPrefixString(account));
            paramsForEmail.put("balance", accountNotificationHelper.formatBigDecimalForEmail(balance) + " руб.");
            paramsForEmail.put("cost", accountNotificationHelper.getCostAbonementForEmail(planRepository.findOne(account.getPlanId())) + " руб/год.");
            String apiName = "MajordomoHmsMoneySoonEnd";
            accountNotificationHelper.sendMail(account, apiName, 10, paramsForEmail);
        }
//        Отправим смс тем, у кого подключена услуга
        if (Arrays.asList(5, 3, 1).contains(remainingDays)) {
            if (accountNotificationHelper.hasActiveSmsNotificationsAndMessageType(
                    account, MailManagerMessageType.SMS_REMAINING_DAYS
            )) {
                HashMap<String, String> paramsForSms = new HashMap<>();
                paramsForSms.put("remaining_days", Utils.pluralizef("остался %d день", "осталось %d дня", "осталось %d дней", remainingDays));
                paramsForSms.put("client_id", account.getAccountId());
                accountNotificationHelper.sendSms(account, "MajordomoRemainingDays", 10, paramsForSms);
            }
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountSendInfoMailEvent(AccountSendInfoMailEvent event) {
        PersonalAccount account = event.getSource();

        int accountAgeInDays = Utils.getDifferentInDaysBetweenDates(account.getCreated().toLocalDate(), LocalDate.now());

        String apiName = null;

        if (accountAgeInDays == 30) {
            //всем, в том числе и неактивным
            apiName = "MajordomoHmsKonstructorNethouse";

        } else if (account.isActive()) {

            switch (accountAgeInDays) {

                case 4:
                    //если не регистрировал домен у нас
                    List<AccountStat> accountStats = accountStatRepository.findByPersonalAccountIdAndTypeAndCreatedAfterOrderByCreatedDesc(
                            account.getId(),
                            AccountStatType.VIRTUAL_HOSTING_REGISTER_DOMAIN,
                            account.getCreated()
                    );

                    if (accountStats.isEmpty()) {
                        apiName = "MajordomoHmsDomainVPodarok";
                    }
                    break;

                case 9:
                    //только для базовых тарифов
                    VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) planRepository.findOne(account.getPlanId()).getPlanProperties();
                    if (!planProperties.isBusinessServices()) {
                        apiName = "MajordomoHmsCorporateTarif";
                    }
                    break;

                case 20:
                    apiName = "MajordomoHmsPromokodGoogle";
                    break;

                case 25:
                    //отправляем, если есть домены и ни один не привязан к biz.mail.ru
                    //делегирован домен на наши NS или нет - неважно
                    List<Domain> domains = accountHelper.getDomains(account);
                    if (domains == null || domains.isEmpty()) {
                        break;
                    }
                    List<Object> bizDomains = new ArrayList<>();
                    try {
                        bizDomains = bizMailFeignClient.getDomainsFromBizmail(account.getId());
                    } catch (Exception e) {
                        logger.error("Could not get domains from bizmail with account id [" + account.getId() + "] " + e.getMessage());
                    }
                    if (bizDomains != null && bizDomains.isEmpty()) {
                        apiName = "MajordomoHmsPochtaMailRu";
                    }
                    break;

                case 35:
                    apiName = "MajordomoHmsProdvigenie";
                    break;

                case 40:
                    apiName = "MajordomoHmsProtectSite";
                    break;

                case 45:
                    apiName = "MajordomoHmsPartners";
                    break;
            }
        }
        if (apiName != null) { accountNotificationHelper.sendInfoMail(account, apiName); }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountNotifyInactiveLongTimeEvent(AccountNotifyInactiveLongTimeEvent event) {
        PersonalAccount account = event.getSource();
        logger.debug("We got AccountNotifyInactiveLongTimeEvent\n");

        LocalDateTime deactivatedDateTime = account.getDeactivated();

        List<AccountStatType> types = new ArrayList<>();
        types.add(AccountStatType.VIRTUAL_HOSTING_ACC_OFF_NOT_ENOUGH_MONEY);
        types.add(AccountStatType.VIRTUAL_HOSTING_ABONEMENT_DELETE);

        AccountStat accountStat = accountStatRepository.findFirstByPersonalAccountIdAndTypeInAndCreatedAfterOrderByCreatedDesc(
                account.getId(), types, deactivatedDateTime.withHour(0).withMinute(0).withSecond(0));

        if (accountStat == null) { return; }

        //Если НЕ СОВПАДАЕТ  дата деактивации и последняя запись в статистике по причине отключения
        LocalDate deactivatedDate = deactivatedDateTime.toLocalDate();
        if (!accountStat.getCreated().toLocalDate().isEqual(deactivatedDate)) { return; }

        int[] monthsAgo = {1, 2, 3, 6, 12};

        for (int months : monthsAgo) {
            if (deactivatedDate.isEqual(LocalDate.now().minusMonths(months))) {
                accountNotificationHelper.sendInfoMail(account, "MajordomoHmsReturnClient");
                //Если найдено совпадения, дальше проверять не надо
                break;
            }
        }
    }

    //Отправка писем в случае выключенного аккаунта из-за нехватки средств
    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountDeactivatedSendMailEvent(AccountDeactivatedSendMailEvent event) {
        PersonalAccount account = event.getSource();

        logger.debug("We got AccountDeactivatedSendMailEvent\n");

        if (account.isActive() || planRepository.findOne(account.getPlanId()).isAbonementOnly()) { return;}

        AccountStat accountStat = accountStatRepository.findFirstByPersonalAccountIdAndTypeAndCreatedAfterOrderByCreatedDesc(
                account.getId(),
                AccountStatType.VIRTUAL_HOSTING_ACC_OFF_NOT_ENOUGH_MONEY,
                LocalDateTime.now().minusDays(21)
        );

        if (accountStat == null) {return;}

        // Если в тот же день есть удаление абонемента, то отправлять уведомление не нужно
        // причина выключения аккаунта - истекший абонемент
        // после удаления абонемента начислилась услуга тарифа и была неудачная попытка списания,
        // после чего аккаунт был выключен
        AccountStat accountStatAbonementDelete = accountStatRepository.findFirstByPersonalAccountIdAndTypeAndCreatedAfterOrderByCreatedDesc(
                account.getId(),
                AccountStatType.VIRTUAL_HOSTING_ABONEMENT_DELETE,
                accountStat.getCreated().withHour(0).withMinute(0).withSecond(0)
        );
        if (accountStatAbonementDelete != null
                && accountStatAbonementDelete.getCreated().toLocalDate().equals(
                accountStat.getCreated().toLocalDate())) {
            return;
        }

        int[] daysAgo = {1, 3, 5, 10, 15, 20};
        LocalDateTime dateFinish = accountStat.getCreated();

        LocalDate now = LocalDate.now();

        for (int days : daysAgo) {
            if (dateFinish.toLocalDate().isEqual(now.minusDays(days))) {
                accountNotificationHelper.sendMailForDeactivatedAccount(account, dateFinish);
                //отправляем только одно письмо
                break;
            }
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountQuotaAdded(AccountQuotaAddedEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        logger.debug("We got AccountQuotaAddedEvent");

        String planName = (String) params.get(SERVICE_NAME_KEY);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("tariff", planName);

        accountNotificationHelper.sendMail(account, "MajordomoVHQuotaAdd", 10, parameters);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountQuotaDiscard(AccountQuotaDiscardEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        logger.debug("We got AccountQuotaDiscardEvent");

        String planName = (String) params.get(SERVICE_NAME_KEY);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("tariff", planName);
        parameters.put("domains", accountNotificationHelper.getDomainForEmail(account));

        accountNotificationHelper.sendMail(account, "MajordomoVHQuotaDiscard", 10, parameters);
    }

    private HashMap<String, String> getParametersForVHLowLevelMoneyMail(PersonalAccount account, int remainingDays, BigDecimal balance) {
        HashMap<String, String> params = new HashMap<>();
        params.put("acc_id", account.getName());
        params.put("domains", accountNotificationHelper.getDomainForEmailWithPrefixString(account));
        params.put("balance", accountNotificationHelper.formatBigDecimalForEmail(balance));
        params.put("days", Utils.pluralizef("остался %d день", "осталось %d дня", "осталось %d дней", remainingDays));
        params.put("cost", accountNotificationHelper.getCostAbonementForEmail(planRepository.findOne(account.getPlanId())) + " руб/год.");
        return params;
    }
}

