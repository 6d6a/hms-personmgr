package ru.majordomo.hms.personmgr.service.Document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.DocumentType;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.repository.AccountDocumentRepository;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.personmgr.service.Rpc.MajordomoRpcClient;
import ru.majordomo.hms.personmgr.service.Rpc.RegRpcClient;

import java.util.Map;

@Service
public class DocumentBuilderFactory {

    private final MajordomoRpcClient majordomoRpcClient;
    private final AccountOwnerManager accountOwnerManager;
    private final PersonalAccountManager personalAccountManager;
    private final AccountDocumentRepository accountDocumentRepository;
    private final RegRpcClient regRpcClient;
    private final RcUserFeignClient rcUserFeignClient;
    private final String wkhtmltopdfUrl;

    @Autowired
    public DocumentBuilderFactory(
            MajordomoRpcClient majordomoRpcClient,
            AccountOwnerManager accountOwnerManager,
            PersonalAccountManager personalAccountManager,
            AccountDocumentRepository accountDocumentRepository,
            RegRpcClient regRpcClient,
            RcUserFeignClient rcUserFeignClient,
            @Value("${converter.wkhtmltopdf.url}") String wkhtmltopdfUrl
    ){
        this.majordomoRpcClient = majordomoRpcClient;
        this.accountOwnerManager = accountOwnerManager;
        this.personalAccountManager = personalAccountManager;
        this.accountDocumentRepository = accountDocumentRepository;
        this.regRpcClient = regRpcClient;
        this.rcUserFeignClient = rcUserFeignClient;
        this.wkhtmltopdfUrl = wkhtmltopdfUrl;
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
                        accountDocumentRepository,
                        params
                );

                break;
            case VIRTUAL_HOSTING_BUDGET_SUPPLEMENTARY_AGREEMENT:
                documentBuilder = new SupplementaryAgreementBilder(
                        personalAccountId,
                        accountOwnerManager
                );

                break;
            case VIRTUAL_HOSTING_COMMERCIAL_PROPOSAL:
                documentBuilder = new CommercialProposalBilder(
                        Boolean.valueOf(params.getOrDefault("withoutStamp", "false"))
                );

                break;
            case VIRTUAL_HOSTING_NOTIFY_RF:
                documentBuilder = new NoticeRFBuilder(
                        accountOwnerManager,
                        personalAccountId,
                        Boolean.valueOf(params.getOrDefault("withoutStamp", "false")),
                        wkhtmltopdfUrl
                );

                break;
            case REGISTRANT_DOMAIN_CERTIFICATE:
                documentBuilder = new RegistrantDomainCertificateBuilder(
                        personalAccountId,
                        regRpcClient,
                        rcUserFeignClient,
                        params
                );

                break;
            default:
                throw new ParameterValidationException("Неизвестный тип документа");
        }
        return documentBuilder;
    }
}
