package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.util.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import ru.majordomo.hms.personmgr.common.*;
import ru.majordomo.hms.personmgr.dto.push.LowBalancePush;
import ru.majordomo.hms.personmgr.dto.fin.PaymentLinkRequest;
import ru.majordomo.hms.personmgr.dto.push.Push;
import ru.majordomo.hms.personmgr.event.account.AccountSendEmailWithExpiredAbonementEvent;
import ru.majordomo.hms.personmgr.event.account.AccountSetSettingEvent;
import ru.majordomo.hms.personmgr.exception.NotEnoughMoneyException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AbonementRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.rc.user.resources.Domain;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.DAYS;
import static ru.majordomo.hms.personmgr.common.AccountStatType.VIRTUAL_HOSTING_ABONEMENT_DELETE;
import static ru.majordomo.hms.personmgr.common.AccountStatType.VIRTUAL_HOSTING_USER_DELETE_ABONEMENT;
import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.SMS_ABONEMENT_EXPIRING;
import static ru.majordomo.hms.personmgr.common.Utils.formatBigDecimalWithCurrency;

@Service
public class AbonementService {
    private final static Logger logger = LoggerFactory.getLogger(AbonementService.class);

    private final PlanManager planManager;
    private final AbonementRepository abonementRepository;
    private final PaymentServiceRepository paymentServiceRepository;
    private final AbonementManager<AccountAbonement> accountAbonementManager;
    private final AccountHelper accountHelper;
    private final AccountServiceHelper accountServiceHelper;
    private final ApplicationEventPublisher publisher;
    private final AccountStatHelper accountStatHelper;
    private final AccountNotificationHelper accountNotificationHelper;
    private final ChargeHelper chargeHelper;
    private final AccountHistoryManager history;
    private final PaymentLinkHelper paymentLinkHelper;
    private final AccountPromotionManager accountPromotionManager;
    private final DiscountFactory discountFactory;

    private static TemporalAdjuster FOURTEEN_DAYS_AFTER = TemporalAdjusters.ofDateAdjuster(date -> date.plusDays(14));

    @Autowired
    public AbonementService(
            PlanManager planManager,
            AbonementRepository abonementRepository,
            PaymentServiceRepository paymentServiceRepository,
            AbonementManager<AccountAbonement> accountAbonementManager,
            AccountHelper accountHelper,
            AccountServiceHelper accountServiceHelper,
            ApplicationEventPublisher publisher,
            AccountStatHelper accountStatHelper,
            AccountNotificationHelper accountNotificationHelper,
            ChargeHelper chargeHelper,
            AccountHistoryManager history,
            PaymentLinkHelper paymentLinkHelper,
            AccountPromotionManager accountPromotionManager,
            DiscountFactory discountFactory
    ) {
        this.planManager = planManager;
        this.abonementRepository = abonementRepository;
        this.paymentServiceRepository = paymentServiceRepository;
        this.accountAbonementManager = accountAbonementManager;
        this.accountHelper = accountHelper;
        this.accountServiceHelper = accountServiceHelper;
        this.publisher = publisher;
        this.accountStatHelper = accountStatHelper;
        this.accountNotificationHelper = accountNotificationHelper;
        this.chargeHelper = chargeHelper;
        this.history = history;
        this.paymentLinkHelper = paymentLinkHelper;
        this.accountPromotionManager = accountPromotionManager;
        this.discountFactory = discountFactory;
    }

