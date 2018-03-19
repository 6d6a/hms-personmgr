package ru.majordomo.hms.personmgr.dto.revisium.rows;

import java.util.ArrayList;
import java.util.List;

public class ExternalLinksRow extends ArrayList<Object> {
    public String getExternalLinkUrl() {
        return (String) this.get(0);
    }
    public String getExternalLinkTitle() {
        return (String) this.get(1);
    }
    public String getExternalLinkType() {
        return (String) this.get(2);
    }
    public List<String> getExternalLinkIdentity() {
        return (List<String>) this.get(3);
    }
}
