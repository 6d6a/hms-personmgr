package ru.majordomo.hms.personmgr.controller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.DocumentType;
import ru.majordomo.hms.personmgr.model.account.AccountDocument;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.AccountDocumentRepository;
import ru.majordomo.hms.personmgr.service.Document.DocumentBuilder;
import ru.majordomo.hms.personmgr.service.Document.DocumentBuilderFactory;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Utils.convertFileToByteArrayOutputStream;


@RestController
@RequestMapping("/{accountId}/document")
public class AccountDocumentRestController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DocumentBuilderFactory documentBuilderFactory;
    private final AccountDocumentRepository accountDocumentRepository;

    @Autowired
    public AccountDocumentRestController(
            DocumentBuilderFactory documentBuilderFactory,
            AccountDocumentRepository accountDocumentRepository
    ){
        this.documentBuilderFactory = documentBuilderFactory;
        this.accountDocumentRepository = accountDocumentRepository;
    }

    @GetMapping("/old/{documentType}")
    public ResponseEntity<List<AccountDocument>> getOldDocuments(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "documentType") DocumentType documentType
    ) {
        List<AccountDocument> documents = accountDocumentRepository.
                findByPersonalAccountIdAndType(
                        accountId,
                        documentType
        );

        if (documents == null || documents.isEmpty()){
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(documents);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/id/{accountDocumentId}")
    public ResponseEntity<AccountDocument> deleteDocumentById(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountDocument.class) @PathVariable(value = "accountDocumentId") String accountDocumentId
    ){
        AccountDocument document = accountDocumentRepository
                .findOneByPersonalAccountIdAndId(
                        accountId,
                        accountDocumentId
                );

        if (document == null) {
            return ResponseEntity.notFound().build();
        } else {
            accountDocumentRepository.delete(document);
            return ResponseEntity.ok(document);
        }
    }

    @GetMapping("/id/{accountDocumentId}")
    @ResponseBody
    public void getDocumentById(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountDocument.class) @PathVariable(value = "accountDocumentId") String accountDocumentId,
            HttpServletResponse response
    ){
        AccountDocument document = accountDocumentRepository.findOneByPersonalAccountIdAndId(accountId, accountDocumentId);
        if (document != null) {
            DocumentBuilder documentBuilder = this.documentBuilderFactory.getBuilder(
                    document.getType(),
                    accountId,
                    document.getParameters()
            );

            File file = documentBuilder.buildFromAccountDocument(document);

            String contentType = "application/pdf";

            printContentFromFileToResponseOutputStream(
                    response,
                    contentType,
                    file.getName(),
                    file.getAbsolutePath()
            );

        }
    }

    @GetMapping("/{documentType}")
    @ResponseBody
    public void getDocument(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "documentType") DocumentType documentType,
            @RequestParam Map<String, String> params,
            HttpServletResponse response
    ) {
        DocumentBuilder documentBuilder = this.documentBuilderFactory.getBuilder(
                documentType,
                accountId,
                params
        );

        File file = documentBuilder.build();

        printContentFromFileToResponseOutputStream(
                response,
                file.getName(),
                file.getAbsolutePath()
        );

    }

    private void printContentFromFileToResponseOutputStream(
            HttpServletResponse response,
            String fileName,
            String fileDir
    ) {
        String contentType = getContentTypeByFileName(fileName);

        printContentFromFileToResponseOutputStream(response, contentType, fileName, fileDir);
    }

    private void printContentFromFileToResponseOutputStream(
            HttpServletResponse response,
            String contentType,
            String fileName,
            String fileDir
    ) {
        try {
            response.setContentType(contentType);
            response.setHeader("Content-disposition", "attachment; filename=" + fileName);
            ByteArrayOutputStream baos;
            baos = convertFileToByteArrayOutputStream(fileDir);
            OutputStream os = response.getOutputStream();
            baos.writeTo(os);
            os.flush();
        } catch (IOException e) {
            logger.error("Не удалось отдать документ, exceptionMessage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getContentTypeByFileName(String fileName){
        String[] splitedFileName = fileName.split("\\.");
        String extension = splitedFileName[splitedFileName.length - 1];

        switch (extension) {
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "png":
                return "image/png";
            default:
                return "text/plain";
        }
    }
}
