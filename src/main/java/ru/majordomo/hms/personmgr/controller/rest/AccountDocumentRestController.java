package ru.majordomo.hms.personmgr.controller.rest;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.NotImplementedException;
import org.codehaus.jettison.json.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.DocumentType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.request.DocumentPreviewRequest;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.NotEnoughMoneyException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.model.account.*;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.*;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.AccountNotificationHelper;
import ru.majordomo.hms.personmgr.service.AccountServiceHelper;
import ru.majordomo.hms.personmgr.service.ChargeMessage;
import ru.majordomo.hms.personmgr.service.Document.DocumentBuilder;
import ru.majordomo.hms.personmgr.service.Document.DocumentBuilderFactory;
import ru.majordomo.hms.personmgr.validation.ObjectId;
import ru.majordomo.hms.rc.user.resources.Domain;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static ru.majordomo.hms.personmgr.common.Constants.ORDER_DOCUMENT_FREE_PER_YEAR;
import static ru.majordomo.hms.personmgr.common.Constants.ORDER_DOCUMENT_PACKAGE_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.DocumentType.*;

@RestController
@RequestMapping(value = {"/{accountId}/document", "/document"})
@RequiredArgsConstructor
public class AccountDocumentRestController extends CommonRestController {

    private final DocumentBuilderFactory documentBuilderFactory;
    private final AccountDocumentRepository accountDocumentRepository;
    private final AccountHelper accountHelper;
    private final DocumentOrderRepository documentOrderRepository;
    private final DocumentOrderCountRepository documentOrderCountRepository;
    private final AccountNotificationHelper accountNotificationHelper;
    @Value("${mail_manager.document_order_email}")
    private final String documentOrderEmail;
    private final AccountServiceHelper accountServiceHelper;

    private final List<DocumentType> DEFAULT_DOCUMENT_TYPES = Collections.unmodifiableList(Arrays.asList(
            VIRTUAL_HOSTING_BUDGET_CONTRACT,
            VIRTUAL_HOSTING_BUDGET_SUPPLEMENTARY_AGREEMENT,
            VIRTUAL_HOSTING_COMMERCIAL_PROPOSAL,
            VIRTUAL_HOSTING_NOTIFY_RF
    ));

    protected final String temporaryFilePath = System.getProperty("java.io.tmpdir") + "/";

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

