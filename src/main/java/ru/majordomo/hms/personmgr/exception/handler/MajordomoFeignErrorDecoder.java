package ru.majordomo.hms.personmgr.exception.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.majordomo.hms.personmgr.exception.BaseException;

import java.io.IOException;

public class MajordomoFeignErrorDecoder implements ErrorDecoder {

    private ErrorDecoder delegate = new ErrorDecoder.Default();
    private Logger logger = LoggerFactory.getLogger(getClass());
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        byte[] responseBody = null;
        try {
            if (response.body() != null)
                responseBody = IOUtils.toByteArray(response.body().asInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to process response body.", e);
        }

        if (response.status() >= 400 && response.status() <= 499) {
            if (responseBody != null) {
                try {
                    return mapper.readValue(new String(responseBody), BaseException.class);
                } catch (Throwable ignore) {
                    logger.error("Can't convert body to majordomo exception, exceptionMessage: " + ignore.getMessage()
                            + " responseBody: " + new String(responseBody));
                    return delegate.decode(methodKey, response);
                }
            }
        }

        return delegate.decode(methodKey, response);
    }
}
