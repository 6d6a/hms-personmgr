package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.service.AppsCatService;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;

@RestController
@Validated
public class AppsCatRestController extends CommonRestController {
    private AppsCatService appsCatService;

    @Autowired
    public void setAppsCatService(AppsCatService appsCatService) {
        this.appsCatService = appsCatService;
    }

    @PostMapping("/{accountId}/app_install")
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            HttpServletResponse response,
            SecurityContextHolderAwareRequestWrapper request,
            Authentication authentication
    ) {
        message.setAccountId(accountId);

        logger.debug("Installing app on website: " + message.toString());

        if (!accountManager.findOne(accountId).isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Установка приложений невозможна.");
        }

        ProcessingBusinessAction businessAction;

        try {
            businessAction = appsCatService.install(message);
        } catch (Exception e) {
            return this.createErrorResponse(e.getMessage());
        }

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        //Save history
        String operator = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "service";
        Map<String, String> params = new HashMap<>();
        params.put(HISTORY_MESSAGE_KEY, "Поступила заявка на установку приложения для сайта (имя: " + message.getParam("name") + ")");
        params.put(OPERATOR_KEY, operator);

        publisher.publishEvent(new AccountHistoryEvent(accountId, params));

        return this.createSuccessResponse(businessAction);
    }
}
