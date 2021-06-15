package ru.majordomo.hms.personmgr.service.Document;

import com.google.common.base.Strings;
import com.samskivert.mustache.Mustache;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;
import ru.majordomo.hms.personmgr.common.DocumentType;
import ru.majordomo.hms.personmgr.common.FileUtils;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.dto.request.DocumentPreviewRequest;
import ru.majordomo.hms.personmgr.dto.rpc.Contract;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.AccountDocument;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.AccountDocumentRepository;
import ru.majordomo.hms.personmgr.service.Rpc.MajordomoRpcClient;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
public class BudgetContractWkBuilder extends DocumentBuilderImpl {

    private final static String PAGE_BREAK_PATTERN = "<div style=\"page-break-after: always;?\">(\\s*<span style=\"display: none;\">&nbsp;</span></div>)?|<div class=\"pagebreak\"><!-- pagebreak --></div>";
    private final static String PAGE_NUMBER_PDF_TAG = "<span class='pageNumber'></span>";

    private final static String DOCUMENT_RESOURCE_PATH = "/contract/budget_contract_header.html.mustache";

    private final static String FOOTER_RESOURCE_PATH = "/contract/footer.html.mustache";

    private final MajordomoRpcClient majordomoRpcClient;
    private final AccountDocumentRepository accountDocumentRepository;
    private final Mustache.Compiler mustacheCompiler;
    private final WkHtmlToPdfWebService wkhtmlToPdfService;


    private AccountDocument document = new AccountDocument();
    private Map<String, String> replaceParameters;
    @Nullable
    private String templateId;

    private final Map<String, String> params;
    @Nullable
    private AccountOwner owner;
    @Nullable
    private PersonalAccount account;
    private String bodyHtml;
    private String footerHtml;

    /**
     * @param personalAccountId null если не нужен, только buildPreview() или buildFromAccountDocument
     * @param params phone - номер телефона и факса, urfio - имя и фамилия заключившего договор,
     *               ustava - на основании чего заключен договор,
     *               day, month, year - дата, если не указаны даты создания аккаунта
     *               для buildPreview может быть пустым
     */
    public BudgetContractWkBuilder(
            @Nullable
            String personalAccountId,
            AccountOwnerManager accountOwnerManager,
            MajordomoRpcClient majordomoRpcClient,
            PersonalAccountManager personalAccountManager,
            AccountDocumentRepository accountDocumentRepository,
            Mustache.Compiler mustacheCompiler,
            WkHtmlToPdfWebService wkhtmlToPdfService,
            @Nullable
            Map<String, String> params,
            boolean withoutStamp
    ){
        this.majordomoRpcClient = majordomoRpcClient;
        this.params = params == null ? Collections.emptyMap() : params;
        if (personalAccountId != null) {
            this.owner = accountOwnerManager.findOneByPersonalAccountId(personalAccountId);
            this.account = personalAccountManager.findOne(personalAccountId);
        }
        this.accountDocumentRepository = accountDocumentRepository;
        this.mustacheCompiler = mustacheCompiler.escapeHTML(false);
        this.wkhtmlToPdfService = wkhtmlToPdfService;
        setWithoutStamp(withoutStamp);
        logger.debug("majordomoRpcClient.serverUrl: {}", majordomoRpcClient.getServerURL());
    }

    @Override
    public byte[] buildPreview(DocumentPreviewRequest documentPreviewRequest) {
        setWithoutStamp(documentPreviewRequest.isWithoutStamp());
        Contract pdfDocumentContract = new Contract();
        pdfDocumentContract.setBody(documentPreviewRequest.getBodyHtml(), true);
        pdfDocumentContract.setFooter(documentPreviewRequest.getFooterHtml(), true);
        pdfDocumentContract.setNoFooterPages(documentPreviewRequest.getNoFooterPages());
        buildTemplateFromContract(pdfDocumentContract);
        convert();
        return getFile();
    }

    @Override
    public byte[] buildFromAccountDocument(AccountDocument document){
        buildTemplateFromDocument(document);
        replaceFieldsWithReplaceMap(document.getParameters());
        convert();
        return getFile();
    }

