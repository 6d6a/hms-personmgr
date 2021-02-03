package ru.majordomo.hms.personmgr.controller.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.Department;
import ru.majordomo.hms.personmgr.dto.cerb.Ticket;
import ru.majordomo.hms.personmgr.dto.cerb.api.AttachmentCerberus;
import ru.majordomo.hms.personmgr.dto.cerb.api.AttachmentDownloadResponse;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.AccountTicket;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.AccountTicketRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.Cerb.CerbApiClient;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.servlet.http.HttpServletResponse;

import static ru.majordomo.hms.personmgr.common.Utils.buildAttachment;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/{accountId}/support-request")
public class SupportRequestRestController extends CommonRestController {

    private final AccountHelper accountHelper;
    private final CerbApiClient cerbApiClient;
    private final AccountTicketRepository accountTicketRepository;

    @PostMapping(value = "")
    public ResponseEntity<String> sendSupportRequest(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestParam(value = "file", required = false) MultipartFile[] files,
            @RequestParam("message") String messageText,
            @RequestParam("subject") String subject,
            @RequestParam("email") String email,
            @RequestParam("department") Department department
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        checkEmail(account, email);

        Map<String, String> attachment = new HashMap<>();

        if (files != null && files.length > 0) {
            try {
                attachment = buildAttachment(files);
            } catch (IOException e) {
                e.printStackTrace();
                throw new ParameterValidationException("Ошибка при обработке вложения.");
            }
        }

        Ticket ticket = cerbApiClient.processUserRequestToCerberus(
                "Запрос из панели " + account.getName() + " (Тема запроса: " + subject + ")",
                email,
                messageText,
                department,
                attachment
        );

        if (ticket == null) {
            throw new ParameterValidationException("Не удалось создать запрос в службу поддержки.");
        }

        AccountTicket accountTicket = new AccountTicket();
        accountTicket.setTicketId(ticket.getTicketId());
        accountTicket.setMask(ticket.getMask());
        accountTicket.setPersonalAccountId(account.getId());
        accountTicket.setSenderId(ticket.getSenderId());
        accountTicket.setSubject(subject);
        accountTicketRepository.save(accountTicket);

        return new ResponseEntity<>("Ваше сообщение отправлено. Спасибо.", HttpStatus.OK);
    }

    @PostMapping(value = "/tickets/{ticketId}/reply")
    public ResponseEntity<String> replySupportRequest(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "ticketId") String ticketId,
            @RequestParam(value = "file", required = false) MultipartFile[] files,
            @RequestParam("message") String messageText
    ) {
        AccountTicket accountTicket = accountTicketRepository.findByPersonalAccountIdAndTicketId(accountId, Integer.valueOf(ticketId));

        Map<String, String> attachment = new HashMap<>();

        if (files != null && files.length > 0) {
            try {
                attachment = buildAttachment(files);
            } catch (IOException e) {
                e.printStackTrace();
                throw new ParameterValidationException("Ошибка при обработке вложения.");
            }
        }

        cerbApiClient.processUserReplayToTicket(
                accountTicket.getTicketId(),
                accountTicket.getSenderId(),
                messageText,
                attachment
        );

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

    @GetMapping("/tickets")
    public ResponseEntity<List<AccountTicket>> listAllTickets(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        List<AccountTicket> accountTickets = accountTicketRepository.findByPersonalAccountId(accountId);

        return new ResponseEntity<>(accountTickets, HttpStatus.OK);
    }

    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<Ticket> getTicket(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "ticketId") String ticketId
    ) {
        AccountTicket accountTicket = accountTicketRepository.findByPersonalAccountIdAndTicketId(accountId, Integer.valueOf(ticketId));

        Ticket ticket = cerbApiClient.getTicketWithMessages(accountTicket.getTicketId());

        if (ticket == null) {
            throw new ParameterValidationException("Ошибка при получении тикета");
        }

        return new ResponseEntity<>(ticket, HttpStatus.OK);
    }

    @GetMapping("/tickets/{ticketId}/file/{fileId}")
    public void downloadFile(
            @ObjectId(PersonalAccount.class) @PathVariable("accountId") String accountId,
            @PathVariable(value = "ticketId") int ticketId,
            @PathVariable(value = "fileId") int fileId,
            HttpServletResponse response
    ) {

        AccountTicket accountTicket = accountTicketRepository.findByPersonalAccountIdAndTicketId(accountId, ticketId);

        Ticket ticket = cerbApiClient.getTicketWithMessages(accountTicket.getTicketId());

        if (ticket == null || ticket.getMessages() == null) {
            throw new ParameterValidationException("Ошибка при получении сообщений");
        }

        AttachmentCerberus attachmentCerberus = ticket.getMessages()
                .stream().flatMap(mes -> mes.getAttachments().stream())
                .filter(at -> at.getAttachmentId() == fileId).findFirst().orElse(null);

        if (attachmentCerberus == null) {
            throw new ParameterValidationException("Ошибка при поиске присоединенного файла");
        }
        AttachmentDownloadResponse downloadedAttachment = cerbApiClient.getAttachment(fileId);
        if (downloadedAttachment == null) {
            throw new ParameterValidationException("Ошибка при загрузке присоединенного файла");
        }

        try {
            response.setContentType(downloadedAttachment.getContentType());
            response.setHeader("Content-disposition", "attachment; filename=" + attachmentCerberus.getName());
            OutputStream os = response.getOutputStream();
            os.write(downloadedAttachment.getBody());
            os.flush();
        } catch (IOException e) {
            logger.error("Не удалось отдать присоединенный файл, exceptionMessage: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
