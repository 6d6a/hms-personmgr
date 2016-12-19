package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.exception.LowBalanceException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.AccountStat;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountAbonementRepository;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.AccountStatRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

@Service
public class PlanChangeService {
    private final FinFeignClient finFeignClient;
    private final PlanRepository planRepository;
    private final AccountAbonementRepository accountAbonementRepository;
    private final AccountStatRepository accountStatRepository;
    private final AccountHistoryService accountHistoryService;
    private final AccountServiceRepository accountServiceRepository;
    private final PersonalAccountRepository personalAccountRepository;
    private final PlanCheckerService planCheckerService;

    @Autowired
    public PlanChangeService(
            FinFeignClient finFeignClient,
            PlanRepository planRepository,
            AccountAbonementRepository accountAbonementRepository,
            AccountStatRepository accountStatRepository,
            AccountHistoryService accountHistoryService,
            AccountServiceRepository accountServiceRepository,
            PersonalAccountRepository personalAccountRepository,
            PlanCheckerService planCheckerService
    ) {
        this.finFeignClient = finFeignClient;
        this.planRepository = planRepository;
        this.accountAbonementRepository = accountAbonementRepository;
        this.accountStatRepository = accountStatRepository;
        this.accountHistoryService = accountHistoryService;
        this.accountServiceRepository = accountServiceRepository;
        this.personalAccountRepository = personalAccountRepository;
        this.planCheckerService = planCheckerService;
    }

    /**
     * Изменение тарифного плана
     *
     * @param account   Аккаунт
     * @param newPlanId ID нового тарифа
     */
    public void changePlan(PersonalAccount account, String newPlanId) {

        String currentPlanId = account.getPlanId();

        Plan currentPlan = planRepository.findOne(currentPlanId);

        if (currentPlan == null) {
            throw new ResourceNotFoundException("Account plan not found");
        }

        Plan newPlan = planRepository.findByOldId(newPlanId);

        if (newPlan == null) {
            throw new ParameterValidationException("New plan with specified planId not found");
        }

        List<AccountAbonement> accountAbonements = accountAbonementRepository.findByPersonalAccountId(account.getId());

        if (accountAbonements != null && !accountAbonements.isEmpty()) {
            throw new ParameterValidationException("Account has abonement. Need to delete it first.");
        }

        //Проверим, можно ли менять тариф
        canChangePlan(account, currentPlan, newPlan);

        //Удалим старую услугу тарифа и добавим новую
        replacePlanService(account, currentPlan, newPlan);

        //Удалим старую услугу смс-уведомлений и добавим новую
        replaceSmsNotificationsService(account, currentPlan, newPlan);

        //Укажем новый тариф
        account.setPlanId(newPlan.getId());
        personalAccountRepository.save(account);

        //Сохраним статистику смены тарифа
        saveStat(account, newPlanId);

        //Сохраним историю аккаунта
        saveHistory(account, currentPlan, newPlan);
    }

    /**
     * Проверим было ли изменение тарифного плана за последний месяц
     *
     * @param account Аккаунт
     */
    private void checkLastMonthPlanChange(PersonalAccount account) {
        List<AccountStat> accountStats = accountStatRepository.findByPersonalAccountIdAndTypeAndCreatedAfter(
                account.getId(),
                AccountStatType.VIRTUAL_HOSTING_PLAN_CHANGE,
                LocalDateTime.now().minusMonths(1)
        );

        if (accountStats != null && !accountStats.isEmpty()) {
            throw new ParameterValidationException("Account plan already changed in last month.");
        }
    }

    /**
     * Проверим не отрицательный ли баланс
     *
     * @param account Аккаунт
     */
    private void checkBalance(PersonalAccount account) {
        BigDecimal available = getBalance(account);

        if (available.compareTo(BigDecimal.ZERO) < 0) {
            throw new LowBalanceException("Account balance is lower than zero.");
        }
    }

