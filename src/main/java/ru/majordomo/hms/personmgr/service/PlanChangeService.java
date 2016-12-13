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
     * @param account Аккаунт
     * @param newPlanId ID нового тарифа
     */
    public void changePlan(PersonalAccount account, String newPlanId) {

        String currentPlanId = account.getPlanId();

        Plan currentPlan = planRepository.findOne(currentPlanId);

        if(currentPlan == null){
            throw new ResourceNotFoundException("Account plan not found");
        }

        Plan newPlan = planRepository.findByOldId(newPlanId);

        if(newPlan == null){
            throw new ParameterValidationException("New plan with specified planId not found");
        }

        List<AccountAbonement> accountAbonements = accountAbonementRepository.findByPersonalAccountId(account.getId());

        if(accountAbonements != null && !accountAbonements.isEmpty()){
            throw new ParameterValidationException("Account has abonement. Need to delete it first.");
        }

        //Проверим, можно ли менять тариф
        canChangePlan(account, currentPlan, newPlan);

        //Удалим старую услугу и добавим новую
        processAccountServices(account, currentPlan, newPlan);

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

        if(accountStats != null && !accountStats.isEmpty()){
            throw new ParameterValidationException("Account plan already changed in last month.");
        }
    }

    /**
     * Проверим не отрицательный ли баланс
     *
     * @param account Аккаунт
     */
    private void checkBalance(PersonalAccount account) {
        Map<String, Object> balance = finFeignClient.getBalance(account.getId());

        if(balance == null){
            throw new ResourceNotFoundException("Account balance not found.");
        }

        BigDecimal available = new BigDecimal((String) balance.get("available"));

        if(available.compareTo(BigDecimal.ZERO) < 0){
            throw new LowBalanceException("Account balance is lower than zero.");
        }
    }

    /**
     * Сохраним в статистику об изменении тарифного плана
     *
     * @param account Аккаунт
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
     * @param account Аккаунт
     * @param currentPlan текущий тариф
     * @param newPlan новый тариф
     */
    private void saveHistory(PersonalAccount account, Plan currentPlan, Plan newPlan) {
        accountHistoryService.addMessage(account.getId(), "Произведена смена тарифа с " + currentPlan.getName() + " на " + newPlan.getName(), "operator");
    }


    /**
     * Работа с услугами привязанными к аккаунту
     *
     * @param account Аккаунт
     * @param currentPlan текущий тариф
     * @param newPlan новый тариф
     */
    private void processAccountServices(PersonalAccount account, Plan currentPlan, Plan newPlan) {
        AccountService accountService = accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), currentPlan.getServiceId());

        if (accountService != null) {
            accountServiceRepository.delete(accountService);
        }

        //Создаем AccountService с выбранным тарифом
        AccountService service = new AccountService();
        service.setPersonalAccountId(account.getId());
        service.setServiceId(newPlan.getServiceId());

        accountServiceRepository.save(service);
    }

    /**
     * Может ли быть произведена смена тарифа
     *
     * @param account Аккаунт
     * @param currentPlan текущий тариф
     * @param newPlan новый тариф
     */
    private void canChangePlan(PersonalAccount account, Plan currentPlan, Plan newPlan) {
        //Проверим не менялся ли тариф в последний месяц
        checkLastMonthPlanChange(account);

        //Проверим баланс
        checkBalance(account);
    }
}
