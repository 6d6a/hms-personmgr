package ru.majordomo.hms.personmgr.exception.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.majordomo.hms.personmgr.exception.BaseException;

import java.io.IOException;
import java.nio.charset.Charset;

public class MajordomoFeignErrorDecoder implements ErrorDecoder {

    private ErrorDecoder delegate = new ErrorDecoder.Default();
    private Logger logger = LoggerFactory.getLogger(getClass());
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        String responseBody = null;
        try {
            if (response.body() != null)
                responseBody = Util.toString(response.body().asReader());
        } catch (IOException e) {
            throw new RuntimeException("Failed to process response body.", e);
        }

        if (response.status() >= 400 && response.status() <= 499) {
            if (responseBody != null) {
                try {
                    return mapper.readValue(responseBody, BaseException.class);
                } catch (Throwable ignore) {
                    logger.warn("Can't convert body to majordomo exception, exceptionMessage: " + ignore.getMessage()
                            + " responseBody: " + responseBody);
                }
            }
        }

        /*
         * тело ответа уже прочитано из потока и не может быть прочитано повторно,
         * поэтому создаем новый ответ с прочитанным телом и передаем декодеру по-умолчанию
         */
        response = Response.builder()
                .body(responseBody, Charset.defaultCharset())
                .headers(response.headers())
                .reason(response.reason())
                .request(response.request())
                .status(response.status())
                .build();

        return delegate.decode(methodKey, response);
    }
}
