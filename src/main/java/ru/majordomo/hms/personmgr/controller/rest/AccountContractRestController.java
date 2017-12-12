package ru.majordomo.hms.personmgr.controller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.dto.rpc.Contract;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.PdfService;
import ru.majordomo.hms.personmgr.service.Rpc.MajordomoRpcClient;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/{accountId}/contract")
public class AccountContractRestController {

    private MajordomoRpcClient majordomoRpcClient;
    private PdfService pdfService;
    private AccountOwnerManager accountOwnerManager;
    private PersonalAccountManager personalAccountManager;
    private Logger logger = LoggerFactory.getLogger(getClass());

    private enum  ContractType {
        VIRTUAL_HOSTING_OFERTA,
        VIRTUAL_HOSTING_CONTRACT,
        VIRTUAL_HOSTING_BUDGET_CONTRACT
    }

    @Autowired
    public AccountContractRestController(
            MajordomoRpcClient majordomoRpcClient,
            PdfService pdfService,
            AccountOwnerManager accountOwnerManager,
            PersonalAccountManager personalAccountManager
    ){
        this.majordomoRpcClient = majordomoRpcClient;
        this.pdfService = pdfService;
        this.accountOwnerManager = accountOwnerManager;
        this.personalAccountManager = personalAccountManager;
    }

    @GetMapping("/virtual-hosting/{contractType}")
    @ResponseBody
    public Object getContract(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "contractType") ContractType contractType,
            @RequestParam Map<String, String> params
    ) {
        AccountOwner owner = accountOwnerManager.findOneByPersonalAccountId("100000");

        Contract contract;
        switch (contractType) {
            case VIRTUAL_HOSTING_OFERTA:
                throw new ParameterValidationException("Нельзя заказать оферту");

            case VIRTUAL_HOSTING_CONTRACT:
                throw new ParameterValidationException("Нельзя заказать договор");

            case VIRTUAL_HOSTING_BUDGET_CONTRACT:
                contract = majordomoRpcClient.getActiveContractVirtualHosting();
                break;
            default:
                throw new ParameterValidationException("Неизвестный тип договора");
        }

        String preparedContractInHtml = createContract(contract, owner, params);

        return pdfService.convertHtmlToPdf(preparedContractInHtml);
    }

    private String createContract(Contract contract, AccountOwner owner, Map<String, String> params) {

        String template = createTemplate(contract.getBody(), contract.getFooter(), contract.getNoFooterPages());

        return replaceFields(template, owner, params);
    }

    private String createTemplate(String body, String footer, List<Integer> noFooterPages) {
                //need add footer to every page exclude noFooterPages
                return getHeader() + body + footer;
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

    private String getHeader(){
        return  "<html><head>\n" +
                "<style>\n" +
                "    @font-face { font-family: sans-serif; src: /home/val/arial.ttf; }\n" +
                "    @font-face { font-family: serif; src: /home/val/arial.ttf; }\n" +
                "    @font-face { font-family: Helvetica; src: /home/val/arial.ttf; }\n" +
                "\n" +
                "    @page {\n" +
                "      \tmargin: 1cm;\n" +
                "\t\tmargin-bottom: 2.5cm;\n" +
                "\n" +
                "\t\t@frame footer {\n" +
                "\t\t    -pdf-frame-content: footerContent;\n" +
                "\t\t    bottom: 1cm;\n" +
                "\t\t    margin-left: 1cm;\n" +
                "\t\t    margin-right: 1cm;\n" +
                "\t\t    height: 1cm;\n" +
                "\t\t}\n" +
                "\t}\n" +
                "\n" +
                "    @page withfooter {\n" +
                "      \tmargin: 1cm;\n" +
                "\t\tmargin-bottom: 2.5cm;\n" +
                "\n" +
                "\t\t@frame footer {\n" +
                "\t\t    -pdf-frame-content: footerContent;\n" +
                "\t\t    bottom: 1cm;\n" +
                "\t\t    margin-left: 1cm;\n" +
                "\t\t    margin-right: 1cm;\n" +
                "\t\t    height: 1cm;\n" +
                "\t\t}\n" +
                "\t}\n" +
                "\n" +
                "    @page withoutfooter {\n" +
                "      \tmargin: 1cm;\n" +
                "\t\tmargin-bottom: 2.5cm;\n" +
                "\t}\n" +
                "\n" +
                "    html {\n" +
                "        font-family: Helvetica;\n" +
                "        font-size: 11pt;\n" +
                "    }\n" +
                "\n" +
                "    body, div {\n" +
                "        font: normal 16px/16px Helvetica, sans-serif;\n" +
                "    }\n" +
                "\n" +
                "    p {\n" +
                "        margin: 0px;\n" +
                "    }\n" +
                "\n" +
                "</style>\n" +
                "\n" +
                "<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"/>\n" +
                "</head>\n" +
                "<body>";
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
