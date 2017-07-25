package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import ru.majordomo.hms.personmgr.common.AvailabilityInfo;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.event.mailManager.SendSmsEvent;
import ru.majordomo.hms.personmgr.exception.DomainNotAvailableException;
import ru.majordomo.hms.personmgr.exception.LowBalanceException;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.model.cart.DomainCartItem;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.repository.PromotionRepository;
import ru.majordomo.hms.rc.user.resources.Domain;

import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.Utils.formatBigDecimalWithCurrency;

@Service
public class DomainService {
    private final static Logger logger = LoggerFactory.getLogger(DomainService.class);

    private static TemporalAdjuster THIRTY_DAYS_AFTER = TemporalAdjusters.ofDateAdjuster(date -> date.plusDays(30));
    private static TemporalAdjuster SIXTY_DAYS_AFTER = TemporalAdjusters.ofDateAdjuster(date -> date.plusDays(60));
    private static TemporalAdjuster TWENTY_NINE_DAYS_BEFORE = TemporalAdjusters.ofDateAdjuster(date -> date.minusDays(29));
    private static TemporalAdjuster THREE_DAYS_BEFORE = TemporalAdjusters.ofDateAdjuster(date -> date.minusDays(3));
    private static TemporalAdjuster FOURTEEN_DAYS_AFTER = TemporalAdjusters.ofDateAdjuster(date -> date.plusDays(14));
    private static TemporalAdjuster FIFTY_DAYS_AFTER = TemporalAdjusters.ofDateAdjuster(date -> date.plusDays(50));
    private static TemporalAdjuster TWENTY_FIVE_DAYS_BEFORE = TemporalAdjusters.ofDateAdjuster(date -> date.minusDays(25));

    private final RcUserFeignClient rcUserFeignClient;
    private final AccountHelper accountHelper;
    private final DomainTldService domainTldService;
    private final BusinessActionBuilder businessActionBuilder;
    private final ApplicationEventPublisher publisher;
    private final DomainRegistrarFeignClient domainRegistrarFeignClient;
    private final BlackListService blackListService;
    private final PersonalAccountManager accountManager;
    private final AccountPromotionManager accountPromotionManager;
    private final BusinessOperationBuilder businessOperationBuilder;
    private final PromotionRepository promotionRepository;

    @Autowired
    public DomainService(
            RcUserFeignClient rcUserFeignClient,
            AccountHelper accountHelper,
            DomainTldService domainTldService,
            BusinessActionBuilder businessActionBuilder,
            ApplicationEventPublisher publisher,
            DomainRegistrarFeignClient domainRegistrarFeignClient,
            BlackListService blackListService,
            PersonalAccountManager accountManager,
            AccountPromotionManager accountPromotionManager,
            BusinessOperationBuilder businessOperationBuilder,
            PromotionRepository promotionRepository
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.accountHelper = accountHelper;
        this.domainTldService = domainTldService;
        this.businessActionBuilder = businessActionBuilder;
        this.publisher = publisher;
        this.domainRegistrarFeignClient = domainRegistrarFeignClient;
        this.blackListService = blackListService;
        this.accountManager = accountManager;
        this.accountPromotionManager = accountPromotionManager;
        this.businessOperationBuilder = businessOperationBuilder;
        this.promotionRepository = promotionRepository;
    }

