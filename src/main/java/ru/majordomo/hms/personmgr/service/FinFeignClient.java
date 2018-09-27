package ru.majordomo.hms.personmgr.service;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.config.FeignConfig;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.fin.MonthlyBill;
import ru.majordomo.hms.personmgr.dto.fin.PaymentLinkRequest;
import ru.majordomo.hms.personmgr.dto.fin.PaymentLinkResponse;

@FeignClient(name = "fin", fallback = FinFeignClientFallback.class, configuration = FeignConfig.class)
public interface FinFeignClient {
    @RequestMapping(method = RequestMethod.POST, value = "/payment_integration/add_payment", consumes = "application/json")
    String addPayment(Map<String, Object> payment);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/payment_operations/overall-payment-amount", consumes = "application/json")
    BigDecimal getOverallPaymentAmount(@PathVariable("accountId") String accountId);

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
}
