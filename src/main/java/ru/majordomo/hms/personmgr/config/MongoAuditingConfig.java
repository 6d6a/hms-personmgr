package ru.majordomo.hms.personmgr.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Profile({"dev", "prod"})
@Configuration
@EnableMongoAuditing
public class MongoAuditingConfig {
}
