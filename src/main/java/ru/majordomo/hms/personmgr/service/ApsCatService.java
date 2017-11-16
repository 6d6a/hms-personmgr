package ru.majordomo.hms.personmgr.service;

import org.springframework.stereotype.Component;

import java.util.List;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.resources.WebSite;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static ru.majordomo.hms.personmgr.common.Constants.DATABASE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATABASE_USER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.WEB_SITE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.RequiredField.APP_INSTALL;

@Component
public class ApsCatService {
    private final RcUserFeignClient rcUserFeignClient;
    private final RcStaffFeignClient rcStaffFeignClient;
    private final BusinessHelper businessHelper;

    public ApsCatService(
            RcUserFeignClient rcUserFeignClient,
            RcStaffFeignClient rcStaffFeignClient,
            BusinessHelper businessHelper
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.rcStaffFeignClient = rcStaffFeignClient;
        this.businessHelper = businessHelper;
    }

    public ProcessingBusinessAction install(SimpleServiceMessage message) {
        Utils.checkRequiredParams(message.getParams(), APP_INSTALL);

        WebSite webSite;

        String webSiteId = (String) message.getParams().get(WEB_SITE_ID_KEY);

        try {
            webSite = rcUserFeignClient.getWebSite(message.getAccountId(), webSiteId);
        } catch (Exception e) {
            throw new ParameterValidationException("Сайт не найден");
        }

        if (webSite == null) {
            throw new ParameterValidationException("Сайт не найден");
        }

        List<Service> databaseServices;

        try {
            databaseServices = rcStaffFeignClient.getDatabaseServicesByServerId(webSite.getUnixAccount().getServerId());
        } catch (Exception e) {
            throw new ParameterValidationException("Сервер баз данных не найден");
        }

        if (databaseServices == null) {
            throw new ParameterValidationException("Сервер баз данных не найден");
        }

        String databaseUserId = (String) message.getParams().get(DATABASE_USER_ID_KEY);

        if (databaseUserId == null) {
            String password = randomAlphabetic(8);
            String databaseUserNamePostfix = randomAlphabetic(4);

            message.addParam("name", webSite.getUnixAccount().getName() + "_" + databaseUserNamePostfix);
            message.addParam("password", password);
            message.addParam("serviceId", databaseServices.get(0).getId());
            message.addParam("type", "MYSQL");

            return businessHelper.buildActionAndOperation(
                    BusinessOperationType.APP_INSTALL,
                    BusinessActionType.DATABASE_USER_CREATE_RC,
                    message
            );
        } else {
            DatabaseUser databaseUser = rcUserFeignClient.getDatabaseUser(message.getAccountId(), databaseUserId);

            String databaseId = (String) message.getParams().get(DATABASE_ID_KEY);

            if (databaseId == null) {

            } else {

            }
        }
    }
}
