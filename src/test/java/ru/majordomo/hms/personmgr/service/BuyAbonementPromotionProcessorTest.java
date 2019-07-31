package ru.majordomo.hms.personmgr.service;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.majordomo.hms.personmgr.common.AccountSetting;
import ru.majordomo.hms.personmgr.common.PromocodeActionType;
import ru.majordomo.hms.personmgr.config.AppConfigTest;
import ru.majordomo.hms.personmgr.config.MongoConfigTest;
import ru.majordomo.hms.personmgr.event.account.AccountBuyAbonement;
import ru.majordomo.hms.personmgr.manager.*;
import ru.majordomo.hms.personmgr.manager.impl.AccountPromotionManagerImpl;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.repository.AccountNoticeRepository;
import ru.majordomo.hms.personmgr.repository.AccountPromotionRepository;
import ru.majordomo.hms.personmgr.repository.PromocodeActionRepository;
import ru.majordomo.hms.personmgr.repository.PromotionRepository;
import ru.majordomo.hms.personmgr.service.promotion.AccountPromotionFactory;
import ru.majordomo.hms.rc.user.resources.Domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                AppConfigTest.class,
                MongoConfigTest.class,
                BuyAbonementPromotionProcessor.class,
                AccountPromotionManagerImpl.class,
                GiftHelper.class,
                AccountPromotionFactory.class
        }
)
@ActiveProfiles("test")
public class BuyAbonementPromotionProcessorTest {
    @MockBean
    private PersonalAccountManager accountManager;
    @MockBean
    private PlanManager planManager;
    @MockBean
    private AbonementManager<AccountAbonement> accountAbonementManager;
    @MockBean
    private AccountHelper accountHelper;
    @MockBean(name = "history")
    private AccountHistoryManager history;
    @MockBean
    private PromotionRepository promotionRepository;
    @MockBean
    private PromocodeActionRepository actionRepository;
    @Autowired
    private AccountNoticeRepository accountNoticeRepository;
    @Autowired
    private AccountPromotionManager accountPromotionManager;
    @Autowired
    private AccountPromotionRepository accountPromotionRepository;
    @Autowired
    private BuyAbonementPromotionProcessor promotionProcessor;

    private final Promotion renewPromotion = generatePromotion(
            "free_renew_domain", generateAction(PromocodeActionType.SERVICE_DISCOUNT), -1
    );
    private final Promotion regPromotion = generatePromotion(
            "free_domain", generateAction(PromocodeActionType.SERVICE_FREE_DOMAIN), 1
    );
    private final String planId = "32156216planId";
    private final String accountId = "123account";
    private final String accountAbonementId = "abonementidjasdjgId";

    @Before
    public void before() {
        Mockito.when(promotionRepository.findByName("free_domain")).thenReturn(regPromotion);
        Mockito.when(promotionRepository.findByName("free_renew_domain")).thenReturn(renewPromotion);
    }

    @After
    public void after() {
        accountPromotionRepository.deleteAll();
    }

    @Test
    public void notAddPromotionIfPlanInactive() {
        mockPlan(false, false);
        mockAccount(true);
        mockAbonement(true, "P1Y");
        mockGetDomains(0);

        promotionProcessor.accept(() -> new AccountBuyAbonement(accountId, "abonement123"));

        assertFalse(accountPromotionManager.existsByPersonalAccountIdAndPromotionId(accountId, renewPromotion.getId()));
        assertFalse(accountPromotionManager.existsByPersonalAccountIdAndPromotionId(accountId, regPromotion.getId()));
    }

    @Test
    public void notAddPromotionIfPlanActiveAndAbonementOnly() {
        mockPlan(true, true);
        mockAccount(true);
        mockAbonement(true, "P1Y");
        mockGetDomains(0);

        promotionProcessor.accept(() -> new AccountBuyAbonement(accountId, "abonement123"));

        assertFalse(accountPromotionManager.existsByPersonalAccountIdAndPromotionId(accountId, renewPromotion.getId()));
        assertFalse(accountPromotionManager.existsByPersonalAccountIdAndPromotionId(accountId, regPromotion.getId()));
    }

    @Test
    public void notAddPromotionIfAbonementInternal() {
        mockPlan(true, false);
        mockAccount(true);
        mockAbonement(true, "P1Y");
        mockGetDomains(0);

        promotionProcessor.accept(() -> new AccountBuyAbonement(accountId, accountAbonementId));

        assertFalse(accountPromotionManager.existsByPersonalAccountIdAndPromotionId(accountId, renewPromotion.getId()));
        assertFalse(accountPromotionManager.existsByPersonalAccountIdAndPromotionId(accountId, regPromotion.getId()));
    }

    @Test
    public void notAddPromotionIfLowerPeriod() {
        mockPlan(true, false);
        mockAccount(true);
        mockAbonement(false, "P1M");
        mockGetDomains(0);

        promotionProcessor.accept(() -> new AccountBuyAbonement(accountId, accountAbonementId));

        assertFalse(accountPromotionManager.existsByPersonalAccountIdAndPromotionId(accountId, renewPromotion.getId()));
        assertFalse(accountPromotionManager.existsByPersonalAccountIdAndPromotionId(accountId, regPromotion.getId()));
    }

    @Test
    public void notAddPromotionIfHasDomains() {
        mockPlan(true, false);
        mockAccount(true);
        mockAbonement(false, "P3M");
        mockGetDomains(1);

        promotionProcessor.accept(() -> new AccountBuyAbonement(accountId, accountAbonementId));

        assertFalse(accountPromotionManager.existsByPersonalAccountIdAndPromotionId(accountId, renewPromotion.getId()));
        assertFalse(accountPromotionManager.existsByPersonalAccountIdAndPromotionId(accountId, regPromotion.getId()));
    }

