package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ru.majordomo.hms.personmgr.common.Constants;
import ru.majordomo.hms.personmgr.common.ResourceType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.service.AccountServiceHelper;
import ru.majordomo.hms.personmgr.service.AccountHelper;

import ru.majordomo.hms.personmgr.service.AppsCatService;
import ru.majordomo.hms.personmgr.service.DedicatedAppServiceHelper;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.common.Constants.ACCOUNT_ID_KEY;

@RestController
@Validated
public class AppsCatRestController extends CommonRestController {
    private AppsCatService appsCatService;
    private PlanManager planManager;
    private DedicatedAppServiceHelper dedicatedAppServiceHelper;
    private AccountHelper accountHelper;

    @Autowired
    public void setPlanManager(PlanManager planManager) {
        this.planManager = planManager;
    }

    @Autowired
    public void setDedicatedAppServiceHelper(DedicatedAppServiceHelper dedicatedAppServiceHelper) {
        this.dedicatedAppServiceHelper = dedicatedAppServiceHelper;
    }

    @Autowired
    public void setAppsCatService(AppsCatService appsCatService) {
        this.appsCatService = appsCatService;
    }

    @Autowired
    public void setAccountHelper(AccountHelper accountHelper) {
        this.accountHelper = accountHelper;
    }

    @PostMapping("/{accountId}/app_install")
    public ResponseEntity<SimpleServiceMessage> create(
            @RequestBody SimpleServiceMessage message,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request,
            Authentication authentication
    ) {
        message.setAccountId(accountId);

        logger.debug("Installing app on website: " + message.toString());

        PersonalAccount account = accountManager.findOne(accountId);

        if (!account.isActive() && !account.isPreorder()) {
            throw new ParameterValidationException("Аккаунт неактивен. Установка приложений невозможна.");
        }

        accountHelper.checkIsCmsAllowed(account);

        message.addParam(ACCOUNT_ID_KEY, account.getAccountId());

        ProcessingBusinessAction businessAction;

        businessAction = appsCatService.processInstall(message);

        history.save(accountId,"Поступила заявка на установку приложения для сайта (имя: " + message.getParam("name") + ")", request);

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }
}
