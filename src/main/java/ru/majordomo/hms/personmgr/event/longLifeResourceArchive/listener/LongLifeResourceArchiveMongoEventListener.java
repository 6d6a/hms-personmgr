package ru.majordomo.hms.personmgr.event.longLifeResourceArchive.listener;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.model.service.LongLifeResourceArchive;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;

@Component
public class LongLifeResourceArchiveMongoEventListener extends AbstractMongoEventListener<LongLifeResourceArchive> {
    private final RcUserFeignClient rcUserFeignClient;

    public LongLifeResourceArchiveMongoEventListener(RcUserFeignClient rcUserFeignClient) {
        this.rcUserFeignClient = rcUserFeignClient;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<LongLifeResourceArchive> event) {
        super.onAfterConvert(event);

        LongLifeResourceArchive longLifeResourceArchive = event.getSource();

        if (longLifeResourceArchive.getResourceArchiveId() != null) {
            try {
                longLifeResourceArchive.setResourceArchive(rcUserFeignClient.getResourceArchive(longLifeResourceArchive.getPersonalAccountId(), longLifeResourceArchive.getResourceArchiveId()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
