package ru.majordomo.hms.personmgr.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.majordomo.hms.personmgr.common.PromocodeActionType;
import ru.majordomo.hms.personmgr.config.AppConfigTest;
import ru.majordomo.hms.personmgr.config.MongoConfigTest;
import ru.majordomo.hms.personmgr.feign.DomainRegistrarFeignClient;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PromotionRepository;
import ru.majordomo.hms.personmgr.service.promotion.AccountPromotionFactory;

import java.time.LocalDateTime;
import java.util.Collections;

import static ru.majordomo.hms.personmgr.common.Constants.FREE_DOMAIN_PROMOTION;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                AppConfigTest.class,
                MongoConfigTest.class,
                DomainService.class
        }
)
@ActiveProfiles("test")
public class DomainServiceTest {
    @Autowired
    private DomainService domainService;

    @MockBean
    private PromotionRepository promotionRepository;
    @MockBean
    private DomainTldService domainTldService;
    @MockBean
    private RcUserFeignClient rcUserFeignClient;
    @MockBean
    private AccountHelper accountHelper;
    @MockBean
    private DiscountFactory discountFactory;
    @MockBean
    private DomainRegistrarFeignClient domainRegistrarFeignClient;
    @MockBean
    private BlackListService blackListService;
    @MockBean
    private PersonalAccountManager accountManager;
    @MockBean
    private AccountPromotionManager accountPromotionManager;
    @MockBean
    private AccountNotificationHelper accountNotificationHelper;
    @MockBean
    private BusinessHelper businessHelper;
    @MockBean
    private AccountHistoryManager history;
    @MockBean
    private PaymentServiceRepository paymentServiceRepository;
    @MockBean
    private AccountPromotionFactory accountPromotionFactory;

    @Before
    public void before() {
        Mockito.when(domainTldService.findActiveDomainTldByDomainName("domain.ru")).thenReturn(generateRuDomainTld());
        Mockito.when(promotionRepository.findByName(FREE_DOMAIN_PROMOTION)).thenReturn(generateFreeDomainPromotion());
    }

    private Promotion generateFreeDomainPromotion() {
        Promotion promotion = new Promotion();
        promotion.setId("prioritizedPromotionId");
        promotion.setName(FREE_DOMAIN_PROMOTION);
        return promotion;
    }

    private DomainTld generateRuDomainTld() {
        DomainTld domainTld = new DomainTld();
        domainTld.setTld("ru");
        return domainTld;
    }

    private AccountPromotion generateActiveAccountPromotion() {
        AccountPromotion accountPromotion = new AccountPromotion();
        accountPromotion.setPromotionId("any");
        accountPromotion.setAction(generateRuRfFreeDomainPromocodeAction());
        accountPromotion.setActive(true);
        accountPromotion.setValidUntil(LocalDateTime.now().plusDays(1));
        return accountPromotion;
    }

    private PromocodeAction generateRuRfFreeDomainPromocodeAction() {
        PromocodeAction action = new PromocodeAction();
        action.getProperties().put("tlds", Collections.singletonList("ru"));
        action.setActionType(PromocodeActionType.SERVICE_FREE_DOMAIN);
        return action;
    }

    @Test
    public void usePromotionValidNowAccountPromotion() {
        AccountPromotion accountPromotion = generateActiveAccountPromotion();
        Assert.assertTrue(accountPromotion.getActive());
        AccountPromotion usedPromotion = domainService.usePromotion("domain.ru", Collections.singletonList(accountPromotion));
        Assert.assertNotNull(usedPromotion);
        Assert.assertFalse(usedPromotion.getActive());
        Assert.assertSame(accountPromotion, usedPromotion);
    }

    @Test
    public void useActivePromotionWithoutValidUntilAccountPromotion() {
        AccountPromotion accountPromotion = generateActiveAccountPromotion();
        Assert.assertTrue(accountPromotion.getActive());
        accountPromotion.setValidUntil(null);
        AccountPromotion usedPromotion = domainService.usePromotion("domain.ru", Collections.singletonList(accountPromotion));
        Assert.assertNotNull(usedPromotion);
        Assert.assertFalse(usedPromotion.getActive());
        Assert.assertSame(accountPromotion, usedPromotion);
    }

    @Test
    public void usePromotionInvalidNowAccountPromotion() {
        AccountPromotion accountPromotion = generateActiveAccountPromotion();
        accountPromotion.setValidUntil(LocalDateTime.now().minusDays(3));
        AccountPromotion usedPromotion = domainService.usePromotion("domain.ru", Collections.singletonList(accountPromotion));
        Assert.assertNull(usedPromotion);
    }
}