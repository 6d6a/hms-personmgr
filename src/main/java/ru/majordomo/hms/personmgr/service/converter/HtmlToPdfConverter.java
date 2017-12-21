package ru.majordomo.hms.personmgr.service.converter;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.FileUtils;

import java.io.*;

@Service
public class HtmlToPdfConverter {

    private String wkhtmltopdfUrl;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    public HtmlToPdfConverter(
            @Value("${converter.wkhtmltopdf.url}") String wkhtmltopdfUrl
    ){
        this.wkhtmltopdfUrl = wkhtmltopdfUrl;
    }

    public void convertHtmlToPdf(String sourceHtmlFilePath, String destinationPdfFilePath) throws Exception {
        File html = new File(sourceHtmlFilePath);
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost post = new HttpPost(this.wkhtmltopdfUrl);
        MultipartEntity entity = new MultipartEntity();
        entity.addPart("file", new FileBody(html));
        post.setEntity(entity);

        HttpResponse response = httpclient.execute(post);

        if (response.getStatusLine().getStatusCode() == 200) {
            FileUtils.saveInputStreamToFile(
                    response.getEntity().getContent(),
                    destinationPdfFilePath
            );
        } else {
            logger.error(
                    "Wkhtmltopdf service return response with code " + response.getStatusLine().getStatusCode() +
                    " Response content: " +
                            FileUtils.getStringFromInputStream(response.getEntity().getContent())
            );
            throw new Exception("Can't convert html to pdf.");
        }
    }
}
