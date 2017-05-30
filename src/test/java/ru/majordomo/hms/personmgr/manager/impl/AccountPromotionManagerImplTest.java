package ru.majordomo.hms.personmgr.manager.impl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.config.AppConfigTest;
import ru.majordomo.hms.personmgr.config.MongoConfigTest;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.repository.AccountPromotionRepository;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                AppConfigTest.class,
                MongoConfigTest.class,
                AccountPromotionManagerImpl.class
        }
)
@ActiveProfiles("test")
public class AccountPromotionManagerImplTest {
    @Autowired
    private AccountPromotionManager accountPromotionManager;

    @Autowired
    private AccountPromotionRepository repository;

    @Before
    public void setUp() throws Exception {
        accountPromotionManager.insert(generateAccountPromotionWithActiveAction());
        accountPromotionManager.insert(generateAccountPromotionWithActiveAction2());
        accountPromotionManager.insert(generateAccountPromotionWithInActiveAction());
    }

    @After
    public void tearDown() throws Exception {
        repository.deleteAll();
    }

    @Test
    public void exists() throws Exception {
        Assert.assertTrue(accountPromotionManager.exists("1"));
    }

    @Test
    public void count() throws Exception {
        Assert.assertEquals(3, accountPromotionManager.count());
    }

    @Test(expected = OptimisticLockingFailureException.class)
    public void parallelSave() {
        AccountPromotion accountPromotion = accountPromotionManager.findOne("1");
        AccountPromotion accountPromotionOld = accountPromotionManager.findOne("1");

        accountPromotion.setPersonalAccountId("2");

        accountPromotionManager.save(accountPromotion);

        accountPromotionOld.setPersonalAccountId("3");

        accountPromotionManager.save(accountPromotionOld);
    }

    @Test
    public void findOne() {
        AccountPromotion account = accountPromotionManager.findOne("1");

        Assert.assertNotNull(account);
        Assert.assertEquals("1", account.getId());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void findOneNotFound() {
        accountPromotionManager.findOne("4");
    }

    @Test
    public void findAll() throws Exception {
        Assert.assertEquals(3, accountPromotionManager.findAll().size());
    }

    @Test
    public void findByPersonalAccountId() throws Exception {
        String personalAccountId = "1";
        List<AccountPromotion> accountPromotions = accountPromotionManager.findByPersonalAccountId(personalAccountId);
        Assert.assertEquals(2, accountPromotions.size());
        Assert.assertEquals(accountPromotions.get(0).getPersonalAccountId(), personalAccountId);
    }

    @Test
    public void findByPersonalAccountIdAndPromotionId() throws Exception {
        String personalAccountId = "1";
        String promotionId = "2";
        List<AccountPromotion> accountPromotions = accountPromotionManager.findByPersonalAccountIdAndPromotionId(
                personalAccountId,
                promotionId
        );
        Assert.assertEquals(1, accountPromotions.size());
        Assert.assertEquals(accountPromotions.get(0).getPersonalAccountId(), personalAccountId);
        Assert.assertEquals(accountPromotions.get(0).getPromotionId(), promotionId);
    }

    @Test
    public void countByPersonalAccountIdAndPromotionId() throws Exception {
        String personalAccountId = "1";
        String promotionId = "2";
        Assert.assertTrue(accountPromotionManager.countByPersonalAccountIdAndPromotionId(
                personalAccountId,
                promotionId
        ) == 1);
    }

    @Test
    public void activateAccountPromotionByIdAndActionId() throws Exception {
        String promotionId = "2";
        String actionId = "3";

        accountPromotionManager.activateAccountPromotionByIdAndActionId(promotionId, actionId);

        Assert.assertTrue(accountPromotionManager.findOne(promotionId).getActionsWithStatus().get(actionId));
    }

    @Test
    public void deactivateAccountPromotionByIdAndActionId() throws Exception {
        String promotionId = "1";
        String actionId = "3";

        accountPromotionManager.deactivateAccountPromotionByIdAndActionId(promotionId, actionId);

        Assert.assertFalse(accountPromotionManager.findOne(promotionId).getActionsWithStatus().get(actionId));
    }

    private AccountPromotion generateAccountPromotionWithActiveAction() {
        AccountPromotion accountPromotion = new AccountPromotion();
        accountPromotion.setId("1");
        accountPromotion.setPersonalAccountId("1");
        accountPromotion.setPromotionId("2");
        accountPromotion.setCreated(LocalDateTime.now());

        Map<String, Boolean> actionsWithStatus = new HashMap<>();
        actionsWithStatus.put("3", true);

        accountPromotion.setActionsWithStatus(actionsWithStatus);

        return accountPromotion;
    }

    private AccountPromotion generateAccountPromotionWithActiveAction2() {
        AccountPromotion accountPromotion = new AccountPromotion();
        accountPromotion.setId("3");
        accountPromotion.setPersonalAccountId("1");
        accountPromotion.setPromotionId("4");
        accountPromotion.setCreated(LocalDateTime.now());

        Map<String, Boolean> actionsWithStatus = new HashMap<>();
        actionsWithStatus.put("3", true);

        accountPromotion.setActionsWithStatus(actionsWithStatus);

        return accountPromotion;
    }

    private AccountPromotion generateAccountPromotionWithInActiveAction() {
        AccountPromotion accountPromotion = new AccountPromotion();
        accountPromotion.setId("2");
        accountPromotion.setPersonalAccountId("2");
        accountPromotion.setPromotionId("2");
        accountPromotion.setCreated(LocalDateTime.now());

        Map<String, Boolean> actionsWithStatus = new HashMap<>();
        actionsWithStatus.put("3", false);

        accountPromotion.setActionsWithStatus(actionsWithStatus);

        return accountPromotion;
    }
}