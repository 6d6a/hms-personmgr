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
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Feature;
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
    private final AccountHelper accountHelper;
    private final AbonementManager<AccountServiceAbonement> accountServiceAbonementManager;
    private final AccountNotificationHelper accountNotificationHelper;
    private final ServiceAbonementService serviceAbonementService;
    private final AccountRedirectServiceRepository redirectServiceRepository;
    private final RevisiumRequestServiceRepository revisiumRequestServiceRepository;
    private final BackupService backupService;

    @Autowired
    public AccountServiceAbonementRestController(
            AccountServiceHelper accountServiceHelper,
            AccountHelper accountHelper,
            AbonementManager<AccountServiceAbonement> accountServiceAbonementManager,
            AccountNotificationHelper accountNotificationHelper,
            ServiceAbonementService serviceAbonementService,
            AccountRedirectServiceRepository redirectServiceRepository,
            RevisiumRequestServiceRepository revisiumRequestServiceRepository,
            BackupService backupService
    ) {
        this.accountServiceHelper = accountServiceHelper;
        this.accountHelper = accountHelper;
        this.accountNotificationHelper = accountNotificationHelper;
        this.serviceAbonementService = serviceAbonementService;
        this.accountServiceAbonementManager = accountServiceAbonementManager;
        this.redirectServiceRepository = redirectServiceRepository;
        this.revisiumRequestServiceRepository = revisiumRequestServiceRepository;
        this.backupService = backupService;
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
            throw new ResourceNotFoundException("Абонемент не найден");
        }

        String message = "Удален абонемент на услугу: " + abonement.getAbonement().getName();

        switch (abonement.getAbonement().getType()) {
            case REDIRECT:
                RedirectAccountService redirect = redirectServiceRepository.findByAccountServiceAbonementId(abonementId);
                accountHelper.deleteRedirects(account, redirect.getFullDomainName());
                redirectServiceRepository.delete(redirect);
                message += " домен: " +  redirect.getFullDomainName();

                break;
            case ANTI_SPAM:
                accountHelper.switchAntiSpamForMailboxes(account, false);

                break;
            case REVISIUM:
                RevisiumRequestService revisiumService = revisiumRequestServiceRepository.findByPersonalAccountIdAndAccountServiceAbonementId(
                        accountId, abonementId
                );

                revisiumRequestServiceRepository.delete(revisiumService);

                message += " домен: " + revisiumService.getSiteUrl();

                break;
            case VIRTUAL_HOSTING_PLAN:
                throw new ParameterValidationException(
                        "Обратитесь в отдел разработки! Абонемент на хостинг обнаружен среди абонементов на доп. услуги."
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
            throw new ResourceNotFoundException("Абонемент на услугу не найден");
        }

        abonement.setAutorenew(autorenew);

        accountServiceAbonementManager.setAutorenew(abonementId, autorenew);

        logger.info("accountId " + accountId + " abonementId " + abonementId + " set autorenew " + autorenew);

        history.save(
                accountId,
                "Автопродление абонемента с id " + abonementId + " " + abonement.getAbonement().getName()
                        + (autorenew ? " включено" : " выключено"),
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
            throw new ParameterValidationException("Услуга " + feature.name() + " не найдена");
        }

        List<AccountServiceAbonement> accountServiceAbonements = serviceAbonementRepository.findByPersonalAccountIdAndAbonementIdIn(
                account.getId(),
                plan.getAbonementIds()
        );

        if (accountServiceAbonements == null || accountServiceAbonements.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else if (feature.isOnlyOnePerAccount() && accountServiceAbonements.size() > 1) {
            throw new ParameterValidationException(
                    "На аккаунте обнаружено больше одного абонемента на услугу '" + plan.getService().getName()
                            + "'. Пожалуйста, обратитесь в финансовый отдел.");}

        return new ResponseEntity<>(accountServiceAbonements, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<AccountServiceAbonement> addServiceAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestParam(value = "feature") Feature feature,
            @RequestParam(value = "abonementId", required = false) String abonementId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        ServicePlan plan = accountServiceHelper.getServicePlanForFeatureByAccount(feature, account);

        if (feature == Feature.SMS_NOTIFICATIONS) {
            accountNotificationHelper.checkSmsAllowness(account);
        }

        if (feature == Feature.ADVANCED_BACKUP_INSTANT_ACCESS) {
            List<Snapshot> snapshots = backupService.getFileSnapshots(account);
            LocalDate minTimeForBackup = backupService.minDateForBackup(account, StorageType.FILE);

            if (minTimeForBackup.isEqual(LocalDate.now().minusDays(30))) {
                throw new ParameterValidationException("Все резервные копии доступны");
            }

            List<Snapshot> filtered = snapshots
                    .stream()
                    .filter(item -> !item.getTime().toLocalDate().isAfter(minTimeForBackup))
                    .collect(Collectors.toList());

            if (filtered.isEmpty()) {
                throw new ParameterValidationException("Дополнительных резервных коопий не найдено");
            }
        }

        if (plan == null) {
            throw new ParameterValidationException("Услуга " + feature.name() + " не найдена");
        }

        if (abonementId != null && (!plan.getAbonementIds().contains(abonementId) || plan.getAbonementById(abonementId).isInternal())) {
            throw new ParameterValidationException("Абонемент на услугу " + feature.name() + " не найден");
        }

        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(
                account.getId(),
                plan.getServiceId()
        );

        if (accountServices != null && feature.isOnlyOnePerAccount() && accountServices.size() > 1) {
            throw new ParameterValidationException(
                    "На аккаунте обнаружено больше одной услуги '" + plan.getService().getName()
                            + "'. Пожалуйста, обратитесь в финансовый отдел.");
        }

        Boolean autorenew = feature != Feature.ADVANCED_BACKUP_INSTANT_ACCESS;

        AccountServiceAbonement accountServiceAbonement = serviceAbonementService.addAbonement(
                account,
                abonementId != null ? abonementId : plan.getNotInternalAbonementId(),
                feature,
                autorenew
        );

        return new ResponseEntity<>(accountServiceAbonement, HttpStatus.OK);
    }
}