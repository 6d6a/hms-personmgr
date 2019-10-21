package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.dto.fin.PaymentLinkRequest;
import ru.majordomo.hms.personmgr.dto.push.LowBalancePush;
import ru.majordomo.hms.personmgr.event.account.AccountSendEmailWithExpiredServiceAbonementEvent;
import ru.majordomo.hms.personmgr.event.account.RedirectWasDisabledEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.ServicePlan;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequestService;
import ru.majordomo.hms.personmgr.model.service.RedirectAccountService;
import ru.majordomo.hms.personmgr.repository.AccountRedirectServiceRepository;
import ru.majordomo.hms.personmgr.repository.RevisiumRequestServiceRepository;
import ru.majordomo.hms.rc.user.resources.Domain;

import static java.time.temporal.ChronoUnit.DAYS;
import static ru.majordomo.hms.personmgr.common.AccountStatType.VIRTUAL_HOSTING_SERVICE_ABONEMENT_DELETE;
import static ru.majordomo.hms.personmgr.common.Constants.DAYS_FOR_SERVICE_ABONEMENT_EXPIRED_EMAIL_SEND;
import static ru.majordomo.hms.personmgr.common.Utils.formatBigDecimalWithCurrency;

@Service
public class ServiceAbonementService { //dis name
    private final static Logger logger = LoggerFactory.getLogger(ServiceAbonementService.class);

    private final AbonementManager<AccountServiceAbonement> abonementManager;
    private final AccountHelper accountHelper;
    private final AccountServiceHelper accountServiceHelper;
    private final ApplicationEventPublisher publisher;
    private final AccountStatHelper accountStatHelper;
    private final AccountNotificationHelper accountNotificationHelper;
    private final AccountHistoryManager history;
    private final AccountRedirectServiceRepository accountRedirectServiceRepository;
    private final RevisiumRequestServiceRepository revisiumRequestServiceRepository;
    private final RcUserFeignClient rcUserFeignClient;
    private final PersonalAccountManager accountManager;
    private final PaymentLinkHelper paymentLinkHelper;

    private static TemporalAdjuster FIVE_DAYS_AFTER = TemporalAdjusters.ofDateAdjuster(date -> date.plusDays(5));

    @Autowired
    public ServiceAbonementService(
            AbonementManager<AccountServiceAbonement> abonementManager,
            AccountHelper accountHelper,
            AccountServiceHelper accountServiceHelper,
            ApplicationEventPublisher publisher,
            AccountStatHelper accountStatHelper,
            AccountNotificationHelper accountNotificationHelper,
            AccountHistoryManager history,
            AccountRedirectServiceRepository accountRedirectServiceRepository,
            RevisiumRequestServiceRepository revisiumRequestServiceRepository,
            RcUserFeignClient rcUserFeignClient,
            PersonalAccountManager accountManager,
            PaymentLinkHelper paymentLinkHelper
    ) {
        this.abonementManager = abonementManager;
        this.accountHelper = accountHelper;
        this.accountServiceHelper = accountServiceHelper;
        this.publisher = publisher;
        this.accountStatHelper = accountStatHelper;
        this.accountNotificationHelper = accountNotificationHelper;
        this.history = history;
        this.accountRedirectServiceRepository = accountRedirectServiceRepository;
        this.revisiumRequestServiceRepository = revisiumRequestServiceRepository;
        this.rcUserFeignClient = rcUserFeignClient;
        this.accountManager = accountManager;
        this.paymentLinkHelper = paymentLinkHelper;
    }

