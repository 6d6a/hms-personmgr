package ru.majordomo.hms.personmgr.controller.rest;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.dto.rpc.Contract;
import ru.majordomo.hms.personmgr.dto.rpc.DocumentType;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.Rpc.MajordomoRpcClient;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.io.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/{accountId}/document")
public class AccountContractRestController {

    private final MajordomoRpcClient majordomoRpcClient;
    private final AccountOwnerManager accountOwnerManager;
    private final PersonalAccountManager personalAccountManager;

    @Autowired
    public AccountContractRestController(
            MajordomoRpcClient majordomoRpcClient,
            AccountOwnerManager accountOwnerManager,
            PersonalAccountManager personalAccountManager
    ){
        this.majordomoRpcClient = majordomoRpcClient;
        this.accountOwnerManager = accountOwnerManager;
        this.personalAccountManager = personalAccountManager;
    }

    @GetMapping("/{documentType}")
    @ResponseBody
    public FileSystemResource getContract(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "documentType") DocumentType documentType,
            @RequestParam Map<String, String> params
    ) {
        AccountOwner owner = accountOwnerManager.findOneByPersonalAccountId(accountId);

        AccountOwner.Type ownerType = owner.getType();

        Contract contract;
        switch (documentType) {
            case VIRTUAL_HOSTING_OFERTA:
                throw new ParameterValidationException("Нельзя заказать оферту");

            case VIRTUAL_HOSTING_CONTRACT:
                throw new ParameterValidationException("Нельзя заказать договор");

            case VIRTUAL_HOSTING_BUDGET_CONTRACT:
                if (!ownerType.equals(AccountOwner.Type.BUDGET_COMPANY)) {
                    throw new ParameterValidationException("Вы не можете заказать такой документ");
                }
                contract = majordomoRpcClient.getActiveContractVirtualHosting();

                break;
            default:
                throw new ParameterValidationException("Неизвестный тип договора");
        }

        String template;

        try {
            String header = getHeader();
            String body = contract.getBody();
            String footer = contract.getFooter();
            List<Integer> noFooterPages = contract.getNoFooterPages();

            if (body == null || footer == null || noFooterPages == null) {
                throw new ParameterValidationException("djkajsdgkjasg");
            }
            template = createTemplate(header, body, footer, noFooterPages);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ParameterValidationException("Не удалось сгенерировать договор.");
        }

        String document = replaceFields(template, owner, new HashMap<>());

//        return document;

        saveFile(accountId + "contract.html", document);

        File file = new File(accountId + "contract.html");
        return new FileSystemResource(file);
    }

    private void saveFile(String fileName, String content){
        BufferedWriter bw = null;
        FileWriter fw = null;
        try {
            fw = new FileWriter(fileName);
            bw = new BufferedWriter(fw);
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null)
                    bw.close();
                if (fw != null)
                    fw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private String createTemplate(String header, String body, String footer, List<Integer> noFooterPages) {
                //need add footer to every page exclude noFooterPages
                return header + body + footer;
    }

    private String replaceFields(String template, AccountOwner owner, Map<String, String> params){
        Map<String, String> replaceMap = buildReplaceParameters(owner, params);

        for (Map.Entry<String, String> entry : replaceMap.entrySet()) {
            try {
                template = template.replaceAll(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                throw new ParameterValidationException("Не удалось создать договор");
            }
        }
        return template;
    }

    private String getHeader() throws IOException {
        InputStream inputStream = this.getClass()
                .getResourceAsStream("/contract/budget_contract_header.html");

       return CharStreams.toString(new InputStreamReader(
                    inputStream, Charsets.UTF_8));
    }

    private Map<String, String> buildReplaceParameters(AccountOwner owner, Map<String, String> params){

        PersonalAccount account = personalAccountManager.findOne(owner.getPersonalAccountId());

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

        return replaceMap;
    }

}
