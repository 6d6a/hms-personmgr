package ru.majordomo.hms.personmgr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import ru.majordomo.hms.personmgr.event.ProcessingBusinessActionEventListener;
import ru.majordomo.hms.personmgr.service.AccountHistoryDBImportService;
import ru.majordomo.hms.personmgr.service.BusinessActionDBSeedService;

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
                boolean seeded = businessActionDBSeedService.seedDB();
                sb.append(" ").append(seeded ? "businessFlow db_seeded" : "businessFlow db_not_seeded");
            } else if (option.equals(dbImportOption)) {
            boolean imported;

                imported = accountHistoryDBImportService.importToMongo();
                sb.append(" ").append(imported ? "accountHistory db_imported" : "accountHistory db_not_imported");
//
//                imported = plan2ServiceDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "plan2Service db_imported" : "plan2Service db_not_imported");

//                imported = paymentAccountDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "paymentAccount db_imported" : "paymentAccount db_not_imported");

//                imported = paymentDBImportService.importToMongo("100800");
//                imported = paymentDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "payment db_imported" : "payment db_not_imported");

//                boolean imported = planDbImportService.importToMongo();
//                sb.append(" ").append(imported ? "plan db_imported" : "plan db_not_imported");
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
}