    /**
     * Покупка абонемента
     *
     * @param account Аккаунт
     * @param abonementId id абонемента
     * @param autorenew автопродление абонемента
     */
    public AccountAbonement addAbonement(PersonalAccount account, String abonementId, Boolean autorenew) {
        Plan plan = getAccountPlan(account);

        boolean accountHasFree14DaysAbonement = false, accountHasInternalAbonement = false;

        AccountAbonement currentAccountAbonement = accountAbonementManager.findByPersonalAccountId(account.getId());

        if (currentAccountAbonement != null) {
            accountHasFree14DaysAbonement = currentAccountAbonement.getAbonement().getPeriod().equals("P14D");

            accountHasInternalAbonement = currentAccountAbonement.getAbonement().isInternal();
        }

        Abonement abonement = checkAbonementAllownes(account, plan, abonementId);

        PaymentService service = abonement.getService();
        BigDecimal cost = service.getCost();

        AccountPromotion accountPromotion = accountPromotionManager.getServiceDiscountPromotion(account, service);

        if (accountPromotion != null) {
            cost = discountFactory.getDiscount(accountPromotion.getAction()).getCost(cost);
        }

        if (cost.compareTo(BigDecimal.ZERO) > 0) {

            ChargeMessage chargeMessage = new ChargeMessage.Builder(abonement.getService())
                    .setAmount(cost)
                    .build();
            accountHelper.charge(account, chargeMessage);
        }

        //при использовании скидки стоимость может стать 0
        //списания не будет, но отметить промоушен как использованный нужно
        if (accountPromotion != null) {
            accountPromotionManager.setAsUsedAccountPromotionById(accountPromotion.getId());
        }

        AccountAbonement accountAbonement = new AccountAbonement();
        accountAbonement.setAbonementId(abonementId);
        accountAbonement.setPersonalAccountId(account.getId());
        accountAbonement.setCreated(LocalDateTime.now());
        accountAbonement.setExpired(LocalDateTime.now().plus(Period.parse(abonement.getPeriod())));
        accountAbonement.setAutorenew(autorenew);

        if (!accountHasFree14DaysAbonement && accountHasInternalAbonement) {
            accountAbonement.setCreated(currentAccountAbonement.getCreated());
            accountAbonement.setExpired(currentAccountAbonement.getExpired().plus(Period.parse(abonement.getPeriod())));
        }

        if (accountHasFree14DaysAbonement || accountHasInternalAbonement) {
            accountAbonementManager.delete(currentAccountAbonement);
        }

        accountAbonement = accountAbonementManager.insert(accountAbonement);

        if (accountServiceHelper.accountHasService(account, plan.getServiceId())) {
            accountServiceHelper.deleteAccountServiceByServiceId(account, plan.getServiceId());
        }

        return accountAbonement;
    }

    /**
     * Продление абонемента c текущей даты
     *
     * @param account Аккаунт
     * @param accountAbonement абонемент аккаунта
     */
    public void renewAbonement(PersonalAccount account, AccountAbonement accountAbonement) {
        Plan plan = getAccountPlan(account);

        ChargeMessage chargeMessage = new ChargeMessage.Builder(accountAbonement.getAbonement().getService())
                .build();
        accountHelper.charge(account, chargeMessage);

        accountAbonementManager.setExpired(
                accountAbonement.getId(),
                LocalDateTime.now()
                        .plus(Period.parse(accountAbonement.getAbonement().getPeriod()))
        );

        if (accountServiceHelper.accountHasService(account, plan.getServiceId())) {
            accountServiceHelper.deleteAccountServiceByServiceId(account, plan.getServiceId());
        }
    }

    /**
     * Продление абонемента с даты его окончания
     *
     * @param account Аккаунт
     * @param accountAbonement абонемент аккаунта
     */
    public void prolongAbonement(PersonalAccount account, AccountAbonement accountAbonement) {
        Plan plan = getAccountPlan(account);

        ChargeMessage chargeMessage = new ChargeMessage.Builder(accountAbonement.getAbonement().getService())
                .build();
        accountHelper.charge(account, chargeMessage);

        accountAbonementManager.setExpired(
                accountAbonement.getId(),
                accountAbonement.getExpired()
                        .plus(Period.parse(accountAbonement.getAbonement().getPeriod()))
        );

        if (accountServiceHelper.accountHasService(account, plan.getServiceId())) {
            accountServiceHelper.deleteAccountServiceByServiceId(account, plan.getServiceId());
        }
    }

    /**
     * Удаление абонемента
     *
     * @param account Аккаунт
     * @param accountAbonementId id абонемента на аккаунте
     */
    public void deleteAbonement(PersonalAccount account, String accountAbonementId) {
        AccountAbonement accountAbonement = accountAbonementManager.findByIdAndPersonalAccountId(accountAbonementId, account.getId());

        processAccountAbonementDelete(account, accountAbonement, VIRTUAL_HOSTING_USER_DELETE_ABONEMENT);
    }

