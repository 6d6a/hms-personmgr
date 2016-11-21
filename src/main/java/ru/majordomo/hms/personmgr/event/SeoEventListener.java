package ru.majordomo.hms.personmgr.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;

import ru.majordomo.hms.personmgr.model.seo.Seo;
import ru.majordomo.hms.personmgr.service.FinFeignClient;

public class SeoEventListener extends AbstractMongoEventListener<Seo> {
    @Autowired
    private FinFeignClient finFeignClient;

    @Override
    public void onAfterConvert(AfterConvertEvent<Seo> event) {
        super.onAfterConvert(event);
        Seo seo = event.getSource();
        try {
            seo.setService(finFeignClient.get(seo.getFinServiceId()));
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}
