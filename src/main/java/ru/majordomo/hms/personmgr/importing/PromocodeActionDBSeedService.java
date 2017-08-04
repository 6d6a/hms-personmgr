package ru.majordomo.hms.personmgr.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.PromocodeActionType;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.repository.PromocodeActionRepository;

import static ru.majordomo.hms.personmgr.common.Constants.BONUS_FREE_DOMAIN_PROMOCODE_ACTION_ID;
import static ru.majordomo.hms.personmgr.common.Constants.BONUS_PARKING_3_M_PROMOCODE_ACTION_ID;
import static ru.majordomo.hms.personmgr.common.Constants.BONUS_UNLIMITED_1_M_PROMOCODE_ACTION_ID;
import static ru.majordomo.hms.personmgr.common.Constants.BONUS_UNLIMITED_3_M_PROMOCODE_ACTION_ID;
import static ru.majordomo.hms.personmgr.common.Constants.PARTNER_PROMOCODE_ACTION_ID;
import static ru.majordomo.hms.personmgr.common.Constants.PLAN_PARKING_DOMAINS_ID;
import static ru.majordomo.hms.personmgr.common.Constants.PLAN_UNLIMITED_ID;


/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class PromocodeActionDBSeedService {
    private final static Logger logger = LoggerFactory.getLogger(PromocodeActionDBSeedService.class);

    private final PromocodeActionRepository promocodeActionRepository;

    private final PlanRepository planRepository;

    @Autowired
    public PromocodeActionDBSeedService(PlanRepository planRepository, PromocodeActionRepository promocodeActionRepository) {
        this.planRepository = planRepository;
        this.promocodeActionRepository = promocodeActionRepository;
    }

    public boolean seedDB() {
        boolean result = false;

        promocodeActionRepository.deleteAll();

        this.seedPromocodeActions();

        logger.debug("promocodeAction found with findAll():");
        logger.debug("-------------------------------");

        List<PromocodeAction> promocodeActions = promocodeActionRepository.findAll();
        if (promocodeActions.size() > 0) {
            result = true;
        }
        for (PromocodeAction promocodeAction : promocodeActions) {
            logger.debug("promocodeAction: " + promocodeAction.toString());
        }

        return result;
    }

    private void seedPromocodeActions() {
        //Пополнение на 50 р
        PromocodeAction promocodeAction = new PromocodeAction();

        promocodeAction.setActionType(PromocodeActionType.BALANCE_FILL);

        Map<String, Object> properties = new HashMap<>();
        properties.put("amount", "50");

        promocodeAction.setProperties(properties);

        promocodeAction.setId(PARTNER_PROMOCODE_ACTION_ID);

        promocodeActionRepository.save(promocodeAction);

        //Безлимит на 1М
        promocodeAction = new PromocodeAction();

        promocodeAction.setActionType(PromocodeActionType.SERVICE_ABONEMENT);

        //Безлимитный план
        Plan plan = planRepository.findByOldId(String.valueOf(PLAN_UNLIMITED_ID));

        properties = new HashMap<>();
        properties.put("serviceId", plan.getServiceId());
        properties.put("period", "P1M");

        promocodeAction.setProperties(properties);

        promocodeAction.setId(BONUS_UNLIMITED_1_M_PROMOCODE_ACTION_ID);

        promocodeActionRepository.save(promocodeAction);

        //Безлимит на 3М
        promocodeAction = new PromocodeAction();

        promocodeAction.setActionType(PromocodeActionType.SERVICE_ABONEMENT);

        properties = new HashMap<>();
        properties.put("serviceId", plan.getServiceId());
        properties.put("period", "P3M");

        promocodeAction.setProperties(properties);

        promocodeAction.setId(BONUS_UNLIMITED_3_M_PROMOCODE_ACTION_ID);

        promocodeActionRepository.save(promocodeAction);

        //Парковка на 3М
        promocodeAction = new PromocodeAction();

        promocodeAction.setActionType(PromocodeActionType.SERVICE_ABONEMENT);

        //ПарковкаДоменов план
        plan = planRepository.findByOldId(String.valueOf(PLAN_PARKING_DOMAINS_ID));

        properties = new HashMap<>();
        properties.put("serviceId", plan.getServiceId());
        properties.put("period", "P3M");

        promocodeAction.setProperties(properties);

        promocodeAction.setId(BONUS_PARKING_3_M_PROMOCODE_ACTION_ID);

        promocodeActionRepository.save(promocodeAction);

        //Бесплатный домен
        promocodeAction = new PromocodeAction();

        promocodeAction.setActionType(PromocodeActionType.SERVICE_FREE_DOMAIN);

        properties = new HashMap<>();
        List<String> tlds = new ArrayList<>();
        tlds.add("ru");
        tlds.add("xn--p1ai");
        properties.put("tlds", tlds);

        promocodeAction.setProperties(properties);

        promocodeAction.setId(BONUS_FREE_DOMAIN_PROMOCODE_ACTION_ID);

        promocodeActionRepository.save(promocodeAction);
    }
}
