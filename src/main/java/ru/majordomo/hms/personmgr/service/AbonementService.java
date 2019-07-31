package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.*;
import ru.majordomo.hms.personmgr.dto.AbonementsWrapper;
import ru.majordomo.hms.personmgr.event.account.AccountBuyAbonement;
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
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.PlanFallback;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AbonementRepository;
import ru.majordomo.hms.personmgr.repository.PlanFallbackRepository;
import ru.majordomo.hms.rc.user.resources.Domain;

import static java.time.temporal.ChronoUnit.DAYS;
import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.SMS_ABONEMENT_EXPIRING;
import static ru.majordomo.hms.personmgr.common.Utils.formatBigDecimalWithCurrency;

@Service
public class AbonementService {
    private final static Logger logger = LoggerFactory.getLogger(AbonementService.class);

    private final PlanManager planManager;
    private final AbonementRepository abonementRepository;
    //    private final PaymentServiceRepository paymentServiceRepository;
    private final AbonementManager<AccountAbonement> accountAbonementManager;
    private final AccountHelper accountHelper;
    private final AccountServiceHelper accountServiceHelper;
    private final ApplicationEventPublisher publisher;
    private final AccountStatHelper accountStatHelper;
    private final AccountNotificationHelper accountNotificationHelper;
    private final ChargeHelper chargeHelper;
    private final AccountHistoryManager history;
    private final AccountPromotionManager accountPromotionManager;
    private final DiscountFactory discountFactory;
    private final PlanFallbackRepository planFallbackRepository;

    private static TemporalAdjuster FOURTEEN_DAYS_AFTER = TemporalAdjusters.ofDateAdjuster(date -> date.plusDays(14));

    @Autowired
    public AbonementService(
            PlanManager planManager,
            AbonementRepository abonementRepository,
//            PaymentServiceRepository paymentServiceRepository,
            AbonementManager<AccountAbonement> accountAbonementManager,
            AccountHelper accountHelper,
            AccountServiceHelper accountServiceHelper,
            ApplicationEventPublisher publisher,
            AccountStatHelper accountStatHelper,
            AccountNotificationHelper accountNotificationHelper,
            ChargeHelper chargeHelper,
            AccountHistoryManager history,
            AccountPromotionManager accountPromotionManager,
            DiscountFactory discountFactory,
            PlanFallbackRepository planFallbackRepository
    ) {
        this.planManager = planManager;
        this.abonementRepository = abonementRepository;
//        this.paymentServiceRepository = paymentServiceRepository;
        this.accountAbonementManager = accountAbonementManager;
        this.accountHelper = accountHelper;
        this.accountServiceHelper = accountServiceHelper;
        this.publisher = publisher;
        this.accountStatHelper = accountStatHelper;
        this.accountNotificationHelper = accountNotificationHelper;
        this.chargeHelper = chargeHelper;
        this.history = history;
        this.accountPromotionManager = accountPromotionManager;
        this.discountFactory = discountFactory;
        this.planFallbackRepository = planFallbackRepository;
    }

    public AccountAbonement buyAbonementManual(PersonalAccount account, String abonementId) {
        AccountAbonement accountAbonement = addAbonement(account, abonementId);

        publisher.publishEvent(new AccountBuyAbonement(account.getId(), accountAbonement.getId()));

        return accountAbonement;
    }

    /**
     * Покупка абонемента
     *
     * @param account Аккаунт
     * @param abonementId id абонемента
     */
    public AccountAbonement addAbonement(PersonalAccount account, String abonementId) {
        Plan plan = getAccountPlan(account);

        Abonement abonement = plan.getAbonements().stream()
                .filter(a -> a.getId().equals(abonementId))
                .findFirst()
                .orElseThrow(() -> new ParameterValidationException(
                        "Current account plan does not have abonement with specified abonementId (not found in abonements)"
                ));

        AbonementsWrapper wrapper = new AbonementsWrapper(
                accountAbonementManager.findAllByPersonalAccountId(account.getId())
        );

        List<AccountAbonement> trialAbonements = wrapper.getAll().stream().filter(
                a -> a.getAbonement().isTrial()
        ).collect(Collectors.toList());

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

        if (wrapper.getAll().stream().noneMatch(a -> !a.getAbonement().isTrial() && a.getExpired() != null)) {
            accountAbonement.setExpired(LocalDateTime.now().plus(Period.parse(abonement.getPeriod())));
        }

        trialAbonements.forEach(accountAbonementManager::delete);

        accountAbonement = accountAbonementManager.insert(accountAbonement);

        deletePlanServiceIfExists(account, plan);

        if (!abonement.isInternal() && account.getSettings().get(AccountSetting.ABONEMENT_AUTO_RENEW) == null) {
            publisher.publishEvent(new AccountSetSettingEvent(account, AccountSetting.ABONEMENT_AUTO_RENEW, true));
        }

        return accountAbonement;
    }

