package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.StorageType;
import ru.majordomo.hms.personmgr.event.account.AccountCheckQuotaEvent;
import ru.majordomo.hms.personmgr.event.account.AntiSpamServiceSwitchEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.ServicePlan;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequestService;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.RedirectAccountService;
import ru.majordomo.hms.personmgr.repository.AccountRedirectServiceRepository;
import ru.majordomo.hms.personmgr.repository.RevisiumRequestServiceRepository;
import ru.majordomo.hms.personmgr.service.*;
import ru.majordomo.hms.personmgr.service.restic.Snapshot;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/{accountId}/account-service-abonement")
@Validated
public class AccountServiceAbonementRestController extends CommonRestController {

    private final AccountServiceHelper accountServiceHelper;
    private final AbonementManager<AccountServiceAbonement> accountServiceAbonementManager;
    private final AccountNotificationHelper accountNotificationHelper;
    private final ServiceAbonementService serviceAbonementService;
    private final AccountRedirectServiceRepository redirectServiceRepository;
    private final RevisiumRequestServiceRepository revisiumRequestServiceRepository;
    private final BackupService backupService;
    private final ResourceHelper resourceHelper;
    private final PlanManager planManager;
    private final AccountHelper accountHelper;

    @Autowired
    public AccountServiceAbonementRestController(
            AccountServiceHelper accountServiceHelper,
            AbonementManager<AccountServiceAbonement> accountServiceAbonementManager,
            AccountNotificationHelper accountNotificationHelper,
            ServiceAbonementService serviceAbonementService,
            AccountRedirectServiceRepository redirectServiceRepository,
            RevisiumRequestServiceRepository revisiumRequestServiceRepository,
            BackupService backupService,
            ResourceHelper resourceHelper,
            PlanManager planManager,
            AccountHelper accountHelper
    ) {
        this.accountServiceHelper = accountServiceHelper;
        this.accountNotificationHelper = accountNotificationHelper;
        this.serviceAbonementService = serviceAbonementService;
        this.accountServiceAbonementManager = accountServiceAbonementManager;
        this.redirectServiceRepository = redirectServiceRepository;
        this.revisiumRequestServiceRepository = revisiumRequestServiceRepository;
        this.backupService = backupService;
        this.resourceHelper = resourceHelper;
        this.planManager = planManager;
        this.accountHelper = accountHelper;
    }

    @GetMapping
    public ResponseEntity<Page<AccountServiceAbonement>> getAllServiceAbonements(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            Pageable pageable
    ) {
        Page<AccountServiceAbonement> accountServices = serviceAbonementRepository.findByPersonalAccountId(accountId, pageable);

        return new ResponseEntity<>(accountServices, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('MANAGE_SERVICES')")
    @DeleteMapping("/{abonementId}")
    public ResponseEntity<AccountServiceAbonement> del(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountServiceAbonement.class) @PathVariable(value = "abonementId") String abonementId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        AccountServiceAbonement abonement = accountServiceAbonementManager.findByIdAndPersonalAccountId(abonementId, accountId);
        PersonalAccount account = accountManager.findOne(accountId);

        if (abonement == null) {
            throw new ResourceNotFoundException("?????????????????? ???? ????????????");
        }

        String message = "???????????? ?????????????????? ???? ????????????: " + abonement.getAbonement().getName();

        switch (abonement.getAbonement().getType()) {
            case REDIRECT:
                RedirectAccountService redirect = redirectServiceRepository.findByAccountServiceAbonementId(abonementId);
                resourceHelper.deleteRedirects(account, redirect.getFullDomainName());
                redirectServiceRepository.delete(redirect);
                message += " ??????????: " +  redirect.getFullDomainName();

                break;
            case ANTI_SPAM:
                publisher.publishEvent(new AntiSpamServiceSwitchEvent(account.getId(), false));

                break;
            case REVISIUM:
                RevisiumRequestService revisiumService = revisiumRequestServiceRepository.findByPersonalAccountIdAndAccountServiceAbonementId(
                        accountId, abonementId
                );

                revisiumRequestServiceRepository.delete(revisiumService);

                message += " ??????????: " + revisiumService.getSiteUrl();

                break;
            case VIRTUAL_HOSTING_PLAN:
                throw new ParameterValidationException(
                        "???????????????????? ?? ?????????? ????????????????????! ?????????????????? ???? ?????????????? ?????????????????? ?????????? ?????????????????????? ???? ??????. ????????????."
                );
        }

        accountServiceAbonementManager.delete(abonement.getId());

        history.save(accountId, message, request);

        return new ResponseEntity<>(abonement, HttpStatus.OK);
    }

    @PostMapping("/{abonementId}")
    public ResponseEntity<AccountServiceAbonement> setAutoRenew(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "abonementId") String abonementId,
            @RequestBody boolean autorenew,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        AccountServiceAbonement abonement = accountServiceAbonementManager.findByIdAndPersonalAccountId(abonementId, accountId);
        if (abonement == null) {
            throw new ResourceNotFoundException("?????????????????? ???? ???????????? ???? ????????????");
        }

        abonement.setAutorenew(autorenew);

        accountServiceAbonementManager.setAutorenew(abonementId, autorenew);

        logger.info("accountId " + accountId + " abonementId " + abonementId + " set autorenew " + autorenew);

        history.save(
                accountId,
                "?????????????????????????? ???????????????????? ?? id " + abonementId + " " + abonement.getAbonement().getName()
                        + (autorenew ? " ????????????????" : " ??????????????????"),
                request
        );

        return new ResponseEntity<>(abonement, HttpStatus.OK);
    }


