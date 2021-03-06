package ru.majordomo.hms.personmgr.service.PlanChange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.util.Pair;
import ru.majordomo.hms.personmgr.common.AccountSetting;
import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.ResourceType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.fin.PaymentRequest;
import ru.majordomo.hms.personmgr.event.account.AccountNotifyFinOnChangeAbonementEvent;
import ru.majordomo.hms.personmgr.event.account.AccountNotifySupportOnChangePlanEvent;
import ru.majordomo.hms.personmgr.event.account.AccountSetSettingEvent;
import ru.majordomo.hms.personmgr.exception.NotEnoughMoneyException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.feign.FinFeignClient;
import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.AccountStat;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.PlanChangeAgreement;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.repository.AccountStatRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.service.*;
import ru.majordomo.hms.personmgr.service.PlanChange.behavior.ReplaceAbonementAdviceBehavior;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.lang.Math.floor;
import static ru.majordomo.hms.personmgr.common.AccountSetting.CREDIT_ACTIVATION_DATE;
import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.Utils.planLimitsComparator;

public abstract class Processor {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected AbonementManager<AccountAbonement> accountAbonementManager;
    protected AccountHelper accountHelper;
    private AccountCountersService accountCountersService;
    private PlanLimitsService planLimitsService;
    private AccountStatRepository accountStatRepository;
    protected AccountServiceHelper accountServiceHelper;
    private PaymentServiceRepository paymentServiceRepository;
    private AccountQuotaService accountQuotaService;
    private PersonalAccountManager accountManager;
    private ApplicationEventPublisher publisher;
    private AccountHistoryManager history;
    private FinFeignClient finFeignClient;
    private PlanManager planManager;
    private ResourceNormalizer resourceNormalizer;
    private ResourceHelper resourceHelper;

    private DedicatedAppServiceHelper dedicatedAppServiceHelper;

    protected final PersonalAccount account;
    protected Plan currentPlan;
    protected final Plan newPlan;
    protected List<AccountAbonement> currentAccountAbonements;
    protected Boolean newAbonementRequired;
    private BigDecimal cashBackAmount;
    private PlanChangeAgreement requestPlanChangeAgreement;
    private String operator = "operator";
    protected Boolean ignoreRestricts = false;

    Processor(PersonalAccount account, Plan newPlan) {
        this.account = account;
        this.newPlan = newPlan;
    }

    void init(
            FinFeignClient finFeignClient,
            AbonementManager<AccountAbonement> accountAbonementManager,
            AccountStatRepository accountStatRepository,
            AccountHistoryManager history,
            PersonalAccountManager accountManager,
            PaymentServiceRepository paymentServiceRepository,
            AccountCountersService accountCountersService,
            PlanLimitsService planLimitsService,
            AccountQuotaService accountQuotaService,
            AccountServiceHelper accountServiceHelper,
            AccountHelper accountHelper,
            ApplicationEventPublisher publisher,
            PlanManager planManager,
            ResourceNormalizer resourceNormalizer,
            ResourceHelper resourceHelper,
            DedicatedAppServiceHelper dedicatedAppServiceHelper
    ) {
        this.finFeignClient = finFeignClient;
        this.accountAbonementManager = accountAbonementManager;
        this.accountStatRepository = accountStatRepository;
        this.history = history;
        this.accountManager = accountManager;
        this.paymentServiceRepository = paymentServiceRepository;
        this.accountCountersService = accountCountersService;
        this.planLimitsService = planLimitsService;
        this.accountQuotaService = accountQuotaService;
        this.accountServiceHelper = accountServiceHelper;
        this.accountHelper = accountHelper;
        this.publisher = publisher;
        this.planManager = planManager;
        this.resourceNormalizer = resourceNormalizer;
        this.resourceHelper = resourceHelper;
        this.dedicatedAppServiceHelper = dedicatedAppServiceHelper;
    }

    void postConstruct() {
        this.currentPlan = planManager.findOne(account.getPlanId());
        this.currentAccountAbonements = accountAbonementManager.findAllByPersonalAccountId(account.getId());
        this.cashBackAmount = calcCashBackAmount();
        this.newAbonementRequired = needToAddAbonement();
    }

