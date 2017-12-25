package ru.majordomo.hms.personmgr.service.Document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.DocumentType;
import ru.majordomo.hms.personmgr.common.FileType;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.service.Rpc.MajordomoRpcClient;
import ru.majordomo.hms.personmgr.service.converter.ConverterFactory;

import java.util.Map;

@Service
public class DocumentBuilderFactory {

    private final MajordomoRpcClient majordomoRpcClient;
    private final AccountOwnerManager accountOwnerManager;
    private final PersonalAccountManager personalAccountManager;
    private final ConverterFactory converterFactory;

    @Autowired
    public DocumentBuilderFactory(
            MajordomoRpcClient majordomoRpcClient,
            AccountOwnerManager accountOwnerManager,
            PersonalAccountManager personalAccountManager,
            ConverterFactory converterFactory
    ){
        this.majordomoRpcClient = majordomoRpcClient;
        this.accountOwnerManager = accountOwnerManager;
        this.personalAccountManager = personalAccountManager;
        this.converterFactory = converterFactory;
    }

    public DocumentBuilder getBuilder(DocumentType type, String personalAccountId, Map<String, String> params){
        DocumentBuilder documentBuilder = null;
        switch (type){
            case VIRTUAL_HOSTING_OFERTA:
                throw new ParameterValidationException("Нельзя заказать оферту");

            case VIRTUAL_HOSTING_CONTRACT:
                throw new ParameterValidationException("Нельзя заказать договор");

            case VIRTUAL_HOSTING_BUDGET_CONTRACT:
                documentBuilder = new BudgetContractBuilder(
                        personalAccountId,
                        accountOwnerManager,
                        majordomoRpcClient,
                        personalAccountManager,
                        converterFactory.getConverter(FileType.HTML, FileType.PDF),
                        params
                );

                break;
            default:
                throw new ParameterValidationException("Неизвестный тип документа");
        }
        return documentBuilder;
    }
}
