package ru.majordomo.hms.personmgr.manager.impl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.config.AppConfigTest;
import ru.majordomo.hms.personmgr.config.MongoConfigTest;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;


import static ru.majordomo.hms.personmgr.common.Constants.PLAN_UNLIMITED_SERVICE_ID;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                AppConfigTest.class,
                MongoConfigTest.class,
                PersonalAccountManagerImpl.class
        }
)
@ActiveProfiles("test")
public class PersonalAccountManagerImplTest {
    @Autowired
    private PersonalAccountManager accountManager;

    @Autowired
    private PersonalAccountRepository repository;

    @Before
    public void setUp() throws Exception {
        repository.insert(generateActivePersonalAccount());
        repository.insert(generateInactivePersonalAccount());
    }

    @After
    public void tearDown() throws Exception {
        repository.deleteAll();
    }

    @Test
    public void setActiveTrue() throws Exception {
        accountManager.setActive("2", true);

        PersonalAccount account = accountManager.findOne("2");
        Assert.assertTrue(account.isActive());
        Assert.assertNull(account.getDeactivated());
    }

    @Test
    public void setActiveFalse() throws Exception {
        accountManager.setActive("1", false);

        PersonalAccount account = accountManager.findOne("1");
        Assert.assertFalse(account.isActive());
        Assert.assertNotNull(account.getDeactivated());
    }

    @Test
    public void setAccountNewFalse() throws Exception {
        accountManager.setAccountNew("2", false);

        PersonalAccount account = accountManager.findOne("2");
        Assert.assertFalse(account.isAccountNew());
    }

    @Test
    public void setAccountNewTrue() throws Exception {
        accountManager.setAccountNew("2", true);

        PersonalAccount account = accountManager.findOne("2");
        Assert.assertTrue(account.isAccountNew());
    }

    @Test(expected = OptimisticLockingFailureException.class)
    public void parallelSave() {
        PersonalAccount account = accountManager.findOne("1");
        PersonalAccount accountOld = accountManager.findOne("1");

        account.setCredit(true);

        accountManager.save(account);

        accountOld.setAddQuotaIfOverquoted(true);

        accountManager.save(accountOld);
    }

    @Test
    public void findOne() {
        PersonalAccount account = accountManager.findOne("1");

        Assert.assertNotNull(account);
        Assert.assertEquals("1", account.getId());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void findOneNotFound() {
        accountManager.findOne("3");
    }

    @Test
    public void setOwnerPersonId() throws Exception {
        accountManager.setOwnerPersonId("1", "2");

        PersonalAccount account = accountManager.findOne("1");
        Assert.assertEquals("2", account.getOwnerPersonId());
    }

    private PersonalAccount generateActivePersonalAccount() {
        return generateActivePersonalAccount("1");
    }

    private PersonalAccount generateActivePersonalAccount(String id) {
        PersonalAccount account = new PersonalAccount();
        account.setId(id);
        account.setAccountType(AccountType.VIRTUAL_HOSTING);
        account.setPlanId(PLAN_UNLIMITED_SERVICE_ID);
        account.setAccountId(id);
        account.setClientId(id);
        account.setName("AC_" + id);
        account.setActive(true);
        account.setCreated(LocalDateTime.now());

        account.setNotifyDays(14);
        account.setCredit(false);
        account.setAutoBillSending(false);
        account.setOverquoted(false);
        account.setAddQuotaIfOverquoted(false);
        account.setAccountNew(false);
        account.setCreditPeriod("P14D");

        return account;
    }

    private PersonalAccount generateInactivePersonalAccount() {
        return generateInactivePersonalAccount("2");
    }

    private PersonalAccount generateInactivePersonalAccount(String id) {
        PersonalAccount account = new PersonalAccount();
        account.setId(id);
        account.setAccountType(AccountType.VIRTUAL_HOSTING);
        account.setPlanId(PLAN_UNLIMITED_SERVICE_ID);
        account.setAccountId(id);
        account.setClientId(id);
        account.setName("AC_" + id);
        account.setActive(false);
        account.setDeactivated(LocalDateTime.now());
        account.setCreated(LocalDateTime.now());

        account.setNotifyDays(14);
        account.setCredit(false);
        account.setAutoBillSending(false);
        account.setOverquoted(false);
        account.setAddQuotaIfOverquoted(false);
        account.setAccountNew(true);
        account.setCreditPeriod("P14D");

        return account;
    }
}