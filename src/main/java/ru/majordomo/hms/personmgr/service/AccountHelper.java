package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.*;
import ru.majordomo.hms.personmgr.common.Constants;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.config.TestPeriodConfig;
import ru.majordomo.hms.personmgr.event.account.AccountCheckQuotaEvent;
import ru.majordomo.hms.personmgr.event.account.AccountWasEnabled;
import ru.majordomo.hms.personmgr.exception.*;
import ru.majordomo.hms.personmgr.feign.FinFeignClient;
import ru.majordomo.hms.personmgr.feign.SiFeignClient;
import ru.majordomo.hms.personmgr.manager.*;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.ArchivalPlanAccountNotice;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.PlanFallback;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountPromocodeRepository;
import ru.majordomo.hms.personmgr.repository.PlanFallbackRepository;
import ru.majordomo.hms.rc.staff.resources.template.ApplicationServer;
import ru.majordomo.hms.rc.user.resources.*;

import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.PromocodeType.GOOGLE;
import static ru.majordomo.hms.personmgr.common.Utils.formatBigDecimalWithCurrency;
import static ru.majordomo.hms.personmgr.common.Utils.getBigDecimalFromUnexpectedInput;

@Service
public class AccountHelper {

    private final static Logger logger = LoggerFactory.getLogger(AccountHelper.class);

    private final FinFeignClient finFeignClient;
    private final SiFeignClient siFeignClient;
    private final ResourceHelper resourceHelper;
    private final PersonalAccountManager accountManager;
    private final ApplicationEventPublisher publisher;
    private final AccountPromocodeRepository accountPromocodeRepository;
    private final PromocodeManager promocodeManager;
    private final AccountOwnerManager accountOwnerManager;
    private final AbonementManager<AccountAbonement> accountAbonementManager;
    private final AccountServiceHelper accountServiceHelper;
    private final AccountHistoryManager history;
    private final PlanManager planManager;
    private final AccountStatHelper accountStatHelper;
    private final AccountNoticeManager accountNoticeManager;
    private final ResourceArchiveService resourceArchiveService;
    private final TestPeriodConfig testPeriodConfig;
    private final PlanFallbackRepository planFallbackRepository;
    private final ResourceChecker resourceChecker;
    private final DedicatedAppServiceHelper dedicatedAppServiceHelper;

    @Autowired
    public AccountHelper(
            FinFeignClient finFeignClient,
            SiFeignClient siFeignClient,
            ResourceHelper resourceHelper,
            PersonalAccountManager accountManager,
            ApplicationEventPublisher publisher,
            AccountPromocodeRepository accountPromocodeRepository,
            PromocodeManager promocodeManager,
            AccountOwnerManager accountOwnerManager,
            AbonementManager<AccountAbonement> accountAbonementManager,
            AccountServiceHelper accountServiceHelper,
            AccountHistoryManager history,
            PlanManager planManager,
            AccountStatHelper accountStatHelper,
            AccountNoticeManager accountNoticeManager,
            ResourceArchiveService resourceArchiveService,
            TestPeriodConfig testPeriodConfig,
            PlanFallbackRepository planFallbackRepository,
            ResourceChecker resourceChecker,
            DedicatedAppServiceHelper dedicatedAppServiceHelper
    ) {
        this.finFeignClient = finFeignClient;
        this.siFeignClient = siFeignClient;
        this.resourceHelper = resourceHelper;
        this.accountManager = accountManager;
        this.publisher = publisher;
        this.accountPromocodeRepository = accountPromocodeRepository;
        this.promocodeManager = promocodeManager;
        this.accountOwnerManager = accountOwnerManager;
        this.accountAbonementManager = accountAbonementManager;
        this.accountServiceHelper = accountServiceHelper;
        this.history = history;
        this.planManager = planManager;
        this.accountStatHelper = accountStatHelper;
        this.accountNoticeManager = accountNoticeManager;
        this.resourceArchiveService = resourceArchiveService;
        this.testPeriodConfig = testPeriodConfig;
        this.planFallbackRepository = planFallbackRepository;
        this.resourceChecker = resourceChecker;
        this.dedicatedAppServiceHelper = dedicatedAppServiceHelper;
    }

