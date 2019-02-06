package ru.majordomo.hms.personmgr.importing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.config.ImportProfile;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.repository.PromotionRepository;

import java.time.LocalDate;
import java.util.Collections;

import static ru.majordomo.hms.personmgr.common.Constants.BONUS_FREE_DOMAIN_PROMOCODE_ACTION_ID;
import static ru.majordomo.hms.personmgr.common.Constants.FREE_DOMAIN_PROMOTION;

@Service
@ImportProfile
public class PromotionDBSeedService {

    private PromotionRepository promotionRepository;

    @Autowired
    public PromotionDBSeedService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public boolean seedDB() {
        promotionRepository.deleteAll();

        this.seedPromotions();

        return true;
    }

    private void seedPromotions() {
        Promotion promotion = new Promotion();
        promotion.setName(FREE_DOMAIN_PROMOTION);
        promotion.setCreatedDate(LocalDate.now());
        promotion.setActive(true);
        promotion.setLimitPerAccount(1);
        promotion.setActionIds(Collections.singletonList(BONUS_FREE_DOMAIN_PROMOCODE_ACTION_ID));
        promotionRepository.save(promotion);
    }
}
