package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.request.RedirectServiceBuyRequest;
import ru.majordomo.hms.personmgr.exception.NotEnoughMoneyException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.model.service.RedirectAccountService;
import ru.majordomo.hms.personmgr.repository.AccountRedirectServiceRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.ChargeMessage;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.personmgr.validation.ObjectId;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Redirect;
import ru.majordomo.hms.rc.user.resources.RedirectItem;
import ru.majordomo.hms.rc.user.resources.WebSite;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Constants.REDIRECT_SERVICE_OLD_ID;
import static ru.majordomo.hms.personmgr.common.FieldRoles.REDIRECT_POST;
import static ru.majordomo.hms.personmgr.common.FieldRoles.REDIRECT_PATCH;

@RestController
@RequestMapping("/{accountId}/redirect")
@Validated
public class RedirectServiceRestController extends CommonRestController {

    private static final TemporalAdjuster PLUS_ONE_YEAR = TemporalAdjusters.ofDateAdjuster(date -> date.plusYears(1));
    private static final int LIMIT_REDIRECT_FOR_DOMAIN = 10;

    private AccountHelper accountHelper;
    private RcUserFeignClient rcUserFeignClient;
    private AccountRedirectServiceRepository accountRedirectServiceRepository;

    @Autowired
    public RedirectServiceRestController(
            AccountHelper accountHelper,
            RcUserFeignClient rcUserFeignClient,
            AccountRedirectServiceRepository accountRedirectServiceRepository
    ) {
        this.accountHelper = accountHelper;
        this.rcUserFeignClient = rcUserFeignClient;
        this.accountRedirectServiceRepository = accountRedirectServiceRepository;
    }

    @GetMapping("/service")
    public List<RedirectAccountService> getService(
            @PathVariable @ObjectId(PersonalAccount.class) String accountId
    ) {
        return accountRedirectServiceRepository.findByPersonalAccountId(accountId);
    }

    @PostMapping("/buy")
    public ResponseEntity buy(
            @PathVariable @ObjectId(PersonalAccount.class) String accountId,
            @Valid @RequestBody RedirectServiceBuyRequest body,
            SecurityContextHolderAwareRequestWrapper request
    ){
        PersonalAccount account = accountManager.findOne(accountId);
        if (!account.isActive()) {
            throw new ParameterValidationException("Заказ услуги на неактивном аккаунте невозможен");
        }

        Domain domain = rcUserFeignClient.getDomain(accountId, body.getDomainId());

        RedirectAccountService redirectAccountService = accountRedirectServiceRepository
                .findByPersonalAccountIdAndFullDomainName(account.getId(), domain.getName());

        if (redirectAccountService == null) {
            chargeByRedirect(account, domain);
            addRedirectService(account, domain);
        } else if (redirectAccountService.getExpireDate().isAfter(LocalDate.now())){
            chargeByRedirect(account, domain);
            redirectAccountService.setExpireDate(LocalDate.now().with(PLUS_ONE_YEAR));
            redirectAccountService.setActive(true);
            accountRedirectServiceRepository.save(redirectAccountService);
        } else {
            throw new ParameterValidationException("Переадресация для домен " + domain.getName() + " оплачена до "
                    + redirectAccountService.getExpireDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        }

        history.save(account, "Заказана услуга перенаправления для домена: " + domain.getName(), request);

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @PostMapping("")
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
        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт не активен");
        }

        List<RedirectItem> redirectItems = (List<RedirectItem>) body.getParam("redirectItems");
        if (redirectItems == null) {
            throw new ParameterValidationException("Список перенаправлений не может быть пуст");
        }

        if (redirectItems.size() > LIMIT_REDIRECT_FOR_DOMAIN) {
            throw new ParameterValidationException("Доступно не более 10 перенаправлений для одного домена");
        }

        Domain domain = rcUserFeignClient.getDomain(accountId, (String) body.getParam("domainId"));
        if (domain == null) {
            throw new ParameterValidationException("Домен не найден на аккаунте");
        }

        boolean serviceIsPaid = accountRedirectServiceRepository
                .existsByPersonalAccountIdAndFullDomainNameAndExpireDateAfter(account.getId(), domain.getName(), LocalDate.now());

        if (!serviceIsPaid) {
            throw new ParameterValidationException("Для создания перенаправления закажите услугу");
        }

        assertDomainNotExistsOnWebsite(accountId, domain.getId());

        accountRedirectServiceRepository.findByPersonalAccountIdAndFullDomainName(accountId, domain.getName());


        String name = body.getParam("name") == null || body.getParam("name").toString().isEmpty() ? domain.getName() : body.getParam("name").toString();

        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(accountId);
        message.addParam("domainId", domain.getId());
        message.addParam("redirectItems", redirectItems);
        message.addParam("name", name);

        ProcessingBusinessAction action = businessHelper.buildActionAndOperation(BusinessOperationType.REDIRECT_CREATE, BusinessActionType.REDIRECT_CREATE_RC, message);

        history.save(account, "Поступила заявка на создание перенаправления для домена: " + domain.getName(), request);

        return new ResponseEntity<>(createSuccessResponse(action), HttpStatus.ACCEPTED);
    }