    public void setRequestPlanChangeAgreement(PlanChangeAgreement requestPlanChangeAgreement) {
        this.requestPlanChangeAgreement = requestPlanChangeAgreement;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public BigDecimal getCashBackAmount() {
        return cashBackAmount;
    }

    public void setIgnoreRestricts(Boolean ignoreRestricts) {
        this.ignoreRestricts = ignoreRestricts;
    }

    //Methods
    abstract Boolean needToAddAbonement();

    abstract BigDecimal calcCashBackAmount();

    abstract void deleteServices();

    public void deleteNotAllowedService() {
        if (!newPlan.getAllowedFeature().contains(Feature.ALLOW_USE_DATABASES)
                && accountServiceHelper.hasAllowUseDbService(account)) {
            accountServiceHelper.deleteAccountServicesByFeature(account, Feature.ALLOW_USE_DATABASES, true);
        }
    }

    void replaceServices() {
        replaceSmsNotificationsService();
        processFtpUserService();
        processWebSiteService();
        deleteNotAllowedService();
        accountQuotaService.processQuotaService(account, newPlan);
    }

    abstract void addServices();

    void postProcess() {
        //Укажем новый тариф
        accountManager.setPlanId(account.getId(), newPlan.getId());

        //Проверим ресурсы на соответствие тарифу
        checkResources();

        //При необходимости отправляем письмо в саппорт
        supportNotification();

        //При необходимости отправляем письмо в фин-отдел
        finNotification();

        //Сохраним статистику смены тарифа
        saveStat();

        //Сохраним историю аккаунта
        saveHistory();
    }

    public PlanChangeAgreement getPlanChangeAgreement() {
        PlanChangeAgreement planChangeAgreement = new PlanChangeAgreement();

        if (account == null) {
            planChangeAgreement.addError("Аккаунт не найден");
            return planChangeAgreement;
        }

        if (currentPlan.getId().equals(newPlan.getId())) {
            planChangeAgreement.addError("Текущий план совпадает с выбранным");
            return planChangeAgreement;
        }

        if (!newPlan.isActive() && !ignoreRestricts) {
            planChangeAgreement.addError("На выбранный тарифный план переход невозможен");
            return planChangeAgreement;
        }

        if (account.isFreeze()) {
            planChangeAgreement.addError("Аккаунт заморожен");
            return planChangeAgreement;
        }

        BigDecimal balance = accountHelper.getBalance(account);
        planChangeAgreement.setBalance(balance);
        planChangeAgreement.setBalanceAfterOperation(balance);

        BigDecimal newBalanceAfterCashBack = balance.add(cashBackAmount);
        planChangeAgreement.setBalanceAfterOperation(newBalanceAfterCashBack);

        if (cashBackAmount.compareTo(BigDecimal.ZERO) != 0) {
            planChangeAgreement.setDelta(cashBackAmount);
            planChangeAgreement.setBalanceChanges(true);
        }

        Boolean hasFreeTestAbonement = hasOnlyFreeTestAbonement();
        // На бесплатном тестовом абонементе можно менять тариф туда сюда без ограничений
        if (!hasFreeTestAbonement) {
            if (!ignoreRestricts) {
                // С бизнеса можно только на бизнес
                if (!checkBusinessPlan(planChangeAgreement)) {
                    return planChangeAgreement;
                }

                // Если есть абонемент - можно перейти только на тариф большей ежемесячной стоимостью
                if (!checkPlanCostWithActiveAbonement(planChangeAgreement)) {
                    return planChangeAgreement;
                }

                // При отрицательном балансе нельзя менять тариф
                if (balance.compareTo(BigDecimal.ZERO) < 0) {
                    planChangeAgreement.addError("Баланс аккаунта отрицательный");
                    return planChangeAgreement;
                }

                // Проверим не менялся ли тариф в последний месяц
                if (!checkLastMonthPlanChange(planChangeAgreement)) {
                    return planChangeAgreement;
                }
            }
        }

        if (
            (hasFreeTestAbonement && newPlan.getFreeTrialAbonement() == null)
            ||
            (!hasFreeTestAbonement && newAbonementRequired)
        ) {
            planChangeAgreement.setBalanceChanges(true);

            List<Abonement> abonements = new ReplaceAbonementAdviceBehavior(
                    newPlan, currentAccountAbonements
            ).abonementsForReplace();

            if (!abonements.isEmpty()) {
                BigDecimal cost = abonements.stream().map(
                        a -> accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), a.getService())
                ).reduce(BigDecimal.ZERO, (a, c) -> c.add(a));

                if (newBalanceAfterCashBack.compareTo(cost) < 0) {
                    // Денег на новый абонемент не хватает
                    planChangeAgreement.setNeedToFeelBalance(
                            cost.subtract(newBalanceAfterCashBack)
                    );
                }

                planChangeAgreement.setBalanceAfterOperation(
                        newBalanceAfterCashBack.subtract(cost)
                );
            } else {
                //Пока тарифов без абонементов нет
                planChangeAgreement.addError(
                        "На новом тарифе не найден подходящий абонемент, для смены тарифа дождитесь окончания абонемента"
                );
                return planChangeAgreement;
            }
        }

