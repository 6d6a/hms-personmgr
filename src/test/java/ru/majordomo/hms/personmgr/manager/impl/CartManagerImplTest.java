package ru.majordomo.hms.personmgr.manager.impl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.common.message.destination.AmqpMessageDestination;
import ru.majordomo.hms.personmgr.common.message.destination.GenericMessageDestination;
import ru.majordomo.hms.personmgr.config.AppConfigTest;
import ru.majordomo.hms.personmgr.config.MongoConfigTest;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.manager.CartManager;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.cart.Cart;
import ru.majordomo.hms.personmgr.repository.CartRepository;
import ru.majordomo.hms.personmgr.service.DomainService;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;


@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                AppConfigTest.class,
                MongoConfigTest.class,
                CartManagerImpl.class
        }
)
@ActiveProfiles("test")
public class CartManagerImplTest {
    private final String cartId = "1";
    private final String accountId = "1";
    private final String domainName = "testsome.ru";

    @MockBean(name="domainService")
    private DomainService domainService;

    @MockBean(name="accountPromotionManager")
    private AccountPromotionManager accountPromotionManager;

    @Autowired
    private CartManager manager;

    @Autowired
    private CartRepository repository;

    @Before
    public void setUp() throws Exception {
        repository.insert(generateCart());

        Mockito
                .when(accountPromotionManager.findByPersonalAccountId(accountId))
                .thenReturn(new ArrayList<>());

        Mockito
                .doNothing()
                .when(accountPromotionManager).deactivateAccountPromotionByIdAndActionId(anyString(), anyString());

        Mockito
                .when(domainService.usePromotion(domainName, new ArrayList<>()))
                .thenReturn(null);

        Mockito
                .when(domainService.getPrice(domainName, null))
                .thenReturn(BigDecimal.valueOf(99));

        Mockito
                .doNothing()
                .when(domainService).check(domainName);

        ProcessingBusinessAction processingBusinessAction = new ProcessingBusinessAction(
                "1",
                "name",
                State.NEED_TO_PROCESS,
                1,
                "1",
                BusinessActionType.DOMAIN_CREATE_RC,
                new AmqpMessageDestination(),
                new SimpleServiceMessage(),
                accountId,
                LocalDateTime.now(),
                LocalDateTime.now(),
                new HashMap<>());

        Mockito
                .when(domainService.buy(accountId, domainName, new ArrayList<>(),null))
                .thenReturn(processingBusinessAction);
    }

    @After
    public void tearDown() throws Exception {
        repository.deleteAll();
    }

    @Test
    public void findOne() throws Exception {
        Cart cart = manager.findOne(cartId);

        Assert.assertNotNull(cart);
        Assert.assertEquals(cartId, cart.getId());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void findOneNotFound() {
        manager.findOne("3");
    }

    @Test
    public void findByPersonalAccountId() throws Exception {
        Cart cart = manager.findByPersonalAccountId(accountId);

        Assert.assertNotNull(cart);
        Assert.assertEquals(accountId, cart.getPersonalAccountId());
    }

    @Test
    public void addCartItem() throws Exception {
    }

    @Test
    public void deleteCartItemByName() throws Exception {
    }

    @Test
    public void setCartItems() throws Exception {
    }

    @Test
    public void setProcessing() throws Exception {
    }

    @Test
    public void setProcessingByName() throws Exception {
    }

    @Test
    public void buy() throws Exception {
    }

    private Cart generateCart() {
        Cart cart = new Cart();
        cart.setId(cartId);
        cart.setPersonalAccountId(accountId);

        return cart;
    }
}