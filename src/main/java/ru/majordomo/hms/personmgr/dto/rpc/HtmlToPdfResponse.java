package ru.majordomo.hms.personmgr.dto.rpc;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HtmlToPdfResponse extends BaseRpcResponse {

    @JsonProperty("pdf_file")
    private String pdfFileInBase64;

    public String getPdfFileInBase64() {
        return pdfFileInBase64;
    }

    public void setPdfFileInBase64(String pdfFileInBase64) {
        this.pdfFileInBase64 = pdfFileInBase64;
    }
}