    /**
     * Получим баланс
     *
     * @param account Аккаунт
     */
    private BigDecimal getBalance(PersonalAccount account) {
        Map<String, Object> balance = finFeignClient.getBalance(account.getId());

        if (balance == null) {
            throw new ResourceNotFoundException("Account balance not found.");
        }

        return new BigDecimal((String) balance.get("available"));
    }

    /**
     * Сохраним в статистику об изменении тарифного плана
     *
     * @param account   Аккаунт
     * @param newPlanId ID нового тарифа
     */
    private void saveStat(PersonalAccount account, String newPlanId) {
        AccountStat accountStat = new AccountStat();
        accountStat.setPersonalAccountId(account.getId());
        accountStat.setCreated(LocalDateTime.now());
        accountStat.setType(AccountStatType.VIRTUAL_HOSTING_PLAN_CHANGE);

        Map<String, String> data = new HashMap<>();
        data.put("oldPlanId", account.getPlanId());
        data.put("newPlanId", newPlanId);

        accountStat.setData(data);

        accountStatRepository.save(accountStat);
    }

    /**
     * Сохраним в историю запись об изменении тарифного плана
     *
     * @param account     Аккаунт
     * @param currentPlan текущий тариф
     * @param newPlan     новый тариф
     */
    private void saveHistory(PersonalAccount account, Plan currentPlan, Plan newPlan) {
        accountHistoryService.addMessage(account.getId(), "Произведена смена тарифа с " + currentPlan.getName() + " на " + newPlan.getName(), "operator");
    }


    /**
     * Работа с услугами привязанными к аккаунту
     *
     * @param account     Аккаунт
     * @param currentPlan текущий тариф
     * @param newPlan     новый тариф
     */
    private void replacePlanService(PersonalAccount account, Plan currentPlan, Plan newPlan) {
        replaceAccountService(account, currentPlan.getServiceId(), newPlan.getServiceId());
    }

    /**
     * Может ли быть произведена смена тарифа
     *
     * @param account     Аккаунт
     * @param currentPlan текущий тариф
     * @param newPlan     новый тариф
     */
    private void canChangePlan(PersonalAccount account, Plan currentPlan, Plan newPlan) {
        //Проверим не менялся ли тариф в последний месяц
        checkLastMonthPlanChange(account);

        //Проверим баланс
        checkBalance(account);

        //Проверим возможность перехода с бизнес тарифа
        checkBusinessPlan(currentPlan, newPlan);

        //Проверим доступный баланс если тариф только с абонементом
        checkOnlyAbonementPlan(account, newPlan);

        //Проверим лимиты нового тарифа
        checkAccountLimits(account, newPlan);
    }

    /**
     * Может ли быть произведена смена тарифа (с Бизнес можно только на Бизнес)
     *
     * @param currentPlan текущий тариф
     * @param newPlan     новый тариф
     */
    private void checkBusinessPlan(Plan currentPlan, Plan newPlan) {
        VirtualHostingPlanProperties currentPlanProperties = (VirtualHostingPlanProperties) currentPlan.getPlanProperties();
        VirtualHostingPlanProperties newPlanProperties = (VirtualHostingPlanProperties) newPlan.getPlanProperties();
        if (currentPlanProperties.isBusinessServices() && !newPlanProperties.isBusinessServices()) {
            throw new ParameterValidationException("Account is on business plan. Change allowed only to business plans.");
        }
    }

    /**
     * Проверить наличие на счету средств если тариф только с абонементом
     *
     * @param account Аккаунт
     * @param newPlan новый тариф
     */
    private void checkOnlyAbonementPlan(PersonalAccount account, Plan newPlan) {
        if (newPlan.isAbonementOnly()) {
            BigDecimal available = getBalance(account);

            if (available.compareTo(newPlan.getAbonements().get(0).getService().getCost()) < 0) {
                throw new LowBalanceException("Account balance is too low for specified plan. Plan is abonementOnly.");
            }
        }
    }