    public void processExpiringAbonementsByAccount(PersonalAccount account) {
        //В итоге нам нужно получить абонементы которые заканчиваются через 14 дней и раньше
        LocalDateTime expireEnd = LocalDateTime.now().with(FOURTEEN_DAYS_AFTER);

        logger.debug("Trying to find all expiring abonements before expireEnd: "
                + expireEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        List<AccountAbonement> accountAbonements  = accountAbonementManager.findByPersonalAccountIdAndExpiredBefore(account.getId(), expireEnd);

        //Ну вообще-то должен быть только один Абонемент)
        accountAbonements.forEach(accountAbonement -> {
            if (accountAbonement.getAbonement() == null
                    || accountAbonement.getAbonement().getService() == null
                    || accountAbonement.getAbonement().getService().getCost() == null) {
                logger.error("We found accountAbonement with null abonement or service or cost: " + accountAbonement);
                return;
            }

            long daysToExpired = DAYS.between(LocalDateTime.now(), accountAbonement.getExpired());

            boolean isDayForSms = Arrays.asList(DAYS_FOR_ABONEMENT_EXPIRED_SMS_SEND).contains(daysToExpired);

            boolean isDayForEmail = Arrays.asList(DAYS_FOR_ABONEMENT_EXPIRED_EMAIL_SEND).contains(daysToExpired);

            if (!isDayForEmail && !isDayForSms) {
                logger.info("not need send email or sms: account {} abonement {} expired {}",
                        account.getName(), accountAbonement.getAbonement().getName(), accountAbonement.getExpired()
                );
                return;
            }

            BigDecimal abonementCost = accountAbonement.getAbonement().getService().getCost();

            logger.debug("We found expiring abonement: " + accountAbonement);

            BigDecimal balance = accountHelper.getBalance(account);

            // Высчитываем предполагаемую месячную стоимость аккаунта
            int daysInCurrentMonth = LocalDateTime.now().toLocalDate().lengthOfMonth();
            Plan plan = planManager.findOne(account.getPlanId());
            PaymentService planAccountService = plan.getService();
            BigDecimal monthCost = planAccountService.getCost();

            List<AccountService> accountServices = account.getServices();
            for (AccountService accountService : accountServices) {
                if (accountService.isEnabled() && accountService.getPaymentService() != null
                        && !accountService.getPaymentService().getId().equals(planAccountService.getId())) {
                    switch (accountService.getPaymentService().getPaymentType()) {
                        case MONTH:
                            monthCost = monthCost.add(accountService.getCost());
                            break;
                        case DAY:
                            monthCost = monthCost.add(accountService.getCost().multiply(BigDecimal.valueOf(daysInCurrentMonth)));
                            break;
                    }
                }
            }

            boolean notEnoughMoneyForAbonement = balance.compareTo(abonementCost) < 0;

            if (isDayForEmail) {
                Lazy<String> dateFinish = Lazy.of(() -> accountAbonement.getExpired().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                if (accountHelper.needChangeArchivalPlanToFallbackPlan(account)) {

                    accountNotificationHelper.emailBuilder()
                            .account(account)
                            .apiName("MajordomoVHExpiringArchivalAbonement")
                            .from("noreply@majordomo.ru")
                            .priority(1)
                            .param("client_id", account.getAccountId())
                            .param("acc_id", account.getName())
                            .param("domains", accountNotificationHelper.getDomainForEmail(account))
                            .param("balance", formatBigDecimalWithCurrency(balance))
                            .param("cost", formatBigDecimalWithCurrency(abonementCost))
                            .param("date_finish", dateFinish.get())
                            .param("from", "noreply@majordomo.ru")
                            .send();

                    accountNotificationHelper.push(
                            new Push(account.getId(), account.getName() + " истекает архивный абонемент",
                            "Архивный абонемент " + account.getName() + " заканчивается " + dateFinish.get()
                                    + ". Мы автоматически переведем аккаунт на наиболее подходящий тариф из действующих."
                            )
                    );
                } else if (notEnoughMoneyForAbonement) {
                    if (plan.isAbonementOnly() || balance.compareTo(monthCost) < 0) {
                        logger.debug("Account balance is too low to buy new abonement. Balance: " + balance + " abonementCost: " + abonementCost);

                        String paymentLink = paymentLinkHelper.generatePaymentLinkForMail(
                                account, new PaymentLinkRequest(abonementCost)
                        ).getPaymentLink();

                        accountNotificationHelper.emailBuilder()
                                .account(account)
                                .apiName("MajordomoVHAbNoMoneyProlong")
                                .from("noreply@majordomo.ru")
                                .priority(1)
                                .param("client_id", account.getAccountId())
                                .param("acc_id", account.getName())
                                .param("domains", accountNotificationHelper.getDomainForEmail(account))
                                .param("balance", formatBigDecimalWithCurrency(balance))
                                .param("cost", formatBigDecimalWithCurrency(abonementCost))
                                .param("date_finish", dateFinish.get())
                                .param("from", "noreply@majordomo.ru")
                                .param("payment_link", paymentLink)
                                .send();

                        accountNotificationHelper.push(
                                new LowBalancePush(account.getId(), account.getName() + " Абонемент истекает " + dateFinish.get(),
                                        format("Срок действия абонемента на аккаунте %s заканчивается %s. " +
                                                        "Для продолжения работы продлите абонемент со скидкой за %s",
                                                account.getName(), dateFinish.get(), Utils.formatBigDecimalWithCurrency(abonementCost)
                                        ), abonementCost
                                )
                        );
                    }
                }
            }

            if (!accountAbonement.isAutorenew() || notEnoughMoneyForAbonement) {
                if (isDayForSms && accountNotificationHelper.isSubscribedToSmsType(account, SMS_ABONEMENT_EXPIRING))
                {
                    HashMap<String, String> paramsForSms = new HashMap<>();
                    paramsForSms.put("acc_id", account.getName());
                    paramsForSms.put("client_id", account.getAccountId());
                    paramsForSms.put("remaining_days", Utils.pluralizef("%d день", "%d дня", "%d дней", ((Long) daysToExpired).intValue()));
                    accountNotificationHelper.sendSms(account, "HmsMajordomoAbonementExpiring", 5, paramsForSms);
                }
            }
        });
    }

    public void processAbonementsAutoRenewByAccount(PersonalAccount account) {
        //В итоге нам нужно получить абонементы которые закончились сегодня и раньше
        LocalDateTime expireEnd = LocalDateTime.now();

        logger.debug("Trying to find all expired abonements before expireEnd: "
                + expireEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        List<AccountAbonement> accountAbonements  = accountAbonementManager.findByPersonalAccountIdAndExpiredBefore(account.getId(), expireEnd);

        if (accountAbonements.isEmpty()) {
            logger.debug("Not found expired abonements for accountId: " + account.getId());
        }

        //Ну вообще-то должен быть только один Абонемент)
        accountAbonements.forEach(accountAbonement -> {
            logger.debug("We found expired abonement: " + accountAbonement);

            BigDecimal balance = accountHelper.getBalance(account);
            BigDecimal abonementCost = accountAbonement.getAbonement().getService().getCost();
            String currentExpired = accountAbonement.getExpired().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

            if (!accountAbonement.getAbonement().isInternal() && accountHelper.needChangeArchivalPlanToFallbackPlan(account)) {
                processArchivalAbonement(account, accountAbonement);
            } else if (!accountAbonement.getAbonement().isInternal() && accountAbonement.isAutorenew()) {
                    logger.debug("Abonement has autorenew option enabled");

                    if (balance.compareTo(abonementCost) >= 0) {
                        logger.debug("Buying new abonement. Balance: " + balance + " abonementCost: " + abonementCost);

                        renewAbonement(account, accountAbonement);

                        history.saveForOperatorService(account,
                                "Автоматическое продление абонемента. Со счета аккаунта списано " +
                                formatBigDecimalWithCurrency(abonementCost) +
                                " Новый срок окончания: " + accountAbonement.getExpired().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                    } else {
                        logger.debug("Account balance is too low to buy new abonement. Balance: " + balance + " abonementCost: " + abonementCost);

                        //Удаляем абонемент и включаем услуги хостинга по тарифу
                        processAccountAbonementDelete(account, accountAbonement);

                        history.saveForOperatorService(account, "Абонемент удален, так как средств на счету аккаунта не достаточно. Стоимость абонемента: " +
                                formatBigDecimalWithCurrency(abonementCost) +
                                " Баланс: " + formatBigDecimalWithCurrency(balance) +
                                " Дата окончания: " + currentExpired
                        );

                        //Сохраним "на будущее" установку автопокупки абонемента
                        publisher.publishEvent(new AccountSetSettingEvent(account, AccountSetting.ABONEMENT_AUTO_RENEW, true));
                    }
            } else {
                // Если абонемент бонусный (internal)

                //Удаляем абонемент и включаем услуги хостинга по тарифу
                processAccountAbonementDelete(account, accountAbonement);

                history.saveForOperatorService(
                        account,
                        "Бонусный абонемент удален. Обычный абонемент не был предзаказн. Дата окончания: " + currentExpired
                );
            }
        });
    }

    private void processArchivalAbonement(PersonalAccount account, AccountAbonement accountAbonement) {
        logger.info("processArchivalAbonement( PersonalAccount (id='" + account.getId()
                + "'), AccountAbonement(id='" + accountAbonement.getId()
                + "', Abonement(id=" + accountAbonement.getAbonement().getId() + "'));"
        );

        accountAbonementManager.delete(accountAbonement);
        accountStatHelper.abonementDelete(accountAbonement);

        Plan fallbackPlan = getArchivalFallbackPlan(getAccountPlan(account));
        if (fallbackPlan == null) {
            defaultProcessArchival(account);
        } else {
            changeArchivalAbonementToActive(account, fallbackPlan);
        }
    }

    public void defaultProcessArchival(PersonalAccount account) {
        accountHelper.changeArchivalPlanToActive(account);

        history.saveForOperatorService(account, "Архивный тариф, абонемент удален");

        chargeHelper.prepareAndProcessChargeRequest(account.getId(), LocalDate.now());
    }

    void changeArchivalAbonementToActive(PersonalAccount account, Plan fallbackPlan) {
        Plan currentPlan = accountHelper.getPlan(account);
        accountServiceHelper.deleteAccountServiceByServiceId(account, currentPlan.getServiceId());

        //Парковку+ без доменов нужно выключить
        //Если есть домены, то меняем на старт
        if (account.getPlanId().equals(PLAN_PARKING_PLUS_ID_STRING)) {
            List<Domain> domains = accountHelper.getDomains(account);
            if (domains == null || domains.isEmpty()) {
                accountHelper.setPlanId(account.getId(), fallbackPlan.getId());
                accountHelper.addArchivalPlanAccountNoticeRepository(account, currentPlan);
                accountHelper.disableAccount(account);

                history.save(
                        account,
                        "Архивный абонемент 'Парковка+' удален после истечения, аккаунт отключен, тариф изменён" +
                                " на " + fallbackPlan.getName());
                return;
            }
        }

        try {
            Abonement fallbackAbonement = getFallbackAbonement(fallbackPlan);
            //Тут может выпасть исключение, если тариф не совпадает с покупаемым абонементом или нехватает денег
            account.setPlanId(fallbackPlan.getId());
            addAbonement(account, fallbackAbonement.getId(), true);

            //При успешной покупке абонемента устанавливаем id тарифа
            accountHelper.setPlanId(account.getId(), fallbackPlan.getId());

            accountStatHelper.archivalPlanChange(account.getId(), currentPlan.getId(), fallbackPlan.getId());
            accountHelper.addArchivalPlanAccountNoticeRepository(account, currentPlan);
            history.save(account, "Архивный абонемент заменен на активный " + fallbackAbonement.getName());
        } catch (Exception e) {
            if (e instanceof NotEnoughMoneyException) {
                history.save(account, "Не хватило денег на абонемент на тарифе " + fallbackPlan.getName()
                        + " " + ((NotEnoughMoneyException) e).getRequiredAmount()
                        + " Выполняется смена с архивного абонемента на посуточные списания");
            } else {
                history.save(account, "Ошибка при покупке абонемента вместо архивного "
                        + " на тарифе " + fallbackPlan.getId() + " message: " + e.getMessage()
                        + " Выполняется смена с архивного абонемента на посуточные списания");
                e.printStackTrace();
            }
            account.setPlanId(currentPlan.getId());
            defaultProcessArchival(account);
        }
    }

    private Abonement getFallbackAbonement(Plan fallbackPlan) throws ParameterValidationException {
        Optional<Abonement> fallbackAbonement = fallbackPlan.getAbonements().stream().filter(abonement ->
                !abonement.isInternal()
                        && abonement.getPeriod().equals("P1Y")
        ).findFirst();
        if (!fallbackAbonement.isPresent()) {
            throw new ParameterValidationException(
                    "Не найден подходящий абонемент для смены с архивного "
                            + " на тарифе " + fallbackPlan.getId());
        }
        return fallbackAbonement.get();
    }

    public Plan getArchivalFallbackPlan(Plan currentPlan) {
        switch (currentPlan.getOldId()) {
            case PLAN_PARKING_PLUS_ID_STRING:
            case SITE_VISITKA_PLAN_OLD_ID:
                return planManager.findByOldId(String.valueOf(PLAN_PARKING_DOMAINS_ID));
            default:
                return null;
        }
    }

    private Plan getAccountPlan(PersonalAccount account) {
        Plan plan = planManager.findOne(account.getPlanId());

        if(plan == null){
            throw new ResourceNotFoundException("Account plan not found");
        }

        return plan;
    }

    private Abonement checkAbonementAllownes(PersonalAccount account, Plan plan, String abonementId) {

        if (!plan.getAbonementIds().contains(abonementId)) {
            throw new ParameterValidationException("Current account plan does not have abonement with specified abonementId");
        }

        Optional<Abonement> newAbonement = plan.getAbonements().stream().filter(abonement1 -> abonement1.getId().equals(abonementId)).findFirst();

        if (!newAbonement.isPresent()) {
            throw new ParameterValidationException("Current account plan does not have abonement with specified abonementId (not found in abonements)");
        }

        Abonement abonement = newAbonement.get();

        AccountAbonement accountAbonement = accountAbonementManager.findByPersonalAccountId(account.getId());

        if (accountAbonement != null && !accountAbonement.getAbonement().isInternal()) {
            throw new ParameterValidationException("Account already has abonement");
        }

        return abonement;
    }

    /**
     * Удаление абонемента
     *
     * @param account Аккаунт
     * @param accountAbonement абонемента на аккаунте
     */
    private void processAccountAbonementDelete(PersonalAccount account, AccountAbonement accountAbonement, AccountStatType reason) {

        accountAbonementManager.delete(accountAbonement);

        Plan plan = planManager.findOne(account.getPlanId());
        boolean needToSendMail = false;
        if (!plan.isAbonementOnly()) {
            BigDecimal balance = accountHelper.getBalance(account);
            BigDecimal costForOneMonth = plan.getService().getCost();
            needToSendMail = balance.compareTo(costForOneMonth) < 0;
        } else {
            needToSendMail = true;
        }
        if (needToSendMail) {
            publisher.publishEvent(new AccountSendEmailWithExpiredAbonementEvent(account));
        }

        accountStatHelper.abonementDelete(accountAbonement);

        //Создаем AccountService с выбранным тарифом
        addPlanServicesAfterAbonementExpire(account);

        if (planManager.findOne(account.getPlanId()).isAbonementOnly()) {
            accountHelper.disableAccount(account);
        } else {
            chargeHelper.prepareAndProcessChargeRequest(account.getId(), LocalDate.now());
        }
    }

    private void processAccountAbonementDelete(PersonalAccount account, AccountAbonement accountAbonement) {
        this.processAccountAbonementDelete(account, accountAbonement, VIRTUAL_HOSTING_ABONEMENT_DELETE);
    }

    /**
     * Добавление(проверка наличия) услуг хостинга по тарифу после окончания абонемента
     *
     * @param account Аккаунт
     */
    private void addPlanServicesAfterAbonementExpire(PersonalAccount account) {
        Plan plan = getAccountPlan(account);

        if (!accountServiceHelper.accountHasService(account, plan.getServiceId())) {
            accountServiceHelper.addAccountService(account, plan.getServiceId());
        }
    }

    public void addFree14DaysAbonement(PersonalAccount account) {
        Plan plan = planManager.findOne(account.getPlanId());
        Abonement abonement = plan.getFree14DaysAbonement();

        if (abonement != null) {
            addAbonement(account, abonement.getId(), false);
            history.saveForOperatorService(account, "Добавлен абонемент на тестовый период (14 дней)");
        }
    }

    public void addPromoAbonementWithActivePlan(PersonalAccount account, Period period) {

        Plan plan = planManager.findOne(account.getPlanId());

        List<Abonement> abonements = abonementRepository.findByIdInAndInternalAndPeriod(
                plan.getAbonementIds(), true, period.toString());

        Abonement abonement = null;
        for (Abonement a : abonements) {
            if (a.getService().getCost().equals(BigDecimal.ZERO)) {
                abonement = a;
                break;
            }
        }

        if (abonement == null) {
            abonement = generatePromoAbonementByPlanAndPeriod(plan, period);
        }

        addAbonement(account, abonement.getId(), false);
    }

    private Abonement generatePromoAbonementByPlanAndPeriod(Plan plan, Period period){
        PaymentService paymentService = generatePromoServiceByPlanAndPeriod(plan, period);

        Abonement abonement = new Abonement();

        abonement.setInternal(true);
        abonement.setName(plan.getName() + suffixName(period));
        abonement.setServiceId(paymentService.getId());
        abonement.setType(Feature.VIRTUAL_HOSTING_PLAN);
        abonement.setPeriod(period.toString());

        abonementRepository.insert(abonement);

        planManager.addAbonementId(plan.getId(), abonement.getId());

        return abonement;
    }

    private PaymentService generatePromoServiceByPlanAndPeriod(Plan plan, Period period){
        PaymentService paymentService = new PaymentService();

        paymentService.setCost(BigDecimal.ZERO);
        paymentService.setName(plan.getName() + suffixName(period));
        paymentService.setAccountType(AccountType.VIRTUAL_HOSTING);
        paymentService.setActive(false);
        paymentService.setLimit(1);
        paymentService.setOldId("plan_abonement_" + plan.getOldId() + "_" + period.toString());
        paymentService.setPaymentType(ServicePaymentType.ONE_TIME);

        return paymentServiceRepository.insert(paymentService);
    }

    private String suffixName(Period period) {
        switch (period.toString()) {
            case "P1M":
                return " (абонемент на 1 месяц)";
            case "P3M":
                return " (абонемент на 3 месяца)";
            case "P6M":
                return " (абонемент на 6 месяцев)";
            case "P9M":
                return " (абонемент на 9 месяцев)";
            case "P1Y":
                return " (абонемент на 1 год)";
            case "P2Y":
                return " (абонемент на 2 года)";
            default:
                throw new ParameterValidationException("Некорректный период");
        }
    }

    public BuyInfo getBuyInfo(PersonalAccount account, Abonement abonement) {
        BuyInfo info = new BuyInfo();

        if (abonement.isInternal()) {
            info.getErrors().add("Нельзя заказать тестовый абонемент");
        }

        AccountAbonement currentAccountAbonement = accountAbonementManager.findByPersonalAccountId(account.getId());

        if (currentAccountAbonement != null && !currentAccountAbonement.getAbonement().isInternal()) {
            info.getErrors().add("Нельзя купить абонемент при наличии другого абонемента");
        }

        Plan plan = getAccountPlan(account);

        if (accountHelper.needChangeArchivalPlanToFallbackPlan(account)) {
            info.getErrors().add("Обслуживание по тарифу \"" + plan.getName() +  "\" прекращено");
        }

        if (!plan.getAbonementIds().contains(abonement.getId())) {
            info.getErrors().add("На тарифе " + plan.getName() + " не доступен абонемент " + abonement.getName());
        }

        PaymentService service = abonement.getService();
        BigDecimal cost = service.getCost();

        AccountPromotion accountPromotion = accountPromotionManager.getServiceDiscountPromotion(account, service);

        if (accountPromotion != null) {
            cost = discountFactory.getDiscount(accountPromotion.getAction()).getCost(cost);
        }

        BigDecimal balance = accountHelper.getBalance(account);

        info.setBalance(balance);
        info.setBalanceAfterOperation(balance.subtract(cost));

        if (info.getErrors().isEmpty() && info.getBalanceAfterOperation().compareTo(BigDecimal.ZERO) >= 0) {
            info.setAllowed(true);
        }

        return info;
    }
}
