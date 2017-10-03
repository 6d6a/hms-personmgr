package ru.majordomo.hms.personmgr.service;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountCheckQuotaEvent;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.exception.ChargeException;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.LowBalanceException;
import ru.majordomo.hms.personmgr.manager.AccountAbonementManager;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountPromocodeRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.repository.PromocodeRepository;
import ru.majordomo.hms.rc.user.resources.*;

import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.PromocodeType.GOOGLE;
import static ru.majordomo.hms.personmgr.common.Utils.formatBigDecimalWithCurrency;
import static ru.majordomo.hms.personmgr.common.Utils.getBigDecimalFromUnexpectedInput;

@Service
public class AccountHelper {

    private final static Logger logger = LoggerFactory.getLogger(AccountHelper.class);

    private final RcUserFeignClient rcUserFeignClient;
    private final FinFeignClient finFeignClient;
    private final SiFeignClient siFeignClient;
    private final AccountPromotionManager accountPromotionManager;
    private final BusinessActionBuilder businessActionBuilder;
    private final PersonalAccountManager accountManager;
    private final ApplicationEventPublisher publisher;
    private final AccountPromocodeRepository accountPromocodeRepository;
    private final PromocodeRepository promocodeRepository;
    private final AccountOwnerManager accountOwnerManager;
    private final PlanRepository planRepository;
    private final AccountAbonementManager accountAbonementManager;
    private final AccountServiceHelper accountServiceHelper;

    @Autowired
    public AccountHelper(
            RcUserFeignClient rcUserFeignClient,
            FinFeignClient finFeignClient,
            SiFeignClient siFeignClient,
            AccountPromotionManager accountPromotionManager,
            BusinessActionBuilder businessActionBuilder,
            PersonalAccountManager accountManager,
            ApplicationEventPublisher publisher,
            AccountPromocodeRepository accountPromocodeRepository,
            PromocodeRepository promocodeRepository,
            AccountOwnerManager accountOwnerManager,
            PlanRepository planRepository,
            AccountAbonementManager accountAbonementManager,
            AccountServiceHelper accountServiceHelper
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.finFeignClient = finFeignClient;
        this.siFeignClient = siFeignClient;
        this.accountPromotionManager = accountPromotionManager;
        this.businessActionBuilder = businessActionBuilder;
        this.accountManager = accountManager;
        this.publisher = publisher;
        this.accountPromocodeRepository = accountPromocodeRepository;
        this.promocodeRepository = promocodeRepository;
        this.accountOwnerManager = accountOwnerManager;
        this.planRepository = planRepository;
        this.accountAbonementManager = accountAbonementManager;
        this.accountServiceHelper = accountServiceHelper;
    }

