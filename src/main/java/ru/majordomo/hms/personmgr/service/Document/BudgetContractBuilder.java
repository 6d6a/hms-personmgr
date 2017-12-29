package ru.majordomo.hms.personmgr.service.Document;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.dto.rpc.Contract;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.AccountDocument;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.Rpc.MajordomoRpcClient;

import java.io.*;
import java.time.LocalDate;
import java.util.*;

import static ru.majordomo.hms.personmgr.common.Utils.saveByteArrayToFile;

public class BudgetContractBuilder implements DocumentBuilder {

    private PersonalAccount account;
    private final MajordomoRpcClient majordomoRpcClient;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<String, String> params;

    private String html;
    private AccountDocument document = new AccountDocument();
    private File pdfFile;
    private String temporaryFilePath = System.getProperty("java.io.tmpdir") + "/";
    private AccountOwner owner;

    public BudgetContractBuilder(
            String personalAccountId,
            AccountOwnerManager accountOwnerManager,
            MajordomoRpcClient majordomoRpcClient,
            PersonalAccountManager personalAccountManager,
            Map<String, String> params
    ){
        this.majordomoRpcClient = majordomoRpcClient;
        this.params = params;
        this.owner = accountOwnerManager.findOneByPersonalAccountId(personalAccountId);
        this.account = personalAccountManager.findOne(personalAccountId);
    }

    public File buildFromDocument(AccountDocument document){
        buildTemplateFromDocument(document);
        replaceFieldsWithReplaceMap(document.getParameters());

        return null;
    }

