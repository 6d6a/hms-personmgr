package ru.majordomo.hms.personmgr.controller.rest;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.dto.domainTransfer.DomainTransferConfirmation;
import ru.majordomo.hms.personmgr.dto.domainTransfer.DomainTransferRequest;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.DomainInTransferManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.domain.DomainInTransfer;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.ServicePlan;
import ru.majordomo.hms.personmgr.repository.ServicePlanRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.DomainService;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/{accountId}/domain-transfer")
@AllArgsConstructor
public class DomainTransferRestController {
    private final PersonalAccountManager personalAccountManager;
    private final AccountHelper accountHelper;
    private final ServicePlanRepository servicePlanRepository;
    private final DomainInTransferManager domainInTransferManager;
    private final DomainService domainService;

    @GetMapping
    public ResponseEntity<List<DomainInTransfer>> listAll(@PathVariable String accountId) {
        List<DomainInTransfer> domains = domainInTransferManager.findAllByAccountId(accountId);
        return ResponseEntity.ok(domains);
    }

    @GetMapping("/active")
    public boolean existActive(@PathVariable String accountId) {
        DomainInTransfer domainInTransfer = domainInTransferManager.findNeedToProcessByAccountId(accountId);
        return domainInTransfer != null;
    }

    @PostMapping("/request")
    public ResponseEntity<DomainInTransfer> transferRequest(
            @PathVariable String accountId,
            @Valid @RequestBody DomainTransferRequest transferRequest
    ) {
        checkRestrictions(accountId);

        String authInfo = transferRequest.getAuthInfo();
        String domainName = transferRequest.getDomainName().toLowerCase();
        String personId = transferRequest.getPersonId();

        domainService.checkForTransfer(domainName, accountId);

        //Делаем запрос на трансфер домена к нам
        //Результат - событие TRANSFER_GET_EMAIL в reg-rpc
        DomainInTransfer domainInTransfer = domainService.requestDomainTransfer(accountId, personId, domainName, authInfo);

        return new ResponseEntity<>(domainInTransfer, HttpStatus.CREATED);
    }

    @PostMapping("/confirmation")
    public ResponseEntity<DomainInTransfer> confirmCode(
            @PathVariable String accountId,
            @Valid @RequestBody DomainTransferConfirmation transferConfirmation
    ) {
        checkRestrictions(accountId);

        //Подтверждаем трансфер по верификационному коду из email
        //Результат - событие TRANSFER_FROM в reg-rpc и домен в rc-user
        DomainInTransfer domain = domainService.confirmDomainTransfer(accountId, transferConfirmation.getVerificationCode());

        return new ResponseEntity<>(domain, HttpStatus.OK);
    }

    @PatchMapping()
    public ResponseEntity<HttpStatus> markTransferAsCancelled(@PathVariable String accountId) {
        DomainInTransfer domainInTransfer = domainInTransferManager.findNeedToProcessByAccountId(accountId);
        if (domainInTransfer != null) {
            domainInTransfer.setState(DomainInTransfer.State.CANCELLED);
            domainInTransferManager.save(domainInTransfer);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Проверка ограничений на заказ услуги трансфера домена к нам
     *
     * @param accountId Идентификатор аккаунта
     */
    private void checkRestrictions(String accountId) {
        PersonalAccount account = personalAccountManager.findByAccountId(accountId);
        if (account == null) {
            throw new ParameterValidationException("Аккаунт не найден");
        }

        if (!account.isActive() || account.isPreorder()) {
            throw new ParameterValidationException("Аккаунт неактивен. Перенос домена невозможен.");
        }

        if (account.isFreeze()) {
            throw new ParameterValidationException("Аккаунт заморожен. Перенос домена невозможен.");
        }

        ServicePlan servicePlan = servicePlanRepository.findOneByFeatureAndActive(Feature.DOMAIN_TRANSFER_RU_RF, true);
        if (servicePlan == null) {
            throw new ParameterValidationException("Услуга не найдена");
        }

        accountHelper.checkIsAdditionalServiceAllowed(account, Feature.DOMAIN_TRANSFER_RU_RF);
    }
}
