package ru.majordomo.hms.personmgr.controller.rest;

import com.querydsl.core.types.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;

@RestController
public class PaymentServiceRestController extends CommonRestController {

    private final PaymentServiceRepository repository;

    @Autowired
    public PaymentServiceRestController(PaymentServiceRepository repository) {
        this.repository = repository;
    }

    @RequestMapping(value = "/{accountId}/payment-services", method = RequestMethod.GET)
    public ResponseEntity<List<PaymentService>> listAllForAccount(
            @PathVariable(value = "accountId") String accountId
    ) {
        List<PaymentService> services = repository.findByActive(true);

        return new ResponseEntity<>(services, HttpStatus.OK);
    }

    @RequestMapping(value = "/payment-services", method = RequestMethod.GET)
    public ResponseEntity<Page<PaymentService>> listAll(
            @QuerydslPredicate(root = PaymentService.class) Predicate predicate,
            Pageable pageable
    ) {
        Page<PaymentService> services = repository.findAll(predicate, pageable);

        return new ResponseEntity<>(services, HttpStatus.OK);
    }

    @RequestMapping(value = "/payment-services/{serviceId}", method = RequestMethod.GET)
    public ResponseEntity<PaymentService> get(
            @PathVariable(value = "serviceId") String serviceId
    ) {
        PaymentService service = repository.findOne(serviceId);

        return new ResponseEntity<>(service, HttpStatus.OK);
    }
}