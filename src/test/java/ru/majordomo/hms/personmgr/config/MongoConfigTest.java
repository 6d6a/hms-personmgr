package ru.majordomo.hms.personmgr.config;

import com.mongodb.MongoClient;

import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.UUID;

@Configuration
@EnableMongoRepositories({"ru.majordomo.hms.personmgr.repository"})
@Profile("test")
public class MongoConfigTest extends AbstractMongoConfiguration {
    @Override
    protected String getDatabaseName() {
        return "personmgr-" + ObjectId.get().toString();
    }

    @Bean(destroyMethod="shutdown")
    public MongoServer mongoServer() {
        MongoServer mongoServer = new MongoServer(new MemoryBackend());
        mongoServer.bind();
        return mongoServer;
    }

    @Override
    public MongoClient mongoClient() {
        return new MongoClient(new ServerAddress(mongoServer().getLocalAddress()));
    }

    @Override
    public MongoTemplate mongoTemplate() throws Exception {
        return new MongoTemplate(new SimpleMongoDbFactory(mongoClient(), UUID.randomUUID().toString()));
    }
}
