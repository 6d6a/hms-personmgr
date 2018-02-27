package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.repository.PromotionRepository;

import java.util.List;

@RestController
@RequestMapping("/promotion")
public class PromotionRestController {
    private final PromotionRepository promotionRepository;

    @Autowired
    public PromotionRestController(
            PromotionRepository promotionRepository
    ) {
        this.promotionRepository = promotionRepository;
    }

    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<Promotion>> listAll() {
        return ResponseEntity.ok(promotionRepository.findAll());
    }
}
