package ru.majordomo.hms.personmgr.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;

import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.service.FinFeignClient;


/**
 * ProcessingBusinessActionEventListener
 */
public class DomainTldEventListener extends AbstractMongoEventListener<DomainTld> {
    @Autowired
    private FinFeignClient finFeignClient;

    @Override
    public void onAfterConvert(AfterConvertEvent<DomainTld> event) {
        super.onAfterConvert(event);
        DomainTld domainTld = event.getSource();
        try {
            domainTld.setRegistrationService(finFeignClient.get(domainTld.getRegistrationServiceId()));
            domainTld.setRenewService(finFeignClient.get(domainTld.getRenewServiceId()));
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}
