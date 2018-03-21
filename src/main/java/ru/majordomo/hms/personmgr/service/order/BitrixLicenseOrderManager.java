package ru.majordomo.hms.personmgr.service.order;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.IdsContainer;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.exception.NotEnoughMoneyException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.order.BitrixLicenseOrder;
import ru.majordomo.hms.personmgr.model.order.QBitrixLicenseOrder;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.AccountNotificationHelper;
import ru.majordomo.hms.personmgr.service.ChargeMessage;
import ru.majordomo.hms.personmgr.service.FinFeignClient;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static ru.majordomo.hms.personmgr.common.Constants.*;

@Slf4j
@Service
public class BitrixLicenseOrderManager extends OrderManager<BitrixLicenseOrder> {

    private final int MAY_PROLONG_AFTER_MONTHS = 11;
    private final int PROLONG_DAYS_AFTER_EXPIRED = 15;
    private final String PROLONG_COST_PERCENT_BEFORE = "0.22";
    private final String PROLONG_COST_PERCENT_AFTER = "0.6";

    @Value("${mail_manager.bitrix_order_email}")
    protected String bitrixOrderEmail;

    private final ApplicationEventPublisher publisher;
    private final AccountHelper accountHelper;
    private final PersonalAccountManager personalAccountManager;
    private final PaymentServiceRepository paymentServiceRepository;
    private final FinFeignClient finFeignClient;
    private final AccountNotificationHelper accountNotificationHelper;
    private final MongoOperations mongoOperations;

    @Autowired
    public BitrixLicenseOrderManager(
            ApplicationEventPublisher publisher,
            AccountHelper accountHelper,
            PersonalAccountManager personalAccountManager,
            PaymentServiceRepository paymentServiceRepository,
            FinFeignClient finFeignClient,
            AccountNotificationHelper accountNotificationHelper,
            MongoOperations mongoOperations
    ){
        this.publisher = publisher;
        this.accountHelper = accountHelper;
        this.personalAccountManager = personalAccountManager;
        this.paymentServiceRepository = paymentServiceRepository;
        this.finFeignClient = finFeignClient;
        this.accountNotificationHelper = accountNotificationHelper;
        this.mongoOperations = mongoOperations;
    }

    @Override
    protected void onCreate(BitrixLicenseOrder order) {
        switch (order.getType()){
            case NEW:
                onCreateByNewLicence(order);
                break;
            case PROLONG:
                onCreateByProlong(order);
        }
    }

    @Override
    protected void onDecline(BitrixLicenseOrder order) {
        //Возвращаем блокированные средства
        try {
            finFeignClient.unblock(order.getPersonalAccountId(), order.getDocumentNumber());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exception in BitrixLicenseOrderManager.onDecline #1 " + e.getMessage());
        }
    }

    @Override
    protected void onFinish(BitrixLicenseOrder order) {
        //Переводим блокировку в реальное списание
        try {
            finFeignClient.chargeBlocked(order.getPersonalAccountId(), order.getDocumentNumber());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exception in BitrixLicenseOrderManager.onFinish #1 " + e.getMessage());
        }
    }

    private void notifyStaff(PersonalAccount account, Map<String, String> parameters){
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam(EMAIL_KEY, bitrixOrderEmail);
        message.addParam(API_NAME_KEY, "MajordomoServiceMessage");
        message.addParam(PRIORITY_KEY, 10);
        message.addParam(PARAMETRS_KEY, parameters);
        publisher.publishEvent(new SendMailEvent(message));
    }

    private void notifyStaffByNewLicence(BitrixLicenseOrder order, PersonalAccount account, String licenseName) {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(CLIENT_ID_KEY, account.getId());

        parameters.put("body", "1. Аккаунт: " + account.getName() + "<br>" +
                "2. Заказ лицензии Битрикс для: " + order.getDomainName() + "<br>" +
                "3. Тип лицензии: " + licenseName + "<br>");
        parameters.put("subject", "Заказ лицензии 1С-Битрикс Управление сайтом");

        notifyStaff(account, parameters);
    }

    private void notifyClientByNewLicence(PersonalAccount account, String licenseName) {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(CLIENT_ID_KEY, account.getAccountId());
        parameters.put(ACC_ID_KEY, account.getName());
        parameters.put("license_name", licenseName);
        parameters.put("from", "noreply@majordomo.ru");

        accountNotificationHelper.sendMail(account, "HmsVHMajordomoZakaz1CBitrix", 1, parameters);
    }

    public boolean isProlonged(BitrixLicenseOrder order){
        QBitrixLicenseOrder qOrder = QBitrixLicenseOrder.bitrixLicenseOrder;
        BooleanBuilder builder = new BooleanBuilder();

        Predicate predicate = builder
                .and(qOrder.previousOrderId.eq(order.getId()))
                .and(qOrder.type.eq(BitrixLicenseOrder.LicenseType.PROLONG))
                .and(qOrder.state.notIn(OrderState.DECLINED));

        return accountOrderRepository.exists(predicate);
    }

