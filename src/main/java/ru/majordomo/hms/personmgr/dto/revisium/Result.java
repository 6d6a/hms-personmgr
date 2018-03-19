package ru.majordomo.hms.personmgr.dto.revisium;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.majordomo.hms.personmgr.dto.revisium.rows.*;

import java.util.HashMap;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class Result {

    /*
    array(
        array('http://mysite.com/', '403'),
        array('http://mysite.com/suburl/', '404'),
        array('wert:\\werwer/ru/%00/', 'Invalid URL')
    );
    */
    @JsonProperty("page_response_errors")
    private List<PageResponseErrorRow> pageResponseErrors;

    /*
    array(
        'YSB' => 'phishing',
        'GSB' => 'malware',
        'VT'  => array('Kaspersky', 'DrWeb', 'Sophos'),
        'rublacklist' => 'blocked',
    );
    */
    @JsonProperty("blacklisted")
    private HashMap<String, Object> blacklisted;

    /*
    array(
        array('http://mysite.com/', 'CMW-INJ-01009-js.mlw', 'malware snippet'),
        array('http://mysite.com/', 'CMW-INJ-01010-js.mlw', 'malware snippet')
    );
    */
    @JsonProperty("html_malware")
    private List<HtmlMalwareRow> htmlMalware;

    /*
    array(
        'http://mysite.ru/main.js'  => array(array('CMW-INJ-01009-js.mlw', 'malware snippet'),
                                             array('CMW-INJ-01032-js.mlw', 'malware snippet')),
        'http://mysite.ru/main.css' => array(array('CMW-INJ-01302-js.mlw', 'malware snippet')
    );
    */
    @JsonProperty("files_malware")
    private HashMap<String, List<FilesMalwareRow>> filesMalware;


    /*
    array(
        array('http://1.trymyfinger.ru/js/jquery.js','scriptUrls'),
        array('http://1eregfvsdg3435.malware.xyz/checkitout/','exLinks'),
        array('http://1.trymyfinger.ru/frame/','iFrame'),
        array('http://1.trymyfinger.ru/main.css','externalResources')
    );

    scriptUrls - скрипты
    iFrames – вставки IFRAME
    sideFrames – динамические фреймы
    objects - <OBJECT> и <EMBED>
    exLinks – внешние ссылки
    externalResources – внешние ресурсы (CSS, изображения)
    redirects – редирект на опасные сайты
    */
    @JsonProperty("blacklisted_urls")
    private List<BlacklistUrlRow> blacklistedUrls;


    /*
    array(
        'UA_MOBILE | REF_SERP' => array(
            array('','http://site1.ru','http://site2.ru','http://site3.ru'),
            array('blacklisted','http://site1.ru','http://site4.ru','http://site5.ru'),
        ),
        'UA_MOBILE | REF_SUBPAGE' => array(
            array('executable','http://site1.ru','http://site2.ru','Application.exe')
        ),
        'UA_DESKTOP | REF_SOCIAL' => array(
            array('blacklisted','http://site1.ru','http://site7.ru','http://site8.ru'),
        ),
    )

    UA_MOBILE | REF_SERP – запрос был отправлен с мобильного User Agent из результатов поисковой выдачи.
    UA_DESKTOP | REF_SUBPAGE – запрос был отправлен с User Agent десктопного браузера с внутренней страницы сайта.
    UA_MOBILE | REF_SOCIAL – запрос был отправлен с User Agent мобильного устройства с переходом из социальных сетей.

    1. пустая строка “” – неизвестный тип (может быть легитимным редиректом)
    2. строка “executable” – тип редиректа на исполняемый файл (опасный, может быть drive-by download атакой)
    3. строка “blacklisted” – тип редиректа с опасными доменами в списке перенаправлений
    */
    @JsonProperty("redirects")
    private HashMap<String, List<RedirectRow>> redirects;

    /*
    array(
        array('http://site1.ru/main.js',"scriptUrls"),
        array('http://site2.ru/zzz.html,"iFrames"),
        array('http://site3.ru/test.jpg',"externalResources")
    );
    */
    @JsonProperty("suspicious_urls")
    private List<SuspiciousRow> suspiciousUrls;

    /*
    array(
        array('http://site1.ru/main.js', '404'),
        array('http://site1.ru/main.css', '401'),
        array('http://site1.ru/adasdasd.jpg', '404'),
    );
     */
    @JsonProperty("resources_errors")
    private List<ResourceErrorRow> resourcesErrors;

    /*
    array(
        array('http://site2.ru/logo.jpg', "resourceList"),
        array('http://site3.ru/test.swf', "embedsList")
    );

    resourceList
    objectsList
    embedsList
    appletsList
     */
    @JsonProperty("external_resources")
    private List<ResourceExternalRow> externalResources;

    /*
    array(
        'http://mysite1.com/' => array(
            'http://facebook.com/article',
            'Facebook',
            '',
            array('UA_DESKTOP | REF_SUBPAGE', 'UA_BOT | REF_SUBPAGE')
        ),
        'http://mysite2.com/' => array(
            'http://adulttizernet.ru/',
            'Adult Only',
            'malware',
            array('UA_DESKTOP | REF_SUBPAGE', 'UA_BOT | REF_SUBPAGE')
        )
    );
    */
    @JsonProperty("external_links")
    private HashMap<String, ExternalLinksRow> externalLinks;

    /*
    массив php/mysql ошибок, обнаруженных на странице.
    */
    @JsonProperty("issues")
    private List<Object> issues;

    @JsonProperty("diff")
    private Boolean diff;
}