    public String getEmail(PersonalAccount account) {
        String clientEmails = "";

        AccountOwner currentOwner = accountOwnerManager.findOneByPersonalAccountId(account.getId());

        if (currentOwner != null) {
            clientEmails = String.join(",", currentOwner.getContactInfo().getEmailAddresses());
        }

        return clientEmails;
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
     * Получаем домены
     *
     * @param account Аккаунт
     */
    public List<Domain> getDomains(PersonalAccount account) {
        List<Domain> domains = null;

        try {
            domains = rcUserFeignClient.getDomains(account.getId());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in AccountHelper.getDomains " + e.getMessage());
        }

        return domains;
    }

    /**
     * Проверим не отрицательный ли баланс
     *
     * @param account Аккаунт
     */
    public void checkBalance(PersonalAccount account) {
        BigDecimal available = getBalance(account);

        if (available.compareTo(BigDecimal.ZERO) < 0) {
            throw new LowBalanceException("Баланс аккаунта отрицательный: "
                    + formatBigDecimalWithCurrency(available));
        }
    }

    /**
     * Проверим хватает ли баланса на услугу
     *
     * @param account Аккаунт
     */
    public void checkBalance(PersonalAccount account, PaymentService service) {
        BigDecimal available = getBalance(account);

        if (available.compareTo(service.getCost()) < 0) {
            throw new LowBalanceException("Баланс аккаунта недостаточен для заказа услуги. " +
                    "Текущий баланс: " + formatBigDecimalWithCurrency(available) +
                    ", стоимость услуги: " + formatBigDecimalWithCurrency(service.getCost()));
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
            BigDecimal dayCost = getDayCostByService(service);

            BigDecimal available = getBalance(account);

            if (available.compareTo(dayCost) < 0) {
                throw new LowBalanceException("Баланс аккаунта недостаточен для заказа услуги. " +
                        "Текущий баланс: " + formatBigDecimalWithCurrency(available) + " стоимость услуги за 1 день: " + formatBigDecimalWithCurrency(dayCost));
            }
        }
    }

    public BigDecimal getDayCostByService(PaymentService service, LocalDateTime chargeDate) {
        Integer daysInCurrentMonth = chargeDate.toLocalDate().lengthOfMonth();

        return service.getCost().divide(BigDecimal.valueOf(daysInCurrentMonth), 4, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal getDayCostByService(PaymentService service) {
        LocalDateTime chargeDate = LocalDateTime.now();

        return getDayCostByService(service, chargeDate);
    }

    public SimpleServiceMessage charge(PersonalAccount account, PaymentService service) {
        BigDecimal amount = service.getCost();

        return charge(account, service, amount, false, false);
    }

    public SimpleServiceMessage charge(PersonalAccount account, PaymentService service, BigDecimal amount) {
        return charge(account, service, amount, false, false);
    }

    public SimpleServiceMessage charge(PersonalAccount account, PaymentService service, BigDecimal amount, Boolean forceCharge, Boolean bonusChargeProhibited) {
        Map<String, Object> paymentOperation = new HashMap<>();
        paymentOperation.put("serviceId", service.getId());
        paymentOperation.put("amount", amount);
        paymentOperation.put("forceCharge", forceCharge);
        paymentOperation.put("bonusChargeProhibited", bonusChargeProhibited);

        SimpleServiceMessage response;

        try {
            response = finFeignClient.charge(account.getId(), paymentOperation);
        } catch (Exception e) {
            if (!(e instanceof FeignException) || ((FeignException) e).status() != 400 ) {
                logger.error("Exception in AccountHelper.charge " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
            throw new ChargeException("Произошла ошибка при списании средств." +
                    " Стоимость услуги: " + formatBigDecimalWithCurrency(service.getCost()));
        }

        if (response != null && (response.getParam("success") == null || !((boolean) response.getParam("success")))) {
            throw new ChargeException("Баланс аккаунта недостаточен для заказа услуги. " +
                    " Стоимость услуги: " + formatBigDecimalWithCurrency(service.getCost()));
        }

        return response;
    }

    public SimpleServiceMessage block(PersonalAccount account, PaymentService service) {
        return block(account, service, false);
    }

    //TODO на самом деле сюда ещё должна быть возможность передать discountedService
    public SimpleServiceMessage block(PersonalAccount account, PaymentService service, Boolean bonusChargeProhibited) {
        Map<String, Object> paymentOperation = new HashMap<>();
        paymentOperation.put("serviceId", service.getId());
        paymentOperation.put("amount", service.getCost());
        paymentOperation.put("bonusChargeProhibited", bonusChargeProhibited);

        SimpleServiceMessage response;
        try {
            response = finFeignClient.block(account.getId(), paymentOperation);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in AccountHelper.block " + e.getMessage());
            throw new ChargeException("Произошла ошибка при блокировке средств." +
                    " Стоимость услуги: " + formatBigDecimalWithCurrency(service.getCost()));
        }

        if (response != null && (response.getParam("success") == null || !((boolean) response.getParam("success")))) {
            throw new ChargeException("Баланс аккаунта недостаточен для заказа услуги. " +
                    " Стоимость услуги: " + formatBigDecimalWithCurrency(service.getCost()));
        }

        return response;
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

    public void giveGift(PersonalAccount account, Promotion promotion) {
        Long currentCount = accountPromotionManager.countByPersonalAccountIdAndPromotionId(account.getId(), promotion.getId());
        if (currentCount < promotion.getLimitPerAccount() || promotion.getLimitPerAccount() == -1) {
            AccountPromotion accountPromotion = new AccountPromotion();
            accountPromotion.setPersonalAccountId(account.getId());
            accountPromotion.setPromotionId(promotion.getId());
            accountPromotion.setPromotion(promotion);
            accountPromotion.setCreated(LocalDateTime.now());

            Map<String, Boolean> actionsWithStatus = new HashMap<>();
            for (String actionId : promotion.getActionIds()) {
                actionsWithStatus.put(actionId, true);
            }
            accountPromotion.setActionsWithStatus(actionsWithStatus);

            accountPromotionManager.insert(accountPromotion);

            saveHistoryForOperatorService(account, "Добавлен бонус " + accountPromotion.getPromotion().getName());
        }
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

    public String giveGooglePromocode(PersonalAccount account) {

        if (this.isGooglePromocodeAllowed(account)) {

            Promocode promocode = promocodeRepository.findByTypeAndActive(GOOGLE.toString(), true);

            if (promocode != null) {

                promocode.setActive(false);

                AccountPromocode accountPromocode = new AccountPromocode();
                accountPromocode.setOwnedByAccount(true);
                accountPromocode.setPersonalAccountId(account.getId());
                accountPromocode.setOwnerPersonalAccountId(account.getId());
                accountPromocode.setPromocodeId(promocode.getId());
                accountPromocode.setPromocode(promocode);

                promocodeRepository.save(promocode);
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
        switchAccountActiveState(account, true);
    }

    public void enableAccount(PersonalAccount account) {
        switchAccountActiveState(account, true);
    }

    public void switchAccountActiveState(PersonalAccount account, Boolean state) {
        if (account.isActive() != state) {
            saveHistoryForOperatorService(account, "Аккаунт " + (state ? "включен" : "выключен"));

            accountManager.setActive(account.getId(), state);
            switchAccountResources(account, state);
        }
    }

    public void switchOffAntiSpamForMailboxes(PersonalAccount account) {

        Collection<Mailbox> mailboxes = rcUserFeignClient.getMailboxes(account.getId());

        for (Mailbox mailbox : mailboxes) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", mailbox.getId());
            message.addParam("antiSpamEnabled", false);

            businessActionBuilder.build(BusinessActionType.MAILBOX_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на выключение анти-спама у почтового ящика '" + mailbox.getName() + "' в связи с отключением услуги";
            saveHistoryForOperatorService(account, historyMessage);
        }
    }

    public void switchAccountResources(PersonalAccount account, Boolean state) {
        try {

            List<WebSite> webSites = rcUserFeignClient.getWebSites(account.getId());

            for (WebSite webSite : webSites) {
                SimpleServiceMessage message = new SimpleServiceMessage();
                message.setParams(new HashMap<>());
                message.setAccountId(account.getId());
                message.addParam("resourceId", webSite.getId());
                message.addParam("switchedOn", state);

                businessActionBuilder.build(BusinessActionType.WEB_SITE_UPDATE_RC, message);

                String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") + " сайта '" + webSite.getName() + "'";
                saveHistoryForOperatorService(account, historyMessage);
            }

        } catch (Exception e) {
            logger.error("account WebSite switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }

        try {

            List<DatabaseUser> databaseUsers = rcUserFeignClient.getDatabaseUsers(account.getId());

            for (DatabaseUser databaseUser : databaseUsers) {
                SimpleServiceMessage message = new SimpleServiceMessage();
                message.setParams(new HashMap<>());
                message.setAccountId(account.getId());
                message.addParam("resourceId", databaseUser.getId());
                message.addParam("switchedOn", state);

                businessActionBuilder.build(BusinessActionType.DATABASE_USER_UPDATE_RC, message);

                String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") + " пользователя базы данных '" + databaseUser.getName() + "'";
                saveHistoryForOperatorService(account, historyMessage);
            }

        } catch (Exception e) {
            logger.error("account DatabaseUsers switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }

        try {

            Collection<Mailbox> mailboxes = rcUserFeignClient.getMailboxes(account.getId());

            for (Mailbox mailbox : mailboxes) {
                SimpleServiceMessage message = new SimpleServiceMessage();
                message.setParams(new HashMap<>());
                message.setAccountId(account.getId());
                message.addParam("resourceId", mailbox.getId());
                message.addParam("switchedOn", state);

                businessActionBuilder.build(BusinessActionType.MAILBOX_UPDATE_RC, message);

                String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") + " почтового ящика '" + mailbox.getName() + "'";
                saveHistoryForOperatorService(account, historyMessage);
            }

        } catch (Exception e) {
            logger.error("account Mailboxes switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }

        try {

            List<Domain> domains = rcUserFeignClient.getDomains(account.getId());

            for (Domain domain : domains) {
                SimpleServiceMessage message = new SimpleServiceMessage();
                message.setParams(new HashMap<>());
                message.setAccountId(account.getId());
                message.addParam("resourceId", domain.getId());
                message.addParam("switchedOn", state);

                businessActionBuilder.build(BusinessActionType.DOMAIN_UPDATE_RC, message);

                String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") + " домена '" + domain.getName() + "'";
                saveHistoryForOperatorService(account, historyMessage);
            }

        } catch (Exception e) {
            logger.error("account Domains switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }

        try {

            List<FTPUser> ftpUsers = rcUserFeignClient.getFTPUsers(account.getId());

            for (FTPUser ftpUser : ftpUsers) {
                SimpleServiceMessage message = new SimpleServiceMessage();
                message.setParams(new HashMap<>());
                message.setAccountId(account.getId());
                message.addParam("resourceId", ftpUser.getId());
                message.addParam("switchedOn", state);

                businessActionBuilder.build(BusinessActionType.FTP_USER_UPDATE_RC, message);

                String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") + " FTP-пользователя '" + ftpUser.getName() + "'";
                saveHistoryForOperatorService(account, historyMessage);
            }

        } catch (Exception e) {
            logger.error("account FTPUsers switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }

        try {

            Collection<UnixAccount> unixAccounts = rcUserFeignClient.getUnixAccounts(account.getId());

            for (UnixAccount unixAccount : unixAccounts) {
                SimpleServiceMessage message = new SimpleServiceMessage();
                message.setParams(new HashMap<>());
                message.setAccountId(account.getId());
                message.addParam("resourceId", unixAccount.getId());
                message.addParam("switchedOn", state);

                businessActionBuilder.build(BusinessActionType.UNIX_ACCOUNT_UPDATE_RC, message);

                String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") + " UNIX-аккаунта '" + unixAccount.getName() + "'";
                saveHistoryForOperatorService(account, historyMessage);
            }

        } catch (Exception e) {
            logger.error("account UnixAccounts switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    public void setWritableForAccountQuotaServices(PersonalAccount account, Boolean state) {

        try {
            Collection<UnixAccount> unixAccounts = rcUserFeignClient.getUnixAccounts(account.getId());

            for (UnixAccount unixAccount : unixAccounts) {
                setWritableForUnixAccount(account, unixAccount, state);
            }

        } catch (Exception e) {
            logger.error("account UnixAccounts writable switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }

        try {

            Collection<Mailbox> mailboxes = rcUserFeignClient.getMailboxes(account.getId());

            for (Mailbox mailbox : mailboxes) {
                setWritableForMailbox(account, mailbox, state);
            }

        } catch (Exception e) {
            logger.error("account Mailbox writable switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }

        try {

            Collection<Database> databases = rcUserFeignClient.getDatabases(account.getId());

            for (Database database : databases) {
                setWritableForDatabase(account, database, state);
            }

        } catch (Exception e) {
            logger.error("account Database writable switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    public void updateUnixAccountQuota(PersonalAccount account, Long quotaInBytes) {
        try {

            Collection<UnixAccount> unixAccounts = rcUserFeignClient.getUnixAccounts(account.getId());

            for (UnixAccount unixAccount : unixAccounts) {
                if (!unixAccount.getQuota().equals(quotaInBytes)) {
                    SimpleServiceMessage message = new SimpleServiceMessage();
                    message.setParams(new HashMap<>());
                    message.setAccountId(account.getId());
                    message.addParam("resourceId", unixAccount.getId());
                    message.addParam("quota", quotaInBytes);

                    businessActionBuilder.build(BusinessActionType.UNIX_ACCOUNT_UPDATE_RC, message);

                    String historyMessage = "Отправлена заявка на установку новой квоты в значение '" + quotaInBytes +
                            " байт' для UNIX-аккаунта '" + unixAccount.getName() + "'";
                    saveHistoryForOperatorService(account, historyMessage);
                }
            }

        } catch (Exception e) {
            logger.error("account UnixAccounts set quota failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    public void disableAllSslCertificates(PersonalAccount account) {

        Collection<SSLCertificate> sslCertificates = rcUserFeignClient.getSSLCertificates(account.getId());

        for (SSLCertificate sslCertificate : sslCertificates) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", sslCertificate.getId());

            businessActionBuilder.build(BusinessActionType.SSL_CERTIFICATE_DELETE_RC, message);

            String historyMessage = "Отправлена заявка на выключение SSL сертификата '" + sslCertificate.getName() + "'";
            saveHistoryForOperatorService(account, historyMessage);
        }

    }

    /*
     * получим стоимость абонемента на период через Plan, PlanId или PersonalAccount
     * @period - период действия абонемента, "P1Y" - на год
     */

    public BigDecimal getCostAbonement(PersonalAccount account) {
        return getCostAbonement(account.getPlanId(), "P1Y");
    }

    public BigDecimal getCostAbonement(PersonalAccount account, String period) {
        return getCostAbonement(account.getPlanId(), period);
    }

    public BigDecimal getCostAbonement(String planId) {
        return getCostAbonement(planId, "P1Y");
    }

    public BigDecimal getCostAbonement(String planId, String period) {
        Plan plan = planRepository.findOne(planId);
        return getCostAbonement(plan, period);
    }

    public BigDecimal getCostAbonement(Plan plan) {
        return this.getCostAbonement(plan, "P1Y");
    }

    public BigDecimal getCostAbonement(Plan plan, String period) {
        return plan.getAbonements()
                .stream().filter(
                        abonement -> abonement.getPeriod().equals(period)
                ).collect(Collectors.toList()).get(0).getService().getCost();
    }

    public boolean hasActiveAbonement(String accountId) {
        PersonalAccount account = accountManager.findOne(accountId);
        return !accountAbonementManager.findByPersonalAccountIdAndExpiredAfter(account.getId(), LocalDateTime.now()).isEmpty();
    }

    public List<Quotable> getQuotableResources(PersonalAccount account) {
        List<Quotable> quotableResources = new ArrayList<>();

        try {
            Collection<UnixAccount> unixAccounts = rcUserFeignClient.getUnixAccounts(account.getId());
            quotableResources.addAll(unixAccounts);
        } catch (Exception e) {
            logger.error("get unixAccounts failed for accountId: " + account.getId());
            e.printStackTrace();
        }

        try {
            Collection<Mailbox> mailboxes = rcUserFeignClient.getMailboxes(account.getId());
            quotableResources.addAll(mailboxes);
        } catch (Exception e) {
            logger.error("get Mailbox failed for accountId: " + account.getId());
            e.printStackTrace();
        }

        try {
            Collection<Database> databases = rcUserFeignClient.getDatabases(account.getId());
            quotableResources.addAll(databases);
        } catch (Exception e) {
            logger.error("get Database failed for accountId: " + account.getId());
            e.printStackTrace();
        }
        return quotableResources;
    }

    public List<Quotable> filterQuotableResoursesByWritableState(List<Quotable> quotableResources, boolean state) {
        return quotableResources.stream().filter(
                quotableResource -> (quotableResource.getWritable() == state)
        ).collect(Collectors.toList());
    }

    public void setWritableForAccountQuotaServicesByList(PersonalAccount account, Boolean state, List<Quotable> resourses) {

        for (Quotable resource: resourses) {
            try {
                if (resource instanceof UnixAccount) {

                    setWritableForUnixAccount(account, (UnixAccount) resource, state);

                } else if (resource instanceof Mailbox) {

                    setWritableForMailbox(account, (Mailbox) resource, state);

                } else if (resource instanceof Database) {

                    setWritableForDatabase(account, (Database) resource, state);

                } else {

                    logger.error("can't cast resource [" + resource + "] for accountId: " + account.getId());
                }
            } catch (Exception e) {
                logger.error("account resource [" + resource + "] writable switch failed for accountId: " + account.getId());
                e.printStackTrace();
            }
        }
    }

    public void setWritableForUnixAccount(PersonalAccount account, UnixAccount unixAccount, boolean state) {

        try {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", unixAccount.getId());
            message.addParam("writable", state);

            businessActionBuilder.build(BusinessActionType.UNIX_ACCOUNT_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") +
                    " возможности записывать данные (writable) для UNIX-аккаунта '" + unixAccount.getName() + "'";
            saveHistoryForOperatorService(account, historyMessage);

        } catch (Exception e) {
            logger.error("account unixAccount [" + unixAccount.getId() + "] writable switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    public void setWritableForMailbox(PersonalAccount account, Mailbox mailbox, boolean state) {

        try {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", mailbox.getId());
            message.addParam("writable", state);

            businessActionBuilder.build(BusinessActionType.MAILBOX_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") +
                    " возможности сохранять письма (writable) для почтового ящика '" + mailbox.getName() + "'";
            saveHistoryForOperatorService(account, historyMessage);


        } catch (Exception e) {
            logger.error("account Mailbox [" + mailbox.getId() + "] writable switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    public void setWritableForDatabase(PersonalAccount account, Database database, Boolean state) {

        try {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", database.getId());
            message.addParam("writable", state);

            businessActionBuilder.build(BusinessActionType.DATABASE_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") +
                    " возможности записывать данные (writable) для базы данных '" + database.getName() + "'";
            saveHistoryForOperatorService(account, historyMessage);

        } catch (Exception e) {
            logger.error("account Database [" + database.getName() + "] writable switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    public void saveHistoryForOperatorService(PersonalAccount account, String message) {
        this.saveHistory(account, message, "service");
    }

    public void saveHistory(PersonalAccount account, String message, String operator) {

        Map<String, String> paramsHistory = new HashMap<>();
        paramsHistory.put(HISTORY_MESSAGE_KEY, message);
        paramsHistory.put(OPERATOR_KEY, "service");

        publisher.publishEvent(new AccountHistoryEvent(account.getId(), paramsHistory));

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
            accountManager.setCreditActivationDate(account.getId(), LocalDateTime.now());
        }
    }

    /*
     *  Ставит active=false в accountService
     *  Если это доп.квота, то кидает ивент на пересчет квоты
     *  Остальные услуги, если требуеются  нужно добавлять индивидуально
     */

    public void disableAdditionalService(AccountService accountService) {
        PersonalAccount account = accountManager.findOne(accountService.getPersonalAccountId());
        accountServiceHelper.disableAccountService(account, accountService.getServiceId());
        String paymentServiceOldId = accountService.getPaymentService().getOldId();
        if (paymentServiceOldId.equals(ADDITIONAL_QUOTA_100_SERVICE_ID)) {
            account.setAddQuotaIfOverquoted(false);
            publisher.publishEvent(new AccountCheckQuotaEvent(account.getId()));
        } else if (paymentServiceOldId.equals(ANTI_SPAM_SERVICE_ID)) {
            try {
                switchOffAntiSpamForMailboxes(account);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Switch account Mailboxes anti-spam failed");
            }
//        } else if (paymentService.getId().equals(smsPaymentService.getId())) {
//            Для SMS достаточно выключать сервис
//            TODO надо сделать выключение для остальных дополнительных услуг, типа доп ftp
        }
        this.saveHistoryForOperatorService(account, "Услуга " + accountService.getPaymentService().getName() + " отключена в связи с нехваткой средств.");
    }
}
