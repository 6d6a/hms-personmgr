package ru.majordomo.hms.personmgr.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.ResourceArchiveCreatedSendMailEvent;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.ServicePlan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.LongLifeResourceArchive;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.LongLifeResourceArchiveRepository;
import ru.majordomo.hms.personmgr.repository.ServicePlanRepository;
import ru.majordomo.hms.rc.user.resources.ResourceArchive;
import ru.majordomo.hms.rc.user.resources.ResourceArchiveType;

import static ru.majordomo.hms.personmgr.common.Constants.ARCHIVED_RESOURCE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.LONG_LIFE;
import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_TYPE;

@Service
public class ResourceArchiveService {
    private final LongLifeResourceArchiveRepository repository;
    private final ServicePlanRepository servicePlanRepository;
    private final PersonalAccountManager personalAccountManager;
    private final RcUserFeignClient rcUserFeignClient;
    private final BusinessHelper businessHelper;
    private final ApplicationEventPublisher publisher;
    private final AccountServiceRepository accountServiceRepository;

    public ResourceArchiveService(
            LongLifeResourceArchiveRepository repository,
            ServicePlanRepository servicePlanRepository,
            PersonalAccountManager personalAccountManager,
            RcUserFeignClient rcUserFeignClient,
            BusinessHelper businessHelper,
            ApplicationEventPublisher publisher,
            AccountServiceRepository accountServiceRepository
    ) {
        this.repository = repository;
        this.servicePlanRepository = servicePlanRepository;
        this.personalAccountManager = personalAccountManager;
        this.rcUserFeignClient = rcUserFeignClient;
        this.businessHelper = businessHelper;
        this.publisher = publisher;
        this.accountServiceRepository = accountServiceRepository;
    }

    public void createFromProcessingBusinessOperation(ProcessingBusinessOperation businessOperation) {
        String resourceArchiveId = (String) businessOperation.getParam(RESOURCE_ID_KEY);
        String archivedResourceId = (String) businessOperation.getParam(ARCHIVED_RESOURCE_ID_KEY);
        ResourceArchiveType resourceArchiveType = ResourceArchiveType.valueOf((String) businessOperation.getParam(RESOURCE_TYPE));

        ServicePlan plan = servicePlanRepository.findOneByFeatureAndActive(Feature.LONG_LIFE_RESOURCE_ARCHIVE, true);

        PersonalAccount account = personalAccountManager.findOne(businessOperation.getPersonalAccountId());

        AccountService accountService = new AccountService();
        accountService.setPersonalAccountId(account.getId());
        accountService.setServiceId(plan.getServiceId());
        accountService.setLastBilled(LocalDateTime.now().plusHours(24));

        accountServiceRepository.save(accountService);

        try {
            LongLifeResourceArchive longLifeResourceArchive = new LongLifeResourceArchive();
            longLifeResourceArchive.setCreated(LocalDateTime.now());
            longLifeResourceArchive.setPersonalAccountId(businessOperation.getPersonalAccountId());
            longLifeResourceArchive.setResourceArchiveId(resourceArchiveId);
            longLifeResourceArchive.setArchivedResourceId(archivedResourceId);
            longLifeResourceArchive.setType(resourceArchiveType);
            longLifeResourceArchive.setAccountServiceId(accountService.getId());

            repository.save(longLifeResourceArchive);
        } catch (Exception e) {
            e.printStackTrace();
            accountServiceRepository.delete(accountService);
        }
    }

    public void processAccountServiceDelete(AccountService accountService) {
        LongLifeResourceArchive longLifeResourceArchive = repository.findByAccountServiceId(accountService.getId());
        longLifeResourceArchive.setAccountServiceId(null);
        longLifeResourceArchive.setAccountServiceDeleted(LocalDateTime.now());

        repository.save(longLifeResourceArchive);
    }

    public void deleteLongLifeResourceArchiveAndAccountService(ProcessingBusinessOperation businessOperation) {
        String resourceArchiveId = (String) businessOperation.getParam(RESOURCE_ID_KEY);

        LongLifeResourceArchive longLifeResourceArchive = repository.findByPersonalAccountIdAndResourceArchiveId(
                businessOperation.getPersonalAccountId(),
                resourceArchiveId
        );

        if (longLifeResourceArchive != null && longLifeResourceArchive.getAccountServiceId() != null) {
            accountServiceRepository.deleteById(longLifeResourceArchive.getAccountServiceId());

            repository.delete(longLifeResourceArchive);
        }
    }

    public void deleteLongLifeResourceArchiveAndAccountService(String personalAccountId, ResourceArchiveType type, String archivedResourceId) {
        List<LongLifeResourceArchive> longLifeResourceArchives = repository.findByPersonalAccountIdAndTypeAndArchivedResourceId(personalAccountId, type, archivedResourceId);

        if (longLifeResourceArchives != null && !longLifeResourceArchives.isEmpty()) {
            longLifeResourceArchives.forEach(longLifeResourceArchive -> {
                accountServiceRepository.deleteById(longLifeResourceArchive.getAccountServiceId());

                repository.delete(longLifeResourceArchive);
            });
        }
    }

    public void notifyByProcessingBusinessOperation(ProcessingBusinessOperation businessOperation) {
        String archivedResourceId = (String) businessOperation.getParam(ARCHIVED_RESOURCE_ID_KEY);
        String resourceArchiveId = (String) businessOperation.getParam(RESOURCE_ID_KEY);
        ResourceArchiveType resourceArchiveType = ResourceArchiveType.valueOf((String) businessOperation.getParam(RESOURCE_TYPE));

        publisher.publishEvent(new ResourceArchiveCreatedSendMailEvent(
                businessOperation.getPersonalAccountId(),
                archivedResourceId,
                resourceArchiveId,
                resourceArchiveType
        ));
    }

    public void processResourceArchiveClean(ResourceArchive resourceArchive) {
        LongLifeResourceArchive longLifeResourceArchive = repository.findByResourceArchiveId(resourceArchive.getId());

        if (longLifeResourceArchive == null
                && resourceArchive.getCreatedAt().isBefore(LocalDateTime.now().minusDays(1))) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setAccountId(resourceArchive.getAccountId());
            message.addParam(RESOURCE_ID_KEY, resourceArchive.getId());
            message.addParam("switchedOn", false);
            message.addParam("willBeDeletedAfter", resourceArchive.getCreatedAt().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            businessHelper.buildActionAndOperation(
                    BusinessOperationType.RESOURCE_ARCHIVE_UPDATE,
                    BusinessActionType.RESOURCE_ARCHIVE_UPDATE_RC,
                    message
            );
        } else if (longLifeResourceArchive != null
                && longLifeResourceArchive.getAccountServiceDeleted() != null
                && longLifeResourceArchive.getAccountServiceDeleted().isBefore(LocalDateTime.now())) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setAccountId(resourceArchive.getAccountId());
            message.addParam(RESOURCE_ID_KEY, resourceArchive.getId());
            message.addParam(LONG_LIFE, true);
            message.addParam("switchedOn", false);
            message.addParam("willBeDeletedAfter", LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            businessHelper.buildActionAndOperation(
                    BusinessOperationType.RESOURCE_ARCHIVE_UPDATE,
                    BusinessActionType.RESOURCE_ARCHIVE_UPDATE_RC,
                    message
            );
        }
    }
}
