package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.dto.IdsContainer;
import ru.majordomo.hms.personmgr.manager.AccountAbonementManager;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.DeferredPlanChangeNotice;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountNoticeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.Constants.SITE_VISITKA_PLAN_OLD_ID;

@Service
public class ArchivalPlanProcessor {
    private final static Logger log = LoggerFactory.getLogger(ArchivalPlanProcessor.class);
    private final static TemporalAdjuster MAX_PERIOD_ARCHIVAL_PLAN_MUST_BE_CHANGED =
            TemporalAdjusters.ofDateAdjuster(date -> date.plusMonths(3));

    private AccountNoticeRepository accountNoticeRepository;
    private PlanManager planManager;
    private PersonalAccountManager accountManager;
    private AccountHelper accountHelper;
    private AccountHistoryManager history;
    private AccountServiceHelper accountServiceHelper;
    private AccountAbonementManager accountAbonementManager;
    private MongoOperations mongoOperations;
    private AccountStatHelper accountStatHelper;
    private ChargeHelper chargeHelper;
    private AbonementService abonementService;

    @Autowired
    public ArchivalPlanProcessor(
            AccountNoticeRepository accountNoticeRepository,
            PlanManager planManager,
            PersonalAccountManager accountManager,
            AccountHelper accountHelper,
            AccountHistoryManager history,
            AccountServiceHelper accountServiceHelper,
            AccountAbonementManager accountAbonementManager,
            MongoOperations mongoOperations,
            AccountStatHelper accountStatHelper,
            ChargeHelper chargeHelper,
            AbonementService abonementService
    ) {
        this.accountNoticeRepository = accountNoticeRepository;
        this.planManager = planManager;
        this.accountManager = accountManager;
        this.accountHelper = accountHelper;
        this.history = history;
        this.accountServiceHelper = accountServiceHelper;
        this.accountAbonementManager = accountAbonementManager;
        this.mongoOperations = mongoOperations;
        this.accountStatHelper = accountStatHelper;
        this.chargeHelper = chargeHelper;
        this.abonementService = abonementService;
    }

    public void createDeferredTariffChangeNoticeForDaily() {
        List<String> accountIds = getAccountIdWithArchivalPlanAndActive(true);
        log.info("active accounts : " + accountIds.size());

        accountIds = filterByWithoutAbonement(accountIds);
        log.info("active accounts without active abonement: " + accountIds.size());

        for(String accountId: accountIds) {
            PersonalAccount account = accountManager.findOne(accountId);
            Plan currentPlan = planManager.findOne(account.getPlanId());

            if (accountHelper.needChangeArchivalPlanToFallbackPlan(account)) {
                int remainingDays = computeRemainingDays(account, currentPlan);

                LocalDate changeAfter = getChargeAfter(remainingDays);

                addDeferredTariffChangeNotice(account, changeAfter);

                history.saveForOperatorService(
                        account,
                        "Текущий архивный тариф " + currentPlan.getName()
                                + " будет изменен на подходящий тариф после " + changeAfter.toString()
                );
            }
        }
    }

    private LocalDate getChargeAfter(int plusDays) {
        LocalDate changeAfter = LocalDate.now().plusDays(plusDays);
        if (changeAfter.isAfter(LocalDate.now().with(MAX_PERIOD_ARCHIVAL_PLAN_MUST_BE_CHANGED))) {
            changeAfter = LocalDate.now().with(MAX_PERIOD_ARCHIVAL_PLAN_MUST_BE_CHANGED);
            while (changeAfter.getDayOfWeek().getValue() > 3) {
                changeAfter = changeAfter.plusDays(1);
            }
        }
        return changeAfter;
    }

    private int computeRemainingDays(PersonalAccount account, Plan plan) {
        String template = "%10s | %5s | причина: %1s";
        int maxPeriod = 92;
        int minPeriod = 3;
        BigDecimal balance = accountHelper.getBalance(account);

        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            log.info(String.format(template, account.getId(), minPeriod, "баланс равен " + balance));
            return minPeriod;
        }

        List<AccountService> accountServices = accountServiceHelper.getDailyServicesToCharge(account, LocalDate.now());

