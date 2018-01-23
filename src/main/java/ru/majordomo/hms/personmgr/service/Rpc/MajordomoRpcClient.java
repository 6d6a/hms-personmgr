package ru.majordomo.hms.personmgr.service.Rpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.dto.rpc.Contract;
import ru.majordomo.hms.personmgr.dto.rpc.ContractResponse;
import ru.majordomo.hms.personmgr.dto.rpc.HtmlToPdfResponse;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Base64;

@Service
public class MajordomoRpcClient extends RpcClient {

    private static final String CONTRACT_CONTROLLER = "contracts.";
    private static final String GET_ACTIVE_CONTRACT_BY_TYPE_METHOD = CONTRACT_CONTROLLER + "get_active_contract_by_type";
    private static final String CONVERT_HTML_TO_PDF = CONTRACT_CONTROLLER + "convert_html_to_pdf";
    private static final String GET_CONTRACT_BY_ID_METHOD = CONTRACT_CONTROLLER + "get_contract";

    private static final String VH_OFERTA = "oferta_virtual_hosting";
    private static final String VH_CONTRACT = "virtual_hosting";
    private static final String VH_BUDGET_CONTRACT = "hms_virtual_hosting_budget_contract";
    private static final String NOTICE_RF = "hms_notice_rf";

    @Autowired
    public MajordomoRpcClient(
            @Value("${rpc.majordomo.url}") String serverURL,
            @Value("${rpc.majordomo.login}") String login,
            @Value("${rpc.majordomo.password}") String password
    ) throws MalformedURLException {
        super(serverURL, login, password);
    }

    private Contract getActiveContractByType(String type) {
        return callMethodNew(
                GET_ACTIVE_CONTRACT_BY_TYPE_METHOD,
                Arrays.asList(type),
                ContractResponse.class
        ).getContract();
    }

    public Contract getContractById(String id){
        return callMethodNew(
                GET_CONTRACT_BY_ID_METHOD,
                Arrays.asList(id),
                ContractResponse.class
        ).getContract();
    }

    public byte[] convertHtmlToPdfFile(String html){
        HtmlToPdfResponse htmlToPdfResponse = callMethodNew(
                CONVERT_HTML_TO_PDF, Arrays.asList(html), HtmlToPdfResponse.class
        );
        return Base64.getDecoder().decode(htmlToPdfResponse.getPdfFileInBase64());
    }

    public Contract getActiveContractVirtualHosting() {
        return getActiveContractByType(VH_CONTRACT);
    }

    public Contract getActiveNoticeRF(){
        return getActiveContractByType(NOTICE_RF);
    }

    public Contract getActiveBudgetContractVH(){
        return getActiveContractByType(VH_BUDGET_CONTRACT);
    }

    public Contract getActiveOfertaVirtualHosting() {
        return getActiveContractByType(VH_OFERTA);
    }
}