package ru.majordomo.hms.personmgr.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.Views;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.notification.Notification;
import ru.majordomo.hms.personmgr.repository.NotificationRepository;
import ru.majordomo.hms.personmgr.validation.ObjectId;

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
        return findAll();
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
        return findAll();
    }

    @RequestMapping(value = "/{accountId}/notifications/{notificationId}",
            method = RequestMethod.POST)
    public ResponseEntity<Object> addNotification(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "notificationId") String notificationId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        setNotifications(accountId, notificationId, true, request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/notifications/{notificationId}",
            method = RequestMethod.DELETE)
    public ResponseEntity<Object> deleteNotification(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "notificationId") String notificationId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        setNotifications(accountId, notificationId, false, request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private ResponseEntity<Notification> findOne(String notificationId) {
        Notification notification = notificationRepository.findOne(notificationId);

        if(notification == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(notification, HttpStatus.OK);
    }

    private ResponseEntity<List<Notification>> findAll() {
        List<Notification> notifications = notificationRepository.findAll();

        return new ResponseEntity<>(notifications, HttpStatus.OK);
    }

    private void setNotifications(String accountId, String notificationId, boolean state, SecurityContextHolderAwareRequestWrapper request) {

        PersonalAccount account =  accountManager.findOne(accountId);
        boolean change = false;
        Set<MailManagerMessageType> notifications = account.getNotifications();
        Notification notification = notificationRepository.findOne(notificationId);
        MailManagerMessageType messageType = notification.getType();

        if (!notifications.contains(messageType)
                && state) {
            notifications.add(messageType);
            change = true;
        } else if (notifications.contains(messageType)
                && !state) {
            notifications.remove(messageType);
            change = true;
        }
        if (change) {
            accountManager.setNotifications(accountId, notifications);
            String operator = request.getUserPrincipal().getName();
            String notificationName = notification.getName();
            String message = notificationName + (state ? "включено." : "отключено.");
            addHistoryMessage(operator, accountId, message);
        }
    }
}