package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.manager.*;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.DeferredPlanChangeNotice;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountNoticeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.majordomo.hms.personmgr.common.Constants.*;


@Service
public class ArchivalPlanProcessor {
    private final static Logger log = LoggerFactory.getLogger(ArchivalPlanProcessor.class);
    private final int CHARGE_MONEY_AFTER_DAYS_INACTIVE = 180;
    private final static TemporalAdjuster MAX_PERIOD_ARCHIVAL_PLAN_MUST_BE_CHANGED =
            TemporalAdjusters.ofDateAdjuster(date -> date.plusMonths(3));

    private AccountNoticeRepository accountNoticeRepository;
    private PlanManager planManager;
    private PersonalAccountManager accountManager;
    private AccountHelper accountHelper;
    private AccountHistoryManager history;
    private AccountServiceHelper accountServiceHelper;
    private AbonementManager<AccountAbonement> accountAbonementManager;
    private AbonementService abonementService;

    @Autowired
    public ArchivalPlanProcessor(
            AccountNoticeRepository accountNoticeRepository,
            PlanManager planManager,
            PersonalAccountManager accountManager,
            AccountHelper accountHelper,
            AccountHistoryManager history,
            AccountServiceHelper accountServiceHelper,
            AbonementManager<AccountAbonement> accountAbonementManager,
            AbonementService abonementService
    ) {
        this.accountNoticeRepository = accountNoticeRepository;
        this.planManager = planManager;
        this.accountManager = accountManager;
        this.accountHelper = accountHelper;
        this.history = history;
        this.accountServiceHelper = accountServiceHelper;
        this.accountAbonementManager = accountAbonementManager;
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
        int minPeriod = 6;
        BigDecimal balance = accountHelper.getBalance(account);

        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            log.info(format(template, account.getId(), minPeriod, "баланс равен " + balance));
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
            log.info(format(template, account.getId(), maxPeriod, "дневное списание = 0"));
            return maxPeriod;
        }

        int remainingDays = balance.divide(dailyCost, 0, BigDecimal.ROUND_DOWN).intValue();

        if (remainingDays >= minPeriod && remainingDays <= maxPeriod) {
            log.info(format(template, account.getId(), remainingDays, "ok"));
            return remainingDays;
        } else {
            log.info(format(template, account.getId(), maxPeriod, "выходит за пределы перида для смены, days: " + remainingDays));
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
                .filter(plan ->
//                        !plan.isAbonementOnly()
//                        &&
                                !plan.isActive()
                        && plan.getService().getCost().compareTo(accountHelper.getArchivalFallbackPlan().getService().getCost()) < 0)
                .map(Plan::getId)
                .collect(Collectors.toList());
    }

    private List<String> getAccountIdWithArchivalPlanAndActive(boolean accountIsActive) {
        List<String> planIds = archivalTariffIds();
        return accountManager.findAccountIdsNotDeletedByPlanIdsInAndAccountIsActive(
                planIds, accountIsActive
        );
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
        archivalToActive.put(PLAN_PARKING_PLUS_ID_STRING, parkingPlan);

        List<String> accountIds = getAccountIdWithArchivalPlanAndActive(false);

        String messagePattern = "%10s | %15s | %15s | %10s | %5s";

        StringBuilder result = new StringBuilder(format(messagePattern, "hasAbonement", "accountId", "oldPlan", "newPlan", "costDiff", "deactivatedDays"));

        log.info(format(messagePattern,"accountId", "oldPlan", "newPlan", "costDiff", "deactivatedDays"));

        accountIds.forEach(accountId -> {
            try {
                PersonalAccount account = accountManager.findByAccountId(accountId);
                Plan currentPlan = planManager.findOne(account.getPlanId());
                Plan newPlan = archivalToActive.getOrDefault(currentPlan.getOldId(), unlimitedPlan);

                accountManager.setPlanId(account.getId(), newPlan.getId());
                log.info("accountId " + accountId + " Установлен planId " + newPlan.getId());

                accountServiceHelper.deleteAccountServiceByServiceId(account, currentPlan.getServiceId());
                log.info("accountId " + accountId + " удалены сервисы старого тарифа с serviceId " + currentPlan.getServiceId());

                if (!newPlan.isAbonementOnly()) {
                    accountServiceHelper.addAccountService(account, newPlan.getServiceId());
                    log.info("accountId " + accountId + " добавлен сервис нового тарифа с serviceId " + newPlan.getServiceId());
                }
                accountHelper.addArchivalPlanAccountNoticeRepository(account, currentPlan);
                log.info("accountId " + accountId + " добавлен notice для архивного тарифа");

                history.save(account, "В связи с нахождением аккаунта в выключенном состоянии и прекращением " +
                        "поддержки архивный тариф " + currentPlan.getName() + " заменен на " + newPlan.getName());

                String message = format(messagePattern,
                        accountAbonementManager.findByPersonalAccountId(accountId) != null,
                        accountId,
                        currentPlan.getName(),
                        newPlan.getName(),
                        newPlan.getService().getCost().subtract(currentPlan.getService().getCost()),
                        account.getDeactivated() == null ? "null" : Utils.differenceInDays(account.getDeactivated().toLocalDate(), LocalDate.now())
                );
                result.append(message).append("\n");
            } catch (Throwable e) {
                log.error("accountId " + accountId + " Ошибка при смене тарифа : " + e.getMessage());
                e.printStackTrace();
            }

        });
        String message = result.toString();
        log.info(message);
        log.info("size: " + accountIds.size());

        return message;
    }

