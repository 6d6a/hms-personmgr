package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.dto.DedicatedAppServiceDto;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.service.DedicatedAppService;
import ru.majordomo.hms.personmgr.service.DedicatedAppServiceHelper;
import ru.majordomo.hms.personmgr.validation.ObjectId;


import java.util.List;
import java.util.Objects;

import static ru.majordomo.hms.personmgr.common.FieldRoles.DEDICATED_APP_SERVER_POST;

@RestController
@Validated
public class DedicatedAppServiceRestController extends CommonRestController {

    private final DedicatedAppServiceHelper dedicatedAppServiceHelper;

    @Autowired
    public DedicatedAppServiceRestController(
            DedicatedAppServiceHelper dedicatedAppServiceHelper
    ) {
        this.dedicatedAppServiceHelper = dedicatedAppServiceHelper;
    }

    @GetMapping("/{accountId}/dedicated-app-service")
    public List<DedicatedAppServiceDto> getServices(
            @PathVariable @ObjectId(PersonalAccount.class) String accountId
    ) {
        return dedicatedAppServiceHelper.getServicesWithStaffService(accountId);
    }

    @GetMapping("/{accountId}/dedicated-app-service/{serviceId}")
    public ResponseEntity<DedicatedAppServiceDto> getService(
            @PathVariable @ObjectId(PersonalAccount.class) String accountId,
            @PathVariable @ObjectId(DedicatedAppService.class) String serviceId
    ) {
        DedicatedAppServiceDto dedicatedService = dedicatedAppServiceHelper.getServiceWithStaffService(serviceId);
        if (dedicatedService != null && !accountId.equals(dedicatedService.getPersonalAccountId())) {
            dedicatedService = null;
        }
        return dedicatedService != null ? ResponseEntity.ok(dedicatedService) : ResponseEntity.notFound().build();
    }

    @PostMapping("/{accountId}/dedicated-app-service")
    public ResponseEntity<SimpleServiceMessage> create(
            @PathVariable @ObjectId(PersonalAccount.class) String accountId,
            @RequestBody SimpleServiceMessage body,
            SecurityContextHolderAwareRequestWrapper request,
            Authentication authentication
    ) {
        if (request.isUserInRole("ADMIN") || request.isUserInRole("OPERATOR")) {
            checkParamsWithRoles(body.getParams(), DEDICATED_APP_SERVER_POST, authentication);
        } else {
            checkParamsWithRolesAndDeleteRestricted(body.getParams(), DEDICATED_APP_SERVER_POST, authentication);
        }

        String templateId = Objects.toString(body.getParam("templateId"), "");
        if (StringUtils.isEmpty(templateId)) {
            throw new ParameterValidationException("Необходимо указать templateId сервиса");
        }

        PersonalAccount account = accountManager.findOne(accountId);


        ProcessingBusinessAction action = dedicatedAppServiceHelper.create(account, templateId);

        return new ResponseEntity<>(action != null ? createSuccessResponse(action) : createSuccessResponse("Выделеный сервис активирован"), HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/{accountId}/dedicated-app-service/{serviceId}")
    public ResponseEntity<SimpleServiceMessage> disable(
            @PathVariable @ObjectId(PersonalAccount.class) String accountId,
            @PathVariable @ObjectId(DedicatedAppService.class) String serviceId
    ) {
        ProcessingBusinessAction action = dedicatedAppServiceHelper.disableDedicatedAppService(accountId, serviceId);
        if (action != null) {
            return ResponseEntity.accepted().body(createSuccessResponse(action));
        } else {
            return ResponseEntity.ok(createSuccessResponse("Услуга выделенного сервиса удалена"));
        }
    }
}
