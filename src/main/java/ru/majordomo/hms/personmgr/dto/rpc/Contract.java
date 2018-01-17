package ru.majordomo.hms.personmgr.dto.rpc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Contract {

    private String body;
    private String footer;
    private Boolean status; //'active', 'archived', 'draft'
    private String type; //'oferta', 'company', 'entrepreneur', 'oferta_virtual_hosting', 'virtual_hosting'

    @JsonIgnore
    private LocalDateTime updated;

    @JsonProperty("no_footer_pages")
    private List<Integer> noFooterPages; //no_footer_pages": "3,5,7",

    @JsonProperty("operator_id")
    private Integer operatorId; //operator_id": "41",

    @JsonProperty("contract_id")
    private Integer contractId; //contract_id": "128",

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

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }
}
