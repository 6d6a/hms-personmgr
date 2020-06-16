package ru.majordomo.hms.personmgr.importing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.Language;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.ResultWithWarning;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.feign.RcStaffFeignClient;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.service.BusinessHelper;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.staff.resources.template.ApplicationServer;
import ru.majordomo.hms.rc.staff.resources.template.HttpServer;
import ru.majordomo.hms.rc.user.resources.CharSet;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.WebSite;

import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

@Component
@RequiredArgsConstructor
public class WebsiteDBImportService{
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Qualifier("namedParameterJdbcTemplate")
    private final NamedParameterJdbcTemplate billingDbJdbcTemplate;
    private final BusinessHelper businessHelper;
    private final RcStaffFeignClient rcStaffClient;
    private final RcUserFeignClient rcUserClient;
    private final PersonalAccountDBImportService personalAccountDBImportService;
    private final ImportHelper importHelper;


    public static final Map<String, CharSet> STRING_CHAR_SET_HASH_MAP = new HashMap<>();

    private static final Pattern EXTENSION_PATTERN = Pattern.compile("^(?!\\.)[\\.a-zA-Zа-яА-Я0-9ёЁ\\-_]+");
    private static final String RELATIVE_FILE_PATH = "^(?!(\\.|/))[a-zA-Zа-яА-Я0-9ёЁ\\-_\\./]*";

    static {
        STRING_CHAR_SET_HASH_MAP.put("CP-1251", CharSet.CP1251);
        STRING_CHAR_SET_HASH_MAP.put("cp1251", CharSet.CP1251);
        STRING_CHAR_SET_HASH_MAP.put("windows-1251", CharSet.CP1251);
        STRING_CHAR_SET_HASH_MAP.put("windows-cp1251", CharSet.CP1251);
        STRING_CHAR_SET_HASH_MAP.put("koi8-r", CharSet.KOI8R);
        STRING_CHAR_SET_HASH_MAP.put("utf-8", CharSet.UTF8);
        STRING_CHAR_SET_HASH_MAP.put("utf8", CharSet.UTF8);
        STRING_CHAR_SET_HASH_MAP.put("iso-8859-1", CharSet.UTF8);
        STRING_CHAR_SET_HASH_MAP.put("", CharSet.UTF8);
    }

    @Data
    @AllArgsConstructor
    private static class LanguageDescription {
        private Language language;
        private String version;
        private String securityLevel;

        public boolean isSuit(@Nullable Service service) {
            if (service == null || StringUtils.isNotEmpty(service.getAccountId())) {
                return false;
            }
            if (service.getTemplate() instanceof HttpServer && language == Language.STATIC) {
                return true;
            } else if (service.getTemplate() instanceof ApplicationServer &&
                    language.equivalent(((ApplicationServer) service.getTemplate()).getLanguage())
            ) {
                if (StringUtils.isNotEmpty(version) && !version.equals(((ApplicationServer) service.getTemplate()).getVersion())) {
                    return false;
                }
                if (StringUtils.isNotEmpty(securityLevel) && !securityLevel.equals(service.getInstanceProps().get("security_level"))) {
                    return false;
                }
                return true;
            }
            return false;
        }
    }

    @Nullable
    private LanguageDescription parseFlag(String flagValue, @Nullable String apacheConf) {
        if (StringUtils.isBlank(flagValue)) {
            return null;
        }

        if ("perl518".equals(flagValue)) {
            return new LanguageDescription(Language.PERL, "", "");
        }
        Pattern phpPattern = Pattern.compile("php([\\d]{1,2})(?:-([\\w_]+))?");
        Matcher m;
        if ((m = phpPattern.matcher(flagValue)).matches()) {
            String version;
            if ("5".equals(m.group(1))) {
                version = "5.2";
            } else {
                StringBuilder sb = new StringBuilder(m.group(1));
                version = sb.insert(1, ".").toString();
            }
            String securityLevel = m.groupCount() == 2 ? "default" : m.group(2);

            if (apacheConf != null && apacheConf.matches(".*php_admin_flag\\s+engine\\s+off.*")) {
                return new LanguageDescription(Language.STATIC, "", "");
            }
            return new LanguageDescription(Language.PHP, version, securityLevel);
        } else {
            return null;
        }
    }

    private List<String> filterExtension(String[] extensions) {
        return Arrays.stream(extensions).map(s -> s.replaceFirst("^\\.", ""))
                .filter(s -> EXTENSION_PATTERN.matcher(s).matches()).collect(Collectors.toList());
    }

