package ru.majordomo.hms.personmgr.event.promocode.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.event.promocode.AccountPromocodeWasCreated;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.PromocodeManager;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeTag;
import ru.majordomo.hms.personmgr.service.Rpc.RegRpcClient;

import java.util.List;

@Component
public class PromocodeEventListener {
    private final PromocodeManager promocodeManager;
    private final PersonalAccountManager accountManager;
    private final RegRpcClient regRpcClient;

    @Autowired
    public PromocodeEventListener(
            PromocodeManager promocodeManager, PersonalAccountManager accountManager, RegRpcClient regRpcClient) {
        this.promocodeManager = promocodeManager;
        this.accountManager = accountManager;
        this.regRpcClient = regRpcClient;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onTockaBankTag(AccountPromocodeWasCreated event) {
        AccountPromocode accountPromocode = event.getSource();

        List<PromocodeTag> tags = promocodeManager.findOne(accountPromocode.getPromocodeId()).getTags();

        if (tags.stream().anyMatch(tag -> "tochkaBank".equals(tag.getInternalName()))) {
            accountManager.setHideGoogleAdWords(accountPromocode.getPersonalAccountId(), true);
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onRegistrantCodeUsed(AccountPromocodeWasCreated event) {
        AccountPromocode accountPromocode = event.getSource();

        Promocode promocode = promocodeManager.findOne(accountPromocode.getPromocodeId());
        List<PromocodeTag> tags = promocode.getTags();

        if (tags.stream().anyMatch(tag -> "control_registrant".equals(tag.getInternalName()))) {
            regRpcClient.setPromocodeUsed(promocode.getCode());
        }
    }
}
