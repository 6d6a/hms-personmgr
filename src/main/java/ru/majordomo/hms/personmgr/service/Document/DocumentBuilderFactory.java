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
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@ParametersAreNonnullByDefault
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
     * @param personalAccountId null если нужен предварительный просмотр документа
     * @param params зависящие от типа параметры которы будут подставлены в документ. Например:
     *               phone - номер телефона и факса, urfio - имя и фамилия заключившего договор,
     *               ustava - на основании чего заключен договор,
     *               day, month, year - дата, если не указаны даты создания аккаунта
     *               для buildPreview может быть пустым
     */
    public DocumentBuilder getBuilder(DocumentType type, @Nullable String personalAccountId, @Nullable Map<String, String> params) throws ParameterValidationException {
        if (params == null) {
            params = Collections.emptyMap();
        }
        DocumentBuilder documentBuilder = null;
        boolean withoutStamp = Boolean.parseBoolean(params.getOrDefault("withoutStamp", "false"));

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
                        params,
                        withoutStamp
                );

                break;
            case VIRTUAL_HOSTING_BUDGET_SUPPLEMENTARY_AGREEMENT:
                documentBuilder = new SupplementaryAgreementBilder(
                        personalAccountId,
                        accountOwnerManager
                );

                break;
            case VIRTUAL_HOSTING_COMMERCIAL_PROPOSAL:
                documentBuilder = new CommercialProposalBilder(withoutStamp);

                break;
            case VIRTUAL_HOSTING_NOTIFY_RF:
                documentBuilder = new NoticeRFBuilder(
                        accountOwnerManager,
                        personalAccountId,
                        withoutStamp,
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
