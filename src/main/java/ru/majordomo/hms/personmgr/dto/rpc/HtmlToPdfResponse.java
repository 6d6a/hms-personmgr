package ru.majordomo.hms.personmgr.dto.rpc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class HtmlToPdfResponse extends BaseRpcResponse {

    @JsonProperty("pdf_file")
    private String pdfFileInBase64;
}
