package ru.majordomo.hms.personmgr.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

import ru.majordomo.hms.personmgr.event.accountAbonement.AccountAbonementImportEvent;
import ru.majordomo.hms.personmgr.event.accountComment.AccountCommentImportEvent;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryImportEvent;
import ru.majordomo.hms.personmgr.event.accountOwner.AccountOwnerImportEvent;
import ru.majordomo.hms.personmgr.event.accountPromocode.AccountPromocodeCleanEvent;
import ru.majordomo.hms.personmgr.event.accountPromocode.AccountPromocodeImportEvent;
import ru.majordomo.hms.personmgr.event.accountPromotion.AccountPromotionImportEvent;
import ru.majordomo.hms.personmgr.event.accountService.AccountServiceImportEvent;
import ru.majordomo.hms.personmgr.event.personalAccount.PersonalAccountImportEvent;
import ru.majordomo.hms.personmgr.event.personalAccount.PersonalAccountNotificationImportEvent;
import ru.majordomo.hms.personmgr.event.promocode.PromocodeCleanEvent;
import ru.majordomo.hms.personmgr.event.promocode.PromocodeImportEvent;

@Service
public class DBImportService {
    private final static Logger logger = LoggerFactory.getLogger(DBImportService.class);

    private final BusinessActionDBSeedService businessActionDBSeedService;
    private final AccountHistoryDBImportService accountHistoryDBImportService;
    private final NotificationDBImportService notificationDBImportService;
    private final AccountNotificationDBImportService accountNotificationDBImportService;
    private final PersonalAccountDBImportService personalAccountDBImportService;
    private final PlanDBImportService planDBImportService;
    private final PromocodeActionDBSeedService promocodeActionDBSeedService;
    private final PromocodeDBImportService promocodeDBImportService;
    private final AccountPromocodeDBImportService accountPromocodeDBImportService;
    private final BonusPromocodeDBImportService bonusPromocodeDBImportService;
    private final DomainTldDBImportService domainTldDBImportService;
    private final SeoServiceDBSeedService seoServiceDBSeedService;
    private final AccountAbonementDBImportService accountAbonementDBImportService;
    private final ServiceDBImportService serviceDBImportService;
    private final AccountServicesDBImportService accountServicesDBImportService;
    private final AccountCommentDBImportService accountCommentDBImportService;
    private final PromotionDBSeedService promotionDBSeedService;
    private final AccountPromotionDBImportService accountPromotionDBImportService;
    private final AccountOwnerFromPersonDBImportService accountOwnerFromPersonDBImportService;
    private final AccountOwnerDBImportService accountOwnerDBImportService;
    private final AccountSelectorService accountSelectorService;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public DBImportService(
            AccountNotificationDBImportService accountNotificationDBImportService,
            BusinessActionDBSeedService businessActionDBSeedService,
            AccountHistoryDBImportService accountHistoryDBImportService,
            AccountServicesDBImportService accountServicesDBImportService,
            AccountAbonementDBImportService accountAbonementDBImportService,
            ServiceDBImportService serviceDBImportService,
            NotificationDBImportService notificationDBImportService,
            PersonalAccountDBImportService personalAccountDBImportService,
            PlanDBImportService planDBImportService,
            PromocodeActionDBSeedService promocodeActionDBSeedService,
            PromocodeDBImportService promocodeDBImportService,
            AccountPromocodeDBImportService accountPromocodeDBImportService,
            SeoServiceDBSeedService seoServiceDBSeedService,
            BonusPromocodeDBImportService bonusPromocodeDBImportService,
            DomainTldDBImportService domainTldDBImportService,
            AccountCommentDBImportService accountCommentDBImportService,
            PromotionDBSeedService promotionDBSeedService,
            AccountPromotionDBImportService accountPromotionDBImportService,
            AccountOwnerFromPersonDBImportService accountOwnerFromPersonDBImportService,
            AccountOwnerDBImportService accountOwnerDBImportService,
            AccountSelectorService accountSelectorService,
            ApplicationEventPublisher publisher
    ) {
        this.accountNotificationDBImportService = accountNotificationDBImportService;
        this.businessActionDBSeedService = businessActionDBSeedService;
        this.accountHistoryDBImportService = accountHistoryDBImportService;
        this.accountServicesDBImportService = accountServicesDBImportService;
        this.accountAbonementDBImportService = accountAbonementDBImportService;
        this.serviceDBImportService = serviceDBImportService;
        this.notificationDBImportService = notificationDBImportService;
        this.personalAccountDBImportService = personalAccountDBImportService;
        this.planDBImportService = planDBImportService;
        this.promocodeActionDBSeedService = promocodeActionDBSeedService;
        this.promocodeDBImportService = promocodeDBImportService;
        this.accountPromocodeDBImportService = accountPromocodeDBImportService;
        this.seoServiceDBSeedService = seoServiceDBSeedService;
        this.bonusPromocodeDBImportService = bonusPromocodeDBImportService;
        this.domainTldDBImportService = domainTldDBImportService;
        this.accountCommentDBImportService = accountCommentDBImportService;
        this.promotionDBSeedService = promotionDBSeedService;
        this.accountPromotionDBImportService = accountPromotionDBImportService;
        this.accountOwnerFromPersonDBImportService = accountOwnerFromPersonDBImportService;
        this.accountOwnerDBImportService = accountOwnerDBImportService;
        this.accountSelectorService = accountSelectorService;
        this.publisher = publisher;
    }

    public boolean seedDB() {
        boolean seeded;

        return true;
    }

