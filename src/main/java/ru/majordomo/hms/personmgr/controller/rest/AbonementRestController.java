package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.AbonementRepository;

@RestController
@RequestMapping("/{accountId}/abonements")
public class AbonementRestController extends CommonRestController {
    private final AbonementRepository abonementRepository;
    private final PlanManager planManager;

    @Autowired
    public AbonementRestController(
            AbonementRepository abonementRepository,
            PlanManager planManager) {
        this.abonementRepository = abonementRepository;
        this.planManager = planManager;
    }

    @RequestMapping(value = "/{abonementId}", method = RequestMethod.GET)
    public ResponseEntity<Abonement> getAbonement(
            @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "abonementId") String abonementId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        Abonement abonement = abonementRepository.findById(abonementId).orElse(null);

        if(abonement == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(abonement, HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<List<Abonement>> getAbonements(
            @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        Plan plan = planManager.findOne(account.getPlanId());

        if(plan == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        List<Abonement> abonements = plan.getAbonements();

        if(abonements == null || abonements.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(abonements, HttpStatus.OK);
    }
}