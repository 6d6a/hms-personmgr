package ru.majordomo.hms.personmgr.service.task.executor;

import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.task.SendMailIfAbonementWasNotBought;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.AccountNotificationHelper;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AbonementWasNotBoughtExecutor implements Executor<SendMailIfAbonementWasNotBought> {

    private PersonalAccountManager accountManager;
    private AbonementManager<AccountAbonement> abonementManager;
    private AccountNotificationHelper notificationHelper;
    private AccountHelper accountHelper;
    private final PlanManager planManager;

    public AbonementWasNotBoughtExecutor(
            PersonalAccountManager accountManager,
            AbonementManager<AccountAbonement> abonementManager,
            AccountNotificationHelper notificationHelper,
            AccountHelper accountHelper,
            PlanManager planManager
    ) {
        this.accountManager = accountManager;
        this.abonementManager = abonementManager;
        this.notificationHelper = notificationHelper;
        this.accountHelper = accountHelper;
        this.planManager = planManager;
    }

    @Override
    public void execute(SendMailIfAbonementWasNotBought task) {
        List<AccountAbonement> abonements = abonementManager.findAllByPersonalAccountId(task.getPersonalAccountId());
        if (abonements.size() > 0 && abonements.stream().allMatch(
                accountAbonement -> accountAbonement.getAbonement().isTrial()
        )) {
            PersonalAccount account = accountManager.findOne(task.getPersonalAccountId());

            Plan plan = planManager.findOne(account.getPlanId());

            if (plan.isAbonementOnly()) { return; }

            BigDecimal cost = plan.getNotInternalAbonement().getService().getCost();

            BigDecimal balance = accountHelper.getBalance(account);

            if (balance.compareTo(cost) >= 0) {
                notificationHelper.sendMailIfAbonementNotBought(account, plan);
                task.setEmailWasSent(true);
            }
        }
    }
}