    @Override
    public void checkAuthority(){
        if (this.owner == null || !this.owner.getType().equals(AccountOwner.Type.BUDGET_COMPANY)) {
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
        if (owner.getContactInfo().getPhoneNumbers() != null
                && !owner.getContactInfo().getPhoneNumbers().contains(params.get("phone"))
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
        buildTemplateFromContract(contract);
    }

    public void buildTemplateFromDocument(AccountDocument document){
        Contract contract = majordomoRpcClient.getContractById(document.getDocumentTemplateId());
        buildTemplateFromContract(contract);
    }

    private void buildTemplateFromContract(Contract contract){
        this.document.setDocumentTemplateId(contract.getContractId().toString());
        try {
            String header = getHeader();
            String body = contract.getBody();
            String footer = contract.getFooter();
            List<Integer> noFooterPages = contract.getNoFooterPages();

            if (body == null || footer == null || noFooterPages == null) {
                throw new ParameterValidationException("Один из элементов (body||footer||noFooterPages) равен null");
            }
            this.html = createTemplate(header, body, footer, noFooterPages);
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            throw new ParameterValidationException("Не удалось сгенерировать договор.");
        }
    }

    @Override
    public void replaceFields() {
        Map<String, String> replaceMap = buildReplaceParameters(this.params, this.owner, this.account);

        replaceFieldsWithReplaceMap(replaceMap);
    }

    private void replaceFieldsWithReplaceMap(Map<String, String> replaceMap){
        for (Map.Entry<String, String> entry : replaceMap.entrySet()) {
            this.html = this.html.replaceAll(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void convert() {
        String pdfFilePath = this.temporaryFilePath + "budget_contract_" + this.account.getAccountId() + ".pdf";
        this.pdfFile = new File(pdfFilePath);

        try {
            Object response = majordomoRpcClient.convertHtmlToPdf(Arrays.asList(this.html));
            byte[] decoded = Base64.getDecoder().decode(((Map<String, Object>) response).get("pdf_file").toString());
            saveByteArrayToFile(decoded, this.pdfFile);
        } catch (Exception e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    public File getDocument() {
        return this.pdfFile;
    }

    private String createTemplate(String header, String body, String footer, List<Integer> noFooterPages) {
        String pdfFooter = "<div id=\"footerContent\">" + footer + "</div>";
        String htmlFooter = "\n</body>\n</html>";
        String pageBreakPattern = "<div style=\"page-break-after: always;?\">(\\s*<span style=\"display: none;\">&nbsp;</span></div>)?|<div class=\"pagebreak\"><!-- pagebreak --></div>";

        StringBuilder templateBuilder = new StringBuilder();
        templateBuilder.append(header);

        List<String> pages = Arrays.asList(body.split(pageBreakPattern));
        Iterator pageIterator = pages.iterator();

        int pageNumber = 0;
        while (pageIterator.hasNext()){
            templateBuilder.append(pageIterator.next());
            if (noFooterPages.contains(pageNumber + 2)){
                templateBuilder.append("<pdf:nexttemplate name=\"withoutfooter\"/>");
            } else {
                templateBuilder.append("<pdf:nexttemplate name=\"withfooter\"/>");
            }
            templateBuilder.append("<pdf:nextpage/>");
            pageNumber++;
        }

        templateBuilder.append(pdfFooter).append(htmlFooter);

        return templateBuilder.toString();
    }

    private String getHeader() throws IOException {
        InputStream inputStream = this.getClass()
                .getResourceAsStream("/contract/budget_contract_header.html");

        return CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));
    }

    private Map<String, String> buildReplaceParameters(Map<String, String> params, AccountOwner owner, PersonalAccount account){
        Map<String, String> replaceMap = new HashMap<>();

        replaceMap.put("#TEL#", params.get("phone"));
        replaceMap.put("#FAX#", params.get("phone"));
        replaceMap.put("#URNAME#", owner.getName());
        replaceMap.put("#URADR#", owner.getPersonalInfo().getAddress());
        replaceMap.put("#PADR#", owner.getContactInfo().getPostalAddress());
        replaceMap.put("#KPP#", owner.getPersonalInfo().getKpp());
        replaceMap.put("#OGRN#", owner.getPersonalInfo().getOgrn());
        replaceMap.put("#INN#", owner.getPersonalInfo().getInn());


        replaceMap.put("#NUMER#", account.getAccountId());
        replaceMap.put("#DEN#", String.valueOf(LocalDate.now().getDayOfMonth()));
        replaceMap.put("#MES#", Utils.getMonthName(LocalDate.now().getMonthValue())); //месяц надо в виде слова
        replaceMap.put("#YAR#", String.valueOf(LocalDate.now().getYear()));

        replaceMap.put("#URFIO#", params.get("urfio")); //названия кого-то там, кого передал юзер
        replaceMap.put("#USTAVA#", params.get("ustava")); // на основании устава

        /*ОКПО: #OKPO#                        необязательно
        ОКВЭД: #OKVED#                      необязательно
        Факс: #FAX#                         необязательно
        Наименование банка: #BANKNAME#      необязательно
        Расчетный счет: #RASSCHET#          необязательно
        БИК: #BIK#                          необязательно
        Корреспондентский счет: #KORSCHET#  необязательно*/
        replaceMap.put("#BANKNAME#", owner.getContactInfo().getBankName() != null ? owner.getContactInfo().getBankName() : "");
        replaceMap.put("#RASSCHET#", owner.getContactInfo().getBankAccount() != null ? owner.getContactInfo().getBankAccount() : "");
        replaceMap.put("#KORSCHET#", owner.getContactInfo().getCorrespondentAccount() != null ? owner.getContactInfo().getCorrespondentAccount() : "");
        replaceMap.put("#BIK#", owner.getContactInfo().getBik() != null ? owner.getContactInfo().getBik() : "");
        replaceMap.put("#OKPO#", owner.getPersonalInfo().getOkpo() != null ? owner.getPersonalInfo().getOkpo() : "");
        replaceMap.put("#OKVED#", owner.getPersonalInfo().getOkvedCodes() != null ? owner.getPersonalInfo().getOkvedCodes() : "");

        //это надо делать отдельно от подстановки юзерских параметров
        replaceMap.put("#PAGE#", "\n<pdf:pagenumber>\n");
//        replaceMap.put("#FONT_PATH#", "\"/arial.ttf\"");
        replaceMap.put("#FONT_PATH#", "\"/home/berezka/git/rpc/arial.ttf\"");
        return replaceMap;
    }
}