    public String getEmail(PersonalAccount account) {
        String clientEmails = "";

        AccountOwner currentOwner = accountOwnerManager.findOneByPersonalAccountId(account.getId());

        if (currentOwner != null) {
            clientEmails = String.join(",", currentOwner.getContactInfo().getEmailAddresses());
        }

        return clientEmails;
    }

    public AccountOwner getOwnerByPersonalAccountId(String personalAccountId){
        return accountOwnerManager.findOneByPersonalAccountId(personalAccountId);
    }

    public AccountOwner.Type getOwnerType(String personalAccountId){
        AccountOwner accountOwner = getOwnerByPersonalAccountId(personalAccountId);
        if (accountOwner == null) {throw  new ResourceNotFoundException("Не найден владелец аккаунта с personalAccountId " + personalAccountId); }
        return accountOwner.getType();
    }

    public List<String> getEmails(PersonalAccount account) {
        AccountOwner currentOwner = accountOwnerManager.findOneByPersonalAccountId(account.getId());

        if (currentOwner != null) {
            return currentOwner.getContactInfo().getEmailAddresses();
        }

        return new ArrayList<>();
    }

    /**
     * Получим баланс
     *
     * @param account Аккаунт
     */
    public BigDecimal getBalance(PersonalAccount account) {
        Map<String, Object> balance = null;

        try {
            balance = finFeignClient.getBalance(account.getId());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in AccountHelper.getBalance #1 " + e.getMessage());
        }

        if (balance == null) {
            throw new ResourceNotFoundException("Не найден баланс аккаунта");
        }

        BigDecimal available;

        try {
            available = getBigDecimalFromUnexpectedInput(balance.get("available"));
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in AccountHelper.getBalance #2 " + e.getMessage());
            available = BigDecimal.ZERO;
        }

        return available;
    }

    /**
     * Получим баланс
     *
     * @param personalAccountId Аккаунт Id
     */
    public BigDecimal getBonusBalance(String personalAccountId) {
        return getBalanceByType(personalAccountId, "BONUS");
    }

    public BigDecimal getPartnerBalance(String personalAccountId) {
        return getBalanceByType(personalAccountId, "PARTNER");
    }

    private BigDecimal getBalanceByType(String accountId, String type) {
        Map<String, Object> balance = null;

        try {
            balance = finFeignClient.getBalance(accountId);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in AccountHelper.getBalance #1 " + e.getMessage());
        }

        if (balance == null) {
            throw new ResourceNotFoundException("Не найден баланс аккаунта");
        }

        BigDecimal available;

        try {
            Map<String, Map<String, Object>> datMap = (Map<String, Map<String, Object>>) balance.get("balance");
            available = getBigDecimalFromUnexpectedInput(datMap.get(type).get("available"));
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in AccountHelper.getBalance #2 " + e.getMessage());
            available = BigDecimal.ZERO;
        }

        return available;
    }

    /**
     * Получаем домены
     *
     * @param account Аккаунт
     */
    public List<Domain> getDomains(PersonalAccount account) {
        return resourceHelper.getDomains(account);
    }

    /**
     * Проверим хватает ли баланса на услугу
     *
     * @param account Аккаунт
     */
    public void checkBalance(PersonalAccount account, PaymentService service) {
        BigDecimal available = getBalance(account);

        BigDecimal cost = accountServiceHelper.getServiceCostDependingOnDiscount(account, service);

        if (available.compareTo(cost) < 0) {
            throw new NotEnoughMoneyException("Баланс аккаунта недостаточен для заказа услуги. " +
                    "Текущий баланс: " + formatBigDecimalWithCurrency(available) +
                    ", стоимость услуги: " + formatBigDecimalWithCurrency(cost),
                    cost
            );
        }
    }

    /**
     * @param account Аккаунт
     */
    public void checkBalanceWithoutBonus(PersonalAccount account, BigDecimal cost) {

        BigDecimal available = getBalance(account);

        BigDecimal bonusBalanceAvailable = getBonusBalance(account.getId());

        if (available.subtract(bonusBalanceAvailable).compareTo(cost) < 0) {
            throw new NotEnoughMoneyException("Бонусные средства недоступны для этой операции. " +
                    "Текущий баланс без учёта бонусных средств: " + formatBigDecimalWithCurrency(available.subtract(bonusBalanceAvailable)) +
                    ", стоимость услуги: " + formatBigDecimalWithCurrency(cost),
                    cost.subtract(available.subtract(bonusBalanceAvailable))
            );
        }
    }