        BigDecimal dailyCost = accountServices
                .stream()
                .filter(accountService -> !accountService.getPaymentService().getId().equals(plan.getServiceId()))
                .filter(AccountService::isEnabled)
                .map(accountService ->
                        accountService.getPaymentService().getCost()
                                .multiply(BigDecimal.valueOf(accountService.getQuantity()))
                )
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO)
                .add(plan.getService().getCost().divide(BigDecimal.valueOf(30), 4, BigDecimal.ROUND_HALF_UP));

        if (dailyCost.compareTo(BigDecimal.ZERO) == 0) {
            log.info(String.format(template, account.getId(), maxPeriod, "дневное списание = 0"));
            return maxPeriod;
        }

        int remainingDays = balance.divide(dailyCost, 0, BigDecimal.ROUND_DOWN).intValue();

        if (remainingDays >= minPeriod && remainingDays <= maxPeriod) {
            log.info(String.format(template, account.getId(), remainingDays, "ok"));
            return remainingDays;
        } else {
            log.info(String.format(template, account.getId(), maxPeriod, "выходит за пределы перида для смены, days: " + remainingDays));
            return maxPeriod;
        }
    }

    private void addDeferredTariffChangeNotice(PersonalAccount account, LocalDate changeAfter) {
        DeferredPlanChangeNotice notice = new DeferredPlanChangeNotice();
        notice.setPersonalAccountId(account.getId());
        notice.setWillBeChangedAfter(changeAfter);
        accountNoticeRepository.save(notice);
    }

    private List<String> archivalTariffIds() {
        return planManager.findAll().stream()
                .filter(plan -> !plan.isAbonementOnly()
                        && !plan.isActive()
                        && plan.getService().getCost().compareTo(accountHelper.getArchivalFallbackPlan().getService().getCost()) < 0)
                .map(Plan::getId).collect(Collectors.toList());
    }

    private List<String> getAccountIdWithArchivalPlanAndActive(boolean accountIsActive) {
        List<String> planIds = archivalTariffIds();

        Aggregation aggregation = newAggregation(
                Aggregation.match(
                        new Criteria()
                                .andOperator(
                                        new Criteria()
                                                .orOperator(
                                                        Criteria.where("deleted").exists(false),
                                                        Criteria.where("deleted").is(null)
                                                ),
                                        Criteria.where("planId").in(planIds),
                                        Criteria.where("active").is(accountIsActive)
                                )
                ),
                Aggregation.group().addToSet("accountId").as("ids")
        );

        List<String> accountIds = new ArrayList<>();

        List<IdsContainer> idsContainers = mongoOperations.aggregate(aggregation, PersonalAccount.class, IdsContainer.class)
                .getMappedResults();

        if (idsContainers != null && !idsContainers.isEmpty()) {
            accountIds = idsContainers.get(0).getIds();
        }

        return accountIds;
    }

    private List<String> filterByWithoutAbonement(List<String> accountIds) {
        LocalDateTime now = LocalDateTime.now();
        return accountIds
                .stream()
                .filter(accountId -> !accountAbonementManager.existsByPersonalAccountIdAndExpiredAfter(accountId, now))
                .collect(Collectors.toList());
    }

    public String changeArchivalPlanOnInactiveAccounts() {
        Plan parkingPlan = planManager.findByOldId(String.valueOf(PLAN_PARKING_DOMAINS_ID));
        Plan startPlan = planManager.findByOldId(String.valueOf(PLAN_START_ID));
        Plan unlimitedPlan = planManager.findByOldId(String.valueOf(PLAN_UNLIMITED_ID));

        Map<String, Plan> archivalToActive = new HashMap<>();
        archivalToActive.put(MAIL_PLAN_OLD_ID, startPlan);
        archivalToActive.put(SITE_VISITKA_PLAN_OLD_ID, parkingPlan);

        List<String> accountIds = getAccountIdWithArchivalPlanAndActive(false);

        String messagePattern = "%10s | %15s | %15s | %10s | %5s";

        StringBuilder result = new StringBuilder(String.format(messagePattern, "hasAbonement", "accountId", "oldPlan", "newPlan", "costDiff", "deactivatedDays"));

        System.out.println(String.format(messagePattern,"accountId", "oldPlan", "newPlan", "costDiff", "deactivatedDays"));

        accountIds.forEach(accountId -> {
            try {
                PersonalAccount account = accountManager.findByAccountId(accountId);
                Plan currentPlan = planManager.findOne(account.getPlanId());
                Plan newPlan = archivalToActive.getOrDefault(currentPlan.getOldId(), unlimitedPlan);

                accountManager.setPlanId(account.getId(), newPlan.getId());
                log.info("accountId " + accountId + " Установлен planId " + newPlan.getId());

                accountServiceHelper.deleteAccountServiceByServiceId(account, currentPlan.getServiceId());
                log.info("accountId " + accountId + " удалены сервисы старого тарифа с serviceId " + currentPlan.getServiceId());

                accountServiceHelper.addAccountService(account, newPlan.getServiceId());
                log.info("accountId " + accountId + " добавлен сервис нового тарифа с serviceId " + newPlan.getServiceId());

                accountHelper.addArchivalPlanAccountNoticeRepository(account, currentPlan);
                log.info("accountId " + accountId + " добавлен notice для архивного тарифа");

                history.save(account, "В связи с нахождением аккаунта в выключенном состоянии и прекращением " +
                        "поддержки архивный тариф " + currentPlan.getName() + " заменен на " + newPlan.getName());

                String message = String.format(messagePattern,
                        accountAbonementManager.findByPersonalAccountId(accountId) != null,
                        accountId,
                        currentPlan.getName(),
                        newPlan.getName(),
                        newPlan.getService().getCost().subtract(currentPlan.getService().getCost()),
                        account.getDeactivated() == null ? "null" : Utils.getDifferentInDaysBetweenDates(account.getDeactivated().toLocalDate(), LocalDate.now())
                );
                result.append(message + "\n");
            } catch (Throwable e) {
                log.error("accountId " + accountId + " Ошибка при смене тарифа : " + e.getMessage());
                e.printStackTrace();
            }

        });
        String message = result.toString();
        System.out.println(message);
        System.out.println("size: " + accountIds.size());

        return message;
    }

    //Обрабатываются только аккаунты с посуточными списаниями (без абонементов),
    // абонементы обработаются после их окончания
    public void processDeferredPlanChange() {
        DeferredPlanChangeNotice example = new DeferredPlanChangeNotice();
        example.setWasChanged(false);

        List<DeferredPlanChangeNotice> notices = accountNoticeRepository.findAll(Example.of(example));

        for (DeferredPlanChangeNotice notice : notices) {
            if (notice.getWillBeChangedAfter().isAfter(LocalDate.now())) {
                return;
            }

            PersonalAccount account = accountManager.findOne(notice.getPersonalAccountId());
            if (!accountHelper.needChangeArchivalPlanToFallbackPlan(account)) {
                log.info("account with id " + account.getId()
                        + " already change plan to active, passed deferred plan change");
                return;
            }

            AccountAbonement accountAbonement = accountAbonementManager.findByPersonalAccountId(account.getId());
            if (accountAbonement != null) {
                log.error("account with id " + account.getId() + " and archival plan has abonement");
                return;
            }

            Plan currentPlan = planManager.findOne(account.getPlanId());
            StringBuilder historyMessage = new StringBuilder(" текущий тариф: ").append(currentPlan.getName());

            Plan abonementFallbackPlan = abonementService.getArchivalFallbackPlan(currentPlan);
            if (abonementFallbackPlan == null) {
                historyMessage.append(", смена на активный тариф с посуточными списаниями");
                accountHelper.changeArchivalPlanToActive(account);
            } else {
                historyMessage.append(", попытка покупки абонемента по тарифу ").append(abonementFallbackPlan.getName());
                abonementService.changeArchivalAbonementToActive(account, abonementFallbackPlan);
            }

            history.save(account, historyMessage.toString());

            notice.setWasChanged(true);
            accountNoticeRepository.save(notice);
        }
    }
}
