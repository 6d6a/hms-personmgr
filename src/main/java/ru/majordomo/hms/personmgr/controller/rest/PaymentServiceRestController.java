package ru.majordomo.hms.personmgr.controller.rest;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;

@RestController
public class PaymentServiceRestController extends CommonRestController {

    private final PaymentServiceRepository repository;

    @Autowired
    public PaymentServiceRestController(PaymentServiceRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{accountId}/payment-services")
    public ResponseEntity<List<PaymentService>> listAllForAccount(
            @PathVariable(value = "accountId") String accountId
    ) {
        List<PaymentService> services = repository.findByActive(true);

        return new ResponseEntity<>(services, HttpStatus.OK);
    }

    @GetMapping(value = "/payment-services", headers = "X-HMS-Pageable=false")
    public ResponseEntity<List<PaymentService>> listAll(
    ) {
        List<PaymentService> services = repository.findAllPaymentServices();

        return new ResponseEntity<>(services, HttpStatus.OK);
    }

    @GetMapping("/payment-services")
    public ResponseEntity<Page<PaymentService>> listAll(
            @QuerydslPredicate(root = PaymentService.class) Predicate predicate,
            Pageable pageable
    ) {
        if (predicate == null) predicate = new BooleanBuilder();
        Page<PaymentService> services = repository.findAll(predicate, pageable);

        return new ResponseEntity<>(services, HttpStatus.OK);
    }

    @GetMapping("/payment-services/{serviceId}")
    public ResponseEntity<PaymentService> get(
            @PathVariable(value = "serviceId") String serviceId
    ) {
        PaymentService service = repository.findById(serviceId).orElse(null);

        return new ResponseEntity<>(service, HttpStatus.OK);
    }

    @GetMapping("/payment-services/search")
    public ResponseEntity<PaymentService> search(@RequestParam Map<String, String> query) {
        PaymentService paymentService = null;
        if (query == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else if (query.get("oldId") != null) {
            paymentService = repository.findByOldId(query.get("oldId"));
        }

        if (paymentService == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(paymentService, HttpStatus.OK);
        }
    }
}