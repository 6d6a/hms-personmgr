package ru.majordomo.hms.personmgr.service;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.resources.WebSite;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static ru.majordomo.hms.personmgr.common.Constants.DATABASE_HOST_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATABASE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATABASE_SERVICE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATABASE_USER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATABASE_USER_NAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATABASE_USER_PASSWORD_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.UNIX_ACCOUNT_NAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.WEBSITE_SERVER_NAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.WEBSITE_SERVICE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.WEB_SITE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.RequiredField.APP_INSTALL;
import static ru.majordomo.hms.personmgr.common.RequiredField.APP_INSTALL_FULL;

@Component
public class AppsCatService {
    private final RcUserFeignClient rcUserFeignClient;
    private final RcStaffFeignClient rcStaffFeignClient;
    private final BusinessHelper businessHelper;
    private final AccountOwnerManager accountOwnerManager;
    private final PlanCheckerService planCheckerService;
    private final AppscatFeignClient appscatFeignClient;

    public AppsCatService(
            RcUserFeignClient rcUserFeignClient,
            RcStaffFeignClient rcStaffFeignClient,
            BusinessHelper businessHelper,
            AccountOwnerManager accountOwnerManager,
            PlanCheckerService planCheckerService,
            AppscatFeignClient appscatFeignClient) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.rcStaffFeignClient = rcStaffFeignClient;
        this.businessHelper = businessHelper;
        this.accountOwnerManager = accountOwnerManager;
        this.planCheckerService = planCheckerService;
        this.appscatFeignClient = appscatFeignClient;
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

        if (appscatFeignClient.isPendingInstallForAccountByWebSiteId(message.getAccountId(), webSiteId)) {
            throw new ParameterValidationException("На указанном сайте уже идет установка приложения");
        }

        message.addParam(UNIX_ACCOUNT_NAME_KEY, webSite.getUnixAccount().getName());
        message.addParam(SERVER_ID_KEY, webSite.getUnixAccount().getServerId());
        message.addParam(WEBSITE_SERVICE_ID_KEY, webSite.getServiceId());

        message.addParam("APP_TITLE", webSite.getName());
        message.addParam("APP_URL", webSite.getDomains().isEmpty() ? webSite.getName() : webSite.getDomains().get(0).getName());
        message.addParam("APP_PATH", webSite.getDocumentRoot());

        List<Service> databaseServices;

        try {
            databaseServices = rcStaffFeignClient.getDatabaseServicesByServerId(webSite.getUnixAccount().getServerId());
        } catch (Exception e) {
            throw new ParameterValidationException("Сервер баз данных не найден");
        }

        if (databaseServices == null
                || databaseServices.isEmpty()
                || databaseServices.get(0).getServiceSockets().isEmpty()) {
            throw new ParameterValidationException("Сервер баз данных не найден");
        }

        message.addParam(DATABASE_SERVICE_ID_KEY, databaseServices.get(0).getId());

        String databaseHost = databaseServices.get(0).getServiceSockets().get(0).getAddressAsString();

        message.addParam(DATABASE_HOST_KEY, databaseHost);

        message.addParam("DB_HOST", databaseHost);

