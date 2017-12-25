package ru.majordomo.hms.personmgr.service.Document;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.majordomo.hms.personmgr.common.FileUtils;
import ru.majordomo.hms.personmgr.dto.rpc.Contract;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.Rpc.MajordomoRpcClient;
import ru.majordomo.hms.personmgr.service.converter.Converter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetContractBuilder implements DocumentBuilder {

    private String personalAccountId;
    private final AccountOwnerManager accountOwnerManager;
    private final MajordomoRpcClient majordomoRpcClient;
    private final PersonalAccountManager personalAccountManager;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Converter converter;
    private final Map<String, String> params;

    private String template;
    private File htmlFile;
    private File pdfFile;
    private String temporaryFilePath = System.getProperty("java.io.tmpdir") + "/";

    public BudgetContractBuilder(
            String personalAccountId,
            AccountOwnerManager accountOwnerManager,
            MajordomoRpcClient majordomoRpcClient,
            PersonalAccountManager personalAccountManager,
            Converter converter,
            Map<String, String> params
    ){
        this.personalAccountId = personalAccountId;
        this.accountOwnerManager = accountOwnerManager;
        this.majordomoRpcClient = majordomoRpcClient;
        this.personalAccountManager = personalAccountManager;
        this.converter = converter;
        this.params = params;
    }

    @Override
    public void checkAuthority(){
        AccountOwner owner = accountOwnerManager.findOneByPersonalAccountId(this.personalAccountId);
        if (!owner.getType().equals(AccountOwner.Type.BUDGET_COMPANY)) {
            throw new ParameterValidationException("Вы не можете заказать такой документ");
        }
    }

    @Override
    public void buildTemplate() {
        Contract contract = majordomoRpcClient.getActiveContractVirtualHosting();

        try {
            String header = getHeader();
            String body = contract.getBody();
            String footer = contract.getFooter();
            List<Integer> noFooterPages = contract.getNoFooterPages();

            if (body == null || footer == null || noFooterPages == null) {
                throw new ParameterValidationException("Один из элементов (body||footer||noFooterPages) равен null");
            }
            this.template = createTemplate(header, body, footer, noFooterPages);
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            throw new ParameterValidationException("Не удалось сгенерировать договор.");
        }
    }

    @Override
    public void replaseFields() {
        AccountOwner owner = accountOwnerManager.findOneByPersonalAccountId(this.personalAccountId);

        String filledDocument = this.template;
        Map<String, String> replaceMap = buildReplaceParameters(owner, this.params);

        for (Map.Entry<String, String> entry : replaceMap.entrySet()) {
            filledDocument = filledDocument.replaceAll(entry.getKey(), entry.getValue());
        }

        String htmlFileName = owner.getPersonalAccountId() + ".html";

        this.htmlFile = new File(this.temporaryFilePath + htmlFileName);
        FileUtils.saveFile(htmlFile, filledDocument);
    }

    @Override
    public void convert() {
        this.pdfFile = converter.convert(this.htmlFile);
    }

    @Override
    public File getDocument() {
        return this.pdfFile;
    }

    private String createTemplate(String header, String body, String footer, List<Integer> noFooterPages) {
        //TODO генерация шаблона
        return header + body + footer;
    }

    private String getHeader() throws IOException {
        InputStream inputStream = this.getClass()
                .getResourceAsStream("/contract/budget_contract_header.html");

        return CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));
    }

    private Map<String, String> buildReplaceParameters(AccountOwner owner, Map<String, String> params){

        PersonalAccount account = personalAccountManager.findOne(this.personalAccountId);

        Map<String, String> replaceMap = new HashMap<>();

        /*Заказчик: #URNAME#                    обязательно
        Юридический адрес: #URADR#              обязательно
        Почтовый адрес: #PADR#                  обязательно
        ИНН: #INN# КПП: #KPP#                   обязательно
        ОГРН: #OGRN#                            обязательно
        ОКПО: #OKPO#                            необязательно
        ОКВЭД: #OKVED#                          необязательно
        Телефон: #TEL#                          обязательно
        Факс: #FAX#                             необязательно
        Наименование банка: #BANKNAME#          необязательно
        Расчетный счет: #RASSCHET#              необязательно
        БИК: #BIK#                              необязательно
        Корреспондентский счет: #KORSCHET#      необязательно*/
        replaceMap.put("#NUMER#", account.getAccountId());
        replaceMap.put("#DEN#", String.valueOf(LocalDate.now().getDayOfMonth()));
        replaceMap.put("#MES#", String.valueOf(LocalDate.now().getMonthValue())); //месяц надо в виде слова
        replaceMap.put("#YAR#", String.valueOf(LocalDate.now().getYear()));
        replaceMap.put("#URNAME#", owner.getName());
        replaceMap.put("#URFIO#", "ИМЯ ВЛАДЕЛЬЦА В РОДИТЕЛЬНОМ ПАДЕЖЕ"); //названия кого-то там, кого передал юзер
        replaceMap.put("#USTAVA#", "УСТАВ ИЛИ ЧЕ ТАМ У НИХ В РОДИТЕЛЬНОМ ПАДЕЖЕ"); // на основании устава
        replaceMap.put("#URADR#", owner.getContactInfo().getPostalAddress());
        replaceMap.put("#PADR#", owner.getPersonalInfo().getAddress());
        replaceMap.put("#TEL#", owner.getPersonalInfo().getNumber() != null && !owner.getPersonalInfo().getNumber().equals("") ? owner.getPersonalInfo().getNumber() : "НОМЕР ТЕЛЕФОНА");
        replaceMap.put("#FAX#", owner.getPersonalInfo().getNumber() != null && !owner.getPersonalInfo().getNumber().equals("") ? owner.getPersonalInfo().getNumber() : "НОМЕР ТЕЛЕФОНА");
        replaceMap.put("#BANKNAME#", owner.getContactInfo().getBankName());
        replaceMap.put("#RASSCHET#", owner.getContactInfo().getBankAccount());
        replaceMap.put("#KORSCHET#", owner.getContactInfo().getCorrespondentAccount());
        replaceMap.put("#BIK#", owner.getContactInfo().getBik());
        replaceMap.put("#INN#", owner.getPersonalInfo().getInn());
        replaceMap.put("#KPP#", owner.getPersonalInfo().getKpp());
        replaceMap.put("#OKPO#", owner.getPersonalInfo().getOkpo() != null ? owner.getPersonalInfo().getOkpo() : "ОКПО");
        replaceMap.put("#OKVED#", owner.getPersonalInfo().getOkvedCodes() != null ? owner.getPersonalInfo().getOkvedCodes() : "ОКВЕД");
        replaceMap.put("#OGRN#", owner.getPersonalInfo().getOgrn());
        replaceMap.put("#PAGE#", "\n<pdf:pagenumber>\n"); //надо определять page при подстановке футера

        //это надо делать отдельно от подстановки юзерских параметров
        replaceMap.put("#FONT_PATH#", "\"https://raw.githubusercontent.com/JotJunior/PHP-Boleto-ZF2/master/public/assets/fonts/arial.ttf\"");
        return replaceMap;
    }
}
