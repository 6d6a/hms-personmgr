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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.IdsContainer;
import ru.majordomo.hms.personmgr.dto.appscat.AppType;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.exception.NotEnoughMoneyException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.feign.AppscatFeignClient;
import ru.majordomo.hms.personmgr.feign.FinFeignClient;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.order.BitrixLicenseOrder;
import ru.majordomo.hms.personmgr.model.order.QBitrixLicenseOrder;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.service.*;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static ru.majordomo.hms.personmgr.common.Constants.*;

@Slf4j
@Service
public class BitrixLicenseOrderManager extends OrderManager<BitrixLicenseOrder> {

    public final static int MAY_PROLONG_DAYS_BEFORE_EXPIRED = 15;

    private final static int DAYS_AFTER_EXPIRED_WITH_MAX_DISCOUNT = 30;
    private final static BigDecimal PROLONG_DISCOUNT_MAX = new BigDecimal("0.22");
    private final static BigDecimal PROLONG_DISCOUNT_MIN = new BigDecimal("0.6");

    @Value("${mail_manager.bitrix_order_email}")
    protected String bitrixOrderEmail;

    private final ApplicationEventPublisher publisher;
    private final AccountHelper accountHelper;
    private final PersonalAccountManager personalAccountManager;
    private final PaymentServiceRepository paymentServiceRepository;
    private final FinFeignClient finFeignClient;
    private final AccountNotificationHelper accountNotificationHelper;
    private final MongoOperations mongoOperations;
    private final AppscatFeignClient appscatFeignClient;

    @Autowired
    public BitrixLicenseOrderManager(
            ApplicationEventPublisher publisher,
            AccountHelper accountHelper,
            PersonalAccountManager personalAccountManager,
            PaymentServiceRepository paymentServiceRepository,
            FinFeignClient finFeignClient,
            AccountNotificationHelper accountNotificationHelper,
            MongoOperations mongoOperations,
            AppscatFeignClient appscatFeignClient
    ){
        this.publisher = publisher;
        this.accountHelper = accountHelper;
        this.personalAccountManager = personalAccountManager;
        this.paymentServiceRepository = paymentServiceRepository;
        this.finFeignClient = finFeignClient;
        this.accountNotificationHelper = accountNotificationHelper;
        this.mongoOperations = mongoOperations;
        this.appscatFeignClient = appscatFeignClient;
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

        return exists(predicate);
    }

    protected void onCreateByProlong(BitrixLicenseOrder order) {
        BitrixLicenseOrder previousOrder = order.getPreviousOrder();

        checkProlongLicenceOnCreate(previousOrder);

        PaymentService paymentService = paymentServiceRepository.findById(previousOrder.getServiceId()).orElseThrow(
                () -> new ParameterValidationException("Не найден сервис с id " + previousOrder.getServiceId())
        );

        BigDecimal discount;

        if(previousOrder.getUpdated().plusYears(1).plusDays(DAYS_AFTER_EXPIRED_WITH_MAX_DISCOUNT)
                .isAfter(LocalDateTime.now())
        ){
            discount = PROLONG_DISCOUNT_MAX;
        } else {
            discount = PROLONG_DISCOUNT_MIN;
        }

        BigDecimal cost = paymentService.getCost().multiply(discount).setScale(0, BigDecimal.ROUND_CEILING);

        PersonalAccount account = personalAccountManager.findOne(order.getPersonalAccountId());

        BigDecimal balance = accountHelper.getBalance(account);

        if (cost.compareTo(balance) > 0) {
            updateState(order, OrderState.DECLINED, "service");
            throw new NotEnoughMoneyException("Баланс недостаточен для продления лицензии", cost.subtract(balance));
        }

        //Списываем деньги
        ChargeMessage chargeMessage = new ChargeMessage.Builder(paymentService)
                .setAmount(cost)
                .setComment("Скидка на продление, цена умножена на " + discount)
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

        if (!previousOrder.getUpdated().plusYears(1).minusDays(MAY_PROLONG_DAYS_BEFORE_EXPIRED).isBefore(LocalDateTime.now())) {
            throw new ParameterValidationException("Заказ продления лицензии доступно за "
                    + MAY_PROLONG_DAYS_BEFORE_EXPIRED + " дней до истечения лицензии");
        }

        if (isProlonged(previousOrder)) {
            throw new ParameterValidationException("Данная лицензия уже продлена");
        }
    }

    private void onCreateByNewLicence(BitrixLicenseOrder order){

        checkThatServiceIdIsBitrixLicense(order.getServiceId());

        PaymentService paymentService = paymentServiceRepository.findById(order.getServiceId()).orElseThrow(
                () -> new ParameterValidationException("Не найден сервис с id " + order.getServiceId())
        );
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

        List<IdsContainer> idsContainers = mongoOperations.aggregate(aggregation, BitrixLicenseOrder.class, IdsContainer.class)
                    .getMappedResults();

        if (idsContainers.isEmpty()) {
            return new ArrayList<>();
        } else {
            return idsContainers.get(0).getIds();
        }
    }

    public Predicate getPredicate(
            String personalAccountId,
            LocalDateTime updatedAfter,
            LocalDateTime updatedBefore,
            OrderState state,
            String domain,
            boolean excludeProlongedOrders
    ){
        QBitrixLicenseOrder qOrder = QBitrixLicenseOrder.bitrixLicenseOrder;

        BooleanBuilder builder = new BooleanBuilder();

        if (personalAccountId != null) {
            builder = builder.and(qOrder.personalAccountId.eq(personalAccountId));
        }

        if (updatedBefore != null) {
            builder = builder.and(qOrder.updated.before(updatedBefore));
        }

        if (updatedAfter != null) {
            builder = builder.and(qOrder.updated.after(updatedAfter));
        }

        if (state != null) {
            builder = builder.and(qOrder.state.eq(state));
        }

        if (domain != null) {
            builder = builder.and(qOrder.domainName.eq(domain));
        }

        if (excludeProlongedOrders) {
            builder = builder.and(qOrder.id.notIn(getProlongedOrProlongingIds(personalAccountId)));
        }

        return builder.getValue();
    }

    private void checkThatServiceIdIsBitrixLicense(String serviceId){
        AppType appType = appscatFeignClient.getAppTypeByInternalName("BITRIX");

        if (appType
                .getServiceIds()
                .entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .noneMatch(string -> string.equals(serviceId))
                ) {
            throw  new ParameterValidationException("serviceId лицензии указан некорректно");
        }
    }

    public void mongoAfterConvert(BitrixLicenseOrder order){
        try {
            PersonalAccount account = mongoOperations.findById(order.getPersonalAccountId(), PersonalAccount.class);

            if (account != null) {
                order.setPersonalAccountName(account.getName());
            }

            PaymentService paymentService = mongoOperations.findById(order.getServiceId(), PaymentService.class);

            if (paymentService != null) {
                order.setServiceName(paymentService.getName());
            }

            if (order.getPreviousOrderId() != null && !order.getPreviousOrderId().isEmpty()) {
                BitrixLicenseOrder previousOrder = mongoOperations.findById(order.getPreviousOrderId(), BitrixLicenseOrder.class);
                order.setPreviousOrder(previousOrder);
            }
        } catch (Exception ignore){
            log.error("Catch exception in " + getClass().getName() + ".mongoAfterConvert(String '"
                    + order.getId() + "') message: " + ignore.getMessage());
        }
    }
}