    /**
     * Проверить счетчики аккаунта и соответствие их новому тарифу
     *
     * @param account Аккаунт
     * @param newPlan новый тариф
     */
    private void checkAccountLimits(PersonalAccount account, Plan newPlan) {
        //Database
        checkAccountDatabaseLimits(account, newPlan);

        //FtpUser
        checkAccountFtpUserLimits(account, newPlan);

        //WebSite
        checkAccountWebSiteLimits(account, newPlan);

        //Quota
        checkAccountQuotaLimits(account, newPlan);
    }

    /**
     * Проверить DB счетчики аккаунта и соответствие их новому тарифу
     *
     * @param account Аккаунт
     * @param newPlan новый тариф
     */
    private void checkAccountDatabaseLimits(PersonalAccount account, Plan newPlan) {
        if (planCheckerService.getCurrentDatabaseCount(account.getId()).compareTo(planCheckerService.getPlanDatabaseFreeLimit(newPlan)) > 0) {
            throw new ParameterValidationException("Account current DB count is more than plan limit.");
        }
    }

    /**
     * Проверить FtpUser счетчики аккаунта и соответствие их новому тарифу
     *
     * @param account Аккаунт
     * @param newPlan новый тариф
     */
    private void checkAccountFtpUserLimits(PersonalAccount account, Plan newPlan) {
        if (planCheckerService.getCurrentFtpUserCount(account.getId()).compareTo(planCheckerService.getPlanFtpUserFreeLimit(newPlan)) > 0) {
            throw new ParameterValidationException("Account current FtpUser count is more than plan limit.");
        }
    }

    /**
     * Проверить WebSite счетчики аккаунта и соответствие их новому тарифу
     *
     * @param account Аккаунт
     * @param newPlan новый тариф
     */
    private void checkAccountWebSiteLimits(PersonalAccount account, Plan newPlan) {
        if (planCheckerService.getCurrentWebSiteCount(account.getId()).compareTo(planCheckerService.getPlanWebSiteFreeLimit(newPlan)) > 0) {
            throw new ParameterValidationException("Account current WebSite count is more than plan limit.");
        }
    }

    /**
     * Проверить Quota счетчики аккаунта и соответствие их новому тарифу
     *
     * @param account Аккаунт
     * @param newPlan новый тариф
     */
    private void checkAccountQuotaLimits(PersonalAccount account, Plan newPlan) {
        if (planCheckerService.getCurrentQuotaUsed(account.getId()).compareTo(planCheckerService.getPlanQuotaKBFreeLimit(newPlan)) > 0) {
            throw new ParameterValidationException("Account current Quota is more than plan limit.");
        }
    }

    /**
     * Обновляем услугу смс-уведомлений (она могла быть на стором тарифе с другой стоимостью или бесплатной)
     *
     * @param account     Аккаунт
     * @param currentPlan текущий тариф
     * @param newPlan     новый тариф
     */
    private void replaceSmsNotificationsService(PersonalAccount account, Plan currentPlan, Plan newPlan) {
        replaceAccountService(account, currentPlan.getSmsServiceId(), newPlan.getSmsServiceId());
    }


    /**
     * Заменяем старую услугу на новую
     *
     * @param account   Аккаунт
     * @param oldServiceId id текущей услуги
     * @param newServiceId id новой услуги
     */
    private void replaceAccountService(PersonalAccount account, String oldServiceId, String newServiceId) {
        if (!oldServiceId.equals(newServiceId)) {
            AccountService accountService = accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), oldServiceId);

            if (accountService != null) {
                accountServiceRepository.delete(accountService);
            }

            //Создаем AccountService с выбранным тарифом
            AccountService service = new AccountService();
            service.setPersonalAccountId(account.getId());
            service.setServiceId(newServiceId);

            accountServiceRepository.save(service);
        }
    }
}
