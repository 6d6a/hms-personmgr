package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.ServicePlan;
import ru.majordomo.hms.personmgr.model.service.LongLifeResourceArchive;
import ru.majordomo.hms.personmgr.repository.LongLifeResourceArchiveRepository;
import ru.majordomo.hms.personmgr.repository.ServicePlanRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.ChargeMessage;
import ru.majordomo.hms.personmgr.validation.ObjectId;
import ru.majordomo.hms.rc.user.resources.ResourceArchiveType;

import static ru.majordomo.hms.personmgr.common.Constants.ARCHIVED_RESOURCE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.LONG_LIFE;
import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_TYPE;

@RestController
@Validated
public class LongLifeResourceArchiveRestController extends CommonRestController {
    private final LongLifeResourceArchiveRepository longLifeResourceArchiveRepository;
    private final ServicePlanRepository servicePlanRepository;
    private final AccountHelper accountHelper;

    @Autowired
    public LongLifeResourceArchiveRestController(
            LongLifeResourceArchiveRepository longLifeResourceArchiveRepository,
            ServicePlanRepository servicePlanRepository,
            AccountHelper accountHelper
    ) {
        this.longLifeResourceArchiveRepository = longLifeResourceArchiveRepository;
        this.servicePlanRepository = servicePlanRepository;
        this.accountHelper = accountHelper;
    }

    @GetMapping("/{accountId}/long-life-resource-archive")
    public List<LongLifeResourceArchive> getAll(
            @PathVariable @ObjectId(PersonalAccount.class) String accountId
    ) {
        return longLifeResourceArchiveRepository.findByPersonalAccountId(accountId);
    }

    @GetMapping("/{accountId}/long-life-resource-archive/filter")
    public List<LongLifeResourceArchive> getAllFilter(
            @PathVariable @ObjectId(PersonalAccount.class) String accountId,
            @RequestParam(required = false) String archivedResourceId,
            @RequestParam(required = false) ResourceArchiveType resourceArchiveType
    ) {
        LongLifeResourceArchive longLifeResourceArchive = new LongLifeResourceArchive();

        longLifeResourceArchive.setPersonalAccountId(accountId);

        if (archivedResourceId != null) {
            longLifeResourceArchive.setArchivedResourceId(archivedResourceId);
        }

        if (resourceArchiveType != null) {
            longLifeResourceArchive.setType(resourceArchiveType);
        }

        return longLifeResourceArchiveRepository.findAll(Example.of(longLifeResourceArchive));
    }

    @GetMapping("/{accountId}/long-life-resource-archive/{id}")
    public LongLifeResourceArchive get(
            @PathVariable @ObjectId(PersonalAccount.class) String accountId,
            @PathVariable @ObjectId(LongLifeResourceArchive.class) String id
    ) {
        return longLifeResourceArchiveRepository.findByPersonalAccountIdAndId(accountId, id);
    }

    @PostMapping("/{accountId}/long-life-resource-archive")
    public ResponseEntity<SimpleServiceMessage> create(
            @PathVariable @ObjectId(PersonalAccount.class) String accountId,
            @RequestBody SimpleServiceMessage body,
            SecurityContextHolderAwareRequestWrapper request,
            Authentication authentication
    ) {
        PersonalAccount account = accountManager.findOne(accountId);
        assertAccountIsActive(account);

        body.setAccountId(accountId);

        String archivedResourceId = (String) body.getParam(ARCHIVED_RESOURCE_ID_KEY);
        ResourceArchiveType resourceArchiveType = ResourceArchiveType.valueOf((String) body.getParam(RESOURCE_TYPE));

        body.addParam(LONG_LIFE, true);

//        ServicePlan plan = servicePlanRepository.findOneByFeatureAndActive(Feature.LONG_LIFE_RESOURCE_ARCHIVE, true);
//
//        if (plan == null) {
//            throw new ParameterValidationException("Тариф для заказа услуги 'вечный архив' не найден");
//        }
//
//        accountHelper.checkBalance(account, plan.getService());
//
//        ChargeMessage chargeMessage = new ChargeMessage.Builder(plan.getService()).build();
//
//        SimpleServiceMessage blockResult = accountHelper.block(account, chargeMessage);
//        String documentNumber = (String) blockResult.getParam("documentNumber");
//        body.addParam("documentNumber", documentNumber);

        ProcessingBusinessAction action = businessHelper.buildActionAndOperation(
                BusinessOperationType.RESOURCE_ARCHIVE_CREATE,
                BusinessActionType.RESOURCE_ARCHIVE_CREATE_RC,
                body
        );

        history.save(account, "Поступила заявка на создание вечного архива " +
                "для ресурса: " + archivedResourceId + " с типом: " + resourceArchiveType.name(), request);

        return new ResponseEntity<>(createSuccessResponse(action), HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/{accountId}/long-life-resource-archive/{id}")
    public ResponseEntity<SimpleServiceMessage> delete(
            @PathVariable @ObjectId(PersonalAccount.class) String accountId,
            @PathVariable @ObjectId(LongLifeResourceArchive.class) String id,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        LongLifeResourceArchive longLifeResourceArchive = longLifeResourceArchiveRepository.findByPersonalAccountIdAndId(accountId, id);

        if (longLifeResourceArchive != null) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setAccountId(accountId);
            message.addParam(RESOURCE_ID_KEY, longLifeResourceArchive.getResourceArchiveId());
            message.addParam(LONG_LIFE, true);

            ProcessingBusinessAction action = businessHelper.buildActionAndOperation(
                    BusinessOperationType.RESOURCE_ARCHIVE_DELETE,
                    BusinessActionType.RESOURCE_ARCHIVE_DELETE_RC,
                    message
            );
            history.save(accountId, "Поступила заявка на удаление вечного архива с ID " + id +
                    " для ресурса: " + longLifeResourceArchive.getArchivedResourceId() + " с типом: " + longLifeResourceArchive.getType(), request);

            return new ResponseEntity<>(createSuccessResponse(action), HttpStatus.ACCEPTED);
        } else {
            throw new ParameterValidationException("Вечный архив не найден.");
        }
    }

    private void assertAccountIsActive(PersonalAccount account) {
        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт не активен");
        }
    }
}