    protected void onCreateByProlong(BitrixLicenseOrder order) {
        BitrixLicenseOrder previousOrder = accountOrderRepository.findOne(order.getPreviousOrderId());

        checkProlongLicenceOnCreate(previousOrder);

        order.setPreviousOrder(previousOrder);

        PaymentService paymentService = paymentServiceRepository.findOne(previousOrder.getServiceId());


        BigDecimal discountPercent;

        if(previousOrder.getUpdated().isBefore(LocalDateTime.now().plusDays(PROLONG_DAYS_AFTER_EXPIRED))){
            discountPercent = new BigDecimal(PROLONG_COST_PERCENT_BEFORE);
        } else {
            discountPercent = new BigDecimal(PROLONG_COST_PERCENT_AFTER);
        }

        BigDecimal cost = paymentService.getCost().multiply(discountPercent);

        PersonalAccount account = personalAccountManager.findOne(order.getPersonalAccountId());

        BigDecimal balance = accountHelper.getBalance(account);

        if (cost.compareTo(balance) > 0) {
            updateState(order, OrderState.DECLINED, "service");
            throw new NotEnoughMoneyException("Баланс недостаточен для продления лицензии", cost.subtract(balance));
        }

        //Списываем деньги
        ChargeMessage chargeMessage = new ChargeMessage.Builder(paymentService)
                .setAmount(cost)
                .setComment("Скидка на продление, цена умножена на " + discountPercent)
                .build();

        SimpleServiceMessage response = accountHelper.block(account, chargeMessage);

        order.setDocumentNumber((String) response.getParam("documentNumber"));

        //Уведомление
        notifyStaffByProlong(order, account, paymentService.getName(), cost);

        notifyClientByProlong(account, paymentService.getName());
    }

    private void notifyClientByProlong(PersonalAccount account, String licenseName) {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(CLIENT_ID_KEY, account.getAccountId());
        parameters.put(ACC_ID_KEY, account.getName());
        parameters.put("license_name", licenseName);
        parameters.put("from", "noreply@majordomo.ru");

        accountNotificationHelper.sendMail(account, "HmsVHMajordomoZakazprodleniya1CBitrix", 1, parameters);
    }

    private void notifyStaffByProlong(BitrixLicenseOrder accountOrder, PersonalAccount account, String licenseName, BigDecimal cost) {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(CLIENT_ID_KEY, account.getId());

        parameters.put("body",
                "1. Аккаунт: " + account.getName() + "<br/>" +
                        "2. Продление лицензии Битрикс для: " + accountOrder.getDomainName() + "<br/>" +
                        "3. Тип лицензии: " + licenseName + "<br/>" +
                        "4. Списано " + cost + " рублей<br/>");
        parameters.put("subject", "Продление лицензии 1С-Битрикс Управление сайтом");
        notifyStaff(account, parameters);
    }

    private void checkProlongLicenceOnCreate(BitrixLicenseOrder previousOrder){
        if (!previousOrder.getState().equals(OrderState.FINISHED)) {
            throw new ParameterValidationException("Продлить можно только успешно заказанную лицензию");
        }

        if (!previousOrder.getUpdated().plusMonths(MAY_PROLONG_AFTER_MONTHS).isBefore(LocalDateTime.now())) {
            throw new ParameterValidationException("Заказ продления лицензии доступно через "
                    + MAY_PROLONG_AFTER_MONTHS + " месяцев после заказа");
        }

        if (isProlonged(previousOrder)) {
            throw new ParameterValidationException("Данная лицензия уже продлена");
        }
    }

    private void onCreateByNewLicence(BitrixLicenseOrder order){
        PaymentService paymentService = paymentServiceRepository.findOne(order.getServiceId());
        BigDecimal cost = paymentService.getCost();

        PersonalAccount account = personalAccountManager.findOne(order.getPersonalAccountId());

        BigDecimal balance = accountHelper.getBalance(account);

        if (cost.compareTo(balance) > 0) {
            updateState(order, OrderState.DECLINED, "service");
            throw new NotEnoughMoneyException("Баланс недостаточен для заказа лицензии", cost.subtract(balance));
        }

        //Списываем деньги
        ChargeMessage chargeMessage = new ChargeMessage.Builder(paymentService)
                .setAmount(cost)
                .build();

        SimpleServiceMessage response = accountHelper.block(account, chargeMessage);

        order.setDocumentNumber((String) response.getParam("documentNumber"));

        //Уведомление
        notifyStaffByNewLicence(order, account, paymentService.getName());

        notifyClientByNewLicence(account, paymentService.getName());
    }

    public List<String> getProlongedOrProlongingIds(String personalAccountId){
        MatchOperation match = match(Criteria
                .where("state").ne(OrderState.DECLINED.name())
                .and("personalAccountId").is(personalAccountId)
                .and("previousOrderId").exists(true)
        );

        GroupOperation group = group().addToSet("previousOrderId").as("ids");

        Aggregation aggregation = Aggregation.newAggregation(match, group);
        return mongoOperations.aggregate(aggregation, BitrixLicenseOrder.class, IdsContainer.class)
                .getMappedResults().get(0).getIds();
    }
}
