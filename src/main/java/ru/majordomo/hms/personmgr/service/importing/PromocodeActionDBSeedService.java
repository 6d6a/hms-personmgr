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

import static ru.majordomo.hms.personmgr.common.ImportConstants.getPartnerPromocodeActionId;


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

        logger.info("BusinessAction found with findAll():");
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
        PromocodeAction promocodeAction = new PromocodeAction();

        promocodeAction.setActionType(PromocodeActionType.BALANCE_FILL);

        Map<String, String> properties = new HashMap<>();
        properties.put("amount", "50");

        promocodeAction.setProperties(properties);

        promocodeAction.setId(getPartnerPromocodeActionId());

        promocodeActionRepository.save(promocodeAction);
    }
}
