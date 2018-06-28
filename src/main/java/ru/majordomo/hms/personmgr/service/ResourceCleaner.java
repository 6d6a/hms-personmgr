package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

import java.util.HashMap;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.Constants.TE_PARAMS_KEY;

@Service
public class ResourceCleaner {

    private BusinessHelper businessHelper;

    @Autowired
    public ResourceCleaner(BusinessHelper businessHelper) {
        this.businessHelper = businessHelper;
    }


    public void cleanData(Database database) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(database.getAccountId());
        message.addParam(RESOURCE_ID_KEY, database.getId());

        Map<String, Object> teParams = new HashMap<>();
        teParams.put(DATA_POSTPROCESSOR_TYPE_KEY, DATA_POSTPROCESSOR_ERASER);
        message.addParam(TE_PARAMS_KEY, teParams);

        businessHelper.buildAction(BusinessActionType.DATABASE_UPDATE_RC, message);
    }

    public void cleanData(UnixAccount unixAccount) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(unixAccount.getAccountId());
        message.addParam(RESOURCE_ID_KEY, unixAccount.getId());

        Map<String, Object> teParams = new HashMap<>();
        teParams.put(DATA_POSTPROCESSOR_TYPE_KEY, DATA_POSTPROCESSOR_ERASER);
        message.addParam(TE_PARAMS_KEY, teParams);

        businessHelper.buildAction(BusinessActionType.UNIX_ACCOUNT_UPDATE_RC, message);
    }
}
