package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.service.PromocodeProcessor;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/promocode")
@PreAuthorize("hasRole('ADMIN')")
public class PromocodeRestController {

    private final PromocodeProcessor promocodeProcessor;

    @Autowired
    public PromocodeRestController(
        PromocodeProcessor promocodeProcessor
    ){
        this.promocodeProcessor = promocodeProcessor;
    }

    @PostMapping("/bonus/generate")
    public ResponseEntity<Map<String, String>> generateNewPromocode(
        @RequestBody Map<String, String> message
    ) {
        Promocode promocode = this.generatePromocodeByParams(message);

        Map<String, String> response = new HashMap<>();
        response.put("code", promocode.getCode());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    private Promocode generatePromocodeByParams(Map<String, String> message) {
        if (message.get("plan").equals("unlimeted")) {
            if (message.get("period").equals("P1M")) {
                return promocodeProcessor.generatePromocodeUnlimitedOneMonth();
            }

            if (message.get("period").equals("P3M")) {
                return promocodeProcessor.generatePromocodeUnlimitedThreeMonth();
            }
        } else if (message.get("plan").equals("parking")) {
            if (message.get("period").equals("P3M")) {
                return promocodeProcessor.generatePromocodeUnlimitedThreeMonth();
            }
        }

        throw new ParameterValidationException("Неверные параметры для генерации промокода");
    }

}
