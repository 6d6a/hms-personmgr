package ru.majordomo.hms.personmgr.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.SeoType;
import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.config.ImportProfile;
import ru.majordomo.hms.personmgr.model.seo.Seo;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.SeoRepository;

import static ru.majordomo.hms.personmgr.common.Constants.SEO_AUDIT_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.Constants.SEO_AUDIT_SERVICE_PREFIX;
import static ru.majordomo.hms.personmgr.common.Constants.SEO_CONTEXT_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.Constants.SEO_CONTEXT_SERVICE_PREFIX;

/**
 * PlanDBImportService
 */
@Service
@ImportProfile
public class SeoServiceDBSeedService {
    private final static Logger logger = LoggerFactory.getLogger(SeoServiceDBSeedService.class);

    private PaymentServiceRepository paymentServiceRepository;
    private SeoRepository seoRepository;

    @Autowired
    public SeoServiceDBSeedService(PaymentServiceRepository paymentServiceRepository, SeoRepository seoRepository) {
        this.paymentServiceRepository = paymentServiceRepository;
        this.seoRepository = seoRepository;
    }

    public boolean seedDB() {
        List<Seo> seos = seoRepository.findAll();

        if (seos != null) {
            for (Seo seo : seos) {
                if (seo.getService() != null) {
                    paymentServiceRepository.delete(seo.getService());
                }
            }
        }

        seoRepository.deleteAll();

        this.seedSeoService();

        return true;
    }

    private void seedSeoService() {
        //"SEO-?????????? ??????????"
        Seo seo = new Seo();
        seo.setName("SEO-?????????? ??????????");
        seo.setType(SeoType.AUDIT);
        seo.setId(SEO_AUDIT_SERVICE_ID);

        PaymentService paymentService = new PaymentService();
        paymentService.setPaymentType(ServicePaymentType.ONE_TIME);
        paymentService.setAccountType(AccountType.VIRTUAL_HOSTING);
        paymentService.setActive(true);
        paymentService.setCost(new BigDecimal("3990"));
        paymentService.setLimit(1);
        paymentService.setOldId(SEO_AUDIT_SERVICE_PREFIX + "1");
        paymentService.setName("SEO-?????????? ??????????");

        paymentServiceRepository.save(paymentService);

        logger.debug(paymentService.toString());

        seo.setServiceId(paymentService.getId());

        seoRepository.save(seo);

        //"?????????????????? ?????????????????????? ??????????????"
        seo = new Seo();
        seo.setName("?????????????????? ?????????????????????? ??????????????");
        seo.setType(SeoType.CONTEXT);
        seo.setId(SEO_CONTEXT_SERVICE_ID);

        paymentService = new PaymentService();
        paymentService.setPaymentType(ServicePaymentType.ONE_TIME);
        paymentService.setAccountType(AccountType.VIRTUAL_HOSTING);
        paymentService.setActive(true);
        paymentService.setCost(new BigDecimal("4990"));
        paymentService.setLimit(1);
        paymentService.setOldId(SEO_CONTEXT_SERVICE_PREFIX + "1");
        paymentService.setName("?????????????????? ?????????????????????? ??????????????");

        paymentServiceRepository.save(paymentService);

        logger.debug(paymentService.toString());

        seo.setServiceId(paymentService.getId());

        seoRepository.save(seo);
    }
}
