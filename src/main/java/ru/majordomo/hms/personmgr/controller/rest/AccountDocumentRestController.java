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

            byte[] file = documentBuilder.buildFromAccountDocument(document);

            printContentFromFileToResponseOutputStream(
                    response,
                    document.getType(),
                    file
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

        byte[] file = documentBuilder.build();

        printContentFromFileToResponseOutputStream(
                response,
                documentType,
                file
        );

    }

//    private void printContentFromFileToResponseOutputStream(
//            HttpServletResponse response,
//            String contentType,
//            String fileName,
//            String fileDir
//    ) {
//        try {
//            response.setContentType(contentType);
//            response.setHeader("Content-disposition", "attachment; filename=" + fileName);
//            ByteArrayOutputStream baos;
//            baos = convertFileToByteArrayOutputStream(fileDir);
//            OutputStream os = response.getOutputStream();
//            baos.writeTo(os);
//            os.flush();
//        } catch (IOException e) {
//            logger.error("Не удалось отдать документ, exceptionMessage: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

    private void printContentFromFileToResponseOutputStream(
            HttpServletResponse response,
            DocumentType type,
            byte[] file
    ) {
        try {
            response.setContentType(getContentType(type));
            response.setHeader("Content-disposition", "attachment; filename=" + getFileName(type));
            ByteArrayOutputStream baos = new ByteArrayOutputStream(file.length);
            baos.write(file, 0, file.length);
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

    private String getContentType(DocumentType type){
        String contentType = "";
        switch (type){
            case REGISTRANT_DOMAIN_CERTIFICATE:
                contentType = "image/png";

                break;
            case VIRTUAL_HOSTING_CONTRACT:
            case VIRTUAL_HOSTING_BUDGET_CONTRACT:
            case VIRTUAL_HOSTING_COMMERCIAL_PROPOSAL:
                contentType = "application/pdf";

                break;
            case VIRTUAL_HOSTING_BUDGET_SUPPLEMENTARY_AGREEMENT:
                contentType = "application/msword";

                break;
            case VIRTUAL_HOSTING_OFERTA:
            default:
                contentType = "application/octet-stream";
        }

        return contentType;
    }

    private String getFileName(DocumentType type){
        String fileName = type.name().toLowerCase();
        String extension;

        switch (type){
            case REGISTRANT_DOMAIN_CERTIFICATE:
                extension = ".png";

                break;
            case VIRTUAL_HOSTING_OFERTA:
            case VIRTUAL_HOSTING_CONTRACT:
            case VIRTUAL_HOSTING_BUDGET_CONTRACT:
            case VIRTUAL_HOSTING_COMMERCIAL_PROPOSAL:
                extension = ".pdf";

                break;
            case VIRTUAL_HOSTING_BUDGET_SUPPLEMENTARY_AGREEMENT:
                extension = ".doc";

                break;
            default:
                extension = "";
        }

        return fileName + extension;
    }
}
