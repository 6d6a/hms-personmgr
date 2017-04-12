package ru.majordomo.hms.personmgr.service.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private final AccountDomainDBImportService accountDomainDBImportService;
    private final SeoServiceDBSeedService seoServiceDBSeedService;
    private final AccountAbonementDBImportService accountAbonementDBImportService;
    private final ServiceDBImportService serviceDBImportService;
    private final AccountServicesDBImportService accountServicesDBImportService;
    private final AccountCommentDBImportService accountCommentDBImportService;
    private final PromotionDBSeedService promotionDBSeedService;
    private final AccountPromotionDBImportService accountPromotionDBImportService;

    @Autowired
    public DBImportService(
            AccountNotificationDBImportService accountNotificationDBImportService,
            BusinessActionDBSeedService businessActionDBSeedService,
            AccountHistoryDBImportService accountHistoryDBImportService,
            AccountServicesDBImportService accountServicesDBImportService,
            AccountDomainDBImportService accountDomainDBImportService,
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
            AccountPromotionDBImportService accountPromotionDBImportService
    ) {
        this.accountNotificationDBImportService = accountNotificationDBImportService;
        this.businessActionDBSeedService = businessActionDBSeedService;
        this.accountHistoryDBImportService = accountHistoryDBImportService;
        this.accountServicesDBImportService = accountServicesDBImportService;
        this.accountDomainDBImportService = accountDomainDBImportService;
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
    }

    public boolean seedDB() {
        boolean seeded;

//        seeded = businessActionDBSeedService.seedDB();
//        logger.debug(seeded ? "businessFlow db_seeded" : "businessFlow db_not_seeded");
//
//        seeded = promocodeActionDBSeedService.seedDB();
//        logger.debug(seeded ? "promocodeAction db_seeded" : "promocodeAction db_not_seeded");
//
//        seeded = seoServiceDBSeedService.seedDB();
//        logger.debug(seeded ? "seo db_seeded" : "seo db_not_seeded");

//        seeded = presentDBSeedService.seedDB();
//        logger.debug(seeded ? "promotion db_seeded" : "promotion db_not_seeded");

        return true;
    }

    public boolean importToMongo() {
        boolean imported;

//        imported = accountHistoryDBImportService.importToMongo();
//        logger.debug(imported ? "accountHistory db_imported" : "accountHistory db_not_imported");
//
//        imported = notificationDBImportService.importToMongo();
//        logger.debug(imported ? "notification db_imported" : "notification db_not_imported");
//
//        imported = accountNotificationDBImportService.importToMongo();
//        logger.debug(imported ? "accountNotification db_imported" : "accountNotification db_not_imported");
//
//        imported = serviceDBImportService.importToMongoFix();
//        logger.debug(imported ? "service db_imported" : "service db_not_imported");
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

        return true;
    }

    public boolean importToMongo(String accountId) {
        boolean imported;

//        imported = accountNotificationDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "accountNotification db_imported" : "accountNotification db_not_imported");
//
//        imported = planDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "plan db_imported" : "plan db_not_imported");
//
//        imported = personalAccountDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "personalAccount db_imported" : "personalAccount db_not_imported");
//
//        imported = accountServicesDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "accountServices db_imported" : "accountServices db_not_imported");
//
//        imported = promocodeDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "promocode db_imported" : "promocode db_not_imported");
//
//        imported = accountPromocodeDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "accountPromocode db_imported" : "accountPromocode db_not_imported");
//
//        imported = bonusPromocodeDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "bonusPromocode db_imported" : "bonusPromocode db_not_imported");
//
//        imported = accountAbonementDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "accountAbonement db_imported" : "accountAbonement db_not_imported");
//
//        imported = accountCommentDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "accountComment db_imported" : "accountComment db_not_imported");

        return true;
    }
}
