package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
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

    @Autowired
    public AccountHelper(
            RcUserFeignClient rcUserFeignClient,
            FinFeignClient finFeignClient,
            SiFeignClient siFeignClient,
            AccountPromotionRepository accountPromotionRepository,
            BusinessActionBuilder businessActionBuilder,
            PersonalAccountRepository personalAccountRepository
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.finFeignClient = finFeignClient;
        this.siFeignClient = siFeignClient;
        this.accountPromotionRepository = accountPromotionRepository;
        this.businessActionBuilder = businessActionBuilder;
        this.personalAccountRepository = personalAccountRepository;
    }

    public String getEmail(PersonalAccount account) {
        String clientEmails = "";

        Person person = null;
        if (account.getOwnerPersonId() != null) {
            try {
                person = rcUserFeignClient.getPerson(account.getId(), account.getOwnerPersonId());
            } catch (Exception e) {
                e.printStackTrace();
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
            throw new LowBalanceException("Account balance is lower than zero. balance is: "
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
        paymentOperation.put("forceCharge", true);

        SimpleServiceMessage response = null;

        try {
            response = finFeignClient.charge(account.getId(), paymentOperation);
        } catch (Exception e) {
            e.printStackTrace();
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
        }
    }

    public void switchAccountResources(PersonalAccount account, Boolean state) {

        account.setActive(state);
        if (!state) {
            account.setDeactivated(LocalDateTime.now());
        } else {
            account.setDeactivated(null);
        }
        personalAccountRepository.save(account);

        try {

            List<WebSite> webSites = rcUserFeignClient.getWebSites(account.getId());

            for (WebSite webSite : webSites) {
                SimpleServiceMessage message = new SimpleServiceMessage();
                message.addParam("resourceId", webSite.getId());
                message.setAccountId(account.getId());
                message.setParams(new HashMap<>());
                message.addParam("switchedOn", state);

                businessActionBuilder.build(BusinessActionType.WEB_SITE_UPDATE_RC, message);
            }

        } catch (Exception e) {
            logger.debug("account WebSite switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }

        try {

            List<DatabaseUser> databaseUsers = rcUserFeignClient.getDatabaseUsers(account.getId());

            for (DatabaseUser databaseUser : databaseUsers) {
                SimpleServiceMessage message = new SimpleServiceMessage();
                message.addParam("resourceId", databaseUser.getId());
                message.setAccountId(account.getId());
                message.setParams(new HashMap<>());
                message.addParam("switchedOn", state);

                businessActionBuilder.build(BusinessActionType.DATABASE_USER_UPDATE_RC, message);
            }

        } catch (Exception e) {
            logger.debug("account DatabaseUsers switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }

        try {

            List<Mailbox> mailboxes = rcUserFeignClient.getMailboxes(account.getId());

            for (Mailbox mailbox : mailboxes) {
                SimpleServiceMessage message = new SimpleServiceMessage();
                message.addParam("resourceId", mailbox.getId());
                message.setAccountId(account.getId());
                message.setParams(new HashMap<>());
                message.addParam("switchedOn", state);

                businessActionBuilder.build(BusinessActionType.MAILBOX_UPDATE_RC, message);
            }

        } catch (Exception e) {
            logger.debug("account Mailboxes switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }

        try {

            List<Domain> domains = rcUserFeignClient.getDomains(account.getId());

            for (Domain domain : domains) {
                SimpleServiceMessage message = new SimpleServiceMessage();
                message.addParam("resourceId", domain.getId());
                message.setAccountId(account.getId());
                message.setParams(new HashMap<>());
                message.addParam("switchedOn", state);

                businessActionBuilder.build(BusinessActionType.DOMAIN_UPDATE_RC, message);
            }

        } catch (Exception e) {
            logger.debug("account Domains switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }

        try {

            List<FTPUser> ftpUsers = rcUserFeignClient.getFTPUsers(account.getId());

            for (FTPUser ftpUser : ftpUsers) {
                SimpleServiceMessage message = new SimpleServiceMessage();
                message.addParam("resourceId", ftpUser.getId());
                message.setAccountId(account.getId());
                message.setParams(new HashMap<>());
                message.addParam("switchedOn", state);

                businessActionBuilder.build(BusinessActionType.FTP_USER_UPDATE_RC, message);
            }

        } catch (Exception e) {
            logger.debug("account FTPUsers switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }

        try {

            List<UnixAccount> unixAccounts = rcUserFeignClient.getUnixAccounts(account.getId());

            for (UnixAccount unixAccount : unixAccounts) {
                SimpleServiceMessage message = new SimpleServiceMessage();
                message.addParam("resourceId", unixAccount.getId());
                message.setAccountId(account.getId());
                message.setParams(new HashMap<>());
                message.addParam("switchedOn", state);

                businessActionBuilder.build(BusinessActionType.UNIX_ACCOUNT_UPDATE_RC, message);
            }

        } catch (Exception e) {
            logger.debug("account UnixAccounts switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }
}
