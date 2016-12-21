package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.exception.LowBalanceException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.AccountStat;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountAbonementRepository;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.AccountStatRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

import static java.lang.Math.floor;
import static java.time.temporal.ChronoUnit.DAYS;
import static ru.majordomo.hms.personmgr.common.Constants.ADDITIONAL_FTP_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.Constants.ADDITIONAL_QUOTA_100_CAPACITY;
import static ru.majordomo.hms.personmgr.common.Constants.ADDITIONAL_QUOTA_100_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.Constants.ADDITIONAL_WEB_SITE_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.Constants.BONUS_PAYMENT_TYPE_ID;

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
    private final PaymentServiceRepository paymentServiceRepository;

    @Autowired
    public PlanChangeService(
            FinFeignClient finFeignClient,
            PlanRepository planRepository,
            AccountAbonementRepository accountAbonementRepository,
            AccountStatRepository accountStatRepository,
            AccountHistoryService accountHistoryService,
            AccountServiceRepository accountServiceRepository,
            PersonalAccountRepository personalAccountRepository,
            PlanCheckerService planCheckerService,
            PaymentServiceRepository paymentServiceRepository
    ) {
        this.finFeignClient = finFeignClient;
        this.planRepository = planRepository;
        this.accountAbonementRepository = accountAbonementRepository;
        this.accountStatRepository = accountStatRepository;
        this.accountHistoryService = accountHistoryService;
        this.accountServiceRepository = accountServiceRepository;
        this.personalAccountRepository = personalAccountRepository;
        this.planCheckerService = planCheckerService;
        this.paymentServiceRepository = paymentServiceRepository;
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

        Plan newPlan = planRepository.findOne(newPlanId);

        if (newPlan == null) {
            throw new ParameterValidationException("New plan with specified planId not found");
        }

        List<AccountAbonement> accountAbonements = accountAbonementRepository.findByPersonalAccountId(account.getId());

        if (accountAbonements != null && !accountAbonements.isEmpty()) {
            throw new ParameterValidationException("Account has abonement. Need to delete it first.");
        }

        //Проверим, можно ли менять тариф
        canChangePlan(account, currentPlan, newPlan);

        //Произведем нужные действия со всеми услугами
        processServices(account, currentPlan, newPlan);

        //Укажем новый тариф
        account.setPlanId(newPlan.getId());
        personalAccountRepository.save(account);

        //Произведем нужные действия с абонементами
        processAbonements(account, currentPlan, newPlan);

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
            throw new LowBalanceException("Account balance is lower than zero. balance is: "
                    + available.toPlainString());
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

        return BigDecimal.valueOf((double) balance.get("available"));
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
     * Произведем нужные действия с абонементами
     *
     * @param account     Аккаунт
     * @param currentPlan текущий тариф
     * @param newPlan     новый тариф
     */
    private void processAbonements(PersonalAccount account, Plan currentPlan, Plan newPlan) {
        //Если старый тариф был только абонементным, то нужно удалить абонемент и вернуть неизрасходованные средства
        processCurrentAccountAbonement(account, currentPlan);

        //Если новый тариф только абонементный, то нужно сразу купить абонемент и списать средства
        processNewAccountAbonement(account, newPlan);
    }

    /**
     * Добавляем при необходимости абонемент на тариф
     *
     * @param account   Аккаунт
     * @param currentPlan текущий тариф
     */
    private void processCurrentAccountAbonement(PersonalAccount account, Plan currentPlan) {
        if (currentPlan.isAbonementOnly()) {
            addRemainingAccountAbonementCost(account, currentPlan);

            deleteAccountAbonement(account, currentPlan);
        }
    }

    /**
     * Возвращаем неизрасходованные средства за старый абонемент
     *
     * @param account   Аккаунт
     * @param currentPlan текущий тариф
     */
    private void addRemainingAccountAbonementCost(PersonalAccount account, Plan currentPlan) {
        List<AccountAbonement> accountAbonements = accountAbonementRepository.findByPersonalAccountIdAndAbonementId(
                account.getId(),
                currentPlan.getAbonementIds().get(0)
        );

        if (accountAbonements != null && !accountAbonements.isEmpty()) {
            AccountAbonement accountAbonement = accountAbonements.get(0);
            Abonement abonement = accountAbonement.getAbonement();

            if (accountAbonement.getExpired().isAfter(LocalDateTime.now())) {
                long remainingDays = DAYS.between(accountAbonement.getExpired(), LocalDateTime.now());
                BigDecimal remainedServiceCost = (BigDecimal.valueOf(remainingDays)).multiply(abonement.getService().getCost().divide(BigDecimal.valueOf(365L), 2, BigDecimal.ROUND_DOWN));

                Map<String, Object> payment = new HashMap<>();
                payment.put("accountId", account.getName());
                payment.put("paymentTypeId", BONUS_PAYMENT_TYPE_ID);
                payment.put("amount", remainedServiceCost);
                payment.put("documentNumber", "N/A");
                payment.put("message", "Возврат неиспользованных средств при отказе от абонемента");

                finFeignClient.addPayment(payment);
            }
        }
    }

    /**
     * Удаляем старый абонемент
     *
     * @param account   Аккаунт
     * @param currentPlan текущий тариф
     */
    private void deleteAccountAbonement(PersonalAccount account, Plan currentPlan) {
        List<AccountAbonement> accountAbonements = accountAbonementRepository.findByPersonalAccountIdAndAbonementId(
                account.getId(),
                currentPlan.getAbonementIds().get(0)
        );

        if (accountAbonements != null && !accountAbonements.isEmpty()) {
            accountAbonementRepository.delete(accountAbonements);
        }
    }

    /**
     * Добавляем при необходимости абонемент на тариф
     *
     * @param account   Аккаунт
     * @param newPlan новый тариф
     */
    private void processNewAccountAbonement(PersonalAccount account, Plan newPlan) {
        if (newPlan.isAbonementOnly()) {
            Abonement abonement = newPlan.getAbonements().get(0);
            addAccountAbonement(account, abonement);

            Map<String, Object> paymentOperation = new HashMap<>();
            paymentOperation.put("serviceId", abonement.getServiceId());
            paymentOperation.put("amount", abonement.getService().getCost());

            Map<String, Object> response = finFeignClient.charge(account.getId(), paymentOperation);

            if (response.get("success") != null && !((boolean) response.get("success"))) {
                throw new LowBalanceException("Could not charge money");
            }
        }
    }

    /**
     * Добавляем абонемент на тариф
     *
     * @param account   Аккаунт
     * @param abonement новый абонемент
     */
    private void addAccountAbonement(PersonalAccount account, Abonement abonement) {
        AccountAbonement accountAbonement = new AccountAbonement();
        accountAbonement.setAbonementId(abonement.getId());
        accountAbonement.setPersonalAccountId(account.getId());
        accountAbonement.setCreated(LocalDateTime.now());
        accountAbonement.setExpired(LocalDateTime.now().plus(Period.parse(abonement.getPeriod())));
        accountAbonement.setAutorenew(false);

        accountAbonementRepository.save(accountAbonement);
    }

    /**
     * Произвести все действия с услугами
     *
     * @param account     Аккаунт
     * @param currentPlan текущий тариф
     * @param newPlan     новый тариф
     */
    private void processServices(PersonalAccount account, Plan currentPlan, Plan newPlan) {
        //Удалим старую услугу тарифа и добавим новую
        replacePlanService(account, currentPlan, newPlan);

        //Удалим старую услугу смс-уведомлений и добавим новую
        replaceSmsNotificationsService(account, currentPlan, newPlan);

        //Обработаем услуги Доп.FTP
        processFtpUserService(account, newPlan);

        //Обработаем услуги Доп.Сайт
        processWebSiteService(account, newPlan);

        //Обработаем услуги Доп.место
        processQuotaService(account, newPlan);
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
                throw new LowBalanceException("Account balance is too low for specified plan. Plan is abonementOnly. " +
                        "Current balance is: " + available.toPlainString());
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
        Long count = planCheckerService.getCurrentDatabaseCount(account.getId());
        Long freeLimit = planCheckerService.getPlanDatabaseFreeLimit(newPlan);
        if (freeLimit.compareTo(-1L) != 0 && count.compareTo(freeLimit) > 0) {
            throw new ParameterValidationException("Account current DB count is more than plan freeLimit. " +
                    "Current: " + count + " FreeLimit: " + freeLimit);
        }
    }

    /**
     * Проверить FtpUser счетчики аккаунта и соответствие их новому тарифу
     *
     * @param account Аккаунт
     * @param newPlan новый тариф
     */
    private void checkAccountFtpUserLimits(PersonalAccount account, Plan newPlan) {
        Long count = planCheckerService.getCurrentFtpUserCount(account.getId());
        Long freeLimit = planCheckerService.getPlanFtpUserFreeLimit(newPlan);
        if (freeLimit.compareTo(-1L) != 0 && count.compareTo(freeLimit) > 0) {
            throw new ParameterValidationException("Account current FtpUser count is more than plan freeLimit. "  +
                    "Current: " + count + " FreeLimit: " + freeLimit);
        }
    }

    /**
     * Проверить WebSite счетчики аккаунта и соответствие их новому тарифу
     *
     * @param account Аккаунт
     * @param newPlan новый тариф
     */
    private void checkAccountWebSiteLimits(PersonalAccount account, Plan newPlan) {
        Long count = planCheckerService.getCurrentWebSiteCount(account.getId());
        Long freeLimit = planCheckerService.getPlanWebSiteFreeLimit(newPlan);
        if (freeLimit.compareTo(-1L) != 0 && count.compareTo(freeLimit) > 0) {
            throw new ParameterValidationException("Account current WebSite count is more than plan limit. "  +
                    "Current: " + count + " FreeLimit: " + freeLimit);
        }
    }

    /**
     * Проверить Quota счетчики аккаунта и соответствие их новому тарифу
     *
     * @param account Аккаунт
     * @param newPlan новый тариф
     */
    private void checkAccountQuotaLimits(PersonalAccount account, Plan newPlan) {
        Long count = planCheckerService.getCurrentQuotaUsed(account.getId());
        Long freeLimit = planCheckerService.getPlanQuotaKBFreeLimit(newPlan);
        if (freeLimit.compareTo(-1L) != 0 && count.compareTo(freeLimit) > 0) {
            throw new ParameterValidationException("Account current Quota is more than plan limit. "  +
                    "Current: " + count + " FreeLimit: " + freeLimit);
        }
    }

    /**
     * Обновляем услугу смс-уведомлений (она могла быть на старом тарифе с другой стоимостью или бесплатной)
     *
     * @param account     Аккаунт
     * @param currentPlan текущий тариф
     * @param newPlan     новый тариф
     */
    private void replaceSmsNotificationsService(PersonalAccount account, Plan currentPlan, Plan newPlan) {
        if (currentPlan.getSmsServiceId() != null && newPlan.getSmsServiceId() != null) {
            replaceAccountService(account, currentPlan.getSmsServiceId(), newPlan.getSmsServiceId());
        }
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
            deleteAccountService(account, oldServiceId);

            addAccountService(account, newServiceId);
        }
    }

    /**
     * Удаляем старую услугу
     *
     * @param account   Аккаунт
     * @param oldServiceId id текущей услуги
     */
    private void deleteAccountService(PersonalAccount account, String oldServiceId) {
        List<AccountService> accountService = accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), oldServiceId);

        if (accountService != null && !accountService.isEmpty()) {
            accountServiceRepository.delete(accountService);
        }
    }

    /**
     * Добавляем новую услугу
     *
     * @param account   Аккаунт
     * @param newServiceId id новой услуги
     */
    private void addAccountService(PersonalAccount account, String newServiceId) {
        AccountService service = new AccountService();
        service.setPersonalAccountId(account.getId());
        service.setServiceId(newServiceId);

        accountServiceRepository.save(service);
    }

    /**
     * Удаляем или добавляем услуги в зависимости от счетчиков
     *
     * @param account   Аккаунт
     * @param serviceId id услуги
     * @param currentCount текущее кол-во услуг
     * @param planFreeLimit бесплатно по тарифу
     */
    private void deleteOrAddAccountService(PersonalAccount account, String serviceId, Long currentCount, Long planFreeLimit) {
        if (currentCount.compareTo(planFreeLimit) <= 0) {
            deleteAccountService(account, serviceId);
        } else {
            Long notFreeServiceCount = currentCount - planFreeLimit;
            for (int i = 1 ; i <= notFreeServiceCount.intValue(); i++) {
                addAccountService(account, serviceId);
            }
        }
    }

    /**
     * Удаляем или добавляем услуги в зависимости от счетчиков
     *
     * @param account   Аккаунт
     * @param serviceId id услуги
     * @param currentCount текущее кол-во услуг
     * @param planFreeLimit бесплатно по тарифу
     */
    private void deleteOrAddAccountService(PersonalAccount account, String serviceId, Long currentCount, Long planFreeLimit, Long oneServiceCapacity) {
        if (currentCount.compareTo(planFreeLimit) <= 0) {
            deleteAccountService(account, serviceId);
        } else {
            int notFreeQuotaCount = 1 + (int) floor((currentCount.intValue() - planFreeLimit.intValue()) / oneServiceCapacity);
            for (int i = 1 ; i <= notFreeQuotaCount; i++) {
                addAccountService(account, serviceId);
            }
        }
    }

    /**
     * Обрабатываем услуги Доп.FTP в соответствии с новым тарифом
     *
     * @param account     Аккаунт
     * @param newPlan     новый тариф
     */
    private void processFtpUserService(PersonalAccount account, Plan newPlan) {
        Long currentFtpUserCount = planCheckerService.getCurrentFtpUserCount(account.getId());
        Long planFtpUserFreeLimit = planCheckerService.getPlanFtpUserFreeLimit(newPlan);

        String ftpServiceId = paymentServiceRepository.findByOldId(ADDITIONAL_FTP_SERVICE_ID).getId();

        deleteOrAddAccountService(account, ftpServiceId, currentFtpUserCount, planFtpUserFreeLimit);
    }

    /**
     * Обрабатываем услуги Доп.Сайт в соответствии с новым тарифом
     *
     * @param account     Аккаунт
     * @param newPlan     новый тариф
     */
    private void processWebSiteService(PersonalAccount account, Plan newPlan) {
        Long currentWebSiteCount = planCheckerService.getCurrentWebSiteCount(account.getId());
        Long planWebSiteFreeLimit = planCheckerService.getPlanWebSiteFreeLimit(newPlan);

        String webSiteServiceId = paymentServiceRepository.findByOldId(ADDITIONAL_WEB_SITE_SERVICE_ID).getId();

        deleteOrAddAccountService(account, webSiteServiceId, currentWebSiteCount, planWebSiteFreeLimit);
    }

    /**
     * Обрабатываем услуги Доп.место в соответствии с новым тарифом
     *
     * @param account     Аккаунт
     * @param newPlan     новый тариф
     */
    private void processQuotaService(PersonalAccount account, Plan newPlan) {
        Long currentQuotaUsed = planCheckerService.getCurrentQuotaUsed(account.getId());
        Long planQuotaKBFreeLimit = planCheckerService.getPlanQuotaKBFreeLimit(newPlan);

        String webSiteServiceId = paymentServiceRepository.findByOldId(ADDITIONAL_QUOTA_100_SERVICE_ID).getId();

        deleteOrAddAccountService(account, webSiteServiceId, currentQuotaUsed, planQuotaKBFreeLimit, ADDITIONAL_QUOTA_100_CAPACITY);
    }
}
