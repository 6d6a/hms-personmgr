package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.PromocodeManager;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeTag;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.repository.PromoTagRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        @RequestBody Map<String, String> message
    ) {
        Promocode promocode = this.generatePromocodeByParams(message);

        Map<String, String> response = new HashMap<>();
        response.put("code", promocode.getCode());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @GetMapping("/tags")
    public List<PromocodeTag> getTags() {
        return promoTagRepository.findAll();
    }

    private Promocode generatePromocodeByParams(Map<String, String> message) {
        if (message.get("plan").equals("unlimited")) {
            if (message.get("period").equals("P1M")) {
                return promocodeManager.generatePromocodeUnlimitedOneMonth();
            }

            if (message.get("period").equals("P3M")) {
                return promocodeManager.generatePromocodeUnlimitedThreeMonth();
            }
        } else if (message.get("plan").equals("parking")) {
            if (message.get("period").equals("P3M")) {
                return promocodeManager.generatePromocodeParkingThreeMonth();
            }
        }

        throw new ParameterValidationException("Неверные параметры для генерации промокода");
    }

}