    @Test
    public void addPromotionIfHasNotDomains() {
        mockPlan(true, false);
        mockAccount(true);
        mockAbonement(false, "P3M");
        mockGetDomains(0);

        assertFalse(accountPromotionManager.existsByPersonalAccountIdAndPromotionId(accountId, regPromotion.getId()));

        promotionProcessor.accept(() -> new AccountBuyAbonement(accountId, accountAbonementId));

        assertEquals(Long.valueOf(1), accountPromotionManager.countByPersonalAccountIdAndPromotionIdAndActionId(
                accountId, regPromotion.getId(), regPromotion.getActionIds().get(0)
        ));
        assertFalse(accountPromotionManager.existsByPersonalAccountIdAndPromotionId(accountId, renewPromotion.getId()));
    }

    @Test
    public void addOnlyOnePromotion() {
        mockPlan(true, false);
        mockAccount(true);
        mockAbonement(false, "P3M");
        mockGetDomains(0);

        assertFalse(accountPromotionManager.existsByPersonalAccountIdAndPromotionId(accountId, regPromotion.getId()));

        promotionProcessor.accept(() -> new AccountBuyAbonement(accountId, accountAbonementId));
        promotionProcessor.accept(() -> new AccountBuyAbonement(accountId, accountAbonementId));

        assertEquals(Long.valueOf(1), accountPromotionManager.countByPersonalAccountIdAndPromotionIdAndActionId(
                        accountId, regPromotion.getId(), regPromotion.getActionIds().get(0)
        ));
        assertFalse(accountPromotionManager.existsByPersonalAccountIdAndPromotionId(accountId, renewPromotion.getId()));
    }

    @Test
    public void addOneOfFreeRegistrationAndThreeMaxOfFreeRenewPromotion() {
        mockPlan(true, false);
        mockAccount(true);
        mockAbonement(false, "P3M");
        mockGetDomains(0);

        assertFalse(accountPromotionManager.existsByPersonalAccountIdAndPromotionId(accountId, regPromotion.getId()));

        promotionProcessor.accept(() -> new AccountBuyAbonement(accountId, accountAbonementId));

        mockAbonement(false, "P1Y");
        mockAccount(false);
        mockGetDomains(10);

        promotionProcessor.accept(() -> new AccountBuyAbonement(accountId, accountAbonementId));
        promotionProcessor.accept(() -> new AccountBuyAbonement(accountId, accountAbonementId));
        promotionProcessor.accept(() -> new AccountBuyAbonement(accountId, accountAbonementId));
        promotionProcessor.accept(() -> new AccountBuyAbonement(accountId, accountAbonementId));

        assertEquals(Long.valueOf(1), accountPromotionManager.countByPersonalAccountIdAndPromotionIdAndActionId(
                accountId, regPromotion.getId(), regPromotion.getActionIds().get(0)
        ));

        assertEquals(Long.valueOf(4), accountPromotionManager.countByPersonalAccountIdAndPromotionIdAndActionId(
                accountId, renewPromotion.getId(), renewPromotion.getActionIds().get(0)
        ));
    }

    private void mockGetDomains(int domainsCount) {
        List<Domain> domains = new ArrayList<>();
        while (domainsCount-- > 0) {
            domains.add(new Domain());
        }
        Mockito.when(accountHelper.getDomains(any(PersonalAccount.class))).thenReturn(domains);
    }

    private Plan generatePlan(boolean active, boolean abonementOnly) {
        Plan plan = new Plan();
        plan.setActive(active);
        plan.setAbonementOnly(abonementOnly);
        plan.setOldId("oldId");
        return plan;
    }

    private Promotion generatePromotion(String name, PromocodeAction action, int limit) {
        Promotion promotion = new Promotion();
        promotion.setId(ObjectId.get().toString());
        promotion.setName(name);
        promotion.setActionIds(Collections.singletonList(action.getId()));
        promotion.setActions(Collections.singletonList(action));
        promotion.setLimitPerAccount(limit);

        return promotion;
    }

    private PromocodeAction generateAction(PromocodeActionType type) {
        PromocodeAction action = new PromocodeAction();
        action.setId(ObjectId.get().toString());
        action.setActionType(type);
        return action;
    }

    private PersonalAccount generateAccount(boolean isNew, String planId) {
        PersonalAccount account = new PersonalAccount();
        account.setId(accountId);
        account.setPlanId(planId);
        account.getSettings().put(AccountSetting.NEW_ACCOUNT, isNew ? "true" : "false");
        return account;
    }

    private Abonement abonement(boolean internal, String period) {
        Abonement abonement = new Abonement();
        abonement.setInternal(internal);
        abonement.setPeriod(period);
        abonement.setType(Feature.VIRTUAL_HOSTING_PLAN);
        return abonement;
    }

    private AccountAbonement accountAbonement(Abonement abonement) {
        AccountAbonement accountAbonement = new AccountAbonement();
        accountAbonement.setAbonement(abonement);
        return accountAbonement;
    }

    private void mockAbonement(boolean internal, String period) {
        Mockito.when(accountAbonementManager.findByIdAndPersonalAccountId(accountAbonementId, accountId))
                .thenReturn(accountAbonement(abonement(internal, period)));

    }

    private void mockPlan(boolean active, boolean abonementOnly) {
        Mockito.when(planManager.findOne(planId)).thenReturn(generatePlan(active, abonementOnly));
    }

    private void mockAccount(boolean isNew) {
        Mockito.when(accountManager.findOne(accountId)).thenReturn(generateAccount(isNew, planId));
    }
}