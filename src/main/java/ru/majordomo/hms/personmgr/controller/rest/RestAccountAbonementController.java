package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.AccountAbonementRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.FinFeignClient;

@RestController
@RequestMapping("/{accountId}/account-abonements")
public class RestAccountAbonementController extends CommonRestController {

    private final PersonalAccountRepository accountRepository;
    private final AccountAbonementRepository accountAbonementRepository;
    private final PlanRepository planRepository;
    private final FinFeignClient finFeignClient;

    @Autowired
    public RestAccountAbonementController(
            PersonalAccountRepository accountRepository,
            AccountAbonementRepository accountAbonementRepository,
            PlanRepository planRepository, FinFeignClient finFeignClient) {
        this.accountRepository = accountRepository;
        this.accountAbonementRepository = accountAbonementRepository;
        this.planRepository = planRepository;
        this.finFeignClient = finFeignClient;
    }

    @RequestMapping(value = "/{accountAbonementId}", method = RequestMethod.GET)
    public ResponseEntity<AccountAbonement> getAccountAbonement(
            @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "accountAbonementId") String accountAbonementId
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        AccountAbonement accountAbonement = accountAbonementRepository.findByIdAndPersonalAccountId(accountAbonementId, account.getId());

        if(accountAbonement == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(accountAbonement, HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountAbonementId}", method = RequestMethod.PATCH)
    public ResponseEntity<SimpleServiceMessage> updateAccountAbonement(
            @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "accountAbonementId") String accountAbonementId,
            @RequestBody Map<String, String> requestBody
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(createErrorResponse("Account not found"), HttpStatus.BAD_REQUEST);
        }

        AccountAbonement accountAbonement = accountAbonementRepository.findByIdAndPersonalAccountId(accountAbonementId, account.getId());

        if(accountAbonement == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        Boolean autorenew = requestBody.get("autorenew") != null ? Boolean.valueOf(requestBody.get("autorenew")) : false;

        accountAbonement.setAutorenew(autorenew);

        accountAbonementRepository.save(accountAbonement);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountAbonementId}", method = RequestMethod.DELETE)
    public ResponseEntity<SimpleServiceMessage> deleteAccountAbonement(
            @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "accountAbonementId") String accountAbonementId
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(createErrorResponse("Account not found"), HttpStatus.BAD_REQUEST);
        }

        AccountAbonement accountAbonement = accountAbonementRepository.findByIdAndPersonalAccountId(accountAbonementId, account.getId());

        if(accountAbonement == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        accountAbonementRepository.delete(accountAbonement);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<Page<AccountAbonement>> getAccountAbonements(
            @PathVariable(value = "accountId") String accountId,
            Pageable pageable
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        Page<AccountAbonement> accountAbonements = accountAbonementRepository.findByPersonalAccountId(account.getId(), pageable);

        if(accountAbonements == null || !accountAbonements.hasContent()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(accountAbonements, HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<SimpleServiceMessage> addAccountAbonement(
            @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, String> requestBody
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(createErrorResponse("Account not found"), HttpStatus.BAD_REQUEST);
        }

        Plan plan = planRepository.findOne(account.getPlanId());

        if(plan == null){
            return new ResponseEntity<>(createErrorResponse("Plan not found"), HttpStatus.BAD_REQUEST);
        }

        String abonementId = requestBody.get("abonementId");
        Boolean autorenew = requestBody.get("autorenew") != null ? Boolean.valueOf(requestBody.get("autorenew")) : false;

        if (!plan.getAbonementIds().contains(abonementId)) {
            return new ResponseEntity<>(createErrorResponse("Yor plan does not have such abonement"), HttpStatus.BAD_REQUEST);
        }

        Optional<Abonement> newAbonement = plan.getAbonements().stream().filter(abonement1 -> abonement1.getId().equals(abonementId)).findFirst();

        if (!newAbonement.isPresent()) {
            return new ResponseEntity<>(createErrorResponse("Yor plan does not have such abonement"), HttpStatus.BAD_REQUEST);
        }

        Abonement abonement = newAbonement.get();

        List<AccountAbonement> accountAbonements = accountAbonementRepository.findByPersonalAccountId(account.getId());

        if(accountAbonements != null && !accountAbonements.isEmpty()){
            return new ResponseEntity<>(createErrorResponse("You already has abonement"), HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> paymentOperation = new HashMap<>();
        paymentOperation.put("serviceId", abonement.getServiceId());
        paymentOperation.put("amount", abonement.getService().getCost());

        Map<String, Object> response = finFeignClient.charge(account.getId(), paymentOperation);

        if (response.get("success") != null && !((boolean) response.get("success"))) {
            return new ResponseEntity<>(createErrorResponse("Could not charge money"), HttpStatus.BAD_REQUEST);
        }

        AccountAbonement accountAbonement = new AccountAbonement();
        accountAbonement.setAbonementId(abonementId);
        accountAbonement.setPersonalAccountId(account.getId());
        accountAbonement.setCreated(LocalDateTime.now());
        accountAbonement.setExpired(LocalDateTime.now().plus(Period.parse(abonement.getPeriod())));
        accountAbonement.setAutorenew(autorenew);

        accountAbonementRepository.save(accountAbonement);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}