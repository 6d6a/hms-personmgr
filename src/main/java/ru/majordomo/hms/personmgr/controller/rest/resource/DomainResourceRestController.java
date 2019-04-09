package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import ru.majordomo.hms.personmgr.common.AvailabilityInfo;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.feign.DomainRegistrarFeignClient;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.service.*;
import ru.majordomo.hms.personmgr.validation.ObjectId;
import ru.majordomo.hms.rc.user.resources.Domain;

import java.math.BigDecimal;
import java.net.IDN;

@RestController
@RequestMapping("/{accountId}/domain")
@Validated
public class DomainResourceRestController extends CommonRestController {
    private final DomainTldService domainTldService;
    private final AccountHelper accountHelper;
    private final RcUserFeignClient rcUserFeignClient;
    private final DomainService domainService;
    private final DomainRegistrarFeignClient domainRegistrarFeignClient;

    @Autowired
    public DomainResourceRestController(
            DomainTldService domainTldService,
            AccountHelper accountHelper,
            RcUserFeignClient rcUserFeignClient,
            DomainService domainService,
            DomainRegistrarFeignClient domainRegistrarFeignClient
    ) {
        this.domainTldService = domainTldService;
        this.accountHelper = accountHelper;
        this.rcUserFeignClient = rcUserFeignClient;
        this.domainService = domainService;
        this.domainRegistrarFeignClient = domainRegistrarFeignClient;
    }

    @PostMapping
    public ResponseEntity<SimpleServiceMessage> create(
            @RequestBody SimpleServiceMessage message,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        message.setAccountId(accountId);
        message.removeParam("register");

        PersonalAccount account = accountManager.findOne(accountId);

        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Добавление домена невозможно.");
        }

        logger.debug("Creating domain " + message.toString());

        String domainName = (String) message.getParam("name");
        domainName = domainName.toLowerCase();
        domainName = IDN.toUnicode(domainName);
        message.addParam("name", domainName);

        String parentDomainId = (String) message.getParam("parentDomainId");
        if (parentDomainId != null && !parentDomainId.equals("")) {
            Domain parentDomain = rcUserFeignClient.getDomain(accountId, parentDomainId);

            domainName = domainName.substring(domainName.length() - 1).equals(".") ?
                    domainName + parentDomain.getName() : domainName + "." + parentDomain.getName();
        }

        accountHelper.checkIsDomainAddAllowed(account, domainName);

        domainService.checkBlacklist(domainName, accountId);

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(
                BusinessOperationType.DOMAIN_CREATE, BusinessActionType.DOMAIN_CREATE_RC, message
        );

        history.save(accountId, "Поступила заявка на добавление домена (имя: " + domainName + ")", request);

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }

    @PatchMapping("/{resourceId}")
    public ResponseEntity<SimpleServiceMessage> update(
            @PathVariable String resourceId,
            @RequestBody SimpleServiceMessage message,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        Domain domain = null;

        PersonalAccount account = accountManager.findOne(accountId);

        message.setAccountId(accountId);
        message.getParams().put("resourceId", resourceId);

        logger.debug("Updating domain with id " + resourceId + " " + message.toString());

        if (!request.isUserInRole("ADMIN") && !account.isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Обновление домена невозможно.");
        }

        if (message.getParam("switchedOn") != null && (boolean) message.getParam("switchedOn")) {
            domain = rcUserFeignClient.getDomain(accountId, resourceId);

            domainService.checkBlacklistOnUpdate(domain.getName());
        }

        boolean isRenew = message.getParam("renew") != null && (boolean) message.getParam("renew");
        boolean isRegistration = message.getParam("register") != null && (boolean) message.getParam("register");

        if (isRenew) {
            if (domain == null) {
                domain = rcUserFeignClient.getDomain(accountId, resourceId);
            }

            DomainTld domainTld = domainTldService.findDomainTldByDomainNameAndRegistrator(domain.getName(), domain.getRegSpec().getRegistrar());

            BigDecimal pricePremium = domainRegistrarFeignClient.getRenewPremiumPrice(domain.getName());
            if (pricePremium != null && (pricePremium.compareTo(BigDecimal.ZERO) > 0)) {
                domainTld.getRenewService().setCost(pricePremium);
            }

            accountHelper.checkBalance(account, domainTld.getRenewService());
            accountHelper.checkBalanceWithoutBonus(account, domainTld.getRenewService());

            ChargeMessage chargeMessage = new ChargeMessage.Builder(domainTld.getRenewService())
                    .excludeBonusPaymentType()
                    .setComment(domain.getName())
                    .build();

            SimpleServiceMessage blockResult = accountHelper.block(account, chargeMessage);

            String documentNumber = (String) blockResult.getParam("documentNumber");
            message.addParam("documentNumber", documentNumber);
        } else if (isRegistration) {
            if (domain == null) {
                domain = rcUserFeignClient.getDomain(accountId, resourceId);
            }

            DomainTld domainTld = domainTldService.findActiveDomainTldByDomainName(domain.getName());

            AvailabilityInfo info = domainRegistrarFeignClient.getAvailabilityInfo(domain.getName());

            if (info == null) {
                throw new ParameterValidationException("Не удалось проверить доступность домена");
            } else if (!info.getFree()) {
                throw new ParameterValidationException("Домен " + domain.getName() + " занят");
            }

            BigDecimal cost = domainTld.getRegistrationService().getCost();

            if (info.getPremiumPrice() != null && info.getPremiumPrice().compareTo(BigDecimal.ZERO) > 0) {
                cost = info.getPremiumPrice();
            }

            accountHelper.checkBalanceWithoutBonus(account, domainTld.getRegistrationService());

            ChargeMessage chargeMessage = new ChargeMessage.Builder(domainTld.getRegistrationService())
                    .setAmount(cost)
                    .excludeBonusPaymentType()
                    .setComment(domain.getName())
                    .build();

            SimpleServiceMessage blockResult = accountHelper.block(account, chargeMessage);

            String documentNumber = (String) blockResult.getParam("documentNumber");
            message.addParam("documentNumber", documentNumber);
        }

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.DOMAIN_UPDATE, BusinessActionType.DOMAIN_UPDATE_RC, message);

        String action = (isRenew ? "продление" : isRegistration ? "регистрацию существующего" : "обновление");
        String name = domain != null ? domain.getName() : (String) message.getParam("name");

        history.save(accountId, "Поступила заявка на " + action + " домена (Id: " + resourceId  + ", имя: " + name + ")", request);

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<SimpleServiceMessage> delete(
            @PathVariable String resourceId,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.addParam("resourceId", resourceId);
        message.setAccountId(accountId);

        logger.debug("Deleting domain with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.DOMAIN_DELETE, BusinessActionType.DOMAIN_DELETE_RC, message);

        history.save(accountId, "Поступила заявка на удаление домена (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")", request);

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }
}
