package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.Department;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.config.EmailsConfig;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.common.Utils.buildAttachment;

@RestController
@RequestMapping("/{accountId}/support-request")
@Validated
public class SupportRequestRestController extends CommonRestController {

    private final AccountHelper accountHelper;
    private Map<Department, String> emails;

    @Autowired
    public SupportRequestRestController(
            AccountHelper accountHelper,
            EmailsConfig emailsConfig
    ) {
        this.accountHelper = accountHelper;
        this.emails = emailsConfig.getDepartments();
    }

    @PostMapping(value = "")
    public ResponseEntity<String> sendSupportRequest(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestParam(value = "file", required = false) MultipartFile[] files,
            @RequestParam("message") String messageText,
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("department") Department department
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        checkEmail(account, email);

        SimpleServiceMessage message = new SimpleServiceMessage();
        message.addParam("email", emails.get(department));
        message.addParam("api_name", "MajordomoHMSFeedbackRequest");
        message.addParam("priority", 10);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("account_name", account.getName());
        parameters.put("client_name", name);
        parameters.put("message_text", messageText);
        parameters.put("from", email);
        parameters.put("reply_to", email);

        message.addParam("parametrs", parameters);

        if (files != null && files.length > 0) {
            try {
                message.addParam("attachment", buildAttachment(files));
            } catch (IOException e) {
                e.printStackTrace();
                throw new ParameterValidationException("Ошибка при обработке вложения.");
            }
        }

        publisher.publishEvent(new SendMailEvent(message));

        return new ResponseEntity<>("Ваше сообщение отправлено. Спасибо.", HttpStatus.OK);
    }

    private void checkEmail(PersonalAccount account, String email) {
        List<String> emails = accountHelper.getEmails(account).stream().map(String::toLowerCase).collect(Collectors.toList());

        email = email.toLowerCase();

        if (!emails.contains(email)) {
            throw new ParameterValidationException("Адрес " +
                    email + " отсутствует в списке контактных адресов вашего аккаунта. " +
                    "Выберите корректный адрес для отправки заявки.");
        }
    }
}
