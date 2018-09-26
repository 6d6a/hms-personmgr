package ru.majordomo.hms.personmgr.service.order;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.fin.MonthlyBill;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.order.documentOrder.*;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.service.AccountNotificationHelper;
import ru.majordomo.hms.personmgr.service.ChargeMessage;
import ru.majordomo.hms.personmgr.service.FinFeignClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static java.time.temporal.TemporalAdjusters.firstDayOfYear;
import static java.time.temporal.TemporalAdjusters.lastDayOfYear;
import static ru.majordomo.hms.personmgr.common.OrderState.FINISHED;
import static ru.majordomo.hms.personmgr.common.OrderState.IN_PROGRESS;
import static ru.majordomo.hms.personmgr.common.OrderState.NEW;
import static ru.majordomo.hms.personmgr.model.order.documentOrder.DeliveryType.FREE_DELIVERY;

@Service
public class DocumentOrderManager extends OrderManager<DocOrder> {

    private FinFeignClient finFeignClient;
    private int freeDeliveryInYear;
    private String departmentEmail;
    private String clientEmailApiName;
    private PaymentServiceRepository paymentServiceRepository;
    private AccountHistoryManager history;
    private PersonalAccountManager accountManager;
    private AccountNotificationHelper accountNotificationHelper;
    private AccountOwnerManager ownerManager;

    private static final Logger log = LoggerFactory.getLogger(DocumentOrderManager.class);

    private static final String SERVICE_REGULAR_OLD_ID = "service_regular_paid_document_delivery";
    private static final String SERVICE_FAST_OLD_ID = "service_fast_paid_document_delivery";
    private static final String SERVICE_EMAIL_API_NAME = "MajordomoServiceMessage";

    @Autowired
    public DocumentOrderManager(
            PaymentServiceRepository paymentServiceRepository,
            AccountHistoryManager history,
            PersonalAccountManager accountManager,
            AccountNotificationHelper accountNotificationHelper,
            AccountOwnerManager ownerManager,
            FinFeignClient finFeignClient,
            @Value("${mail_manager.fin_document_order_email}") String departmentEmail,
            @Value("${document_order.free_delivery_in_year}") int freeDeliveryInYear,
            @Value("${document_order.client_api_name}") String clientEmailApiName
    ) {
        this.finFeignClient = finFeignClient;
        this.paymentServiceRepository = paymentServiceRepository;
        this.departmentEmail = departmentEmail;
        this.freeDeliveryInYear = freeDeliveryInYear;
        this.clientEmailApiName = clientEmailApiName;
        this.history = history;
        this.accountManager = accountManager;
        this.accountNotificationHelper = accountNotificationHelper;
        this.ownerManager = ownerManager;
    }

    @Override
    protected void onCreate(DocOrder order) {

        order.setDocumentNumber(null);

        log.info("onCreate(" + order.toString() + ")");

        PersonalAccount account = accountManager.findOne(order.getPersonalAccountId());

        AccountOwner owner = ownerManager.findOneByPersonalAccountId(order.getPersonalAccountId());

        checkOwner(owner);

        order.setAddress(owner.getContactInfo().getPostalAddress());

        checkDocs(order);

        switch (order.getDeliveryType()) {
            case FREE_DELIVERY:
                List<DocOrder> activeOrders = getFreeSuccessOrdersInCurrentYear(order.getPersonalAccountId());
                if (activeOrders.size() >= freeDeliveryInYear) {
                    throw new ParameterValidationException(
                            "За один календарный год доступно не более "
                                    + freeDeliveryInYear + " бесплатных отправок."
                    );
                }

                break;
            case FAST_PAID_DELIVERY:
                blockMoneyForFastDelivery(order);

                break;
            case REGULAR_PAID_DELIVERY:
                blockMoneyForRegularPaidDelivery(order);

                break;
            default:
                throw new ParameterValidationException("Неизвестный тип доставки: " + order.getDeliveryType());
        }

        try {
            save(order);
        } catch (Exception e) {
            log.error("onCreate() Catch e.class " + e.getClass().getName() + " e.message " + e.getMessage());

            unblock(order);

            throw e;
        }

        notifyStaff(order, account);

        notifyClient(order, account);

        history.save(
                account,
                "Сделан заказ документов с типом доставки '" + order.getDeliveryType().humanize()
                        + "' на адрес '" + order.getAddress()
                        + "' с комментарием '" + order.getComment() + "'"
                        + " список документов: "
                        + order.getDocs().stream().map(Doc::humanize).reduce((s1, s2) -> s1 + s2)
        );
    }

    @Override
    protected void onDecline(DocOrder order) {
        //Возвращаем блокированные средства
        switch (order.getDeliveryType()) {
            case FREE_DELIVERY:
                break;

            case REGULAR_PAID_DELIVERY:
            case FAST_PAID_DELIVERY:
                unblock(order);
        }
    }

    @Override
    protected void onFinish(DocOrder order) {
        switch (order.getDeliveryType()) {
            case FREE_DELIVERY:
                log.info("free delivery order.id " + order.getId() + " pass unblock");

                break;
            case REGULAR_PAID_DELIVERY:
            case FAST_PAID_DELIVERY:
                //Переводим блокировку в реальное списание
                try {
                    finFeignClient.chargeBlocked(order.getPersonalAccountId(), order.getDocumentNumber());
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("Can't charge by documentNubmer " + order.getDocumentNumber() + ", e.class " + e.getClass().getName()
                            + " e.message " + e.getMessage() + " order " + order.toString());
                }
        }
    }

