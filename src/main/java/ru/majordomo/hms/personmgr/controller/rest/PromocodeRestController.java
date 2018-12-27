package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.PromocodeType;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.PromocodeManager;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeTag;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.repository.PromoTagRepository;

import java.util.*;

import static ru.majordomo.hms.personmgr.common.Constants.*;

@RestController
@RequestMapping("/promocode")
public class PromocodeRestController {

    private final PromocodeManager promocodeManager;
    private final PromoTagRepository promoTagRepository;

    @Autowired
    public PromocodeRestController(
            PromocodeManager promocodeManager,
            PromoTagRepository promoTagRepository
    ){
        this.promocodeManager = promocodeManager;
        this.promoTagRepository = promoTagRepository;
    }

    @PreAuthorize("hasAuthority('CREATE_PROMOCODE')")
    @PostMapping("/bonus/generate")
    public ResponseEntity<Map<String, String>> generateNewPromocode(
        @RequestBody Map<String, String> message,
        Authentication authentication
    ) {
        PromocodeTag tag = getOrCreateTag(authentication.getName());

        Promocode example = getExampleBonusPromocode(message);

        example.setTagIds(Collections.singletonList(tag.getId()));

        Promocode promocode = promocodeManager.generatePromocode(example);

        Map<String, String> response = new HashMap<>();
        response.put("code", promocode.getCode());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @GetMapping("/tags")
    public List<PromocodeTag> getTags() {
        return promoTagRepository.findAll();
    }

    private Promocode getExampleBonusPromocode(Map<String, String> message) {
        Promocode example = new Promocode();
        example.setType(PromocodeType.BONUS);

        if (message.get("plan").equals("unlimited")) {
            if (message.get("period").equals("P1M")) {
                example.setActionIds(Collections.singletonList(BONUS_UNLIMITED_1_M_PROMOCODE_ACTION_ID));
                return example;
            }

            if (message.get("period").equals("P3M")) {
                example.setActionIds(Collections.singletonList(BONUS_UNLIMITED_3_M_PROMOCODE_ACTION_ID));
                return example;
            }
        } else if (message.get("plan").equals("parking")) {
            if (message.get("period").equals("P3M")) {
                example.setActionIds(Collections.singletonList(BONUS_PARKING_3_M_PROMOCODE_ACTION_ID));
                return example;
            }
        }
        throw new ParameterValidationException("Неверные параметры для генерации промокода");
    }

    private PromocodeTag getOrCreateTag(String internalName) {
        PromocodeTag tag = promoTagRepository.findOneByInternalName(internalName);
        if (tag == null) {
            tag = new PromocodeTag();
            tag.setName(internalName);
            tag.setInternalName(internalName);
            promoTagRepository.insert(tag);
        }
        return tag;
    }

}
