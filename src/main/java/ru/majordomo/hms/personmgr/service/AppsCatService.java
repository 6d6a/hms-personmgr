package ru.majordomo.hms.personmgr.service;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.resources.WebSite;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static ru.majordomo.hms.personmgr.common.Constants.DATABASE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATABASE_SERVICE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATABASE_USER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATABASE_USER_NAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.UNIX_ACCOUNT_NAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.WEB_SITE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.RequiredField.APP_INSTALL;
import static ru.majordomo.hms.personmgr.common.RequiredField.APP_INSTALL_FULL;

@Component
public class AppsCatService {
    private final RcUserFeignClient rcUserFeignClient;
    private final RcStaffFeignClient rcStaffFeignClient;
    private final BusinessHelper businessHelper;

    public AppsCatService(
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

        String webSiteId = (String) message.getParam(WEB_SITE_ID_KEY);

        try {
            webSite = rcUserFeignClient.getWebSite(message.getAccountId(), webSiteId);
        } catch (Exception e) {
            throw new ParameterValidationException("Сайт не найден");
        }

        if (webSite == null) {
            throw new ParameterValidationException("Сайт не найден");
        }

        message.addParam(UNIX_ACCOUNT_NAME_KEY, webSite.getUnixAccount().getName());
        message.addParam(SERVER_ID_KEY, webSite.getUnixAccount().getServerId());

        List<Service> databaseServices;

        try {
            databaseServices = rcStaffFeignClient.getDatabaseServicesByServerId(webSite.getUnixAccount().getServerId());
        } catch (Exception e) {
            throw new ParameterValidationException("Сервер баз данных не найден");
        }

        if (databaseServices == null) {
            throw new ParameterValidationException("Сервер баз данных не найден");
        }

        message.addParam(DATABASE_SERVICE_ID_KEY, databaseServices.get(0).getId());

        return addDatabaseUser(message);
    }

    public ProcessingBusinessAction addDatabaseUser(SimpleServiceMessage message) {
        String databaseUserId = (String) message.getParam(DATABASE_USER_ID_KEY);
        String databaseServiceId = (String) message.getParam(DATABASE_SERVICE_ID_KEY);

        if (databaseUserId == null) {
            String unixAccountName = (String) message.getParam(UNIX_ACCOUNT_NAME_KEY);

            String password = randomAlphabetic(8);
            String databaseUserNamePostfix = randomAlphabetic(4);

            message.addParam("name", unixAccountName + "_" + databaseUserNamePostfix);
            message.addParam("password", password);
            message.addParam("serviceId", databaseServiceId);
            message.addParam("type", "MYSQL");

            return businessHelper.buildActionAndOperation(
                    BusinessOperationType.APP_INSTALL,
                    BusinessActionType.DATABASE_USER_CREATE_RC,
                    message
            );
        } else {
            DatabaseUser databaseUser = rcUserFeignClient.getDatabaseUser(message.getAccountId(), databaseUserId);

            message.addParam(DATABASE_USER_NAME_KEY, databaseUser.getName());

            return addDatabase(message);
        }
    }

    public ProcessingBusinessAction addDatabase(SimpleServiceMessage message) {
        String databaseId = (String) message.getParam(DATABASE_ID_KEY);
        String databaseUserId = (String) message.getParam(DATABASE_USER_ID_KEY);
        String databaseServiceId = (String) message.getParam(DATABASE_SERVICE_ID_KEY);

        if (databaseId == null) {
            String unixAccountName = (String) message.getParam(UNIX_ACCOUNT_NAME_KEY);

            String databaseNamePostfix = randomAlphabetic(4);
            message.addParam("name", "b" + unixAccountName.substring(1) + "_" + databaseNamePostfix);
            message.addParam("serviceId", databaseServiceId);
            message.addParam("type", "MYSQL");

            Set<String> databaseUserIds = new HashSet<>();
            databaseUserIds.add(databaseUserId);

            message.addParam("databaseUserIds", databaseUserIds);

            return businessHelper.buildActionAndOperation(BusinessOperationType.APP_INSTALL, BusinessActionType.DATABASE_CREATE_RC, message);
        } else {
            return processInstall(message);
        }
    }

    public ProcessingBusinessAction processInstall(SimpleServiceMessage message) {
        Utils.checkRequiredParams(message.getParams(), APP_INSTALL_FULL);

        return businessHelper.buildActionAndOperation(BusinessOperationType.APP_INSTALL, BusinessActionType.APP_INSTALL_APPSCAT, message);
    }
}
