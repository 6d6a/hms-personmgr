package ru.majordomo.hms.personmgr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import ru.majordomo.hms.personmgr.event.BusinessFlowEventListener;
import ru.majordomo.hms.personmgr.service.BusinessFlowDBSeedService;

@SpringBootApplication
public class Application implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Autowired
    private BusinessFlowDBSeedService businessFlowDBSeedService;

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
                boolean seeded = businessFlowDBSeedService.seedDB();
                sb.append(" ").append(seeded ? "businessFlow db_seeded" : "businessFlow db_not_seeded");
            } //else if (option.equals(dbImportOption)) {
                //boolean imported;

//                imported = serviceDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "service db_imported" : "service db_not_imported");
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
//            }
        }
        sb = sb.length() == 0 ? sb.append("No Options Specified") : sb;
        logger.info(String.format("Launched personal manager with following options: %s", sb.toString()));
    }

    @Bean
    public BusinessFlowEventListener businessFlowEventListener() {
        return new BusinessFlowEventListener();
    }

    @Bean
    public ValidatingMongoEventListener validatingMongoEventListener() {
        return new ValidatingMongoEventListener(validator());
    }

    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }
}
