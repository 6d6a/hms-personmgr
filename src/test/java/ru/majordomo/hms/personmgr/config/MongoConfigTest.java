package ru.majordomo.hms.personmgr.config;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;

import org.bson.types.ObjectId;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.UUID;

@Configuration
@EnableMongoRepositories({"ru.majordomo.hms.personmgr.repository"})
public class MongoConfigTest extends AbstractMongoConfiguration {
    @Override
    protected String getDatabaseName() {
        return "personmgr-" + ObjectId.get().toString();
    }

    @Override
    public MongoClient mongo() throws Exception {
        return new Fongo(getDatabaseName()).getMongo();
    }

    @Override
    public MongoTemplate mongoTemplate() throws Exception {
        return new MongoTemplate(new SimpleMongoDbFactory(mongo(), UUID.randomUUID().toString()));
    }
}
