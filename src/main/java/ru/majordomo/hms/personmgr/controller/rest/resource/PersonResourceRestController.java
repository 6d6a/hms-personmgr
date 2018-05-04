package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.dto.request.NicHandleAndPassword;
import ru.majordomo.hms.personmgr.dto.rpc.ClientInfoResponse;
import ru.majordomo.hms.personmgr.dto.rpc.ClientsLoginResponse;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.personmgr.service.Rpc.RegRpcClient;
import ru.majordomo.hms.personmgr.validation.ObjectId;
import ru.majordomo.hms.rc.user.resources.Person;

import javax.validation.Valid;

import static ru.majordomo.hms.personmgr.common.Constants.MJ_PARENT_CLIENT_ID_IN_REGISTRANT;

@RestController
@RequestMapping("/{accountId}/person")
@Validated
public class PersonResourceRestController extends CommonRestController {
    private final RcUserFeignClient rcUserFeignClient;
    private final RegRpcClient regRpcClient;

    public PersonResourceRestController(
            RcUserFeignClient rcUserFeignClient,
            RegRpcClient regRpcClient
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.regRpcClient = regRpcClient;
    }

    @PostMapping
    public ResponseEntity<SimpleServiceMessage> create(
            @RequestBody SimpleServiceMessage message,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        message.setAccountId(accountId);

        logger.debug("Creating person " + message.toString());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.PERSON_CREATE, BusinessActionType.PERSON_CREATE_RC, message);

        history.save(accountId, "Поступила заявка на создание персоны (имя: " + message.getParam("name") + ")", request);

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }

    @PatchMapping("/{resourceId}")
    public ResponseEntity<SimpleServiceMessage> update(
            @PathVariable String resourceId,
            @RequestBody SimpleServiceMessage message,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        message.setAccountId(accountId);
        message.addParam("resourceId", resourceId);

        logger.debug("Updating person with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.PERSON_UPDATE, BusinessActionType.PERSON_UPDATE_RC, message);

        history.save(accountId, "Поступила заявка на обновление персоны (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")", request);

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

        logger.debug("Deleting person with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.PERSON_DELETE, BusinessActionType.PERSON_DELETE_RC, message);

        history.save(accountId, "Поступила заявка на удаление персоны (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")",request);

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }

    @PreAuthorize("hasAuthority('MANAGE_PERSONS')")
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public Person create(
            @RequestBody Map<String, String> requestBody,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        String nicHandle = requestBody.get("nicHandle");

        if (nicHandle == null || nicHandle.equals("")) {
            throw new ParameterValidationException("Для добавления персоны необходимо указать её nicHandle");
        }

        logger.debug("Adding person by nicHandle: " + nicHandle);

        return rcUserFeignClient.addPersonByNicHandle(accountId, requestBody);
    }

    @PostMapping("/add-partner-person")
    public Person addPartnerPersonFromRegistrant(
            @Valid @RequestBody NicHandleAndPassword credentials,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        ClientsLoginResponse clientsLoginResponse = regRpcClient.loginAsClient(credentials);
        if (!clientsLoginResponse.getSuccess()) {
            throw new ParameterValidationException("Nic-Handle или пароль указаны неверно");
        }

        ClientInfoResponse clientInfoResponse = regRpcClient.getClientInfo(clientsLoginResponse.getClientId());
        if (!clientInfoResponse.getSuccess()) {
            throw new InternalApiException();
        }

        if (clientInfoResponse.getClient().getParentClientId().equals(MJ_PARENT_CLIENT_ID_IN_REGISTRANT)) {
            throw new ParameterValidationException("Клиент не обслуживается на партнерском договоре");
        }

        if (!clientInfoResponse.getClient().getNicHandle().equals(credentials.getNicHandle())) {
            throw new ParameterValidationException("Nic-Handle персоны не совпадает с переданным в запросе");
        }

//        if (rcUserFeignClient.getPersons(accountId)
//                .stream()
//                .anyMatch(p -> credentials.getNicHandle().equals(p.getNicHandle()))
//        ) {
//            throw new ParameterValidationException("На аккаунте уже есть персона с Nic-Handle");
//        }

        Map<String, String> params = new HashMap<>();
        params.put("nicHandle", credentials.getNicHandle());

        Person person = rcUserFeignClient.addPersonByNicHandle(accountId, params);
        history.save(
                accountId,
                "На аккаунт добавлена персона по Nic-Handle " + credentials.getNicHandle()
                        + " и паролю. Person: " + person.toString(),
                request
        );
        return person;
    }
}
