package ru.majordomo.hms.personmgr.service.promotion;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.majordomo.hms.personmgr.config.AppConfigTest;
import ru.majordomo.hms.personmgr.config.MongoConfigTest;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;

import java.time.LocalDate;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                AppConfigTest.class,
                MongoConfigTest.class,
                AccountPromotionFactory.class
        }
)
@ActiveProfiles("test")
public class AccountPromotionFactoryTest {
    @Autowired
    AccountPromotionFactory accountPromotionFactory;

    @Test
    public void buildWithValidUntilPeriod() {
        PersonalAccount account = new PersonalAccount();
        account.setId("accountId");

        Promotion promotion = new Promotion();
        promotion.setId("promotionId");

        PromocodeAction action = new PromocodeAction();
        action.setId("actionId");
        action.getProperties().put("validPeriod", "P1Y");

        AccountPromotion accountPromotion = accountPromotionFactory.build(account, promotion, action);

        assertNotNull(accountPromotion);
        assertTrue(accountPromotion.isValidNow());
        assertNotNull(accountPromotion.getValidUntil());
        assertEquals(accountPromotion.getValidUntil().toLocalDate(), LocalDate.now().plusYears(1));
        assertSame(accountPromotion.getAction(), action);
        assertSame(accountPromotion.getPromotion(), promotion);
    }
}