package ru.majordomo.hms.personmgr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.sql.DataSource;

import ru.majordomo.hms.personmgr.event.ProcessingBusinessActionEventListener;
import ru.majordomo.hms.personmgr.service.importing.AccountHistoryDBImportService;
import ru.majordomo.hms.personmgr.service.importing.AccountNotificationDBImportService;
import ru.majordomo.hms.personmgr.service.importing.AccountPromocodeDBImportService;
import ru.majordomo.hms.personmgr.service.importing.BonusPromocodeDBImportService;
import ru.majordomo.hms.personmgr.service.importing.BusinessActionDBSeedService;
import ru.majordomo.hms.personmgr.service.importing.NotificationDBImportService;
import ru.majordomo.hms.personmgr.service.importing.PersonalAccountDBImportService;
import ru.majordomo.hms.personmgr.service.importing.PlanDBImportService;
import ru.majordomo.hms.personmgr.service.importing.PromocodeActionDBSeedService;
import ru.majordomo.hms.personmgr.service.importing.PromocodeDBImportService;

@SpringBootApplication
@PropertySources({
        @PropertySource(name = "application", value = "classpath:application.properties"),
        @PropertySource(name = "mail_manager", value = "classpath:mail_manager.properties")
})
@EnableDiscoveryClient
public class Application implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Autowired
    private BusinessActionDBSeedService businessActionDBSeedService;

    @Autowired
    private AccountHistoryDBImportService accountHistoryDBImportService;

    @Autowired
    private NotificationDBImportService notificationDBImportService;

    @Autowired
    private AccountNotificationDBImportService accountNotificationDBImportService;

    @Autowired
    private PersonalAccountDBImportService personalAccountDBImportService;

    @Autowired
    private PlanDBImportService planDBImportService;

    @Autowired
    private PromocodeActionDBSeedService promocodeActionDBSeedService;

    @Autowired
    private PromocodeDBImportService promocodeDBImportService;

    @Autowired
    private AccountPromocodeDBImportService accountPromocodeDBImportService;

    @Autowired
    private BonusPromocodeDBImportService bonusPromocodeDBImportService;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    public void run(String... args) {
        String dbSeedOption = "--db_seed";
        String dbImportOption = "--db_import";
        StringBuilder sb = new StringBuilder();
        for (String option : args) {
            sb.append(" ").append(option);

            if (option.equals(dbSeedOption)) {
                boolean seeded;
//                seeded = businessActionDBSeedService.seedDB();
//                sb.append(" ").append(seeded ? "businessFlow db_seeded" : "businessFlow db_not_seeded");

                seeded = promocodeActionDBSeedService.seedDB();
                sb.append(" ").append(seeded ? "promocodeAction db_seeded" : "promocodeAction db_not_seeded");
            } else if (option.equals(dbImportOption)) {
            boolean imported;
//                imported = personalAccountDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "personalAccount db_imported" : "personalAccount db_not_imported");

//                imported = accountHistoryDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "accountHistory db_imported" : "accountHistory db_not_imported");

//                imported = notificationDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "notification db_imported" : "notification db_not_imported");

//                imported = accountNotificationDBImportService.importToMongo("ac_100800");
//                sb.append(" ").append(imported ? "accountNotification db_imported" : "accountNotification db_not_imported");

//                imported = planDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "plan db_imported" : "plan db_not_imported");
//                imported = promocodeDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "promocode db_imported" : "promocode db_not_imported");
//                imported = accountPromocodeDBImportService.importToMongo("137010");
//                sb.append(" ").append(imported ? "accountPromocode db_imported" : "accountPromocode db_not_imported");

                imported = bonusPromocodeDBImportService.importToMongo();
                sb.append(" ").append(imported ? "bonusPromocode db_imported" : "bonusPromocode db_not_imported");
            }
        }
        sb = sb.length() == 0 ? sb.append("No Options Specified") : sb;
        logger.info(String.format("Launched personal manager with following options: %s", sb.toString()));
    }

    @Bean
    public ProcessingBusinessActionEventListener processingBusinessActionEventListener() {
        return new ProcessingBusinessActionEventListener();
    }

    @Bean
    public ValidatingMongoEventListener validatingMongoEventListener() {
        return new ValidatingMongoEventListener(validator());
    }

    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

//    @Bean(name = "OBJECT_MAPPER_BEAN")
//    public ObjectMapper jsonObjectMapper() {
//        return Jackson2ObjectMapperBuilder.json()
//                .serializationInclusion(JsonInclude.Include.NON_NULL) // Don’t include null values
//                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) //ISODate
//                .modules(new JSR310Module())
//                .build();
//    }
}
