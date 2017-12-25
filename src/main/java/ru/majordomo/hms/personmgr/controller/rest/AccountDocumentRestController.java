package ru.majordomo.hms.personmgr.controller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.DocumentType;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.Document.DocumentBuilder;
import ru.majordomo.hms.personmgr.service.Document.DocumentBuilderFactory;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Utils.convertFileToByteArrayOutputStream;


@RestController
@RequestMapping("/{accountId}/document")
public class AccountDocumentRestController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DocumentBuilderFactory documentBuilderFactory;

    @Autowired
    public AccountDocumentRestController(
            DocumentBuilderFactory documentBuilderFactory
    ){
        this.documentBuilderFactory = documentBuilderFactory;
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
                DocumentType.VIRTUAL_HOSTING_BUDGET_CONTRACT,
                accountId,
                params
        );

        File file = documentBuilder.build();

        String contentType = "application/pdf";

        printContentFromFileToResponseOutputStream(
                response,
                contentType,
                file.getName(),
                file.getAbsolutePath()
        );

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
}
