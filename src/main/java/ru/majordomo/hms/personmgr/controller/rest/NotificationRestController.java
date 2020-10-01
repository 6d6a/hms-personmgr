package ru.majordomo.hms.personmgr.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.Department;
import ru.majordomo.hms.personmgr.common.Views;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.cerb.CerbTicket;
import ru.majordomo.hms.personmgr.model.notification.Notification;
import ru.majordomo.hms.personmgr.repository.CerbTicketRepository;
import ru.majordomo.hms.personmgr.repository.NotificationRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.AccountNotificationHelper;
import ru.majordomo.hms.personmgr.service.Cerb.CerbApiClient;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.util.List;


@RestController
@Validated
@AllArgsConstructor
public class NotificationRestController extends CommonRestController {
    private final NotificationRepository notificationRepository;
    private final AccountNotificationHelper accountNotificationHelper;
    private final AccountHelper accountHelper;
    private final PersonalAccountRepository personalAccountRepository;
    private final CerbTicketRepository cerbTicketRepository;
    private final CerbApiClient cerbApiClient;

    @JsonView(Views.Public.class)
    @RequestMapping(value = "/{accountId}/notifications/{notificationId}", method = RequestMethod.GET)
    public ResponseEntity<Notification> get(
            @ObjectId(Notification.class) @PathVariable(value = "notificationId") String notificationId
    ) {
        return findOne(notificationId);
    }

    @JsonView(Views.Public.class)
    @RequestMapping(value = "/{accountId}/notifications", method = RequestMethod.GET)
    public ResponseEntity<List<Notification>> getAll() {
        return ResponseEntity.ok(notificationRepository.findByActive(true));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @JsonView(Views.Internal.class)
    @RequestMapping(value = "/notifications/{notificationId}", method = RequestMethod.GET)
    public ResponseEntity<Notification> getInternal(
            @ObjectId(Notification.class) @PathVariable(value = "notificationId") String notificationId
    ) {
        return findOne(notificationId);
    }

    @JsonView(Views.Internal.class)
    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/notifications", method = RequestMethod.GET)
    public ResponseEntity<List<Notification>> getAllInternal() {
        return ResponseEntity.ok(notificationRepository.findAll());
    }

    private ResponseEntity<Notification> findOne(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);

        if(notification == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(notification, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/notifications/send-to-client")
    public ResponseEntity<SimpleServiceMessage> sendNotificationFromServiceToMailManager(
            @RequestBody SimpleServiceMessage message
    ) {
        accountNotificationHelper.sendNotification(message);
        return ResponseEntity.ok().build();
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/phpmail-disable-notify")
    public ResponseEntity<String> gotAccountId(
            @RequestBody String accountId
    ) {

        PersonalAccount account = personalAccountRepository.findByAccountId(accountId);
        Department department = Department.TECH;
        String subject = "Владельцу аккаунта AC_" + accountId + " от компании Majordomo";
        String contactEmails = accountHelper.getEmail(account);
        String content = cerbTicketRepository.findByViolation("phpmail").getTicketMessage()
                .replace("REPLACE_ACCOUND_ID", accountId);

        cerbApiClient.sendTicket(subject, department, contactEmails, content);

        return ResponseEntity.ok().build();
    }
}