    //Обрабатываются только аккаунты с посуточными списаниями (без абонементов),
    // абонементы обработаются после их окончания
    public void processDeferredPlanChange() {
        DeferredPlanChangeNotice example = new DeferredPlanChangeNotice();
        example.setWasChanged(false);

        accountNoticeRepository
                .findAll(Example.of(example))
                .stream()
                .filter(n -> n.getWillBeChangedAfter().isBefore(LocalDate.now()))
                .forEach(this::processDeferredPlanChange);
    }

    private void processDeferredPlanChange(DeferredPlanChangeNotice notice) {
        PersonalAccount account = accountManager.findOne(notice.getPersonalAccountId());

        if (!accountHelper.needChangeArchivalPlanToFallbackPlan(account)) {
            history.save(notice.getPersonalAccountId(),
                    "Смена архивного тарифа на активный не потребовалась", "service");

            log.info("account with id " + account.getId()
                    + " already change plan to active, passed deferred plan change");
            notice.setWasChanged(true);
            accountNoticeRepository.save(notice);
        } else if(accountAbonementManager.findByPersonalAccountId(account.getId()) != null) {
            log.error("account with id " + account.getId() + " and archival plan has abonement");
        } else {
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

    public void processChargeRemainderForInactiveLongTime() {
        PaymentService accessToTheControlPanelService = accountServiceHelper.getAccessToTheControlPanelService();

        if (accessToTheControlPanelService == null) {
            throw new InternalApiException("Услуга для списания с неактивных аккаунтов не найден");
        }

        BigDecimal fullCost = accessToTheControlPanelService.getCost();

        List<String> accountIds = accountManager.findByActiveAndDeactivatedBefore(
                false, LocalDateTime.now().minusDays(CHARGE_MONEY_AFTER_DAYS_INACTIVE)
        );

        for (String accountId: accountIds) {
            PersonalAccount account = accountManager.findOne(accountId);
            BigDecimal balance;
            try {
                balance = accountHelper.getBalance(account);
            } catch (Exception e) {
                log.error("не удалось получить баланс для списания с неактивных аккаунтов, accountId: "+ accountId + " message: " + e.getMessage());
                continue;
            }

            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal amount = balance.compareTo(fullCost) > 0 ? fullCost : balance;
                try {
                    accountHelper.charge(
                            account,
                            ChargeMessage.builder(accessToTheControlPanelService)
                                    .setAmount(amount)
                                    .build()
                    );
                    history.save(
                            account,
                            "Аккаунт неактивен более " + CHARGE_MONEY_AFTER_DAYS_INACTIVE
                                    + " дней. Списано " + amount + " руб."
                    );
                } catch (Exception ignore) {
                    log.error("Ошибка при списании с неактивного аккаунта с id " + accountId + " message: " + ignore.getMessage());
                }
            }
        }
    }
}
