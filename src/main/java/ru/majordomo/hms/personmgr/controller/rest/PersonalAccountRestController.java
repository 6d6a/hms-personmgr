package ru.majordomo.hms.personmgr.controller.rest;

import com.google.common.collect.ImmutableSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import ru.majordomo.hms.personmgr.common.AccountSetting;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.TokenType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountCheckQuotaEvent;
import ru.majordomo.hms.personmgr.event.account.AccountPasswordChangedEvent;
import ru.majordomo.hms.personmgr.event.account.AccountPasswordRecoverConfirmedEvent;
import ru.majordomo.hms.personmgr.event.account.AccountPasswordRecoverEvent;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.event.token.TokenDeleteEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.ContactInfo;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.notification.Notification;
import ru.majordomo.hms.personmgr.model.token.Token;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.PlanChangeAgreement;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.NotificationRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.*;
import ru.majordomo.hms.personmgr.model.account.projection.PersonalAccountWithNotificationsProjection;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.PlanChange.Factory;
import ru.majordomo.hms.personmgr.service.PlanChange.Processor;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.personmgr.service.TokenHelper;
import ru.majordomo.hms.personmgr.validation.ObjectId;
import ru.majordomo.hms.rc.user.resources.Domain;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.PhoneNumberManager.phoneValid;
import static ru.majordomo.hms.personmgr.common.RequiredField.ACCOUNT_PASSWORD_CHANGE;
import static ru.majordomo.hms.personmgr.common.RequiredField.ACCOUNT_PASSWORD_RECOVER;
import static ru.majordomo.hms.personmgr.common.Utils.getClientIP;

@RestController
@Validated
public class PersonalAccountRestController extends CommonRestController {
    private final PlanRepository planRepository;
    private final AccountOwnerManager accountOwnerManager;
    private final RcUserFeignClient rcUserFeignClient;
    private final ApplicationEventPublisher publisher;
    private final AccountHelper accountHelper;
    private final TokenHelper tokenHelper;
    private final NotificationRepository notificationRepository;
    private final AccountServiceRepository accountServiceRepository;
    private final AccountServiceHelper accountServiceHelper;
    private final Factory planChangeFactory;

    @Autowired
    public PersonalAccountRestController(
            PlanRepository planRepository,
            AccountOwnerManager accountOwnerManager,
            RcUserFeignClient rcUserFeignClient,
            ApplicationEventPublisher publisher,
            AccountHelper accountHelper,
            TokenHelper tokenHelper,
            NotificationRepository notificationRepository,
            AccountServiceRepository accountServiceRepository,
            AccountServiceHelper accountServiceHelper,
            Factory planChangeFactory
    ) {
        this.planRepository = planRepository;
        this.accountOwnerManager = accountOwnerManager;
        this.rcUserFeignClient = rcUserFeignClient;
        this.publisher = publisher;
        this.accountHelper = accountHelper;
        this.tokenHelper = tokenHelper;
        this.notificationRepository = notificationRepository;
        this.accountServiceRepository = accountServiceRepository;
        this.accountServiceHelper = accountServiceHelper;
        this.planChangeFactory = planChangeFactory;
    }

