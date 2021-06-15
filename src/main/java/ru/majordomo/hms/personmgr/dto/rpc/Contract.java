package ru.majordomo.hms.personmgr.dto.rpc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import ru.majordomo.hms.personmgr.common.DocumentType;
import ru.majordomo.hms.personmgr.common.Utils;

import javax.annotation.Nonnull;
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

    /**
     * "hms_virtual_hosting_budget_contract", 'oferta', 'company', 'entrepreneur', 'oferta_virtual_hosting', 'virtual_hosting'
     * или {@link DocumentType#getBilling2Type()}
     */
    private String type;

    @JsonProperty("no_footer_pages")
    private List<Integer> noFooterPages; //no_footer_pages": "3,5,7",

    @JsonProperty("contract_id")
    private Integer contractId; //contract_id": "128",

    @JsonSetter
    public void setBody(String bodyLatin1) {
        setBody(bodyLatin1, false);
    }

    @JsonIgnore
    public void setBody(String bodyLatin1OrUtf8, boolean noConvertToUtf8) {
        try {
            this.body = noConvertToUtf8 ? bodyLatin1OrUtf8 : Utils.convertToUTF8(bodyLatin1OrUtf8, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            this.body = bodyLatin1OrUtf8;
        }
    }

    @JsonSetter
    public void setFooter(String footerLatin1) {
        setFooter(footerLatin1, false);
    }

    @JsonIgnore
    public void setFooter(String footerLatin1OrUtf8, boolean noConvertToUtf8) {

        try {
            this.footer = noConvertToUtf8 ? footerLatin1OrUtf8 : Utils.convertToUTF8(footerLatin1OrUtf8, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            this.footer = footerLatin1OrUtf8;
        }
    }

    public void setNoFooterPages(@Nonnull int[] noFooterPages) {
        this.noFooterPages = Arrays.stream(noFooterPages).boxed().collect(Collectors.toList());
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
