package ru.majordomo.hms.personmgr.controller.rest.resource;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.config.NsCheckConfig;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.dto.request.RedirectServiceBuyRequest;
import ru.majordomo.hms.personmgr.event.account.RedirectWasProlongEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.ServicePlan;
import ru.majordomo.hms.personmgr.model.service.RedirectAccountService;
import ru.majordomo.hms.personmgr.repository.AccountRedirectServiceRepository;
import ru.majordomo.hms.personmgr.repository.ServicePlanRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.NsCheckService;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.service.ServiceAbonementService;
import ru.majordomo.hms.personmgr.validation.ObjectId;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Redirect;
import ru.majordomo.hms.rc.user.resources.WebSite;

import static ru.majordomo.hms.personmgr.common.FieldRoles.REDIRECT_PATCH;
import static ru.majordomo.hms.personmgr.common.FieldRoles.REDIRECT_POST;

@RestController
@Validated
@RequiredArgsConstructor
public class RedirectServiceRestController extends CommonRestController {
    private static final TemporalAdjuster PLUS_ONE_YEAR = TemporalAdjusters.ofDateAdjuster(date -> date.plusYears(1));
    private static final int LIMIT_REDIRECT_FOR_DOMAIN = 10;
    private static final int MAX_YEARS_PROLONG = 3;

    private final RcUserFeignClient rcUserFeignClient;
    private final AccountRedirectServiceRepository accountRedirectServiceRepository;
    private final NsCheckService nsCheckService;
    private final ServicePlanRepository servicePlanRepository;
    private final ServiceAbonementService serviceAbonementService;
    private final AccountHelper accountHelper;
    private final NsCheckConfig nsCheckConfig;

    @GetMapping("/{accountId}/account-service-redirect")
    public List<RedirectAccountService> getServices(
            @PathVariable @ObjectId(PersonalAccount.class) String accountId
    ) {
        return accountRedirectServiceRepository.findByPersonalAccountId(accountId);
    }

    @GetMapping("/{accountId}/account-service-redirect/{serviceId}")
    public RedirectAccountService getService(
            @PathVariable @ObjectId(PersonalAccount.class) String accountId,
            @PathVariable @ObjectId(RedirectAccountService.class) String serviceId
    ) {
        return accountRedirectServiceRepository.findByPersonalAccountIdAndId(accountId, serviceId);
    }