    /**
     * Продление абонемента c текущей даты
     *  @param account Аккаунт
     * @param accountAbonement абонемент аккаунта
     * @param nextAbonement
     */
    private void renewAbonement(PersonalAccount account, AccountAbonement accountAbonement, Abonement nextAbonement) {
        Plan plan = getAccountPlan(account);

        ChargeMessage chargeMessage = new ChargeMessage.Builder(nextAbonement.getService())
                .build();
        accountHelper.charge(account, chargeMessage);

        accountAbonement.setAbonementId(nextAbonement.getId());
        accountAbonement.setExpired(
                accountAbonement.getExpired().plus(Period.parse(nextAbonement.getPeriod()))
        );

        accountAbonementManager.save(accountAbonement);

        deletePlanServiceIfExists(account, plan);
    }

    private void deletePlanServiceIfExists(PersonalAccount account, Plan plan) {
        if (accountServiceHelper.accountHasService(account, plan.getServiceId())) {
            accountServiceHelper.deleteAccountServiceByServiceId(account, plan.getServiceId());
        }
    }

    public void processExpiringAbonementsByAccount(PersonalAccount account) {
        if (account.getDeleted() != null) {
            logger.info("processExpiringAbonementsByAccount: account {} is deleted, return");
            return;
        }

        List<AccountAbonement> allAbonements = accountAbonementManager.findAllByPersonalAccountId(account.getId());

        Optional<AccountAbonement> optionalExpiredAbonement = getOneExpiredBeforeAbonement(
                LocalDateTime.now().with(FOURTEEN_DAYS_AFTER), allAbonements
        );

        if (optionalExpiredAbonement.isPresent()) {
            AccountAbonement current = optionalExpiredAbonement.get();

            LocalDateTime expired = current.getExpired();

            for (AccountAbonement abonement : allAbonements) {
                if (!abonement.getId().equals(current.getId())) {
                    expired = expired.plus(Period.parse(abonement.getAbonement().getPeriod()));
                }
            }

            long daysToExpired = DAYS.between(LocalDateTime.now(), expired);

            boolean isDayForSms = Arrays.asList(DAYS_FOR_ABONEMENT_EXPIRED_SMS_SEND).contains(daysToExpired);

            boolean isDayForEmail = Arrays.asList(DAYS_FOR_ABONEMENT_EXPIRED_EMAIL_SEND).contains(daysToExpired);

            if (!isDayForEmail && !isDayForSms) {
                logger.info("not need send email or sms: account {} expired {} daysToExpired {}",
                        account.getName(), expired, daysToExpired);
                return;
            }

            Optional<AccountAbonement> lastAbonement = allAbonements.stream()
                    .max(Comparator.comparing(AccountAbonement::getCreated));

            Plan plan = planManager.findOne(account.getPlanId());

            BigDecimal defaultP1YAbonementCost = plan.getDefaultP1YAbonementCost();

            //todo стоимость абонемента может быть 0, например, если это тестовый или бонусный абонемент
            //но у бонусных абонементов не может быть включено автопродление
            BigDecimal abonementCost = lastAbonement.isPresent() && !lastAbonement.get().getAbonement().isInternal()
                    ? lastAbonement.get().getAbonement().getService().getCost()
                    : defaultP1YAbonementCost;

            BigDecimal balance = accountHelper.getBalance(account);

            // Высчитываем предполагаемую месячную стоимость аккаунта
            int daysInCurrentMonth = LocalDateTime.now().toLocalDate().lengthOfMonth();
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

            //todo добавить дневные списания на daysToExpired дней, иначе с посуточными услугами может не хватить на следующий абонемент
            boolean notEnoughMoneyForAbonement = balance.compareTo(abonementCost) < 0;

            if (isDayForEmail) {
                if (plan.isArchival()) {
                    accountNotificationHelper.sendArchivalAbonementExpiring(account, balance, defaultP1YAbonementCost, expired);
                } else if (notEnoughMoneyForAbonement) {
                    if (plan.isAbonementOnly() || balance.compareTo(monthCost) < 0) {
                        accountNotificationHelper.sendHostingAbonementNoMoneyToProlong(account, balance, defaultP1YAbonementCost, expired);
                    }
                }
            }

            if (!account.isAbonementAutoRenew() || notEnoughMoneyForAbonement) {
                if (isDayForSms && accountNotificationHelper.isSubscribedToSmsType(account, SMS_ABONEMENT_EXPIRING)) {
                    accountNotificationHelper.sendSmsVhAbonementExpiring(account, daysToExpired);
                }
            }
        } else {
            logger.error("Not found expired hosting abonement on account {}, abonements: {}",
                    account.getId(), allAbonements);
        }
    }