    public boolean importToMongo() {
        boolean imported;

//        imported = businessActionDBSeedService.seedDB();
//        logger.debug(imported ? "businessFlow db_seeded" : "businessFlow db_not_seeded");
//
//        imported = promocodeActionDBSeedService.seedDB();
//        logger.debug(imported ? "promocodeAction db_seeded" : "promocodeAction db_not_seeded");
//
//        imported = promotionDBSeedService.seedDB();
//        logger.debug(imported ? "promotion db_seeded" : "promotion db_not_seeded");
//
//        imported = accountHistoryDBImportService.importToMongo();
//        logger.debug(imported ? "accountHistory db_imported" : "accountHistory db_not_imported");
//
//        imported = notificationDBImportService.importToMongo();
//        logger.debug(imported ? "notification db_imported" : "notification db_not_imported");
//
//        imported = accountNotificationDBImportService.importToMongo();
//        logger.debug(imported ? "accountNotification db_imported" : "accountNotification db_not_imported");
//
//        imported = serviceDBImportService.importToMongo(); //#1
//        logger.debug(imported ? "service db_imported" : "service db_not_imported");
//
//        imported = seoServiceDBSeedService.seedDB();
//        logger.debug(imported ? "seo db_seeded" : "seo db_not_seeded");
//
//        imported = planDBImportService.importToMongo();
//        logger.debug(imported ? "plan db_imported" : "plan db_not_imported");
//
//        imported = personalAccountDBImportService.importToMongo();
//        logger.debug(imported ? "personalAccount db_imported" : "personalAccount db_not_imported");
//
//        imported = accountServicesDBImportService.importToMongo();
//        logger.debug(imported ? "accountServices db_imported" : "accountServices db_not_imported");
//
//        imported = promocodeDBImportService.importToMongo();
//        logger.debug(imported ? "promocode db_imported" : "promocode db_not_imported");
//
//        imported = accountPromocodeDBImportService.importToMongo();
//        logger.debug(imported ? "accountPromocode db_imported" : "accountPromocode db_not_imported");
//
//        imported = bonusPromocodeDBImportService.importToMongo();
//        logger.debug(imported ? "bonusPromocode db_imported" : "bonusPromocode db_not_imported");
//
//        imported = domainTldDBImportService.importToMongo();
//        logger.debug(imported ? "domainTldD db_imported" : "domainTldD db_not_imported");
//
//        imported = accountDomainDBImportService.importToMongo();
//        logger.debug(imported ? "accountDomain db_imported" : "accountDomain db_not_imported");
//
//        imported = accountAbonementDBImportService.importToMongo();
//        logger.debug(imported ? "accountAbonement db_imported" : "accountAbonement db_not_imported");
//
//        imported = accountCommentDBImportService.importToMongo();
//        logger.debug(imported ? "accountComment db_imported" : "accountComment db_not_imported");
//
//        imported = accountPromotionDBImportService.importToMongo();
//        logger.debug(imported ? "accountPromotion db_imported" : "accountPromotion db_not_imported");
//
//        imported = accountOwnerDBImportService.importToMongo();
//        logger.debug(imported ? "accountOwner db_imported" : "accountOwner db_not_imported");

        return true;
    }

    public boolean importToMongo(String accountId) {
        boolean imported;

//        publisher.publishEvent(new PromocodeImportEvent(accountId));
//        imported = promocodeDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "promocode db_imported" : "promocode db_not_imported");

//        publisher.publishEvent(new PersonalAccountImportEvent(accountId));
//        imported = personalAccountDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "personalAccount db_imported" : "personalAccount db_not_imported");

//        publisher.publishEvent(new PersonalAccountNotificationImportEvent(accountId));
//        imported = accountNotificationDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "accountNotification db_imported" : "accountNotification db_not_imported");

//        publisher.publishEvent(new AccountServiceImportEvent(accountId));
//        imported = accountServicesDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "accountServices db_imported" : "accountServices db_not_imported");

//        publisher.publishEvent(new AccountHistoryImportEvent(accountId));
//        imported = accountHistoryDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "accountHistory db_imported" : "accountHistory db_not_imported");

//        publisher.publishEvent(new AccountCommentImportEvent(accountId));
//        imported = accountCommentDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "accountComment db_imported" : "accountComment db_not_imported");

//        publisher.publishEvent(new AccountPromocodeImportEvent(accountId));
//        imported = accountPromocodeDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "accountPromocode db_imported" : "accountPromocode db_not_imported");

//        publisher.publishEvent(new AccountAbonementImportEvent(accountId));
//        imported = accountAbonementDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "accountAbonement db_imported" : "accountAbonement db_not_imported");

//        publisher.publishEvent(new AccountOwnerImportEvent(accountId));
//        imported = accountOwnerDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "accountOwner db_imported" : "accountOwner db_not_imported");

//        publisher.publishEvent(new AccountPromotionImportEvent(accountId));
//        imported = accountPromotionDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "accountPromotion db_imported" : "accountPromotion db_not_imported");

        return true;
    }

    public void clean(String accountId) {
        publisher.publishEvent(new PromocodeCleanEvent(accountId));

        accountHistoryDBImportService.clean(accountId);
        accountCommentDBImportService.clean(accountId);
        accountServicesDBImportService.clean(accountId);

        publisher.publishEvent(new AccountPromocodeCleanEvent(accountId));

        accountAbonementDBImportService.clean(accountId);
        accountOwnerDBImportService.clean(accountId);

        personalAccountDBImportService.clean(accountId);
    }

    public boolean importToMongoByServerId(String serverId) {
        List<String> accountIds = accountSelectorService.selectAccountIdsByServerId(serverId);

        accountIds.forEach(this::importToMongo);

        return true;
    }

    public boolean cleanByServerId(String serverId) {
        List<String> accountIds = accountSelectorService.selectAccountIdsByServerId(serverId);

        accountIds.forEach(this::clean);

        return true;
    }
}
