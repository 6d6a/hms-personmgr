package ru.majordomo.hms.personmgr.service.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.PromocodeActionType;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.repository.PromocodeActionRepository;

import static ru.majordomo.hms.personmgr.common.ImportConstants.getBonusFreeDomainPromocodeActionId;
import static ru.majordomo.hms.personmgr.common.ImportConstants.getBonusParking3MPromocodeActionId;
import static ru.majordomo.hms.personmgr.common.ImportConstants.getBonusUnlimited1MPromocodeActionId;
import static ru.majordomo.hms.personmgr.common.ImportConstants.getBonusUnlimited3MPromocodeActionId;
import static ru.majordomo.hms.personmgr.common.ImportConstants.getParkingPlanServiceId;
import static ru.majordomo.hms.personmgr.common.ImportConstants.getPartnerPromocodeActionId;
import static ru.majordomo.hms.personmgr.common.ImportConstants.getUnlimitedPlanServiceId;


/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class PromocodeActionDBSeedService {
    private final static Logger logger = LoggerFactory.getLogger(PromocodeActionDBSeedService.class);

    @Autowired
    private PromocodeActionRepository promocodeActionRepository;


    public boolean seedDB() {
        boolean result = false;

        promocodeActionRepository.deleteAll();

        this.seedPromocodeActions();

        logger.info("promocodeAction found with findAll():");
        logger.info("-------------------------------");

        List<PromocodeAction> promocodeActions = promocodeActionRepository.findAll();
        if (promocodeActions.size() > 0) {
            result = true;
        }
        for (PromocodeAction promocodeAction : promocodeActions) {
            logger.info("promocodeAction: " + promocodeAction.toString());
        }

        return result;
    }

    private void seedPromocodeActions() {
        //Пополнение на 50 р
        PromocodeAction promocodeAction = new PromocodeAction();

        promocodeAction.setActionType(PromocodeActionType.BALANCE_FILL);

        Map<String, String> properties = new HashMap<>();
        properties.put("amount", "50");

        promocodeAction.setProperties(properties);

        promocodeAction.setId(getPartnerPromocodeActionId());

        promocodeActionRepository.save(promocodeAction);

        //Безлимит на 1М
        promocodeAction = new PromocodeAction();

        promocodeAction.setActionType(PromocodeActionType.SERVICE_ABONEMENT);

        properties = new HashMap<>();
        properties.put("serviceId", getUnlimitedPlanServiceId());
        properties.put("period", "P1M");

        promocodeAction.setProperties(properties);

        promocodeAction.setId(getBonusUnlimited1MPromocodeActionId());

        promocodeActionRepository.save(promocodeAction);

        //Безлимит на 3М
        promocodeAction = new PromocodeAction();

        promocodeAction.setActionType(PromocodeActionType.SERVICE_ABONEMENT);

        properties = new HashMap<>();
        properties.put("serviceId", getUnlimitedPlanServiceId());
        properties.put("period", "P3M");

        promocodeAction.setProperties(properties);

        promocodeAction.setId(getBonusUnlimited3MPromocodeActionId());

        promocodeActionRepository.save(promocodeAction);

        //Парковка на 3М
        promocodeAction = new PromocodeAction();

        promocodeAction.setActionType(PromocodeActionType.SERVICE_ABONEMENT);

        properties = new HashMap<>();
        properties.put("serviceId", getParkingPlanServiceId());
        properties.put("period", "P3M");

        promocodeAction.setProperties(properties);

        promocodeAction.setId(getBonusParking3MPromocodeActionId());

        promocodeActionRepository.save(promocodeAction);

        //Бесплатный домен
        promocodeAction = new PromocodeAction();

        promocodeAction.setActionType(PromocodeActionType.SERVICE_FREE_DOMAIN);

        promocodeAction.setId(getBonusFreeDomainPromocodeActionId());

        promocodeActionRepository.save(promocodeAction);
    }
}
