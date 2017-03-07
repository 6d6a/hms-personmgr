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
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.serializer.PageSerializer;
import ru.majordomo.hms.personmgr.service.DomainService;
import ru.majordomo.hms.personmgr.service.importing.*;

@SpringBootApplication
@PropertySources({
        @PropertySource(name = "application", value = "classpath:application.yml"),
        @PropertySource(name = "mail_manager", value = "classpath:mail_manager.properties")
})
@EnableDiscoveryClient
@EnableFeignClients
@EnableCaching
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

    @Autowired
    private DomainTldDBImportService domainTldDBImportService;

    @Autowired
    private AccountDomainDBImportService accountDomainDBImportService;

    @Autowired
    private SeoServiceDBSeedService seoServiceDBSeedService;

    @Autowired
    private AccountAbonementDBImportService accountAbonementDBImportService;

    @Autowired
    private ServiceDBImportService serviceDBImportService;

    @Autowired
    private AccountServicesDBImportService accountServicesDBImportService;

    @Autowired
    private DomainService domainService;

    @Autowired
    private PersonalAccountRepository personalAccountRepository;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    public void run(String... args) {
        String dbSeedOption = "--db_seed";
        String dbImportOption = "--db_import";
        String processChargesOption = "--process_charges";
        StringBuilder sb = new StringBuilder();
        for (String option : args) {
            sb.append(" ").append(option);

            if (option.equals(dbSeedOption)) {
                boolean seeded;
//                seeded = businessActionDBSeedService.seedDB();
//                sb.append(" ").append(seeded ? "businessFlow db_seeded" : "businessFlow db_not_seeded");
//
//                seeded = promocodeActionDBSeedService.seedDB();
//                sb.append(" ").append(seeded ? "promocodeAction db_seeded" : "promocodeAction db_not_seeded");

//                seeded = seoServiceDBSeedService.seedDB();
//                sb.append(" ").append(seeded ? "seo db_seeded" : "seo db_not_seeded");
            } else if (option.equals(dbImportOption)) {
                boolean imported;
//                imported = accountHistoryDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "accountHistory db_imported" : "accountHistory db_not_imported");

//                imported = notificationDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "notification db_imported" : "notification db_not_imported");

//                imported = accountNotificationDBImportService.importToMongo("ac_100800");
//                sb.append(" ").append(imported ? "accountNotification db_imported" : "accountNotification db_not_imported");

//                imported = serviceDBImportService.importToMongoFix();
//                sb.append(" ").append(imported ? "service db_imported" : "service db_not_imported");

//                imported = planDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "plan db_imported" : "plan db_not_imported");

//                imported = personalAccountDBImportService.importToMongo("100800");
//                  imported = personalAccountDBImportService.importToMongo("137010");
//                imported = personalAccountDBImportService.importToMongo("188480");
//                imported = personalAccountDBImportService.importToMongo("188401");
//                imported = personalAccountDBImportService.importToMongo("188378");
//                imported = personalAccountDBImportService.importToMongo("188239");
//                imported = personalAccountDBImportService.importToMongo("188235");
//                imported = personalAccountDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "personalAccount db_imported" : "personalAccount db_not_imported");

//                imported = accountServicesDBImportService.importToMongo();
                imported = accountServicesDBImportService.importToMongo("100800");
                sb.append(" ").append(imported ? "accountServices db_imported" : "accountServices db_not_imported");

//                imported = promocodeDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "promocode db_imported" : "promocode db_not_imported");

//                imported = accountPromocodeDBImportService.importToMongo("137010");
//                sb.append(" ").append(imported ? "accountPromocode db_imported" : "accountPromocode db_not_imported");

//                imported = bonusPromocodeDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "bonusPromocode db_imported" : "bonusPromocode db_not_imported");

//                imported = domainTldDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "domainTldD db_imported" : "domainTldD db_not_imported");

//                imported = accountDomainDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "accountDomain db_imported" : "accountDomain db_not_imported");

//                  imported = accountAbonementDBImportService.importToMongo();
//                  sb.append(" ").append(imported ? "accountAbonement db_imported" : "accountAbonement db_not_imported");
            } else if (option.equals(processChargesOption)) {
//                PersonalAccount account = personalAccountRepository.findByAccountId("100800");
//                domainService.processDomainsAutoRenewByAccount(account);
//                paymentChargesProcessorService.processCharges("ac_100800");
//                monthlyBillService.processMonthlyBill("ac_179219", LocalDate.of(2016, 11, 17));
            }
        }
        sb = sb.length() == 0 ? sb.append("No Options Specified") : sb;
        logger.info(String.format("Launched personal manager with following options: %s", sb.toString()));
    }

    @Bean
    public ValidatingMongoEventListener validatingMongoEventListener() {
        return new ValidatingMongoEventListener(validator());
    }

    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();
        validatorFactoryBean.setValidationMessageSource(messageSource());
        return validatorFactoryBean;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public ReloadableResourceBundleMessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages/messages");
        return messageSource;
    }

    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        MethodValidationPostProcessor methodValidationPostProcessor = new MethodValidationPostProcessor();
        methodValidationPostProcessor.setValidator(validator());
        return methodValidationPostProcessor;
    }

    @Bean
    public MessageSourceAccessor messageSourceAccessor() {
        return new MessageSourceAccessor(messageSource());
    }

    @Bean
    public Module jacksonPageWithJsonViewModule() {
        SimpleModule module = new SimpleModule("jackson-page-with-jsonview", Version.unknownVersion());
        module.addSerializer(PageImpl.class, new PageSerializer());
        return module;
    }
}
