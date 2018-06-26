package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.*;
import ru.majordomo.hms.personmgr.event.account.RedirectWasDisabledEvent;
import ru.majordomo.hms.personmgr.event.account.RedirectWasProlongEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.ServicePlan;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequestService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.model.service.RedirectAccountService;
import ru.majordomo.hms.personmgr.repository.*;
import ru.majordomo.hms.rc.user.resources.Domain;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static java.time.temporal.ChronoUnit.DAYS;
import static ru.majordomo.hms.personmgr.common.AccountStatType.VIRTUAL_HOSTING_ABONEMENT_DELETE;
import static ru.majordomo.hms.personmgr.common.AccountStatType.VIRTUAL_HOSTING_SERVICE_ABONEMENT_DELETE;
import static ru.majordomo.hms.personmgr.common.AccountStatType.VIRTUAL_HOSTING_USER_DELETE_SERVICE_ABONEMENT;
import static ru.majordomo.hms.personmgr.common.Constants.DAYS_FOR_ABONEMENT_EXPIRED_EMAIL_SEND;
import static ru.majordomo.hms.personmgr.common.Utils.formatBigDecimalWithCurrency;

@Service
public class ServiceAbonementService { //dis name
    private final static Logger logger = LoggerFactory.getLogger(ServiceAbonementService.class);

    private final ServicePlanRepository servicePlanRepository;
    private final AbonementRepository abonementRepository;
    private final PaymentServiceRepository paymentServiceRepository;
    private final AbonementManager<AccountServiceAbonement> abonementManager;
    private final AccountHelper accountHelper;
    private final AccountServiceHelper accountServiceHelper;
    private final ApplicationEventPublisher publisher;
    private final AccountStatHelper accountStatHelper;
    private final AccountNotificationHelper accountNotificationHelper;
    private final ChargeHelper chargeHelper;
    private final AccountHistoryManager history;
    private final AccountRedirectServiceRepository accountRedirectServiceRepository;
    private final RevisiumRequestServiceRepository revisiumRequestServiceRepository;
    private final RcUserFeignClient rcUserFeignClient;

    private static TemporalAdjuster FIVE_DAYS_AFTER = TemporalAdjusters.ofDateAdjuster(date -> date.plusDays(5));

    @Autowired
    public ServiceAbonementService(
            ServicePlanRepository servicePlanRepository,
            AbonementRepository abonementRepository,
            PaymentServiceRepository paymentServiceRepository,
            AbonementManager<AccountServiceAbonement> abonementManager,
            AccountHelper accountHelper,
            AccountServiceHelper accountServiceHelper,
            ApplicationEventPublisher publisher,
            AccountStatHelper accountStatHelper,
            AccountNotificationHelper accountNotificationHelper,
            ChargeHelper chargeHelper,
            AccountHistoryManager history,
            AccountRedirectServiceRepository accountRedirectServiceRepository,
            RevisiumRequestServiceRepository revisiumRequestServiceRepository,
            RcUserFeignClient rcUserFeignClient
    ) {
        this.servicePlanRepository = servicePlanRepository;
        this.abonementRepository = abonementRepository;
        this.paymentServiceRepository = paymentServiceRepository;
        this.abonementManager = abonementManager;
        this.accountHelper = accountHelper;
        this.accountServiceHelper = accountServiceHelper;
        this.publisher = publisher;
        this.accountStatHelper = accountStatHelper;
        this.accountNotificationHelper = accountNotificationHelper;
        this.chargeHelper = chargeHelper;
        this.history = history;
        this.accountRedirectServiceRepository = accountRedirectServiceRepository;
        this.revisiumRequestServiceRepository = revisiumRequestServiceRepository;
        this.rcUserFeignClient = rcUserFeignClient;
    }

