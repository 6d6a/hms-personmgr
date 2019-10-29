package ru.majordomo.hms.personmgr.service.promocodeAction;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.repository.AccountPromocodeRepository;
import ru.majordomo.hms.personmgr.service.AbonementService;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.AccountServiceHelper;
import ru.majordomo.hms.personmgr.service.PreorderService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static ru.majordomo.hms.personmgr.common.Utils.differenceInDays;

@Slf4j
@Service
public class AbonementActionProcessor implements PromocodeActionProcessor {

    private final PlanManager planManager;
    private final AccountHelper accountHelper;
    private final AccountHistoryManager history;
    private final AbonementService abonementService;
    private final AccountPromocodeRepository accountPromocodeRepository;
    private final AccountServiceHelper accountServiceHelper;
    private final PreorderService preorderService;

    @Autowired
    public AbonementActionProcessor(
            PlanManager planManager,
            AccountHelper accountHelper,
            AccountHistoryManager history,
            AbonementService abonementService,
            AccountPromocodeRepository accountPromocodeRepository,
            PreorderService preorderService,
            AccountServiceHelper accountServiceHelper) {
        this.planManager = planManager;
        this.accountHelper = accountHelper;
        this.history = history;
        this.abonementService = abonementService;
        this.accountPromocodeRepository = accountPromocodeRepository;
        this.accountServiceHelper = accountServiceHelper;
        this.preorderService = preorderService;
    }

    @Override
    public Result process(PersonalAccount account, PromocodeAction action, String code) {
        log.debug("Processing promocode SERVICE_ABONEMENT codeAction: " + action.toString());

        Plan currentPlan = planManager.findOne(account.getPlanId());

        String serviceId = action.getProperties().get("serviceId").toString();

        Plan requiredPlan = planManager.findByServiceId(serviceId);

        boolean needChangePlan = !requiredPlan.getId().equals(currentPlan.getId());
        boolean nowIsDayOfRegistration = differenceInDays(account.getCreated().toLocalDate(), LocalDate.now()) == 0;

        if (needChangePlan && !nowIsDayOfRegistration) {
            return Result.error("Для использования акции необходимо сменить тариф на " + requiredPlan.getName());
        }

        String period = action.getProperties().get("period").toString();
        // Ищем соответствующий abonementId по периоду и плану
        Optional<Abonement> abonementOptional = requiredPlan.getAbonements().stream()
                .filter(a -> a.isInternal() && a.getPeriod().equals(period))
                .findFirst();

        if (!abonementOptional.isPresent()) {
            return Result.gotException("Не найден абонемент с тарифом " + requiredPlan.getName() + " и периодом " + period);
        }

        if (needChangePlan) {
            accountHelper.setPlanId(account.getId(), requiredPlan.getId());
            account.setPlanId(requiredPlan.getId());
            accountServiceHelper.deleteAccountServiceByServiceId(account, currentPlan.getServiceId());
            accountServiceHelper.addAccountService(account, requiredPlan.getServiceId());
            history.save(account, "Тариф изменён с " + currentPlan.getName() + " на " + requiredPlan.getName()
                    + " для применения промокода " + code);
        }

        Abonement abonement = abonementOptional.get();

        if (preorderService.isPreorder(account.getId())) {
            preorderService.addPromoPreorder(account, abonement);
        } else {
            abonementService.addAbonement(account, abonement.getId());
            accountHelper.enableAccount(account);
        }

        history.save(account, "Добавлен абонемент " + abonement.getName() + " при использовании промокода " + code);

        return Result.success();
    }

    @Override
    public boolean isAllowed(PersonalAccount account, PromocodeAction action) {
        List<AccountPromocode> accountPromocodes = accountPromocodeRepository.findByPersonalAccountId(account.getId());

        return accountPromocodes
                .stream()
                .flatMap(ap -> ap.getPromocode().getActions().stream())
                .noneMatch(usedAction -> usedAction.getActionType().equals(action.getActionType()));
    }
}