        // Лимиты Database
        Boolean isDatabaseLimitsOk = checkAccountDatabaseLimits(planChangeAgreement);

        // Лимиты FtpUser
        Boolean isFtpUserLimitsOk = checkAccountFtpUserLimits(planChangeAgreement);

        // Лимиты WebSite
        Boolean isWebSiteLimitsOk = checkAccountWebSiteLimits(planChangeAgreement);

        Boolean isWebSitesOk = checkAccountWebSites(planChangeAgreement);

        Boolean isDedicatedAppServicesOk = checkAccountDedicatedAppServices(planChangeAgreement);

        // Лимиты Quota
        Boolean isQuotaLimitsOk = checkAccountQuotaLimits(planChangeAgreement);

        if (!isDatabaseLimitsOk || !isFtpUserLimitsOk || !isWebSiteLimitsOk || !isQuotaLimitsOk || !isWebSitesOk || !isDedicatedAppServicesOk) {
            return planChangeAgreement;
        }

        if (ignoreRestricts) {
            planChangeAgreement.setNeedToFeelBalance(BigDecimal.ZERO);
        }

        if (planChangeAgreement.getErrors().isEmpty()
                && planChangeAgreement.getNeedToFeelBalance().compareTo(BigDecimal.ZERO) <= 0) {
            planChangeAgreement.setPlanChangeAllowed(true);
        }
        return planChangeAgreement;
    }

    void preValidate() {
        if (account == null) {
            throw new ParameterValidationException("Аккаунт не найден");
        }

        if (!requestPlanChangeAgreement.equals(getPlanChangeAgreement())) {
            throw new ParameterValidationException("Произошла ошибка при смене тарифа");
        }

        if (!requestPlanChangeAgreement.isPlanChangeAllowed()) {
            throw new ParameterValidationException("Смена тарифа запрещена");
        }
    }

    final public void process() {
        preValidate();
        deleteServices();
        replaceServices();
        addServices();
        postProcess();
    }

    //Additions

    // ДЕНЬГИ

    void executeCashBackPayment(Boolean forceCharge) {
        //Начислить деньги
        if (cashBackAmount.compareTo(BigDecimal.ZERO) > 0) {
            try {
                finFeignClient.addPayment(
                        new PaymentRequest(account.getName())
                                .withAmount(cashBackAmount)
                                .withBonusType()
                                .withMessage("Возврат средств при отказе от абонемента")
                                .withDisableAsync(true)
                );
                history.addMessage(
                        account.getId(),
                        "Возврат средств при отказе от абонемента: " + cashBackAmount + " руб.",
                        operator);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("[PlanChangeProcessor] Exception in executeCashBackPayment: " + e.getMessage());
                throw new ParameterValidationException("Произошла ошибка при отказе от абонемента");
            }
        } else if (cashBackAmount.compareTo(BigDecimal.ZERO) < 0) {
            //Списать деньги

            ChargeMessage chargeMessage = new ChargeMessage.Builder(currentPlan.getService())
                    .setAmount(cashBackAmount.abs())
                    .setForceCharge(forceCharge)
                    .build();
            accountHelper.charge(account, chargeMessage);
        }
    }

    // ПРОВЕРКИ

    /**
     * Проверка является ли абонемент бесплатным тестовым и один ли он
     */
    Boolean hasOnlyFreeTestAbonement() {
        return currentAccountAbonements.size() == 1 && currentAccountAbonements.get(0).getAbonement().isTrial();
    }

    /**
     * Может ли быть произведена смена тарифа (с Бизнес можно только на Бизнес)
     * Предусмотрен переход "корпоративный -> партнерский -> (только!) корпоративный"
     */
    private Boolean checkBusinessPlan(PlanChangeAgreement planChangeAgreement) {
        VirtualHostingPlanProperties oldPlanProperties;
        VirtualHostingPlanProperties newPlanProperties = (VirtualHostingPlanProperties) newPlan.getPlanProperties();

        //Переход на партнерский тариф доступен с любого другого
        if (newPlan.isPartnerPlan()) {
            return true;
        }

        if (currentPlan.isPartnerPlan()) {  //Если переход с партнерского тарифа, то учесть предыдущий тариф, если есть
            Plan previousPlan = accountHelper.getPreviousPlan(account);
            if (previousPlan != null) {
                oldPlanProperties = (VirtualHostingPlanProperties) previousPlan.getPlanProperties();
            } else {
                return true;
            }
        } else {    //Просто обработать текущий и новый запрашиваемый тарифы
            oldPlanProperties = (VirtualHostingPlanProperties) currentPlan.getPlanProperties();
        }

        if (oldPlanProperties.isBusinessServices() && !newPlanProperties.isBusinessServices()) {
            planChangeAgreement.addError("Вы можете выбрать только другой корпоративный тарифный план");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Является ли это сменой одного абонемента на другой с владельцем юр-лицом или ИП
     */
    private boolean isCompanyChangeAbonementToAbonement() {
        AccountOwner accountOwner = accountHelper.getOwnerByPersonalAccountId(account.getId());

        return accountOwner != null &&
                (accountOwner.getType().equals(AccountOwner.Type.COMPANY)
                        || accountOwner.getType().equals(AccountOwner.Type.BUDGET_COMPANY))
                && !currentAccountAbonements.isEmpty()
                && newAbonementRequired;
    }

    /**
     * Является ли это переходом с обычного тарифа на бизнес
     */
    private boolean isFromRegularToBusiness() {
        VirtualHostingPlanProperties currentPlanProperties = (VirtualHostingPlanProperties) currentPlan.getPlanProperties();
        VirtualHostingPlanProperties newPlanProperties = (VirtualHostingPlanProperties) newPlan.getPlanProperties();
        return !currentPlanProperties.isBusinessServices() && newPlanProperties.isBusinessServices();
    }

    private Boolean checkPlanCostWithActiveAbonement(PlanChangeAgreement planChangeAgreement) {
        if (accountAbonementManager.existsByPersonalAccountId(account.getId())
                && newPlan.getService().getCost().compareTo(currentPlan.getService().getCost()) < 0) {
            planChangeAgreement.addError("Переход на тарифный план с меньшей стоимостью при активном абонементе невозможен");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Проверить DB счетчики аккаунта и соответствие их новому тарифу
     */
    private Boolean checkAccountDatabaseLimits(PlanChangeAgreement planChangeAgreement) {
        Long count = accountCountersService.getCurrentDatabaseCount(account.getId());
        Long freeLimit = planLimitsService.getDatabaseFreeLimit(newPlan);
        if (planLimitsComparator(count, freeLimit) > 0) {
            planChangeAgreement.addError("Текущее количество баз данных больше, чем лимит на новом тарифном плане. " +
                    "Текущее количество: " + count + " Лимит: " + freeLimit);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Проверить FtpUser счетчики аккаунта и соответствие их новому тарифу
     */
    private Boolean checkAccountFtpUserLimits(PlanChangeAgreement planChangeAgreement) {
        Long count = accountCountersService.getCurrentFtpUserCount(account.getId());
        Long planLimit = planLimitsService.getFtpUserLimit(newPlan);
        if (planLimitsComparator(count, planLimit) > 0) {
            planChangeAgreement.addError("Текущее количество FTP-пользователей больше, чем лимит на новом тарифном плане. "  +
                    "Текущее количество: " + count + " Лимит: " + planLimit);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Проверить WebSite счетчики аккаунта и соответствие их новому тарифу
     */
    private Boolean checkAccountWebSiteLimits(PlanChangeAgreement planChangeAgreement) {
        Long count = accountCountersService.getCurrentWebSiteCount(account.getId());
        Long freeLimit = planLimitsService.getWebsiteFreeLimit(newPlan);
        if (planLimitsComparator(count, freeLimit) > 0) {
            planChangeAgreement.addError("Текущее количество сайтов больше, чем лимит на новом тарифном плане. "  +
                    "Текущее количество: " + count + " Лимит: " + freeLimit);
            return false;
        } else {
            return true;
        }
    }
    
    private Boolean checkAccountDedicatedAppServices(PlanChangeAgreement planChangeAgreement) {
        int serviceCount = dedicatedAppServiceHelper.getServices(account.getId()).size();

        if (account.getProperties().getAllowDedicatedApps() != null && account.getProperties().getAllowDedicatedApps()) {
            return true;
        }

        if (newPlan.getProhibitedResourceTypes().contains(ResourceType.DEDICATED_APP_SERVICE) && serviceCount > 0) {
            planChangeAgreement.addError("На аккаунте заказаны выделенные сервисы приложений которые не поддерживаются на новом тарифе");
            return false;
        }
        return true;
    }

    /**
     * Проверить WebSite аккаунта на соответствие их новому тарифу
     */
    private Boolean checkAccountWebSites(PlanChangeAgreement planChangeAgreement) {
        try {
            resourceNormalizer.normalizeResources(account, ResourceType.WEB_SITE, newPlan, false);
        } catch (Exception e) {
            e.printStackTrace();
            planChangeAgreement.addError("Тариф не может быть изменен. Ошибка: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Проверить Quota счетчики аккаунта и соответствие их новому тарифу
     */
    private Boolean checkAccountQuotaLimits(PlanChangeAgreement planChangeAgreement) {
        Long count = accountCountersService.getCurrentQuotaUsed(account.getId());
        Long freeLimit = planLimitsService.getQuotaKBFreeLimit(newPlan);
        if (planLimitsComparator(count, freeLimit * 1024) > 0) {
            planChangeAgreement.addError("Использованная квота превышает лимит на новом тарифном плане. "  +
                    "Текущая квота: " + (count / 1048576) + " MB Лимит: " + (freeLimit / 1024) + " MB");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Проверим было ли изменение тарифного плана за последний месяц
     */
    private Boolean checkLastMonthPlanChange(PlanChangeAgreement planChangeAgreement) {
        List<AccountStat> accountStats = accountStatRepository.findByPersonalAccountIdAndTypeAndCreatedAfterOrderByCreatedDesc(
                account.getId(),
                AccountStatType.VIRTUAL_HOSTING_PLAN_CHANGE,
                LocalDateTime.now().minusMonths(1)
        );

        if (accountStats != null && !accountStats.isEmpty()) {
            if (currentPlan.getService().getCost().compareTo(newPlan.getService().getCost()) > 0) {

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

                planChangeAgreement.addError("Смена тарифного плана на меньший " +
                        "по стоимости возможна не чаще 1 раза в месяц. Последняя дата смены плана: " + accountStats.get(0).getCreated().format(formatter));
                return false;
            }
        }

        return true;
    }

    // РАБОТА С УСЛУГАМИ

    /**
     * Добавляем абонемент на аккаунт
     *
     * @param abonement новый абонемент
     */
    void addAccountAbonement(Abonement abonement, LocalDateTime expired) {
        AccountAbonement accountAbonement = new AccountAbonement();
        accountAbonement.setAbonementId(abonement.getId());
        accountAbonement.setPersonalAccountId(account.getId());
        accountAbonement.setCreated(LocalDateTime.now());
        accountAbonement.setExpired(expired);
        accountAbonement.setAutorenew(true);

        BigDecimal cost = accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), abonement.getService());
        accountAbonement.fillInBuyInformation(abonement, cost);

        accountAbonementManager.insert(accountAbonement);
    }

    private void addAccountAbonement(Abonement abonement) {
        addAccountAbonement(abonement, LocalDateTime.now().plus(Period.parse(abonement.getPeriod())));
    }

    private void addAccountAbonements(List<Abonement> abonements) {
        for (int i = 0; i < abonements.size(); i++) {
            if (i == 0) {
                addAccountAbonement(abonements.get(i));
            } else {
                addAccountAbonement(abonements.get(i), null);
            }
        }
    }

    void deleteAbonements() {
        accountAbonementManager.deleteByPersonalAccountId(account.getId());
    }

    /**
     * Работа с услугами привязанными к аккаунту
     */
    void addPlanService() {
        accountServiceHelper.addAccountService(account, newPlan.getServiceId());
    }

    void deletePlanService() {
        accountServiceHelper.deleteAccountServiceByServiceId(account, currentPlan.getServiceId());
    }

    void addServiceById(String serviceId) {
        accountServiceHelper.addAccountService(account, serviceId);
    }

    Boolean accountHasService(String serviceId) {
        return accountServiceHelper.accountHasService(account, serviceId);
    }

    /**
     * Обрабатываем услуги Доп.FTP в соответствии с новым тарифом
     */
    private void processFtpUserService() {
        Long currentFtpUserCount = accountCountersService.getCurrentFtpUserCount(account.getId());
        Long planFtpUserFreeLimit = planLimitsService.getFtpUserFreeLimit(newPlan);

        String ftpServiceId = paymentServiceRepository.findByOldId(ADDITIONAL_FTP_SERVICE_ID).getId();

        deleteOrAddAccountService(account, ftpServiceId, currentFtpUserCount, planFtpUserFreeLimit);
    }

    /**
     * Обрабатываем услуги Доп.Сайт в соответствии с новым тарифом
     */
    private void processWebSiteService() {
        Long currentWebSiteCount = accountCountersService.getCurrentWebSiteCount(account.getId());
        Long planWebSiteFreeLimit = planLimitsService.getWebsiteFreeLimit(newPlan);

        String webSiteServiceId = paymentServiceRepository.findByOldId(ADDITIONAL_WEB_SITE_SERVICE_ID).getId();

        deleteOrAddAccountService(account, webSiteServiceId, currentWebSiteCount, planWebSiteFreeLimit);
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
        if (planLimitsComparator(currentCount, planFreeLimit) <= 0) {
            accountServiceHelper.deleteAccountServiceByServiceId(account, serviceId);
        } else {
            int notFreeServiceCount = (int) floor(currentCount - planFreeLimit);
            accountServiceHelper.updateAccountService(account, serviceId, notFreeServiceCount);
        }
    }

    /**
     * Обновляем услугу смс-уведомлений (она могла быть на старом тарифе с другой стоимостью или бесплатной)
     */
    private void replaceSmsNotificationsService() {
        if (currentPlan.getSmsServiceId() != null && newPlan.getSmsServiceId() != null) {
            accountServiceHelper.replaceAccountService(account, currentPlan.getSmsServiceId(), newPlan.getSmsServiceId());
        }
    }


    //РАЗНОЕ

    /**
     * Сохраним в статистику об изменении тарифного плана
     */
    private void saveStat() {
        AccountStat accountStat = new AccountStat();
        accountStat.setPersonalAccountId(account.getId());
        accountStat.setCreated(LocalDateTime.now());
        accountStat.setType(AccountStatType.VIRTUAL_HOSTING_PLAN_CHANGE);

        Map<String, String> data = new HashMap<>();
        data.put("oldPlanId", currentPlan.getId());
        data.put("newPlanId", newPlan.getId());

        accountStat.setData(data);

        accountStatRepository.save(accountStat);
    }

    /**
     * Сохраним в историю запись об изменении тарифного плана
     */
    private void saveHistory() {
        history.addMessage(account.getId(), "Произведена смена тарифа с " +
                currentPlan.getName() + " на " + newPlan.getName(), operator);
    }

    private void supportNotification() {
        if (isFromRegularToBusiness()) {
            publisher.publishEvent(new AccountNotifySupportOnChangePlanEvent(account));
        }
    }

    private void finNotification() {
        if (isCompanyChangeAbonementToAbonement()) {
            publisher.publishEvent(new AccountNotifyFinOnChangeAbonementEvent(account));
        }
    }

    private void checkResources() {
        //Разрешены ли SSL-сертификаты на новом тарифе
        normalizeSslCertificate();

        //Разрешены ли почтовые ящики на новом тарифе
        normalizeMailbox();

        //Разрешены ли базы данных на новом тарифе
        normalizeDatabase();

        //Разрешены ли пользователи баз данных на новом тарифе
        normalizeDatabaseUser();

        //Разрешены ли установленные для сайтов serviceId на новом тарифе
        normalizeWebSite();

        //Разрешены ли домены на новом тарифе
        normalizeDomain();
    }

    private void normalizeDomain() {
        if (!newPlan.isDomainAllowed()) {
            resourceHelper.switchDomains(account, false);
            history.save(account, "Для аккаунта отключены домены в соответствии с тарифным планом", operator);
        } else {
            resourceHelper.switchDomains(account, true);
            history.save(account, "Для аккаунта включены домены в соответствии с тарифным планом", operator);
        }
    }

    private void normalizeMailbox() {
        if (!newPlan.isMailboxAllowed()) {
            resourceHelper.disableAndScheduleDeleteForAllMailboxes(account);
            history.save(account, "Для аккаунта выключены и запланированы на удаление почтовые ящики в соответствии с тарифным планом", operator);
        } else {
            resourceHelper.unScheduleDeleteForAllMailboxes(account);
            history.save(account, "Для аккаунта отменено запланированное удаление почтовых ящиков в соответствии с тарифным планом", operator);
        }
    }

    private void normalizeWebSite() {
        resourceNormalizer.normalizeResources(account, ResourceType.WEB_SITE, newPlan, true);
    }

    private void normalizeSslCertificate() {
        if (!newPlan.isSslCertificateAllowed()) {
            resourceHelper.switchCertificates(account, false);
            history.save(account, "Для аккаунта отключены SSL сертификаты в соответствии с тарифным планом", operator);
        }
    }

    private void normalizeDatabase() {
        if (!newPlan.isDatabaseAllowed()) {
            if (newPlan.getAllowedFeature().contains(Feature.ALLOW_USE_DATABASES)) {
                resourceHelper.switchDatabases(account, false);
            } else {
                resourceHelper.disableAndScheduleDeleteForAllDatabases(account);
                history.save(account.getId(), "Для аккаунта выключены и запланированы на удаление базы данных в соответствии с тарифным планом", operator);
            }
        }else {
            String message = resourceHelper.unScheduleDeleteForAllDatabases(account) ?
                    "Для аккаунта отменено запланированное удаление баз данных в соответствии с тарифным планом" :
                    "Базы данных включены в соответствии с тарифным планом";

            history.save(account, message, operator);
        }
    }

    private void normalizeDatabaseUser() {
        if (!newPlan.isDatabaseUserAllowed()) {
            if (newPlan.getAllowedFeature().contains(Feature.ALLOW_USE_DATABASES)) {
                resourceHelper.switchDatabaseUsers(account, false);
            } else {
                resourceHelper.disableAndScheduleDeleteForAllDatabaseUsers(account);
                history.save(account.getId(), "Для аккаунта выключены и запланированы на удаление пользователи баз данных в соответствии с тарифным планом", operator);
            }
        }else {
            if (resourceHelper.unScheduleDeleteForAllDatabaseUsers(account)) {
                history.save(account.getId(), "Для аккаунта отменено запланированное удаление пользователей баз данных в соответствии с тарифным планом", operator);
            } else {
                history.save(account.getId(), "Пользователи базы данных включены в соответствии с тарифным планом", operator);
            }
        }
    }

    void disableCredit() {
        if (account.isCredit()) {
            accountManager.removeSettingByName(account.getId(), CREDIT_ACTIVATION_DATE);
            accountManager.setCredit(account.getId(), false);
            history.save(account.getId(), "Для аккаунта отключен кредит в связи с переходом на тариф с обязательным абонементом", operator);
        }
    }

    /**
     * Абонемент должен быть, иначе сломалось бы на
     * @see Processor#getPlanChangeAgreement()
     */
    final void buyNotInternalAbonement() {
        List<Abonement> abonements = new ReplaceAbonementAdviceBehavior(
                newPlan, currentAccountAbonements
        ).abonementsForReplace();

        List<SimpleServiceMessage> chargeResults = new ArrayList<>();
        for (Abonement abonement : abonements) {
            ChargeMessage chargeMessage = new ChargeMessage.Builder(abonement.getService())
                    .setAmount(accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), abonement.getService()))
                    .setForceCharge(ignoreRestricts)
                    .build();

            try {
                chargeResults.add(
                        accountHelper.charge(account, chargeMessage)
                );
            } catch (Exception e) {
                logger.info("account {} on charge for {} catch e {} message {}",
                        account.getId(), chargeMessage.toString(), e.getClass(), e.getMessage()
                );

                for (SimpleServiceMessage chargeResult : chargeResults) {
                    try {
                        finFeignClient.unblock(account.getId(), (String) chargeResult.getParam("documentNubmer"));
                    } catch (Exception ignore) {}
                }

                throw e;
            }
        }

        addAccountAbonements(abonements);

        if (account.getSettings().get(AccountSetting.ABONEMENT_AUTO_RENEW) == null) {
            publisher.publishEvent(new AccountSetSettingEvent(account, AccountSetting.ABONEMENT_AUTO_RENEW, true));
        }
    }
}