    @Override
    public void checkAuthority(){
        if (owner == null || !owner.getType().equals(AccountOwner.Type.BUDGET_COMPANY)) {
            throw new ParameterValidationException("Вы не можете заказать такой документ");
        }
    }

    @Override
    public void checkRequireParams(){
        Assert.notNull(owner, "Должен быть задан владелец аккаунта");
        /*
        Заказчик: #URNAME#                  oбязательно
        Юридический адрес: #URADR#          обязательно
        Почтовый адрес: #PADR#              обязательно
        ИНН: #INN# КПП: #KPP#               обязательно
        ОГРН: #OGRN#                        обязательно
        Телефон: #TEL#                      обязательно
        */
        validateStringParam(
                owner.getName(),
                "Необходимо указать название организации"
        );
        validateStringParam(
                owner.getContactInfo().getPostalAddress(),
                "Необходимо указать почтовый адрес");
        validateStringParam(
                owner.getPersonalInfo().getAddress(),
                "Необходимо указать юридический адрес"
        );
        validateStringParam(
                owner.getPersonalInfo().getInn()
                ,"Необходимо указать ИНН"
        );
        validateStringParam(
                owner.getPersonalInfo().getOgrn(),
                "Необходимо указать ОГРН"
        );
        validateStringParam(
                params.get("urfio"),
                "Необходимо указать ФИО заключающего договор в родительном падеже"
        );
        validateStringParam(
                params.get("ustava"),
                "Необходимо указать, на основании чего заключается договор в родительном падеже"
        );
        if (owner.getContactInfo().getPhoneNumbers() == null
                || !owner.getContactInfo().getPhoneNumbers().contains(params.get("phone"))
        ){
            throw new ParameterValidationException("Необходимо указать номер телефона");
        }
    }

    private void validateStringParam(@Nullable String param, String errorMessage){
        if (param == null || param.trim().equals("")){
            throw new ParameterValidationException(errorMessage);
        }
    }

    @Override
    public void buildTemplate() {
        Contract contract = majordomoRpcClient.getActiveBudgetContractVH();
        templateId = contract.getContractId().toString();

        buildTemplateFromContract(contract);
    }

    private void buildTemplateFromDocument(AccountDocument document){
        Contract contract = majordomoRpcClient.getContractById(document.getTemplateId());
        buildTemplateFromContract(contract);
    }

    private void buildTemplateFromContract(Contract contract) {
        try (InputStream documentStream = this.getClass().getResourceAsStream(DOCUMENT_RESOURCE_PATH);
             InputStream footerStream = this.getClass().getResourceAsStream(FOOTER_RESOURCE_PATH);) {
            Assert.notNull(documentStream, "There is not a template of a document body");
            Assert.notNull(footerStream, "There is not a template of a document footer");

            HashMap<String, Object> mustacheParams = new HashMap<>();
            mustacheParams.put("body", contract.getBody());
            mustacheParams.put("withoutStamp", isWithoutStamp());
            if (account != null && StringUtils.isNotEmpty(account.getName())) {
                mustacheParams.put("accountName", account.getName());
            }
            String arialBase64 = FileUtils.getResourceInBase64("/fonts/arial.ttf");
            mustacheParams.put("helveticaBase64", arialBase64);
            bodyHtml = mustacheCompiler.compile(new InputStreamReader(documentStream, StandardCharsets.UTF_8)).execute(mustacheParams);
            footerHtml = mustacheCompiler.compile(new InputStreamReader(footerStream, StandardCharsets.UTF_8)).execute(new HashMap<String, Object>(){{
                put("body", contract.getFooter());
                put("excludeFooter", contract.getNoFooterPages().stream().map(Object::toString).collect(Collectors.joining(",")));
                put("withoutStamp", isWithoutStamp());
            }});
            footerHtml = footerHtml.replaceAll("#PAGE#", PAGE_NUMBER_PDF_TAG);

                //todo удалить эти картинки из pm, брать их из mj-rpc, или хотя бы из конфигурационного файла
            String signBase64 = WkHtmlToPdfWebService.PREFIX_IMAGE_PNG + FileUtils.getResourceInBase64("/images/sign-ts-cropped.png");
            String stampBase64 = WkHtmlToPdfWebService.PREFIX_IMAGE_PNG + FileUtils.getResourceInBase64("/images/stamp_hosting.png");
            footerHtml = footerHtml.replaceAll("/images/pdf/sign-ts.png", signBase64);
            bodyHtml = bodyHtml.replaceAll("/images/pdf/sign-ts.png", signBase64);
            bodyHtml = bodyHtml.replaceAll("/images/pdf/stamp_hosting.png", stampBase64);

        } catch (IOException e) {
            logger.error("Cannot create html from templates", e);
            throw new InternalApiException("Не удалось создать шаблон документа");
        }
    }

