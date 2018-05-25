package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountCheckQuotaEvent;
import ru.majordomo.hms.personmgr.exception.BaseException;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.NotEnoughMoneyException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.*;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.AccountServiceExpiration;
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
    private final BusinessHelper businessHelper;
    private final PersonalAccountManager accountManager;
    private final ApplicationEventPublisher publisher;
    private final AccountPromocodeRepository accountPromocodeRepository;
    private final PromocodeRepository promocodeRepository;
    private final AccountOwnerManager accountOwnerManager;
    private final PlanRepository planRepository;
    private final AccountAbonementManager accountAbonementManager;
    private final AccountServiceHelper accountServiceHelper;
    private final AccountHistoryManager history;
    private final PlanManager planManager;
    private final AccountStatHelper accountStatHelper;

    @Autowired
    public AccountHelper(
            RcUserFeignClient rcUserFeignClient,
            FinFeignClient finFeignClient,
            SiFeignClient siFeignClient,
            AccountPromotionManager accountPromotionManager,
            BusinessHelper businessHelper,
            PersonalAccountManager accountManager,
            ApplicationEventPublisher publisher,
            AccountPromocodeRepository accountPromocodeRepository,
            PromocodeRepository promocodeRepository,
            AccountOwnerManager accountOwnerManager,
            PlanRepository planRepository,
            AccountAbonementManager accountAbonementManager,
            AccountServiceHelper accountServiceHelper,
            AccountHistoryManager history,
            PlanManager planManager,
            AccountStatHelper accountStatHelper
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.finFeignClient = finFeignClient;
        this.siFeignClient = siFeignClient;
        this.accountPromotionManager = accountPromotionManager;
        this.businessHelper = businessHelper;
        this.accountManager = accountManager;
        this.publisher = publisher;
        this.accountPromocodeRepository = accountPromocodeRepository;
        this.promocodeRepository = promocodeRepository;
        this.accountOwnerManager = accountOwnerManager;
        this.planRepository = planRepository;
        this.accountAbonementManager = accountAbonementManager;
        this.accountServiceHelper = accountServiceHelper;
        this.history = history;
        this.planManager = planManager;
        this.accountStatHelper = accountStatHelper;
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

    public AccountOwner getOwner(PersonalAccount account){
        return accountOwnerManager.findOneByPersonalAccountId(account.getId());
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
            throw new NotEnoughMoneyException("Баланс аккаунта отрицательный: " + formatBigDecimalWithCurrency(available),
                    available
            );
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
            throw new NotEnoughMoneyException("Баланс аккаунта недостаточен для заказа услуги. " +
                    "Текущий баланс: " + formatBigDecimalWithCurrency(available) +
                    ", стоимость услуги: " + formatBigDecimalWithCurrency(service.getCost()),
                    service.getCost().subtract(available)
            );
        }
    }

    /**
     * @param account Аккаунт
     */
    public void checkBalanceWithoutBonus(PersonalAccount account, PaymentService service) {

        BigDecimal available = getBalance(account);

        BigDecimal bonusBalanceAvailable = getBonusBalance(account.getId());

        if (available.subtract(bonusBalanceAvailable).compareTo(service.getCost()) < 0) {
            throw new NotEnoughMoneyException("Бонусные средства недоступны для этой операции. " +
                    "Текущий баланс без учёта бонусных средств: " + formatBigDecimalWithCurrency(available.subtract(bonusBalanceAvailable)) +
                    ", стоимость услуги: " + formatBigDecimalWithCurrency(service.getCost()),
                    service.getCost().subtract(available.subtract(bonusBalanceAvailable))
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
            BigDecimal dayCost = getDayCostByService(service);

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

    public BigDecimal getDayCostByService(PaymentService service, LocalDateTime chargeDate) {
        Integer daysInCurrentMonth = chargeDate.toLocalDate().lengthOfMonth();

        return service.getCost().divide(BigDecimal.valueOf(daysInCurrentMonth), 4, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal getDayCostByService(PaymentService service) {
        LocalDateTime chargeDate = LocalDateTime.now();

        return getDayCostByService(service, chargeDate);
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

            history.saveForOperatorService(account, "Добавлен бонус " + accountPromotion.getPromotion().getName());
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
            history.saveForOperatorService(account, "Аккаунт " + (state ? "включен" : "выключен"));

            accountManager.setActive(account.getId(), state);
            switchAccountResources(account, state);
        }
    }

    public void switchAntiSpamForMailboxes(PersonalAccount account, Boolean state) {

        Collection<Mailbox> mailboxes = rcUserFeignClient.getMailboxes(account.getId());

        for (Mailbox mailbox : mailboxes) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", mailbox.getId());
            message.addParam("antiSpamEnabled", state);

            businessHelper.buildAction(BusinessActionType.MAILBOX_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на" + (state ? "включение" : "отключение") + "анти-спама у почтового ящика '"
                    + mailbox.getFullName() + "' в связи с " + (state ? "включением" : "отключением") + " услуги";
            history.saveForOperatorService(account, historyMessage);
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

                businessHelper.buildAction(BusinessActionType.WEB_SITE_UPDATE_RC, message);

                String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") + " сайта '" + webSite.getName() + "'";
                history.saveForOperatorService(account, historyMessage);
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

                businessHelper.buildAction(BusinessActionType.DATABASE_USER_UPDATE_RC, message);

                String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") + " пользователя базы данных '" + databaseUser.getName() + "'";
                history.saveForOperatorService(account, historyMessage);
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

                businessHelper.buildAction(BusinessActionType.MAILBOX_UPDATE_RC, message);

                String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") + " почтового ящика '" + mailbox.getFullName() + "'";
                history.saveForOperatorService(account, historyMessage);
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

                businessHelper.buildAction(BusinessActionType.DOMAIN_UPDATE_RC, message);

                String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") + " домена '" + domain.getName() + "'";
                history.saveForOperatorService(account, historyMessage);
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

                businessHelper.buildAction(BusinessActionType.FTP_USER_UPDATE_RC, message);

                String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") + " FTP-пользователя '" + ftpUser.getName() + "'";
                history.saveForOperatorService(account, historyMessage);
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

                businessHelper.buildAction(BusinessActionType.UNIX_ACCOUNT_UPDATE_RC, message);

                String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") + " UNIX-аккаунта '" + unixAccount.getName() + "'";
                history.saveForOperatorService(account, historyMessage);
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

                    businessHelper.buildAction(BusinessActionType.UNIX_ACCOUNT_UPDATE_RC, message);

                    String historyMessage = "Отправлена заявка на установку новой квоты в значение '" + quotaInBytes +
                            " байт' для UNIX-аккаунта '" + unixAccount.getName() + "'";
                    history.saveForOperatorService(account, historyMessage);
                }
            }

        } catch (Exception e) {
            logger.error("account UnixAccounts set quota failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    public void deleteAllSslCertificates(PersonalAccount account) {
        Collection<SSLCertificate> sslCertificates = rcUserFeignClient.getSSLCertificates(account.getId());

        for (SSLCertificate sslCertificate : sslCertificates) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", sslCertificate.getId());

            businessHelper.buildAction(BusinessActionType.SSL_CERTIFICATE_DELETE_RC, message);

            String historyMessage = "Отправлена заявка на удаление SSL сертификата '" + sslCertificate.getName() + "'";
            history.saveForOperatorService(account, historyMessage);
        }
    }

    public void deleteAllMailboxes(PersonalAccount account) {
        Collection<Mailbox> mailboxes = rcUserFeignClient.getMailboxes(account.getId());

        for (Mailbox mailbox : mailboxes) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", mailbox.getId());

            businessHelper.buildAction(BusinessActionType.MAILBOX_DELETE_RC, message);

            String historyMessage = "Отправлена заявка на удаление почтового ящика '" + mailbox.getName() + "'";
            history.saveForOperatorService(account, historyMessage);
        }
    }

    public void deleteAllDatabases(PersonalAccount account) {
        Collection<Database> databases = rcUserFeignClient.getDatabases(account.getId());

        for (Database database : databases) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", database.getId());

            businessHelper.buildAction(BusinessActionType.DATABASE_DELETE_RC, message);

            String historyMessage = "Отправлена заявка на удаление базы данных '" + database.getName() + "'";
            history.saveForOperatorService(account, historyMessage);
        }
    }

    public void deleteAllDatabaseUsers(PersonalAccount account) {
        Collection<DatabaseUser> databaseUsers = rcUserFeignClient.getDatabaseUsers(account.getId());

        for (DatabaseUser databaseUser : databaseUsers) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", databaseUser.getId());

            businessHelper.buildAction(BusinessActionType.DATABASE_USER_DELETE_RC, message);

            String historyMessage = "Отправлена заявка на удаление пользователя баз данных '" + databaseUser.getName() + "'";
            history.saveForOperatorService(account, historyMessage);
        }
    }

    public void disableAndScheduleDeleteForAllMailboxes(PersonalAccount account) {
        Collection<Mailbox> mailboxes = rcUserFeignClient.getMailboxes(account.getId());

        for (Mailbox mailbox : mailboxes) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", mailbox.getId());
            message.addParam("switchedOn", false);
            message.addParam("willBeDeletedAfter", LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            businessHelper.buildAction(BusinessActionType.MAILBOX_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на выключение и отложенное удаление почтового ящика '" + mailbox.getName() + "'";
            history.saveForOperatorService(account, historyMessage);
        }
    }

    public void disableAndScheduleDeleteForAllDatabases(PersonalAccount account) {
        Collection<Database> databases = rcUserFeignClient.getDatabases(account.getId());

        for (Database database : databases) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", database.getId());
            message.addParam("switchedOn", false);
            message.addParam("willBeDeletedAfter", LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            businessHelper.buildAction(BusinessActionType.DATABASE_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на выключение и отложенное удаление базы данных '" + database.getName() + "'";
            history.saveForOperatorService(account, historyMessage);
        }
    }

    public void disableAndScheduleDeleteForAllDatabaseUsers(PersonalAccount account) {
        Collection<DatabaseUser> databaseUsers = rcUserFeignClient.getDatabaseUsers(account.getId());

        for (DatabaseUser databaseUser : databaseUsers) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", databaseUser.getId());
            message.addParam("switchedOn", false);
            message.addParam("willBeDeletedAfter", LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            businessHelper.buildAction(BusinessActionType.DATABASE_USER_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на выключение и отложенное удаление пользователя баз данных '" + databaseUser.getName() + "'";
            history.saveForOperatorService(account, historyMessage);
        }
    }

    public void unScheduleDeleteForAllMailboxes(PersonalAccount account) {
        Collection<Mailbox> mailboxes = rcUserFeignClient.getMailboxes(account.getId());

        for (Mailbox mailbox : mailboxes) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", mailbox.getId());
            message.addParam("willBeDeletedAfter", null);

            businessHelper.buildAction(BusinessActionType.MAILBOX_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на отмену отложенного удаления почтового ящика '" + mailbox.getName() + "'";
            history.saveForOperatorService(account, historyMessage);
        }
    }

    public void unScheduleDeleteForAllDatabases(PersonalAccount account) {
        Collection<Database> databases = rcUserFeignClient.getDatabases(account.getId());

        for (Database database : databases) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", database.getId());
            message.addParam("willBeDeletedAfter", null);

            businessHelper.buildAction(BusinessActionType.DATABASE_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на отмену отложенного удаления базы данных '" + database.getName() + "'";
            history.saveForOperatorService(account, historyMessage);
        }
    }

    public void unScheduleDeleteForAllDatabaseUsers(PersonalAccount account) {
        Collection<DatabaseUser> databaseUsers = rcUserFeignClient.getDatabaseUsers(account.getId());

        for (DatabaseUser databaseUser : databaseUsers) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", databaseUser.getId());
            message.addParam("willBeDeletedAfter", null);

            businessHelper.buildAction(BusinessActionType.DATABASE_USER_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на отмену отложенного удаления пользователя баз данных '" + databaseUser.getName() + "'";
            history.saveForOperatorService(account, historyMessage);
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

            businessHelper.buildAction(BusinessActionType.UNIX_ACCOUNT_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") +
                    " возможности записывать данные (writable) для UNIX-аккаунта '" + unixAccount.getName() + "'";
            history.saveForOperatorService(account, historyMessage);

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

            businessHelper.buildAction(BusinessActionType.MAILBOX_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") +
                    " возможности сохранять письма (writable) для почтового ящика '" + mailbox.getFullName() + "'";
            history.saveForOperatorService(account, historyMessage);


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

            businessHelper.buildAction(BusinessActionType.DATABASE_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") +
                    " возможности записывать данные (writable) для базы данных '" + database.getName() + "'";
            history.saveForOperatorService(account, historyMessage);

        } catch (Exception e) {
            logger.error("account Database [" + database.getName() + "] writable switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
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
            history.saveForOperatorService(account, "Установлена дата активации кредита на " + now);
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
                switchAntiSpamForMailboxes(account, false);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Switch account Mailboxes anti-spam failed");
            }
//        } else if (paymentService.getId().equals(smsPaymentService.getId())) {
//            Для SMS достаточно выключать сервис
//            TODO надо сделать выключение для остальных дополнительных услуг, типа доп ftp
        }
        history.saveForOperatorService(account, "Услуга " + accountService.getPaymentService().getName() + " отключена в связи с нехваткой средств.");
    }

    public Boolean isExpirationServiceNeedProlong(AccountServiceExpiration expiration) {
        return expiration.getAccountService().getPaymentService().getPaymentType() == ServicePaymentType.ONE_TIME
                && expiration.getAutoRenew()
                //Автопродление за 5 дней до и 5 дней после выключения
                && expiration.getExpireDate().isAfter(LocalDate.now().minusDays(5L))
                && expiration.getExpireDate().isBefore(LocalDate.now().plusDays(5L));
    }

    //На тарифах, дешевле 245р, не даём покупать и продлевать абонемент
    public Boolean isAbonementMinCostOrderAllowed(PersonalAccount account) {
        Plan plan = planManager.findOne(account.getPlanId());
        if (!plan.isActive() && !plan.isAbonementOnly()
                && plan.getService().getCost().compareTo(BigDecimal.valueOf(PLAN_MIN_COST_TO_ORDER_ABONEMENT)) < 0) {
            return false;
        }
        return true;
    }

    public boolean needChangeArchivePlanToUnlimitedPlan(PersonalAccount account) {
        Plan plan = planManager.findOne(account.getPlanId());
        if (plan.isActive()) {
            return false;
        } else if (plan.getService().getCost().compareTo(getArchivalFallbackPlan().getService().getCost()) < 0) {
            return true;
        } else {
            return false;
        }
    }

    public Plan getArchivalFallbackPlan() {
        return planManager.findByOldId(String.valueOf(PLAN_UNLIMITED_ID));
    }

    public Plan getPlan(PersonalAccount account) {
        return planManager.findOne(account.getPlanId());
    }

    public void changeArchivalPlanToActive(PersonalAccount account, AccountAbonement accountAbonement) {
        accountAbonementManager.delete(accountAbonement);
        accountStatHelper.abonementDelete(accountAbonement);
        changeArchivalPlanToActive(account);
    }

    public void changeArchivalPlanToActive(PersonalAccount account) {
        Plan currentPlan = getPlan(account);
        accountServiceHelper.deleteAccountServiceByServiceId(account, currentPlan.getServiceId());

        Plan fallbackPlan = getArchivalFallbackPlan();
        accountManager.setPlanId(account.getId(), fallbackPlan.getId());
        accountServiceHelper.addAccountService(account, fallbackPlan.getServiceId());
        accountStatHelper.archivalPlanChange(account.getId(), currentPlan.getId(), fallbackPlan.getId());
    }
}
