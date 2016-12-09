package ru.majordomo.hms.personmgr.test.config;

import com.github.fakemongo.Fongo;
import com.mongodb.Mongo;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import ru.majordomo.hms.personmgr.controller.rest.RestPlanController;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

@Configuration
@EnableWebMvc
@EnableMongoRepositories("ru.majordomo.hms.personmgr.repository")
public class ConfigRestPlanController extends AbstractMongoConfiguration {
    @Bean
    public EmbeddedServletContainerFactory embeddedServletContainerFactory() {
        return new JettyEmbeddedServletContainerFactory(0);
    }


    @Bean
    @Autowired
    public RestPlanController restPlanController(PlanRepository repository) {
        return new RestPlanController(repository);
    }

    @Override
    protected String getDatabaseName() {
        return "personmgr" + ObjectId.get().toString();
    }

    @Override
    public Mongo mongo() throws Exception {
        return new Fongo(getDatabaseName()).getMongo();
    }
}
