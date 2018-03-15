package ru.majordomo.hms.personmgr.dto.revisium;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class MonitoringCustomDeserializer extends JsonDeserializer<Monitoring> {

    @Override
    public Monitoring deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectCodec oc = p.getCodec();
        JsonNode node = oc.readTree(p);

        Monitoring monitoring = new Monitoring();

        try {
            RawMonitoring  rawMonitoring = new ObjectMapper().convertValue(node, RawMonitoring.class);

            monitoring.setHtmlMalware(rawMonitoring.getHtmlMalware());
            monitoring.setFilesMalware(rawMonitoring.getFilesMalware());
            monitoring.setBlacklistedUrls(rawMonitoring.getBlacklistedUrls());
            monitoring.setRedirects(rawMonitoring.getRedirects());
            monitoring.setResponseErrors(rawMonitoring.getResponseErrors());
            monitoring.setBlacklisted(rawMonitoring.getBlacklisted());
            monitoring.setSuspiciousUrls(rawMonitoring.getSuspiciousUrls());
            monitoring.setExternalResources(rawMonitoring.getExternalResources());
            monitoring.setExternalLinks(rawMonitoring.getExternalLinks());
            monitoring.setIssues(rawMonitoring.getIssues());
            monitoring.setIp(rawMonitoring.getIp());
            monitoring.setDns(rawMonitoring.getDns());
            monitoring.setDnsExpiration(rawMonitoring.getDnsExpiration());
            monitoring.setCms(rawMonitoring.getCms());
            monitoring.setJsErrors(rawMonitoring.getJsErrors());

        } catch (IllegalArgumentException e) {
            //К нам пришёл "monitoring":[] заместо положенного "monitoring":{}
            e.printStackTrace();
        }

//      monitoring = super.deserialize(p, ctxt, new Monitoring());

        return monitoring;
    }

    @Override
    public Class<Monitoring> handledType() {
        return Monitoring.class;
    }
}