    @PreAuthorize("hasAuthority('ACCOUNT_DOCUMENT_ORDER_VIEW')")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<Page<DocumentOrder>> listAll(
            @ObjectId(PersonalAccount.class) @PathVariable("accountId") String accountId,
            Pageable pageable
    ) {
        Page<DocumentOrder> orders = documentOrderRepository.findByPersonalAccountId(accountId, pageable);

        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_DOCUMENT_ORDER_VIEW')")
    @GetMapping("/order/{documentOrderId}")
    @ResponseBody
    public void downloadDocumentOrder(
            @ObjectId(PersonalAccount.class) @PathVariable("accountId") String accountId,
            @ObjectId(DocumentOrder.class) @PathVariable("documentOrderId") String documentOrderId,
            HttpServletResponse response
    ) {
        DocumentOrder documentOrder = documentOrderRepository.findOneByIdAndPersonalAccountId(documentOrderId, accountId);
        if (documentOrder == null) {
            throw new ResourceNotFoundException("???? ???????????? ?????????? ????????????????????");
        }

        PersonalAccount account = accountManager.findOne(documentOrder.getPersonalAccountId());

        documentOrder.getParams().put("withoutStamp", "true");

        List<Domain> domains = accountHelper.getDomains(account);

        Map<String, byte[]> fileMap = buildFileMap(documentOrder, domains);

        if (!documentOrder.getErrors().isEmpty()) {
            throw new InternalApiException(documentOrder.getErrors().toString());
        }

        byte[] zipFileByteArray;
        try {
            zipFileByteArray = getZipFileByteArray(fileMap, accountId);
        } catch (IOException e) {
            throw new InternalApiException("???????????? ?????? ???????????????? zip-????????????");
        }

        printContentFromFileToResponseOutputStream(
                response,
                "application/zip",
                accountId + ".zip",
                zipFileByteArray
        );
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

    @GetMapping("/check")
    public ResponseEntity<DocumentOrder> check(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestParam Map<String, String> params
    ){
        DocumentOrder documentOrder = new DocumentOrder();

        documentOrder.setPostalAddress(params.getOrDefault("postalAddress", null));
        documentOrder.setParams(params);

        validatePostalAddressInDocumentOrder(documentOrder);

        PersonalAccount account = accountManager.findOne(accountId);

        DEFAULT_DOCUMENT_TYPES.forEach(documentType ->{
            try {
                checkDocument(documentType, accountId, params);
                documentOrder.getDocumentTypes().add(documentType);
            } catch (Exception e) {
                documentOrder.setChecked(false);
                documentOrder.getErrors().put(documentType.name(), e.getMessage());
            }
        });

        if (documentOrder.getErrors().isEmpty()) {
            documentOrder.setChecked(true);
        }

        List<Domain> domains = accountHelper.getDomains(account);

        domains = filterDomains(domains);

        if (domains != null && !domains.isEmpty()) {
            for (Domain domain : domains){
                try {
                    params.put("domainId", domain.getId());

                    checkDocument(REGISTRANT_DOMAIN_CERTIFICATE, accountId, params);

                    documentOrder.getDomainIds().add(domain.getId());
                } catch (Exception ignore){
                    logger.info("?????? ???????????? " + domain.getName() + " ?????????? ?????????????????????? ???????????????????? ???? ??????????????: "
                            + ignore.getMessage());
                }
            }
        }

        int freeOrdersAvailable;
        DocumentOrderCount documentOrderCount = documentOrderCountRepository.findByPersonalAccountId(accountId);
        if (documentOrderCount != null && documentOrderCount.getLastOrderedYear() >= Year.now().getValue()) {
            freeOrdersAvailable = ORDER_DOCUMENT_FREE_PER_YEAR - documentOrderCount.getTimesOrdered();
            if (freeOrdersAvailable < 0) {
                freeOrdersAvailable = 0;
            }
        } else {
            freeOrdersAvailable = ORDER_DOCUMENT_FREE_PER_YEAR;
        }

        if (freeOrdersAvailable <= 0) {
            try {
                PaymentService paymentService = paymentServiceRepository.findByOldId(ORDER_DOCUMENT_PACKAGE_SERVICE_ID);
                accountHelper.checkBalance(account, paymentService);
            } catch (NotEnoughMoneyException e) {
                documentOrder.getErrors().put("balance", e.getMessage());
                documentOrder.getErrors().put("requiredAmount", e.getRequiredAmount().toString());
            }
        } else {
            documentOrder.setFreeOrdersAvailable(freeOrdersAvailable);
        }

        if (!documentOrder.getErrors().isEmpty()) {
            return ResponseEntity.badRequest().body(documentOrder);
        }

        return ResponseEntity.ok(documentOrder);
    }

    @GetMapping(value = "/times-ordered", produces= MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> checkEligibilityForFreeOrder(@PathVariable String accountId) throws JSONException {
        DocumentOrderCount documentOrderCount = documentOrderCountRepository.findByPersonalAccountId(accountId);
        JSONObject response = new JSONObject();
        response.put("max", ORDER_DOCUMENT_FREE_PER_YEAR);
        if (documentOrderCount == null || documentOrderCount.getLastOrderedYear() < Year.now().getValue()) {
            response.put("current", 0);
        } else {
            response.put("current", documentOrderCount.getTimesOrdered());
        }
        return new ResponseEntity<>(response.toString(), HttpStatus.OK);
    }

    @PostMapping("/order")
    public ResponseEntity<Object> order(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody DocumentOrder documentOrder,
            SecurityContextHolderAwareRequestWrapper request
    ){
        PersonalAccount account = accountManager.findOne(accountId);

        Boolean agreement = documentOrder.getAgreement();

        validatePostalAddressInDocumentOrder(documentOrder);

        if (!agreement) {
            throw new ParameterValidationException("???????????????????? ?????????????????????? ?????????? ????????????????????");
        }
        documentOrder.getParams().put("withoutStamp", "true");

        documentOrder.setPersonalAccountId(accountId);

        List<Domain> domains = accountHelper.getDomains(account);

        Map<String, byte[]> fileMap = buildFileMap(documentOrder, domains);
        String operator = request.getUserPrincipal().getName();

        try {
            DocumentOrderCount timesOrdered = documentOrderCountRepository.findByPersonalAccountId(accountId);
            SimpleServiceMessage response = null;

            if (timesOrdered == null
                    || timesOrdered.getLastOrderedYear() < Year.now().getValue()
                    || timesOrdered.getTimesOrdered() < ORDER_DOCUMENT_FREE_PER_YEAR) {
                if (timesOrdered == null) {
                    timesOrdered = new DocumentOrderCount();
                    timesOrdered.setLastOrderedYear(Year.now().getValue());
                    timesOrdered.setPersonalAccountId(accountId);
                    timesOrdered.setTimesOrdered(1);
                } else {
                    timesOrdered.incrementOrderedCount();
                }
                documentOrderCountRepository.save(timesOrdered);
            } else {
                PaymentService paymentService = paymentServiceRepository.findByOldId(ORDER_DOCUMENT_PACKAGE_SERVICE_ID);
                BigDecimal cost = accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), paymentService);
                ChargeMessage chargeMessage = new ChargeMessage.Builder(paymentService)
                        .setAmount(cost)
                        .build();
                response = accountHelper.charge(account, chargeMessage);
            }

            documentOrder.setPaid(true);
            documentOrder.setDocumentNumber(response != null ? (String) response.getParam("documentNumber") : null);
            documentOrder = documentOrderRepository.save(documentOrder);

            // ???????????????? ???????????? c ?????????????????????? ??????????????????
            sendDocumentOrderToSecretary(account, documentOrder, domains, fileMap, operator);

            history.save(accountId, "?????????????? ?????????? ???????????????????? " + documentOrder.toString(), operator);

        } catch (Exception e){
            logger.error("???? ?????????????? ?????????????? ???? ?????????? ???????????????????? " + e.getMessage());
            documentOrder.setChecked(false);
            documentOrder.getErrors().put("balance", e.getMessage());
        }

        if (!documentOrder.getErrors().isEmpty()) {
            return ResponseEntity.badRequest().body(documentOrder);
        } else {
            return ResponseEntity.ok(documentOrder);
        }
    }

    /**
     * ???????????????? ???????? ???????????????? ?????? ?????????????? ??????????????????
     * ?????????????????? -> ?????????? ???????????????????? -> ?????????????????? ?? ?????????????????????? ???????? -> ??????????????
     * @param accountId
     * @param documentType
     * @param params ???????????????????????????? ?????????????????? ??????????????, ???????????????? withoutStamp, phone, urfio ?? ??.??
     * @param response ?????????? ?? ???????????? ?? Transfer-Encoding: chunked
     */
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

    /**
     * ?????????????????????????????? ???????????????? ?????? ?????????????????? ???????????????????? (???????????? ?? billing2)
     * @param billing2Type mj-rpc.Contract.type ?? DocumentType.billing2Type
     * @param response ???????????????? application/pdf ?? Transfer-Encoding: chunked
     */
    @PostMapping(path = "/preview/billing2/{billing2Type}")
    @ResponseBody
    public void getDocumentPreview(
            @NotEmpty @PathVariable("billing2Type") String billing2Type,
            @Validated @RequestBody DocumentPreviewRequest documentPreviewRequest,
            HttpServletResponse response
    ) {
        DocumentType documentType = Arrays.stream(values()).filter(type -> billing2Type.equals(type.getBilling2Type()))
                .findFirst()
                .orElseThrow(() -> new ParameterValidationException("???? ?????????????? ?????????? ???????????????? ???????????????????????? ????????"));

        try {
            DocumentBuilder documentBuilder = this.documentBuilderFactory.getBuilder(
                    documentType,
                    null,
                    null
            );

            byte[] fileBin = documentBuilder.buildPreview(documentPreviewRequest);

            printContentFromFileToResponseOutputStream(
                    response,
                    documentType,
                    fileBin
            );
        } catch (NotImplementedException e) {
            throw new InternalApiException("?????????????????????????????? ???????????????? ???? ???????????????????? ?????? ??????????????????: " + documentType.getNameForHuman());
        }
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
//            logger.error("???? ?????????????? ???????????? ????????????????, exceptionMessage: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

    private void printContentFromFileToResponseOutputStream(
            HttpServletResponse response,
            DocumentType type,
            byte[] file
    ) {
        String contentType = getContentType(type);
        String fileName = getFileName(type);

        printContentFromFileToResponseOutputStream(
                response,
                contentType,
                fileName,
                file
        );
    }

    private void printContentFromFileToResponseOutputStream(
            HttpServletResponse response,
            String contentType,
            String fileName,
            byte[] file
    ) {
        try {
            response.setContentType(contentType);
            response.setHeader("Content-disposition", "attachment; filename=" + fileName);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(file.length);
            baos.write(file, 0, file.length);
            OutputStream os = response.getOutputStream();
            baos.writeTo(os);
            os.flush();
        } catch (IOException e) {
            logger.error("???? ?????????????? ???????????? ????????????????, exceptionMessage: " + e.getMessage());
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
            case VIRTUAL_HOSTING_NOTIFY_RF:
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
            case VIRTUAL_HOSTING_NOTIFY_RF:
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

    private void saveByteArrayToZipFile(File zipFile, String zipEntryName, byte[] content) throws IOException {

        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));


        ZipEntry e = new ZipEntry(zipEntryName);
        out.putNextEntry(e);

        out.write(content, 0, content.length);
        out.closeEntry();

        out.close();
    }

    private void saveFilesToZip(File zipFile, Map<String, byte[]> fileMap) throws IOException {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));

        Set<Map.Entry<String, byte[]>> fileNameContent = fileMap.entrySet();
        for (Map.Entry<String, byte[]> entry: fileNameContent) {
            ZipEntry e = new ZipEntry(entry.getKey());
            out.putNextEntry(e);

            out.write(entry.getValue(), 0, entry.getValue().length);
            out.closeEntry();
        }



        out.close();
    }

