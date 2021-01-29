package ru.majordomo.hms.personmgr.service.Document;

import com.samskivert.mustache.Mustache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.DocumentType;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.repository.AccountDocumentRepository;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.service.Rpc.MajordomoRpcClient;
import ru.majordomo.hms.personmgr.service.Rpc.RegRpcClient;

import javax.annotation.Nullable;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentBuilderFactory {

    private final MajordomoRpcClient majordomoRpcClient;
    private final AccountOwnerManager accountOwnerManager;
    private final PersonalAccountManager personalAccountManager;
    private final AccountDocumentRepository accountDocumentRepository;
    private final RegRpcClient regRpcClient;
    private final RcUserFeignClient rcUserFeignClient;
    private final Mustache.Compiler mustacheCompiler;
    private final WkHtmlToPdfWebService wkhtmlToPdfService;

    /**
     * @param type
     * @param personalAccountId null если нужен предварительный просмотр документа
     * @param params
     * @return
     */
    public DocumentBuilder getBuilder(DocumentType type, @Nullable String personalAccountId, Map<String, String> params){
        DocumentBuilder documentBuilder = null;
        switch (type){
            case VIRTUAL_HOSTING_OFERTA:
                throw new ParameterValidationException("Нельзя заказать оферту");

            case VIRTUAL_HOSTING_CONTRACT:
                throw new ParameterValidationException("Нельзя заказать договор");

            case VIRTUAL_HOSTING_BUDGET_CONTRACT:
                documentBuilder = new BudgetContractWkBuilder(
                        personalAccountId,
                        accountOwnerManager,
                        majordomoRpcClient,
                        personalAccountManager,
                        accountDocumentRepository,
                        mustacheCompiler,
                        wkhtmlToPdfService,
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
                        wkhtmlToPdfService,
                        params
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
