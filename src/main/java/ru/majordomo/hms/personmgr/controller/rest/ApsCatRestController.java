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
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.service.ApsCatService;
import ru.majordomo.hms.personmgr.service.RcStaffFeignClient;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.personmgr.validation.ObjectId;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.resources.WebSite;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.WEB_SITE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.RequiredField.APP_INSTALL;

@RestController
@Validated
public class ApsCatRestController extends CommonRestController {
    private ApsCatService apsCatService;
    private RcUserFeignClient rcUserFeignClient;
    private RcStaffFeignClient rcStaffFeignClient;

    @Autowired
    public void setApsCatService(ApsCatService apsCatService) {
        this.apsCatService = apsCatService;
    }
    @Autowired
    public void setRcUserFeignClient(RcUserFeignClient rcUserFeignClient) {
        this.rcUserFeignClient = rcUserFeignClient;
    }

    @Autowired
    public void setRcStaffFeignClient(RcStaffFeignClient rcStaffFeignClient) {
        this.rcStaffFeignClient = rcStaffFeignClient;
    }

    @PostMapping("/app_install")
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
            businessAction = apsCatService.install(message);
        } catch (Exception e) {
            return this.createErrorResponse(e.getMessage());
        }

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        //Save history
        String operator = request.getUserPrincipal().getName();
        Map<String, String> params = new HashMap<>();
        params.put(HISTORY_MESSAGE_KEY, "Поступила заявка на создание пользователя баз данных " +
                "для установки приложения на сайт (имя: " + message.getParam("name") + ")");
        params.put(OPERATOR_KEY, operator);

        publisher.publishEvent(new AccountHistoryEvent(accountId, params));

//        checkRequiredParams(message.getParams(), APP_INSTALL_FULL);
//
//        ProcessingBusinessAction businessAction = process(BusinessOperationType.APP_INSTALL, BusinessActionType.APP_INSTALL_APSCAT, message);
//
//        response.setStatus(HttpServletResponse.SC_ACCEPTED);
//
//        //Save history
//        String operator = request.getUserPrincipal().getName();
//        Map<String, String> params = new HashMap<>();
//        params.put(HISTORY_MESSAGE_KEY, "Поступила заявка на установку приложения для сайта (имя: " + message.getParam("name") + ")");
//        params.put(OPERATOR_KEY, operator);
//
//        publisher.publishEvent(new AccountHistoryEvent(accountId, params));

        return this.createSuccessResponse(businessAction);
    }
}