    @Override
    public void buildReplaceParameters(){
        replaceParameters = buildReplaceParameters(params, owner, account);
    }

    @Override
    public void replaceFields() {
        replaceFieldsWithReplaceMap(replaceParameters);
    }

    private void replaceFieldsWithReplaceMap(Map<String, String> replaceMap){
        for (Map.Entry<String, String> entry : replaceMap.entrySet()) {
            bodyHtml = bodyHtml.replaceAll(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void convert() {
        WkHtmlToPdfOptions options = new WkHtmlToPdfOptions();
        options.setDpi(300);
        options.setZoom(0.91);
        options.setMarginBottom("25mm");
        options.setMarginTop("10mm");
        options.setMarginLeft("10mm");
        options.setMarginRight("10mm");
        options.setDisableSmartShrinking(true);
        options.setPageSize("A4");
        options.setPrintMediaType(true);
        setFile(wkhtmlToPdfService.convertHtmlToPdfFile(bodyHtml, footerHtml, options));
    }

    @Override
    public void saveAccountDocument(){
        document.unSetId();
        document.setPersonalAccountId(account.getId());
        document.setType(DocumentType.VIRTUAL_HOSTING_BUDGET_CONTRACT);
        document.setTemplateId(templateId);
        document.setParameters(replaceParameters);

        accountDocumentRepository.save(document);
    }

    private Map<String, String> buildReplaceParameters(Map<String, String> params, AccountOwner owner, PersonalAccount account){
        Map<String, String> replaceMap = new HashMap<>();

        //обязательные параметры
        replaceMap.put("#TEL#", params.get("phone"));
        replaceMap.put("#FAX#", params.get("phone"));
        replaceMap.put("#URFIO#", params.get("urfio")); //имя лица, заключающего договор
        replaceMap.put("#USTAVA#", params.get("ustava")); // на основании устава
        replaceMap.put("#URNAME#", owner.getName());
        replaceMap.put("#URADR#", owner.getPersonalInfo().getAddress());
        replaceMap.put("#PADR#", owner.getContactInfo().getPostalAddress());
        replaceMap.put("#KPP#", owner.getPersonalInfo().getKpp());
        replaceMap.put("#OGRN#", owner.getPersonalInfo().getOgrn());
        replaceMap.put("#INN#", owner.getPersonalInfo().getInn());

        replaceMap.put("#NUMER#", account.getAccountId());

        LocalDate created = account.getCreated().toLocalDate();
        replaceMap.put("#DEN#", params.getOrDefault("day", String.valueOf(created.getDayOfMonth())));
        replaceMap.put("#MES#", params.getOrDefault("month", Utils.getMonthName(created.getMonthValue())));
        replaceMap.put("#YAR#", params.getOrDefault("year", String.valueOf(created.getYear())));

        //необязательные
        replaceMap.put("#BANKNAME#", Strings.nullToEmpty(owner.getContactInfo().getBankName()));
        replaceMap.put("#RASSCHET#", Strings.nullToEmpty(owner.getContactInfo().getBankAccount()));
        replaceMap.put("#KORSCHET#", Strings.nullToEmpty(owner.getContactInfo().getCorrespondentAccount()));
        replaceMap.put("#BIK#", Strings.nullToEmpty(owner.getContactInfo().getBik()));
        replaceMap.put("#OKPO#", Strings.nullToEmpty(owner.getPersonalInfo().getOkpo()));
        replaceMap.put("#OKVED#", Strings.nullToEmpty(owner.getPersonalInfo().getOkvedCodes()));

        return replaceMap;
    }
}
