package ru.majordomo.hms.personmgr.feign;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.config.WkHttpToPdfFeignConfig;
import ru.majordomo.hms.personmgr.service.Document.WkHtmlToPdfOptions;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/** Клиент для https://gitlab.intr/dockerfiles/wkhtml2pdf */
@ParametersAreNonnullByDefault
@FeignClient(name = "wkhttptopdf", url = "${converter.wkhtmltopdf.url}", configuration = WkHttpToPdfFeignConfig.class)
public interface WkHttpToPdfFeignClient {
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @ParametersAreNonnullByDefault
    class WkHttpToPdfRequest {
        /** основной html документ закодированный в base64 */
        @JsonProperty("contents")
        private String bodyHtmlBase64;
        /** Нижний колонтитул, html документ закодированный в base64, необязательно */
        @Nullable
        @JsonProperty("footer")
        private String footerHtmlBase64;
        @Nullable
        WkHtmlToPdfOptions options;
    }

    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    byte[] convertHtmlToPdfFile(@RequestBody WkHttpToPdfRequest request);

    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    byte[] convertHtmlToPdfFile(@RequestBody String wkHttpToPdfRequestJson);
}
