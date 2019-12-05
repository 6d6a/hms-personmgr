package ru.majordomo.hms.personmgr.event.account.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.DedicatedAppServiceDeleteEvent;
import ru.majordomo.hms.personmgr.event.account.DedicatedAppServiceDisabledEvent;
import ru.majordomo.hms.personmgr.event.account.DedicatedAppServiceEnabledEvent;
import ru.majordomo.hms.personmgr.feign.RcStaffFeignClient;
import ru.majordomo.hms.personmgr.service.BusinessHelper;
import ru.majordomo.hms.personmgr.service.DedicatedAppServiceHelper;
import ru.majordomo.hms.rc.staff.resources.Service;

import java.util.List;

@Component
public class DedicatedAppServiceEventListener {
    private RcStaffFeignClient rcStaffFeignClient;
    private BusinessHelper businessHelper;

    private final DedicatedAppServiceHelper dedicatedAppServiceHelper;

    @Autowired
    public DedicatedAppServiceEventListener(
            RcStaffFeignClient rcStaffFeignClient,
            BusinessHelper businessHelper,
            DedicatedAppServiceHelper dedicatedAppServiceHelper
    ) {
        this.rcStaffFeignClient = rcStaffFeignClient;
        this.businessHelper = businessHelper;
        this.dedicatedAppServiceHelper = dedicatedAppServiceHelper;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void enabled(DedicatedAppServiceEnabledEvent event) {
        String accountId = event.getSource();
        String templateId = event.getTemplateId();
        switchResources(true, accountId, templateId);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void disabled(DedicatedAppServiceDisabledEvent event) {
        String accountId = event.getSource();
        String templateId = event.getTemplateId();
        switchResources(false, accountId, templateId);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void delete(DedicatedAppServiceDeleteEvent event) {
        String accountId = event.getSource();
        String accountServiceId = event.getAccountServiceId();
        dedicatedAppServiceHelper.deleteDedicatedAppServiceAndAccountService(accountId, accountServiceId);
    }


    private void switchResources(boolean state, String accountId, String templateId) {
        List<Service> services = rcStaffFeignClient.getServicesByAccountIdAndTemplateId(accountId, templateId);

        services.forEach(service -> {
                    SimpleServiceMessage message = new SimpleServiceMessage();
                    message.setAccountId(accountId);
                    message.addParam("resourceId", service.getId());
                    message.addParam("switchedOn", state);
                    businessHelper.buildActionAndOperation(
                            BusinessOperationType.DEDICATED_APP_SERVICE_UPDATE,
                            BusinessActionType.DEDICATED_APP_SERVICE_UPDATE_RC_STAFF,
                            message
                    );
                });
    }


}