    /**
     * Покупка абонемента
     *
     * @param account Аккаунт
     * @param abonementId id абонемента
     * @param autorenew автопродление абонемента
     */
    public AccountServiceAbonement addAbonement(PersonalAccount account, String abonementId, Feature feature, Boolean autorenew) {

        ServicePlan plan = servicePlanRepository.findOneByFeatureAndActive(feature, true);

        Abonement abonement = checkAbonementAllownes(plan, abonementId);

        if (abonement.getService().getCost().compareTo(BigDecimal.ZERO) > 0) {

            ChargeMessage chargeMessage = new ChargeMessage.Builder(abonement.getService())
                    .build();
            accountHelper.charge(account, chargeMessage);
        }

        AccountServiceAbonement accountServiceAbonement = new AccountServiceAbonement();
        accountServiceAbonement.setAbonementId(abonementId);
        accountServiceAbonement.setAbonement(abonement);
        accountServiceAbonement.setPersonalAccountId(account.getId());
        accountServiceAbonement.setCreated(LocalDateTime.now());
        accountServiceAbonement.setExpired(LocalDateTime.now().plus(Period.parse(abonement.getPeriod())));
        accountServiceAbonement.setAutorenew(autorenew);

        abonementManager.insert(accountServiceAbonement);

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
    public void prolongAbonement(PersonalAccount account, AccountServiceAbonement accountServiceAbonement) {

        if (!account.getId().equals(accountServiceAbonement.getPersonalAccountId())) {
            throw new ResourceNotFoundException("account and accountServiceAbonement are not linked");
        }

        ServicePlan servicePlan = getServicePlan(accountServiceAbonement);

        ChargeMessage chargeMessage = new ChargeMessage.Builder(accountServiceAbonement.getAbonement().getService())
                .build();
        accountHelper.charge(account, chargeMessage);


        LocalDateTime newExpireDate;
        if (accountServiceAbonement.getExpired().isBefore(LocalDateTime.now())) {
            newExpireDate = LocalDateTime.now()
                    .plus(Period.parse(accountServiceAbonement.getAbonement().getPeriod()));
        } else {
            newExpireDate = accountServiceAbonement.getExpired()
                    .plus(Period.parse(accountServiceAbonement.getAbonement().getPeriod()));
        }

        abonementManager.setExpired(
                accountServiceAbonement.getId(),
                newExpireDate
        );

        if (accountServiceHelper.accountHasService(account, servicePlan.getServiceId())) {
            accountServiceHelper.deleteAccountServiceByServiceId(account, servicePlan.getServiceId());
        }
    }

    /**
     * Удаление абонемента
     *
     * @param account Аккаунт
     * @param serviceAbonementId id абонемента на услугу
     */
    public void deleteAbonement(PersonalAccount account, String serviceAbonementId) {
        AccountServiceAbonement accountServiceAbonement = abonementManager.findByIdAndPersonalAccountId(serviceAbonementId, account.getId());

        processAccountAbonementDelete(account, accountServiceAbonement, VIRTUAL_HOSTING_USER_DELETE_SERVICE_ABONEMENT);
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

            BigDecimal abonementCost = accountServiceAbonement.getAbonement().getService().getCost();

            logger.debug("We found expiring service abonement: " + accountServiceAbonement);

            BigDecimal balance = accountHelper.getBalance(account);

            long daysToExpired = DAYS.between(LocalDateTime.now(), accountServiceAbonement.getExpired());

            Boolean needSendEmail = false;

            boolean notEnoughMoneyForAbonement = balance.compareTo(abonementCost) < 0;
            boolean todayIsDayForSendingEmail = Arrays.asList(DAYS_FOR_ABONEMENT_EXPIRED_EMAIL_SEND).contains(daysToExpired);

            if (todayIsDayForSendingEmail && notEnoughMoneyForAbonement && !accountServiceAbonement.getAbonement().isInternal()) {
                needSendEmail = true;
            }

            //TODO пока что не отправялем никаких писем
            //(учесть при рефакторинге всей логики писем)

            if (needSendEmail) {
//                logger.debug("Account balance is too low to buy new abonement. Balance: " + balance + " abonementCost: " + abonementCost);
//
//                List<Domain> domains = accountHelper.getDomains(account);
//                List<String> domainNames = new ArrayList<>();
//                for (Domain domain: domains) {
//                    domainNames.add(domain.getName());
//                }
//
//                //Отправим письмо
//                HashMap<String, String> parameters = new HashMap<>();
//                parameters.put("client_id", account.getAccountId());
//                parameters.put("acc_id", account.getName());
//                parameters.put("domains", String.join("<br>", domainNames));
//                parameters.put("balance", formatBigDecimalWithCurrency(balance));
//                parameters.put("cost", formatBigDecimalWithCurrency(abonementCost)); //Этот параметр передаётся, но не используется
//                parameters.put("date_finish", accountAbonement.getExpired().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
//                parameters.put("auto_renew", accountAbonement.isAutorenew() ? "включено" : "выключено");
//                parameters.put("from", "noreply@majordomo.ru");
//
//                accountNotificationHelper.sendMail(account, "MajordomoVHAbNoMoneyProlong", 1, parameters);
            }
//
//            if (!accountAbonement.isAutorenew() || notEnoughMoneyForAbonement) {
//                if (accountNotificationHelper.isSubscribedToSmsType(account, SMS_ABONEMENT_EXPIRING)
//                        && Arrays.asList(DAYS_FOR_ABONEMENT_EXPIRED_SMS_SEND).contains(daysToExpired))
//                {
//                    HashMap<String, String> paramsForSms = new HashMap<>();
//                    paramsForSms.put("acc_id", account.getName());
//                    paramsForSms.put("client_id", account.getAccountId());
//                    paramsForSms.put("remaining_days", Utils.pluralizef("%d день", "%d дня", "%d дней", ((Long) daysToExpired).intValue()));
//                    accountNotificationHelper.sendSms(account, "HmsMajordomoAbonementExpiring", 5, paramsForSms);
//                }
//            }
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
            BigDecimal abonementCost = accountServiceAbonement.getAbonement().getService().getCost();
            String currentExpired = accountServiceAbonement.getExpired().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

            // Если абонемент не бонусный (internal) и стоит автопродление
            if (!accountServiceAbonement.getAbonement().isInternal() && accountServiceAbonement.isAutorenew()) {
                    logger.debug("Abonement has autorenew option enabled");

                    if (balance.compareTo(abonementCost) >= 0) {
                        if (isRevisiumServiceAbonementAllowedToProlong(account, accountServiceAbonement)) {
                            logger.debug("Buying new abonement. Balance: " + balance + " abonementCost: " + abonementCost);

                            prolongAbonement(account, accountServiceAbonement);

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
                // Если абонемент бонусный (internal)

                //Удаляем абонемент и включаем услуги хостинга по тарифу
                processAccountAbonementDelete(account, accountServiceAbonement);

                history.saveForOperatorService(
                        account,
                        "Бонусный абонемент на услугу удален. Обычный абонемент не был предзаказн. Дата окончания: " + currentExpired
                );
            }
        });
    }

    private ServicePlan getServicePlan(AccountServiceAbonement serviceAbonement) {
        ServicePlan plan = servicePlanRepository.findOneByFeatureAndActive(serviceAbonement.getAbonement().getType(), true);

        if (plan == null) {
            throw new ResourceNotFoundException("ServicePlan not found");
        }

        return plan;
    }

    private Abonement checkAbonementAllownes(ServicePlan plan, String abonementId) {

        if (!plan.getAbonementIds().contains(abonementId)) {
            throw new ParameterValidationException("Current service plan does not have abonement with specified abonementId");
        }

        Optional<Abonement> newAbonement = plan.getAbonements().stream().filter(abonement1 -> abonement1.getId().equals(abonementId)).findFirst();

        if (!newAbonement.isPresent()) {
            throw new ParameterValidationException("Current service plan does not have abonement with specified abonementId (not found in abonements)");
        }

        return newAbonement.get();
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
        if (!servicePlan.isAbonementOnly()) {
            BigDecimal balance = accountHelper.getBalance(account);
            BigDecimal costForOneMonth = servicePlan.getService().getCost();
            needToSendMail = balance.compareTo(costForOneMonth) < 0;
        } else {
            needToSendMail = true;
        }
        if (needToSendMail) {
            ///TODO пока что не отправялем никаких писем
            //(учесть при рефакторинге всей логики писем)
            //publisher.publishEvent(new AccountSendEmailWithExpiredAbonementEvent(account));
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
