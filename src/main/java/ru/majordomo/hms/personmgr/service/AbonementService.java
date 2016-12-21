package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountAbonementRepository;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

@Service
public class AbonementService {
    private final FinFeignClient finFeignClient;
    private final PlanRepository planRepository;
    private final AccountAbonementRepository accountAbonementRepository;
    private final AccountServiceRepository accountServiceRepository;

    @Autowired
    public AbonementService(
            FinFeignClient finFeignClient,
            PlanRepository planRepository,
            AccountAbonementRepository accountAbonementRepository,
            AccountServiceRepository accountServiceRepository
    ) {
        this.finFeignClient = finFeignClient;
        this.planRepository = planRepository;
        this.accountAbonementRepository = accountAbonementRepository;
        this.accountServiceRepository = accountServiceRepository;
    }


    /**
     * Покупка абонемента
     *
     * @param account Аккаунт
     * @param abonementId id абонемента
     * @param autorenew автопродление абонемента
     */
    public void addAbonement(PersonalAccount account, String abonementId, Boolean autorenew) {
        Plan plan = planRepository.findOne(account.getPlanId());

        if(plan == null){
            throw new ResourceNotFoundException("Account plan not found");
        }

        if (!plan.getAbonementIds().contains(abonementId)) {
            throw new ParameterValidationException("Current account plan does not have abonement with specified abonementId");
        }

        Optional<Abonement> newAbonement = plan.getAbonements().stream().filter(abonement1 -> abonement1.getId().equals(abonementId)).findFirst();

        if (!newAbonement.isPresent()) {
            throw new ParameterValidationException("Current account plan does not have abonement with specified abonementId (not found in abonements)");
        }

        Abonement abonement = newAbonement.get();

        List<AccountAbonement> accountAbonements = accountAbonementRepository.findByPersonalAccountId(account.getId());

        if(accountAbonements != null && !accountAbonements.isEmpty()){
            throw new ParameterValidationException("Account already has abonement");
        }

        Map<String, Object> paymentOperation = new HashMap<>();
        paymentOperation.put("serviceId", abonement.getServiceId());
        paymentOperation.put("amount", abonement.getService().getCost());

        Map<String, Object> response = finFeignClient.charge(account.getId(), paymentOperation);

        if (response.get("success") != null && !((boolean) response.get("success"))) {
            throw new ParameterValidationException("Could not charge money for abonement");
        }

        AccountAbonement accountAbonement = new AccountAbonement();
        accountAbonement.setAbonementId(abonementId);
        accountAbonement.setPersonalAccountId(account.getId());
        accountAbonement.setCreated(LocalDateTime.now());
        accountAbonement.setExpired(LocalDateTime.now().plus(Period.parse(abonement.getPeriod())));
        accountAbonement.setAutorenew(autorenew);

        accountAbonementRepository.save(accountAbonement);

        List<AccountService> accountService = accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), plan.getServiceId());

        if (accountService != null && !accountService.isEmpty()) {
            accountServiceRepository.delete(accountService);
        }
    }

    /**
     * Удаление абонемента
     *
     * @param account Аккаунт
     * @param accountAbonementId id абонемента на аккаунте
     */
    public void deleteAbonement(PersonalAccount account, String accountAbonementId) {
        Plan plan = planRepository.findOne(account.getPlanId());

        if(plan == null){
            throw new ResourceNotFoundException("Account plan not found");
        }

        AccountAbonement accountAbonement = accountAbonementRepository.findByIdAndPersonalAccountId(accountAbonementId, account.getId());

        accountAbonementRepository.delete(accountAbonement);

        //Создаем AccountService с выбранным тарифом
        AccountService service = new AccountService();
        service.setPersonalAccountId(account.getId());
        service.setServiceId(plan.getServiceId());

        accountServiceRepository.save(service);
    }
}
