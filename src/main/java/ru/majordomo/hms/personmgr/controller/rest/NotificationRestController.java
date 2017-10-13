package ru.majordomo.hms.personmgr.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.Views;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.notification.Notification;
import ru.majordomo.hms.personmgr.repository.NotificationRepository;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.common.Constants.*;

@RestController
@Validated
public class NotificationRestController extends CommonRestController {
    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationRestController(
            NotificationRepository notificationRepository
    ) {
        this.notificationRepository = notificationRepository;
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

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{accountId}/notifications/send")
    public ResponseEntity<Void> sendNotificationToClient(
            @RequestBody Map<String, Object> message,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        if (!message.get(TYPE_KEY).equals("EMAIL")
                && !message.get(TYPE_KEY).equals("SMS")
        ) {
            throw new ParameterValidationException("Не указан параметр " + TYPE_KEY);
        }

        if (message.get(API_NAME_KEY) == null) {
            throw new ParameterValidationException("Не указан параметр " + API_NAME_KEY);
        }

        Map<String, Object> parameters = new HashMap<>();
        if (message.get(PARAMETRS_KEY) != null) {
            try {
                parameters = (Map<String, Object>) message.get(PARAMETRS_KEY);
            } catch (ClassCastException e) {
                throw new ParameterValidationException("Неверный формат " + PARAMETRS_KEY + " , должен быть Map<String, Object> или null");
            }
        }

        return ResponseEntity.ok().build();
    }

    private ResponseEntity<Notification> findOne(String notificationId) {
        Notification notification = notificationRepository.findOne(notificationId);

        if(notification == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(notification, HttpStatus.OK);
    }
}