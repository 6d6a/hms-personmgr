package ru.majordomo.hms.personmgr.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;

import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.service.FinFeignClient;

public class AbonementEventListener extends AbstractMongoEventListener<Abonement> {
    @Autowired
    private FinFeignClient finFeignClient;

    @Override
    public void onAfterConvert(AfterConvertEvent<Abonement> event) {
        super.onAfterConvert(event);
        Abonement abonement = event.getSource();
        try {
            abonement.setService(finFeignClient.get(abonement.getFinServiceId()));
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}
