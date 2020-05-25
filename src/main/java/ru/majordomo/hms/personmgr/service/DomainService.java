package ru.majordomo.hms.personmgr.service;

import com.google.common.net.InternetDomainName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.IDN;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.*;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.Container;
import ru.majordomo.hms.personmgr.dto.push.DomainExpiredPush;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.feign.DomainRegistrarFeignClient;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.manager.DomainInTransferManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.model.cart.DomainCartItem;
import ru.majordomo.hms.personmgr.model.domain.DomainInTransfer;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.ServicePlan;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PromotionRepository;
import ru.majordomo.hms.personmgr.repository.ServicePlanRepository;
import ru.majordomo.hms.personmgr.service.promotion.AccountPromotionFactory;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Person;

import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.Utils.*;

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
    private final AccountStatHelper accountStatHelper;
    private final DomainTldService domainTldService;
    private final DiscountFactory discountFactory;
    private final DomainRegistrarFeignClient domainRegistrarFeignClient;
    private final BlackListService blackListService;
    private final PersonalAccountManager accountManager;
    private final AccountPromotionManager accountPromotionManager;
    private final DomainInTransferManager domainInTransferManager;
    private final PromotionRepository promotionRepository;
    private final ServicePlanRepository servicePlanRepository;
    private final AccountNotificationHelper accountNotificationHelper;
    private final BusinessHelper businessHelper;
    private final AccountHistoryManager history;
    private final PaymentServiceRepository paymentServiceRepository;
    private final AccountPromotionFactory accountPromotionFactory;

    @Autowired
    public DomainService(
            RcUserFeignClient rcUserFeignClient,
            AccountHelper accountHelper,
            AccountStatHelper accountStatHelper,
            DomainTldService domainTldService,
            DiscountFactory discountFactory,
            DomainRegistrarFeignClient domainRegistrarFeignClient,
            BlackListService blackListService,
            PersonalAccountManager accountManager,
            AccountPromotionManager accountPromotionManager,
            DomainInTransferManager domainInTransferManager,
            PromotionRepository promotionRepository,
            ServicePlanRepository servicePlanRepository,
            AccountNotificationHelper accountNotificationHelper,
            BusinessHelper businessHelper,
            AccountHistoryManager history,
            PaymentServiceRepository paymentServiceRepository,
            AccountPromotionFactory accountPromotionFactory
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.accountHelper = accountHelper;
        this.accountStatHelper = accountStatHelper;
        this.domainTldService = domainTldService;
        this.discountFactory = discountFactory;
        this.domainRegistrarFeignClient = domainRegistrarFeignClient;
        this.blackListService = blackListService;
        this.accountManager = accountManager;
        this.accountPromotionManager = accountPromotionManager;
        this.domainInTransferManager = domainInTransferManager;
        this.promotionRepository = promotionRepository;
        this.servicePlanRepository = servicePlanRepository;
        this.accountNotificationHelper = accountNotificationHelper;
        this.businessHelper = businessHelper;
        this.history = history;
        this.paymentServiceRepository = paymentServiceRepository;
        this.accountPromotionFactory = accountPromotionFactory;
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
            List<Domain> expiredDomainsForRenewDiscounts = new ArrayList<>();

            //Отправлять уведомление о истечении регистрации домена
            //на почту за сколько дней до
            List<Integer> daysBeforeExpiredForEmail = Arrays.asList(30, 25, 20, 15, 10, 7, 5, 3, 2, 1);
            //на почту через сколько дней после
            List<Integer> daysAfterExperedForEmail = Arrays.asList(1, 5, 10, 15, 20, 25);
            //Скидки на продление конкретных доменов
            List<Integer> daysAfterExpiredForAddRenewDiscount = Arrays.asList(3, 7, 10);
            //по sms за сколько дней до
            int daysBeforeExpiredForSms = 5;

            int daysBeforeExpired;

            for (Domain domain : domains) {
                //дней до истечения, может быть отрицательным
                daysBeforeExpired = differenceInDays(LocalDate.now(), domain.getRegSpec().getPaidTill());
                if (daysBeforeExpiredForEmail.contains(daysBeforeExpired)) { expiringDomains.add(domain); }
                else if (daysAfterExperedForEmail.contains(-daysBeforeExpired)) { expiredDomains.add(domain); }
                if (daysBeforeExpired == daysBeforeExpiredForSms) { expiringDomainsForSms.add(domain); }
                if (daysAfterExpiredForAddRenewDiscount.contains(-daysBeforeExpired)) { expiredDomainsForRenewDiscounts.add(domain); }
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

            //Нужно ли отправлять SMS
            boolean sendSms = accountNotificationHelper.isSubscribedToSmsType(account, MailManagerMessageType.SMS_DOMAIN_DELEGATION_ENDING);
            boolean sendTelegram = account.hasNotification(MailManagerMessageType.TELEGRAM_DOMAIN_DELEGATION_ENDING);
            //sms
            if (!expiringDomainsForSms.isEmpty()) {
                boolean oneDomain = expiringDomainsForSms.size() == 1;
                HashMap<String, String> parameters = new HashMap<>();
                parameters.put("client_id", account.getAccountId());
                parameters.put("domain", expiringDomainsForSms.get(0).getName());
                parameters.put("acc_id", account.getName());
                if (sendSms) {
                    String apiName = oneDomain ? "MajordomoOneDomainDelegationEnding" : "MajordomoSomeDomainsDelegationEnding";
                    accountNotificationHelper.sendSms(account, apiName, 10, parameters);
                }
                if (sendTelegram) {
                    String telegramApiName = oneDomain ? "TelegramMajordomoOneDomainDelegationEnding" : "TelegramMajordomoSomeDomainsDelegationEnding";
                    accountNotificationHelper.sendTelegram(account, telegramApiName, parameters);
                }
            }

            if (!expiredDomainsForRenewDiscounts.isEmpty()) {
                processAddRenewPromotions(expiredDomainsForRenewDiscounts, account);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.service.DomainService.processExpiringDomainsByAccount " + e.getMessage());
        }
    }

    public void processDomainsAutoRenewByAccount(PersonalAccount account) {
        if (!accountHelper.getPlan(account).isDomainAllowed()) {
            logger.debug("Domains AutoRenew for account '{}' is not available due to tariff restrictions", account.getId());
            return;
        }

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
                SimpleServiceMessage domainRenewMessage = new SimpleServiceMessage();

                domainRenewMessage.setAccountId(account.getId());
                domainRenewMessage.addParam(RESOURCE_ID_KEY, domain.getId());
                domainRenewMessage.addParam("renew", true);
                domainRenewMessage.addParam(AUTO_RENEW_KEY, true);

                try {
                    blockMoneyBeforeAutoRenewExistsDomain(account, domainRenewMessage, domain);

                    businessHelper.buildAction(BusinessActionType.DOMAIN_UPDATE_RC, domainRenewMessage);

                } catch (Exception e) {
                    //Если денег не хватает
                    //Запишем попытку в историю клиента
                    history.saveForOperatorService(account, "Автоматическое продление " + domain.getName() + " невозможно, на счету " + balance + " руб.");

                    domainNotProlong.add(domain);
                }
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

    public void checkForTransfer(String domainName, String accountId) {
        checkBlacklist(domainName, accountId);

        getDomainTld(domainName);
    }

    public AccountPromotion usePromotion(String domainName, List<AccountPromotion> accountPromotions) {
        DomainTld domainTld = getDomainTld(domainName);

        String prioritizedPromotionId = promotionRepository.findByName(FREE_DOMAIN_PROMOTION).getId();

        accountPromotions.sort((o1, o2) -> {
            if (o1.getPromotionId().equals(o2.getPromotionId())) return 0;
            if (o1.getPromotionId().equals(prioritizedPromotionId)) return -1;
            if (o2.getPromotionId().equals(prioritizedPromotionId)) return 1;
            return 0;
        });

        for (AccountPromotion accountPromotion : accountPromotions) {
            if (!accountPromotion.isValidNow()) {
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

        return null;
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

    /**
     * Делаем запрос с помощью AuthInfo в reg-rpc на отправку кода подтверждения трансфера в email/sms (TRANSFER_GET_EMAIL)
     *
     * @param accountId  ID аккаунта, на который будет назначен переносимый домен
     * @param personId   ID персоны, на которую будет назначен переносимый домен
     * @param domainName Доменное имя
     * @param authInfo   AuthInfo-код для верификации трансфера
     * @return DomainInTransfer - объект, связанный с новым|существующим запросом на трансфер
     */
    public DomainInTransfer requestDomainTransfer(String accountId, String personId, String domainName, String authInfo) {
        PersonalAccount account = accountManager.findOne(accountId);

        DomainInTransfer domainInTransfer = domainInTransferManager.findNeedToProcessByAccountId(accountId);
        if (domainInTransfer != null) {
            return domainInTransfer;
        }

        ServicePlan servicePlan = servicePlanRepository.findOneByFeatureAndActive(Feature.DOMAIN_TRANSFER_RU_RF, true);
        accountHelper.checkBalanceWithoutBonus(account, servicePlan.getService().getCost());

        Person person = rcUserFeignClient.getPerson(accountId, personId);

        domainRegistrarFeignClient.transferRequest(person.getNicHandle(), authInfo, domainName);

        domainInTransfer = new DomainInTransfer();
        domainInTransfer.setPersonalAccountId(accountId);
        domainInTransfer.setPersonId(personId);
        domainInTransfer.setDomainName(domainName);
        domainInTransfer.setState(DomainInTransfer.State.NEED_TO_PROCESS);
        domainInTransferManager.save(domainInTransfer);

        logger.debug("Created request for transfer domain {} to account {} with person {}", domainName, accountId, personId);

        return domainInTransfer;
    }

    /**
     * Подтверждаем перенос домена к нам с помощью кода подтверждения. Создаем запрос на трансфер домена в reg-rpc (TRANSFER_FROM)
     *
     * @param accountId        ID аккаунта, на который будет назначен переносимый домен
     * @param verificationCode Код подтверждения переноса
     * @return DomainInTransfer - объект, связанный с созданным запросом на трансфер домена
     */
    public DomainInTransfer confirmDomainTransfer(String accountId, String verificationCode) {
        PersonalAccount account = accountManager.findOne(accountId);

        DomainInTransfer domainInTransfer = domainInTransferManager.findNeedToProcessByAccountId(accountId);
        if (domainInTransfer == null) {
            logger.error("DomainInTransfer to confirm not found on account {}", accountId);
            throw new ParameterValidationException("Запрос на трансфер не найден");
        }

        ServicePlan servicePlan = servicePlanRepository.findOneByFeatureAndActive(Feature.DOMAIN_TRANSFER_RU_RF, true);
        accountHelper.checkBalanceWithoutBonus(account, servicePlan.getService().getCost());

        ChargeMessage chargeMessage = new ChargeMessage.Builder(servicePlan.getService())
                .excludeBonusPaymentType()
                .setComment("Domain transfer: " + domainInTransfer.getDomainName())
                .build();

        String documentNumber;
        try {
            SimpleServiceMessage blockResult = accountHelper.block(account, chargeMessage);
            documentNumber = (String) blockResult.getParam("documentNumber");
        } catch (Exception e) {
            logger.error("DomainService.confirmDomainTransfer() got Exception on money block (fin): " + e.getClass().getName() +
                    " e.message: " + e.getMessage());
            throw e;
        }

        Person person = rcUserFeignClient.getPerson(accountId, domainInTransfer.getPersonId());
        try {
            domainRegistrarFeignClient.transferConfirmation(person.getNicHandle(), verificationCode);
        } catch (Exception e) {
            accountHelper.unblock(accountId, documentNumber);
            logger.error("DomainService.confirmDomainTransfer() got Exception on transfer confirmation (reg-rpc): " + e.getClass().getName() +
                    " e.message: " + e.getMessage());
            throw e;
        }

        logger.debug("Domain {} transfer request to accound {} and person {} confirmed. Going to create DomainInTransfer and charge payment",
                domainInTransfer.getDomainName(), accountId, person.getId());

        domainInTransfer.setState(DomainInTransfer.State.PROCESSING);
        domainInTransfer.setDocumentNumber(documentNumber);
        domainInTransferManager.save(domainInTransfer);

        String historyMessage = "Поступила заявка на перенос к нам домена " + domainInTransfer.getDomainName();
        history.save(account, historyMessage);

        return domainInTransfer;
    }

    /**
     * Перенос домена был выполнен успешно. Ставим соответствующий статус в DomainInTransfer и создаем домен в rc-user
     *
     * @param domainName Перенесенный к нам домен
     * @return ProcessingBusinessAction
     */
    public ProcessingBusinessAction processSuccessfulTransfer(String domainName) {
        DomainInTransfer domainInTransfer = domainInTransferManager.findProcessingByDomainName(domainName.toLowerCase());

        if (domainInTransfer == null) {
            String errorMsg = "DomainService.processSuccessfulTransfer(): DomainInTransfer " + domainName + " not found";
            logger.error(errorMsg);
            throw new ParameterValidationException(errorMsg);
        }

        logger.debug("Domain {} transferred to us. Creating domain in rc-user", domainName);

        domainInTransfer.setState(DomainInTransfer.State.ACCEPTED);
        domainInTransferManager.save(domainInTransfer);

        PersonalAccount account = accountManager.findOne(domainInTransfer.getPersonalAccountId());

        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(account.getAccountId());
        message.addParam("name", domainInTransfer.getDomainName());
        message.addParam("personId", domainInTransfer.getPersonId());
        message.addParam("documentNumber", domainInTransfer.getDocumentNumber());
        message.addParam("transfer", true);

        ProcessingBusinessOperation processingBusinessOperation = businessHelper.buildOperation(
                BusinessOperationType.DOMAIN_CREATE,
                message
        );
        ProcessingBusinessAction processingBusinessAction = businessHelper.buildActionByOperation(
                BusinessActionType.DOMAIN_CREATE_RC,
                message,
                processingBusinessOperation
        );

        history.save(account, "Перенос домена " + domainName + " подтвержден. Отправлен запрос на создание домена");

        Map<String, String> stat = new HashMap<>();
        stat.put(DOMAIN_NAME_KEY, domainInTransfer.getDomainName());
        stat.put(PERSON_ID_KEY, domainInTransfer.getPersonId());
        accountStatHelper.add(account.getAccountId(), AccountStatType.VIRTUAL_HOSTING_DOMAIN_TRANSFER_ACCEPT, stat);

        return processingBusinessAction;
    }

    /**
     * Перенос домена был отклонен. Ставим соответствующий статус в DomainInTransfer и снимаем блокировку средств
     *
     * @param domainName Отклоненный домен
     */
    public void processRejectedTransfer(String domainName) {
        DomainInTransfer domainInTransfer = domainInTransferManager.findProcessingByDomainName(domainName);

        if (domainInTransfer == null) {
            String errorMsg = "DomainService.processRejectedTransfer(): DomainInTransfer " + domainName + " not found";
            logger.error(errorMsg);
            throw new ParameterValidationException(errorMsg);
        }

        logger.debug("Domain {} transfer was rejected", domainName);

        accountHelper.unblock(domainInTransfer.getPersonalAccountId(), domainInTransfer.getDocumentNumber());

        domainInTransfer.setState(DomainInTransfer.State.REJECTED);
        domainInTransferManager.save(domainInTransfer);

        PersonalAccount account = accountManager.findOne(domainInTransfer.getPersonalAccountId());
        history.save(account, "Перенос домена " + domainName + " был отклонен.");

        Map<String, String> stat = new HashMap<>();
        stat.put(DOMAIN_NAME_KEY, domainInTransfer.getDomainName());
        stat.put(PERSON_ID_KEY, domainInTransfer.getPersonId());
        accountStatHelper.add(account.getAccountId(), AccountStatType.VIRTUAL_HOSTING_DOMAIN_TRANSFER_REJECT, stat);
    }

    /**
     * Перенос домена был отменен. Ставим соответствующий статус в DomainInTransfer и снимаем блокировку средств
     *
     * @param domainName Отмененный домен
     */
    public void processCancelledTransfer(String domainName) {
        DomainInTransfer domainInTransfer = domainInTransferManager.findProcessingByDomainName(domainName);

        if (domainInTransfer == null) {
            String errorMsg = "DomainService.processCancelledTransfer(): DomainInTransfer " + domainName + " not found";
            logger.error(errorMsg);
            throw new ParameterValidationException(errorMsg);
        }

        logger.debug("Domain {} transfer was cancelled", domainName);

        accountHelper.unblock(domainInTransfer.getPersonalAccountId(), domainInTransfer.getDocumentNumber());

        domainInTransfer.setState(DomainInTransfer.State.CANCELLED);
        domainInTransferManager.save(domainInTransfer);

        PersonalAccount account = accountManager.findOne(domainInTransfer.getPersonalAccountId());
        history.save(account, "Перенос домена " + domainName + " был отменен.");

        Map<String, String> stat = new HashMap<>();
        stat.put(DOMAIN_NAME_KEY, domainInTransfer.getDomainName());
        stat.put(PERSON_ID_KEY, domainInTransfer.getPersonId());
        accountStatHelper.add(account.getAccountId(), AccountStatType.VIRTUAL_HOSTING_DOMAIN_TRANSFER_CANCEL, stat);
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
                                message.addParam("accountPromotionId", foundAccountPromotionId);
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
                                message.addParam("accountPromotionId", foundAccountPromotionId);
                                isDiscountedDomain = true;
                            }
                        }
                }
            }

        }

        if (!isFreeDomain) {
            accountHelper.checkBalanceWithoutBonus(account, domainTld.getRegistrationService().getCost());
        }

        setAsUsedAccountPromotion(message, domainName);

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
                setAsActiveAccountPromotion(message);
                throw e;
            }
        }

        ProcessingBusinessOperation processingBusinessOperation = businessHelper.buildOperation(BusinessOperationType.DOMAIN_CREATE, message);

        ProcessingBusinessAction processingBusinessAction = businessHelper.buildActionByOperation(BusinessActionType.DOMAIN_CREATE_RC, message, processingBusinessOperation);

        String actionText = isFreeDomain ?
                "бесплатную регистрацию (actionPromotion Id: " + message.getParam("accountPromotionId") + " )" :
                (isDiscountedDomain ?
                        "регистрацию со скидкой (actionPromotion Id: " + message.getParam("accountPromotionId") + " )" :
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

    private DomainTld getDomainTld(Domain domain) {
        DomainTld domainTld = domainTldService.findDomainTldByDomainNameAndRegistrator(domain.getName(), domain.getRegSpec().getRegistrar());

        if (domainTld == null) {
            logger.error("Домен: {} id {} недоступен. Зона домена отсутствует в системе", domain.getName(), domain.getId());
            throw new ParameterValidationException("Домен: " + domain.getName() + " недоступен. Зона домена отсутствует в системе");
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

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String domainsForMail = "";
        for (Domain domain : domains) {
            String autoRenew = domain.getAutoRenew() ? "включено" : "выключено";
            domainsForMail += String.format(
                    "%-20s - %s - %-10s<br>",
                    domain.getName(),
                    domain.getRegSpec().getPaidTill().format(dateFormatter),
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

        String domainsCount = pluralizef("%d домена", "%d доменов", "%d доменов", domains.size());

        String domainsForPush = domains.stream()
                .map(d -> d.getName() + " - " + d.getRegSpec().getPaidTill().format(dateFormatter))
                .collect(Collectors.joining(", "));

        if (expired) {
            accountNotificationHelper.push(new DomainExpiredPush(
                    account.getId(), account.getName() + " Срочно продлите домен",
                    "У " + domainsCount + " истек срок регистрации: " + domainsForPush
            ));
        } else {
            accountNotificationHelper.push(new DomainExpiredPush(
                    account.getId(), account.getName() + " Окончание срока регистрации домена",
                    "У " + domainsCount + " истекает срок регистрации: " + domainsForPush
            ));
        }
    }

    private void notifyForDomainNoProlongNoMoney(PersonalAccount account, List<Domain> domains) {
        if (domains != null && !domains.isEmpty()) {

            //Email
            String balance = accountNotificationHelper.getBalanceForEmail(account);

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            String domainsForMail = "";
            for (Domain domain : domains) {
                domainsForMail += String.format(
                        "%-20s - %s<br>",
                        domain.getName(),
                        domain.getRegSpec().getPaidTill().format(dateFormatter)
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

            String domainsForPush = domains.stream()
                    .map(d -> d.getName() + " - " + d.getRegSpec().getPaidTill().format(dateFormatter))
                    .collect(Collectors.joining(", "));

            String domainsCount = pluralizef("%d домен", "%d домена", "%d доменов", domains.size());

            accountNotificationHelper.push(new DomainExpiredPush(
                    account.getId(), account.getName() + " Невозможно продлить " + domainsCount,
                    "Недостаточно средств для автоматического продления следующих доменов: " + domainsForPush
            ));

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

    private void setAsActiveAccountPromotion(SimpleServiceMessage message) {
        if (message.getParam("accountPromotionId") != null) {
            accountPromotionManager.setAsActiveAccountPromotionById((String) message.getParam("accountPromotionId"));
        }
    }

    private void setAsUsedAccountPromotion(SimpleServiceMessage message, String domainName) {
        if (message.getParam("accountPromotionId") != null) {
            accountPromotionManager.setAsUsedAccountPromotionById((String) message.getParam("accountPromotionId"),
                    "Использован на домене: " + domainName);
        }
    }

    public void blockMoneyBeforeManualRenewOrRegistrationExistsDomain(
            PersonalAccount account, SimpleServiceMessage message, Domain domain
    ) {
        blockMoneyForExistsDomain(account, message, domain,
                accountPromotionManager.findByPersonalAccountIdAndActive(account.getId(), true));
    }

    private void blockMoneyBeforeAutoRenewExistsDomain(
            PersonalAccount account, SimpleServiceMessage message, Domain domain
    ) {
        blockMoneyForExistsDomain(account, message, domain, Collections.emptyList());
    }

    private void blockMoneyForExistsDomain(
            PersonalAccount account, SimpleServiceMessage message, Domain domain, List<AccountPromotion> accountPromotions
    ) {
        boolean isRenew = message.getParam("renew") != null && (boolean) message.getParam("renew");
        boolean isRegistration = message.getParam("register") != null && (boolean) message.getParam("register");

        PaymentService service = isRegistration
                ? getDomainTld(domain.getName()).getRegistrationService()
                : isRenew
                ? getDomainTld(domain).getRenewService()
                : null;

        BigDecimal premiumPrice = isRegistration
                ? getAvailabilityInfo(domain.getName()).getPremiumPrice()
                : isRenew
                ? domainRegistrarFeignClient.getRenewPremiumPrice(domain.getName())
                : null;

        if (service == null) {
            throw new InternalApiException("Не удалось найти услугу для продления домена " + domain.getName());
        }

        Container<BigDecimal> costContainer = new Container<>(service.getCost());
        Container<Optional<AccountPromotion>> promotionContainer = new Container<>(Optional.empty());

        if (premiumPrice != null && premiumPrice.compareTo(BigDecimal.ZERO) > 0) {
            costContainer.setData(premiumPrice);
        } else {
            findPromotion(accountPromotions, service, domain).ifPresent(accountPromotion -> {
                        promotionContainer.setData(Optional.of(accountPromotion));
                        costContainer.setData(
                                getDiscountCost(costContainer.getData(), accountPromotion)
                        );

                        message.addParam("accountPromotionId", accountPromotion.getId());
                    });
        }

        if (costContainer.getData().compareTo(BigDecimal.ZERO) > 0) {
            accountHelper.checkBalanceWithoutBonus(account, costContainer.getData());
        }

        setAsUsedAccountPromotion(message, domain.getName());
        promotionContainer.getData().ifPresent(a -> a.setActive(false));

        if (costContainer.getData().compareTo(BigDecimal.ZERO) > 0) {
            try {
                ChargeMessage chargeMessage = new ChargeMessage.Builder(service)
                        .setAmount(costContainer.getData())
                        .excludeBonusPaymentType()
                        .setComment(domain.getName())
                        .build();

                    SimpleServiceMessage blockResult = accountHelper.block(account, chargeMessage);

                    String documentNumber = (String) blockResult.getParam("documentNumber");
                    message.addParam("documentNumber", documentNumber);
            } catch (Throwable e) {
                logger.error("Catch exception when block money for domain service  account {} domain {} e {} message {}",
                        account.getId(), domain.getName(), e.getClass(), e.getMessage()
                );
                setAsActiveAccountPromotion(message);
                promotionContainer.getData().ifPresent(a -> a.setActive(true));
                throw e;
            }
        }
    }

    private Optional<AccountPromotion> findPromotion(
            List<AccountPromotion> accountPromotions, PaymentService service, Domain domain
    ) {
        return accountPromotions.stream()
                .filter(p -> {
                    if (!p.isValidNow()) return false;

                    PromocodeActionType actionType = p.getAction().getActionType();
                    List serviceIds = (List) p.getAction().getProperties().get("serviceIds");
                    Object domainName = p.getProperties().get("domainName");

                    return actionType.equals(PromocodeActionType.SERVICE_DISCOUNT)
                            && (serviceIds).contains(service.getId())
                            && (domainName == null || domainName.equals(domain.getName()));
                }).max((p1, p2) -> {
            Object d1 = p1.getProperties().get("domainName");
            Object d2 = p2.getProperties().get("domainName");
            return d1 == d2 ? 0 : d1 != null ? 1 : -1;
        });
    }

    private void processAddRenewPromotions(List<Domain> domains, PersonalAccount account) {
        Promotion promotion = promotionRepository.findByName("ru_rf_domain_renew_by_name_discount");

        List<AccountPromotion> existsRenewPromotions = accountPromotionManager.findByPersonalAccountIdAndPromotionId(account.getId(), promotion.getId())
                .stream().filter(AccountPromotion::isValidNow)
                .collect(Collectors.toList());

        Lazy<Integer> domainCount = new Lazy<>(() -> rcUserFeignClient.getDomains(account.getAccountId()).size());

        if (existsRenewPromotions.isEmpty() && domainCount.get() > 7) {
            logger.info("account {} domains count > 7 and promotions not exists, can't add renew discount", account.getId());
            return;
        }

        Map<PromocodeAction, List<PaymentService>> actionIdAndRenewService = new HashMap<>();

        for (PromocodeAction action : promotion.getActions()) {
            List<PaymentService> renewServices = new ArrayList<>();

            paymentServiceRepository.findAllById(
                    (List<String>) action.getProperties().get("serviceIds")
            ).forEach(renewServices::add);

            actionIdAndRenewService.put(action, renewServices);
        }

        Lazy<List<String>> emails = new Lazy<>(() -> accountHelper.getEmails(account));

        domains.forEach(domain -> {
            Container<PromocodeAction> action = new Container<>();
            Container<PaymentService> service = new Container<>();

            try {
                DomainTld domainTld = getDomainTld(domain);
                service.setData(domainTld.getRenewService());

                actionIdAndRenewService.entrySet().stream().filter(entry -> entry.getValue()
                        .stream()
                        .anyMatch(s -> s.getId().equals(domainTld.getRenewServiceId())))
                        .findFirst()
                        .ifPresent(entry -> action.setData(entry.getKey()));

                if (action.getData() == null) {
                    return;
                }

                BigDecimal premiumPrice = domainRegistrarFeignClient.getRenewPremiumPrice(domain.getName());

                if (premiumPrice != null && premiumPrice.compareTo(BigDecimal.ZERO) > 0) {
                    logger.info("account {} domain {} has premium price {} , can't add renew discount", account.getId(), domain.getName());
                    return;
                }
            } catch (Exception e) {
                return;
            }

            Container<AccountPromotion> accountPromotionContainer = new Container<>();
            accountPromotionContainer.setData(existsRenewPromotions.stream()
                    .filter(ap -> ap.getActionId().equals(action.getData().getId())
                            && domain.getName().equals(ap.getProperties().get("domainName")))
                    .findFirst().orElse(null)
            );

            if (accountPromotionContainer.getData() == null && domainCount.get() <= 7) {

                AccountPromotion accountPromotion = accountPromotionFactory.build(account, promotion, action.getData());

                accountPromotion.getProperties().put("domainName", domain.getName());

                accountPromotionManager.insert(accountPromotion);

                accountPromotionContainer.setData(accountPromotion);

                history.save(account, "Добавлена скидка " + action.getData().getDescription()
                        + " для домена " + domain.getName());
            }

            if (accountPromotionContainer.getData() != null) {
                BigDecimal discountCost = getDiscountCost(
                        service.getData().getCost(), accountPromotionContainer.getData()
                );

                accountNotificationHelper.emailBuilder()
                        .account(account)
                        .emails(emails.get())
                        .apiName("HmsDomainSkidka50")
                        .param("reg_till", domain.getRegSpec().getPaidTillAsString())
                        .param("days_to_free_date", Utils.pluralizeDays(differenceInDays(LocalDate.now(), domain.getRegSpec().getFreeDate())))
                        .param("old_cost", service.getData().getCost().setScale(2, BigDecimal.ROUND_UP).toString())
                        .param("new_cost", Utils.currencyValue(discountCost))
                        .param("person", domain.getPerson().getName())
                        .param("domain", domain.getName())
                        .send();
            }
        });
    }

    private BigDecimal getDiscountCost(BigDecimal cost, AccountPromotion accountPromotion) {
        if (accountPromotion == null) return cost;
        return discountFactory.getDiscount(
                accountPromotion.getAction()
        ).getCost(cost);
    }

    public BigDecimal getRenewCost(String personalAccountId, String domainId) {
        Domain domain = rcUserFeignClient.getDomain(personalAccountId, domainId);
        DomainTld domainTld = getDomainTld(domain);

        BigDecimal renewPremiumPrice = domainRegistrarFeignClient.getRenewPremiumPrice(domain.getName());

        if (renewPremiumPrice != null && renewPremiumPrice.compareTo(domainTld.getRenewService().getCost()) > 0) {
            return renewPremiumPrice;
        }
        List<AccountPromotion> accountPromotions = accountPromotionManager.findByPersonalAccountIdAndActive(
                personalAccountId, true);

        AccountPromotion accountPromotion = accountPromotions.stream().filter(ap -> {
            switch (ap.getAction().getActionType()) {
                case SERVICE_DISCOUNT:
                    List<String> serviceIds = (List<String>) ap.getAction().getProperties().get("serviceIds");
                    Object domainName = ap.getProperties().get("domainName");
                    return serviceIds.contains(domainTld.getRenewServiceId())
                            && (domainName == null || domain.getName().equals(domainName));

                default:
                    return false;
            }
        }).findFirst().orElse(null);

        return getDiscountCost(domainTld.getRenewService().getCost(), accountPromotion);
    }
}