    @PostMapping("/{accountId}/redirect/{serviceId}/prolong")
    public ResponseEntity prolong(
            @PathVariable @ObjectId(PersonalAccount.class) String accountId,
            @PathVariable @ObjectId(RedirectAccountService.class) String serviceId,
            SecurityContextHolderAwareRequestWrapper request
    ){
        PersonalAccount account = accountManager.findOne(accountId);
        assertAccountIsActive(account);
        assertAccountIsFreeze(account);

        RedirectAccountService redirectAccountService = accountRedirectServiceRepository
                .findByPersonalAccountIdAndId(account.getId(), serviceId);

        Domain domain;
        try {
            domain = rcUserFeignClient.findDomain(redirectAccountService.getFullDomainName());
            if (domain == null || !domain.getAccountId().equals(accountId)) {
                throw new ParameterValidationException(
                        "?????????? ?? ???????????? " + redirectAccountService.getFullDomainName() + " ???? ???????????? ???? ????????????????");
            }
        } catch (ResourceNotFoundException e) {
            throw new ParameterValidationException("?????????? ?? ???????????? " + redirectAccountService.getFullDomainName() + " ???? ???????????? ???? ????????????????");
        }

        if (!nsCheckService.checkOurNs(domain)) {
            throw new ParameterValidationException(String.format(
                    "?????????? ???????????? ???????? ?????????????????????? ???? ???????? DNS-?????????????? (%s)",
                    String.join(", ", nsCheckConfig.getAllowedNsList())
            ));
        }

        if (redirectAccountService.getAccountServiceAbonement() == null) {

            ServicePlan servicePlan = servicePlanRepository.findOneByFeatureAndActive(Feature.REDIRECT, true);

            AccountServiceAbonement accountServiceAbonement = serviceAbonementService.addAbonement(
                    account, servicePlan.getNotInternalAbonementId(), Feature.REDIRECT, true);

            redirectAccountService.setAccountServiceAbonementId(accountServiceAbonement.getId());
            redirectAccountService.setAccountServiceAbonement(accountServiceAbonement);
            redirectAccountService.setExpireDate(LocalDate.now().with(PLUS_ONE_YEAR));
            redirectAccountService.setActive(true);
            accountRedirectServiceRepository.save(redirectAccountService);
            history.save(account, "???????????????? ???????????????? ???????????? ?????????????????????????????? ?????? ????????????: " + domain.getName(), request);
        } else if (
                redirectAccountService.getAccountServiceAbonement().getExpired().toLocalDate()
                        .isBefore(
                                LocalDate.now().plusYears(MAX_YEARS_PROLONG)
                        )
                || redirectAccountService.getAccountServiceAbonement().getExpired().toLocalDate()
                        .isEqual(
                                LocalDate.now().plusYears(MAX_YEARS_PROLONG)
                        )
        ){

            serviceAbonementService.prolongAbonement(
                    account, redirectAccountService.getAccountServiceAbonement(), null);

            redirectAccountService.setExpireDate(redirectAccountService.getExpireDate().with(PLUS_ONE_YEAR));
            redirectAccountService.setActive(true);
            accountRedirectServiceRepository.save(redirectAccountService);
            history.save(account, "???????????????? ???????????? ?????????????????????????????? ?????? ????????????: " + domain.getName(), request);
        } else {
            throw new ParameterValidationException("?????????????????????????? ?????? ???????????? " + domain.getName() + " ?????? ???????????????? ???? "
                    + redirectAccountService.getAccountServiceAbonement().getExpired().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        }

        publisher.publishEvent(new RedirectWasProlongEvent(accountId, domain.getName()));

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/{accountId}/redirect/buy")
    public RedirectAccountService buy(
            @PathVariable @ObjectId(PersonalAccount.class) String accountId,
            @Valid @RequestBody RedirectServiceBuyRequest body,
            SecurityContextHolderAwareRequestWrapper request
    ){
        PersonalAccount account = accountManager.findOne(accountId);
        assertAccountIsActive(account);
        assertAccountIsFreeze(account);

        accountHelper.checkIsAdditionalServiceAllowed(account, Feature.REDIRECT);

        Domain domain = rcUserFeignClient.getDomain(accountId, body.getDomainId());

        if (!nsCheckService.checkOurNs(domain)) {
            throw new ParameterValidationException(String.format(
                    "?????????? ???????????? ???????? ?????????????????????? ???? ???????? DNS-?????????????? (%s)",
                    String.join(", ", nsCheckConfig.getAllowedNsList())
            ));
        }

        RedirectAccountService redirectAccountService = accountRedirectServiceRepository
                .findByPersonalAccountIdAndFullDomainName(account.getId(), domain.getName());

        if (redirectAccountService != null) {
            throw new ParameterValidationException("?????????? ???????????????? ???????????? ???????? ???????????? ?????????????????????????? ?????? ???????????? ????????????");
        }

        ServicePlan servicePlan = servicePlanRepository.findOneByFeatureAndActive(Feature.REDIRECT, true);

        AccountServiceAbonement abonement = serviceAbonementService.addAbonement(
                account, servicePlan.getNotInternalAbonementId(), Feature.REDIRECT, true);

        RedirectAccountService result = addRedirectService(account, domain, abonement);

        history.save(account, "???????????????? ???????????? ?????????????????????????????? ?????? ????????????: " + domain.getName(), request);

        publisher.publishEvent(new RedirectWasProlongEvent(accountId, domain.getName()));

        return result;
    }

    @PostMapping("/{accountId}/redirect")
    public ResponseEntity<SimpleServiceMessage> create(
            @PathVariable @ObjectId(PersonalAccount.class) String accountId,
            @RequestBody SimpleServiceMessage body,
            SecurityContextHolderAwareRequestWrapper request,
            Authentication authentication
    ) {
        if (request.isUserInRole("ADMIN") || request.isUserInRole("OPERATOR")) {
            checkParamsWithRoles(body.getParams(), REDIRECT_POST, authentication);
        } else {
            checkParamsWithRolesAndDeleteRestricted(body.getParams(), REDIRECT_POST, authentication);
        }

        PersonalAccount account = accountManager.findOne(accountId);
        assertAccountIsActive(account);
        assertAccountIsFreeze(account);

        accountHelper.checkIsAdditionalServiceAllowed(account, Feature.REDIRECT);

        Domain domain = rcUserFeignClient.getDomain(accountId, (String) body.getParam("domainId"));

        assertServiceIsPaid(accountId, domain.getName());

        assertDomainNotExistsOnWebsite(accountId, domain.getId());

        accountRedirectServiceRepository.findByPersonalAccountIdAndFullDomainName(accountId, domain.getName());

        checkRedirectLimits(account, body);

        String name = body.getParam("name") == null || body.getParam("name").toString().isEmpty() ? domain.getName() : body.getParam("name").toString();

        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(accountId);
        message.addParam("domainId", domain.getId());
        message.addParam("redirectItems", body.getParam("redirectItems"));
        message.addParam("name", name);

        ProcessingBusinessAction action = businessHelper.buildActionAndOperation(BusinessOperationType.REDIRECT_CREATE, BusinessActionType.REDIRECT_CREATE_RC, message);

        history.save(account, "?????????????????? ???????????? ???? ???????????????? ?????????????????????????????? ?????? ????????????: " + domain.getName(), request);

        return new ResponseEntity<>(createSuccessResponse(action), HttpStatus.ACCEPTED);
    }

    @PatchMapping("/{accountId}/redirect/{resourceId}")
    public ResponseEntity<SimpleServiceMessage> update(
            @PathVariable @ObjectId(PersonalAccount.class) String accountId,
            @PathVariable String resourceId,
            @RequestBody SimpleServiceMessage message,
            SecurityContextHolderAwareRequestWrapper request,
            Authentication authentication
    ) {
        if (request.isUserInRole("ADMIN") || request.isUserInRole("OPERATOR")) {
            checkParamsWithRoles(message.getParams(), REDIRECT_PATCH, authentication);
        } else {
            checkParamsWithRolesAndDeleteRestricted(message.getParams(), REDIRECT_PATCH, authentication);
        }

        PersonalAccount account = accountManager.findOne(accountId);

        assertAccountIsActive(account);
        assertAccountIsFreeze(account);

        checkRedirectLimits(account, message);

        Redirect redirect = rcUserFeignClient.getRedirect(accountId, resourceId);

        assertServiceIsPaid(accountId, redirect.getDomain().getName());

        message.setAccountId(accountId);
        message.addParam("resourceId", resourceId);


        ProcessingBusinessAction action = businessHelper.buildActionAndOperation(BusinessOperationType.REDIRECT_UPDATE, BusinessActionType.REDIRECT_UPDATE_RC, message);

        history.save(account, "?????????????????? ???????????? ???? ???????????????????? ?????????????????????????????? ?????? ????????????: " + redirect.getDomain().getName(), request);

        return new ResponseEntity<>(createSuccessResponse(action), HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/{accountId}/redirect/{resourceId}")
    public ResponseEntity<SimpleServiceMessage> delete(
            @PathVariable @ObjectId(PersonalAccount.class) String accountId,
            @PathVariable String resourceId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(accountId);
        message.addParam("resourceId", resourceId);
        ProcessingBusinessAction action = businessHelper.buildActionAndOperation(BusinessOperationType.REDIRECT_DELETE, BusinessActionType.REDIRECT_DELETE_RC, message);
        history.save(accountId, "?????????????????? ???????????? ???? ???????????????? ?????????????????????????????? ?? ID " + resourceId, request);
        return new ResponseEntity<>(createSuccessResponse(action), HttpStatus.ACCEPTED);
    }

    private RedirectAccountService addRedirectService(PersonalAccount account, Domain domain, AccountServiceAbonement abonement) {
        RedirectAccountService redirectAccountService = new RedirectAccountService();
        redirectAccountService.setFullDomainName(domain.getName());
        redirectAccountService.setPersonalAccountId(account.getId());
        redirectAccountService.setAutoRenew(true);
        redirectAccountService.setCreatedDate(LocalDate.now());
        redirectAccountService.setExpireDate(LocalDate.now().with(PLUS_ONE_YEAR));
        redirectAccountService.setAccountServiceAbonementId(abonement.getId());
        redirectAccountService.setAccountServiceAbonement(abonement);
        redirectAccountService.setServiceId(abonement.getAbonement().getServiceId());
        redirectAccountService.setActive(true);
        return accountRedirectServiceRepository.insert(redirectAccountService);
    }

    private void assertDomainNotExistsOnWebsite(String accountId, String domainId) {
        WebSite webSite = null;
        try {
            webSite = rcUserFeignClient.getWebSiteByDomainId(accountId, domainId);
        } catch (ResourceNotFoundException ignored) {} //this is normal

        if (webSite != null) {
            throw new ParameterValidationException("?????????? ?????? ???????????????????????? ???? ?????????? " + webSite.getName());
        }
    }

    private void checkRedirectLimits(PersonalAccount account, SimpleServiceMessage message) {
        List<Map> redirectItems = (List<Map>) message.getParam("redirectItems");
        if (redirectItems != null
                && (redirectItems).size() > LIMIT_REDIRECT_FOR_DOMAIN) {
            throw new ParameterValidationException("???????????????? ???? ?????????? 10 ?????????????????????????????? ?????? ???????????? ????????????");
        }
    }

    private void assertServiceIsPaid(String accountId, String domainName) {

        RedirectAccountService ras = accountRedirectServiceRepository.findByPersonalAccountIdAndFullDomainName(accountId, domainName);

        boolean serviceIsPaid = ras.getAccountServiceAbonement().getExpired().isAfter(LocalDateTime.now());

        if (!serviceIsPaid) {
            throw new ParameterValidationException("?????? ???????????????????? ???????????????????????????????? ?????? ???????????? " + domainName + " ???????????????? ????????????");
        }
    }

    private void assertAccountIsActive(PersonalAccount account) {
        if (!account.isActive()) {
            throw new ParameterValidationException("?????????????? ???? ??????????????");
        }
    }

    private void assertAccountIsFreeze(PersonalAccount account) {
        if (account.isFreeze()) {
            throw new ParameterValidationException("?????????????? ??????????????????");
        }
    }

}
