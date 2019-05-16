package ru.majordomo.hms.personmgr.service;

import com.google.common.net.InternetDomainName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.IDN;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import ru.majordomo.hms.personmgr.common.*;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.NotEnoughMoneyException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.feign.DomainRegistrarFeignClient;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.model.cart.DomainCartItem;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.repository.PromotionRepository;
import ru.majordomo.hms.rc.user.resources.Domain;

import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.Utils.formatBigDecimalWithCurrency;

@Service
public class DomainService {
    private final static Logger logger = LoggerFactory.getLogger(DomainService.class);

    private static TemporalAdjuster THIRTY_DAYS_AFTER = TemporalAdjusters.ofDateAdjuster(date -> date.plusDays(30));
    private static TemporalAdjuster THIRTY_DAYS_BEFORE = TemporalAdjusters.ofDateAdjuster(date -> date.minusDays(30));
//    private static TemporalAdjuster SIXTY_DAYS_AFTER = TemporalAdjusters.ofDateAdjuster(date -> date.plusDays(60));
//    private static TemporalAdjuster TWENTY_NINE_DAYS_BEFORE = TemporalAdjusters.ofDateAdjuster(date -> date.minusDays(29));
//    private static TemporalAdjuster THREE_DAYS_BEFORE = TemporalAdjusters.ofDateAdjuster(date -> date.minusDays(3));
//    private static TemporalAdjuster FOURTEEN_DAYS_AFTER = TemporalAdjusters.ofDateAdjuster(date -> date.plusDays(14));
    private static TemporalAdjuster FIFTY_DAYS_AFTER = TemporalAdjusters.ofDateAdjuster(date -> date.plusDays(50));
    private static TemporalAdjuster TWENTY_FIVE_DAYS_BEFORE = TemporalAdjusters.ofDateAdjuster(date -> date.minusDays(25));

    private final RcUserFeignClient rcUserFeignClient;
    private final AccountHelper accountHelper;
    private final DomainTldService domainTldService;
    private final ApplicationEventPublisher publisher;
    private final DomainRegistrarFeignClient domainRegistrarFeignClient;
    private final BlackListService blackListService;
    private final PersonalAccountManager accountManager;
    private final AccountPromotionManager accountPromotionManager;
    private final PromotionRepository promotionRepository;
    private final AccountNotificationHelper accountNotificationHelper;
    private final BusinessHelper businessHelper;
    private final AccountHistoryManager history;

    @Autowired
    public DomainService(
            RcUserFeignClient rcUserFeignClient,
            AccountHelper accountHelper,
            DomainTldService domainTldService,
            ApplicationEventPublisher publisher,
            DomainRegistrarFeignClient domainRegistrarFeignClient,
            BlackListService blackListService,
            PersonalAccountManager accountManager,
            AccountPromotionManager accountPromotionManager,
            PromotionRepository promotionRepository,
            AccountNotificationHelper accountNotificationHelper,
            BusinessHelper businessHelper,
            AccountHistoryManager history
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.accountHelper = accountHelper;
        this.domainTldService = domainTldService;
        this.publisher = publisher;
        this.domainRegistrarFeignClient = domainRegistrarFeignClient;
        this.blackListService = blackListService;
        this.accountManager = accountManager;
        this.accountPromotionManager = accountPromotionManager;
        this.promotionRepository = promotionRepository;
        this.accountNotificationHelper = accountNotificationHelper;
        this.businessHelper = businessHelper;
        this.history = history;
    }

