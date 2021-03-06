package ru.majordomo.hms.personmgr.controller.rest;

import com.google.common.collect.ImmutableSet;
import com.querydsl.core.types.Predicate;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.AccountSetting;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.TokenType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.*;
import ru.majordomo.hms.personmgr.event.token.TokenDeleteEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.feign.SiFeignClient;
import ru.majordomo.hms.personmgr.feign.StatFeignClient;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.manager.TokenManager;
import ru.majordomo.hms.personmgr.model.account.*;
import ru.majordomo.hms.personmgr.model.account.projection.PersonalAccountWithNotificationsProjection;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.model.notification.Notification;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.PlanChangeAgreement;
import ru.majordomo.hms.personmgr.model.plan.PlanCost;
import ru.majordomo.hms.personmgr.model.plan.ServiceCost;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.model.token.Token;
import ru.majordomo.hms.personmgr.repository.AuthIpRedisRepository;
import ru.majordomo.hms.personmgr.repository.NotificationRepository;
import ru.majordomo.hms.personmgr.service.*;
import ru.majordomo.hms.personmgr.service.PlanChange.Factory;
import ru.majordomo.hms.personmgr.service.PlanChange.Processor;
import ru.majordomo.hms.personmgr.validation.ObjectId;
import ru.majordomo.hms.rc.user.resources.Domain;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.PhoneNumberManager.phoneValid;
import static ru.majordomo.hms.personmgr.common.RequiredField.*;
import static ru.majordomo.hms.personmgr.common.Utils.getClientIP;

@RestController
@Validated
@AllArgsConstructor
public class PersonalAccountRestController extends CommonRestController {
    private final PlanManager planManager;
    private final AccountOwnerManager accountOwnerManager;
    private final RcUserFeignClient rcUserFeignClient;
    private final ApplicationEventPublisher publisher;
    private final AccountHelper accountHelper;
    private final TokenManager tokenManager;
    private final NotificationRepository notificationRepository;
    private final AccountServiceHelper accountServiceHelper;
    private final Factory planChangeFactory;
    private final AccountNotificationHelper accountNotificationHelper;
    private final SiFeignClient siFeignClient;
    private final StatFeignClient statFeignClient;
    private final PaymentLinkHelper paymentLinkHelper;
    private final ServiceAbonementService serviceAbonementService;
    private final AbonementService abonementService;
    private final AuthIpRedisRepository authIpRedisRepository;

    @GetMapping("/accounts")
    public ResponseEntity<Page<PersonalAccount>> getAccounts(
            @RequestParam("accountId") String accountId,
            Pageable pageable
    ) {
        Page<PersonalAccount> accounts = accountManager.findByAccountIdContaining(accountId, pageable);

        if (accounts == null || !accounts.hasContent()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }

    @GetMapping("/accounts/filter")
    public ResponseEntity<Page<PersonalAccount>> filterAccounts(
            @QuerydslPredicate(root = PersonalAccount.class) Predicate predicate,
            Pageable pageable
    ) {
        Page<PersonalAccount> accounts = accountManager.findByPredicate(predicate, pageable);

        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }

    @GetMapping("/{accountId}/account")
    public ResponseEntity<PersonalAccount> getAccount(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    @GetMapping("/{accountId}/plan")
    public ResponseEntity<Plan> getAccountPlan(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Plan plan = planManager.findOne(account.getPlanId());

        if (plan == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        plan.getAbonements().forEach(item-> {
            BigDecimal discountCost = accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), item.getService());
            item.getService().setDiscountCost(discountCost);
        });

        return new ResponseEntity<>(plan, HttpStatus.OK);
    }

    @GetMapping("/{accountId}/discount-plan-cost")
    public ResponseEntity<PlanCost> getDiscountPlanCost(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        return new ResponseEntity<>(new PlanCost(accountServiceHelper.getPlanCostDependingOnDiscount(account)), HttpStatus.OK);
    }

    @GetMapping("/{accountId}/discount-abonements-cost")
    public ResponseEntity<List<ServiceCost>> getDiscountAbonementsCost(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Plan plan = planManager.findOne(account.getPlanId());

        List<ServiceCost> serviceCosts = new ArrayList<>();

        plan.getAbonements().forEach(item-> {
            BigDecimal cost = accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), item.getService());
            serviceCosts.add(new ServiceCost(item.getServiceId(), cost));
        });