    public void processExpiringDomainsByAccount(PersonalAccount account) {
        //В итоге нам нужно получить домены которые заканчиваются через 30 дней или между 14 "до" и 3 днями "после" окончания

        //Ищем paidTill равный +30 дням от текущей даты
        LocalDate paidTillStart = LocalDate.now().with(THIRTY_DAYS_AFTER);
        LocalDate paidTillEnd = LocalDate.now().with(THIRTY_DAYS_AFTER);

        logger.debug("Trying to find all expiring domains from paidTillStart: "
                + paidTillStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + " to paidTillEnd: " + paidTillEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        List<Domain> domains = rcUserFeignClient.getExpiringDomainsByAccount(
                account.getId(),
                paidTillStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                paidTillEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        //Ищем paidTill начиная с 3 дней до текущей даты
        paidTillStart = LocalDate.now().with(THREE_DAYS_BEFORE);
        //И закакнчивая 14 днями после текущей даты
        paidTillEnd = LocalDate.now().with(FOURTEEN_DAYS_AFTER);

        logger.debug("Trying to find expiring domains from paidTillStart: "
                + paidTillStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + " to paidTillEnd: " + paidTillEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        domains.addAll(rcUserFeignClient.getExpiringDomainsByAccount(
                account.getId(),
                paidTillStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                paidTillEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        ));

        if (domains.isEmpty()) {
            logger.debug("Not found expiring domains for accountId: " + account.getId());
        }

        domains.forEach(domain -> logger.debug("We found expiring domain: " + domain));

        if (!domains.isEmpty() && account.hasNotification(MailManagerMessageType.EMAIL_DOMAIN_DELEGATION_ENDING)) {
            BigDecimal balance = accountHelper.getBalance(account);

            String domainsForMail = "";
            for (Domain domain : domains) {
                String autoRenew = domain.getAutoRenew() ? "включено" : "выключено";
                domainsForMail += String.format(
                        "%-20s - %s - %-10s<br>",
                        domain.getName(),
                        domain.getRegSpec().getPaidTill().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        autoRenew
                );
            }

            //Отправим письмо
            String email = accountHelper.getEmail(account);

            SimpleServiceMessage message = new SimpleServiceMessage();

            message.setAccountId(account.getId());
            message.setParams(new HashMap<>());
            message.addParam("email", email);
            message.addParam("api_name", "MajordomoHMSVHDomainsExpires");
            message.addParam("priority", 10);

            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("acc_id", account.getName());
            parameters.put("domains", domainsForMail);
            parameters.put("balance", formatBigDecimalWithCurrency(balance));
            parameters.put("from", "noreply@majordomo.ru");

            message.addParam("parametrs", parameters);

            publisher.publishEvent(new SendMailEvent(message));
        }
    }

    public void processDomainsAutoRenewByAccount(PersonalAccount account) {
        //Ищем paidTill начиная с 25 дней до текущей даты
        LocalDate paidTillStart = LocalDate.now().with(TWENTY_FIVE_DAYS_BEFORE);
        //И закакнчивая 50 днями после текущей даты
        LocalDate paidTillEnd = LocalDate.now().with(FIFTY_DAYS_AFTER);

        logger.debug("Trying to find domains for AutoRenew from paidTillStart: "
                + paidTillStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + " to paidTillEnd: " + paidTillEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        List<Domain> domains = rcUserFeignClient.getExpiringDomainsByAccount(
                account.getId(),
                paidTillStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                paidTillEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        if (domains.isEmpty()) {
            logger.debug("Not found expiring domains for AutoRenew for accountId: " + account.getId());
        }

        domains.forEach(domain -> logger.debug("We found domain for AutoRenew: " + domain));

        for (Domain domain : domains) {

            if (domain.getAutoRenew()) {

                BigDecimal balance = accountHelper.getBalance(account);

                DomainTld domainTld = domainTldService.findDomainTldByDomainNameAndRegistrator(domain.getName(), domain.getRegSpec().getRegistrar());

                AvailabilityInfo availabilityInfo = domainRegistrarFeignClient.getAvailabilityInfo(domain.getName());
                if (availabilityInfo.getPremiumPrice() != null && (availabilityInfo.getPremiumPrice().compareTo(BigDecimal.ZERO) > 0)) {
                    domainTld.getRenewService().setCost(availabilityInfo.getPremiumPrice());
                }
                try {
                    accountHelper.checkBalance(account, domainTld.getRenewService());
                } catch (LowBalanceException e) {
                    //Если денег не хватает
                    //Запишем попытку в историю клиента
                    Map<String, String> params = new HashMap<>();
                    params.put(HISTORY_MESSAGE_KEY, "Автоматическое продление " + domain.getName() + " невозможно, на счету " + balance + " руб.");
                    params.put(OPERATOR_KEY, "service");

                    publisher.publishEvent(new AccountHistoryEvent(account.getId(), params));

                    //Отправим письмо
                    String email = accountHelper.getEmail(account);

                    SimpleServiceMessage message = new SimpleServiceMessage();

                    message.setAccountId(account.getId());
                    message.setParams(new HashMap<>());
                    message.addParam("email", email);
                    message.addParam("api_name", "MajordomoVHNomoneyProlong");
                    message.addParam("priority", 10);

                    HashMap<String, String> parameters = new HashMap<>();
                    parameters.put("client_id", message.getAccountId());
                    parameters.put("acc_id", account.getName());
                    parameters.put("domen", domain.getName());
                    parameters.put("from", "noreply@majordomo.ru");

                    message.addParam("parametrs", parameters);

                    publisher.publishEvent(new SendMailEvent(message));

                    String smsPhone = account.getSmsPhoneNumber();

                    //Если подключено СМС-уведомление, то также отправим его
                    if (account.hasNotification(MailManagerMessageType.SMS_NO_MONEY_TO_AUTORENEW_DOMAIN)
                            && smsPhone != null
                            && !smsPhone.equals("")) {
                        message = new SimpleServiceMessage();

                        message.setAccountId(account.getId());
                        message.setParams(new HashMap<>());
                        message.addParam("phone", smsPhone);
                        message.addParam("api_name", "MajordomoNoMoneyToAutoRenewDomain");
                        message.addParam("priority", 10);

                        parameters = new HashMap<>();
                        parameters.put("client_id", message.getAccountId());
                        parameters.put("domain", domain.getName());

                        message.addParam("parametrs", parameters);

                        publisher.publishEvent(new SendSmsEvent(message));
                    }
                }

                SimpleServiceMessage blockResult = accountHelper.block(account, domainTld.getRenewService());

                String documentNumber = (String) blockResult.getParam("documentNumber");

                SimpleServiceMessage domainRenewMessage = new SimpleServiceMessage();

                domainRenewMessage.setAccountId(account.getId());
                domainRenewMessage.addParam(RESOURCE_ID_KEY, domain.getId());
                domainRenewMessage.addParam("renew", true);
                domainRenewMessage.addParam(AUTO_RENEW_KEY, true);
                domainRenewMessage.addParam("documentNumber", documentNumber);

                businessActionBuilder.build(BusinessActionType.DOMAIN_UPDATE_RC, domainRenewMessage);
            }
        }
    }

    public void check(String domainName) {
        if (blackListService.domainExistsInControlBlackList(domainName)) {
            logger.debug("domain: " + domainName + " exists in control BlackList");
            throw new DomainNotAvailableException("Домен: " + domainName + " уже присутствует в системе и не может быть добавлен.");
        }

        getDomainTld(domainName);

        getAvailabilityInfo(domainName);
    }

    public AccountPromotion usePromotion(String domainName, List<AccountPromotion> accountPromotions) {
        DomainTld domainTld = getDomainTld(domainName);

        Optional<AccountPromotion> foundAccountPromotion = Optional.empty();

        String prioritizedPromotionId = promotionRepository.findByName(FREE_DOMAIN_PROMOTION).getId();

        accountPromotions.sort((o1, o2) -> {
            if (o1.getPromotionId().equals(o2.getPromotionId())) return 0;
            if (o1.getPromotionId().equals(prioritizedPromotionId)) return -1;
            if (o2.getPromotionId().equals(prioritizedPromotionId)) return 1;
            return 0;
        });

        for (AccountPromotion accountPromotion : accountPromotions) {
            Map<String, Boolean> map = accountPromotion.getActionsWithStatus();
            foundAccountPromotion = map
                    .entrySet()
                    .stream()
                    .filter(Map.Entry::getValue)
                    .filter(stringBooleanEntry -> {
                        PromocodeAction promocodeAction = accountPromotion.getActions().get(stringBooleanEntry.getKey());
                        List<String> availableTlds = (List<String>) promocodeAction.getProperties().get("tlds");

                        return availableTlds.contains(domainTld.getTld());
                    }).map(stringBooleanEntry -> accountPromotion).findFirst();

            if (foundAccountPromotion.isPresent()) {
                map.put(foundAccountPromotion.get().getActionsWithStatus().keySet().iterator().next(), false);

                break;
            }
        }

        return foundAccountPromotion.orElse(null);
    }

    public BigDecimal getPrice(String domainName, AccountPromotion accountPromotion) {
        DomainTld domainTld;

        try {
            domainTld = getDomainTld(domainName);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Домен: " + domainName + " недоступен. Зона домена отсутствует в системе");
            return null;
        }

        //Проверить домен на доступность/премиальность
        AvailabilityInfo availabilityInfo;

        try {
            availabilityInfo = getAvailabilityInfo(domainName);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Домен: " + domainName + ". Сервис регистрации недоступен. Ошибка: " + e.getMessage());
            return null;
        }

        if (accountPromotion != null) {
            PromocodeAction actionFreeDomain = accountPromotion.getActions().get(BONUS_FREE_DOMAIN_PROMOCODE_ACTION_ID);
            PromocodeAction actionDiscountDomain = accountPromotion.getActions().get(DOMAIN_DISCOUNT_RU_RF_ACTION_ID);

            if (actionFreeDomain != null) {
                List<String> availableTlds = (List<String>) actionFreeDomain.getProperties().get("tlds");

                if (availableTlds.contains(domainTld.getTld())) {
                    return BigDecimal.ZERO;
                }
            } else if (actionDiscountDomain != null) {
                List<String> availableTlds = (List<String>) actionDiscountDomain.getProperties().get("tlds");

                if (availableTlds.contains(domainTld.getTld())) {
                    // Устанавливает цену со скидкой
                    return BigDecimal.valueOf((Integer) actionDiscountDomain.getProperties().get("cost"));
                }
            }
        }

        //Проверить домен на премиальность, если да - установить новую цену
        if (availabilityInfo.getPremiumPrice() != null && (availabilityInfo.getPremiumPrice().compareTo(BigDecimal.ZERO) > 0)) {
            return availabilityInfo.getPremiumPrice();
        }

        return domainTld.getRegistrationService().getCost();
    }

    public ProcessingBusinessAction buy(String accountId, DomainCartItem domain, List<AccountPromotion> accountPromotions, AccountPromotion accountPromotion) {
        PersonalAccount account = accountManager.findOne(accountId);

        String domainName = domain.getName();

        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(accountId);
        message.addParam("name", domainName.toLowerCase());
        message.addParam("register", true);
        message.addParam("personId", domain.getPersonId());

        logger.debug("Buying domain " + domainName.toLowerCase());

        boolean isFreeDomain = false;
        boolean isDiscountedDomain = false;

        DomainTld domainTld = getDomainTld(domainName);

        if (accountPromotion != null) {
            PromocodeAction actionFreeDomain = accountPromotion.getActions().get(BONUS_FREE_DOMAIN_PROMOCODE_ACTION_ID);
            PromocodeAction actionDiscountDomain = accountPromotion.getActions().get(DOMAIN_DISCOUNT_RU_RF_ACTION_ID);
            if (actionFreeDomain != null) {
                List<String> availableTlds = (List<String>) actionFreeDomain.getProperties().get("tlds");

                if (availableTlds.contains(domainTld.getTld())) {
                    Optional<AccountPromotion> foundAccountPromotion = accountPromotions
                            .stream()
                            .filter(accountPromotion1 -> accountPromotion1.getId().equals(accountPromotion.getId()))
                            .findFirst();

                    if (foundAccountPromotion.isPresent()) {
                        String foundAccountPromotionId = foundAccountPromotion.get().getId();
                        accountPromotionManager.deactivateAccountPromotionByIdAndActionId(
                                foundAccountPromotionId,
                                BONUS_FREE_DOMAIN_PROMOCODE_ACTION_ID
                        );
                        isFreeDomain = true;
                        message.addParam("freeDomainPromotionId", foundAccountPromotionId);
                    }
                }
            } else if (actionDiscountDomain != null) {
                List<String> availableTlds = (List<String>) actionDiscountDomain.getProperties().get("tlds");

                if (availableTlds.contains(domainTld.getTld())) {
                    Optional<AccountPromotion> foundAccountPromotion = accountPromotions
                            .stream()
                            .filter(accountPromotion1 -> accountPromotion1.getId().equals(accountPromotion.getId()))
                            .findFirst();

                    if (foundAccountPromotion.isPresent()) {
                        String foundAccountPromotionId = foundAccountPromotion.get().getId();
                        accountPromotionManager.deactivateAccountPromotionByIdAndActionId(
                                foundAccountPromotionId,
                                DOMAIN_DISCOUNT_RU_RF_ACTION_ID
                        );

                        // Устанавливает цену со скидкой
                        domainTld.getRegistrationService().setCost(BigDecimal.valueOf((Integer) actionDiscountDomain.getProperties().get("cost")));
                        message.addParam("domainDiscountPromotionId", foundAccountPromotionId);
                        isDiscountedDomain = true;
                    }
                }
            }
        }

        //Проверить домен на премиальность
        AvailabilityInfo availabilityInfo = getAvailabilityInfo(domainName);

        //Проверить домен на премиальность, если да - установить новую цену
        if (availabilityInfo.getPremiumPrice() != null && (availabilityInfo.getPremiumPrice().compareTo(BigDecimal.ZERO) > 0)) {
            domainTld.getRegistrationService().setCost(availabilityInfo.getPremiumPrice());
            isFreeDomain = false;
            isDiscountedDomain = false;
        }

        if (!isFreeDomain) {
            accountHelper.checkBalance(account, domainTld.getRegistrationService());
            SimpleServiceMessage blockResult = accountHelper.block(account, domainTld.getRegistrationService());
            String documentNumber = (String) blockResult.getParam("documentNumber");
            message.addParam("documentNumber", documentNumber);
        }

        ProcessingBusinessOperation processingBusinessOperation = businessOperationBuilder.build(BusinessOperationType.DOMAIN_CREATE, message);

        ProcessingBusinessAction processingBusinessAction = businessActionBuilder.build(BusinessActionType.DOMAIN_CREATE_RC, message, processingBusinessOperation);

        String actionText = isFreeDomain ?
                "бесплатную регистрацию" :
                (isDiscountedDomain ?
                        "регистрацию со скидкой" :
                        "регистрацию");
        //Save history
        String operator = account.getName();
        Map<String, String> params = new HashMap<>();
        params.put(HISTORY_MESSAGE_KEY, "Поступила заявка на " + actionText +" домена (имя: " + message.getParam("name") + ")");
        params.put(OPERATOR_KEY, operator);

        publisher.publishEvent(new AccountHistoryEvent(accountId, params));

        return processingBusinessAction;
    }

    private DomainTld getDomainTld(String domainName) {
        DomainTld domainTld = domainTldService.findActiveDomainTldByDomainName(domainName);

        if (domainTld == null) {
            logger.error("Домен: " + domainName + " недоступен. Зона домена отсутствует в системе");
            throw new DomainNotAvailableException("Домен: " + domainName + " недоступен. Зона домена отсутствует в системе");
        }

        return domainTld;
    }

    private AvailabilityInfo getAvailabilityInfo(String domainName) {
        //Проверить домен на доступность/премиальность
        AvailabilityInfo availabilityInfo = null;

        try {
            availabilityInfo = domainRegistrarFeignClient.getAvailabilityInfo(domainName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (availabilityInfo == null) {
            logger.error("Домен: " + domainName + ". Сервис регистрации недоступен.");
            throw new DomainNotAvailableException("Домен: " + domainName + ". Сервис регистрации недоступен.");
        }

        if (!availabilityInfo.getFree()) {
            logger.error("Домен: " + domainName + " по данным whois занят.");
            throw new DomainNotAvailableException("Домен: " + domainName + " по данным whois занят.");
        }

        return availabilityInfo;
    }
}
