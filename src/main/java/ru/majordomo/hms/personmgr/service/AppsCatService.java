package ru.majordomo.hms.personmgr.service;

import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;

import static ru.majordomo.hms.personmgr.common.RequiredField.APP_INSTALL;

@Component
public class AppsCatService {
    private final BusinessHelper businessHelper;
    private final AccountOwnerManager accountOwnerManager;

    public AppsCatService(
            BusinessHelper businessHelper,
            AccountOwnerManager accountOwnerManager
    ) {
        this.businessHelper = businessHelper;
        this.accountOwnerManager = accountOwnerManager;
    }

    public ProcessingBusinessAction processInstall(SimpleServiceMessage message) {
        Utils.checkRequiredParams(message.getParams(), APP_INSTALL);

        AccountOwner accountOwner = accountOwnerManager.findOneByPersonalAccountId(message.getAccountId());

        message.addParam("ADMIN_EMAIL", accountOwner.getContactInfo().getEmailAddresses().get(0));

        return businessHelper.buildActionAndOperation(
                BusinessOperationType.APP_INSTALL,
                BusinessActionType.APP_INSTALL_APPSCAT,
                message
        );
    }
}
