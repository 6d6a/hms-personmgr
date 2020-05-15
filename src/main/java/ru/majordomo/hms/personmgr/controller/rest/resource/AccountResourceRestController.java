package ru.majordomo.hms.personmgr.controller.rest.resource;

//TODO Выделить работу с услугами, абонементами, и ежидневными услугами в один класс.

import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.util.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.*;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.feign.SiFeignClient;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.ContactInfo;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.account.PersonalInfo;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.Plans;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.personmgr.service.AccountTransferService;
import ru.majordomo.hms.personmgr.service.PreorderService;
import ru.majordomo.hms.personmgr.service.SequenceCounterService;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.RequiredField.ACCOUNT_CREATE;
import static ru.majordomo.hms.personmgr.common.RequiredField.ACCOUNT_TRANSFER;

@RestController
public class AccountResourceRestController extends CommonRestController {
    private final SequenceCounterService sequenceCounterService;
    private final ProcessingBusinessOperationRepository processingBusinessOperationRepository;
    private final PlanManager planManager;
    private final AccountServiceRepository accountServiceRepository;
    private final AccountOwnerManager accountOwnerManager;
    private final SiFeignClient siFeignClient;
    private final RcUserFeignClient rcUserFeignClient;
    private final AccountTransferService accountTransferService;
    private final PreorderService preorderService;

