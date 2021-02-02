package ru.majordomo.hms.personmgr.service.Document;

import brave.internal.Nullable;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import ru.majordomo.hms.personmgr.common.FileUtils;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;


@ParametersAreNonnullByDefault
public class NoticeRFBuilder extends DocumentBuilderImpl {
    @Nullable
    private AccountOwner accountOwner;

    private final WkHtmlToPdfWebService wkHtmlToPdfWebService;

    private String template;
    private Map<String, String> replaceMap;

    @Nullable
    private final Map<String, String> params;

    public NoticeRFBuilder(
            AccountOwnerManager accountOwnerManager,
            @Nullable String personalAccountId,
            boolean withoutStamp,
            WkHtmlToPdfWebService wkHtmlToPdfWebService,
            @Nullable Map<String, String> params
    ) {
        setWithoutStamp(withoutStamp);
        this.params = params;
        if (personalAccountId != null) {
            this.accountOwner = accountOwnerManager.findOneByPersonalAccountId(personalAccountId);
        }
        this.wkHtmlToPdfWebService = wkHtmlToPdfWebService;
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
    }

    @Override
    public void replaceFields() {
        replaceFieldsWithReplaceMap(replaceMap);
    }

    @Override
    public void convert() {
        WkHtmlToPdfOptions options = new WkHtmlToPdfOptions();
        options.setDisableSmartShrinking(true);
        options.setPrintMediaType(true);
        options.setDpi(300);
        options.setMarginBottom("10mm");
        options.setMarginRight("10mm");
        options.setMarginTop("10mm");
        options.setMarginLeft("20mm");
        file = wkHtmlToPdfWebService.convertHtmlToPdfFile(template, options);
    }

    @Override
    public void saveAccountDocument() {

    }

    @Override
    public void buildReplaceParameters(){
        try {
            Map<String, String> replaceMap = new HashMap<>();

            //обязательные параметры
            String stamp = "<img src=\"data:image/png;base64," +
                           FileUtils.getResourceInBase64("/images/stamp_hosting.png") +
                           "\" alt=\"Подпись\"/><img width=70% height=auto class=\"sign\" src=\"data:image/png;base64," +
                           FileUtils.getResourceInBase64("/images/sign-ts.png") +
                           "\" alt=\"Печать\"/>";


            String dateInString = String.valueOf(LocalDate.now().getDayOfMonth()) +
                                  " " + Utils.getMonthName(LocalDate.now().getMonthValue()) +
                                  " " + String.valueOf(LocalDate.now().getYear());

            replaceMap.put("#ORG_NAME#", accountOwner != null ? accountOwner.getName() : params.get("ORG_NAME"));
            replaceMap.put("#ORG_ADDRESS#", accountOwner != null ? (accountOwner.getContactInfo().getPostalAddress() != null
                    ? accountOwner.getContactInfo().getPostalAddress() : "") : params.get("ORG_ADDRESS"));
            replaceMap.put("#DATE#", dateInString);
            replaceMap.put("#STAMP#", isWithoutStamp() ? "" : stamp);
            replaceMap.put("#MAJORDOMO_LOGO#", FileUtils.getResourceInBase64("/images/majordomo.png"));

            this.replaceMap = replaceMap;
        } catch (IOException e) {
            throw new InternalApiException("Ошибка при формировании уведомления о расположении серверов", e);
        }
    }

    private void replaceFieldsWithReplaceMap(Map<String, String> replaceMap){
        for (Map.Entry<String, String> entry : replaceMap.entrySet()) {
            template = template.replaceAll(entry.getKey(), entry.getValue());
        }
    }

}
