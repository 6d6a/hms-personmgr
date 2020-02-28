package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.validation.ObjectId;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;

import java.util.Map;

import static ru.majordomo.hms.personmgr.common.FieldRoles.DNS_RECORD_PATCH;

@RestController
@RequestMapping("/{accountId}/dns-record")
@Validated
public class DnsRecordResourceRestController extends CommonRestController {

    private RcUserFeignClient rcUserFeignClient;

    @Autowired
    public DnsRecordResourceRestController(RcUserFeignClient rcUserFeignClient) {
        this.rcUserFeignClient = rcUserFeignClient;
    }

    @PostMapping
    public ResponseEntity<SimpleServiceMessage> create(
            @RequestBody SimpleServiceMessage message,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        message.setAccountId(accountId);

        logger.debug("Creating DnsRecord. Message: " + message.toString());

        PersonalAccount account = accountManager.findOne(accountId);

        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Создание DNS-записи невозможно.");
        }

        if (account.isFreeze()) {
            throw new ParameterValidationException("Аккаунт заморожен. Создание DNS-записи невозможно.");
        }

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.DNS_RECORD_CREATE, BusinessActionType.DNS_RECORD_CREATE_RC, message);

        history.save(accountId, "Поступила заявка на создание днс-записи. (Данные: " +
                getFromParamsForDnsHistory(message.getParams()) + ")", request);

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
        message.setAccountId(accountId);
        message.getParams().put("resourceId", resourceId);

        logger.debug("Updating DnsRecord with id " + resourceId + " " + message.toString());

        PersonalAccount account = accountManager.findOne(accountId);

        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Обновление DNS-записи невозможно.");
        }

        if (account.isFreeze()) {
            throw new ParameterValidationException("Аккаунт заморожен. Обновление DNS-записи невозможно.");
        }

        checkParamsWithRoles(message.getParams(), DNS_RECORD_PATCH, authentication);

        history.save(accountId, "Поступила заявка на обновление днс-записи c Id: " + resourceId  + " (Старые данные: " +
                getFromRecordForDnsHistory(accountId, resourceId) + ". Новые данные: " + getFromParamsForDnsHistory(message.getParams()) + ")", request);

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.DNS_RECORD_UPDATE, BusinessActionType.DNS_RECORD_UPDATE_RC, message);

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }

    private String getFromRecordForDnsHistory(String accountId, String resourceId) {
        DNSResourceRecord record = rcUserFeignClient.getRecord(accountId, resourceId);
        return "доменное имя: " + record.getOwnerName() + ", тип: " + record.getRrType() + ", значение: " + record.getData() +
                ", ttl: " + record.getTtl() + ", приоритет: " + record.getPrio();
    }

    private String getFromParamsForDnsHistory(Map<String, Object> params) {
        String ownerName = params.get("ownerName") != null ? params.get("ownerName").toString() : "";
        String type = params.get("rrType") != null ? params.get("rrType").toString() : "";
        String ttl = params.get("ttl") != null ? params.get("ttl").toString() : "";
        String prio = params.get("prio") != null ? params.get("prio").toString() : "";
        String data = params.get("data") != null ? params.get("data").toString() : "";

        return "доменное имя: " + ownerName + ", тип: " + type + ", значение: " + data +
                ", ttl: " + ttl + ", приоритет: " + prio;
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<SimpleServiceMessage> delete(
            @PathVariable String resourceId,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        SimpleServiceMessage message = new SimpleServiceMessage()
                .withAccountId(accountId)
                .withParam("resourceId", resourceId);

        logger.debug("Deleting DnsRecord with id " + resourceId + " " + message.toString());

        PersonalAccount account = accountManager.findOne(accountId);

        if (account.isFreeze()) {
            throw new ParameterValidationException("Аккаунт заморожен. Обновление DNS-записи невозможно.");
        }

        history.save(accountId, "Поступила заявка на удаление днс-записи c Id: " + resourceId  + " (Данные: " +
                getFromRecordForDnsHistory(accountId, resourceId) + ")", request);

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.DNS_RECORD_DELETE, BusinessActionType.DNS_RECORD_DELETE_RC, message);

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }
}
