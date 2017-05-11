package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.exception.ChargeException;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.LowBalanceException;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountPromotionRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.rc.user.resources.*;

import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.PASSWORD_KEY;

@Service
public class AccountHelper {

    private final static Logger logger = LoggerFactory.getLogger(AccountHelper.class);

    private final RcUserFeignClient rcUserFeignClient;
    private final FinFeignClient finFeignClient;
    private final SiFeignClient siFeignClient;
    private final AccountPromotionRepository accountPromotionRepository;
    private final BusinessActionBuilder businessActionBuilder;
    private final PersonalAccountRepository personalAccountRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public AccountHelper(
            RcUserFeignClient rcUserFeignClient,
            FinFeignClient finFeignClient,
            SiFeignClient siFeignClient,
            AccountPromotionRepository accountPromotionRepository,
            BusinessActionBuilder businessActionBuilder,
            PersonalAccountRepository personalAccountRepository,
            ApplicationEventPublisher publisher
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.finFeignClient = finFeignClient;
        this.siFeignClient = siFeignClient;
        this.accountPromotionRepository = accountPromotionRepository;
        this.businessActionBuilder = businessActionBuilder;
        this.personalAccountRepository = personalAccountRepository;
        this.publisher = publisher;
    }

    public String getEmail(PersonalAccount account) {
        String clientEmails = "";

        Person person = null;
        if (account.getOwnerPersonId() != null) {
            try {
                person = rcUserFeignClient.getPerson(account.getId(), account.getOwnerPersonId());
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Exception in ru.majordomo.hms.personmgr.service.AccountHelper.getEmail " + e.getMessage());
            }
        }

        if (person != null) {
            clientEmails = String.join(", ", person.getEmailAddresses());
        }

        return clientEmails;
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
            logger.error("Exception in ru.majordomo.hms.personmgr.service.AccountHelper.getBalance #1 " + e.getMessage());
        }

        if (balance == null) {
            throw new ResourceNotFoundException("Account balance not found.");
        }

        BigDecimal available;

        try {
            if (balance.get("available") instanceof Integer) {
                available = BigDecimal.valueOf((Integer) balance.get("available"));
            } else if (balance.get("available") instanceof Double) {
                available = BigDecimal.valueOf((Double) balance.get("available"));
            } else {
                available = (BigDecimal) balance.get("available");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.service.AccountHelper.getBalance #2 " + e.getMessage());
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
            logger.error("Exception in ru.majordomo.hms.personmgr.service.AccountHelper.getDomains " + e.getMessage());
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
                    + available.toPlainString());
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
            throw new LowBalanceException("Account balance is too low for specified service. " +
                    "Current balance is: " + available.toPlainString() + " service cost is: " + service.getCost());
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
                throw new LowBalanceException("Account balance is too low for specified service. " +
                        "Current balance is: " + available.toPlainString() + " service oneDayCost is: " + dayCost);
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

    //TODO на самом деле сюда ещё должна быть возможность передать discountedService
    public SimpleServiceMessage charge(PersonalAccount account, PaymentService service) {
        BigDecimal amount = service.getCost();

        return charge(account, service, amount, false);
    }

    public SimpleServiceMessage charge(PersonalAccount account, PaymentService service, BigDecimal amount) {
        return charge(account, service, amount, false);
    }

    //TODO на самом деле сюда ещё должна быть возможность передать discountedService
    public SimpleServiceMessage charge(PersonalAccount account, PaymentService service, BigDecimal amount, Boolean forceCharge) {
        Map<String, Object> paymentOperation = new HashMap<>();
        paymentOperation.put("serviceId", service.getId());
        paymentOperation.put("amount", amount);
        paymentOperation.put("forceCharge", forceCharge);

        SimpleServiceMessage response = null;

        try {
            response = finFeignClient.charge(account.getId(), paymentOperation);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.service.AccountHelper.charge " + e.getMessage());
            throw new ChargeException("ChargeException. Error when charging money." +
                    " Service cost is: " + service.getCost());
        }

        if (response != null && (response.getParam("success") == null || !((boolean) response.getParam("success")))) {
            throw new ChargeException("Account balance is too low for specified service. " +
                    " Service cost is: " + service.getCost());
        }

        return response;
    }

    //TODO на самом деле сюда ещё должна быть возможность передать discountedService
    public SimpleServiceMessage block(PersonalAccount account, PaymentService service) {
        Map<String, Object> paymentOperation = new HashMap<>();
        paymentOperation.put("serviceId", service.getId());
        paymentOperation.put("amount", service.getCost());

        SimpleServiceMessage response = null;
        try {
            response = finFeignClient.block(account.getId(), paymentOperation);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.service.AccountHelper.block " + e.getMessage());
            throw new ChargeException("ChargeException. Error when blocking money." +
                    " Service cost is: " + service.getCost());
        }

        if (response != null && (response.getParam("success") == null || !((boolean) response.getParam("success")))) {
            throw new ChargeException("Account balance is too low for specified service. " +
                    " Service cost is: " + service.getCost());
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
            logger.error("Exception in ru.majordomo.hms.personmgr.service.AccountHelper.changePassword " + e.getMessage());
        }

        if (response != null && (response.getParam("success") == null || !((boolean) response.getParam("success")))) {
            throw new InternalApiException("Account password not changed. ");
        }

        return response;
    }

    public void giveGift(PersonalAccount account, Promotion promotion) {
        Long currentCount = accountPromotionRepository.countByPersonalAccountIdAndPromotionId(account.getId(), promotion.getId());
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

            accountPromotionRepository.save(accountPromotion);

            //Save history
            Map<String, String> params = new HashMap<>();
            params.put(HISTORY_MESSAGE_KEY, "Добавлен бонус " + accountPromotion.getPromotion().getName());
            params.put(OPERATOR_KEY, "service");

            publisher.publishEvent(new AccountHistoryEvent(account.getId(), params));
        }
    }

