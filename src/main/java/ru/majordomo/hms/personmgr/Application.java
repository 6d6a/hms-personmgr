package ru.majordomo.hms.personmgr;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageImpl;
import org.springframework.retry.annotation.EnableRetry;

import ru.majordomo.hms.personmgr.serializer.PageSerializer;
import ru.majordomo.hms.personmgr.importing.DBImportService;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableCaching
@EnableRetry
public class Application implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private DBImportService dbImportService;

    @Autowired(required = false)
    public void setDbImportService(DBImportService dbImportService) {
        this.dbImportService = dbImportService;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    public void run(String... args) {
        if (dbImportService != null) {
            String dbSeedOption = "--db_seed";
            String dbImportOption = "--db_import";
            String dbImportOneAccountOption = "--db_import_one_account";
            String dbImportOneServerOption = "--db_import_one_server";
            String dbCleanOneServerOption = "--db_clean_one_server";
            String processOption = "--process";
            StringBuilder sb = new StringBuilder();

            String serverId = "122";
//            String serverId = "136";

            for (String option : args) {
                sb.append(" ").append(option);

                if (option.equals(dbSeedOption)) {
                    boolean seeded;

                    seeded = dbImportService.seedDB();
                    sb.append(" ").append(seeded ? "dbImportService db_seeded" : "dbImportService db_not_seeded");
                } else if (option.equals(dbImportOption)) {
                    boolean imported;

                    imported = dbImportService.importToMongo();
                    sb.append(" ").append(imported ? "dbImportService db_imported" : "dbImportService db_not_imported");
                } else if (option.equals(dbImportOneAccountOption)) {
                    boolean imported;

                    imported = dbImportService.importToMongo("100800");
                    sb.append(" ").append(imported ? "dbImportService db_imported" : "dbImportService db_not_imported");
                } else if (option.equals(dbImportOneServerOption)) {
                    boolean imported;

                    imported = dbImportService.importToMongoByServerId(serverId);
                    sb.append(" ").append(imported ? "dbImportService db_imported" : "dbImportService db_not_imported");
                } else if (option.equals(dbCleanOneServerOption)) {
                    boolean imported;

                    imported = dbImportService.cleanByServerId(serverId);
                    sb.append(" ").append(imported ? "dbImportService db_cleaned" : "dbImportService db_not_cleaned");
                } else if (option.equals(processOption)) {
                    //                PersonalAccount account = personalAccountRepository.findByAccountId("100800");
                    //                domainService.processDomainsAutoRenewByAccount(account);
                    //                paymentChargesProcessorService.processCharges("ac_100800");
                    //                monthlyBillService.processMonthlyBill("ac_179219", LocalDate.of(2016, 11, 17));
                }
            }
            sb = sb.length() == 0 ? sb.append("No Options Specified") : sb;
            logger.info(String.format("Launched personal manager with following options: %s", sb.toString()));
        } else {
            logger.info("Launched personal manager without dbImportService Autowired");
        }
    }

    @Bean
    public Module jacksonPageWithJsonViewModule() {
        SimpleModule module = new SimpleModule("jackson-page-with-jsonview", Version.unknownVersion());
        module.addSerializer(PageImpl.class, new PageSerializer());
        return module;
    }
}
