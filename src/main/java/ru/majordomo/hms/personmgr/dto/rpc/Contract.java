package ru.majordomo.hms.personmgr.dto.rpc;

import ru.majordomo.hms.personmgr.common.Utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Contract implements RpcResponse {
    private static final String BODY_KEY = "body";
    private static final String FOOTER_KEY = "footer";
    private static final String NO_FOOTER_PAGES = "no_footer_pages";
    private static final String OPERATOR_ID_KEY = "operator_id";
    private static final String STATUS_KEY = "status";
    private static final String UPDATED_KEY = "updated";
    private static final String TYPE_KEY = "type";
    private static final String CONTRACT_ID_KEY = "contract_id";
    private static final String RPC_RESPONSE_CODING = "ISO-8859-1";


    private String body;
    private String footer;
    private List<Integer> noFooterPages; //no_footer_pages": "3,5,7",
    private Integer operatorId; //operator_id": "41",
    private Boolean status; //'active', 'archived', 'draft'
    private LocalDateTime updated; //updated": "2015-03-19 15:58:18"
    private String type; //'oferta', 'company', 'entrepreneur', 'oferta_virtual_hosting', 'virtual_hosting'
    private Integer contractId; //contract_id": "128",

    @Override
    public void mapping(Map<?, ?> contract){
        try {
            setBody(Utils.convertToUTF8((String) contract.get(BODY_KEY), RPC_RESPONSE_CODING));
            setFooter(Utils.convertToUTF8((String) contract.get(FOOTER_KEY), RPC_RESPONSE_CODING));
            setStatus((String) contract.get(STATUS_KEY));
            setType((String) contract.get(TYPE_KEY));
            setUpdated(LocalDateTime.parse((String) contract.get(UPDATED_KEY), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            setContractId(Integer.valueOf((String) contract.get(CONTRACT_ID_KEY)));
            setNoFooterPages((String) contract.get(NO_FOOTER_PAGES));
            setOperatorId(Integer.valueOf((String) contract.get(OPERATOR_ID_KEY)));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public Integer getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Integer operatorId) {
        this.operatorId = operatorId;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getContractId() {
        return contractId;
    }

    public void setContractId(Integer contractId) {
        this.contractId = contractId;
    }

    public List<Integer> getNoFooterPages() {
        return noFooterPages;
    }

    public void setNoFooterPages(List<Integer> noFooterPages) {
        this.noFooterPages = noFooterPages;
    }

    public void setNoFooterPages(String row) {
        if (row == null || row.length() == 0) {
            setNoFooterPages(Collections.emptyList());
        } else {
            List<String> strings = Arrays.asList(row.split(","));
            setNoFooterPages(
                    strings
                            .stream()
                            .map(Integer::valueOf)
                            .collect(Collectors.toList())
            );
        }
    }

    public void setStatus(String status){
        setStatus(status.equals("active"));
    }
}