    /**
     * Покупка абонемента
     *
     * @param account Аккаунт
     * @param abonementId id абонемента
     * @param autorenew автопродление абонемента
     */
    public AccountServiceAbonement addAbonement(PersonalAccount account, String abonementId, Feature feature, Boolean autorenew) {
        ServicePlan plan = accountServiceHelper.getServicePlanForFeatureByAccount(feature, account);

        Abonement abonement = checkAbonementAllownes(plan, abonementId);

        BigDecimal cost = accountServiceHelper.getServiceCostDependingOnDiscount(account, abonement.getService());
        if (cost.compareTo(BigDecimal.ZERO) > 0) {
            ChargeMessage chargeMessage = new ChargeMessage.Builder(abonement.getService())
                    .setAmount(cost)
                    .build();
            accountHelper.charge(account, chargeMessage);
        }

        List<AccountServiceAbonement> currentAccountServiceAbonements = abonementManager.findByPersonalAccountIdAndAbonementIdIn(
                account.getId(),
                plan.getAbonementIds()
        );

        AccountServiceAbonement accountServiceAbonement;

        if (plan.getFeature().isOnlyOnePerAccount() && currentAccountServiceAbonements != null && !currentAccountServiceAbonements.isEmpty()) {
            accountServiceAbonement = currentAccountServiceAbonements.get(0);

            accountServiceAbonement.setAbonementId(abonementId);
            accountServiceAbonement.setAbonement(abonement);

            LocalDateTime newExpireDate;

            if (accountServiceAbonement.getExpired().isBefore(LocalDateTime.now())) {
                newExpireDate = LocalDateTime.now().plus(Period.parse(abonement.getPeriod()));
            } else {
                newExpireDate = accountServiceAbonement.getExpired().plus(Period.parse(abonement.getPeriod()));
            }

            accountServiceAbonement.setExpired(newExpireDate);

            abonementManager.save(accountServiceAbonement);
        } else {
            accountServiceAbonement = new AccountServiceAbonement();
            accountServiceAbonement.setAbonementId(abonementId);
            accountServiceAbonement.setAbonement(abonement);
            accountServiceAbonement.setPersonalAccountId(account.getId());
            accountServiceAbonement.setCreated(LocalDateTime.now());
            accountServiceAbonement.setExpired(LocalDateTime.now().plus(Period.parse(abonement.getPeriod())));
            accountServiceAbonement.setAutorenew(autorenew);

            abonementManager.insert(accountServiceAbonement);
        }

        if (accountServiceHelper.accountHasService(account, plan.getServiceId())) {
            accountServiceHelper.deleteAccountServiceByServiceId(account, plan.getServiceId());
        }

        return accountServiceAbonement;
    }

    /**
     * Продление абонемента
     *
     * @param account Аккаунт
     * @param accountServiceAbonement абонемент аккаунта
     */
    public void prolongAbonement(PersonalAccount account, AccountServiceAbonement accountServiceAbonement, String prolongPeriodAbonementId) {
        if (!account.getId().equals(accountServiceAbonement.getPersonalAccountId())) {
            throw new ResourceNotFoundException("account and accountServiceAbonement are not linked");
        }

        ServicePlan servicePlan = getServicePlan(accountServiceAbonement);

        BigDecimal cost = accountServiceHelper.getServiceCostDependingOnDiscount(account, accountServiceAbonement.getAbonement().getService());

        ChargeMessage chargeMessage = new ChargeMessage.Builder(accountServiceAbonement.getAbonement().getService())
                .setAmount(cost)
                .build();
        accountHelper.charge(account, chargeMessage);

        String prolongPeriod;

        if (prolongPeriodAbonementId == null) {
            prolongPeriod = accountServiceAbonement.getAbonement().getPeriod();
        } else {
            prolongPeriod = checkAbonementAllownes(servicePlan, prolongPeriodAbonementId).getPeriod();
        }

        LocalDateTime newExpireDate;
        if (accountServiceAbonement.getExpired().isBefore(LocalDateTime.now())) {
            newExpireDate = LocalDateTime.now()
                    .plus(Period.parse(prolongPeriod));
        } else {
            newExpireDate = accountServiceAbonement.getExpired()
                    .plus(Period.parse(prolongPeriod));
        }

        abonementManager.setExpired(
                accountServiceAbonement.getId(),
                newExpireDate
        );

        if (accountServiceHelper.accountHasService(account, servicePlan.getServiceId())) {
            accountServiceHelper.deleteAccountServiceByServiceId(account, servicePlan.getServiceId());
        }
    }

