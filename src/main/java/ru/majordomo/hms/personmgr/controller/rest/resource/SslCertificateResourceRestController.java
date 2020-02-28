package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.springframework.http.ResponseEntity;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.ResourceType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.common.Constants.SSL_CHECK_PROHIBITED_KEY;

@RestController
@RequestMapping("/{accountId}/ssl-certificate")
@Validated
public class SslCertificateResourceRestController extends CommonRestController {

    @PostMapping
    public ResponseEntity<SimpleServiceMessage> create(
            @RequestBody SimpleServiceMessage message,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        message.setAccountId(accountId);

        logger.debug("Creating sslcertificate " + message.toString());

        PersonalAccount account = accountManager.findOne(accountId);

        if (!account.isActive() || account.isPreorder()) {
            throw new ParameterValidationException("Аккаунт неактивен. Заказ SSL-сертификата невозможен.");
        }

        if (account.isFreeze()) {
            throw new ParameterValidationException("Аккаунт заморожен. Заказ SSL-сертификата невозможен.");
        }

        resourceChecker.checkResource(account, ResourceType.SSL_CERTIFICATE, message.getParams());
        message.removeParam(SSL_CHECK_PROHIBITED_KEY);

        String domainName = (String) message.getParam("name");

        boolean inProcessing = businessHelper.existsActiveOperations(accountId, BusinessOperationType.SSL_CERTIFICATE_CREATE, message);
        if (inProcessing) {
            throw new ParameterValidationException("Сертификат для домена " + domainName + " находится в процессе создания");
        }

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.SSL_CERTIFICATE_CREATE, BusinessActionType.SSL_CERTIFICATE_CREATE_RC, message);

        history.save(accountId, "Поступила заявка на создание SSL-сертификата (имя: " + message.getParam("name") + ")", request);

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }

    @PatchMapping("/{resourceId}")
    public ResponseEntity<SimpleServiceMessage> update(
            @PathVariable String resourceId,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody SimpleServiceMessage message,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        message.setAccountId(accountId);
        message.addParam("resourceId", resourceId);

        PersonalAccount account = accountManager.findOne(accountId);

        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Управление SSL-сертификатами недоступно.");
        }

        if (account.isFreeze()) {
            throw new ParameterValidationException("Аккаунт заморожен. Управление SSL-сертификатами недоступно.");
        }

        logger.debug("Update sslcertificate " + message.toString());

        resourceChecker.checkResource(account, ResourceType.SSL_CERTIFICATE, message.getParams());
        message.removeParam(SSL_CHECK_PROHIBITED_KEY);

        boolean inProcessing = businessHelper.existsActiveOperations(accountId, BusinessOperationType.SSL_CERTIFICATE_UPDATE, message);
        if (inProcessing) {
            throw new ParameterValidationException("Сертификат находится в процессе обновления");
        }

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(
                BusinessOperationType.SSL_CERTIFICATE_UPDATE, BusinessActionType.SSL_CERTIFICATE_UPDATE_RC, message);

        history.save(accountId, "Поступила заявка на обновление SSL-сертификата (id: " + resourceId + ")", request);

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

        logger.debug("Deleting sslcertificate with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.SSL_CERTIFICATE_DELETE, BusinessActionType.SSL_CERTIFICATE_DELETE_RC, message);

        history.save(accountId, "Поступила заявка на удаление SSL-сертификата (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")", request);

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }
}
