package ru.majordomo.hms.personmgr.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import ru.majordomo.hms.personmgr.common.Views;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.notification.Notification;
import ru.majordomo.hms.personmgr.repository.NotificationRepository;
import ru.majordomo.hms.personmgr.service.AccountNotificationHelper;
import ru.majordomo.hms.personmgr.validation.ObjectId;

@RestController
@Validated
public class NotificationRestController extends CommonRestController {
    private final NotificationRepository notificationRepository;
    private final AccountNotificationHelper accountNotificationHelper;

    @Autowired
    public NotificationRestController(
            NotificationRepository notificationRepository,
            AccountNotificationHelper accountNotificationHelper
    ) {
        this.notificationRepository = notificationRepository;
        this.accountNotificationHelper = accountNotificationHelper;
    }

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
        Notification notification = notificationRepository.findOne(notificationId);

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
}