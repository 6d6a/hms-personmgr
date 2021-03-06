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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.common.message.destination.AmqpMessageDestination;
import ru.majordomo.hms.personmgr.config.AppConfigTest;
import ru.majordomo.hms.personmgr.config.MongoConfigTest;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.manager.CartManager;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.cart.Cart;
import ru.majordomo.hms.personmgr.model.cart.CartItem;
import ru.majordomo.hms.personmgr.model.cart.DomainCartItem;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.repository.CartRepository;
import ru.majordomo.hms.personmgr.service.DomainService;
import ru.majordomo.hms.rc.user.resources.Person;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
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
    private final String emptyCartId = "1";
    private final String notEmptyCartId = "2";
    private final String processingCartId = "3";

    private final String accountId1 = "1";
    private final String accountId2 = "2";
    private final String accountId3 = "3";

    private final String domainName1 = "testsome.ru";
    private final String domainName2 = "testsome2.ru";
    private final String domainName3 = "testsome3.ru";

    private final BigDecimal price = BigDecimal.valueOf(99);

    @MockBean(name="domainService")
    private DomainService domainService;

    @MockBean(name="accountPromotionManager")
    private AccountPromotionManager accountPromotionManager;

    @MockBean(name="rcUserFeignClient")
    private RcUserFeignClient rcUserFeignClient;

    @Autowired
    private CartManager manager;

    @Autowired
    private CartRepository repository;

    @Before
    public void setUp() throws Exception {
        repository.insert(generateEmptyCart());
        repository.insert(generateNotEmptyCart());
        repository.insert(generateProcessingCart());

        Mockito
                .when(accountPromotionManager.findByPersonalAccountId(accountId1))
                .thenReturn(new ArrayList<>());

        Mockito
                .doNothing()
                .when(accountPromotionManager).setAsUsedAccountPromotionById(anyString());

        Mockito
                .when(domainService.usePromotion(domainName1, new ArrayList<>()))
                .thenReturn(null);

        Mockito
                .when(domainService.getPrice(domainName1, null))
                .thenReturn(price);

        Mockito
                .doNothing()
                .when(domainService).check(domainName1, accountId1);

        Mockito
                .when(domainService.buy(anyString(), any(DomainCartItem.class), anyListOf(AccountPromotion.class), any()))
                .thenAnswer(invocation -> generateProcessingBusinessAction(
                        invocation.getArgument(0),
                        invocation.getArgument(1)
                ))
        ;
        Mockito.when(rcUserFeignClient.getPersons(anyString())).thenAnswer(invocation -> {
            Person person = new Person();
            person.setId("1");
            person.setAccountId(invocation.getArgument(0));
            return Collections.singletonList(person);
        });
    }

    @After
    public void tearDown() throws Exception {
        repository.deleteAll();
    }

    @Test
    public void findOne() throws Exception {
        Cart cart = manager.findOne(emptyCartId);

        Assert.assertNotNull(cart);
        Assert.assertEquals(emptyCartId, cart.getId());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void findOneNotFound() {
        manager.findOne("3324253");
    }

    @Test
    public void findByPersonalAccountId() throws Exception {
        Cart cart = manager.findByPersonalAccountId(accountId1);

        Assert.assertNotNull(cart);
        Assert.assertEquals(accountId1, cart.getPersonalAccountId());
    }

    @Test
    public void addCartItem() throws Exception {
        Cart cart = manager.addCartItem(accountId2, generateCartItem(domainName2, false));

        Assert.assertNotNull(cart);
        Assert.assertEquals(accountId2, cart.getPersonalAccountId());
        Assert.assertEquals(2, cart.getItems().size());
    }

    @Test(expected = ParameterValidationException.class)
    public void addCartItemOnProcessingCart() throws Exception {
        Cart cart = manager.addCartItem(accountId3, generateCartItem(domainName2, false));

        Assert.assertNotNull(cart);
        Assert.assertEquals(accountId2, cart.getPersonalAccountId());
        Assert.assertEquals(2, cart.getItems().size());
    }

    @Test
    public void deleteCartItemByName() throws Exception {
        Cart cart = manager.deleteCartItemByName(accountId2, domainName1);

        Assert.assertNotNull(cart);
        Assert.assertEquals(accountId2, cart.getPersonalAccountId());
        Assert.assertEquals(0, cart.getItems().size());
    }

    @Test
    public void setCartItems() throws Exception {
        Set<CartItem> cartItems = new HashSet<>();
        cartItems.add(generateCartItem(domainName2, false));
        cartItems.add(generateCartItem(domainName3, false));

        Cart cart = manager.setCartItems(accountId2, cartItems);

        Assert.assertNotNull(cart);
        Assert.assertEquals(accountId2, cart.getPersonalAccountId());
        Assert.assertEquals(2, cart.getItems().size());
    }

    @Test
    public void setProcessing() throws Exception {
        manager.setProcessing(accountId2, true);

        Cart cart = manager.findByPersonalAccountId(accountId2);

        Assert.assertNotNull(cart);
        Assert.assertEquals(accountId2, cart.getPersonalAccountId());
        Assert.assertTrue(cart.getItems().stream().allMatch(CartItem::getProcessing));
    }

    @Test
    public void setProcessingByName() throws Exception {
        manager.setProcessingByName(accountId2, domainName1, true);

        Cart cart = manager.findByPersonalAccountId(accountId2);

        Assert.assertNotNull(cart);
        Assert.assertEquals(accountId2, cart.getPersonalAccountId());
        Assert.assertTrue(cart.getItems().stream().anyMatch(item -> item.getName().equals(domainName1) && item.getProcessing()));
    }

    @Test
    public void buy() throws Exception {
        List<ProcessingBusinessAction> processingBusinessActions = manager.buy(accountId2, price);
        Assert.assertNotNull(processingBusinessActions);
        Assert.assertEquals(1, processingBusinessActions.size());
        Assert.assertEquals(domainName1, processingBusinessActions.get(0).getParam("name"));
    }

    @Test(expected = ParameterValidationException.class)
    public void buyWrongCartPrice() throws Exception {
        manager.buy(accountId2, BigDecimal.valueOf(49));
    }

    @Test
    public void findNotEmptyCartsAtLastMonth() throws Exception {
        List<Cart> notEmptyCartByLastMonth = manager.findNotEmptyCartsAtLastMonth();
        Assert.assertEquals(notEmptyCartByLastMonth.size(), 1);
    }

    private Cart generateEmptyCart() {
        Cart cart = new Cart();
        cart.setId(emptyCartId);
        cart.setPersonalAccountId(accountId1);

        return cart;
    }

    private Cart generateNotEmptyCart() {
        Cart cart = new Cart();
        cart.setId(notEmptyCartId);
        cart.setPersonalAccountId(accountId2);
        cart.setUpdateDateTime(LocalDateTime.now());

        Set<CartItem> cartItems = new HashSet<>();

        cartItems.add(generateCartItem(domainName1,false));

        cart.setItems(cartItems);

        return cart;
    }

    private Cart generateProcessingCart() {
        Cart cart = new Cart();
        cart.setId(processingCartId);
        cart.setPersonalAccountId(accountId3);
        cart.setUpdateDateTime(LocalDateTime.now().minusMonths(2));

        Set<CartItem> cartItems = new HashSet<>();

        cartItems.add(generateCartItem(domainName1,true));

        cart.setItems(cartItems);

        return cart;
    }

    private CartItem generateCartItem(String domainName, Boolean processing) {
        DomainCartItem domainCartItem = new DomainCartItem();
        domainCartItem.setName(domainName);
        domainCartItem.setPersonId("1");
        domainCartItem.setProcessing(processing);

        return domainCartItem;
    }

    private ProcessingBusinessAction generateProcessingBusinessAction(String accountId, DomainCartItem domain) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", domain.getName());
        params.put("register", true);
        params.put("personId", domain.getPersonId());

        ProcessingBusinessAction processingBusinessAction = new ProcessingBusinessAction(
                "1",
                "Some processingBusinessAction",
                State.NEED_TO_PROCESS,
                1,
                "1",
                BusinessActionType.DOMAIN_CREATE_RC,
                new AmqpMessageDestination(),
                new SimpleServiceMessage(),
                accountId,
                LocalDateTime.now(),
                LocalDateTime.now(),
                params);

        return processingBusinessAction;
    }
}