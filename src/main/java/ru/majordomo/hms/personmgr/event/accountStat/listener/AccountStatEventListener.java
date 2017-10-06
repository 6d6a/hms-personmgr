package ru.majordomo.hms.personmgr.event.accountStat.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.accountStat.AccountStatDomainUpdateEvent;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.service.AccountStatHelper;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.rc.user.resources.Domain;

import java.util.HashMap;

import static ru.majordomo.hms.personmgr.common.AccountStatType.VIRTUAL_HOSTING_AUTO_RENEW_DOMAIN;
import static ru.majordomo.hms.personmgr.common.Constants.*;

@Component
public class AccountStatEventListener {
    private final RcUserFeignClient rcUserFeignClient;
    private final AccountStatHelper accountStatHelper;
    private final ProcessingBusinessActionRepository processingBusinessActionRepository;

    public AccountStatEventListener(
            RcUserFeignClient rcUserFeignClient,
            AccountStatHelper accountStatHelper,
            ProcessingBusinessActionRepository processingBusinessActionRepository
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.accountStatHelper = accountStatHelper;
        this.processingBusinessActionRepository = processingBusinessActionRepository;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountStatDomainUpdateEvent(AccountStatDomainUpdateEvent event) {
        SimpleServiceMessage message = event.getSource();
        String domainName = (String) message.getParam(NAME_KEY);
        String accountId = message.getAccountId();
        boolean statDataAutoRenew = (Boolean) message.getParam(AUTO_RENEW_KEY);

        if (domainName == null) {
            ProcessingBusinessAction businessAction = processingBusinessActionRepository.findOne(message.getActionIdentity());
            Domain domain = rcUserFeignClient.getDomain(accountId, (String) businessAction.getParam(RESOURCE_ID_KEY));
            if (domain != null) { domainName = domain.getName(); }
        }

        HashMap<String, String> statData = new HashMap<>();
        statData.put(ACCOUNT_ID_KEY, accountId);
        statData.put(DOMAIN_NAME_KEY, domainName);

        accountStatHelper.add(
                accountId,
                statDataAutoRenew ? VIRTUAL_HOSTING_AUTO_RENEW_DOMAIN : AccountStatType.VIRTUAL_HOSTING_MANUAL_RENEW_DOMAIN,
                statData);
    }
}
