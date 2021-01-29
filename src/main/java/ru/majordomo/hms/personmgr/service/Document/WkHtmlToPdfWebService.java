package ru.majordomo.hms.personmgr.service.Document;

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
        try {
            bodyHtml = Base64.getEncoder().encodeToString(bodyHtml.getBytes(StandardCharsets.UTF_8));
            if (StringUtils.isNotEmpty(footerHtml)) {
                footerHtml = Base64.getEncoder().encodeToString(footerHtml.getBytes(StandardCharsets.UTF_8));
            } else {
                footerHtml = null;
            }
            byte[] result = wkHttpToPdfFeignClient.convertHtmlToPdfFile(new WkHttpToPdfFeignClient.WkHttpToPdfRequest(bodyHtml, footerHtml, options));
            return result;
        } catch (FeignException e) {
            log.error(String.format("Error when send wkhtmltopdf request, with message: %s, content: %s", e.getMessage(), e.contentUTF8()), e);
            throw new InternalApiException("Ошибка при отправке запроса в сервисе формирования PDF", e);
        }
    }

    public byte[] convertHtmlToPdfFile(String bodyHtml, @Nullable WkHtmlToPdfOptions options) throws InternalApiException {
        return convertHtmlToPdfFile(bodyHtml, null, options);
    }

}
