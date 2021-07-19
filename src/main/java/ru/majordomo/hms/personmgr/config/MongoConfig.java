package ru.majordomo.hms.personmgr.config;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCommandException;
import com.mongodb.WriteConcern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import ru.majordomo.hms.personmgr.common.Constants;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.validation.event.listener.CustomValidatingMongoEventListener;

import javax.annotation.Nonnull;

import static ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction.*;

/**
 * MongoConfig
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MongoConfig extends AbstractMongoConfiguration {
    private final MongoProperties mongoProperties;

    @Nonnull
    @Override
    protected String getDatabaseName() {
        return mongoProperties.getDatabase();
    }

    @Nonnull
    @Override
    @Bean
    @Primary
    public MongoClient mongoClient() {
        return new MongoClient(new MongoClientURI(mongoProperties.getUri()));
    }

    @Nonnull
    @Override
    public MongoTemplate mongoTemplate() throws Exception {
        MongoTemplate mongoTemplate = new MongoTemplate(mongoDbFactory(), mappingMongoConverter());
        mongoTemplate.setWriteConcern(WriteConcern.ACKNOWLEDGED);

        initIndicesAfterStartup(mongoTemplate, true);

        return mongoTemplate;
    }

    public static void initIndicesAfterStartup(MongoTemplate mongoTemplate, boolean recreateIfConflict) {
        String indexName = "partial_unique__deny_similar_actions";
        Index partialUniqueIndex = new Index()
                .named(indexName)
                .on(KEY_DENY_SAME_KEY, Sort.Direction.ASC)
                .on(Constants.PERSONAL_ACCOUNT_ID_KEY, Sort.Direction.ASC)
                .on(RESOURCE_DENY_SAME_KEY, Sort.Direction.ASC)
                .on(EXECUTING_KEY, Sort.Direction.ASC)
                .unique()
                .partial(PartialIndexFilter.of(
                        Criteria.where(KEY_DENY_SAME_KEY).exists(true).and(EXECUTING_KEY).is(true)
                ));
        try {
            String result = mongoTemplate.indexOps(ProcessingBusinessAction.class).ensureIndex(partialUniqueIndex);
        } catch (UncategorizedMongoDbException e) {
            if (recreateIfConflict && e.getCause() instanceof MongoCommandException && ((MongoCommandException) e.getCause()).getCode() == 86) {
                // IndexKeySpecsConflict,
                // Index already exists with the same options, so no need to build a new
                // one (not an error). Most likely requested by a client using ensureIndex.
                log.info("IndexKeySpecsConflict. Attempt to drop and create index: " + indexName, e);
                mongoTemplate.indexOps(ProcessingBusinessAction.class).dropIndex(indexName);
                String result = mongoTemplate.indexOps(ProcessingBusinessAction.class).ensureIndex(partialUniqueIndex);
            } else {
                log.error("We got mongodb's exception during index initialization", e);
                throw e;
            }
        }
    }

    @Bean
    public CustomValidatingMongoEventListener validatingMongoEventListener() {
        return new CustomValidatingMongoEventListener(validator());
    }

    @Bean
    LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();
        validatorFactoryBean.setValidationMessageSource(messageSource());
        return validatorFactoryBean;
    }

    @Bean
    public ReloadableResourceBundleMessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages/messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    @Bean
    public MessageSourceAccessor messageSourceAccessor() {
        return new MessageSourceAccessor(messageSource());
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        MethodValidationPostProcessor methodValidationPostProcessor = new MethodValidationPostProcessor();
        methodValidationPostProcessor.setValidator(validator());
        return methodValidationPostProcessor;
    }

    @Bean("jongoMongoClient")
    public MongoClient jongoMongoClient() {
        return new MongoClient(new MongoClientURI(mongoProperties.getUri()));
    }
}
