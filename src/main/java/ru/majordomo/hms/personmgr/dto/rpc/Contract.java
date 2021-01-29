package ru.majordomo.hms.personmgr.dto.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import ru.majordomo.hms.personmgr.common.Utils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Contract {

    private String body;
    private String footer;
    private Boolean status; //'active', 'archived', 'draft'

    /** "hms_virtual_hosting_budget_contract", 'oferta', 'company', 'entrepreneur', 'oferta_virtual_hosting', 'virtual_hosting' */
    private String type;

    @JsonProperty("no_footer_pages")
    private List<Integer> noFooterPages; //no_footer_pages": "3,5,7",

    @JsonProperty("contract_id")
    private Integer contractId; //contract_id": "128",

    public void setBody(String body) {
        try {
            this.body = Utils.convertToUTF8(body, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            this.body = body;
        }
    }

    public void setFooter(String footer) {
        try {
            this.footer = Utils.convertToUTF8(footer, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            this.footer = footer;
        }
    }

    public void setNoFooterPages(String row) {
        if (row == null || row.length() == 0) {
            this.noFooterPages = Collections.emptyList();
        } else {
            List<String> strings = Arrays.asList(row.split(","));
            this.noFooterPages = strings
                    .stream()
                    .map(s -> Integer.valueOf(s.trim()))
                    .collect(Collectors.toList());
        }
    }

    public void setStatus(String status){
        this.status = status.equals("active");
    }
}
