package ru.majordomo.hms.personmgr.controller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.WebAccessAccount;
import ru.majordomo.hms.personmgr.common.message.ResponseMessage;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.service.BusinessActionBuilder;
import ru.majordomo.hms.personmgr.service.FinFeignClient;
import ru.majordomo.hms.personmgr.service.SequenceCounterService;
import ru.majordomo.hms.personmgr.service.SiFeignClient;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static ru.majordomo.hms.personmgr.common.StringConstants.VH_ACCOUNT_PREFIX;

/**
 * RestAccountController
 */
@RestController
@RequestMapping("/account")
public class RestAccountController extends CommonRestController {
    private final static Logger logger = LoggerFactory.getLogger(RestAccountController.class);

    @Autowired
    private SequenceCounterService sequenceCounterService;

    @Autowired
    private BusinessActionBuilder businessActionBuilder;

    @Autowired
    private ProcessingBusinessActionRepository processingBusinessActionRepository;

    @Autowired
    private PersonalAccountRepository personalAccountRepository;

    @Autowired
    private SiFeignClient siFeignClient;

    @Autowired
    private FinFeignClient finFeignClient;

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response
    ) {
        logger.info(message.toString());
        //Create pm, si and fin account

        String accountId = String.valueOf(sequenceCounterService.getNextSequence("PersonalAccount"));
        String password = randomAlphabetic(8);

        PersonalAccount personalAccount = new PersonalAccount();
        personalAccount.setAccountType(AccountType.VIRTUAL_HOSTING);
        personalAccount.setAccountId(accountId);
        personalAccount.setName(VH_ACCOUNT_PREFIX + accountId);

        personalAccountRepository.save(personalAccount);

        WebAccessAccount webAccessAccount = new WebAccessAccount();
        webAccessAccount.setAccountId(personalAccount.getId());
        webAccessAccount.setPassword(password);
        webAccessAccount.setEnabled(true);
        webAccessAccount.setAccountNonExpired(true);
        webAccessAccount.setAccountNonLocked(true);
        webAccessAccount.setCredentialsNonExpired(true);
        webAccessAccount.setUsername(personalAccount.getName());
        Set<String> roles = new HashSet<>();
        roles.addAll(Collections.singletonList("ROLE_USER"));

        webAccessAccount.setRoles(roles);

        siFeignClient.create(webAccessAccount);

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.ACCOUNT_CREATE_RC, message);

        processingBusinessActionRepository.save(businessAction);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createResponse(businessAction);
    }

    @RequestMapping(value = "/{accountId}", method = RequestMethod.PATCH)
    public ResponseMessage update(
            @PathVariable String accountId,
            @RequestBody SimpleServiceMessage message, HttpServletResponse response
    ) {
        logger.info("Updating account with id " + accountId + " " + message.toString());

        message.getParams().put("id", accountId);

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.ACCOUNT_UPDATE_RC, message);

        processingBusinessActionRepository.save(businessAction);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createResponse(businessAction);
    }

    @RequestMapping(value = "/{accountId}", method = RequestMethod.DELETE)
    public ResponseMessage delete(
            @PathVariable String accountId,
            @RequestBody SimpleServiceMessage message, HttpServletResponse response
    ) {
        logger.info("Deleting account with id " + accountId + " " + message.toString());

        message.getParams().put("id", accountId);

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.ACCOUNT_DELETE_RC, message);

        processingBusinessActionRepository.save(businessAction);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createResponse(businessAction);
    }
}
