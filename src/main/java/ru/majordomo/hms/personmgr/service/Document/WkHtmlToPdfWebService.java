package ru.majordomo.hms.personmgr.service.Document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.feign.WkHttpToPdfFeignClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
@ParametersAreNonnullByDefault
public class WkHtmlToPdfWebService {
    private final WkHttpToPdfFeignClient wkHttpToPdfFeignClient;
    private final ObjectMapper objectMapper;

    public final static String PREFIX_IMAGE_PNG = "data:image/png;base64,";
    public final static String PREFIX_FONT_TTF = "data:font/truetype;charset=utf-8;base64,";

    /**
     * @param bodyHtml
     * @param footerHtml html документ, необязательный нижний колонтитул
     * @param options
     * @return pdf документ
     * @throws InternalApiException
     */
    @Nonnull
    public byte[] convertHtmlToPdfFile(String bodyHtml, @Nullable String footerHtml, @Nullable WkHtmlToPdfOptions options) throws InternalApiException {
        String wkHttpToPdfRequestJson = null;
        try {
            bodyHtml = Base64.getEncoder().encodeToString(bodyHtml.getBytes(StandardCharsets.UTF_8));
            if (StringUtils.isNotEmpty(footerHtml)) {
                footerHtml = Base64.getEncoder().encodeToString(footerHtml.getBytes(StandardCharsets.UTF_8));
            } else {
                footerHtml = null;
            }
            WkHttpToPdfFeignClient.WkHttpToPdfRequest request = new WkHttpToPdfFeignClient.WkHttpToPdfRequest(bodyHtml, footerHtml, options);
            wkHttpToPdfRequestJson = objectMapper.writeValueAsString(request);
            byte[] pdfBin = wkHttpToPdfFeignClient.convertHtmlToPdfFile(wkHttpToPdfRequestJson);
            return pdfBin;
        } catch (JsonProcessingException e) {
            log.error(String.format("Error when create json request for wkhtmltopdf, with message: %s", e.getMessage()), e);
            throw new InternalApiException("Ошибка при отправке запроса в сервисе формирования PDF", e);
        } catch (FeignException e) {
            if (e.status() == 413) {
                String sizeKb = "null";
                if (wkHttpToPdfRequestJson != null) {
                    sizeKb = Integer.toString(wkHttpToPdfRequestJson.length() / 1024);
                }
                log.error(String.format("Cannot send wkhtmltopdf too large request. Size: %s KB, Message: %s, content: %s", sizeKb, e.getMessage(), e.contentUTF8()), e);
            } else {
                log.error(String.format("Error when send wkhtmltopdf request, with message: %s, content: %s", e.getMessage(), e.contentUTF8()), e);
            }
            throw new InternalApiException("Ошибка при отправке запроса в сервисе формирования PDF", e);
        }
    }

    public byte[] convertHtmlToPdfFile(String bodyHtml, @Nullable WkHtmlToPdfOptions options) throws InternalApiException {
        return convertHtmlToPdfFile(bodyHtml, null, options);
    }

}
