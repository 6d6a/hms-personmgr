package ru.majordomo.hms.personmgr.service.Document;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import ru.majordomo.hms.personmgr.common.FileUtils;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.dto.rpc.Contract;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.service.Rpc.MajordomoRpcClient;

import java.io.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class NoticeRFBuilder extends DocumentBuilderImpl {

    private final MajordomoRpcClient majordomoRpcClient;
    private AccountOwner accountOwner;

    private final String tmpDir = System.getProperty("java.io.tmpdir") + "/";
    private final String wkhtmltopdfUrl;

    private String template;
    private Contract contract;
    private Map<String, String> replaceMap;

    public NoticeRFBuilder(
            MajordomoRpcClient majordomoRpcClient,
            AccountOwnerManager accountOwnerManager,
            String personalAccountId,
            boolean withoutStamp,
            String wkhtmltopdfUrl
    ) {
        setWithoutStamp(withoutStamp);
        this.majordomoRpcClient = majordomoRpcClient;
        this.accountOwner = accountOwnerManager.findOneByPersonalAccountId(personalAccountId);
        this.wkhtmltopdfUrl = wkhtmltopdfUrl;
    }


    @Override
    public void buildTemplate() {
        try {
            InputStream inputStream = this.getClass().getResourceAsStream("/contract/notification.xhtml");
            template = CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));
        } catch (IOException e) {
            logger.error("Не удалось создать шаблон уведомления " + e.getMessage());
            e.printStackTrace();
            template = "";
        }
//        contract = majordomoRpcClient.getActiveNoticeRF();
        //должна быть одна страница
//        String nextTemplateWithFootherTag = "<pdf:nexttemplate name=\"withfooter\"/><pdf:nextpage/>";
//        template = contract.getBody() + nextTemplateWithFootherTag + contract.getFooter();
//        String stampPath = "/images/stamp_hosting.gif";
//        String logoPath = "/images/majordomo.png";
//        String imageSrcBeforeBase64 = "data:image/png;base64,";
//        String stamp = "<img src=\"data:image/png;base64,#STAMP_HOSTING#\" alt=\"Подпись/печать\"/>";
    }

    @Override
    public void replaceFields() {
        replaceFieldsWithReplaceMap(replaceMap);
    }

    @Override
    public void convert() {
        try {
            File htmlFile = new File(tmpDir + accountOwner.getPersonalAccountId() + "_notice_rf.html");
            FileUtils.saveFile(htmlFile, template);
            file = convertHtmlToPdf(htmlFile);
        } catch (IOException e){}
    }

    public byte[] convertHtmlToPdf(File htmlFile) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost post = new HttpPost(wkhtmltopdfUrl);
        MultipartEntity entity = new MultipartEntity();
        entity.addPart("file", new FileBody(htmlFile));
        post.setEntity(entity);

        HttpResponse response = httpclient.execute(post);

        byte[] targetArray;
        if (response.getStatusLine().getStatusCode() == 200) {
            targetArray = IOUtils.toByteArray(response.getEntity().getContent());
        } else {
            logger.error(
                    "Wkhtmltopdf service return response with code " + response.getStatusLine().getStatusCode() +
                            " Response content: " +
                            FileUtils.getStringFromInputStream(response.getEntity().getContent())
            );
            throw new IOException("Can't convert html to pdf.");
        }

        return targetArray;
    }

    @Override
    public void saveAccountDocument() {

    }

    @Override
    public void buildReplaceParameters(){
        Map<String, String> replaceMap = new HashMap<>();

        //обязательные параметры
        String stamp = "<img src=\"data:image/png;base64," +
                getResourceInBase64("/images/stamp_hosting.gif") +
                "\" alt=\"Подпись/печать\"/>";

        String dateInString = String.valueOf(LocalDate.now().getDayOfMonth()) +
                " " + Utils.getMonthName(LocalDate.now().getMonthValue()) +
                " " + String.valueOf(LocalDate.now().getYear());

        replaceMap.put("#ORG_NAME#", accountOwner.getName());
        replaceMap.put("#ORG_ADDRESS#", accountOwner.getContactInfo().getPostalAddress());
        replaceMap.put("#DATE#", dateInString);
        replaceMap.put("#STAMP#", isWithoutStamp() ? "" : stamp);
        replaceMap.put("#MAJORDOMO_LOGO#", getResourceInBase64("/images/majordomo.png"));

        this.replaceMap = replaceMap;
    }

    private void replaceFieldsWithReplaceMap(Map<String, String> replaceMap){
        for (Map.Entry<String, String> entry : replaceMap.entrySet()) {
            template = template.replaceAll(entry.getKey(), entry.getValue());
        }
    }

    private String getResourceInBase64(String resourcePath){
        InputStream inputStream = this.getClass()
                .getResourceAsStream(resourcePath);
        String imageStr = "";
        try {
            byte[] bytes = IOUtils.toByteArray(inputStream);
            imageStr = Base64.encodeBase64String(bytes);
        } catch (IOException e) {
            logger.error("Ошибка при получении файла " + resourcePath);
        }
        return imageStr;
    }
}
