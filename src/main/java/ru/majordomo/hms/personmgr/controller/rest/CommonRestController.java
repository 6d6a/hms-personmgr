package ru.majordomo.hms.personmgr.controller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ParameterWithRoleSecurityException;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.service.BusinessHelper;
import ru.majordomo.hms.personmgr.service.PlanCheckerService;

import static ru.majordomo.hms.personmgr.common.Constants.*;

public class CommonRestController {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected PersonalAccountManager accountManager;
    protected ApplicationEventPublisher publisher;
    protected PaymentServiceRepository paymentServiceRepository;
    protected AccountServiceRepository accountServiceRepository;
    protected PlanCheckerService planCheckerService;
    protected BusinessHelper businessHelper;

    @Autowired
    public void setAccountManager(PersonalAccountManager accountManager) {
        this.accountManager = accountManager;
    }

    @Autowired
    public void setPaymentServiceRepository(PaymentServiceRepository paymentServiceRepository) {
        this.paymentServiceRepository = paymentServiceRepository;
    }

    @Autowired
    public void setAccountServiceRepository(AccountServiceRepository accountServiceRepository) {
        this.accountServiceRepository = accountServiceRepository;
    }

    @Autowired
    public void setPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Autowired
    public void setPlanCheckerService(PlanCheckerService planCheckerService) {
        this.planCheckerService = planCheckerService;
    }

    @Autowired
    public void setBusinessHelper(BusinessHelper businessHelper) {
        this.businessHelper = businessHelper;
    }

    private SimpleServiceMessage createResponse() {
        return new SimpleServiceMessage();
    }

    protected SimpleServiceMessage createSuccessResponse(ProcessingBusinessAction businessAction) {
        SimpleServiceMessage message = createResponse();
        message = fillStatus(message, true);
        message = fillFromBusinessAction(message, businessAction);

        return message;
    }

    protected SimpleServiceMessage createSuccessResponse(String successMessage) {
        SimpleServiceMessage message = createResponse();
        message = fillStatus(message, true);
        message = fillTextMessage(message, "successMessage", successMessage);

        return message;
    }

    protected SimpleServiceMessage createErrorResponse(String errorMessage) {
        SimpleServiceMessage message = createResponse();
        message = fillStatus(message, false);
        message = fillTextMessage(message, "errorMessage", errorMessage);

        return message;
    }

    private SimpleServiceMessage fillFromBusinessAction(SimpleServiceMessage message, ProcessingBusinessAction businessAction) {
        message.setActionIdentity(businessAction.getId());
        message.setOperationIdentity(businessAction.getOperationId());

        message.addParams(businessAction.getParams());

        return message;
    }

    private SimpleServiceMessage fillStatus(SimpleServiceMessage message, boolean success) {
        message.addParam("success", success);

        return message;
    }

    private SimpleServiceMessage fillTextMessage(SimpleServiceMessage message, String messageName, String messageText) {
        message.addParam(messageName, messageText);

        return message;
    }

    protected void checkParamsWithRoles(Map<String, Object> params, Map<String, String> paramsWithRoles, Authentication request) {
        paramsWithRoles.forEach((param, role) -> {
            if (params.get(param) != null && request.getAuthorities().stream().noneMatch(ga -> ga.getAuthority().equals(role))) {
                logger.debug("Changing '" + param + "' property is forbidden. Only role '" + role + "' allowed to edit.");
                throw new ParameterWithRoleSecurityException("Изменение параметра '" + param + "' запрещено");
            }
        });
    }

    protected void checkParamsWithRolesAndDeleteRestricted(
            Map<String, Object> params,
            Map<String, String> paramsWithRoles,
            Authentication request
    ) {
        paramsWithRoles.forEach((param, role) -> {
            if (params.get(param) != null && request.getAuthorities().stream().noneMatch(ga -> ga.getAuthority().equals(role))) {
                logger.debug("Changing '" + param + "' property is forbidden. Only role '" + role + "' allowed to edit.");
                params.remove(param);
            }
        });
    }

    protected void checkParamsForServicesOnUpdate(
            Map<String, Object> params,
            PersonalAccount account
    ) {
        params.forEach((k,v) -> {
            //Антиспам
            if (k.equals(MAILBOX_ANTISPAM_FIELD) && (Boolean) v) {
                //Проверяем что у аккаунта есть услуга анти-спам
                PaymentService paymentService = paymentServiceRepository.findByOldId(ANTI_SPAM_SERVICE_ID);

                if (paymentService == null) {
                    throw new ParameterValidationException("Услуга анти-спам не найдена");
                }

                List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(
                        account.getId(), paymentService.getId()
                );

                if (accountServices == null || accountServices.isEmpty()) {
                    throw new ParameterValidationException("Услуга анти-спам не подключена");
                }

                accountServices.forEach(item-> {
                    if (!item.isEnabled()){
                        throw new ParameterValidationException("Услуга анти-спам не подключена");
                    }
                });
            }

        });
    }

    public void addHistoryMessage(String operator, String accountId, String message) {
        Map<String, String> params = new HashMap<>();
        params.put(HISTORY_MESSAGE_KEY, message);
        params.put(OPERATOR_KEY, operator);
        publisher.publishEvent(new AccountHistoryEvent(accountId, params));
    }
}