    @PatchMapping("/{resourceId}")
    public ResponseEntity<SimpleServiceMessage> update(
            @PathVariable @ObjectId(PersonalAccount.class) String accountId,
            @PathVariable String resourceId,
            @RequestBody SimpleServiceMessage message,
            SecurityContextHolderAwareRequestWrapper request,
            Authentication authentication
    ) {
        if (request.isUserInRole("ADMIN") || request.isUserInRole("OPERATOR")) {
            checkParamsWithRoles(message.getParams(), REDIRECT_POST, authentication);
        } else {
            checkParamsWithRolesAndDeleteRestricted(message.getParams(), REDIRECT_POST, authentication);
        }

        if (message.getParam("redirectItems") != null
                && ((List<Map>) message.getParam("redirectItems")).size() > LIMIT_REDIRECT_FOR_DOMAIN) {
            throw new ParameterValidationException("Доступно не более 10 перенаправлений для одного домена");
        }

        PersonalAccount account = accountManager.findOne(accountId);
        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт не активен");
        }

        Redirect redirect = rcUserFeignClient.getRedirect(accountId, resourceId);

        message.setAccountId(accountId);
        message.addParam("resourceId", resourceId);

        boolean serviceIsPaid = accountRedirectServiceRepository
                .existsByPersonalAccountIdAndFullDomainNameAndExpireDateAfter(account.getId(), redirect.getDomain().getName(), LocalDate.now());

        if (!serviceIsPaid) {
            throw new ParameterValidationException("Для создания перенаправления закажите услугу");
        }

        ProcessingBusinessAction action = businessHelper.buildActionAndOperation(BusinessOperationType.REDIRECT_UPDATE, BusinessActionType.REDIRECT_UPDATE_RC, message);

        history.save(account, "Поступила заявка на обновление перенаправления для домена: " + redirect.getDomain().getName(), request);

        return new ResponseEntity<>(createSuccessResponse(action), HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<SimpleServiceMessage> delete(
            @PathVariable @ObjectId(PersonalAccount.class) String accountId,
            @PathVariable String resourceId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(accountId);
        message.addParam("resourceId", resourceId);
        ProcessingBusinessAction action = businessHelper.buildActionAndOperation(BusinessOperationType.REDIRECT_DELETE, BusinessActionType.REDIRECT_DELETE_RC, message);
        history.save(accountId, "Поступила заявка на удаление перенаправления с ID " + resourceId, request);
        return new ResponseEntity<>(createSuccessResponse(action), HttpStatus.ACCEPTED);
    }

    private void chargeByRedirect(PersonalAccount account, Domain domain) throws NotEnoughMoneyException {
        PaymentService paymentService = paymentServiceRepository.findByOldId(REDIRECT_SERVICE_OLD_ID);

        accountHelper.charge(
                account,
                new ChargeMessage.Builder(paymentService)
                        .setComment(domain.getName())
                        .build()
        );
    }

    private void addRedirectService(PersonalAccount account, Domain domain) {
        RedirectAccountService redirectAccountService = new RedirectAccountService();
        redirectAccountService.setFullDomainName(domain.getName());
        redirectAccountService.setPersonalAccountId(account.getId());
        redirectAccountService.setAutoRenew(true);
        redirectAccountService.setCreatedDate(LocalDate.now());
        redirectAccountService.setExpireDate(LocalDate.now().with(PLUS_ONE_YEAR));
        redirectAccountService.setActive(true);
        accountRedirectServiceRepository.insert(redirectAccountService);
    }

    private void assertDomainNotExistsOnWebsite(String accountId, String domainId) {
        WebSite webSite = null;
        try {
            webSite = rcUserFeignClient.getWebSiteByDomainId(accountId, domainId);
        } catch (ResourceNotFoundException ignored) {} //this is normal

        if (webSite != null) {
            throw new ParameterValidationException("Домен используется уже используется на сайте " + webSite.getName());
        }
    }
}
