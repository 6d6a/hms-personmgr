package ru.majordomo.hms.personmgr;

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
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;


import feign.Feign;
import ru.majordomo.hms.personmgr.event.AbonementEventListener;
import ru.majordomo.hms.personmgr.event.AccountAbonementEventListener;
import ru.majordomo.hms.personmgr.event.AccountDomainEventListener;
import ru.majordomo.hms.personmgr.event.AccountPromocodeEventListener;
import ru.majordomo.hms.personmgr.event.AccountSeoOrderEventListener;
import ru.majordomo.hms.personmgr.event.AccountServiceEventListener;
import ru.majordomo.hms.personmgr.event.DomainTldEventListener;
import ru.majordomo.hms.personmgr.event.PersonalAccountEventListener;
import ru.majordomo.hms.personmgr.event.PlanEventListener;
import ru.majordomo.hms.personmgr.event.ProcessingBusinessActionEventListener;
import ru.majordomo.hms.personmgr.event.PromocodeEventListener;
import ru.majordomo.hms.personmgr.event.SeoEventListener;
import ru.majordomo.hms.personmgr.repository.AccountAbonementRepository;
import ru.majordomo.hms.personmgr.service.DomainTldService;
import ru.majordomo.hms.personmgr.service.PlanBuilder;
import ru.majordomo.hms.personmgr.service.importing.*;
import ru.majordomo.hms.personmgr.validators.ObjectIdValidator;

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
    private PersonalAccountServicesDBImportService personalAccountServicesDBImportService;

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
                seeded = businessActionDBSeedService.seedDB();
                sb.append(" ").append(seeded ? "businessFlow db_seeded" : "businessFlow db_not_seeded");
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

//                imported = serviceDBImportService.importToMongo();
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

//                imported = personalAccountServicesDBImportService.importToMongo();
//                imported = personalAccountServicesDBImportService.importToMongo("100800");
//                sb.append(" ").append(imported ? "personalAccountServices db_imported" : "personalAccountServices db_not_imported");

//                imported = promocodeDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "promocode db_imported" : "promocode db_not_imported");

                imported = accountPromocodeDBImportService.importToMongo("137010");
                sb.append(" ").append(imported ? "accountPromocode db_imported" : "accountPromocode db_not_imported");

//                imported = bonusPromocodeDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "bonusPromocode db_imported" : "bonusPromocode db_not_imported");

//                imported = domainTldDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "domainTldD db_imported" : "domainTldD db_not_imported");

//                imported = accountDomainDBImportService.importToMongo();
//                sb.append(" ").append(imported ? "accountDomain db_imported" : "accountDomain db_not_imported");

//                  imported = accountAbonementDBImportService.importToMongo();
//                  sb.append(" ").append(imported ? "accountAbonement db_imported" : "accountAbonement db_not_imported");
            }
        }
        sb = sb.length() == 0 ? sb.append("No Options Specified") : sb;
        logger.info(String.format("Launched personal manager with following options: %s", sb.toString()));
    }

    @Bean
    @Autowired
    public ProcessingBusinessActionEventListener processingBusinessActionEventListener(MongoOperations mongoOperations) {
        return new ProcessingBusinessActionEventListener(mongoOperations);
    }

    @Bean
    @Autowired
    public AccountDomainEventListener accountDomainEventListener(DomainTldService domainTldService) {
        return new AccountDomainEventListener(domainTldService);
    }

    @Bean
    @Autowired
    public DomainTldEventListener domainTldEventListener(MongoOperations mongoOperations) {
        return new DomainTldEventListener(mongoOperations);
    }

    @Bean
    @Autowired
    public PlanEventListener planEventListener(PlanBuilder planBuilder) {
        return new PlanEventListener(planBuilder);
    }

    @Bean
    @Autowired
    public PromocodeEventListener promocodeEventListener(MongoOperations mongoOperations) {
        return new PromocodeEventListener(mongoOperations);
    }

    @Bean
    @Autowired
    public SeoEventListener seoEventListener(MongoOperations mongoOperations) {
        return new SeoEventListener(mongoOperations);
    }

    @Bean
    @Autowired
    public AccountSeoOrderEventListener accountSeoOrderEventListener(MongoOperations mongoOperations) {
        return new AccountSeoOrderEventListener(mongoOperations);
    }

    @Bean
    @Autowired
    public AccountPromocodeEventListener accountPromocodeEventListener(MongoOperations mongoOperations) {
        return new AccountPromocodeEventListener(mongoOperations);
    }

    @Bean
    @Autowired
    public AbonementEventListener abonementEventListener(MongoOperations mongoOperations) {
        return new AbonementEventListener(mongoOperations);
    }

    @Bean
    @Autowired
    public AccountAbonementEventListener accountAbonementEventListener(MongoOperations mongoOperations) {
        return new AccountAbonementEventListener(mongoOperations);
    }

    @Bean
    @Autowired
    public PersonalAccountEventListener personalAccountEventListener(MongoOperations mongoOperations) {
        return new PersonalAccountEventListener(mongoOperations);
    }

    @Bean
    @Autowired
    public AccountServiceEventListener accountServiceEventListener(MongoOperations mongoOperations) {
        return new AccountServiceEventListener(mongoOperations);
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

//    @Bean
//    public Feign.Builder feignBuilder() {
//        return Feign.builder();
//    }
}
