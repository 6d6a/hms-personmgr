package ru.majordomo.hms.personmgr.exception.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import ru.majordomo.hms.personmgr.exception.BaseException;
import ru.majordomo.hms.personmgr.exception.InternalApiException;

import java.io.IOException;
import java.util.ArrayList;

public class MajordomoFeignErrorDecoder implements ErrorDecoder {

    private ErrorDecoder delegate = new ErrorDecoder.Default();
    private Logger logger = LoggerFactory.getLogger(getClass());
    private final ObjectMapper mapper = new ObjectMapper();

    private void printError(String message) {
        logger.error(message);
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        byte[] responseBody;
        try {
            responseBody = IOUtils.toByteArray(response.body().asInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to process response body.", e);
        }

        if (response.status() >= 400 && response.status() <= 499) {
            try {
                return mapper.readValue(new String(responseBody), BaseException.class);
            } catch (Throwable ignore) {
                printError("Can't convert json to majordomo exception, exceptionMessage: " + ignore.getMessage() + " responseBody: " + new String(responseBody));
                ignore.printStackTrace();
                return new InternalApiException();
            }
        }

        if (response.status() >= 500 && response.status() <= 599) {
            HttpHeaders responseHeaders = new HttpHeaders();
            response.headers().entrySet().stream()
                    .forEach(entry -> responseHeaders.put(entry.getKey(), new ArrayList<>(entry.getValue())));

            HttpStatus statusCode = HttpStatus.valueOf(response.status());
            String statusText = response.reason();
            return new HttpServerErrorException(statusCode, statusText, responseHeaders, responseBody, null);
        }
        return delegate.decode(methodKey, response);
    }
}