    @Autowired
    public AccountResourceRestController(
            SequenceCounterService sequenceCounterService,
            ProcessingBusinessOperationRepository processingBusinessOperationRepository,
            PlanManager planManager,
            AccountServiceRepository accountServiceRepository,
            AccountOwnerManager accountOwnerManager,
            SiFeignClient siFeignClient,
            RcUserFeignClient rcUserFeignClient,
            PreorderService preorderService,
            AccountTransferService accountTransferService

    ) {
        this.sequenceCounterService = sequenceCounterService;
        this.processingBusinessOperationRepository = processingBusinessOperationRepository;
        this.planManager = planManager;
        this.accountServiceRepository = accountServiceRepository;
        this.accountOwnerManager = accountOwnerManager;
        this.siFeignClient = siFeignClient;
        this.rcUserFeignClient = rcUserFeignClient;
        this.accountTransferService = accountTransferService;
        this.preorderService = preorderService;
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response
    ) {
       logger.debug("Got SimpleServiceMessage: " + message.toString());

        Utils.checkRequiredParams(message.getParams(), ACCOUNT_CREATE);

        boolean agreement = (boolean) message.getParam("agreement");

        if (!agreement) {
            logger.debug("Agreement not accepted");
            return this.createErrorResponse("Необходимо согласиться с условиями оферты");
        }

        String password = randomAlphabetic(8);

        Plan plan = planManager.findByOldId((String) message.getParam("plan"));

        if (plan == null) {
            logger.debug("No plan found with OldId: " + message.getParam("plan"));
            return this.createErrorResponse("Не найден тарифный план с id: " + message.getParam("plan"));
        }

        if (!plan.isActive()) {
            logger.info("Попытка регистрации на неактивном тарифе. message: " + message.toString());
            return this.createErrorResponse("Не найден тарифный план с id: " + message.getParam("plan"));
        }

        EmailValidator validator = EmailValidator.getInstance(true, true);
        List<String> emails = (List<String>) message.getParam("emailAddresses");

        for (String emailAddress: emails) {
            if (!validator.isValid(emailAddress)) {
                return this.createErrorResponse("Адрес " + emailAddress + " некорректен");
            }
        }

        AccountOwner.Type accountOwnerType;
        try {
            accountOwnerType = AccountOwner.Type.valueOf((String) message.getParam("type"));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            logger.debug("No or wrong type of accountOwner: " + message.getParam("type"));
            return this.createErrorResponse("Не указан, либо неверно указан тип владельца аккаунта");
        }
        List<String> phoneNumbers = new ArrayList<>();;

        if (message.getParam("phone") != null) { // можно отправить один телефон или массив телефонов.
            try {
                if (message.getParam("phone") instanceof String) {
                    phoneNumbers.add((String) message.getParam("phone"));
                } else if (message.getParam("phone") instanceof List) {
                    phoneNumbers.addAll((List<String>) message.getParam("phone"));
                } else {
                    throw new IllegalArgumentException();
                }
                phoneNumbers = phoneNumbers.stream().filter(p -> !StringUtils.isEmpty(p))
                        .map(phone -> phone.replaceAll("\\s+", ""))
                        .peek(phone -> { if (!PhoneNumberManager.phoneValid(phone)) throw new IllegalArgumentException(); })
                        .collect(Collectors.toList()); // потому что php высылает телефон с пробелами и все портит
            } catch (ClassCastException | IllegalArgumentException ex) {
                logger.debug("Wrong phonenumbers: " + message.getParam("phone"));
                return this.createErrorResponse("Неверно указаны номера телефона");
            }
        }

        Period planPeriod = null;
        boolean trial = false;

        if (Plans.PARKING_DOMAIN.oldIdStr().equals(plan.getOldId())) {  // для тарифа парковка домена предзаказ недоступен
            planPeriod = null;
        } else if ("TRIAL".equals(message.getParam("period"))) {
            trial = true;
            if (plan.getFreeTrialAbonement() == null) {
                logger.debug("Trial abonement not allowed for plan: " + plan);
                return this.createErrorResponse(String.format("Для тарифного плана '%s' не доступен тестовый период", plan.getName()));
            }
        } else if (!StringUtils.isEmpty(message.getParam("period"))) {
            String periodStr = (String) message.getParam("period");
            try {
                planPeriod = Period.parse(periodStr);
            } catch (DateTimeParseException ex) {
                planPeriod = null;
            }
            if (planPeriod == null) {
                logger.debug("Wrong period: " + periodStr);
                return this.createErrorResponse("Неверно указан период " + periodStr);
            }
            Result result = preorderService.whyCannotPreorder(planPeriod, plan);
            if (!result.isSuccess()) {
                logger.debug("Wrong plan preorder: " + result.getMessage());
                return this.createErrorResponse(result.getMessage());
            }
        }

        Map<Feature, Period> additionServiceList = new HashMap<>(); // содержит предзаказы для дополнительных услуг, уже обработанные
        Map<String, String> services;
        try {
            Object rawServices = message.getParam("services");
            if (rawServices instanceof Map) {
                services = (Map<String, String>) rawServices;
            } else if (rawServices == null || (rawServices instanceof List && ((List) rawServices).isEmpty())) {
                // php вместо ассоциативного массива может прислать пустой нумерованный
                services = Collections.EMPTY_MAP;
            } else {
                throw new ClassCastException();
            }
        } catch (ClassCastException ex) {
            logger.debug("Wrong format preorder serveces, received: " + message.getParam("services"));
            return this.createErrorResponse("Внутренняя ошибка, список дополнительных услуг передан в неверном формате");
        }
        if (services != null && !services.isEmpty()) {
            for (Map.Entry<String, String> order : services.entrySet()) {
                Feature feature;
                try {
                    feature = Feature.valueOf(order.getKey());
                } catch (IllegalArgumentException ex) {
                    logger.debug("Wrong feature: " + order.getKey());
                    return this.createErrorResponse("Неверно указана дополнительная услуга");
                }
                Period servicePeriod;
                try {
                    servicePeriod = Period.parse(order.getValue());
                } catch (DateTimeParseException ex) {
                    servicePeriod = null;
                }
                if (servicePeriod == null) {
                    logger.debug("Wrong period: " + order.getValue());
                    return this.createErrorResponse("Неверно указан период " + order.getValue());
                }
                Result reasonText = preorderService.whyCannotPreorderService(servicePeriod, feature, plan);
                if (!reasonText.isSuccess()) {
                    logger.debug("Wrong addition service preorder: " + reasonText.getMessage());
                    return this.createErrorResponse(reasonText.getMessage());
                }
                additionServiceList.put(feature, servicePeriod);
            }
        }
        String smsPhoneNumber = "";
        if (additionServiceList.containsKey(Feature.SMS_NOTIFICATIONS)) {
            if (CollectionUtils.isEmpty(phoneNumbers) ||
                    !Pattern.compile("^(\\+7|8)\\d{10}").matcher(smsPhoneNumber = phoneNumbers.get(0)).matches()
            ) {
                additionServiceList.remove(Feature.SMS_NOTIFICATIONS);
            } else {
                smsPhoneNumber = phoneNumbers.get(0).startsWith("8") ? "+7" + phoneNumbers.get(0).substring(1) : phoneNumbers.get(0);
            }
        }

        PersonalAccount personalAccount = createPersonalAccount(
                plan,
                smsPhoneNumber,
                planPeriod != null || !additionServiceList.isEmpty() || trial
        );

        // Добавление прездаказов
        if (planPeriod != null) {
            preorderService.addPreorder(personalAccount, planPeriod, plan);
        } else if (trial) {
            preorderService.addPromoPreorder(personalAccount, plan.getFreeTrialAbonement());
        }

        for (Map.Entry<Feature, Period> pd: additionServiceList.entrySet()) {
            preorderService.addPreorder(personalAccount, pd.getValue(), pd.getKey(), plan);
        }

        createAccountOwner(personalAccount, emails, accountOwnerType, phoneNumbers, message);
        createAccountService(personalAccount, plan);


        //Сохраним в мессагу квоту по тарифу
        message.setAccountId(personalAccount.getId());
        message.addParam("username", personalAccount.getName());
        message.addParam(PASSWORD_KEY, password);

        ProcessingBusinessOperation processingBusinessOperation = businessHelper.buildOperation(BusinessOperationType.ACCOUNT_CREATE, message);

        logger.debug("processingBusinessOperation saved: " + processingBusinessOperation.toString());

        SimpleServiceMessage siResponse = null;
        try {
            siResponse = siFeignClient.createWebAccessAccount(message);
        } catch (Exception e) {
            logger.debug("WebAccessAccount creation failed when send create command on SI. Exception:" + e.toString());
            return this.createErrorResponse("WebAccessAccount creation failed when send create command on SI. Exception:" + e.getMessage());
        }

        if (siResponse.getParam("success") == null || !((boolean) siResponse.getParam("success"))) {
            return this.createErrorResponse("WebAccessAccount creation failed. Response params:" + siResponse.getParams());
        }

        message.addParam("token", siResponse.getParam("token"));

        ProcessingBusinessAction businessAction = businessHelper.buildActionByOperation(BusinessActionType.ACCOUNT_CREATE_FIN, message, processingBusinessOperation);

        logger.debug("ProcessingBusinessAction saved: " + businessAction.toString());

        processingBusinessOperation.addParam("token", siResponse.getParam("token"));
        processingBusinessOperationRepository.save(processingBusinessOperation);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        SimpleServiceMessage responseMessage = this.createSuccessResponse(businessAction);
        responseMessage.addParam("token", siResponse.getParam("token"));

        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", personalAccount.getName());
        credentials.put(PASSWORD_KEY, password);

        responseMessage.addParam("credentials", credentials);

        return responseMessage;
    }

