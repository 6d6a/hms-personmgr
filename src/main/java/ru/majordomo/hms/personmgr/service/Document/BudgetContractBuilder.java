package ru.majordomo.hms.personmgr.service.Document;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.majordomo.hms.personmgr.common.DocumentType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.dto.rpc.Contract;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.AccountDocument;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.AccountDocumentRepository;
import ru.majordomo.hms.personmgr.service.Rpc.MajordomoRpcClient;

import java.io.*;
import java.time.LocalDate;
import java.util.*;

import static ru.majordomo.hms.personmgr.common.Utils.saveByteArrayToFile;

public class BudgetContractBuilder implements DocumentBuilder {

    private final static String PAGE_NUMBER_PDF_TAG = "\n<pdf:pagenumber>\n";
    private final static String NEXT_PAGE_PDF_TAG = "<pdf:nextpage/>";
    private final static String NEXT_TEMPLATE_WITH_FOOTHER_TAG = "<pdf:nexttemplate name=\"withfooter\"/>";
    private final static String NEXT_TEMPLATE_WITHOUT_FOOTHER_TAG = "<pdf:nexttemplate name=\"withoutfooter\"/>";

    private final static String PAGE_BREAK_PATTERN = "<div style=\"page-break-after: always;?\">(\\s*<span style=\"display: none;\">&nbsp;</span></div>)?|<div class=\"pagebreak\"><!-- pagebreak --></div>";

//    private final static String FONT_PATH = "\"/home/git/billing/web/fonts/arial.ttf\"";
    private final static String FONT_PATH = "\"arial.ttf\"";
    private final static String HEADER_RESOURCE_PATH = "/contract/budget_contract_header.html";

    private final MajordomoRpcClient majordomoRpcClient;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AccountDocumentRepository accountDocumentRepository;


    private AccountDocument document = new AccountDocument();
    private Map<String, String> replaceParameters;
    private String templateId;

    private final Map<String, String> params;
    private String html;
    private File pdfFile;
    private String temporaryFilePath = System.getProperty("java.io.tmpdir") + "/";
    private AccountOwner owner;
    private PersonalAccount account;

    public BudgetContractBuilder(
            String personalAccountId,
            AccountOwnerManager accountOwnerManager,
            MajordomoRpcClient majordomoRpcClient,
            PersonalAccountManager personalAccountManager,
            AccountDocumentRepository accountDocumentRepository,
            Map<String, String> params
    ){
        this.majordomoRpcClient = majordomoRpcClient;
        this.params = params;
        this.owner = accountOwnerManager.findOneByPersonalAccountId(personalAccountId);
        this.account = personalAccountManager.findOne(personalAccountId);
        this.accountDocumentRepository = accountDocumentRepository;
    }

    @Override
    public File buildFromAccountDocument(AccountDocument document){
        buildTemplateFromDocument(document);
        replaceFieldsWithReplaceMap(document.getParameters());
        convert();
        return getDocument();
    }

    @Override
    public void checkAuthority(){
        if (owner == null || !owner.getType().equals(AccountOwner.Type.BUDGET_COMPANY)) {
            throw new ParameterValidationException("Вы не можете заказать такой документ");
        }
    }

    @Override
    public void checkRequireParams(){
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

    private void validateStringParam(String param, String errorMessage){
        if (param == null || param.trim().equals("")){
            throw new ParameterValidationException(errorMessage);
        }
    }

    @Override
    public void buildTemplate() {
        Contract contract = majordomoRpcClient.getActiveContractVirtualHosting();
        templateId = contract.getContractId().toString();

        buildTemplateFromContract(contract);
    }

    private void buildTemplateFromDocument(AccountDocument document){
        Contract contract = majordomoRpcClient.getContractById(document.getTemplateId());
        buildTemplateFromContract(contract);
    }

    private void buildTemplateFromContract(Contract contract){

        try {
            String header = getHeader();
            String body = contract.getBody();
            String footer = contract.getFooter();
            List<Integer> noFooterPages = contract.getNoFooterPages();

            if (body == null || footer == null || noFooterPages == null) {
                throw new ParameterValidationException("Один из элементов (body||footer||noFooterPages) равен null");
            }
            html = createTemplate(header, body, footer, noFooterPages);
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            throw new ParameterValidationException("Не удалось сгенерировать договор.");
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
            html = html.replaceAll(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void convert() {
        String pdfFilePath = temporaryFilePath + "budget_contract_" + account.getAccountId() + ".pdf";
        pdfFile = new File(pdfFilePath);

        try {
//            Object response = majordomoRpcClient.convertHtmlToPdf(Arrays.asList(html));
//            byte[] decoded = Base64.getDecoder().decode(((Map<String, Object>) response).get("pdf_file").toString());
            byte[] decoded = majordomoRpcClient.convertHtmlToPdfFile(html);
            saveByteArrayToFile(decoded, pdfFile);
        } catch (Exception e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
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

    @Override
    public File getDocument() {
        return pdfFile;
    }

    private String createTemplate(String header, String body, String footer, List<Integer> noFooterPages) {
        String pdfFooter = "<div id=\"footerContent\">" + footer + "</div>";
        String htmlFooter = "\n</body>\n</html>";

        StringBuilder templateBuilder = new StringBuilder();
        templateBuilder.append(header);

        List<String> pages = Arrays.asList(body.split(PAGE_BREAK_PATTERN));
        Iterator pageIterator = pages.iterator();

        int pageNumber = 0;
        while (pageIterator.hasNext()){
            templateBuilder.append(pageIterator.next());
            if (noFooterPages.contains(pageNumber + 2)){
                templateBuilder.append(NEXT_TEMPLATE_WITHOUT_FOOTHER_TAG);
            } else {
                templateBuilder.append(NEXT_TEMPLATE_WITH_FOOTHER_TAG);
            }
            templateBuilder.append(NEXT_PAGE_PDF_TAG);
            pageNumber++;
        }

        templateBuilder.append(pdfFooter).append(htmlFooter);
        String template = templateBuilder.toString();

        Map<String, String> templateReplaceMap = new HashMap<>();
        templateReplaceMap.put("#PAGE#", PAGE_NUMBER_PDF_TAG);
        templateReplaceMap.put("#FONT_PATH#", FONT_PATH);


        for (Map.Entry<String, String> entry : templateReplaceMap.entrySet()) {
            template = template.replaceAll(entry.getKey(), entry.getValue());
        }

        return template;
    }

    private String getHeader() throws IOException {
        InputStream inputStream = this.getClass()
                .getResourceAsStream(HEADER_RESOURCE_PATH);

        return CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));
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
        replaceMap.put("#DEN#", String.valueOf(LocalDate.now().getDayOfMonth()));
        replaceMap.put("#MES#", Utils.getMonthName(LocalDate.now().getMonthValue()));
        replaceMap.put("#YAR#", String.valueOf(LocalDate.now().getYear()));

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
