package ru.majordomo.hms.personmgr.common.message;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.personmgr.common.CharSet;

/**
 * WebSiteCreateMessageParams
 */
public class WebSiteCreateMessageParams extends ServiceMessageParams {
    private String id;
    private List<String> domainIds = new ArrayList<>();
    private String name;
    private String applicationServerId;
    private String documentRoot;
    private String unixAccountId;
    private CharSet charSet;
    private boolean ssiEnabled;
    private List<String> ssiFileExtensions = new ArrayList<>();
    private boolean cgiEnabled;
    private List<String> cgiFileExtensions = new ArrayList<>();
    private String scriptAlias;
    private boolean autoSubDomain;
    private boolean accessByOldHttpVersion;
    private List<String> staticFileExtensions = new ArrayList<>();
    private List<String> indexFileList = new ArrayList<>();
    private boolean accessLogEnabled;
    private boolean errorLogEnabled;
    private boolean allowUrlFopen;
    private int mbstringFuncOverload;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getDomainIds() {
        return domainIds;
    }

    public void setDomainIds(List<String> domainIds) {
        this.domainIds = domainIds;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApplicationServerId() {
        return applicationServerId;
    }

    public void setApplicationServerId(String applicationServerId) {
        this.applicationServerId = applicationServerId;
    }

    public String getDocumentRoot() {
        return documentRoot;
    }

    public void setDocumentRoot(String documentRoot) {
        this.documentRoot = documentRoot;
    }

    public String getUnixAccountId() {
        return unixAccountId;
    }

    public void setUnixAccountId(String unixAccountId) {
        this.unixAccountId = unixAccountId;
    }

    public CharSet getCharSet() {
        return charSet;
    }

    public void setCharSet(CharSet charSet) {
        this.charSet = charSet;
    }

    public boolean isSsiEnabled() {
        return ssiEnabled;
    }

    public void setSsiEnabled(boolean ssiEnabled) {
        this.ssiEnabled = ssiEnabled;
    }

    public List<String> getSsiFileExtensions() {
        return ssiFileExtensions;
    }

    public void setSsiFileExtensions(List<String> ssiFileExtensions) {
        this.ssiFileExtensions = ssiFileExtensions;
    }

    public boolean isCgiEnabled() {
        return cgiEnabled;
    }

    public void setCgiEnabled(boolean cgiEnabled) {
        this.cgiEnabled = cgiEnabled;
    }

    public List<String> getCgiFileExtensions() {
        return cgiFileExtensions;
    }

    public void setCgiFileExtensions(List<String> cgiFileExtensions) {
        this.cgiFileExtensions = cgiFileExtensions;
    }

    public String getScriptAlias() {
        return scriptAlias;
    }

    public void setScriptAlias(String scriptAlias) {
        this.scriptAlias = scriptAlias;
    }

    public boolean isAutoSubDomain() {
        return autoSubDomain;
    }

    public void setAutoSubDomain(boolean autoSubDomain) {
        this.autoSubDomain = autoSubDomain;
    }

    public boolean isAccessByOldHttpVersion() {
        return accessByOldHttpVersion;
    }

    public void setAccessByOldHttpVersion(boolean accessByOldHttpVersion) {
        this.accessByOldHttpVersion = accessByOldHttpVersion;
    }

    public List<String> getStaticFileExtensions() {
        return staticFileExtensions;
    }

    public void setStaticFileExtensions(List<String> staticFileExtensions) {
        this.staticFileExtensions = staticFileExtensions;
    }

    public List<String> getIndexFileList() {
        return indexFileList;
    }

    public void setIndexFileList(List<String> indexFileList) {
        this.indexFileList = indexFileList;
    }

    public boolean isAccessLogEnabled() {
        return accessLogEnabled;
    }

    public void setAccessLogEnabled(boolean accessLogEnabled) {
        this.accessLogEnabled = accessLogEnabled;
    }

    public boolean isErrorLogEnabled() {
        return errorLogEnabled;
    }

    public void setErrorLogEnabled(boolean errorLogEnabled) {
        this.errorLogEnabled = errorLogEnabled;
    }

    public boolean isAllowUrlFopen() {
        return allowUrlFopen;
    }

    public void setAllowUrlFopen(boolean allowUrlFopen) {
        this.allowUrlFopen = allowUrlFopen;
    }

    public int getMbstringFuncOverload() {
        return mbstringFuncOverload;
    }

    public void setMbstringFuncOverload(int mbstringFuncOverload) {
        this.mbstringFuncOverload = mbstringFuncOverload;
    }

    @Override
    public String toString() {
        return "WebSiteCreateMessageParams{" +
                "id='" + id + '\'' +
                ", domainIds=" + domainIds +
                ", name='" + name + '\'' +
                ", applicationServerId='" + applicationServerId + '\'' +
                ", documentRoot='" + documentRoot + '\'' +
                ", unixAccountId='" + unixAccountId + '\'' +
                ", charSet=" + charSet +
                ", ssiEnabled=" + ssiEnabled +
                ", ssiFileExtensions=" + ssiFileExtensions +
                ", cgiEnabled=" + cgiEnabled +
                ", cgiFileExtensions=" + cgiFileExtensions +
                ", scriptAlias='" + scriptAlias + '\'' +
                ", autoSubDomain=" + autoSubDomain +
                ", accessByOldHttpVersion=" + accessByOldHttpVersion +
                ", staticFileExtensions=" + staticFileExtensions +
                ", indexFileList=" + indexFileList +
                ", accessLogEnabled=" + accessLogEnabled +
                ", errorLogEnabled=" + errorLogEnabled +
                ", allowUrlFopen=" + allowUrlFopen +
                ", mbstringFuncOverload=" + mbstringFuncOverload +
                "} " + super.toString();
    }
}
