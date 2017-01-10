package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.DomainTldService;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.personmgr.validators.ObjectId;
import ru.majordomo.hms.rc.user.resources.Domain;

@RestController
@RequestMapping("/{accountId}/domain")
@Validated
public class RestDomainController extends CommonRestResourceController {
    private final DomainTldService domainTldService;
    private final PersonalAccountRepository accountRepository;
    private final AccountHelper accountHelper;
    private final RcUserFeignClient rcUserFeignClient;
    private final static Logger logger = LoggerFactory.getLogger(RestDomainController.class);

    @Autowired
    public RestDomainController(
            DomainTldService domainTldService,
            PersonalAccountRepository accountRepository,
            AccountHelper accountHelper,
            RcUserFeignClient rcUserFeignClient
    ) {
        this.domainTldService = domainTldService;
        this.accountRepository = accountRepository;
        this.accountHelper = accountHelper;
        this.rcUserFeignClient = rcUserFeignClient;
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId", required = false) String accountId) {
        message.setAccountId(accountId);

        logger.debug(message.toString());

        PersonalAccount account = accountRepository.findOne(accountId);

        boolean isRegistration = message.getParam("register") != null && (boolean) message.getParam("register");

        String domainName = (String) message.getParam("name");

        if (isRegistration) {
            DomainTld domainTld = domainTldService.findActiveDomainTldByDomainName(domainName);
            accountHelper.checkBalance(account, domainTld.getRegistrationService());
            SimpleServiceMessage blockResult = accountHelper.block(account, domainTld.getRegistrationService());
            String documentNumber = (String) blockResult.getParam("documentNumber");
            message.addParam("documentNumber", documentNumber);
        }

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.DOMAIN_CREATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{domainId}", method = RequestMethod.PATCH)
    public SimpleServiceMessage update(
            @PathVariable String domainId,
            @RequestBody SimpleServiceMessage message, HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId", required = false) String accountId) {
        PersonalAccount account = accountRepository.findOne(accountId);

        message.setAccountId(accountId);

        logger.debug("Updating domain with id " + domainId + " " + message.toString());

        message.getParams().put("resourceId", domainId);

        boolean isRenew = message.getParam("renew") != null && (boolean) message.getParam("renew");

        if (isRenew) {
            Domain domain = rcUserFeignClient.getDomain(accountId, domainId);

            DomainTld domainTld = domainTldService.findDomainTldByDomainNameAndRegistrator(domain.getName(), domain.getRegSpec().getRegistrar());

            accountHelper.checkBalance(account, domainTld.getRenewService());

            SimpleServiceMessage blockResult = accountHelper.block(account, domainTld.getRenewService());

            String documentNumber = (String) blockResult.getParam("documentNumber");
            message.addParam("documentNumber", documentNumber);
        }

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.DOMAIN_UPDATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{domainId}", method = RequestMethod.DELETE)
    public SimpleServiceMessage delete(
            @PathVariable String domainId,
            HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId", required = false) String accountId) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(accountId);
        message.addParam("resourceId", domainId);
        message.setAccountId(accountId);

        logger.debug("Deleting domain with id " + domainId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.DOMAIN_DELETE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }
}
