package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.PromocodeActionType;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.repository.DomainTldRepository;

@RestController
@RequestMapping({"/{accountId}/domain-tlds", "/domain-tlds"})
public class DomainTldRestController extends CommonRestController {

    private final DomainTldRepository repository;
    private final AccountPromotionManager accountPromotionManager;

    @Autowired
    public DomainTldRestController(
            DomainTldRepository repository,
            AccountPromotionManager accountPromotionManager
    )
    {
        this.repository = repository;
        this.accountPromotionManager = accountPromotionManager;
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<List<DomainTld>> listAll(
            @PathVariable(value = "accountId", required = false) String accountId
    ) {
        List<DomainTld> domainTlds = repository.findAll();

        List<AccountPromotion> accountPromotions = accountPromotionManager.findByPersonalAccountId(accountId);
        Map<String, BigDecimal> discountedCosts = new HashMap<>();
        for (AccountPromotion accountPromotion : accountPromotions) {
            PromocodeAction action = accountPromotion.getAction();
            if (accountPromotion.isValidNow() && action.getActionType() == PromocodeActionType.SERVICE_DOMAIN_DISCOUNT_RU_RF) {
                List<String> availableTlds = (List<String>) action.getProperties().get("tlds");
                for (String tld : availableTlds) {
                    discountedCosts.put(tld, BigDecimal.valueOf((Integer) action.getProperties().get("cost")));
                }
                break;
            }
        }

        if (!discountedCosts.isEmpty()) {
            for (DomainTld domainTld : domainTlds) {
                if (discountedCosts.containsKey(domainTld.getTld())) {
                    domainTld.getRegistrationService().setCost(discountedCosts.get(domainTld.getTld()));
                }
            }
        }

        if(domainTlds.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(domainTlds, HttpStatus.OK);
    }
}