    private void checkDocument(DocumentType type, String accountId, Map<String, String> params) {
        DocumentBuilder builder = getBuilder(type, accountId, params);
        builder.check();
    }

    private byte[] buildDocument(DocumentType type, String accountId, Map<String, String> params){
        DocumentBuilder builder = getBuilder(type, accountId, params);
        return builder.build();
    }

    private DocumentBuilder getBuilder(DocumentType type, String accountId, Map<String, String> params){
        return documentBuilderFactory.getBuilder(
                type,
                accountId,
                params
        );
    }

    private List<Domain> filterDomains(List<Domain> domains){
        domains = domains.stream()
                .filter(domain -> domain.getParentDomainId() == null
                        || domain.getParentDomainId().isEmpty()
                        || domain.getPersonId() == null
                        || domain.getPersonId().isEmpty()
                )
                .collect(Collectors.toList());

        return domains;
    }

    private Map<String, byte[]> buildFileMap(DocumentOrder documentOrder, List<Domain> domains){

        Map<String, byte[]> fileMap = new HashMap<>();

        DEFAULT_DOCUMENT_TYPES.forEach(documentType ->{
            try {
                byte[] file = buildDocument(
                        documentType,
                        documentOrder.getPersonalAccountId(),
                        documentOrder.getParams()
                );
                fileMap.put(getFileName(documentType), file);

            } catch (Exception e) {
                documentOrder.setChecked(false);
                documentOrder.getErrors().put(documentType.name(), e.getMessage());
            }
        });

        if (documentOrder.getErrors().isEmpty()) {
            documentOrder.setChecked(true);
        }

        domains = filterDomains(domains);

        if (domains != null && !domains.isEmpty()) {
            for (Domain domain : domains){
                try {
                    byte[] file = buildDocument(
                            REGISTRANT_DOMAIN_CERTIFICATE,
                            documentOrder.getPersonalAccountId(),
                            documentOrder.getParams()
                    );

                    fileMap.put(domain.getName() + getFileName(REGISTRANT_DOMAIN_CERTIFICATE), file);
                    documentOrder.getDomainIds().add(domain.getId());

                } catch (Exception e){
                    logger.debug("???????????? ?????? ?????????????????? ?????????????????????? ?????? ???????????? " + domain.getName());
                }
            }
        }

        return fileMap;
    }

