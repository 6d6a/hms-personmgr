package ru.majordomo.hms.personmgr.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.config.FeignConfig;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.fin.*;
import ru.majordomo.hms.personmgr.dto.stat.MetaProjection;
import ru.majordomo.hms.personmgr.service.ChargeMessage;

import javax.annotation.Nullable;

@FeignClient(name = "fin", configuration = FeignConfig.class)
public interface FinFeignClient {
    @RequestMapping(method = RequestMethod.POST, value = "/payment_integration/add_payment", consumes = "application/json")
    String addPayment(PaymentRequest paymentRequest);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/payment_operations/overall-payment-amount", consumes = "application/json")
    BigDecimal getOverallPaymentAmount(@PathVariable("accountId") String accountId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/payment_operations/spent-payment-amount", consumes = "application/json")
    BigDecimal getRealSpentPaymentAmount(@PathVariable("accountId") String accountId);

    @RequestMapping(method = RequestMethod.POST, value = "/{accountId}/payment_operations/charge", consumes = "application/json")
    SimpleServiceMessage charge(@PathVariable("accountId") String accountId, ChargeMessage chargeMessage);

    @RequestMapping(method = RequestMethod.POST, value = "/{accountId}/payment_operations/block", consumes = "application/json")
    SimpleServiceMessage block(@PathVariable("accountId") String accountId, ChargeMessage chargeMessage);

    @RequestMapping(method = RequestMethod.DELETE, value = "/{accountId}/payment_operations/{documentNumber}", consumes = "application/json")
    SimpleServiceMessage unblock(@PathVariable("accountId") String accountId, @PathVariable("documentNumber") String documentNumber);

    @RequestMapping(method = RequestMethod.POST, value = "/{accountId}/payment_operations/{documentNumber}/charge", consumes = "application/json")
    SimpleServiceMessage chargeBlocked(@PathVariable("accountId") String accountId, @PathVariable("documentNumber") String documentNumber);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/balance", consumes = "application/json")
    Map<String, Object> getBalance(@PathVariable("accountId") String accountId);

    @RequestMapping(method = RequestMethod.GET, value = "/recurrent/get_all", consumes = "application/json")
    List<String> getRecurrentAccounts();

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/recurrent/is_active", consumes = "application/json")
    Boolean isRecurrentActive(@PathVariable("accountId") String accountId);

    @RequestMapping(method = RequestMethod.POST, value = "/{accountId}/recurrent/repeat_payment/{sumAmount}", consumes = "application/json")
    ResponseEntity<Void> repeatPayment(@PathVariable("accountId") String accountId, @PathVariable("sumAmount") BigDecimal sumAmount);

    @GetMapping(value = "/{accountId}/monthly-bills/{monthlyBillId}", consumes = "application/json")
    MonthlyBill getMonthlyBill(@PathVariable("accountId") String accountId, @PathVariable("monthlyBillId") String monthlyBillId);

    @PostMapping(value = "/{accountId}/generate_payment_link", consumes = "application/json")
    PaymentLinkResponse generatePaymentLink(@PathVariable("accountId") String accountId, @RequestBody PaymentLinkRequest paymentLinkRequest);

    @PostMapping(value = "/stat/generate-money-meta-data", consumes = "application/json")
    Map<LocalDate, MetaProjection> generateMoneyMetaData(
            @RequestBody List<String> accountIds,
            @RequestParam("start") String startDate,
            @RequestParam("end") String endDate
    );

    @PostMapping(value = "/{accountId}/promised-payment", consumes = "application/json")
    Map<String, Object> addPromisedPayment(@PathVariable("accountId") String personalAccountId, PromisedPaymentRequest request);

    @GetMapping(value = "/{accountId}/promised-payment", consumes = "application/json")
    List<Object> getPromisedPayments(@PathVariable("accountId") String accountId);

    @PostMapping(value = "/import/{accountId}")
    List<String> importToMongo(@PathVariable("accountId") String accountId);

    @GetMapping(value = "/{accountId}/payment_operations/last-payment", consumes = "application/json")
    @Nullable
    BillingOperation getLastPayment(@PathVariable("accountId") String accountId);
}
