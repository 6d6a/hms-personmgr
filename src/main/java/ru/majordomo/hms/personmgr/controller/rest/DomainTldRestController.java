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

import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.repository.DomainTldRepository;
import ru.majordomo.hms.personmgr.repository.PromocodeActionRepository;

import static ru.majordomo.hms.personmgr.common.Constants.DOMAIN_DISCOUNT_RU_RF_ACTION_ID;

@RestController
@RequestMapping({"/{accountId}/domain-tlds", "/domain-tlds"})
public class DomainTldRestController extends CommonRestController {

    private final DomainTldRepository repository;
    private final AccountPromotionManager accountPromotionManager;
    private final PromocodeActionRepository promocodeActionRepository;

    @Autowired
    public DomainTldRestController(
            DomainTldRepository repository,
            AccountPromotionManager accountPromotionManager,
            PromocodeActionRepository promocodeActionRepository
    )
    {
        this.repository = repository;
        this.accountPromotionManager = accountPromotionManager;
        this.promocodeActionRepository = promocodeActionRepository;
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<List<DomainTld>> listAll(
            @PathVariable(value = "accountId", required = false) String accountId
    ) {
        List<DomainTld> domainTlds = repository.findAllByActive(true);

        List<AccountPromotion> accountPromotions = accountPromotionManager.findByPersonalAccountId(accountId);
        Map<String, BigDecimal> discountedCosts = new HashMap<>();
        for (AccountPromotion accountPromotion : accountPromotions) {
            Map<String, Boolean> map = accountPromotion.getActionsWithStatus();
            if (map.get(DOMAIN_DISCOUNT_RU_RF_ACTION_ID) != null && map.get(DOMAIN_DISCOUNT_RU_RF_ACTION_ID) == true) {
                PromocodeAction promocodeAction = promocodeActionRepository.findOne(DOMAIN_DISCOUNT_RU_RF_ACTION_ID);
                List<String> availableTlds = (List<String>) promocodeAction.getProperties().get("tlds");
                for (String tld : availableTlds) {
                    discountedCosts.put(tld, BigDecimal.valueOf((Integer) promocodeAction.getProperties().get("cost")));
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