        return addDatabaseUser(message);
    }

    public ProcessingBusinessAction addDatabaseUser(SimpleServiceMessage message) {
        String databaseUserId = (String) message.getParam(DATABASE_USER_ID_KEY);
        String databaseServiceId = (String) message.getParam(DATABASE_SERVICE_ID_KEY);

        if (databaseUserId == null) {
            String unixAccountName = (String) message.getParam(UNIX_ACCOUNT_NAME_KEY);

            String password = randomAlphabetic(8);
            String databaseUserNamePostfix = randomAlphabetic(4);

            List<DatabaseUser> databaseUsers;

            try {
                databaseUsers = rcUserFeignClient.getDatabaseUsers(message.getAccountId());
            } catch (Exception e) {
                throw new ParameterValidationException("Ошибка при получении пользователей баз данных");
            }

            if (databaseUsers != null && !databaseUsers.isEmpty()) {
                databaseUserNamePostfix = generateUnusedNamePostfix(databaseUsers
                        .stream()
                        .map(Resource::getName)
                        .collect(Collectors.toList())
                );
            }

            String databaseUserName = unixAccountName + "_" + databaseUserNamePostfix;

            message.addParam("DB_USER", databaseUserName);
            message.addParam("DB_PASSWORD", password);

            message.addParam("name", databaseUserName);
            message.addParam("password", password);
            message.addParam("serviceId", databaseServiceId);
            message.addParam("type", "MYSQL");

            message.addParam(DATABASE_USER_PASSWORD_KEY, password);

            if (message.getOperationIdentity() != null) {
                return businessHelper.buildActionByOperationId(
                        BusinessActionType.DATABASE_USER_CREATE_RC,
                        message,
                        message.getOperationIdentity()
                );
            } else {
                return businessHelper.buildActionAndOperation(
                        BusinessOperationType.APP_INSTALL,
                        BusinessActionType.DATABASE_USER_CREATE_RC,
                        message
                );
            }
        } else {
            DatabaseUser databaseUser;
            try {
                databaseUser = rcUserFeignClient.getDatabaseUser(message.getAccountId(), databaseUserId);
            } catch (Exception e) {
                throw new ParameterValidationException("Пользователь баз данных не найден");
            }

            message.addParam(DATABASE_USER_NAME_KEY, databaseUser.getName());

            message.addParam("DB_USER", databaseUser.getName());

            if (message.getParam(DATABASE_USER_PASSWORD_KEY) == null
                    || message.getParam(DATABASE_USER_PASSWORD_KEY).equals("")) {
                throw new ParameterValidationException("Не указан пароль для пользователя баз данных");
            }

            message.addParam("DB_PASSWORD", message.getParam(DATABASE_USER_PASSWORD_KEY));

            return addDatabase(message);
        }
    }

    public ProcessingBusinessAction addDatabase(SimpleServiceMessage message) {
        String databaseId = (String) message.getParam(DATABASE_ID_KEY);
        String databaseUserId = (String) message.getParam(DATABASE_USER_ID_KEY);
        String databaseServiceId = (String) message.getParam(DATABASE_SERVICE_ID_KEY);

        if (databaseId == null) {
            if (!planCheckerService.canAddDatabase(message.getAccountId())) {
                throw new ParameterValidationException("На аккаунте уже создано максимальнео количество баз данных");
            }

            String unixAccountName = (String) message.getParam(UNIX_ACCOUNT_NAME_KEY);

            String databaseNamePostfix = randomAlphabetic(4);

            List<Database> databases;

            try {
                databases = (List<Database>) rcUserFeignClient.getDatabases(message.getAccountId());
            } catch (Exception e) {
                throw new ParameterValidationException("Ошибка при получении баз данных");
            }

            if (databases != null && !databases.isEmpty()) {
                databaseNamePostfix = generateUnusedNamePostfix(databases
                        .stream()
                        .map(Resource::getName)
                        .collect(Collectors.toList())
                );
            }

            String databaseName = "b" + unixAccountName.substring(1) + "_" + databaseNamePostfix;

            message.addParam("DB_NAME", databaseName);

            message.addParam("name", databaseName);
            message.addParam("serviceId", databaseServiceId);
            message.addParam("type", "MYSQL");

            Set<String> databaseUserIds = new HashSet<>();
            databaseUserIds.add(databaseUserId);

            message.addParam("databaseUserIds", databaseUserIds);

            if (message.getOperationIdentity() != null) {
                return businessHelper.buildActionByOperationId(
                        BusinessActionType.DATABASE_CREATE_RC,
                        message,
                        message.getOperationIdentity()
                );
            } else {
                return businessHelper.buildActionAndOperation(
                        BusinessOperationType.APP_INSTALL,
                        BusinessActionType.DATABASE_CREATE_RC,
                        message
                );
            }
        } else {
            Database database;
            try {
                database = rcUserFeignClient.getDatabase(message.getAccountId(), databaseId);
            } catch (Exception e) {
                throw new ParameterValidationException("База данных не найдена");
            }

            message.addParam("DB_NAME", database.getName());

            return processInstall(message);
        }
    }

    public ProcessingBusinessAction processInstall(SimpleServiceMessage message) {
        Utils.checkRequiredParams(message.getParams(), APP_INSTALL_FULL);

        AccountOwner accountOwner = accountOwnerManager.findOneByPersonalAccountId(message.getAccountId());

        String password = randomAlphabetic(8);

        message.addParam("ADMIN_USERNAME", "admin");
        message.addParam("ADMIN_EMAIL", String.join(",", accountOwner.getContactInfo().getEmailAddresses()));
        message.addParam("ADMIN_PASSWORD", password);

        String serverName = rcStaffFeignClient.getServerByServiceId((String) message.getParam(WEBSITE_SERVICE_ID_KEY)).getName();

        message.addParam(WEBSITE_SERVER_NAME_KEY, serverName.split("\\.")[0]);

        if (message.getOperationIdentity() != null) {
            return businessHelper.buildActionByOperationId(
                    BusinessActionType.APP_INSTALL_APPSCAT,
                    message,
                    message.getOperationIdentity()
            );
        } else {
            return businessHelper.buildActionAndOperation(
                    BusinessOperationType.APP_INSTALL,
                    BusinessActionType.APP_INSTALL_APPSCAT,
                    message
            );
        }
    }

    private String generateUnusedNamePostfix(List<String> usedNames) {
        String namePostfix;
        for (int i = 0; i < 5; i++) {
            namePostfix = randomAlphabetic(4).toLowerCase();
            int count = 0;
            for (String usedName : usedNames) {
                count++;
                if (usedName.endsWith("_" + namePostfix)) {
                    break;
                } else if (count == usedNames.size()) {
                    return namePostfix;
                }
            }
        }
        throw new ParameterValidationException("Создание уникального имени ресурса не удалось");
    }
}