    @PreAuthorize("hasAuthority('TRANSFER_ACCOUNT')")
    @RequestMapping(value = "/account/{accountId}/move", method = RequestMethod.POST)
    public Boolean moveAccount(
            @ObjectId(PersonalAccount.class) @PathVariable("accountId") String accountId,
            @RequestBody Map<String, String> params,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        Utils.checkRequiredParams(params, ACCOUNT_TRANSFER);

        String desiredServerId = params.get(SERVER_ID_KEY);
        if (desiredServerId.equals("")) {
            return false;
        }

        try {
            Map<String, String> body = new HashMap<>();
            body.put(SERVER_ID_KEY, desiredServerId);
            Boolean success = rcUserFeignClient.moveAccount(accountId, body);

            if (success) {
                history.save(accountId, "Сервер аккаунта " + accountId + " изменён на " + desiredServerId, request);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @PreAuthorize("hasAuthority('TRANSFER_ACCOUNT')")
    @PostMapping(value = "/account/{accountId}/transfer")
    public SimpleServiceMessage transferAccount(
            @ObjectId(PersonalAccount.class) @PathVariable("accountId") String accountId,
            @RequestBody SimpleServiceMessage message,
            SecurityContextHolderAwareRequestWrapper request,
            HttpServletResponse response
    ) {
        message.setAccountId(accountId);
        Utils.checkRequiredParams(message.getParams(), ACCOUNT_TRANSFER);

        String desiredServerId = (String) message.getParam(SERVER_ID_KEY);

        if (desiredServerId.equals("")) {
            throw new ParameterValidationException("Передано пустое значение в параметре " + SERVER_ID_KEY);
        }

        ProcessingBusinessAction businessAction;

        try {
            businessAction = accountTransferService.startTransfer(message);
        } catch (Exception e) {
            if (!(e instanceof ParameterValidationException)) {
                e.printStackTrace();
            }
            return this.createErrorResponse(e.getMessage());
        }

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        history.save(accountId, "Поступила заявка на перенос акканута на serverId: " + desiredServerId, request);

        return this.createSuccessResponse(businessAction);
    }

    private PersonalAccount createPersonalAccount(Plan plan, @Nullable String smsProneNumber, boolean preorder){
        String accountId = String.valueOf(sequenceCounterService.getNextSequence("PersonalAccount"));

        PersonalAccount personalAccount = new PersonalAccount();
        personalAccount.setAccountType(AccountType.VIRTUAL_HOSTING);
        personalAccount.setPlanId(plan.getId());
        personalAccount.setAccountId(accountId);
        personalAccount.setId(accountId);
        personalAccount.setClientId(accountId);
        personalAccount.setName(VH_ACCOUNT_PREFIX + accountId);
        personalAccount.setPreorder(preorder);
        personalAccount.setActive(!plan.isAbonementOnly() && !preorder);

        personalAccount.setCreated(LocalDateTime.now());
        personalAccount.setAccountNew(true);
        personalAccount.setCredit(false);
        personalAccount.setCreditPeriod("P14D");
        personalAccount.setNotifications(defaultNotifications());
        if (!StringUtils.isEmpty(smsProneNumber)) {
            personalAccount.setNotifications(EnumSet.of(
                    MailManagerMessageType.SMS_ABONEMENT_EXPIRING,
                    MailManagerMessageType.SMS_DOMAIN_DELEGATION_ENDING,
                    MailManagerMessageType.SMS_REMAINING_DAYS,
                    MailManagerMessageType.TELEGRAM_ABONEMENT_EXPIRING,
                    MailManagerMessageType.TELEGRAM_DOMAIN_DELEGATION_ENDING,
                    MailManagerMessageType.TELEGRAM_REMAINING_DAYS
            ));
            personalAccount.setSmsPhoneNumber(smsProneNumber);
        }

        accountManager.insert(personalAccount);
        logger.debug("personalAccount saved: " + personalAccount.toString());

        return personalAccount;
    }

    private Set<MailManagerMessageType> defaultNotifications() {
        return new HashSet<>(Collections.singletonList(MailManagerMessageType.EMAIL_NEWS));
    }

    private void createAccountOwner(
            PersonalAccount personalAccount,
            List<String> emails,
            AccountOwner.Type accountOwnerType,
            @Nullable List<String> phoneNumbers,
            SimpleServiceMessage message
    ) {
        String name = (String) message.getParam("name");
        String inn = (String) message.getParam("inn");

        ContactInfo contactInfo = new ContactInfo();
        contactInfo.setEmailAddresses(emails);
        if (!CollectionUtils.isEmpty(phoneNumbers)) {
            contactInfo.setPhoneNumbers(phoneNumbers);
        }


        AccountOwner accountOwner = new AccountOwner();
        accountOwner.setPersonalAccountId(personalAccount.getId());
        accountOwner.setContactInfo(contactInfo);
        accountOwner.setName(name);
        accountOwner.setType(accountOwnerType);


        if (inn != null) {
            PersonalInfo personalInfo = new PersonalInfo();
            personalInfo.setInn(inn);

            accountOwner.setPersonalInfo(personalInfo);
        }

        accountOwnerManager.insert(accountOwner);
        logger.debug("accountOwner saved: " + accountOwner.toString());
    }

    private void createAccountService(PersonalAccount personalAccount, Plan plan) {
        if (!plan.isAbonementOnly() && !preorderService.isHostingPreorder(personalAccount.getId())) {
            AccountService service = new AccountService();
            service.setPersonalAccountId(personalAccount.getId());
            service.setServiceId(plan.getServiceId());
            service.setEnabled(true);
            accountServiceRepository.save(service);
            logger.debug("AccountService saved: " + service.toString());
        }
    }
}
