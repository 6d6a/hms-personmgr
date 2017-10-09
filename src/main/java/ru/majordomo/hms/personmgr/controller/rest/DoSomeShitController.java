package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.majordomo.hms.personmgr.event.account.AccountProcessExpiringAbonementsEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

@RestController
public class DoSomeShitController {

//    private final PersonalAccountManager manager;
//    private final ApplicationEventPublisher publisher;
//
//    @Autowired
//    public DoSomeShitController(
//            PersonalAccountManager manager,
//            ApplicationEventPublisher publisher
//    ) {
//        this.manager = manager;
//        this.publisher = publisher;
//    }
//
//    @PostMapping("/do-some-shit")
//    public ResponseEntity<Void> doSomeShit() {
//        PersonalAccount account = manager.findByAccountId("179550");
//        publisher.publishEvent(new AccountProcessExpiringAbonementsEvent(account.getId()));
//        return ResponseEntity.ok().build();
//    }
}