    /**
     * Проверим хватает ли баланса на один день услуги
     *
     * @param account Аккаунт
     */
    public void checkBalance(PersonalAccount account, PaymentService service, boolean forOneDay) {
        if (!forOneDay) {
            checkBalance(account, service);
        } else {
            BigDecimal dayCost = getDayCostByService(account, service);

            BigDecimal available = getBalance(account);

            if (available.compareTo(dayCost) < 0) {
                throw new NotEnoughMoneyException("Баланс аккаунта недостаточен для заказа услуги. " +
                        "Текущий баланс: " + formatBigDecimalWithCurrency(available)
                        + " стоимость услуги за 1 день: " + formatBigDecimalWithCurrency(dayCost),
                        dayCost.subtract(available)
                );
            }
        }
    }

    private BigDecimal getDayCostByService(PersonalAccount account, PaymentService service, LocalDateTime chargeDate) {
        int daysInCurrentMonth = chargeDate.toLocalDate().lengthOfMonth();

        return accountServiceHelper.getServiceCostDependingOnDiscount(account, service)
                .divide(BigDecimal.valueOf(daysInCurrentMonth), 4, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal getDayCostByService(PersonalAccount account, PaymentService service) {
        LocalDateTime chargeDate = LocalDateTime.now();

        return getDayCostByService(account, service, chargeDate);
    }


    /**
     * @param account PersonalAccount
     * @param chargeMessage ChargeMessage
     * @return SimpleServiceMessage with param documentNumber of this success charge
     *
     * @throws BaseException inherited
     * @throws NotEnoughMoneyException in case not enough money
     * @throws InternalApiException in case request is not valid
     * @throws InternalApiException in case can't convert response body to BaseException
     *
     * @see ru.majordomo.hms.personmgr.exception.handler.MajordomoFeignErrorDecoder
     */
    public SimpleServiceMessage charge(PersonalAccount account, ChargeMessage chargeMessage) throws BaseException{
        return finFeignClient.charge(account.getId(), chargeMessage);
    }

    /**
     * @param account PersonalAccount
     * @param chargeMessage ChargeMessage
     * @return SimpleServiceMessage with param documentNumber of this success block
     *
     * @throws BaseException inherited
     * @throws NotEnoughMoneyException in case not enough money
     * @throws InternalApiException in case request is not valid
     * @throws InternalApiException in case can't convert response body to BaseException
     *
     * @see ru.majordomo.hms.personmgr.exception.handler.MajordomoFeignErrorDecoder
     */
    public SimpleServiceMessage block(PersonalAccount account, ChargeMessage chargeMessage) throws BaseException {
            return finFeignClient.block(account.getId(), chargeMessage);
    }

    public SimpleServiceMessage chargeBlocked(String personalAccountId, String documentNumber) throws BaseException {
        return finFeignClient.chargeBlocked(personalAccountId, documentNumber);
    }

    public SimpleServiceMessage unblock(String personalAccountId, String documentNumber) throws BaseException {
        return finFeignClient.unblock(personalAccountId, documentNumber);
    }

    public SimpleServiceMessage changePassword(PersonalAccount account, String newPassword) {
        Map<String, String> params = new HashMap<>();
        params.put(PASSWORD_KEY, newPassword);

        SimpleServiceMessage response = null;

        try {
            response = siFeignClient.changePassword(account.getId(), params);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in AccountHelper.changePassword " + e.getMessage());
        }

        if (response != null && (response.getParam("success") == null || !((boolean) response.getParam("success")))) {
            throw new InternalApiException("Ошибка. Пароль не изменен.");
        }

        return response;
    }

    public String getGooglePromocode(PersonalAccount account) {

        List<AccountPromocode> accountPromocodes = accountPromocodeRepository.findByPersonalAccountId(account.getId());

        AccountPromocode googleAccountPromocode;

        for (AccountPromocode accountPromocode : accountPromocodes) {
            if (accountPromocode.getPromocode().getType().equals(GOOGLE)) {
                googleAccountPromocode = accountPromocode;
                return googleAccountPromocode.getPromocode().getCode();
            }
        }

        return null;
    }

    public Boolean isGooglePromocodeAllowed(PersonalAccount account) {

        if (this.getGooglePromocode(account) != null) {
            return false;
        } else {
            try {
                BigDecimal overallPaymentAmount = finFeignClient.getOverallPaymentAmount(account.getId());
                return overallPaymentAmount.compareTo(BigDecimal.valueOf(500L)) >= 0;
            } catch (Exception e) {
                e.printStackTrace();
                throw new ParameterValidationException("Ошибка при получении промокода");
            }
        }
    }

    public Boolean isGoogleActionUsed(PersonalAccount account) {
        return account.getProperties().getGoogleActionUsed() != null && account.getProperties().getGoogleActionUsed();
    }

    public String giveGooglePromocode(PersonalAccount account) {
        if (this.isGooglePromocodeAllowed(account)) {
            Promocode promocode = promocodeManager.findByTypeAndActive(GOOGLE, true);

            if (promocode != null) {
                promocode.setActive(false);

                AccountPromocode accountPromocode = new AccountPromocode();
                accountPromocode.setOwnedByAccount(true);
                accountPromocode.setPersonalAccountId(account.getId());
                accountPromocode.setOwnerPersonalAccountId(account.getId());
                accountPromocode.setPromocodeId(promocode.getId());
                accountPromocode.setPromocode(promocode);

                promocodeManager.save(promocode);
                accountPromocodeRepository.save(accountPromocode);

                return promocode.getCode();
            } else {
                throw new ParameterValidationException("Ошибка при получнеии промокода Google");
            }
        } else {
            throw new ParameterValidationException("Ошибка при получнеии промокода Google");
        }
    }

    public void disableAccount(PersonalAccount account) {
        switchAccountActiveState(account, false);
    }

    public void enableAccount(String accountId) {
        PersonalAccount account = accountManager.findOne(accountId);
        enableAccount(account);
    }

    public void enableAccount(PersonalAccount account) {
        switchAccountActiveState(account, true);
    }

    public void switchAccountActiveState(PersonalAccount account, Boolean state) {
        if (account.isActive() != state) {
            history.save(account, "Аккаунт " + (state ? "включен" : "выключен"));

            accountManager.setActive(account.getId(), state);
            resourceHelper.switchAccountResources(account, state);

            if (state) {
                publisher.publishEvent(new AccountWasEnabled(account.getId(), account.getDeactivated()));
            }
        }
    }

    /*
     * получим стоимость абонемента на период через Plan, PlanId или PersonalAccount
     * @period - период действия абонемента, "P1Y" - на год
     */

    public BigDecimal getCostAbonement(PersonalAccount account) {
        return getCostAbonement(account, "P1Y");
    }

    private BigDecimal getCostAbonement(PersonalAccount account, String period) {
        Plan plan = planManager.findOne(account.getPlanId());
        return getCostAbonement(account, plan, period);
    }

    public BigDecimal getCostAbonement(PersonalAccount account, Plan plan) {
        return this.getCostAbonement(account, plan, "P1Y");
    }

    private BigDecimal getCostAbonement(PersonalAccount account, Plan plan, String period) {
        return accountServiceHelper.getServiceCostDependingOnDiscount(account, plan.getAbonements()
                .stream().filter(
                        abonement -> abonement.getPeriod().equals(period)
                ).collect(Collectors.toList()).get(0).getService());
    }

    public boolean hasActiveAbonement(String accountId) {
        PersonalAccount account = accountManager.findOne(accountId);
        return !accountAbonementManager.findByPersonalAccountIdAndExpiredAfter(account.getId(), LocalDateTime.now()).isEmpty();
    }

    public Boolean hasActiveCredit(PersonalAccount account) {
        if (account.isCredit()) {
            //Если у аккаунта подключен кредит
            LocalDateTime creditActivationDate = account.getCreditActivationDate();
            //Проверяем что дата активации выставлена
            if (creditActivationDate == null) {
                // Далее дата активация выставляется в null, только при платеже, который вывел аккаунт из минуса
                return true;
            } else {
                // Проверяем сколько он уже пользуется
                return !creditActivationDate.isBefore(
                        LocalDateTime
                                .now()
                                .minus(Period.parse(account.getCreditPeriod()))
                );
            }
        } else {
            return false;
        }
    }

    /*
     *  Устанавливает дату активации кредита, если она отсутствует
     */

    public void setCreditActivationDateIfNotSet(PersonalAccount account) {
        if (account.getCreditActivationDate() == null) {
            LocalDateTime now = LocalDateTime.now();
            accountManager.setCreditActivationDate(account.getId(), now);
            history.save(account, "Установлена дата активации кредита на " + now);
        }
    }

    /*
     *  Ставит active=false в accountService
     *  Если это доп.квота, то кидает ивент на пересчет квоты
     *  Остальные услуги, если требуеются  нужно добавлять индивидуально
     */

    public void disableAdditionalService(AccountService accountService) {
        PersonalAccount account = accountManager.findOne(accountService.getPersonalAccountId());

        if (accountService.getPaymentService().getPaymentType() == ServicePaymentType.ONE_TIME) {
            accountServiceHelper.disableAccountService(accountService);
        } else {
            accountServiceHelper.disableAccountService(account, accountService.getServiceId());
        }

        String paymentServiceOldId = accountService.getPaymentService().getOldId();
        if (paymentServiceOldId.equals(ADDITIONAL_QUOTA_100_SERVICE_ID)) {
            account.setAddQuotaIfOverquoted(false);
            publisher.publishEvent(new AccountCheckQuotaEvent(account.getId()));
        } else if (paymentServiceOldId.equals(ANTI_SPAM_SERVICE_ID)) {
            try {
                resourceHelper.switchAntiSpamForMailboxes(account, false);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Switch account Mailboxes anti-spam failed");
            }
        } else if (paymentServiceOldId.equals(LONG_LIFE_RESOURCE_ARCHIVE_SERVICE_ID)) {
            resourceArchiveService.processAccountServiceDelete(accountService);
        }

        history.save(account, "Услуга " + accountService.getPaymentService().getName() + " отключена в связи с нехваткой средств.");
    }

    public Plan getArchivalFallbackPlan() {
        return planManager.findByOldId(String.valueOf(PLAN_UNLIMITED_ID));
    }

    private Plan getArchivalFallbackPlan(Plan currentPlan) {
        PlanFallback planFallback = planFallbackRepository.findOneByPlanId(currentPlan.getId());
        if (planFallback != null) {
            return planManager.findOne(planFallback.getFallbackPlanId());
        } else {
            return getArchivalFallbackPlan();
        }
    }

    public void setPlanId(String personalAccountId, String planId) {
        accountManager.setPlanId(personalAccountId, planId);
    }

    public Plan getPlan(PersonalAccount account) {
        return planManager.findOne(account.getPlanId());
    }

    public void changeArchivalPlanToActive(PersonalAccount account) {
        Plan currentPlan = getPlan(account);
        accountServiceHelper.deleteAccountServiceByServiceId(account, currentPlan.getServiceId());

        Plan fallbackPlan = getArchivalFallbackPlan(currentPlan);
        accountManager.setPlanId(account.getId(), fallbackPlan.getId());
        accountServiceHelper.addAccountService(account, fallbackPlan.getServiceId());
        accountStatHelper.archivalPlanChange(account.getId(), currentPlan.getId(), fallbackPlan.getId());
        addArchivalPlanAccountNoticeRepository(account, currentPlan);

        history.save(account, "Архивный тариф " + currentPlan.getName() + " был изменен на тариф "
                + fallbackPlan.getName() + " по причине прекращения поддержки");
    }

    public void addArchivalPlanAccountNoticeRepository(PersonalAccount account, Plan plan) {
        if (!accountNoticeManager.existsByPersonalAccountIdAndTypeAndViewed(
                account.getId(), AccountNoticeType.ARCHIVAL_PLAN_CHANGE, false)
        ) {
            ArchivalPlanAccountNotice notification = new ArchivalPlanAccountNotice();
            notification.setPersonalAccountId(account.getId());
            notification.setCreated(LocalDateTime.now());
            notification.setViewed(false);
            notification.setOldPlanName(plan.getName());

            accountNoticeManager.save(notification);
        }
    }

    public void checkIsDomainAddAllowed(PersonalAccount account, String domainName) {
        List<AccountAbonement> abonements = accountAbonementManager.findAllByPersonalAccountId(account.getId());

        boolean onlyTrialAbonement = abonements.size() > 0 && abonements.stream().allMatch(a -> a.getAbonement().isTrial());

        BooleanSupplier hasNoMoney = () -> {
            BigDecimal overallPaymentAmount = finFeignClient.getOverallPaymentAmount(account.getId());
            Plan currentPlan = planManager.findOne(account.getPlanId());
            return overallPaymentAmount.compareTo(accountServiceHelper.getServiceCostDependingOnDiscount(account, currentPlan.getService())) < 0;
        };

        if (onlyTrialAbonement) {
            List<Domain> domainsList = resourceHelper.getDomains(account);
            if (domainsList != null && !domainsList.isEmpty()) {
                if (hasNoMoney.getAsBoolean()) {
                    throw new ParameterValidationException(
                            "Для добавления домена необходимо оплатить хостинг или купить абонемент.");
                }
            } else {
                for (String zone : testPeriodConfig.getDisallowDomainZones()) {
                    if (domainName.endsWith(zone)) {
                        if (hasNoMoney.getAsBoolean()) {
                            throw new ParameterValidationException(
                                    "Для добавления домена в зоне " + zone +
                                            " необходимо оплатить хостинг или купить абонемент.");
                        }
                    }
                }
            }
        }
    }

    /**
     * Проверка доступности дополнительной услуги на тарифном плане
     * @param personalAccount PersonalAccount instance
     * @param feature Feature instance
     */
    public void checkIsAdditionalServiceAllowed(PersonalAccount personalAccount, Feature feature) {
        Plan plan = planManager.findOne(personalAccount.getPlanId());

        switch (feature) {
            case ADVANCED_BACKUP:
            case ADVANCED_BACKUP_INSTANT_ACCESS:
                if (!plan.isUnixAccountAllowed()) {
                    throw new ParameterValidationException("Заказ услуги недоступен на вашем тарифном плане");
                }
                break;

            case ANTI_SPAM:
                if (!plan.isMailboxAllowed()) {
                    throw new ParameterValidationException("Заказ услуги недоступен на вашем тарифном плане");
                }
                break;

            case REDIRECT:
            case REVISIUM:
            case GOOGLE_3000:
                if (!plan.isDomainAllowed()) {
                    throw new ParameterValidationException("Заказ услуги недоступен на вашем тарифном плане");
                }
                break;

            case SEO:
                if (plan.isPartnerPlan()) {
                    throw new ParameterValidationException("Заказ услуги недоступен на вашем тарифном плане");
                }
                break;
        }
    }

    /**
     * Проверка возможности установки CMS на текущем тарифном плане
     * @param account PersonalAccount instance
     */
    public void checkIsCmsAllowed(PersonalAccount account) {
        try {
            resourceChecker.checkResource(account, ResourceType.DATABASE, null);
        } catch (ParameterValidationException ex) {
            throw new ParameterValidationException("Для установки CMS необходима возможность использовать базы данных");
        }

        Plan plan = planManager.findOne(account.getPlanId());

        if (!plan.isWebSiteAllowed()) {
            throw new ParameterValidationException("Для установки CMS необходима возможность использовать web-сайты");
        }

        if (plan.getPlanProperties() instanceof VirtualHostingPlanProperties) {
            VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) plan.getPlanProperties();
            if (!planProperties.getWebSiteAllowedServiceTypes().contains(Constants.WEBSITE_APACHE2_PHP)) {
                if (plan.getAllowedFeature().contains(Feature.DEDICATED_APP_SERVICE)) {
                    if (dedicatedAppServiceHelper.getServicesWithStaffService(account.getId()).stream()
                            .noneMatch(das -> das.getService() != null && das.getService().getTemplate() instanceof ApplicationServer
                                    && ((ApplicationServer) das.getService().getTemplate()).getLanguage() == ApplicationServer.Language.PHP)) {
                        throw new ParameterValidationException("Для установки CMS необходимо заказать сервис с поддержкой PHP");
                    }
                } else {
                    throw new ParameterValidationException("Для установки CMS необходим тарифный план с поддержкой PHP");
                }
            }
        }
    }
}
