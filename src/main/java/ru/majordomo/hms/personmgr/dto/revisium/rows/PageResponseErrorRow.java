package ru.majordomo.hms.personmgr.dto.revisium.rows;

import java.util.ArrayList;

public class PageResponseErrorRow extends ArrayList<String> {
    public String getPageResponseErrorUrl() {
        return this.get(0);
    }

    public String getPageResponseErrorCode() {
        return this.get(1);
    }
}
