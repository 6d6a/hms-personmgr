package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.majordomo.hms.personmgr.dto.partners.Code;
import ru.majordomo.hms.personmgr.dto.request.EmailInviteRequest;
import ru.majordomo.hms.personmgr.dto.request.InviteRequest;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.AccountNotificationHelper;
import ru.majordomo.hms.personmgr.service.AccountStatHelper;
import ru.majordomo.hms.personmgr.feign.PartnersFeignClient;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@RestController
@Validated
public class InviteRestController {
    private PersonalAccountManager accountManager;
    private AccountStatHelper accountStatHelper;
    private AccountNotificationHelper notificationHelper;
    private PartnersFeignClient partnersFeignClient;
    private final int maxInvitesPerDay;

    @Autowired
    public InviteRestController(
            PersonalAccountManager accountManager,
            AccountStatHelper accountStatHelper,
            AccountNotificationHelper notificationHelper,
            PartnersFeignClient partnersFeignClient,
            @Value("${invites.max_invites_per_day}") int maxInvitesPerDay
    ) {
        this.accountManager = accountManager;
        this.accountStatHelper = accountStatHelper;
        this.notificationHelper = notificationHelper;
        this.partnersFeignClient = partnersFeignClient;
        this.maxInvitesPerDay = maxInvitesPerDay;
    }

    @PostMapping("/{accountId}/invite/send")
    public ResponseEntity<Object> toggleDeleted(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody @Valid InviteRequest inviteRequestBody
    ) {
        PersonalAccount account = accountManager.findOne(accountId);
        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт не активен");
        }

        int sentToday = accountStatHelper.getCountInviteSend(accountId, LocalDate.now());
        if (sentToday >= maxInvitesPerDay) {
            throw new ParameterValidationException("Отправлено слишком много приглашений, действие временно заблокировано");
        }

        Code code = null;
        try {
            code = partnersFeignClient.getHmsAccountCode(accountId);
        } catch (Exception ignore) {}

        if (code == null) {
            throw new InternalApiException("Не найден промокод аккаунта");
        }

        if (inviteRequestBody instanceof EmailInviteRequest) {
            EmailInviteRequest emailInvite = (EmailInviteRequest) inviteRequestBody;
            String codeString = code.getCode();
            Set<String> uniqueEmails = new HashSet<>(emailInvite.getEmails());

            if (sentToday + uniqueEmails.size() > maxInvitesPerDay) {
                throw new ParameterValidationException("Отправлено слишком много приглашений, действие временно заблокировано");
            }

            uniqueEmails.forEach(email -> {
                notificationHelper.sendInviteMail(account, email, codeString);
                accountStatHelper.addEmailInvite(accountId, email);
            });

        } else {
            throw new ParameterValidationException("Неизвестный тип приглашения");
        }

        return ResponseEntity.noContent().build();
    }
}