    private void unblock(DocOrder order) {
        if (order.getDocumentNumber() == null) {
            log.info("order id " + order.getId() + " documentNumber == null, unblock is passed");
            return;
        }

        try {
            finFeignClient.unblock(order.getPersonalAccountId(), order.getDocumentNumber());
        } catch (Exception e) {
            log.error("Can't unblock money, e.class " + e.getClass().getName()
                    + " e.message " + e.getMessage() + " order " + order.toString());
        }

    }

    private void blockMoneyForFastDelivery(DocOrder order) {
        PaymentService paymentService = paymentServiceRepository.findByOldId(SERVICE_FAST_OLD_ID);

        block(order, paymentService);
    }

    private void blockMoneyForRegularPaidDelivery(DocOrder order) {
        PaymentService paymentService = paymentServiceRepository.findByOldId(SERVICE_REGULAR_OLD_ID);

        block(order, paymentService);
    }

    private void block(DocOrder order, PaymentService paymentService) {
        ChargeMessage chargeMessage = ChargeMessage.builder(paymentService).build();

        SimpleServiceMessage blockMessage = finFeignClient.block(order.getPersonalAccountId(), chargeMessage);

        order.setDocumentNumber((String) blockMessage.getParam("documentNumber"));
    }

    public int getFreeLimit(String personalAccountId) {
        List<DocOrder> docOrders = getFreeSuccessOrdersInCurrentYear(personalAccountId);
        if (docOrders == null) {
            return this.freeDeliveryInYear;
        } else {
            return freeDeliveryInYear - docOrders.size();
        }
    }

    private List<DocOrder> getFreeSuccessOrdersInCurrentYear(String personalAccountId) {
        LocalDate now = LocalDate.now();
        LocalDateTime firstDayOfYear = LocalDateTime.of(now.with(firstDayOfYear()), LocalTime.MIN);
        LocalDateTime lastDayOfYear = LocalDateTime.of(now.with(lastDayOfYear()), LocalTime.MAX);

        QDocOrder qOrder = QDocOrder.docOrder;

        Predicate predicate = new BooleanBuilder()
                .and(qOrder.deliveryType.eq(FREE_DELIVERY))
                .and(qOrder.personalAccountId.eq(personalAccountId))
                .and(qOrder.created.before(lastDayOfYear))
                .and(qOrder.created.after(firstDayOfYear))
                .and(qOrder.state.in(IN_PROGRESS, FINISHED, NEW))
                .getValue();

        return findAll(predicate);
    }

    private void notifyClient(DocOrder order, PersonalAccount account) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("type", order.getDeliveryType().humanize());
        parameters.put("address", order.getAddress());

        accountNotificationHelper.sendMail(account, clientEmailApiName, parameters);
    }

    private void notifyStaff(DocOrder order, PersonalAccount account) {
        StringJoiner body = new StringJoiner("<br>")
                .add("Поступил заказ на отправку документов ")
                .add("Аккаунт: " + account.getName())
                .add("Тип отправки: " + order.getDeliveryType().humanize())
                .add("Адрес доставки: " + order.getAddress())
                .add("<a href=\"https://hms-billing.intr/order/document/" + order.getId() + "\">Ссылка на заказ</a>")
                .add("Список документов: " + order.getDocs().stream().map(Doc::humanize).reduce((s1,s2)->s1+s2));

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("body", body.toString());
        parameters.put("subject", "Заказ отправки документов");

        accountNotificationHelper.sendInternalEmail(departmentEmail, SERVICE_EMAIL_API_NAME, account.getId(), 10, parameters);
    }

    private void checkDocs(DocOrder order) {
        for (Doc doc : order.getDocs()) {
            if (doc instanceof ActOfWorkPerformed) {
                try {
                    MonthlyBill monthlyBill = finFeignClient.getMonthlyBill(order.getPersonalAccountId(), ((ActOfWorkPerformed) doc).getId());
                    ((ActOfWorkPerformed) doc).setBillDate(monthlyBill.getBillDate());
                } catch (Exception e) {
                    log.info("Не найден " + doc.humanize() + " e.class " + e.getClass().getName()
                            + " e.message " + e.getMessage() + " order " + order.toString());
                    throw new ParameterValidationException("Не найден " + doc.humanize());
                }
            } else if (doc instanceof ActOfReconciliation) {
                ActOfReconciliation act = (ActOfReconciliation) doc;
                if (!act.getEndDate().isBefore(LocalDate.now())) {
                    throw new ParameterValidationException("Дата 'до' акта сверки не может быть после текущей даты");
                }
                if (!act.getStartDate().isBefore(LocalDate.now())) {
                    throw new ParameterValidationException("Дата 'от' акта сверки не может быть после текущей даты");
                }
                if (!act.getStartDate().isBefore(act.getEndDate())) {
                    throw new ParameterValidationException("Дата 'от' акта сверки не может быть после даты 'до'");
                }
            }
        }
    }

    private void checkOwner(AccountOwner owner) {
        switch (owner.getType()) {
            case COMPANY:
            case BUDGET_COMPANY:

                break;
            case INDIVIDUAL:
            default:
                throw new ParameterValidationException("Заказ документов недоступен физическим лицам");
        }

        String postalAddress = owner.getContactInfo().getPostalAddress();
        if (postalAddress == null || postalAddress.isEmpty()) {
            throw new ParameterValidationException("У владельца аккаунта не указан почтовый адрес");
        }
    }
}