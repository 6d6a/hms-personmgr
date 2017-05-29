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
import ru.majordomo.hms.personmgr.config.AppConfigTest;
import ru.majordomo.hms.personmgr.config.MongoConfigTest;
import ru.majordomo.hms.personmgr.manager.AccountAbonementManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.repository.AccountAbonementRepository;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                AppConfigTest.class,
                MongoConfigTest.class,
                AccountAbonementManagerImpl.class
        }
)
@ActiveProfiles("test")
public class AccountAbonementManagerImplTest {
    @Autowired
    private AccountAbonementManager accountAbonementManager;

    @Autowired
    private AccountAbonementRepository repository;

    @Before
    public void setUp() throws Exception {
        accountAbonementManager.insert(generateAccountAbonement());
    }

    @After
    public void tearDown() throws Exception {
        repository.deleteAll();
    }

    @Test
    public void exists() throws Exception {
        Assert.assertTrue(accountAbonementManager.exists("1"));
    }

    @Test
    public void count() throws Exception {
        Assert.assertEquals(1, accountAbonementManager.count());
    }

    @Test(expected = OptimisticLockingFailureException.class)
    public void parallelSave() {
        AccountAbonement accountAbonement = accountAbonementManager.findOne("1");
        AccountAbonement accountAbonementOld = accountAbonementManager.findOne("1");

        accountAbonement.setPersonalAccountId("2");

        accountAbonementManager.save(accountAbonement);

        accountAbonementOld.setPersonalAccountId("3");

        accountAbonementManager.save(accountAbonementOld);
    }

    @Test
    public void findOne() throws Exception {
        AccountAbonement accountAbonement = accountAbonementManager.findOne("1");

        Assert.assertNotNull(accountAbonement);
        Assert.assertEquals("1", accountAbonement.getId());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void findOneNotFound() {
        accountAbonementManager.findOne("4");
    }

    @Test
    public void findAll() throws Exception {
        Assert.assertEquals(1, accountAbonementManager.findAll().size());
    }

    @Test
    public void findByAbonementId() throws Exception {
        String abonementId = "1";
        List<AccountAbonement> accountAbonements = accountAbonementManager.findByAbonementId(abonementId);
        Assert.assertEquals(1, accountAbonements.size());
        Assert.assertEquals(accountAbonements.get(0).getAbonementId(), abonementId);
    }

    @Test
    public void setExpired() throws Exception {
        String id = "1";
        LocalDateTime expired = LocalDateTime.now();

        accountAbonementManager.setExpired(id, expired);

        Assert.assertEquals(expired, accountAbonementManager.findOne(id).getExpired());
    }

    @Test
    public void setAutorenew() throws Exception {
        String id = "1";

        accountAbonementManager.setAutorenew(id, true);

        Assert.assertTrue(accountAbonementManager.findOne(id).isAutorenew());
    }

    private AccountAbonement generateAccountAbonement() {
        AccountAbonement accountAbonement = new AccountAbonement();
        accountAbonement.setId("1");
        accountAbonement.setAbonementId("1");
        accountAbonement.setPersonalAccountId("1");
        accountAbonement.setCreated(LocalDateTime.now());
        accountAbonement.setExpired(LocalDateTime.now().plus(Period.parse("P1Y")));
        accountAbonement.setAutorenew(false);

        return accountAbonement;
    }
}