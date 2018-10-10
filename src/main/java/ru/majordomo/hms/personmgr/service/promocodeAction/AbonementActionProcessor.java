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

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AbonementActionProcessor implements PromocodeActionProcessor {

    private final PlanManager planManager;
    private final AccountHelper accountHelper;
    private final AccountHistoryManager history;
    private final AbonementService abonementService;
    private final AccountPromocodeRepository accountPromocodeRepository;

    @Autowired
    public AbonementActionProcessor(
            PlanManager planManager,
            AccountHelper accountHelper,
            AccountHistoryManager history,
            AbonementService abonementService,
            AccountPromocodeRepository accountPromocodeRepository
    ) {
        this.planManager = planManager;
        this.accountHelper = accountHelper;
        this.history = history;
        this.abonementService = abonementService;
        this.accountPromocodeRepository = accountPromocodeRepository;
    }

    @Override
    public Result process(PersonalAccount account, PromocodeAction action, String code) {
        log.debug("Processing promocode SERVICE_ABONEMENT codeAction: " + action.toString());

        Plan plan = planManager.findOne(account.getPlanId());

        String serviceId = action.getProperties().get("serviceId").toString();

        if (!serviceId.equals(plan.getServiceId())) {
            Plan required = planManager.findByServiceId(serviceId);
            return Result.error("Для использования акции необходимо сменить тариф на " + required.getName());
        }

        String period = action.getProperties().get("period").toString();
        // Ищем соответствующий abonementId по периоду и плану
        Optional<Abonement> abonementOptional = plan.getAbonements().stream()
                .filter(a -> a.getPeriod().equals(period))
                .findFirst();

        if (!abonementOptional.isPresent()) {
            return Result.gotException("Не найден абонемент с тарифом " + plan.getName() + " и периодом " + period);
        }

        Abonement abonement = abonementOptional.get();

        abonementService.addAbonement(account, abonement.getId(), false);

        accountHelper.enableAccount(account);

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