    @RequestMapping(value = "/accounts",
                    method = RequestMethod.GET)
    public ResponseEntity<Page<PersonalAccount>> getAccounts(@RequestParam("accountId") String accountId, Pageable pageable) {
        Page<PersonalAccount> accounts = accountManager.findByAccountIdContaining(accountId, pageable);

        if (accounts == null || !accounts.hasContent()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/account",
                    method = RequestMethod.GET)
    public ResponseEntity<PersonalAccount> getAccount(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/plan",
                    method = RequestMethod.GET)
    public ResponseEntity<Plan> getAccountPlan(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Plan plan = planRepository.findOne(account.getPlanId());

        if (plan == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(plan, HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/plan/{planId}",
                    method = RequestMethod.POST)
    public ResponseEntity<Object> changeAccountPlan(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "planId") String planId,
            @RequestBody PlanChangeAgreement planChangeAgreement
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Plan currentPlan = planRepository.findOne(account.getPlanId());
        Plan newPlan = planRepository.findOne(planId);

        Processor planChangeProcessor = planChangeFactory.createPlanChangeProcessor(currentPlan, newPlan);
        planChangeProcessor.setAccount(account);
        PlanChangeAgreement planChangeAgreementToCompare = planChangeProcessor.isPlanChangeAllowed();

        if (planChangeAgreementToCompare.equals(planChangeAgreement) && planChangeAgreement.getPlanChangeAllowed()) {
            planChangeProcessor.setPlanChangeRequired(true);
            planChangeProcessor.process();
        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/plan-check/{planId}",
                    method = RequestMethod.POST)
    public ResponseEntity<PlanChangeAgreement> changeAccountPlanCheck(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "planId") String planId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Plan currentPlan = planRepository.findOne(account.getPlanId());
        Plan newPlan = planRepository.findOne(planId);

        Processor planChangeProcessor = planChangeFactory.createPlanChangeProcessor(currentPlan, newPlan);
        planChangeProcessor.setAccount(account);
        PlanChangeAgreement planChangeAgreement = planChangeProcessor.isPlanChangeAllowed();

        if (!planChangeAgreement.getErrors().isEmpty()) {
            return new ResponseEntity<>(planChangeAgreement, HttpStatus.FORBIDDEN);
            //Ошибки хранятся в planChangeAgreement.getErrors()
        } else if (planChangeAgreement.getNeedToFeelBalance().compareTo(BigDecimal.ZERO) != 0) {
            return new ResponseEntity<>(planChangeAgreement, HttpStatus.ACCEPTED); // 202 Accepted
        } else {
            return new ResponseEntity<>(planChangeAgreement, HttpStatus.OK);
        }
    }

    @RequestMapping(value = "/change-email",
            method = RequestMethod.GET)
    public ResponseEntity<Object> confirmEmailsChange(
            @RequestParam("token") String tokenId,
            HttpServletRequest request,
            @RequestHeader HttpHeaders httpHeaders
    ) {
        logger.debug("confirmChangeOwnerEmail httpHeaders: " + httpHeaders.toString());

        Token token = tokenHelper.getToken(tokenId, TokenType.CHANGE_OWNER_EMAILS);
        if (token == null) {
            return new ResponseEntity<>(
                    createErrorResponse(
                            "Запрос на изменение контактных e-mail адресов не найден или уже выполнен ранее."
                    ),
                    HttpStatus.BAD_REQUEST
            );
        }

        PersonalAccount account = accountManager.findOne(token.getPersonalAccountId());
        if (account == null) {
            return new ResponseEntity<>(
                    createErrorResponse(
                            "Аккаунт не найден."
                    ),
                    HttpStatus.BAD_REQUEST
            );
        }
        List<String> newEmail = (List<String>) token.getParam("newemails");
        AccountOwner accountOwner = accountOwnerManager.findOneByPersonalAccountId(account.getId());
        ContactInfo contactInfo = accountOwner.getContactInfo();
        contactInfo.setEmailAddresses(newEmail);
        accountOwner.setContactInfo(contactInfo);
        accountOwnerManager.save(accountOwner);

        publisher.publishEvent(new TokenDeleteEvent(token));

        SimpleServiceMessage message = createSuccessResponse("Email-адреса владельца аккаунта успешно изменены. ");
        return new ResponseEntity<>(
                message,
                HttpStatus.OK
        );
    }

    @RequestMapping(value = "/{accountId}/password",
                    method = RequestMethod.POST)
    public ResponseEntity<Object> changePassword(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        checkRequiredParams(requestBody, ACCOUNT_PASSWORD_CHANGE);

        String password = (String) requestBody.get(PASSWORD_KEY);

        accountHelper.changePassword(account, password);

        String ip = getClientIP(request);

        Map<String, String> params = new HashMap<>();
        params.put(IP_KEY, ip);

        publisher.publishEvent(new AccountPasswordChangedEvent(account, params));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/password-recovery",
                    method = RequestMethod.POST)
    public ResponseEntity<Object> requestPasswordRecovery(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request,
            @RequestHeader HttpHeaders httpHeaders
    ) {
        logger.debug("confirmPasswordRecovery httpHeaders: " + httpHeaders.toString());

        checkRequiredParams(requestBody, ACCOUNT_PASSWORD_RECOVER);

        String accountId = (String) requestBody.get(ACCOUNT_ID_KEY);

        PersonalAccount account = null;

        Pattern p = Pattern.compile("(?ui)(^ac_|^ас_)(\\d*)");
        Matcher m = p.matcher(accountId);

        if (m.matches() && m.groupCount() == 2) {
            accountId = m.group(2);

            if (accountId.length() > 0) {
                account = accountManager.findByAccountId(accountId);
            }
        } else {
            Domain domain;

            try {
                domain = rcUserFeignClient.findDomain(accountId);

                if (domain != null) {
                    accountId = domain.getAccountId();
                    account = accountManager.findOne(accountId);
                } else {
                    return new ResponseEntity<>(createErrorResponse("Домен не найден."), HttpStatus.NOT_FOUND);
                }
            } catch (Exception e) {
                logger.error("[requestPasswordRecovery] exception :" + e.getMessage());
                return new ResponseEntity<>(createErrorResponse("Домен не найден."), HttpStatus.NOT_FOUND);
            }
        }

        if (account != null) {
            String ip = getClientIP(request);

            Map<String, String> params = new HashMap<>();
            params.put(IP_KEY, ip);

            publisher.publishEvent(new AccountPasswordRecoverEvent(account, params));

            return new ResponseEntity<>(createSuccessResponse("Произведен запрос на восстановление пароля."), HttpStatus.OK);
        }

        return new ResponseEntity<>(createErrorResponse("Аккаунт/домен не найден."), HttpStatus.NOT_FOUND);
    }
    @RequestMapping(value = "/password-recovery",
                    method = RequestMethod.GET)
    public ResponseEntity<Object> confirmPasswordRecovery(
            @RequestParam("token") String tokenId,
            HttpServletRequest request,
            @RequestHeader HttpHeaders httpHeaders
    ) {
        logger.debug("confirmPasswordRecovery httpHeaders: " + httpHeaders.toString());

        Token token = tokenHelper.getToken(tokenId);

        if (token == null) {
            return new ResponseEntity<>(
                    createErrorResponse(
                    "Запрос на восстановление пароля не найден или уже выполнен ранее."
                    ),
                    HttpStatus.BAD_REQUEST
            );
        }

        PersonalAccount account = accountManager.findOne(token.getPersonalAccountId());

        if (account == null) {
            return new ResponseEntity<>(
                    createErrorResponse(
                            "Аккаунт не найден."
                    ),
                    HttpStatus.BAD_REQUEST
            );
        }

        String ip = getClientIP(request);

        String password = randomAlphabetic(8);

        accountHelper.changePassword(account, password);

        Map<String, String> params = new HashMap<>();
        params.put(PASSWORD_KEY, password);
        params.put(IP_KEY, ip);

        publisher.publishEvent(new AccountPasswordRecoverConfirmedEvent(account, params));

        publisher.publishEvent(new TokenDeleteEvent(token));

        SimpleServiceMessage message = createSuccessResponse("Пароль успешно восстановлен. " +
                "Новый пароль отправлен на контактный e-mail владельца аккаунта.");
        message.addParam("password", password);
        message.addParam("login", account.getName());

        return new ResponseEntity<>(
                message,
                HttpStatus.OK
        );
    }

    @RequestMapping(value = "/{accountId}/google-adwords-promocode", method = RequestMethod.GET)
    public ResponseEntity<Object> getGooglePromocode(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Map<String, Object> message = new HashMap<>();

        String code = accountHelper.getGooglePromocode(account);
        if (code != null) {
            message.put("promocode", code);
        } else {
            message.put("promocode", null);
            message.put("canGetPromocode", accountHelper.isGooglePromocodeAllowed(account));
        }

        return new ResponseEntity<>(
                message,
                HttpStatus.OK
        );
    }

    @RequestMapping(value = "/{accountId}/google-adwords-promocode", method = RequestMethod.POST)
    public ResponseEntity<Object> generateGooglePromocode(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Map<String, Object> message = new HashMap<>();

        String code = accountHelper.getGooglePromocode(account);
        if (code != null) {
            message.put("promocode", code);
        } else {
            code = accountHelper.giveGooglePromocode(account);
            message.put("promocode", code);

            //Save history
            String operator = request.getUserPrincipal().getName();
            Map<String, String> params = new HashMap<>();
            params.put(HISTORY_MESSAGE_KEY, "Пользователю выдан промокод google adwords (" + code + ")");
            params.put(OPERATOR_KEY, operator);

            publisher.publishEvent(new AccountHistoryEvent(account.getId(), params));
        }

        return new ResponseEntity<>(
                message,
                HttpStatus.OK
        );
    }

    @RequestMapping(value = "/{accountId}/account/settings",
            method = RequestMethod.PATCH)
    public ResponseEntity<Object> setSettings(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, Object> requestBody,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        if (requestBody.get(AccountSetting.CREDIT.name()) != null) {
            Boolean credit = (Boolean)requestBody.get(AccountSetting.CREDIT.name());
            if (!credit) {
                // Выключение кредита
                if (account.isCredit() && account.getCreditActivationDate() != null) {
                    // Кредит был активирован (Прошло первое списание)
                    throw new ParameterValidationException("Credit already activated. Credit disabling prohibited.");
                }
            } else {
                if (planRepository.findOne(account.getPlanId()).isAbonementOnly()) {
                    throw new ParameterValidationException("Включение кредита невозможно на вашем тарифном плане");
                }
                // Включение кредита
                if (!account.isCredit() && !account.isActive()) {
                    accountHelper.enableAccount(account);
                }
            }
            accountManager.setCredit(accountId, credit);

            //Save history
            String operator = request.getUserPrincipal().getName();
            Map<String, String> params = new HashMap<>();
            params.put(HISTORY_MESSAGE_KEY, (credit ? "Включен": "Выключен") + " кредит");
            params.put(OPERATOR_KEY, operator);

            publisher.publishEvent(new AccountHistoryEvent(accountId, params));
        }

        if (requestBody.get(AccountSetting.ADD_QUOTA_IF_OVERQUOTED.name()) != null) {
            Boolean addQuotaIfOverquoted = (Boolean) requestBody.get(AccountSetting.ADD_QUOTA_IF_OVERQUOTED.name());
            accountManager.setAddQuotaIfOverquoted(accountId, addQuotaIfOverquoted);

            //Установим новую квоту, начислим услуги и тд.
            account.setAddQuotaIfOverquoted(addQuotaIfOverquoted);
            publisher.publishEvent(new AccountCheckQuotaEvent(account));

            //Save history
            String operator = request.getUserPrincipal().getName();
            Map<String, String> params = new HashMap<>();
            params.put(HISTORY_MESSAGE_KEY, (addQuotaIfOverquoted ? "Включено": "Выключено") + " добавление квоты при превышении доступной по тарифу");
            params.put(OPERATOR_KEY, operator);

            publisher.publishEvent(new AccountHistoryEvent(accountId, params));
        }

        if (requestBody.get(AccountSetting.AUTO_BILL_SENDING.name()) != null) {
            Boolean autoBillSending = (Boolean) requestBody.get(AccountSetting.AUTO_BILL_SENDING.name());
            accountManager.setAutoBillSending(accountId, autoBillSending);

            //Save history
            String operator = request.getUserPrincipal().getName();
            Map<String, String> params = new HashMap<>();
            params.put(HISTORY_MESSAGE_KEY, (autoBillSending ? "Включена": "Выключена") + " автоматическая отправка бухгалтерских документов");
            params.put(OPERATOR_KEY, operator);

            publisher.publishEvent(new AccountHistoryEvent(accountId, params));
        }

        if (requestBody.get(AccountSetting.NOTIFY_DAYS.name()) != null) {
            Integer notifyDays = (Integer) requestBody.get(AccountSetting.NOTIFY_DAYS.name());

            Set<Integer> notifyDaysVariants = ImmutableSet.of(7, 14, 30);
            if (!notifyDaysVariants.contains(notifyDays)) {
                throw new ParameterValidationException("Установить срок отправки уведомлений возможно только на " + notifyDaysVariants + " дней");
            }

            accountManager.setNotifyDays(accountId, notifyDays);

            //Save history
            String operator = request.getUserPrincipal().getName();
            Map<String, String> params = new HashMap<>();
            params.put(HISTORY_MESSAGE_KEY, "Установлен срок отправки уведомлений об окончании оплаченного периода хостинга на '" + notifyDays + "' дней");
            params.put(OPERATOR_KEY, operator);

            publisher.publishEvent(new AccountHistoryEvent(accountId, params));
        }

        if (requestBody.get(AccountSetting.SMS_PHONE_NUMBER.name()) != null) {
            String smsPhoneNumber = (String)requestBody.get(AccountSetting.SMS_PHONE_NUMBER.name());

            if (smsPhoneNumber.equals("") && accountServiceHelper.hasSmsNotifications(account)) {
                throw new ParameterValidationException("SMSPhoneNumber can't be empty with active sms notifications.");
            }

            if (!phoneValid(smsPhoneNumber) && !smsPhoneNumber.equals("")) {
                throw new ParameterValidationException("SMSPhoneNumber is not valid.");
            }

            accountManager.setSmsPhoneNumber(accountId, smsPhoneNumber);

            //Save history
            String operator = request.getUserPrincipal().getName();
            Map<String, String> params = new HashMap<>();
            params.put(HISTORY_MESSAGE_KEY, "Установлен телефон для СМС-уведомлений на '" + smsPhoneNumber + "'");
            params.put(OPERATOR_KEY, operator);

            publisher.publishEvent(new AccountHistoryEvent(accountId, params));
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/account/notifications",
            method = RequestMethod.PATCH)
    public ResponseEntity<Object> setNotifications(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Set<MailManagerMessageType> notifications,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Set<MailManagerMessageType> oldNotifications = account.getNotifications();

        accountManager.setNotifications(accountId, notifications);

        //Save history
        String operator = request.getUserPrincipal().getName();
        Map<String, String> params = new HashMap<>();
        params.put(HISTORY_MESSAGE_KEY, "Изменен список уведомлений аккаунта c [" + oldNotifications + "] на [" + notifications + "]");
        params.put(OPERATOR_KEY, operator);

        publisher.publishEvent(new AccountHistoryEvent(accountId, params));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/account/notifications/sms",
                    method = RequestMethod.PATCH)
    public ResponseEntity<Object> setSmsNotifications(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Set<MailManagerMessageType> notifications,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        if (notifications.isEmpty() && accountServiceHelper.hasSmsNotifications(account)) {
            throw new ParameterValidationException("Для отправки SMS-уведомлений необходимо выбрать хотя бы один вид уведомлений.");
        }

        setNotifications(account, notifications, "SMS_", request);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/account/notifications/email-news",
            method = RequestMethod.POST)
    public ResponseEntity<Object> addNotification(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, Object> requestBody,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        try {
            boolean newState = (boolean) requestBody.get("enabled");
            setNotification(accountId, MailManagerMessageType.EMAIL_NEWS, newState, request);
        } catch (ClassCastException e) {
            return new ResponseEntity<>(
                    this.createErrorResponse("Некорректный параметр запроса."),
                    HttpStatus.BAD_REQUEST
            );
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/is_subscribed_account",
                    method = RequestMethod.GET)
    public ResponseEntity<Map<String, Boolean>> isSubscribed(
            @ObjectId(
                    value = PersonalAccount.class,
                    idFieldName = "accountId"
            )
            @RequestParam(value = "accountId") String accountId
    ) {
        PersonalAccountWithNotificationsProjection account = accountManager.findOneByAccountIdWithNotifications(accountId);

        Map<String, Boolean> isSubscribedResult = new HashMap<>();
        isSubscribedResult.put("is_subscribed", account.hasNotification(MailManagerMessageType.EMAIL_NEWS));

        return new ResponseEntity<>(isSubscribedResult, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @RequestMapping(value = "/{accountId}/account/toggle_state",
            method = RequestMethod.POST)
    public ResponseEntity<Object> toggleAccount(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        accountHelper.switchAccountActiveState(account, !account.isActive());

        String operator = request.getUserPrincipal().getName();
        Map<String, String> params = new HashMap<>();
        params.put(HISTORY_MESSAGE_KEY, "Аккаунт " + (!account.isActive() ? "включен" : "выключен"));
        params.put(OPERATOR_KEY, operator);

        publisher.publishEvent(new AccountHistoryEvent(accountId, params));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void setNotifications(PersonalAccount account, Set<MailManagerMessageType> notifications, String pattern, SecurityContextHolderAwareRequestWrapper request) {

        Set<MailManagerMessageType> oldNotifications = account.getNotifications();

        //Получаем список только изменяемых уведомлений
        notifications = notifications.stream()
                .filter(mailManagerMessageType -> mailManagerMessageType.name().startsWith(pattern))
                .collect(Collectors.toSet());

        String newNotificationsAsString = notifications.toString();
        String oldNotificationsAsString = oldNotifications.stream()
                .filter(mailManagerMessageType -> mailManagerMessageType.name().startsWith(pattern))
                .toString();

        //К списку уведомлений добавляем все текущие другие уведомления
        notifications.addAll(
                oldNotifications.stream()
                        .filter(mailManagerMessageType -> !mailManagerMessageType.name().startsWith(pattern))
                        .collect(Collectors.toSet()
                        )
        );

        accountManager.setNotifications(account.getId(), notifications);

        //Save history
        String operator = request.getUserPrincipal().getName();
        String message = "Уведомления [" + pattern + "] изменены c [" + oldNotificationsAsString + "] на [" + newNotificationsAsString + "]";
        addHistoryMessage(operator, account.getId(), message);
    }

    private void setNotification(String accountId, MailManagerMessageType messageType, boolean state, SecurityContextHolderAwareRequestWrapper request) {

        PersonalAccount account = accountManager.findOne(accountId);
        boolean change = false;
        Set<MailManagerMessageType> notifications = account.getNotifications();
        Notification notification = notificationRepository.findByType(messageType);
        if (notification == null) {
            return;
        }

        if (!notifications.contains(messageType)
                && state) {
            notifications.add(messageType);
            change = true;
        } else if (notifications.contains(messageType)
                && !state) {
            notifications.remove(messageType);
            change = true;
        }
        if (change) {
            accountManager.setNotifications(accountId, notifications);
            String operator = request.getUserPrincipal().getName();
            String notificationName = notification.getName();
            String message = notificationName + (state ? " включено." : " отключено.");
            addHistoryMessage(operator, accountId, message);
        }
    }
}