    public ResultWithWarning importToMongo(String accountId, String serverId, String operationId, boolean accountEnabled) {
        String query = "SELECT a.id, a.uid, a.server_id, a.homedir, " +
                "v.active, v.mod_php, v.mod_ssl, v.charset_disable, v.server, v.vhname, v.ServerName, " +
                "v.DocumentRoot, v.VirtualDocumentRoot, v.ScriptAlias, v.VirtualScriptAlias, v.CustomLog, " +
                "v.ErrorLog, v.DirectoryIndex, v.CharsetSourceEnc, v.Options, v.ServerAlias, v.apache_conf, " +
                "v.log_errors, v.prodfarm_id, v.anti_ddos, v.auto_subdomain, v.nginx_static, " +
                "v.unsecure_admin_panel_access, v.perl_lib_path, " +
                "d.Domain_name, " +
                "s.id as web_id, " +
                "nc.flag " +
                "FROM vhosts v " +
                "LEFT JOIN domain d ON v.ServerName = d.Domain_name " +
                "LEFT JOIN servers s ON CONCAT(s.name, '.majordomo.ru') = v.server " +
                "LEFT JOIN nginx_conf nc ON nc.redir_to = v.vhname AND nc.server = v.server " +
                "JOIN account a ON v.uid = a.uid " +
                "WHERE a.id = :accountId and nc.flag != 'otd_ip' " +
                "GROUP BY v.ServerName";
        SqlParameterSource sqlParams = new MapSqlParameterSource("accountId", accountId);

        ResultWithWarning result = new ResultWithWarning();
        String unixAccountId = "unixAccount_" + accountId;
        Set<Language> allowLanguageByPlan = personalAccountDBImportService.getWebSiteAllowedServiceTypes(accountId);
        List<Domain> domains = rcUserClient.getDomains(accountId);
        List<Service> services = rcStaffClient.getWebsiteServicesByServerId(serverId);

        SqlRowSet rs = billingDbJdbcTemplate.queryForRowSet(query, sqlParams);
        while (rs.next()) {
            if (rs.getString("ServerName").endsWith(".onparking.ru")) {
                continue;
            }

            String serverName = StringUtils.trimToEmpty(rs.getString("ServerName"));
            String unicodeServerName = java.net.IDN.toUnicode(serverName);
            if (allowLanguageByPlan == null) {
                result.addWarning(String.format("Сайт %s пропущен, так как не удалось определить разрешенные сервисы", unicodeServerName));
                continue;
            } else if (allowLanguageByPlan.isEmpty()) {
                continue;
            }

            String documentRoot = StringUtils.trimToEmpty(rs.getString("DocumentRoot")).replaceFirst("^/", "");
            boolean modPhp = "Y".equals(rs.getString("mod_php"));
            String flag = StringUtils.trimToEmpty(rs.getString("flag"));
            String apacheConf = rs.getString("apache_conf");
            boolean aniDdos = rs.getString("anti_ddos").equals("Y");
            String charsetSourceEnc = rs.getString("CharsetSourceEnc");
            boolean autoSubdomain = rs.getString("auto_subdomain").equals("Y");
            boolean oldHttp = rs.getString("unsecure_admin_panel_access").equals("Y");
            String scriptAlias = rs.getString("ScriptAlias");
            String directoryIndex = StringUtils.trimToEmpty(rs.getString("DirectoryIndex"));
            String nginxStatic = StringUtils.trimToEmpty(rs.getString("nginx_static"));
            String options = StringUtils.trimToEmpty(rs.getString("Options"));

            logger.debug("Found WebSite for acc id: " + accountId + " name: " + unicodeServerName);

            Service service;
            LanguageDescription languageDescription = parseFlag(flag, apacheConf);
            if (languageDescription != null) {
                if (!allowLanguageByPlan.contains(languageDescription.language)) {
                    if (languageDescription.language == Language.STATIC && allowLanguageByPlan.contains(Language.PERL)) {
                        languageDescription = new LanguageDescription(Language.PERL, "", "");
                    } else {
                        languageDescription = new LanguageDescription(allowLanguageByPlan.stream().findFirst().orElseThrow(InternalApiException::new), "", "");
                    }
                }

                final LanguageDescription finalLanguageDescription = languageDescription;
                service = services.stream().filter(finalLanguageDescription::isSuit).findFirst().orElse(null);
                if (service == null && finalLanguageDescription.getLanguage() == Language.PHP && finalLanguageDescription.getVersion().equals("4")) {
                    finalLanguageDescription.setVersion("5.2");
                    finalLanguageDescription.setSecurityLevel("default");
                    service = services.stream().filter(finalLanguageDescription::isSuit).findFirst().orElse(null);
                }
                if (service == null) {
                    finalLanguageDescription.setVersion("*");
                    finalLanguageDescription.setSecurityLevel("*");
                    service = services.stream().filter(finalLanguageDescription::isSuit).findFirst().orElse(null);
                }
                if (service == null) {
                    result.addWarning("Не удалось определить подходящий сервис для сайта " + unicodeServerName + ". Сайт пропущен");
                    continue;
                }
            } else {
                result.addWarning("Не удалось определить язык и версию у сайта " + unicodeServerName + ". Сайт пропущен");
                continue;
            }

            boolean usingPhp = ((ApplicationServer) service.getTemplate()).getLanguage() == ApplicationServer.Language.PHP;

            WebSite webSite = new WebSite();
            webSite.setAccountId(accountId);
            webSite.setSwitchedOn(accountEnabled);
            webSite.setName(unicodeServerName);
            webSite.setUnixAccountId(unixAccountId);
            webSite.setServiceId(service.getId());
            webSite.setCharSet(STRING_CHAR_SET_HASH_MAP.get(charsetSourceEnc));
            webSite.setDocumentRoot(documentRoot);
            webSite.setAutoSubDomain(autoSubdomain);
            webSite.setDdosProtection(aniDdos);

            webSite.setAccessLogEnabled(true);
            webSite.setErrorLogEnabled(true);
            webSite.setAccessByOldHttpVersion(oldHttp);

            if (scriptAlias.matches(RELATIVE_FILE_PATH)) {
                webSite.setScriptAlias(scriptAlias);
            }

            Pattern p;
            Matcher m;

            //CGI
            boolean cgiEnabled = Pattern.compile("[^-]?ExecCGI", CASE_INSENSITIVE).matcher(options).find();
            webSite.setCgiEnabled(cgiEnabled);

            if(StringUtils.isNotBlank(apacheConf)) {
                //SSI
                p = Pattern.compile("AddHandler\\s+server-parsed\\s+(.*)", CASE_INSENSITIVE);
                m = p.matcher(apacheConf);
                if (m.find()) {
                    webSite.setSsiEnabled(true);
                    String[] ssiFileExtensions = m.group(1).split("\\s+");
                    webSite.setSsiFileExtensions(filterExtension(ssiFileExtensions));
                } else {
                    webSite.setSsiEnabled(false);
                }

                //CGI FileExtensions
                p = Pattern.compile("AddHandler\\s+cgi-script\\s+(.*)", CASE_INSENSITIVE);
                m = p.matcher(apacheConf);
                if (m.find()) {
                    String[] cgiFileExtensions = m.group(1).split("\\s+");
                    webSite.setCgiFileExtensions(filterExtension(cgiFileExtensions));
                }

                if (usingPhp) {
                    // allow_url_fopen
                    m = Pattern.compile("php_admin_flag\\s+allow_url_fopen\\s+(\\w+)").matcher(apacheConf);
                    if (m.find()) {
                        if ("on".equalsIgnoreCase(m.group(1)) || "1".equalsIgnoreCase(m.group(1))) {
                            webSite.setAllowUrlFopen(true);
                        } else {
                            webSite.setAllowUrlFopen(false);
                        }
                    }

                    m = Pattern.compile("php_admin_flag\\s+allow_url_include\\s+(\\w+)").matcher(apacheConf);
                    if (m.find()) {
                        if ("on".equalsIgnoreCase(m.group(1)) || "1".equalsIgnoreCase(m.group(1))) {
                            webSite.setAllowUrlInclude(true);
                        } else {
                            webSite.setAllowUrlInclude(false);
                        }
                    }
                    m = Pattern.compile("php_admin_flag\\s+mbstring.func_overload\\s+([0-7])").matcher(apacheConf);
                    if (m.find()) {
                        try {
                            int mbstringFuncOverload = Integer.parseInt(m.group(1));
                            webSite.setMbstringFuncOverload(mbstringFuncOverload);
                        } catch (NumberFormatException ignore) { }
                    }
                }
            }

            //StaticFileExtensions
            webSite.setStaticFileExtensions(filterExtension(nginxStatic.split("\\s+")));

            //IndexFileList

            List<String> indexFileList;

            if (StringUtils.isNotBlank(directoryIndex)) {
                indexFileList = Arrays.asList(directoryIndex.split("\\s+"));
            } else {
                indexFileList = null;
            }
            webSite.setIndexFileList(indexFileList);

            Domain domain = domains.stream().filter(d -> unicodeServerName.equalsIgnoreCase(d.getName())).findFirst().orElse(null);

            if (domain != null) {
                webSite.addDomain(domain);
            } else {
                result.addWarning("Не удалось найти домен для сайта " + unicodeServerName + ". Сайт пропущен");
                continue;
            }

            SimpleServiceMessage message = importHelper.makeServiceMessage(accountId, operationId, webSite);
            message.addParam("domainIds", webSite.getDomainIds());
            message.addParam("applicationServiceId", webSite.getServiceId());

            businessHelper.buildActionByOperationId(BusinessActionType.WEB_SITE_CREATE_RC, message, operationId);
            logger.debug("Sent message with params: " + message.getParams());
        }

        result.getData().forEach(s -> businessHelper.addWarning(operationId, s));
        return result;
    }

    public void removeWebsites(String accountId, String operationId) throws InternalApiException {
        List<WebSite> webSites = rcUserClient.getWebSitesOnly(accountId);
        if (webSites == null) {
            throw new InternalApiException("Не удалось получить сайты на аккаунте");
        }
        webSites.forEach(website -> {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setOperationIdentity(operationId);
            message.addParam("resourceId", website.getId());
            message.setAccountId(accountId);
            if (StringUtils.isNotEmpty(website.getName())) {
                message.addParam("name", website.getName());
            }
            businessHelper.buildActionByOperationId(BusinessActionType.WEB_SITE_DELETE_RC, message, operationId);
        });

    }
}
