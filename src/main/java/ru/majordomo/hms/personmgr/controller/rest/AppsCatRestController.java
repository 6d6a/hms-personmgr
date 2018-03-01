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

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.service.AppsCatService;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.common.Constants.ACCOUNT_ID_KEY;

@RestController
@Validated
public class AppsCatRestController extends CommonRestController {
    private AppsCatService appsCatService;

    @Autowired
    public void setAppsCatService(AppsCatService appsCatService) {
        this.appsCatService = appsCatService;
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

        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Установка приложений невозможна.");
        }

        message.addParam(ACCOUNT_ID_KEY, account.getAccountId());

        ProcessingBusinessAction businessAction;

        businessAction = appsCatService.processInstall(message);

        saveHistory(request, accountId,
                "Поступила заявка на установку приложения для сайта (имя: " + message.getParam("name") + ")");

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }
}
