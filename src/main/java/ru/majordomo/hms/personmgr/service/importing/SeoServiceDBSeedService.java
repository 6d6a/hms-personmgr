package ru.majordomo.hms.personmgr.service.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.FinService;
import ru.majordomo.hms.personmgr.common.SeoType;
import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.model.seo.Seo;
import ru.majordomo.hms.personmgr.repository.SeoRepository;
import ru.majordomo.hms.personmgr.service.FinFeignClient;

import static ru.majordomo.hms.personmgr.common.StringConstants.SEO_AUDIT_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.StringConstants.SEO_AUDIT_SERVICE_PREFIX;
import static ru.majordomo.hms.personmgr.common.StringConstants.SEO_CONTEXT_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.StringConstants.SEO_CONTEXT_SERVICE_PREFIX;

/**
 * PlanDBImportService
 */
@Service
public class SeoServiceDBSeedService {
    private final static Logger logger = LoggerFactory.getLogger(SeoServiceDBSeedService.class);

    private FinFeignClient finFeignClient;
    private SeoRepository seoRepository;

    @Autowired
    public SeoServiceDBSeedService(FinFeignClient finFeignClient, SeoRepository seoRepository) {
        this.finFeignClient = finFeignClient;
        this.seoRepository = seoRepository;
    }

    public boolean seedDB() {
        seoRepository.deleteAll();

        this.seedSeoService();

        return true;
    }

    private void seedSeoService() {
        //"SEO-аудит сайта"
        Seo seo = new Seo();
        seo.setName("SEO-аудит сайта");
        seo.setType(SeoType.AUDIT);
        seo.setId(SEO_AUDIT_SERVICE_ID);

        FinService finService = new FinService();
        finService.setPaymentType(ServicePaymentType.ONE_TIME);
        finService.setAccountType(AccountType.VIRTUAL_HOSTING);
        finService.setActive(true);
        finService.setCost(new BigDecimal("3990"));
        finService.setLimit(1);
        finService.setOldId(SEO_AUDIT_SERVICE_PREFIX + "1");
        finService.setName("SEO-аудит сайта");

        finService = finFeignClient.createService(finService);

        logger.info(finService.toString());

        seo.setFinServiceId(finService.getId());

        seoRepository.save(seo);

        //"Настройка контекстной рекламы"
        seo = new Seo();
        seo.setName("Настройка контекстной рекламы");
        seo.setType(SeoType.CONTEXT);
        seo.setId(SEO_CONTEXT_SERVICE_ID);

        finService = new FinService();
        finService.setPaymentType(ServicePaymentType.ONE_TIME);
        finService.setAccountType(AccountType.VIRTUAL_HOSTING);
        finService.setActive(true);
        finService.setCost(new BigDecimal("4990"));
        finService.setLimit(1);
        finService.setOldId(SEO_CONTEXT_SERVICE_PREFIX + "1");
        finService.setName("Настройка контекстной рекламы");

        finService = finFeignClient.createService(finService);

        logger.info(finService.toString());

        seo.setFinServiceId(finService.getId());

        seoRepository.save(seo);
    }
}