    private byte[] getZipFileByteArray(Map<String, byte[]> fileMap, String filePrefixName) throws IOException{
        File zipFile = new File(temporaryFilePath + filePrefixName + ".zip");
        zipFile.delete();
        zipFile.createNewFile();
        saveFilesToZip(zipFile, fileMap);
        return Files.readAllBytes(Paths.get(zipFile.getAbsolutePath()));
    }

    private void sendDocumentOrderToSecretary(
            PersonalAccount account,
            DocumentOrder documentOrder,
            List<Domain> domains,
            Map<String, byte[]> fileMap,
            String operatorName
    ){
        byte[] zipFileByteArray;
        try {
            zipFileByteArray = getZipFileByteArray(fileMap, account.getAccountId());
        } catch (IOException e) {
            logger.error("???? ?????????????? ?????????????? ?????????? ?? ?????????????????????? ?????????? ?????????????????? ??????????????????. ErrorMessage: " + e.getMessage());
            history.save(account,"???? ?????????????? ?????????????? ?????????? ?? ?????????????????????? ?????????? ?????????????????? ??????????????????. ErrorMessage: " + e.getMessage(), operatorName);
            e.printStackTrace();
            throw new InternalApiException("???????????????? ???????????? ?????? ???????????????? ???????????? ?? ??????????????????????, ???????????????????? ??????????");
        }

        //???????????????? ???????????? ??????????????????

        String documentListInHtml = String.join("<br/>", documentOrder.getDocumentTypes()
                .stream().map(e -> e.getNameForHuman()).collect(Collectors.toList()));

        String domainListInHtml = "???? ????????????????";
        if (!documentOrder.getDomainIds().isEmpty()) {
            List<String> domainNameList = new ArrayList<>();
            domains.stream().forEach(domain -> {
                if (documentOrder.getDomainIds().contains(domain.getId())) {
                    domainNameList.add(domain.getName());
                }
            });
            domainListInHtml = String.join("<br/>", domainNameList);
        }

        String attachementBody = Base64.getMimeEncoder().encodeToString(zipFileByteArray);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("address", documentOrder.getPostalAddress());
        parameters.put("document_list", documentListInHtml);
        parameters.put("domain_list", domainListInHtml);

        Map<String, String> attachment = new HashMap<>();
        attachment.put("body", attachementBody);
        attachment.put("mime_type", "application/zip");
        attachment.put("filename", account.getAccountId() + "_documentOrder.zip");

        accountNotificationHelper.sendMailWithAttachement(
                account,
                documentOrderEmail,
                "HmsSendDocumentOrder",
                10,
                parameters,
                attachment
        );
    }

    private void validatePostalAddressInDocumentOrder(DocumentOrder documentOrder){
        if (documentOrder.getPostalAddress() == null || documentOrder.getPostalAddress().isEmpty()){
            throw new ParameterValidationException("???????????????????? ?????????????? ???????????????? ?????????? ?????? ???????????? ????????????????????");
        }
    }
}
