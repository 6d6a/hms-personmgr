package ru.majordomo.hms.personmgr.controller.rest.resource;

import feign.FeignException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.ResourceType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.exception.BaseException;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.service.*;
import ru.majordomo.hms.personmgr.validation.ObjectId;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.WebSite;

import java.net.IDN;

import static ru.majordomo.hms.personmgr.common.Constants.WEB_SITE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.FieldRoles.DOMAIN_PATCH;

@RestController
@RequestMapping("/{accountId}/domain")
@Validated
public class DomainResourceRestController extends CommonRestController {
    private final AccountHelper accountHelper;
    private final RcUserFeignClient rcUserFeignClient;
    private final DomainService domainService;

    @Autowired
    public DomainResourceRestController(
            AccountHelper accountHelper,
            RcUserFeignClient rcUserFeignClient,
            DomainService domainService
    ) {
        this.accountHelper = accountHelper;
        this.rcUserFeignClient = rcUserFeignClient;
        this.domainService = domainService;
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

        if (!account.isActive() || account.isPreorder()) {
            throw new ParameterValidationException("?????????????? ??????????????????. ???????????????????? ???????????? ????????????????????.");
        }

        if (account.isFreeze()) {
            throw new ParameterValidationException("?????????????? ??????????????????. ???????????????????? ???????????? ????????????????????.");
        }

        resourceChecker.checkResource(account, ResourceType.DOMAIN, message.getParams());

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

        String webSiteId = (String) message.getParam(WEB_SITE_ID_KEY);
        BusinessOperationType operationType;
        if (StringUtils.isNotEmpty(webSiteId)) {
            try {
                WebSite webSite = rcUserFeignClient.getWebSite(accountId, webSiteId);
                operationType = BusinessOperationType.DOMAIN_CREATE_CHANGE_WEBSITE;
                if (webSite == null || webSite.isWillBeDeleted()) {
                    throw new ParameterValidationException("?????????????????????? ?????? ???????????????? ???????????? ??????-???????? ???? ????????????????????");
                }
            } catch (FeignException e) {
                logger.error("FeignException was thrown when get web-site " + webSiteId, e);
                throw new InternalApiException("???????????? ?????? ???????????????????? ?????????????? ?? ??????????: " + webSiteId, e);
            } catch (BaseException e) {
                throw e;
            } catch (Exception e) {
                logger.error("Unknown exception was thrown when get web-site " + webSiteId, e);
                throw e;
            }
        } else {
            message.removeParam(WEB_SITE_ID_KEY);
            operationType = BusinessOperationType.DOMAIN_CREATE;
        }

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(
                operationType, BusinessActionType.DOMAIN_CREATE_RC, message
        );

        history.save(accountId, "?????????????????? ???????????? ???? ???????????????????? ???????????? (??????: " + domainName + ")", request);

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }

    @PatchMapping("/{resourceId}")
    public ResponseEntity<SimpleServiceMessage> update(
            @PathVariable String resourceId,
            @RequestBody SimpleServiceMessage message,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request,
            Authentication authentication
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        message.setAccountId(accountId);
        message.getParams().put("resourceId", resourceId);

        logger.debug("Updating domain with id " + resourceId + " " + message.toString());

        if (!request.isUserInRole("ADMIN") && !account.isActive() || account.isPreorder()) {
            throw new ParameterValidationException("?????????????? ??????????????????. ???????????????????? ???????????? ????????????????????.");
        }

        resourceChecker.checkResource(account, ResourceType.DOMAIN, message.getParams());

        boolean switchedOn = message.getParam("switchedOn") != null && (boolean) message.getParam("switchedOn");
        boolean isRenew = message.getParam("renew") != null && (boolean) message.getParam("renew");
        boolean isRegistration = message.getParam("register") != null && (boolean) message.getParam("register");

        if (message.getParam("infested") != null) {
            if (request.isUserInRole("ADMIN") || request.isUserInRole("OPERATOR")) {
                checkParamsWithRoles(message.getParams(), DOMAIN_PATCH, authentication);
            } else {
                throw new ParameterValidationException("???????????????????? ???????????? ????????????????????.");
            }
        }

        final Domain domain = (switchedOn || isRenew || isRegistration)
                ? rcUserFeignClient.getDomain(accountId, resourceId) : null;

        if (switchedOn) {
            domainService.checkBlacklistOnUpdate(domain.getName());
        }

        if (isRegistration || isRenew) {
            domainService.blockMoneyBeforeManualRenewOrRegistrationExistsDomain(account, message, domain);
        }

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.DOMAIN_UPDATE, BusinessActionType.DOMAIN_UPDATE_RC, message);

        String action = (isRenew ? "??????????????????" : isRegistration ? "?????????????????????? ??????????????????????????" : "????????????????????");
        String name = domain != null ? domain.getName() : (String) message.getParam("name");

        history.save(accountId, "?????????????????? ???????????? ???? " + action + " ???????????? (Id: " + resourceId  + ", ??????: " + name + ")", request);

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

        history.save(accountId, "?????????????????? ???????????? ???? ???????????????? ???????????? (Id: " + resourceId  + ", ??????: " + message.getParam("name") + ")", request);

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }
}