        return new ResponseEntity<>(serviceCosts, HttpStatus.OK);
    }

    @PostMapping("/{accountId}/plan/{planId}")
    public ResponseEntity<Object> changeAccountPlan(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(Plan.class) @PathVariable(value = "planId") String planId,
            @RequestBody PlanChangeAgreement planChangeAgreement,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);
        String operator = request.getUserPrincipal().getName();
        Plan newPlan = planManager.findOne(planId);

        Processor planChangeProcessor = planChangeFactory.createPlanChangeProcessor(account, newPlan);
        planChangeProcessor.setOperator(operator);
        planChangeProcessor.setRequestPlanChangeAgreement(planChangeAgreement);

        planChangeProcessor.process();

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasAuthority('FORCE_PLAN_CHANGE')")
    @PostMapping("/{accountId}/force-plan/{planId}")
    //?????? ???????????????????? ?????????????? ???? ???????????????? ???????????????????????? ?????? ?????????? ???????????????? - ???????????????? ???????????????????? ?? ????????????
    public ResponseEntity<Object> forceChangeAccountPlan(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "planId") String planId,
            @RequestBody PlanChangeAgreement planChangeAgreement,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);
        String operator = request.getUserPrincipal().getName();
        Plan newPlan = planManager.findOne(planId);

        Processor planChangeProcessor = planChangeFactory.createPlanChangeProcessor(account, newPlan);
        planChangeProcessor.setOperator(operator);
        planChangeProcessor.setRequestPlanChangeAgreement(planChangeAgreement);
        planChangeProcessor.setIgnoreRestricts(true);

        planChangeProcessor.process();

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/{accountId}/plan-check/{planId}")
    public ResponseEntity<PlanChangeAgreement> accountPlanCheck(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "planId") String planId
    ) {
        return planCheck(accountId, planId, false);
    }

    @PreAuthorize("hasAuthority('FORCE_PLAN_CHANGE')")
    @PostMapping("/{accountId}/force-plan-check/{planId}")
    //?????? ???????????????????? ?????????????? ???? ???????????????? ???????????????????????? ?????? ?????????? ???????????????? - ???????????????? ???????????????????? ?? ????????????
    public ResponseEntity<PlanChangeAgreement> forceAccountPlanCheck(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "planId") String planId
    ) {
        return planCheck(accountId, planId, true);
    }

    private ResponseEntity<PlanChangeAgreement> planCheck(String accountId, String planId, Boolean ignoreRestricts) {
        PersonalAccount account = accountManager.findOne(accountId);
        Plan newPlan = planManager.findOne(planId);

        if (newPlan == null) {
            throw new ResourceNotFoundException("???? ???????????? ???????????????? ???????? ?? ??????????????????????????????: " + planId);
        }
        
        Processor planChangeProcessor = planChangeFactory.createPlanChangeProcessor(account, newPlan);
        planChangeProcessor.setIgnoreRestricts(ignoreRestricts);
        PlanChangeAgreement planChangeAgreement = planChangeProcessor.getPlanChangeAgreement();

        if (!planChangeAgreement.getErrors().isEmpty()) {
            return new ResponseEntity<>(planChangeAgreement, HttpStatus.FORBIDDEN);
            //???????????? ???????????????? ?? planChangeAgreement.getErrors()
        } else if (planChangeAgreement.getNeedToFeelBalance().compareTo(BigDecimal.ZERO) != 0) {
            return new ResponseEntity<>(planChangeAgreement, HttpStatus.ACCEPTED); // 202 Accepted
        } else {
            return new ResponseEntity<>(planChangeAgreement, HttpStatus.OK);
        }
    }

    @GetMapping("/change-email")
    public ResponseEntity<Object> confirmEmailsChange(
            @RequestParam("token") String tokenId,
            HttpServletRequest request,
            @RequestHeader HttpHeaders httpHeaders
    ) {
        logger.debug("confirmChangeOwnerEmail httpHeaders: " + httpHeaders.toString());

        Token token = tokenManager.getToken(tokenId, TokenType.CHANGE_OWNER_EMAILS);

        if (token == null) {
            throw new ParameterValidationException("???????????? ???? ?????????????????? ???????????????????? e-mail ?????????????? ???? ???????????? ?????? ?????? ???????????????? ??????????.");
        }

        PersonalAccount account = accountManager.findOne(token.getPersonalAccountId());

        if (account == null) {
            throw new ParameterValidationException("?????????????? ???? ????????????.");
        }

        List<String> newEmails = (List<String>) token.getParam("newemails");
        AccountOwner accountOwner = accountOwnerManager.findOneByPersonalAccountId(account.getId());
        ContactInfo contactInfo = accountOwner.getContactInfo();

        String oldEmails = contactInfo.getEmailAddresses().toString();

        contactInfo.setEmailAddresses(newEmails);
        accountOwner.setContactInfo(contactInfo);
        accountOwnerManager.save(accountOwner);

        String ip = getClientIP(request);
        history.saveForOperatorService(account,
                "?? IP " + ip + "???????????????????????? ?????????????????? ???????????????????? ?????????????? ?? "
                        + oldEmails
                        + " ???? " + newEmails.toString()
        );

        publisher.publishEvent(new TokenDeleteEvent(token.getId()));

        SimpleServiceMessage message = createSuccessResponse("Email-???????????? ?????????????????? ???????????????? ?????????????? ????????????????. ");

        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    @PostMapping("/{accountId}/password")
    public ResponseEntity<Object> changePassword(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Utils.checkRequiredParams(requestBody, ACCOUNT_PASSWORD_CHANGE);

        String password = (String) requestBody.get(PASSWORD_KEY);

        accountHelper.changePassword(account, password);

        String ip = getClientIP(request);

        Map<String, String> params = new HashMap<>();
        params.put(IP_KEY, ip);

        publisher.publishEvent(new AccountPasswordChangedEvent(account, params));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/password-recovery")
    public ResponseEntity<Object> requestPasswordRecovery(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request,
            @RequestHeader HttpHeaders httpHeaders
    ) {
        logger.debug("confirmPasswordRecovery httpHeaders: " + httpHeaders.toString());

        Utils.checkRequiredParams(requestBody, ACCOUNT_PASSWORD_RECOVER);

        String accountId = (String) requestBody.get(ACCOUNT_ID_KEY);

        PersonalAccount account = null;

        Pattern p = Pattern.compile("(?ui)(^ac_|^????_)(\\d*)");
        Matcher m = p.matcher(accountId);

        if (m.matches() && m.groupCount() == 2) {
            accountId = m.group(2);

            if (accountId.length() > 0) {
                account = accountManager.findByAccountId(accountId);
            }
        } else {
            String domainName = accountId.toLowerCase();
            Domain domain = null;
            try {
                domain = rcUserFeignClient.findDomain(domainName);
            } catch (Exception ignore) {}

            if (domain == null) {
                throw new ResourceNotFoundException("?????????? ???? ????????????.");
            }
            accountId = domain.getAccountId();
            account = accountManager.findOne(accountId);
        }

        if (account == null || account.getDeleted() != null) {
            throw new ResourceNotFoundException("??????????????/?????????? ???? ????????????.");
        }

        String ip = getClientIP(request);

        Map<String, String> params = new HashMap<>();
        params.put(IP_KEY, ip);

        publisher.publishEvent(new AccountPasswordRecoverEvent(account, params));

        return new ResponseEntity<>(createSuccessResponse("???????????????????? ???????????? ???? ???????????????????????????? ????????????."), HttpStatus.OK);
    }

    @GetMapping(PAYMENT_REDIRECT_PATH)
    public ResponseEntity paymentRedirect(
            @RequestParam Map<String, String> params
    ) {
        String link = paymentLinkHelper.getPaymentLink(params.get("token"));

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Location", link);

        logger.info("from {} with token {} redirect to {}", PAYMENT_REDIRECT_PATH, params.get("token"), link);

        return new ResponseEntity(headers, HttpStatus.TEMPORARY_REDIRECT);
    }

    @GetMapping("/password-recovery")
    public ResponseEntity<Object> confirmPasswordRecovery(
            @RequestParam("token") String tokenId,
            HttpServletRequest request,
            @RequestHeader HttpHeaders httpHeaders
    ) {
        logger.debug("confirmPasswordRecovery httpHeaders: " + httpHeaders.toString());

        Token token = tokenManager.getToken(tokenId, TokenType.PASSWORD_RECOVERY_REQUEST);

        if (token == null) {
            throw new ParameterValidationException("???????????? ???? ???????????????????????????? ???????????? ???? ???????????? ?????? ?????? ???????????????? ??????????.");
        }

        PersonalAccount account = accountManager.findOne(token.getPersonalAccountId());

        if (account == null) {
            throw new ParameterValidationException("?????????????? ???? ????????????.");
        }

        String ip = getClientIP(request);

        String password = randomAlphabetic(8);

        accountHelper.changePassword(account, password);

        Map<String, String> params = new HashMap<>();
        params.put(PASSWORD_KEY, password);
        params.put(IP_KEY, ip);

        publisher.publishEvent(new AccountPasswordRecoverConfirmedEvent(account, params));

        publisher.publishEvent(new TokenDeleteEvent(token.getId()));

        SimpleServiceMessage message = createSuccessResponse("???????????? ?????????????? ????????????????????????. " +
                "?????????? ???????????? ?????????????????? ???? ???????????????????? e-mail ?????????????????? ????????????????.");
        message.addParam("password", password);
        message.addParam("login", account.getName());

        return new ResponseEntity<>(
                message,
                HttpStatus.OK
        );
    }

    @GetMapping({"/{accountId}/google-adwords-promocode", "/{accountId}/ga-promocode"})
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

    @PostMapping({"/{accountId}/google-adwords-promocode", "/{accountId}/ga-promocode"})
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
            history.save(account, "???????????????????????? ?????????? ???????????????? google adwords (" + code + ")", request);
        }

        return new ResponseEntity<>(
                message,
                HttpStatus.OK
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FIN')")
    @PatchMapping("/{accountId}/account/credit-activation-date")
    public ResponseEntity<Object> setCreditActivationDate(
            @PathVariable(value = "accountId") String accountId,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam("date") LocalDate date,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        if (account == null) { return ResponseEntity.notFound().build();}

        LocalDateTime newCreditActivationDate = LocalDateTime.of(date, LocalTime.of(3, 0, 0));
        logger.info(newCreditActivationDate.toString());
        accountManager.setCreditActivationDate(accountId, newCreditActivationDate);

        String operator = request.getUserPrincipal().getName();
        history.save(
                accountId,
                "???????? ?????????????????? ?????????????? ???????????????? ?? " + account.getCreditActivationDate() + " ???? " + newCreditActivationDate,
                operator
        );

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{accountId}/account/settings")
    public ResponseEntity<Object> setSettings(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<AccountSetting, Object> requestBody,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        if (requestBody.get(AccountSetting.CREDIT) != null) {
            Boolean credit = (Boolean)requestBody.get(AccountSetting.CREDIT);
            if (!credit) {
                // ???????????????????? ??????????????
                if (account.isCredit() && account.getCreditActivationDate() != null) {
                    // ???????????? ?????? ?????????????????????? (???????????? ???????????? ????????????????)
                    throw new ParameterValidationException("Credit already activated. Credit disabling prohibited.");
                }
            } else {
                throw new ParameterValidationException("???????????? '?????????????? ?? ????????????' ????????????????????");
//                List<AccountAbonement> abonements = accountAbonementManager.findAllByPersonalAccountId(account.getId());
//
//                if (!abonements.isEmpty() && abonements.stream().anyMatch(a -> a.getAbonement().isTrial())) {
//                    throw new ParameterValidationException("?????????????????? ?????????????? ???????????????????? ???? ???????????????? ??????????????");
//                }
//
//                if (planManager.findOne(account.getPlanId()).isAbonementOnly()) {
//                    throw new ParameterValidationException("?????????????????? ?????????????? ???????????????????? ???? ?????????? ???????????????? ??????????");
//                }
//                // ?????????????????? ??????????????
//                if (!account.isCredit() && !account.isActive()) {
//                    accountHelper.enableAccount(account);
//                }
            }
            accountManager.setCredit(accountId, credit);

            history.save(account, (credit ? "??????????????" : "????????????????") + " ????????????", request);
        }

        if (requestBody.get(AccountSetting.ADD_QUOTA_IF_OVERQUOTED) != null) {
            Boolean addQuotaIfOverquoted = (Boolean) requestBody.get(AccountSetting.ADD_QUOTA_IF_OVERQUOTED);

            //???????? ???????????????????????? ???????????????? ???????????????? ???????????? ???? ???????????????? ?? ?????? ??????????????????????, ???? ???????????????????? ????????????,
            // ??????????????????, ?????? ?????????????? ?????????????? ???????? ???? ???? 1 ????????
            //?????????? ???????????????? ?????? #13066, ?????????? ?????? ???????????? ?????????????????? ???????????? ?????????????????????? ????-???? ???????????????? ??????????????, ?? ??????????
            // ???????????????????????? ???? ?????????? ?????????? ???????????????? ??, ?????????? ??????????????, ???????????????? ???????????????? ????????????
            if (addQuotaIfOverquoted && account.getPotentialQuotaCount() > 0) {
                PaymentService quotaPaymentService = paymentServiceRepository.findByOldId(ADDITIONAL_QUOTA_100_SERVICE_ID);
                BigDecimal additionalQuotaCost = accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), quotaPaymentService)
                        .multiply(BigDecimal.valueOf(account.getPotentialQuotaCount()));
                accountHelper.checkBalance(account, accountServiceHelper.getDailyCostForService(quotaPaymentService, additionalQuotaCost));
            }

            accountManager.setAddQuotaIfOverquoted(accountId, addQuotaIfOverquoted);

            //?????????????????? ?????????? ??????????, ???????????????? ???????????? ?? ????.
            account.setAddQuotaIfOverquoted(addQuotaIfOverquoted);
            publisher.publishEvent(new AccountCheckQuotaEvent(account.getId()));

            history.save(account, (addQuotaIfOverquoted ? "????????????????" : "??????????????????") + " ???????????????????? ?????????? ?????? ???????????????????? ?????????????????? ???? ????????????", request);

            String quotaServiceId = paymentServiceRepository.findByOldId(ADDITIONAL_QUOTA_100_SERVICE_ID).getId();
            publisher.publishEvent(new UserDisabledServiceEvent(account.getId(), quotaServiceId));
        }

        if (requestBody.get(AccountSetting.AUTO_BILL_SENDING) != null) {
            Boolean autoBillSending = (Boolean) requestBody.get(AccountSetting.AUTO_BILL_SENDING);
            accountManager.setAutoBillSending(accountId, autoBillSending);

            history.save(account, (autoBillSending ? "????????????????" : "??????????????????") + " ???????????????????????????? ???????????????? ?????????????????????????? ????????????????????", request);
        }

        if (requestBody.get(AccountSetting.NOTIFY_DAYS) != null) {
            Integer notifyDays = (Integer) requestBody.get(AccountSetting.NOTIFY_DAYS);

            Set<Integer> notifyDaysVariants = ImmutableSet.of(7, 14, 30);
            if (!notifyDaysVariants.contains(notifyDays)) {
                throw new ParameterValidationException("???????????????????? ???????? ???????????????? ?????????????????????? ???????????????? ???????????? ???? " + notifyDaysVariants + " ????????");
            }

            accountManager.setNotifyDays(accountId, notifyDays);

            history.save(account, "???????????????????? ???????? ???????????????? ?????????????????????? ???? ?????????????????? ?????????????????????? ?????????????? ???????????????? ???? '" + notifyDays + "' ????????", request);
        }

        if (requestBody.get(AccountSetting.SMS_PHONE_NUMBER) != null) {
            String smsPhoneNumber = (String)requestBody.get(AccountSetting.SMS_PHONE_NUMBER);

            if (smsPhoneNumber.equals("") && accountServiceHelper.hasSmsNotifications(account)) {
                throw new ParameterValidationException("SMSPhoneNumber can't be empty with active sms notifications.");
            }

            if (!phoneValid(smsPhoneNumber) && !smsPhoneNumber.equals("")) {
                throw new ParameterValidationException("SMSPhoneNumber is not valid.");
            }

            accountManager.setSmsPhoneNumber(accountId, smsPhoneNumber);

            history.save(account, "???????????????????? ?????????????? ?????? ??????-?????????????????????? ???? '" + smsPhoneNumber + "'", request);
        }

        if (requestBody.get(AccountSetting.ABONEMENT_AUTO_RENEW) != null) {
            Boolean autoRenew = (Boolean) requestBody.get(AccountSetting.ABONEMENT_AUTO_RENEW);

            accountManager.setSettingByName(accountId, AccountSetting.ABONEMENT_AUTO_RENEW, autoRenew);

            history.save(account, (autoRenew ? "????????????????" : "??????????????????") + " ?????????????????????????? ???????????????????? ???? ??????????????", request);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/{accountId}/account/notifications")
    public ResponseEntity<Object> setNotifications(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Set<MailManagerMessageType> notifications,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Set<MailManagerMessageType> oldNotifications = account.getNotifications();

        List<MailManagerMessageType> activeNotifications = accountNotificationHelper.getActiveMailManagerMessageTypes();

        Set<MailManagerMessageType> filteredNotifications = notifications.stream().filter(activeNotifications::contains).collect(Collectors.toSet());

        accountManager.setNotifications(accountId, filteredNotifications);

        history.save(account, "?????????????? ???????????? ?????????????????????? ???????????????? c [" + oldNotifications + "] ???? [" + filteredNotifications + "]", request);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/{accountId}/account/properties")
    public ResponseEntity<AccountProperties> setProperties(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody AccountProperties accountProperties,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        if (accountProperties.getAngryClient() != null) {
            accountManager.setAngryClient(accountId, accountProperties.getAngryClient());

            history.save(account, (accountProperties.getAngryClient() ? "????????????????" : "??????????????????")
                    + " ?????????????? ?? ?????? ?????? ???????????? '??????????????????????'", request);
        }

        if (accountProperties.getShowScamWarningDisabled() != null) {
            accountManager.setScamWarning(accountId, accountProperties.getShowScamWarningDisabled());
        }
        if (accountProperties.getAppHostingMessageDisabled() != null) {
            accountManager.setAppHostingMessageDisabled(accountId, accountProperties.getAppHostingMessageDisabled());
        }

        account = accountManager.findOne(accountId);
        return ResponseEntity.ok(account.getProperties());
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('FIN')")
    @PatchMapping("/{accountId}/account/sbis")
    public ResponseEntity<AccountProperties> setSbis(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody AccountProperties accountProperties,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        if (accountProperties.getSbis() != null) {
            accountManager.setSbis(accountId, accountProperties.getSbis());

            history.save(account, (accountProperties.getSbis() ? "????????????????" : "??????????????????")
                    + " ???????????????? ?????????? ?? SBIS", request);
        }

        account = accountManager.findOne(accountId);
        return ResponseEntity.ok(account.getProperties());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/accounts/sbis")
    public ResponseEntity<List<String>> getSbisAccounts() {
        return ResponseEntity.ok(accountManager.findAccountIdsForSbis());
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('FIN')")
    @GetMapping(value = "/sbis-list")
    public ResponseEntity<Page<PersonalAccount>> getAllOrders(
            Pageable pageable,
            @RequestParam Map<String, String> search
    ) {
        String accId = getAccountIdFromNameOrAccountId(search.getOrDefault("personalAccountId", ""));

        Page<PersonalAccount> orders;

        if (!accId.isEmpty()) {
            orders = accountManager.findAccountsForSbis(accId, pageable);
        } else {
            orders = accountManager.findAccountsForSbis(pageable);
        }

        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @PatchMapping("/{accountId}/account/notifications/sms")
    public ResponseEntity<Object> setSmsNotifications(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Set<MailManagerMessageType> notifications,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        if (notifications.isEmpty() && accountServiceHelper.hasSmsNotifications(account)) {
            throw new ParameterValidationException("?????? ???????????????? SMS-?????????????????????? ???????????????????? ?????????????? ???????? ???? ???????? ?????? ??????????????????????.");
        }

        setNotifications(account, notifications, "SMS_", request);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/{accountId}/account/notifications/email-news")
    public ResponseEntity<Object> addNotification(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, Object> requestBody,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        Utils.checkRequiredParams(requestBody, ACCOUNT_EMAIL_NEWS_PATCH);

        boolean newState;

        try {
            newState = (boolean) requestBody.get("enabled");
        } catch (ClassCastException e) {
            throw new ParameterValidationException("???????????????????????? ???????????????? 'enabled'. ???????????????? ???????????? ???????? 'true' ?????? 'false'");
        }

        setNotification(accountId, MailManagerMessageType.EMAIL_NEWS, newState, request);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/is_subscribed_account")
    public ResponseEntity<Map<String, Boolean>> isSubscribed(
            @ObjectId(
                    value = PersonalAccount.class,
                    idFieldName = "accountId"
            )
            @RequestParam(value = "accountId") String accountId
    ) {
        PersonalAccountWithNotificationsProjection account = accountManager.findOneByAccountIdWithNotifications(accountId);

        Map<String, Boolean> isSubscribedResult = new HashMap<>();
        isSubscribedResult.put("is_subscribed", account.getDeleted() == null && account.hasNotification(MailManagerMessageType.EMAIL_NEWS));

        return new ResponseEntity<>(isSubscribedResult, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('UNSUBSCRIBE_EMAIL_NEWS')")
    @PostMapping("/unsubscribe/{accountId}")
    public Map<String, Object> unsubscribe(
            @PathVariable("accountId") String accountId,
            @RequestBody Map<String, String> params
    ) {
        if (params == null) {
            params = Collections.emptyMap();
        }
        PersonalAccountWithNotificationsProjection account = accountManager.findOneByAccountIdWithNotifications(accountId);

        if (account == null) {
            logger.info("account with accountId " + accountId + " not found, unsubscribe failed");
            Map<String, Object> result = new HashMap<>();
            result.put("message", "client with accountId " + accountId + " not found");
            result.put("success", false);
            return result;
        }

        Set<MailManagerMessageType> notifications = account
                .getNotifications()
                .stream()
                .filter(n -> !n.equals(MailManagerMessageType.EMAIL_NEWS))
                .collect(Collectors.toSet());

        accountManager.setNotifications(account.getId(), notifications);

        try {
            Map<String, String> statMessage = prepareUnsubscribeStatMessage(accountId, params);
            statFeignClient.saveUnsubscribeStat(statMessage);
        } catch (Exception e) {
            logger.error("Can't save unsubscribe statistics, class: {}, message: {}", e.getClass().getName(), e.getMessage());
        }

        return Collections.singletonMap("success", true);
    }

    private Map<String, String> prepareUnsubscribeStatMessage(String accountId, Map<String, String> params) {
        Map<String, String> message = new HashMap<>();
        message.put("account", accountId);
        message.put("email", params.get("email"));
        message.put("service", "HMS");
        message.put("cause", params.get("cause"));
        message.put("causeText", params.getOrDefault("causeText", null));
        return message;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @PostMapping("/{accountId}/account/toggle_state")
    public ResponseEntity<SimpleServiceMessage> toggleAccount(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        ProcessingBusinessOperation operation = accountHelper.switchAccountActiveState(account, !account.isActive());

        history.save(account, "?????????????? " + (!account.isActive() ? "??????????????" : "????????????????"), request);
        if (operation != null) {
            return ResponseEntity.accepted().body(createBusinessOperationResponse(operation));
        } else {
            //todo return body
            return ResponseEntity.noContent().build();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @PostMapping("/{accountId}/account/toggle_freeze")
    public ResponseEntity<Object> toggleAccountFreeze(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Boolean switchState = !account.isFreeze();

        accountServiceHelper.switchServicesAfterFreeze(account, switchState);
        serviceAbonementService.switchServiceAbonementsAfterFreeze(account, switchState);
        abonementService.switchAbonementsAfterFreeze(account, switchState);
        ProcessingBusinessOperation operation = accountHelper.switchAccountFreezeState(account, switchState);

        history.save(account, "?????????????? " + (!account.isFreeze() ? "??????????????????" : "????????????????????"), request);

        if (operation != null) {
            return ResponseEntity.accepted().body(createBusinessOperationResponse(operation));
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasAuthority('MANAGE_ACCOUNT_DELETED')")
    @PostMapping("/{accountId}/account/toggle_deleted")
    public ResponseEntity<Object> toggleDeleted(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        boolean delete = account.getDeleted() == null;

        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(DELETE_KEY, Boolean.toString(delete));

        SimpleServiceMessage siResponse = siFeignClient.toggleDelete(account.getId(), requestParams);

        if (siResponse.getParam("success") == null || !((boolean) siResponse.getParam("success"))) {
            throw new ParameterValidationException("???? ?????????????? ?????????????? ?????????? ?????? ?????????? ?? ????. ????????????: " + siResponse.getParam(ERROR_MESSAGE_KEY));
        }

        ProcessingBusinessOperation operation = null;
        if (delete) {
            operation = accountHelper.disableAccount(account);
        }

        accountManager.setDeleted(accountId, delete);

        history.save(account, "?????????????? " + (delete ? "????????????" : "??????????????????"), request);

        if (operation != null) {
            return ResponseEntity.accepted().body(createBusinessOperationResponse(operation));
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private void setNotifications(PersonalAccount account, Set<MailManagerMessageType> notifications, String pattern, SecurityContextHolderAwareRequestWrapper request) {

        Set<MailManagerMessageType> oldNotifications = account.getNotifications();

        //???????????????? ???????????? ???????????? ???????????????????? ??????????????????????
        notifications = notifications.stream()
                .filter(mailManagerMessageType -> mailManagerMessageType.name().startsWith(pattern))
                .collect(Collectors.toSet());

        String newNotificationsAsString = notifications.toString();
        String oldNotificationsAsString = oldNotifications.stream()
                .filter(mailManagerMessageType -> mailManagerMessageType.name().startsWith(pattern))
                .toString();

        //?? ???????????? ?????????????????????? ?????????????????? ?????? ?????????????? ???????????? ??????????????????????
        notifications.addAll(
                oldNotifications.stream()
                        .filter(mailManagerMessageType -> !mailManagerMessageType.name().startsWith(pattern))
                        .collect(Collectors.toSet()
                        )
        );

        accountManager.setNotifications(account.getId(), notifications);
        String message = "?????????????????????? [" + pattern + "] ???????????????? c [" + oldNotificationsAsString + "] ???? [" + newNotificationsAsString + "]";
        history.save(account, message, request);
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
            history.save(account, notification.getName() + (state ? " ????????????????." : " ??????????????????."), request);
        }
    }

    @GetMapping(value = "/{accountId}/account/notifications/new-auth-notify")
    public ResponseEntity<Map<String, Boolean>> getAuthNotification(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        AuthIPRedis authIPRedis = null;

        try {
            authIPRedis = authIpRedisRepository.findById(accountId).orElse(null);
        } catch (Exception ignored) { }

        return new ResponseEntity<>(Collections.singletonMap("new-auth-notify", authIPRedis.isNotify()), HttpStatus.OK);
    }

    @PostMapping("/{accountId}/account/notifications/new-auth-notify")
    public ResponseEntity<Object> addAuthNotification(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, Object> requestBody,
            SecurityContextHolderAwareRequestWrapper request
    ) {

        Utils.checkRequiredParams(requestBody, ACCOUNT_EMAIL_NEW_AUTH_NOTIFY_PATCH);
        boolean newState;

        try {
            newState = (boolean) requestBody.get("new-auth-notify");
        } catch (ClassCastException e) {
            throw new ParameterValidationException("???????????????????????? ???????????????? 'enabled'. ???????????????? ???????????? ???????? 'true' ?????? 'false'");
        }

        setNewAuthNotification(accountId, newState, request);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private void setNewAuthNotification(String accountId, boolean state, SecurityContextHolderAwareRequestWrapper request) {

        PersonalAccount account = accountManager.findOne(accountId);
        AuthIPRedis authIPRedis = null;

        try {
            authIPRedis = authIpRedisRepository.findById(accountId).orElse(null);
        } catch (Exception ignored) { }

        if (authIPRedis != null && authIPRedis.isNotify() != state) {
            authIPRedis.setNotify(state);
            authIpRedisRepository.save(authIPRedis);
            history.save(account, "?????????????????????? ???? ?????????????????????? ?? ?????????? IP-??????????????" + (state ? " ????????????????." : " ??????????????????."), request);
        }
    }
}