    public void processExpiringAbonementsByAccount(PersonalAccount account) {
        //В итоге нам нужно получить абонементы которые заканчиваются через 5 дней и раньше
        LocalDateTime expireEnd = LocalDateTime.now().with(FIVE_DAYS_AFTER);

        logger.debug("Trying to find all service expiring abonements before expireEnd: "
                + expireEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        List<AccountServiceAbonement> accountServiceAbonements  = abonementManager.findByPersonalAccountIdAndExpiredBefore(account.getId(), expireEnd);

        accountServiceAbonements.forEach(accountServiceAbonement -> {
            if (accountServiceAbonement.getAbonement() == null
                    || accountServiceAbonement.getAbonement().getService() == null
                    || accountServiceAbonement.getAbonement().getService().getCost() == null) {
                logger.error("We found accountAbonement with null abonement or service or cost: " + accountServiceAbonement);
                return;
            }

            BigDecimal abonementCost = accountServiceHelper.getServiceCostDependingOnDiscount(account, accountServiceAbonement.getAbonement().getService());

            logger.debug("We found expiring service abonement: " + accountServiceAbonement);

            BigDecimal balance = accountHelper.getBalance(account);

            long daysToExpired = DAYS.between(LocalDateTime.now(), accountServiceAbonement.getExpired());

            boolean needSendEmail = false;

            boolean notEnoughMoneyForAbonement = balance.compareTo(abonementCost) < 0;
            boolean todayIsDayForSendingEmail = Arrays.asList(DAYS_FOR_SERVICE_ABONEMENT_EXPIRED_EMAIL_SEND).contains(daysToExpired);

            if (todayIsDayForSendingEmail
                    && notEnoughMoneyForAbonement
                    && !accountServiceAbonement.getAbonement().isInternal()
                    && accountServiceAbonement.getAbonement().getType() != Feature.ADVANCED_BACKUP_INSTANT_ACCESS
                    ) {
                needSendEmail = true;
            }

            if (needSendEmail) {
                logger.debug("Account balance is too low to buy new abonement for service. Balance: " + balance + " abonementCost: " + abonementCost);

                List<Domain> domains = accountHelper.getDomains(account);
                List<String> domainNames = new ArrayList<>();
                for (Domain domain: domains) {
                    domainNames.add(domain.getName());
                }

                String paymentLink = paymentLinkHelper.generatePaymentLinkForMail(
                        account,
                        new PaymentLinkRequest(abonementCost)
                ).getPaymentLink();

                String serviceName = accountServiceAbonement.getAbonement().getService().getName();

                //Отправим письмо
                HashMap<String, String> parameters = new HashMap<>();
                parameters.put("client_id", account.getAccountId());
                parameters.put("acc_id", account.getName());
                parameters.put("domains", String.join("<br>", domainNames));
                parameters.put("balance", formatBigDecimalWithCurrency(balance));
                parameters.put("date_finish", accountServiceAbonement.getExpired().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                parameters.put("service_name", serviceName);
                parameters.put("from", "noreply@majordomo.ru");
                parameters.put("payment_link", paymentLink);

                accountNotificationHelper.sendMail(account, "MajordomoHmsAddServicesAbonementSoonEnd", 1, parameters);

                String dateFinish = "через " + Utils.pluralizeDays(Long.valueOf(daysToExpired).intValue());

                accountNotificationHelper.push(new LowBalancePush(
                        account.getId(),
                        account.getName() + " Заканчивается абонемент на доп.услугу",
                        "Для сохранения работы услуги пополните баланс аккаунта. Абонемент на услугу " + serviceName
                                + " заканчивается " + dateFinish,
                        abonementCost
                ));
            }
        });
    }

    public boolean isRevisiumServiceAbonementAllowedToProlong(PersonalAccount account, AccountServiceAbonement accountServiceAbonement) {
        RevisiumRequestService revisiumRequestService = revisiumRequestServiceRepository
                .findByPersonalAccountIdAndAccountServiceAbonementId(account.getId(), accountServiceAbonement.getId());

        if (revisiumRequestService == null) {
            //Абонемент не принадлежит сервису ревизиума -> проверка не требуется
            return true;
        }

        String siteUrl = revisiumRequestService.getSiteUrl();

        URL url;

        try {
            url = new URL(siteUrl);
        } catch (MalformedURLException e) {
            return false;
        }

        String domainName = url.getHost();
        domainName = domainName.startsWith("www.") ? domainName.substring(4) : domainName;

        Domain domain;

        try {
            domain = rcUserFeignClient.findDomain(domainName);
        } catch (ru.majordomo.hms.personmgr.exception.ResourceNotFoundException e) {
            return false;
        }

        return domain != null && domain.getAccountId().equals(account.getId());
    }

    public void processAbonementsAutoRenewByAccount(PersonalAccount account) {
        //В итоге нам нужно получить абонементы которые закончились сегодня и раньше
        LocalDateTime expireEnd = LocalDateTime.now();

        logger.debug("Trying to find all expired service abonements before expireEnd: "
                + expireEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        List<AccountServiceAbonement> accountServiceAbonements  = abonementManager.findByPersonalAccountIdAndExpiredBefore(account.getId(), expireEnd);

        if (accountServiceAbonements.isEmpty()) {
            logger.debug("Not found expired service abonements for accountId: " + account.getId());
        }

        accountServiceAbonements.forEach(accountServiceAbonement -> {
            logger.debug("We found expired abonement: " + accountServiceAbonement);

            BigDecimal balance = accountHelper.getBalance(account);

            BigDecimal abonementCost = accountServiceHelper.getServiceCostDependingOnDiscount(account, accountServiceAbonement.getAbonement().getService());
            String currentExpired = accountServiceAbonement.getExpired().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

            // Если абонемент не бонусный (internal) и стоит автопродление
            if (!accountServiceAbonement.getAbonement().isInternal() && accountServiceAbonement.isAutorenew()) {
                    logger.debug("Abonement has autorenew option enabled");

                    if (balance.compareTo(abonementCost) >= 0) {
                        if (isRevisiumServiceAbonementAllowedToProlong(account, accountServiceAbonement)) {
                            logger.debug("Buying new abonement. Balance: " + balance + " abonementCost: " + abonementCost);

                            prolongAbonement(account, accountServiceAbonement, null);

                            history.saveForOperatorService(account,
                                    "Автоматическое продление абонемента на услугу. Со счета аккаунта списано " +
                                            formatBigDecimalWithCurrency(abonementCost) +
                                            " Новый срок окончания: " + accountServiceAbonement.getExpired().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                        } else {
                            logger.debug("Domain in Revisium Service not found on account: " + account.getId());

                            //Удаляем абонемент и включаем услуги по тарифу
                            processAccountAbonementDelete(account, accountServiceAbonement);

                            history.saveForOperatorService(account, "Абонемент на услугу удален, так как не найден домен, для которого была подключена услуга ревизиума. Стоимость абонемента: " +
                                    formatBigDecimalWithCurrency(abonementCost) +
                                    " Баланс: " + formatBigDecimalWithCurrency(balance) +
                                    " Дата окончания: " + currentExpired
                            );
                        }
                    } else {
                        logger.debug("Account balance is too low to buy new service abonement. Balance: " + balance + " abonementCost: " + abonementCost);

                        //Удаляем абонемент и включаем услуги по тарифу
                        processAccountAbonementDelete(account, accountServiceAbonement);

                        history.saveForOperatorService(account, "Абонемент на услугу удален, так как средств на счету аккаунта не достаточно. Стоимость абонемента: " +
                                formatBigDecimalWithCurrency(abonementCost) +
                                " Баланс: " + formatBigDecimalWithCurrency(balance) +
                                " Дата окончания: " + currentExpired
                        );
                    }
            } else {
                // Если абонемент бонусный (internal) или нет автопроделния

                //Удаляем абонемент и включаем услуги хостинга по тарифу
                processAccountAbonementDelete(account, accountServiceAbonement);

                history.saveForOperatorService(
                        account,
                        "Абонемент на услугу удален. Обычный абонемент не был предзаказн. Дата окончания: " + currentExpired
                );
            }
        });
    }

    private ServicePlan getServicePlan(AccountServiceAbonement serviceAbonement) {
        PersonalAccount account = accountManager.findOne(serviceAbonement.getPersonalAccountId());
        ServicePlan plan = accountServiceHelper.getServicePlanForFeatureByAccount(serviceAbonement.getAbonement().getType(), account);

        if (plan == null) {
            throw new ResourceNotFoundException("ServicePlan not found");
        }

        return plan;
    }

    private Abonement checkAbonementAllownes(ServicePlan plan, String abonementId) {

        Abonement abonement = plan.getAbonementById(abonementId);

        if (abonement == null) {
            throw new ParameterValidationException("Current service plan does not have abonement with specified abonementId");
        }

        return abonement;
    }

    /**
     * Удаление абонемента
     *
     * @param account Аккаунт
     * @param accountServiceAbonement абонемент на услугу
     */
    private void processAccountAbonementDelete(PersonalAccount account, AccountServiceAbonement accountServiceAbonement, AccountStatType reason) {

        abonementManager.delete(accountServiceAbonement);

        ServicePlan servicePlan = getServicePlan(accountServiceAbonement);

        boolean needToSendMail = false;

        BigDecimal balance = accountHelper.getBalance(account);
        if (!servicePlan.isAbonementOnly()) {
            BigDecimal costForOneMonth = accountServiceHelper.getServiceCostDependingOnDiscount(account, servicePlan.getService());
            needToSendMail = balance.compareTo(costForOneMonth) < 0;
        } else {
            needToSendMail = true;
        }
        if (needToSendMail) {
            List<Domain> domains = accountHelper.getDomains(account);
            List<String> domainNames = new ArrayList<>();
            for (Domain domain: domains) {
                domainNames.add(domain.getName());
            }

            publisher.publishEvent(new AccountSendEmailWithExpiredServiceAbonementEvent(
                    account.getAccountId(),
                    accountServiceAbonement.getAbonement().getService().getName(),
                    accountServiceAbonement.getExpired().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    String.join("<br>", domainNames),
                    balance
                    )
            );
        }

        Map<String, String> data = new HashMap<>();
        data.put("expireEnd", accountServiceAbonement.getExpired().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        data.put("abonementId", accountServiceAbonement.getAbonementId());
        accountStatHelper.add(account.getId(), reason, data);

        processServicePlanServicesAfterAbonementExpire(account, accountServiceAbonement);
    }

    private void processAccountAbonementDelete(PersonalAccount account, AccountServiceAbonement accountServiceAbonement) {
        this.processAccountAbonementDelete(account, accountServiceAbonement, VIRTUAL_HOSTING_SERVICE_ABONEMENT_DELETE);
    }

    /**
     * Добавление(проверка наличия) услуг хостинга по тарифу после окончания абонемента на услугу
     *
     * @param account Аккаунт
     */
    private void processServicePlanServicesAfterAbonementExpire(PersonalAccount account, AccountServiceAbonement accountServiceAbonement) {
        ServicePlan servicePlan = getServicePlan(accountServiceAbonement);

        if (servicePlan.getFeature() == Feature.REDIRECT) {
            RedirectAccountService redirectAccountService = accountRedirectServiceRepository.findByAccountServiceAbonementId(accountServiceAbonement.getId());
            publisher.publishEvent(new RedirectWasDisabledEvent(redirectAccountService.getPersonalAccountId(), redirectAccountService.getFullDomainName()));
            redirectAccountService.setActive(false);
            accountRedirectServiceRepository.save(redirectAccountService);
        }

        /* Отключение Feature.ANTI_SPAM не требуется, так как мы добавляем посуточную услугу
        if (servicePlan.getFeature() == Feature.ANTI_SPAM) {
            accountHelper.switchAntiSpamForMailboxes(account, false);
        }
        */

        if (!servicePlan.isAbonementOnly() && !accountServiceHelper.accountHasService(account, servicePlan.getServiceId())) {
            accountServiceHelper.addAccountService(account, servicePlan.getServiceId());
        }
    }
}