    @GetMapping("/filter")
    public ResponseEntity<List<AccountServiceAbonement>> getAbonementService(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestParam(value = "feature") Feature feature
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        ServicePlan plan = accountServiceHelper.getServicePlanForFeatureByAccount(feature, account);

        if (plan == null) {
            throw new ParameterValidationException("???????????? " + feature.name() + " ???? ??????????????");
        }

        List<AccountServiceAbonement> accountServiceAbonements = serviceAbonementRepository.findByPersonalAccountIdAndAbonementIdIn(
                account.getId(),
                plan.getAbonementIds()
        );

        if (accountServiceAbonements == null || accountServiceAbonements.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else if (feature.isOnlyOnePerAccount() && accountServiceAbonements.size() > 1) {
            throw new ParameterValidationException(
                    "???? ???????????????? ???????????????????? ???????????? ???????????? ???????????????????? ???? ???????????? '" + plan.getService().getName()
                            + "'. ????????????????????, ???????????????????? ?? ???????????????????? ??????????.");}

        return new ResponseEntity<>(accountServiceAbonements, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<AccountServiceAbonement> addServiceAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestParam(value = "feature") Feature feature,
            @RequestParam(value = "abonementId", required = false) String abonementId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        if (account.isFreeze()) {
            throw new ParameterValidationException("?????????????? ??????????????????. ?????????? ???????????????????? ????????????????????.");
        }

        accountHelper.checkIsAdditionalServiceAllowed(account, feature);

        ServicePlan servicePlan = accountServiceHelper.getServicePlanForFeatureByAccount(feature, account);

        if (servicePlan == null) {
            throw new ParameterValidationException("???????????? " + feature.name() + " ???? ??????????????");
        }

        if (feature == Feature.SMS_NOTIFICATIONS) {
            accountNotificationHelper.checkSmsAllowness(account);
        }

        if (feature == Feature.ADVANCED_BACKUP_INSTANT_ACCESS) {
            List<Snapshot> snapshots = backupService.getFileSnapshots(account);
            LocalDate minTimeForBackup = backupService.minDateForBackup(account, StorageType.FILE);

            if (minTimeForBackup.isEqual(LocalDate.now().minusDays(30))) {
                throw new ParameterValidationException("?????? ?????????????????? ?????????? ????????????????");
            }

            List<Snapshot> filtered = snapshots
                    .stream()
                    .filter(item -> !item.getTime().toLocalDate().isAfter(minTimeForBackup))
                    .collect(Collectors.toList());

            if (filtered.isEmpty()) {
                throw new ParameterValidationException("???????????????????????????? ?????????????????? ?????????? ???? ??????????????");
            }
        }

        if (servicePlan.isForSomePlan()) {
            Plan accountPlan = planManager.findOne(account.getPlanId());
            if (!accountPlan.getAllowedFeature().contains(feature)) {
                throw new ParameterValidationException(String.format(
                        "???????????? '%s' ???????????????????? ?????? ?????????????????? ?????????? '%s'",
                        servicePlan.getName(),
                        accountPlan.getName()
                ));
            }
        }

        if (abonementId != null && (!servicePlan.getAbonementIds().contains(abonementId) || servicePlan.getAbonementById(abonementId).isInternal())) {
            throw new ParameterValidationException("?????????????????? ???? ???????????? " + feature.name() + " ???? ????????????");
        }

        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(
                account.getId(),
                servicePlan.getServiceId()
        );

        if (accountServices != null && feature.isOnlyOnePerAccount() && accountServices.size() > 1) {
            throw new ParameterValidationException(
                    "???? ???????????????? ???????????????????? ???????????? ?????????? ???????????? '" + servicePlan.getService().getName()
                            + "'. ????????????????????, ???????????????????? ?? ???????????????????? ??????????.");
        }

        Boolean autorenew = feature != Feature.ADVANCED_BACKUP_INSTANT_ACCESS;

        AccountServiceAbonement accountServiceAbonement = serviceAbonementService.addAbonement(
                account,
                abonementId != null ? abonementId : servicePlan.getNotInternalAbonementId(),
                feature,
                autorenew
        );

        return new ResponseEntity<>(accountServiceAbonement, HttpStatus.OK);
    }
}