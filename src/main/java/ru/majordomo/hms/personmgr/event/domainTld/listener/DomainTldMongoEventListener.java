package ru.majordomo.hms.personmgr.event.domainTld.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static ru.majordomo.hms.personmgr.common.Constants.*;

@Component
public class DomainTldMongoEventListener extends AbstractMongoEventListener<DomainTld> {
    private final MongoOperations mongoOperations;

    @Autowired
    public DomainTldMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<DomainTld> event) {
        super.onAfterConvert(event);
        DomainTld domainTld = event.getSource();
        try {
            domainTld.setRegistrationService(mongoOperations.findOne(new Query(where("_id").is(domainTld.getRegistrationServiceId())), PaymentService.class));
            domainTld.setRenewService(mongoOperations.findOne(new Query(where("_id").is(domainTld.getRenewServiceId())), PaymentService.class));
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        // Заплатка для акциий
        // TODO включение\выключений акций через биллинг?
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime startDate = LocalDateTime.parse(ACTION_DOMAIN_START_DATE, formatter);
        LocalDateTime endDate = LocalDateTime.parse(ACTION_DOMAIN_END_DATE, formatter);

        if (now.isAfter(startDate)) {
            if (Arrays.asList(ACTION_DOMAINS).contains(domainTld.getTld())) {
                domainTld.getRegistrationService().setCost(BigDecimal.valueOf(145L));
            }

            switch (domainTld.getTld()) {
                case "online":
                    domainTld.getRenewService().setCost(BigDecimal.valueOf(2250L));
                    break;
                case "tech":
                    domainTld.getRenewService().setCost(BigDecimal.valueOf(3050L));
                    break;
                case "store":
                    domainTld.getRenewService().setCost(BigDecimal.valueOf(3850L));
                    break;
                case "fun":
                    domainTld.getRenewService().setCost(BigDecimal.valueOf(2200L));
                    break;
                case "site":
                    domainTld.getRenewService().setCost(BigDecimal.valueOf(2600L));
                    break;
                case "website":
                    domainTld.getRenewService().setCost(BigDecimal.valueOf(2250L));
                    break;
                case "space":
                    domainTld.getRenewService().setCost(BigDecimal.valueOf(2250L));
                    break;
            }
        }
    }
}
