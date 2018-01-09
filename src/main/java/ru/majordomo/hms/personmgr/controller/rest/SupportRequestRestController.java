package ru.majordomo.hms.personmgr.controller.rest;

import com.google.common.collect.ImmutableMap;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.validation.ObjectId;

@RestController
@RequestMapping("/{accountId}/support-request")
@Validated
public class SupportRequestRestController extends CommonRestController {

    private enum Department {
        INFO,
        TECH,
        FIN,
        DOMAIN,
        TEST
    }

    private Map<Department, String> departmentEmails = ImmutableMap.of(
            Department.INFO, "info@majordomo.ru",
            Department.TECH, "support@majordomo.ru",
            Department.FIN, "billing@majordomo.ru",
            Department.DOMAIN, "domain@majordomo.ru",
            Department.TEST, "il.dd84@gmail.com"
    );

    private final AccountHelper accountHelper;

    @Autowired
    public SupportRequestRestController(
            AccountHelper accountHelper
    ) {
        this.accountHelper = accountHelper;
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

        List<String> emails = accountHelper.getEmails(account);
        emails.forEach(String::toLowerCase);

        email = email.toLowerCase();

        if (!emails.contains(email)) {
            throw new ParameterValidationException("Адрес " +
                    email + " отсутствует в списке контактных адресов вашего аккаунта. " +
                    "Выберите корректный адрес для отправки заявки.");
        }

        SimpleServiceMessage message = new SimpleServiceMessage();
        message.addParam("email", departmentEmails.get(department));
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

        try {
            if (files != null && files.length > 0) {
                byte[] fileBytes;
                String fileType, fileName;

                if (files.length == 1 && !files[0].isEmpty()) {
                    fileName = files[0].getOriginalFilename();
                    fileBytes = files[0].getBytes();
                    fileType = files[0].getContentType();
                } else {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    ZipOutputStream zipOut = new ZipOutputStream(outputStream);
                    for (MultipartFile oneFile : files) {
                        InputStream inputStream = oneFile.getInputStream();
                        ZipEntry zipEntry = new ZipEntry(oneFile.getOriginalFilename());
                        zipOut.putNextEntry(zipEntry);

                        byte[] bytes = new byte[1024];
                        int length;
                        while((length = inputStream.read(bytes)) >= 0) {
                            zipOut.write(bytes, 0, length);
                        }
                        inputStream.close();
                    }
                    zipOut.close();
                    outputStream.close();
                    fileBytes = outputStream.toByteArray();
                    fileName = "attachment.zip";
                    fileType = "application/zip";
                }

                if (fileBytes != null && fileType != null && fileName != null) {
                    Map<String, String> attachment = new HashMap<>();
                    attachment.put("body", Base64.getMimeEncoder().encodeToString(fileBytes));
                    attachment.put("mime_type", fileType);
                    attachment.put("filename", fileName);

                    message.addParam("attachment", attachment);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new ParameterValidationException("Ошибка при обработке вложения.");
        }


        publisher.publishEvent(new SendMailEvent(message));

        return new ResponseEntity<>("Ваше сообщение отправлено. Спасибо.", HttpStatus.OK);
    }
}