    public void switchAccountResources(PersonalAccount account, Boolean state) {

        account.setActive(state);

        //Save history
        Map<String, String> paramsHistory = new HashMap<>();
        paramsHistory.put(HISTORY_MESSAGE_KEY, "Аккаунт " + (state ? "включен" : "выключен"));
        paramsHistory.put(OPERATOR_KEY, "service");

        publisher.publishEvent(new AccountHistoryEvent(account.getId(), paramsHistory));

        if (!state) {
            if (account.getDeactivated() == null) {
                account.setDeactivated(LocalDateTime.now());
            }
        } else {
            account.setDeactivated(null);
        }
        personalAccountRepository.save(account);

        try {

            List<WebSite> webSites = rcUserFeignClient.getWebSites(account.getId());

            for (WebSite webSite : webSites) {
                SimpleServiceMessage message = new SimpleServiceMessage();
                message.setParams(new HashMap<>());
                message.setAccountId(account.getId());
                message.addParam("resourceId", webSite.getId());
                message.addParam("switchedOn", state);

                businessActionBuilder.build(BusinessActionType.WEB_SITE_UPDATE_RC, message);

                //Save history
                paramsHistory = new HashMap<>();
                paramsHistory.put(HISTORY_MESSAGE_KEY, "Отправлена заявка на " + (state ? "включение" : "выключение") + " сайта '" + webSite.getName() + "'");
                paramsHistory.put(OPERATOR_KEY, "service");

                publisher.publishEvent(new AccountHistoryEvent(account.getId(), paramsHistory));
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

                //Save history
                paramsHistory = new HashMap<>();
                paramsHistory.put(HISTORY_MESSAGE_KEY, "Отправлена заявка на " + (state ? "включение" : "выключение") + " пользователя базы данных '" + databaseUser.getName() + "'");
                paramsHistory.put(OPERATOR_KEY, "service");

                publisher.publishEvent(new AccountHistoryEvent(account.getId(), paramsHistory));
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

                //Save history
                paramsHistory = new HashMap<>();
                paramsHistory.put(HISTORY_MESSAGE_KEY, "Отправлена заявка на " + (state ? "включение" : "выключение") + " почтового ящика '" + mailbox.getName() + "'");
                paramsHistory.put(OPERATOR_KEY, "service");

                publisher.publishEvent(new AccountHistoryEvent(account.getId(), paramsHistory));
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

                //Save history
                paramsHistory = new HashMap<>();
                paramsHistory.put(HISTORY_MESSAGE_KEY, "Отправлена заявка на " + (state ? "включение" : "выключение") + " домена '" + domain.getName() + "'");
                paramsHistory.put(OPERATOR_KEY, "service");

                publisher.publishEvent(new AccountHistoryEvent(account.getId(), paramsHistory));
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

                //Save history
                paramsHistory = new HashMap<>();
                paramsHistory.put(HISTORY_MESSAGE_KEY, "Отправлена заявка на " + (state ? "включение" : "выключение") + " FTP-пользователя '" + ftpUser.getName() + "'");
                paramsHistory.put(OPERATOR_KEY, "service");

                publisher.publishEvent(new AccountHistoryEvent(account.getId(), paramsHistory));
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
                message.addParam("resourceId", unixAccount.getId());;
                message.addParam("switchedOn", state);

                businessActionBuilder.build(BusinessActionType.UNIX_ACCOUNT_UPDATE_RC, message);

                //Save history
                paramsHistory = new HashMap<>();
                paramsHistory.put(HISTORY_MESSAGE_KEY, "Отправлена заявка на " + (state ? "включение" : "выключение") + " UNIX-аккаунта '" + unixAccount.getName() + "'");
                paramsHistory.put(OPERATOR_KEY, "service");

                publisher.publishEvent(new AccountHistoryEvent(account.getId(), paramsHistory));
            }

        } catch (Exception e) {
            logger.error("account UnixAccounts switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    public void setWritableForAccountQuotaServices(PersonalAccount account, Boolean state) {
        Map<String, String> paramsHistory;

        try {
            Collection<UnixAccount> unixAccounts = rcUserFeignClient.getUnixAccounts(account.getId());

            for (UnixAccount unixAccount : unixAccounts) {
                SimpleServiceMessage message = new SimpleServiceMessage();
                message.setParams(new HashMap<>());
                message.setAccountId(account.getId());
                message.addParam("resourceId", unixAccount.getId());;
                message.addParam("writable", state);

                businessActionBuilder.build(BusinessActionType.UNIX_ACCOUNT_UPDATE_RC, message);

                //Save history
                paramsHistory = new HashMap<>();
                paramsHistory.put(HISTORY_MESSAGE_KEY, "Отправлена заявка на " + (state ? "включение" : "выключение") +
                        " возможности записывать данные (writable) для UNIX-аккаунта '" + unixAccount.getName() + "'");
                paramsHistory.put(OPERATOR_KEY, "service");

                publisher.publishEvent(new AccountHistoryEvent(account.getId(), paramsHistory));
            }

        } catch (Exception e) {
            logger.error("account UnixAccounts writable switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }

        try {

            Collection<Mailbox> mailboxes = rcUserFeignClient.getMailboxes(account.getId());

            for (Mailbox mailbox : mailboxes) {
                SimpleServiceMessage message = new SimpleServiceMessage();
                message.setParams(new HashMap<>());
                message.setAccountId(account.getId());
                message.addParam("resourceId", mailbox.getId());
                message.addParam("writable", state);

                businessActionBuilder.build(BusinessActionType.MAILBOX_UPDATE_RC, message);

                //Save history
                paramsHistory = new HashMap<>();
                paramsHistory.put(HISTORY_MESSAGE_KEY, "Отправлена заявка на " + (state ? "включение" : "выключение") +
                        " возможности сохранять письма (writable) для почтового ящика '" + mailbox.getName() + "'");
                paramsHistory.put(OPERATOR_KEY, "service");

                publisher.publishEvent(new AccountHistoryEvent(account.getId(), paramsHistory));
            }

        } catch (Exception e) {
            logger.error("account Mailbox writable switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }

        try {

            Collection<Database> databases = rcUserFeignClient.getDatabases(account.getId());

            for (Database database : databases) {
                SimpleServiceMessage message = new SimpleServiceMessage();
                message.setParams(new HashMap<>());
                message.setAccountId(account.getId());
                message.addParam("resourceId", database.getId());
                message.addParam("writable", state);

                businessActionBuilder.build(BusinessActionType.DATABASE_UPDATE_RC, message);

                //Save history
                paramsHistory = new HashMap<>();
                paramsHistory.put(HISTORY_MESSAGE_KEY, "Отправлена заявка на " + (state ? "включение" : "выключение") +
                        " возможности записывать данные (writable) для базы данных '" + database.getName() + "'");
                paramsHistory.put(OPERATOR_KEY, "service");

                publisher.publishEvent(new AccountHistoryEvent(account.getId(), paramsHistory));
            }

        } catch (Exception e) {
            logger.error("account Database writable switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    public void updateUnixAccountQuota(PersonalAccount account, Long quota) {
        try {

            Collection<UnixAccount> unixAccounts = rcUserFeignClient.getUnixAccounts(account.getId());

            for (UnixAccount unixAccount : unixAccounts) {
                SimpleServiceMessage message = new SimpleServiceMessage();
                message.setParams(new HashMap<>());
                message.setAccountId(account.getId());
                message.addParam("resourceId", unixAccount.getId());
                message.addParam("quota", quota);

                businessActionBuilder.build(BusinessActionType.UNIX_ACCOUNT_UPDATE_RC, message);

                //Save history
                Map<String, String> paramsHistory = new HashMap<>();
                paramsHistory.put(HISTORY_MESSAGE_KEY, "Отправлена заявка на установку новой квоты в значение '" + quota +
                        " байт' для UNIX-аккаунта '" + unixAccount.getName() + "'");
                paramsHistory.put(OPERATOR_KEY, "service");

                publisher.publishEvent(new AccountHistoryEvent(account.getId(), paramsHistory));
            }

        } catch (Exception e) {
            logger.error("account UnixAccounts set quota failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }
}
