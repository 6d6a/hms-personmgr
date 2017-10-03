package ru.majordomo.hms.personmgr.service.PlanChange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.event.account.AccountNotifySupportOnChangePlanEvent;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountAbonementManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.AccountStat;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.PlanChangeAgreement;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.repository.AccountStatRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.floor;
import static ru.majordomo.hms.personmgr.common.AccountSetting.CREDIT_ACTIVATION_DATE;
import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;
import static ru.majordomo.hms.personmgr.common.Utils.planChangeComparator;

public abstract class Processor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private AccountAbonementManager accountAbonementManager;
    private AccountHelper accountHelper;
    private AccountCountersService accountCountersService;
    private PlanLimitsService planLimitsService;
    private AccountStatRepository accountStatRepository;
    private AccountServiceHelper accountServiceHelper;
    private PaymentServiceRepository paymentServiceRepository;
    private AccountQuotaService accountQuotaService;
    private PersonalAccountManager accountManager;
    private ApplicationEventPublisher publisher;
    private AccountHistoryService accountHistoryService;
    private FinFeignClient finFeignClient;
    private PlanRepository planRepository;

    private PersonalAccount account;
    private Plan currentPlan;
    private Plan newPlan;
    private Boolean newAbonementRequired;
    private BigDecimal cashBackAmount;
    private PlanChangeAgreement requestPlanChangeAgreement;
    private String operator = "operator";

    Processor(PersonalAccount account, Plan newPlan) {
        this.account = account;
        this.newPlan = newPlan;
    }

    void postConstruct() {
        this.currentPlan = planRepository.findOne(account.getPlanId());
        this.cashBackAmount = calcCashBackAmount();
        this.newAbonementRequired = needToAddAbonement();
    }

    //Services

    void setPlanRepository(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    void setFinFeignClient(FinFeignClient finFeignClient) {
        this.finFeignClient = finFeignClient;
    }

    void setAccountHistoryService(AccountHistoryService accountHistoryService) {
        this.accountHistoryService = accountHistoryService;
    }

    void setPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    void setAccountManager(PersonalAccountManager accountManager) {
        this.accountManager = accountManager;
    }

    void setAccountQuotaService(AccountQuotaService accountQuotaService) {
        this.accountQuotaService = accountQuotaService;
    }

    void setAccountServiceHelper(AccountServiceHelper accountServiceHelper) {
        this.accountServiceHelper = accountServiceHelper;
    }

    void setPaymentServiceRepository(PaymentServiceRepository paymentServiceRepository) {
        this.paymentServiceRepository = paymentServiceRepository;
    }

    AccountAbonementManager getAccountAbonementManager() {
        return accountAbonementManager;
    }

    void setAccountAbonementManager(AccountAbonementManager accountAbonementManager) {
        this.accountAbonementManager = accountAbonementManager;
    }

    AccountHelper getAccountHelper() {
        return accountHelper;
    }

    void setAccountHelper(AccountHelper accountHelper) {
        this.accountHelper = accountHelper;
    }

    void setAccountCountersService(AccountCountersService accountCountersService) {
        this.accountCountersService = accountCountersService;
    }

    void setPlanLimitsService(PlanLimitsService planLimitsService) {
        this.planLimitsService = planLimitsService;
    }

    void setAccountStatRepository(AccountStatRepository accountStatRepository) {
        this.accountStatRepository = accountStatRepository;
    }

    //Getters and setters
    PersonalAccount getAccount() {
        return account;
    }

    Plan getCurrentPlan() {
        return currentPlan;
    }

    Plan getNewPlan() {
        return newPlan;
    }

    Boolean getNewAbonementRequired() {
        return newAbonementRequired;
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

    //Methods
    abstract Boolean needToAddAbonement();

    abstract BigDecimal calcCashBackAmount();

    abstract void deleteServices();

    void replaceServices() {

        replaceSmsNotificationsService();
        processFtpUserService();
        processWebSiteService();
        accountQuotaService.processQuotaService(account, newPlan);
    }

    abstract void addServices();

    void postProcess() {

        //Укажем новый тариф
        accountManager.setPlanId(getAccount().getId(), getNewPlan().getId());

        //Разрешён ли сертификат на новом тарифе
        sslCertAllowed();

        //При необходимости отправляем письмо в саппорт
        supportNotification();

        //Сохраним статистику смены тарифа
        saveStat();

        //Сохраним историю аккаунта
        saveHistory();
    }

    public PlanChangeAgreement isPlanChangeAllowed() {

        PlanChangeAgreement planChangeAgreement = new PlanChangeAgreement();

        if (account == null) {
            planChangeAgreement.addError("Аккаунт не найден");
            return planChangeAgreement;
        }

        if (currentPlan.getId().equals(newPlan.getId())) {
            planChangeAgreement.addError("Текущий план совпадает с выбранным");
            return planChangeAgreement;
        }

        if (!newPlan.isActive()) {
            planChangeAgreement.addError("На выбранный тарифный план переход невозможен");
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

        // На бесплатном тестовом абонементе можно менять тариф туда сюда без ограничений
        if (!hasFreeTestAbonement(accountAbonementManager.findByPersonalAccountId(account.getId()))) {

            // С бизнеса можно только на бизнес
            if (!checkBusinessPlan(planChangeAgreement)) {
                return planChangeAgreement;
            }

            // Если есть абонемент - можно перейти только на тариф большей ежемесячной стоимостью
            if (!checkPlanCostWithActiveAbonement(planChangeAgreement)) {
                return planChangeAgreement;
            }

            if (!checkBonusAbonements(planChangeAgreement)) {
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

            if (newAbonementRequired) {

                planChangeAgreement.setBalanceChanges(true);

                if (newBalanceAfterCashBack.compareTo(newPlan.getNotInternalAbonement().getService().getCost()) < 0) {
                    // Денег на новый абонемент не хватает
                    planChangeAgreement.setNeedToFeelBalance(
                            newPlan.getNotInternalAbonement().getService().getCost().subtract(newBalanceAfterCashBack)
                    );
                }

                planChangeAgreement.setBalanceAfterOperation(
                        newBalanceAfterCashBack.subtract(newPlan.getNotInternalAbonement().getService().getCost())
                );
            }
        }

        // Лимиты Database
        Boolean isDatabaseLimitsOk = checkAccountDatabaseLimits(planChangeAgreement);

        // Лимиты FtpUser
        Boolean isFtpUserLimitsOk = checkAccountFtpUserLimits(planChangeAgreement);

        // Лимиты WebSite
        Boolean isWebSiteLimitsOk = checkAccountWebSiteLimits(planChangeAgreement);

        // Лимиты Quota
        Boolean isQuotaLimitsOk = checkAccountQuotaLimits(planChangeAgreement);

        if (!isDatabaseLimitsOk || !isFtpUserLimitsOk || !isWebSiteLimitsOk || !isQuotaLimitsOk) {
            return planChangeAgreement;
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

        if (!requestPlanChangeAgreement.equals(isPlanChangeAllowed())) {
            throw new ParameterValidationException("Произошла ошибка при смене тарифа");
        }

        if (!requestPlanChangeAgreement.getPlanChangeAllowed()) {
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
            Map<String, Object> payment = new HashMap<>();
            payment.put("accountId", account.getName());
            payment.put("paymentTypeId", BONUS_PAYMENT_TYPE_ID);
            payment.put("amount", cashBackAmount);
            payment.put("message", "Возврат средств при отказе от абонемента");
            payment.put("disableAsync", true);

            try {
                finFeignClient.addPayment(payment);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("[PlanChangeProcessor] Exception in executeCashBackPayment: " + e.getMessage());
                throw new ParameterValidationException("Произошла ошибка при отказе от абонемента");
            }
        } else if (cashBackAmount.compareTo(BigDecimal.ZERO) < 0) {
            //Списать деньги
            accountHelper.charge(account, currentPlan.getService(), cashBackAmount.abs(), forceCharge, false);
        }
    }

    // ПРОВЕРКИ

    /**
     * Проверка наличия бонусных абонементов
     */
    private Boolean checkBonusAbonements(PlanChangeAgreement planChangeAgreement) {
        AccountAbonement accountAbonement = accountAbonementManager.findByPersonalAccountId(account.getId());
        if (accountAbonement != null && accountAbonement.getAbonement().isInternal()) {
            planChangeAgreement.addError("Для смены тарифного плана вам необходимо приобрести абонемент на " +
                    "текущий тарифный план сроком на 1 год или дождаться окончания бесплатного абонемента");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Проверка является ли абонемент бесплатным тестовым
     */
    Boolean hasFreeTestAbonement(AccountAbonement accountAbonement) {
        return accountAbonement != null && accountAbonement.getAbonement().getPeriod().equals("P14D");
    }

    /**
     * Может ли быть произведена смена тарифа (с Бизнес можно только на Бизнес)
     */
    private Boolean checkBusinessPlan(PlanChangeAgreement planChangeAgreement) {
        VirtualHostingPlanProperties currentPlanProperties = (VirtualHostingPlanProperties) currentPlan.getPlanProperties();
        VirtualHostingPlanProperties newPlanProperties = (VirtualHostingPlanProperties) newPlan.getPlanProperties();
        if (currentPlanProperties.isBusinessServices() && !newPlanProperties.isBusinessServices()) {
            planChangeAgreement.addError("Вы можете выбрать только другой корпоративный тарифный план");
            return false;
        } else {
            return true;
        }
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
        if (accountAbonementManager.findByPersonalAccountId(account.getId()) != null
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
        if (planChangeComparator(count, freeLimit) > 0) {
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
        Long freeLimit = planLimitsService.getFtpUserFreeLimit(newPlan);
        if (planChangeComparator(count, freeLimit) > 0) {
            planChangeAgreement.addError("Текущее количество FTP-пользователей больше, чем лимит на новом тарифном плане. "  +
                    "Текущее количество: " + count + " Лимит: " + freeLimit);
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
        if (planChangeComparator(count, freeLimit) > 0) {
            planChangeAgreement.addError("Текущее количество сайтов больше, чем лимит на новом тарифном плане. "  +
                    "Текущее количество: " + count + " Лимит: " + freeLimit);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Проверить Quota счетчики аккаунта и соответствие их новому тарифу
     */
    private Boolean checkAccountQuotaLimits(PlanChangeAgreement planChangeAgreement) {
        Long count = accountCountersService.getCurrentQuotaUsed(account.getId());
        Long freeLimit = planLimitsService.getQuotaKBFreeLimit(newPlan);
        if (planChangeComparator(count, freeLimit * 1024) > 0) {
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
    AccountAbonement addAccountAbonement(Abonement abonement) {
        AccountAbonement accountAbonement = new AccountAbonement();
        accountAbonement.setAbonementId(abonement.getId());
        accountAbonement.setPersonalAccountId(account.getId());
        accountAbonement.setCreated(LocalDateTime.now());
        accountAbonement.setExpired(LocalDateTime.now().plus(Period.parse(abonement.getPeriod())));
        accountAbonement.setAutorenew(true);

        accountAbonementManager.insert(accountAbonement);

        return accountAbonement;
    }

    void deleteRegularAbonement() {
        List<AccountAbonement> accountAbonements = accountAbonementManager.findByPersonalAccountIdAndAbonementId(
                account.getId(),
                currentPlan.getNotInternalAbonementId()
        );

        if (accountAbonements != null && !accountAbonements.isEmpty()) {
            accountAbonementManager.delete(accountAbonements);
        }
    }

    void deleteFreeTestAbonement() {
        List<AccountAbonement> accountAbonements = accountAbonementManager.findByPersonalAccountIdAndAbonementId(
                account.getId(),
                currentPlan.getFree14DaysAbonement().getId()
        );

        if (accountAbonements != null && !accountAbonements.isEmpty()) {
            accountAbonementManager.delete(accountAbonements);
        }
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
        if (planChangeComparator(currentCount, planFreeLimit) <= 0) {
            accountServiceHelper.deleteAccountServiceByServiceId(account, serviceId);
        } else {
            int notFreeServiceCount = (int) floor(currentCount - planFreeLimit);
            accountServiceHelper.addAccountService(account, serviceId, notFreeServiceCount);
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
        accountHistoryService.addMessage(account.getId(), "Произведена смена тарифа с " +
                currentPlan.getName() + " на " + newPlan.getName(), operator);
    }

    private void supportNotification() {
        if (isFromRegularToBusiness()) {
            publisher.publishEvent(new AccountNotifySupportOnChangePlanEvent(account));
        }
    }

    private void sslCertAllowed() {
        if (!newPlan.isSslCertificateAllowed()) {

            accountHelper.disableAllSslCertificates(account);

            //Запишем в историю клиента
            Map<String, String> historyParams = new HashMap<>();
            historyParams.put(HISTORY_MESSAGE_KEY, "Для аккаунта отключны SSL сертификаты в соответствии с тарифным планом");
            historyParams.put(OPERATOR_KEY, operator);

            publisher.publishEvent(new AccountHistoryEvent(account.getId(), historyParams));
        }
    }

    void disableCredit() {
        if (account.isCredit()) {
            accountManager.removeSettingByName(account.getId(), CREDIT_ACTIVATION_DATE);
            accountManager.setCredit(account.getId(), false);

            //Запишем в историю клиента
            Map<String, String> historyParams = new HashMap<>();
            historyParams.put(HISTORY_MESSAGE_KEY, "Для аккаунта отключен кредит в связи " +
                    "с переходом на тариф с обязательным абонементом");
            historyParams.put(OPERATOR_KEY, operator);

            publisher.publishEvent(new AccountHistoryEvent(account.getId(), historyParams));
        }
    }

}