    public void processExpiringDomainsByAccount(PersonalAccount account) {

        try {
            LocalDate paidTillStart = LocalDate.now().with(THIRTY_DAYS_BEFORE);
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
            if (domains.isEmpty()) {
                logger.debug("Not found expiring domains for accountId: " + account.getId());
                return;
            }


            List<Domain> expiringDomains = new ArrayList<>();
            List<Domain> expiredDomains = new ArrayList<>();
            List<Domain> expiringDomainsForSms = new ArrayList<>();

            //Отправлять уведомление о истечении регистрации домена
            //на почту за сколько дней до
            List<Integer> daysBeforeExpiredForEmail = Arrays.asList(30, 25, 20, 15, 10, 7, 5, 3, 2, 1);
            //на почту через сколько дней после
            List<Integer> daysAfterExperedForEmail = Arrays.asList(1, 5, 10, 15, 20, 25);
            //по sms за сколько дней до
            Integer daysBeforeExpiredForSms = 5;

            //Нужно ли отправлять SMS
            Boolean sendSms = accountNotificationHelper.isSubscribedToSmsType(account, MailManagerMessageType.SMS_DOMAIN_DELEGATION_ENDING);

            int daysBeforeExpired;

            for (Domain domain : domains) {
                //дней до истечения, может быть отрицательным
                daysBeforeExpired = Utils.differenceInDays(LocalDate.now(), domain.getRegSpec().getPaidTill());
                if (daysBeforeExpiredForEmail.contains(daysBeforeExpired)) { expiringDomains.add(domain); }
                else if (daysAfterExperedForEmail.contains(-daysBeforeExpired)) { expiredDomains.add(domain); }
                if (sendSms && daysBeforeExpired == daysBeforeExpiredForSms) { expiringDomainsForSms.add(domain); }
            }

            //notification by email
            expiringDomains.forEach(domain -> logger.debug("(EMAIL) We found expiring domain: " + domain));
            if (!expiringDomains.isEmpty()) {
                sendMailForExpiringAndExpiredDomain(account, expiringDomains, false);
            }
            expiredDomains.forEach(domain -> logger.debug("(EMAIL) We found expired domain: " + domain));
            if (!expiredDomains.isEmpty()) {
                sendMailForExpiringAndExpiredDomain(account, expiredDomains, true);
            }

            //sms
            if (sendSms && !expiringDomainsForSms.isEmpty()) {
                String apiName = expiringDomainsForSms.size() == 1 ? "MajordomoOneDomainDelegationEnding" : "MajordomoSomeDomainsDelegationEnding";
                HashMap<String, String> parameters = new HashMap<>();
                parameters.put("client_id", account.getAccountId());
                parameters.put("domain", expiringDomainsForSms.get(0).getName());
                parameters.put("acc_id", account.getName());
                accountNotificationHelper.sendSms(account, apiName, 10, parameters);
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.service.DomainService.processExpiringDomainsByAccount " + e.getMessage());
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

        domains.sort(Comparator.comparing(domain -> domain.getRegSpec().getPaidTill()));

        domains.forEach(domain -> logger.debug("We found domain for AutoRenew: " + domain));

        List<Domain> domainNotProlong = new ArrayList<>();

        for (Domain domain : domains) {

            if (domain.getAutoRenew()) {

                BigDecimal balance = accountHelper.getBalance(account);

                DomainTld domainTld = domainTldService.findDomainTldByDomainNameAndRegistrator(domain.getName(), domain.getRegSpec().getRegistrar());

                BigDecimal pricePremium = domainRegistrarFeignClient.getRenewPremiumPrice(domain.getName());
                if (pricePremium != null && pricePremium.compareTo(BigDecimal.ZERO) > 0) {
                    domainTld.getRenewService().setCost(pricePremium);
                }
                try {
                    accountHelper.checkBalance(account, domainTld.getRenewService());
                    accountHelper.checkBalanceWithoutBonus(account, domainTld.getRenewService());
                } catch (NotEnoughMoneyException e) {
                    //Если денег не хватает
                    //Запишем попытку в историю клиента
                    history.saveForOperatorService(account, "Автоматическое продление " + domain.getName() + " невозможно, на счету " + balance + " руб.");

                    domainNotProlong.add(domain);
                }

                ChargeMessage chargeMessage = new ChargeMessage.Builder(domainTld.getRenewService())
                        .excludeBonusPaymentType()
                        .setComment(domain.getName())
                        .build();

                SimpleServiceMessage blockResult = accountHelper.block(account, chargeMessage);

                String documentNumber = (String) blockResult.getParam("documentNumber");

                SimpleServiceMessage domainRenewMessage = new SimpleServiceMessage();

                domainRenewMessage.setAccountId(account.getId());
                domainRenewMessage.addParam(RESOURCE_ID_KEY, domain.getId());
                domainRenewMessage.addParam("renew", true);
                domainRenewMessage.addParam(AUTO_RENEW_KEY, true);
                domainRenewMessage.addParam("documentNumber", documentNumber);

                businessHelper.buildAction(BusinessActionType.DOMAIN_UPDATE_RC, domainRenewMessage);
            }
        }
        //Отправим уведомления по почте и смс
        notifyForDomainNoProlongNoMoney(account, domainNotProlong);
    }

    private List<Domain> getDomainsByName(String domainName) {
        List<Domain> domains = new ArrayList<>();

        try {
            domains.add(rcUserFeignClient.findDomain(domainName));
        } catch (ResourceNotFoundException ignore) {
            logger.info("rc-user: домен " + domainName + " не найден");
        }

        try {
            domains.add(rcUserFeignClient.findDomain(IDN.toASCII(domainName)));
        } catch (ResourceNotFoundException ignore) {
            logger.info("rc-user: домен " + domainName + " не найден");
        }

        return domains;
    }

    public void checkBlacklist(String domainName, String accountId) {
        domainName = IDN.toUnicode(domainName);

        PersonalAccount account = accountManager.findOne(accountId);
        InternetDomainName domain = InternetDomainName.from(domainName);
        String topPrivateDomainName = domainName;
        try {
            topPrivateDomainName = IDN.toUnicode(domain.topPrivateDomain().toString());
        } catch (Exception ignored) {
        }

        //Full domain check
        List<Domain> domainsByName = getDomainsByName(domainName);
        if (!domainsByName.isEmpty() || blackListService.domainExistsInControlBlackList(domainName)) {
            logger.debug("domain: " + domainName + " exists in control BlackList");
            throw new ParameterValidationException("Домен " + domainName
                    + " уже присутствует в системе и не может быть добавлен.");
        }

        //Top private domain check
        domainsByName = getDomainsByName(topPrivateDomainName);
        if (!domainName.equals(topPrivateDomainName)
                && (!domainsByName.isEmpty() || blackListService.domainExistsInControlBlackList(topPrivateDomainName))) {
            boolean existOnAccount = false;

            List<Domain> domains = accountHelper.getDomains(account);

            if (domains != null) {
                for (Domain d : domains) {
                    if (d.getName().equals(topPrivateDomainName) || d.getName().equals(IDN.toASCII(topPrivateDomainName))) {
                        existOnAccount = true;
                        break;
                    }
                }
            }

            if (!existOnAccount) {
                logger.debug("domain: " + domainName + " exists in control BlackList");
                throw new ParameterValidationException("Домен " + domain.topPrivateDomain().toString()
                        + " уже присутствует в системе и не может быть добавлен.");
            }
        }
    }

    public void checkBlacklistOnUpdate(String domainName) {
        domainName = IDN.toUnicode(domainName);

        //Full domain check
        if (blackListService.domainExistsInControlBlackList(domainName)) {
            logger.debug("domain: " + domainName + " exists in control BlackList");
            throw new ParameterValidationException("Домен " + domainName
                    + " не может быть обновлен, так как уже присутствует в системе на другом аккаунте или заблокирован.");
        }
    }

    public void check(String domainName, String accountId) {
        checkBlacklist(domainName, accountId);

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
            if (!accountPromotion.getActive()) {
                continue;
            }

            PromocodeAction action = accountPromotion.getAction();

            switch (action.getActionType()) {
                case SERVICE_FREE_DOMAIN:
                case SERVICE_DOMAIN_DISCOUNT_RU_RF:
                    List<String> availableTlds = (List<String>) action.getProperties().get("tlds");

                    if (availableTlds.contains(domainTld.getTld())) {
                        accountPromotion.setActive(false);
                        return accountPromotion;
                    }
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

        //Проверить домен на премиальность, если да - установить новую цену
        if (availabilityInfo.getPremiumPrice() != null && (availabilityInfo.getPremiumPrice().compareTo(BigDecimal.ZERO) > 0)) {
            return availabilityInfo.getPremiumPrice();
        }

        if (accountPromotion != null) {
            PromocodeAction action = accountPromotion.getAction();
            List<String> availableTlds;
            switch (action.getActionType()) {
                case SERVICE_FREE_DOMAIN:
                    availableTlds = (List<String>) action.getProperties().get("tlds");
                    if (availableTlds.contains(domainTld.getTld())) {
                        return BigDecimal.ZERO;
                    }

                    break;
                case SERVICE_DOMAIN_DISCOUNT_RU_RF:
                    availableTlds = (List<String>) action.getProperties().get("tlds");
                    if (availableTlds.contains(domainTld.getTld())) {
                        // Устанавливает цену со скидкой
                        return BigDecimal.valueOf((Integer) action.getProperties().get("cost"));
                    }

                    break;
            }
        }

        return domainTld.getRegistrationService().getCost();
    }

    public ProcessingBusinessAction buy(String accountId, DomainCartItem domain, List<AccountPromotion> accountPromotions, AccountPromotion accountPromotion) {
        PersonalAccount account = accountManager.findOne(accountId);

        String domainName = domain.getName().toLowerCase();

        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(accountId);
        message.addParam("name", domainName);
        message.addParam("register", true);
        message.addParam("personId", domain.getPersonId());

        logger.debug("Buying domain " + domainName);

        boolean isFreeDomain = false;
        boolean isDiscountedDomain = false;

        DomainTld domainTld = getDomainTld(domainName);

        AvailabilityInfo availabilityInfo = getAvailabilityInfo(domainName);

        //Проверить домен на премиальность, если да - установить новую цену
        if (availabilityInfo.getPremiumPrice() != null && (availabilityInfo.getPremiumPrice().compareTo(BigDecimal.ZERO) > 0)) {
            domainTld.getRegistrationService().setCost(availabilityInfo.getPremiumPrice());
            isFreeDomain = false;
            isDiscountedDomain = false;
        } else {
            if (accountPromotion != null) {
                PromocodeAction action = accountPromotion.getAction();
                List<String> availableTlds;
                switch (action.getActionType()) {
                    case SERVICE_FREE_DOMAIN:
                        availableTlds = (List<String>) action.getProperties().get("tlds");

                        if (availableTlds.contains(domainTld.getTld())) {
                            Optional<AccountPromotion> foundAccountPromotion = accountPromotions
                                    .stream()
                                    .filter(accountPromotion1 -> accountPromotion1.getId().equals(accountPromotion.getId()))
                                    .findFirst();

                            if (foundAccountPromotion.isPresent()) {
                                String foundAccountPromotionId = foundAccountPromotion.get().getId();
                                isFreeDomain = true;
                                message.addParam("freeDomainPromotionId", foundAccountPromotionId);
                            }
                        }

                        break;

                    case SERVICE_DOMAIN_DISCOUNT_RU_RF:
                        availableTlds = (List<String>) action.getProperties().get("tlds");

                        if (availableTlds.contains(domainTld.getTld())) {
                            Optional<AccountPromotion> foundAccountPromotion = accountPromotions
                                    .stream()
                                    .filter(accountPromotion1 -> accountPromotion1.getId().equals(accountPromotion.getId()))
                                    .findFirst();

                            if (foundAccountPromotion.isPresent()) {
                                String foundAccountPromotionId = foundAccountPromotion.get().getId();

                                // Устанавливает цену со скидкой
                                domainTld.getRegistrationService().setCost(BigDecimal.valueOf((Integer) action.getProperties().get("cost")));
                                message.addParam("domainDiscountPromotionId", foundAccountPromotionId);
                                isDiscountedDomain = true;
                            }
                        }
                }
            }

        }

        if (!isFreeDomain) {
            accountHelper.checkBalanceWithoutBonus(account, domainTld.getRegistrationService());
        }

        deactivateAccountPromotion(message);

        if (!isFreeDomain) {
            ChargeMessage chargeMessage = new ChargeMessage.Builder(domainTld.getRegistrationService())
                    .excludeBonusPaymentType()
                    .setComment(domainName)
                    .build();

            try {
                SimpleServiceMessage blockResult = accountHelper.block(account, chargeMessage);
                String documentNumber = (String) blockResult.getParam("documentNumber");
                message.addParam("documentNumber", documentNumber);
            } catch (Exception e) {
                logger.info("DomainService.buy() with try block money catch " + e.getClass().getName() + " e.message: " + e.getMessage());
                activateAccountPromotion(message);
                throw e;
            }
        }

        ProcessingBusinessOperation processingBusinessOperation = businessHelper.buildOperation(BusinessOperationType.DOMAIN_CREATE, message);

        ProcessingBusinessAction processingBusinessAction = businessHelper.buildActionByOperation(BusinessActionType.DOMAIN_CREATE_RC, message, processingBusinessOperation);

        String actionText = isFreeDomain ?
                "бесплатную регистрацию (actionPromotion Id: " + message.getParam("freeDomainPromotionId") + " )" :
                (isDiscountedDomain ?
                        "регистрацию со скидкой (actionPromotion Id: " + message.getParam("domainDiscountPromotionId") + " )" :
                        "регистрацию");

        history.save(account, "Поступила заявка на " + actionText +" домена (имя: " + message.getParam("name") + ")", account.getName());

        return processingBusinessAction;
    }

    private DomainTld getDomainTld(String domainName) {
        DomainTld domainTld = domainTldService.findActiveDomainTldByDomainName(domainName);

        if (domainTld == null) {
            logger.error("Домен: " + domainName + " недоступен. Зона домена отсутствует в системе");
            throw new ParameterValidationException("Домен: " + domainName + " недоступен. Зона домена отсутствует в системе");
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
            throw new InternalApiException("Домен: " + domainName + ". Сервис регистрации недоступен.");
        }

        if (!availabilityInfo.getFree()) {
            logger.error("Домен: " + domainName + " по данным whois занят.");
            throw new ParameterValidationException("Домен: " + domainName + " по данным whois занят.");
        }

        return availabilityInfo;
    }

    private void sendMailForExpiringAndExpiredDomain(PersonalAccount account, List<Domain> domains, boolean expired){

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
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("acc_id", account.getName());
        parameters.put("domains", domainsForMail);
        parameters.put("balance", formatBigDecimalWithCurrency(balance));
        parameters.put("from", "noreply@majordomo.ru");
        accountNotificationHelper.sendMail(account,
                expired ? "MajordomoHMSVHDomainsAfterExpired" : "MajordomoHMSVHDomainsExpires",
                10, parameters);
    }

    private void notifyForDomainNoProlongNoMoney(PersonalAccount account, List<Domain> domains) {
        if (domains != null && !domains.isEmpty()) {

            //Email
            String balance = accountNotificationHelper.getBalanceForEmail(account);

            String domainsForMail = "";
            for (Domain domain : domains) {
                domainsForMail += String.format(
                        "%-20s - %s<br>",
                        domain.getName(),
                        domain.getRegSpec().getPaidTill().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                );
            }

            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("client_id", account.getAccountId());
            parameters.put("acc_id", account.getName());
            parameters.put("domain_for_subject", domains.get(0).getName());
            parameters.put("domains", domainsForMail);
            parameters.put("from", "noreply@majordomo.ru");
            parameters.put("balance", balance);

            accountNotificationHelper.sendMail(account, "HMSVHNomoneyDomainProlong", 10, parameters);

//            //Если подключено СМС-уведомление, то также отправим его
//            //Отправляем SMS за ... дней до истечения
//            // не отправляем sms https://redmine.intr/issues/8168
//            int days = 3;
//            if (domains.stream().filter(
//                    domain -> domain.getRegSpec().getPaidTill().equals(LocalDate.now().plusDays(days))).collect(Collectors.toList())
//                    .isEmpty())
//            if (accountNotificationHelper.hasActiveSmsNotificationsAndMessageType(account, MailManagerMessageType.SMS_NO_MONEY_TO_AUTORENEW_DOMAIN)) {
//
//                parameters = new HashMap<>();
//                parameters.put("client_id", account.getAccountId());
//                //Текст SMS изменяется в зависимости от количества доменов, требующих продления
//                parameters.put("domain", (domains.size() > 1) ? "истекающих доменов" : ("домена " + domains.get(0).getName()));
//                parameters.put("acc_id", account.getName());
//                accountNotificationHelper.sendSms(account, "HMSMajordomoNoMoneyToAutoRenewDomain", 10, parameters);
//            }
        }

    }

    private void activateAccountPromotion(SimpleServiceMessage message) {
        if (message.getParam("freeDomainPromotionId") != null) {
            accountPromotionManager.activateAccountPromotionById((String) message.getParam("freeDomainPromotionId"));
        }

        if (message.getParam("domainDiscountPromotionId") != null) {
            accountPromotionManager.activateAccountPromotionById((String) message.getParam("domainDiscountPromotionId"));
        }
    }

    private void deactivateAccountPromotion(SimpleServiceMessage message) {
        if (message.getParam("freeDomainPromotionId") != null) {
            accountPromotionManager.deactivateAccountPromotionById((String) message.getParam("freeDomainPromotionId"));
        }

        if (message.getParam("domainDiscountPromotionId") != null) {
            accountPromotionManager.deactivateAccountPromotionById((String) message.getParam("domainDiscountPromotionId"));
        }
    }
}