    public void processAbonementsAutoRenewByAccount(PersonalAccount account) {
        List<AccountAbonement> allAbonements = accountAbonementManager.findAllByPersonalAccountId(account.getId());

        if (account.getDeleted() != null) {
            processExpiredAbonementsForDeletedAccount(account, allAbonements);
        } else {
            Optional<AccountAbonement> optionalExpiredAbonement = getOneExpiredBeforeAbonement(LocalDateTime.now(), allAbonements);

            if (optionalExpiredAbonement.isPresent()) {
                AccountAbonement accountAbonement = optionalExpiredAbonement.get();

                if (allAbonements.size() > 1) {
                    processMoreThanOneAbonements(accountAbonement, allAbonements);
                } else {
                    processLastAbonement(account, accountAbonement);
                }
            } else {
                logger.error("Not found expired hosting abonement on account {}, abonements: {}",
                        account.getId(), allAbonements);
            }
        }
    }

    private void processLastAbonement(PersonalAccount account, AccountAbonement accountAbonement) {
        logger.debug("We found expired abonement: " + accountAbonement);

        BigDecimal balance = accountHelper.getBalance(account);
        String currentExpired = accountAbonement.getExpired().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        Plan plan = getAccountPlan(account);

        if (plan.isArchival()) {
            processArchivalAbonement(account, accountAbonement);
        } else if (account.isAbonementAutoRenew()) {
            logger.debug("Account has abonement autoRenew option enabled");

            Optional<Abonement> withEnoughMoney = plan.getAbonements().stream()
                    .filter(a -> !a.isInternal() && balance.compareTo(a.getService().getCost()) >= 0)
                    .max(Comparator.comparing(a -> a.getService().getCost()));

            Abonement nextAbonement = accountAbonement.getAbonement().isInternal()
                    ? withEnoughMoney.orElseGet(plan::getDefaultP1YAbonement)
                    : accountAbonement.getAbonement();

            BigDecimal abonementCost = nextAbonement.getService().getCost();

            if (balance.compareTo(abonementCost) >= 0) {
                logger.debug("Buying new abonement. Balance: " + balance + " abonementCost: " + abonementCost);

                renewAbonement(account, accountAbonement, nextAbonement);

                history.save(account, "Автоматическое продление абонемента. Со счета аккаунта списано " +
                        formatBigDecimalWithCurrency(abonementCost) + " Новый срок окончания: "
                        + accountAbonement.getExpired().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            } else {
                logger.debug("Account balance is too low to buy new abonement. Balance: " + balance + " abonementCost: " + abonementCost);

                //Удаляем абонемент и включаем услуги хостинга по тарифу
                processAccountAbonementDelete(account, accountAbonement);

                history.save(account, "Абонемент удален, так как средств на счету аккаунта не достаточно. " +
                        "Стоимость абонемента: " + formatBigDecimalWithCurrency(abonementCost) +
                        " Баланс: " + formatBigDecimalWithCurrency(balance) +
                        " Дата окончания: " + currentExpired
                );
            }
        } else {
            //Удаляем абонемент и включаем услуги хостинга по тарифу
            processAccountAbonementDelete(account, accountAbonement);

            history.saveForOperatorService(
                    account,
                    "Абонемент удален, так как автопродление отключено. Дата окончания: " + currentExpired
            );
        }
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

    private void defaultProcessArchival(PersonalAccount account) {
        accountHelper.changeArchivalPlanToActive(account);

        history.saveForOperatorService(account, "Архивный тариф, абонемент удален");

        chargeHelper.prepareAndProcessChargeRequest(account.getId(), LocalDate.now());
    }

    void changeArchivalAbonementToActive(PersonalAccount account, Plan fallbackPlan) {
        Plan currentPlan = accountHelper.getPlan(account);
        accountServiceHelper.deleteAccountServiceByServiceId(account, currentPlan.getServiceId());

        //Парковку+ без доменов нужно выключить
        //Если есть домены, то меняем на старт
        if (currentPlan.getOldId().equals(PLAN_PARKING_PLUS_ID_STRING)) {
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
            addAbonement(account, fallbackAbonement.getId());

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
        PlanFallback planFallback = planFallbackRepository.findOneByPlanId(currentPlan.getId());
        if (planFallback != null && planFallback.getWithAbonementFallbackPlanId() != null) {
            return planManager.findOne(planFallback.getWithAbonementFallbackPlanId());
        } else {
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

    /**
     * Удаление абонемента
     *
     * @param account Аккаунт
     * @param accountAbonement абонемента на аккаунте
     */
    private void processAccountAbonementDelete(PersonalAccount account, AccountAbonement accountAbonement) {

        accountAbonementManager.delete(accountAbonement);

        Plan plan = planManager.findOne(account.getPlanId());
        boolean needToSendMail;
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
            addAbonement(account, abonement.getId());
            history.saveForOperatorService(account, "Добавлен абонемент на тестовый период (14 дней)");
        }
    }

    public void addPromoAbonementWithActivePlan(PersonalAccount account, Period period) {
        Plan plan = planManager.findOne(account.getPlanId());

        List<Abonement> abonements = abonementRepository.findByIdInAndInternalAndPeriod(
                plan.getAbonementIds(), true, period.toString());

        for (Abonement abonement : abonements) {
            if (abonement.getService().getCost().equals(BigDecimal.ZERO)) {
                addAbonement(account, abonement.getId());
                return;
            }
        }

        if (accountAbonementManager.existsByPersonalAccountId(account.getId())) {
            throw new ParameterValidationException("Не найден абонемент с бесплатным периодом " + period);
        } else {
            Abonement internal = plan.getAbonements().stream().filter(a -> a.isInternal() && !a.isTrial()).findFirst()
                    .orElseThrow(() -> new ParameterValidationException(
                            "Добавление абонемента на период " + period + " недоступно"
                    ));

            AccountAbonement accountAbonement = addAbonement(account, internal.getId());
            accountAbonementManager.setExpired(accountAbonement.getId(), LocalDateTime.now().plus(period));
        }
    }
/*
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
*/

    public BuyInfo getBuyInfo(PersonalAccount account, Abonement abonement) {
        BuyInfo info = new BuyInfo();

        if (abonement.isInternal()) {
            info.getErrors().add("Нельзя заказать тестовый абонемент");
            return info;
        }

        Plan plan = getAccountPlan(account);

        if (plan.isArchival()) {
            info.getErrors().add("Обслуживание по тарифу \"" + plan.getName() +  "\" прекращено");
            return info;
        }

        if (!plan.getAbonementIds().contains(abonement.getId())) {
            info.getErrors().add("На тарифе " + plan.getName() + " не доступен абонемент " + abonement.getName());
            return info;
        }

        AbonementsWrapper container = new AbonementsWrapper(
                accountAbonementManager.findAllByPersonalAccountId(account.getId())
        );

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime fullExpired = container.getExpired().plus(Period.parse(abonement.getPeriod()));

        //Абонемент нельзя продлить более чем на три года
        if (fullExpired.isAfter(now.plusYears(3))) {
            info.getErrors().add("Продление абонемента возможно не более чем на три года");
            return info;
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

    private void processExpiredAbonementsForDeletedAccount(PersonalAccount account, List<AccountAbonement> all) {
        LocalDateTime now = LocalDateTime.now();
        for (AccountAbonement accountAbonement : all) {
            if (accountAbonement.getExpired().isBefore(now)) {
                accountAbonementManager.delete(accountAbonement);
                all.remove(accountAbonement);
            }
        }
        if (all.isEmpty()) {
            accountServiceHelper.addAccountService(account, getAccountPlan(account).getServiceId());
        } else {
            AccountAbonement next = all.get(0);
            accountAbonementManager.setExpired(next.getId(), now.plus(Period.parse(next.getAbonement().getPeriod())));
        }
    }

    private void processMoreThanOneAbonements(AccountAbonement currentAbonement, List<AccountAbonement> allAbonements) {
        allAbonements.remove(currentAbonement);
        AccountAbonement next = allAbonements.get(0);
        accountAbonementManager.delete(currentAbonement);
        accountAbonementManager.setExpired(
                next.getId(), currentAbonement.getExpired().plus(Period.parse(next.getAbonement().getPeriod()))
        );
    }

    private Optional<AccountAbonement> getOneExpiredBeforeAbonement(
            LocalDateTime expiredBefore, List<AccountAbonement> allAbonements
    ) {
        logger.debug("Trying to find all expired abonements before expireEnd: "
                + expiredBefore.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        return allAbonements.stream()
                .filter(a -> a.getExpired() != null && a.getExpired().isBefore(expiredBefore))
                .min(Comparator.comparing(AccountAbonement::getExpired));
    }
}
